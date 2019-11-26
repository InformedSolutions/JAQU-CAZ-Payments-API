package uk.gov.caz.psr.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doThrow;

import liquibase.exception.LiquibaseException;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import uk.gov.caz.psr.util.LiquibaseWrapper;

@ExtendWith(MockitoExtension.class)
public class DbSchemaMigrationControllerTest {
  @Mock
  private LiquibaseWrapper liquibaseWrapper;

  @InjectMocks
  private DbSchemaMigrationControllerApi dbSchemaMigrationControllerApi;

  @Test
  public void shouldReturnOKWhenMigrateDbSchemaSucceeded() {
    //when
    ResponseEntity<Void> response = dbSchemaMigrationControllerApi.migrateDb();
    
    assertThat(response).isNotNull();
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getBody()).isNull();
  }
  
  @Test
  public void shouldReturnErrorWhenMigrateDbSchemaFailed() throws Exception {
    //given
    doThrow(LiquibaseException.class)
     .when(liquibaseWrapper)
     .update();
    
    //when
    ResponseEntity<Void> response = dbSchemaMigrationControllerApi.migrateDb();
    
    assertThat(response).isNotNull();
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
    assertThat(response.getBody()).isNull();
  }
}