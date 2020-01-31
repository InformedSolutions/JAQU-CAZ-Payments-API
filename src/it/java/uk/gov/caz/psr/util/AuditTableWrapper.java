package uk.gov.caz.psr.util;

import lombok.Builder;

@Builder
public class AuditTableWrapper {

  // table references
  public final static String DETAIL = "caz_payment_audit.t_clean_air_zone_payment_detail";
  public final static String MASTER = "caz_payment_audit.t_clean_air_zone_payment_master";
  
  // field references
  public final static String MASTER_ID = "clean_air_zone_payment_master_id";
  
  // sql statements
  public final static String MASTER_ID_SQL = "SELECT clean_air_zone_payment_master_id "
      + "FROM caz_payment_audit.t_clean_air_zone_payment_master "
      + "WHERE vrn = ? AND clean_air_zone_id = ?";
    
}
