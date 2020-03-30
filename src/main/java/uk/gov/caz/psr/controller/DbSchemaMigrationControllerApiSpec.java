package uk.gov.caz.psr.controller;

import static uk.gov.caz.psr.controller.DbSchemaMigrationControllerApi.PATH;

import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * Interface with swagger documentation for DbSchemaMigrationController.
 */
@RequestMapping(value = PATH, produces = {
    MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE})
public interface DbSchemaMigrationControllerApiSpec {
  /**
   * Trigger DB schema migration.
   */
  @ApiOperation(value = "${swagger.operations.db.migration.description:Evolve DB schema}",
      response = Void.class)
  @ApiResponses({
      @ApiResponse(code = 500,
          message = "Internal Server Error / Migration failed"),
      @ApiResponse(code = 200, message = "DB Schema migration succeeded"),})
  @PostMapping
  ResponseEntity<Void> migrateDb();
}