package uk.gov.caz.psr.service.audit;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.jdbc.Sql.ExecutionPhase;
import org.springframework.test.jdbc.JdbcTestUtils;

import uk.gov.caz.psr.annotation.IntegrationTest;

@IntegrationTest
@Sql(scripts = "classpath:data/sql/clear-data-and-drop-fk-constraint.sql", executionPhase = ExecutionPhase.BEFORE_TEST_METHOD)
@Sql(scripts = "classpath:data/sql/re-add-fk-constraint-to-payment-audit-tables.sql", executionPhase = ExecutionPhase.AFTER_TEST_METHOD)
public class PaymentDataCleanupServiceTestIT {
  @Autowired
  private PaymentDataCleanupService service;

  @Autowired
  private JdbcTemplate jdbcTemplate;

  @Test
  public void shouldCleanupOldData() {
    //given
    auditTablesShouldBeEmpty();

    //when
    insert18MonthsOldAuditData();
     //then
    checkIfTableContainsNumberOfRows(1,"caz_payment_audit.t_clean_air_zone_payment_detail");
    checkIfTableContainsNumberOfRows(1,"caz_payment_audit.t_clean_air_zone_payment_master");
    checkIfTableContainsNumberOfRows(1,"caz_payment_audit.logged_actions");

    //when
    service.cleanupData();
    //then
    checkIfTableContainsNumberOfRows(0,"caz_payment_audit.t_clean_air_zone_payment_detail");
    checkIfTableContainsNumberOfRows(0,"caz_payment_audit.t_clean_air_zone_payment_master");
    checkIfTableContainsNumberOfRows(0,"caz_payment_audit.logged_actions");
  }

  @Test
  public void shouldLeave12MonthsOldData() {
    //given
    auditTablesShouldBeEmpty();

    //when
    whenWeInsertSomeSampleAuditData();
     //then
    checkIfTableContainsNumberOfRows(2,"caz_payment_audit.t_clean_air_zone_payment_detail");
    checkIfTableContainsNumberOfRows(1,"caz_payment_audit.t_clean_air_zone_payment_master");
    checkIfTableContainsNumberOfRows(1,"caz_payment_audit.logged_actions");

    //when
    service.cleanupData();
    //then
    checkIfTableContainsNumberOfRows(1,"caz_payment_audit.t_clean_air_zone_payment_detail");
    checkIfTableContainsNumberOfRows(1,"caz_payment_audit.t_clean_air_zone_payment_master");
    checkIfTableContainsNumberOfRows(0,"caz_payment_audit.logged_actions");
  }

  private void auditTablesShouldBeEmpty() {
    checkIfTableContainsNumberOfRows(0,"caz_payment_audit.t_clean_air_zone_payment_detail");
    checkIfTableContainsNumberOfRows(0,"caz_payment_audit.t_clean_air_zone_payment_master");
    checkIfTableContainsNumberOfRows(0,"caz_payment_audit.logged_actions");
  }

  private void checkIfTableContainsNumberOfRows(int expectedNumberOfRows, String tableName) {
    int numberOfRowsInAuditTable =
        JdbcTestUtils.countRowsInTable(jdbcTemplate, tableName);
    assertThat(numberOfRowsInAuditTable)
        .as("Expected %s row(s) in " + tableName + " table",
            expectedNumberOfRows)
        .isEqualTo(expectedNumberOfRows);
  }

  private void whenWeInsertSomeSampleAuditData() {
    insert18MonthsOldAuditData();
    insertOneRecordToPaymentDetailTable(12);
  }
  
  private void insert18MonthsOldAuditData() {
    insertOneRecordToPaymentAuditTable();
    insertOneRecordToLoggedActionsTable();
  }

  private void insertOneRecordToPaymentAuditTable() {
    insertOneRecordToPaymentMasterTable();
    insertOneRecordToPaymentDetailTable(18);
  }

  private void insertOneRecordToPaymentMasterTable() {
    String sql = "INSERT INTO caz_payment_audit.t_clean_air_zone_payment_master (clean_air_zone_payment_master_id,vrn,clean_air_zone_id,inserttimestamp) "
        + "VALUES ('ce3bbb9a-9bf9-11ea-bb37-0242ac130002','CAS310','fee86f08-9c04-11ea-bb37-0242ac130002',current_timestamp - '18 MONTH'::interval)";
    jdbcTemplate.update(sql);
  }

  private void insertOneRecordToPaymentDetailTable(int age) {
    String id = UUID.randomUUID().toString();
    String sql = String.format("INSERT INTO caz_payment_audit.t_clean_air_zone_payment_detail (t_clean_air_zone_payment_detail_id,clean_air_zone_payment_master_id,"
        + "travel_date,payment_status,tariff_code,charge,case_reference,update_actor,entrant_payment_insert_timestamp,entrant_payment_update_timestamp,"
        + "payment_id,central_reference_number,payment_insert_timestamp,payment_update_timestamp,payment_provider_id,payment_provider_status,latest,inserttimestamp) "
        + "VALUES ('%s','ce3bbb9a-9bf9-11ea-bb37-0242ac130002',current_date,'payment_status','tariff_code',50,'case_reference',"
        + "'update_actor',current_date,current_date,'4aa74d14-9c06-11ea-bb37-0242ac130002',1000000,current_date,current_date,'payment_provider_id',"
        + "'payment_provider_status',true,current_timestamp - '%d MONTH'::interval)", id, age);
    jdbcTemplate.update(sql);
  }

  private void insertOneRecordToLoggedActionsTable() {
    String sql = "INSERT INTO caz_payment_audit.logged_actions (schema_name,table_name,user_name,action,original_data,new_data,query,action_tstamp) "
        + "VALUES ('TG_TABLE_SCHEMA','TG_TABLE_NAME','session_user','I','original_data','new_data', 'current_query', current_timestamp - '18 MONTH'::interval)";
    jdbcTemplate.update(sql);
  }
}