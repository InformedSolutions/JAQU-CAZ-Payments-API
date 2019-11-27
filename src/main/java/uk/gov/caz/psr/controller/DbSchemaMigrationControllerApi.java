package uk.gov.caz.psr.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import uk.gov.caz.psr.util.LiquibaseWrapper;

@RestController
public class DbSchemaMigrationControllerApi implements DbSchemaMigrationControllerApiSpec {
  public static final String PATH = "/v1/migrate-db";

  private final LiquibaseWrapper liquibaseWrapper;
  
  /**
   * Creates an instance of {@link DbSchemaMigrationControllerApi}.
   *
   * @param liquibaseWrapper An instance of {@link LiquibaseWrapper}.
   */
  public DbSchemaMigrationControllerApi(LiquibaseWrapper liquibaseWrapper) {
    this.liquibaseWrapper = liquibaseWrapper;
  }

  @Override
  public ResponseEntity<Void> migrateDb() {
    try {
      liquibaseWrapper.update();
      return ResponseEntity
              .status(HttpStatus.OK)
              .body(null);
    } catch (Exception ex) {
      return ResponseEntity
              .status(HttpStatus.INTERNAL_SERVER_ERROR)
              .body(null);
    }
  }
}