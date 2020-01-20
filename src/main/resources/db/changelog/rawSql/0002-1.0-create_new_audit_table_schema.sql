
-- create a new schema named "CAZ_PAYMENT_AUDIT"
CREATE SCHEMA IF NOT EXISTS CAZ_PAYMENT_AUDIT;
REVOKE CREATE ON schema CAZ_PAYMENT_AUDIT FROM public;
 
CREATE TABLE IF NOT EXISTS CAZ_PAYMENT_AUDIT.logged_actions (
    schema_name text NOT NULL,
    TABLE_NAME text NOT NULL,
    user_name text,
    action_tstamp TIMESTAMP WITH TIME zone NOT NULL DEFAULT CURRENT_TIMESTAMP,
    action TEXT NOT NULL CHECK (action IN ('I','D','U')),
    original_data text,
    new_data text,
    query text
) WITH (fillfactor=100);
 
REVOKE ALL ON CAZ_PAYMENT_AUDIT.logged_actions FROM public;
 
-- You may wish to use different permissions; this lets anybody
-- see the full audit data. In Pg 9.0 and above you can use column
-- permissions for fine-grained control.
GRANT SELECT ON CAZ_PAYMENT_AUDIT.logged_actions TO public;
 
CREATE INDEX IF NOT EXISTS logged_actions_schema_table_idx 
ON CAZ_PAYMENT_AUDIT.logged_actions(((schema_name||'.'||TABLE_NAME)::TEXT));
 
CREATE INDEX IF NOT EXISTS logged_actions_action_tstamp_idx 
ON CAZ_PAYMENT_AUDIT.logged_actions(action_tstamp);
 
CREATE INDEX IF NOT EXISTS logged_actions_action_idx 
ON CAZ_PAYMENT_AUDIT.logged_actions(action);
 
--
-- Now, define the actual trigger function:
--
CREATE OR REPLACE FUNCTION CAZ_PAYMENT_AUDIT.if_modified_func() RETURNS TRIGGER AS $body$
DECLARE
    v_old_data TEXT;
    v_new_data TEXT;
BEGIN
    /*  If this actually for real auditing (where you need to log EVERY action),
        then you would need to use something like dblink or plperl that could log outside the transaction,
        regardless of whether the transaction committed or rolled back.
    */
 
    /* This dance with casting the NEW and OLD values to a ROW is not necessary in pg 9.0+ */
 
    IF (TG_OP = 'UPDATE') THEN
        v_old_data := ROW(OLD.*);
        v_new_data := ROW(NEW.*);
        INSERT INTO CAZ_PAYMENT_AUDIT.logged_actions (schema_name,table_name,user_name,action,original_data,new_data,query) 
        VALUES (TG_TABLE_SCHEMA::TEXT,TG_TABLE_NAME::TEXT,session_user::TEXT,substring(TG_OP,1,1),v_old_data,v_new_data, current_query());
        RETURN NEW;
    ELSIF (TG_OP = 'DELETE') THEN
        v_old_data := ROW(OLD.*);
        INSERT INTO CAZ_PAYMENT_AUDIT.logged_actions (schema_name,table_name,user_name,action,original_data,query)
        VALUES (TG_TABLE_SCHEMA::TEXT,TG_TABLE_NAME::TEXT,session_user::TEXT,substring(TG_OP,1,1),v_old_data, current_query());
        RETURN OLD;
    ELSIF (TG_OP = 'INSERT') THEN
        v_new_data := ROW(NEW.*);
        INSERT INTO CAZ_PAYMENT_AUDIT.logged_actions (schema_name,table_name,user_name,action,new_data,query)
        VALUES (TG_TABLE_SCHEMA::TEXT,TG_TABLE_NAME::TEXT,session_user::TEXT,substring(TG_OP,1,1),v_new_data, current_query());
        RETURN NEW;
    ELSE
        RAISE WARNING '[CAZ_PAYMENT_AUDIT.IF_MODIFIED_FUNC] - Other action occurred: %, at %',TG_OP,now();
        RETURN NULL;
    END IF;
 
EXCEPTION
    WHEN data_exception THEN
        RAISE WARNING '[CAZ_PAYMENT_AUDIT.IF_MODIFIED_FUNC] - UDF ERROR [DATA EXCEPTION] - SQLSTATE: %, SQLERRM: %',SQLSTATE,SQLERRM;
        RETURN NULL;
    WHEN unique_violation THEN
        RAISE WARNING '[CAZ_PAYMENT_AUDIT.IF_MODIFIED_FUNC] - UDF ERROR [UNIQUE] - SQLSTATE: %, SQLERRM: %',SQLSTATE,SQLERRM;
        RETURN NULL;
    WHEN OTHERS THEN
        RAISE WARNING '[CAZ_PAYMENT_AUDIT.IF_MODIFIED_FUNC] - UDF ERROR [OTHER] - SQLSTATE: %, SQLERRM: %',SQLSTATE,SQLERRM;
        RETURN NULL;
END;
$body$
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = pg_catalog, CAZ_PAYMENT_AUDIT;

-- Attach new trigger events to audit log

-- First drop existing triggers if exist for idempotency
DROP TRIGGER IF EXISTS audit_trigger ON CAZ_PAYMENT.t_payment;
DROP TRIGGER IF EXISTS audit_trigger ON CAZ_PAYMENT.t_clean_air_zone_entrant_payment;
DROP TRIGGER IF EXISTS audit_trigger ON CAZ_PAYMENT.t_clean_air_zone_entrant_payment_match;

CREATE TRIGGER audit_trigger
AFTER INSERT OR UPDATE OR DELETE ON CAZ_PAYMENT.t_payment
FOR EACH ROW EXECUTE PROCEDURE CAZ_PAYMENT_AUDIT.if_modified_func();

CREATE TRIGGER audit_trigger
AFTER INSERT OR UPDATE OR DELETE ON CAZ_PAYMENT.t_clean_air_zone_entrant_payment
FOR EACH ROW EXECUTE PROCEDURE CAZ_PAYMENT_AUDIT.if_modified_func();

CREATE TRIGGER audit_trigger
AFTER INSERT OR UPDATE OR DELETE ON CAZ_PAYMENT.t_clean_air_zone_entrant_payment_match
FOR EACH ROW EXECUTE PROCEDURE CAZ_PAYMENT_AUDIT.if_modified_func();
