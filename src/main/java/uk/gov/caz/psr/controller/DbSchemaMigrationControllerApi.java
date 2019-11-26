package uk.gov.caz.psr.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import uk.gov.caz.psr.util.LiquibaseWrapper;

@RestController
public class DbSchemaMigrationControllerApi implements DbSchemaMigrationControllerApiSpec {
  public static final String PATH = "/v1/migrate-db";

  @Autowired
  LiquibaseWrapper liquibaseWrapper;

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