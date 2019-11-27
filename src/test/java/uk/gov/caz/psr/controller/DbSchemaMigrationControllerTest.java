package uk.gov.caz.psr.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static uk.gov.caz.psr.util.LiquibaseWrapper.LiquibaseFactory;

import java.sql.SQLException;
import javax.sql.DataSource;

import liquibase.Contexts;
import liquibase.Liquibase;
import liquibase.exception.LiquibaseException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import uk.gov.caz.psr.util.LiquibaseWrapper;

@ExtendWith(MockitoExtension.class)
public class DbSchemaMigrationControllerTest {
  @Mock
  private DataSource dataSource;

  @Mock
  private Liquibase liquibase;

  @Mock
  private LiquibaseFactory liquibaseFactory;

  private LiquibaseWrapper liquibaseWrapper;
  private DbSchemaMigrationControllerApi dbSchemaMigrationControllerApi;

  @BeforeEach
  public void setup() throws LiquibaseException, SQLException {
    liquibaseWrapper = new LiquibaseWrapper(dataSource, liquibaseFactory, "classpath:db/changelog/db.changelog-master.yaml");
    dbSchemaMigrationControllerApi = new DbSchemaMigrationControllerApi(liquibaseWrapper);

    //given
    when(liquibaseFactory.getInstance(any(DataSource.class), any(String.class))).thenReturn(liquibase);
  }

  @Test
  public void shouldReturnOKWhenMigrateDbSchemaSucceeded() throws LiquibaseException {
    //when
    ResponseEntity<Void> response = dbSchemaMigrationControllerApi.migrateDb();

    //then
    verify(liquibase).tag(anyString());
    verify(liquibase).update(any(Contexts.class));
    verify(liquibase).getDatabase();
    verifyNoMoreInteractions(liquibase);
    assertThat(response).isNotNull();
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getBody()).isNull();
  }
  
  @Test
  public void shouldReturnErrorWhenMigrateDbSchemaFailed() throws Exception {
    //given
    doThrow(LiquibaseException.class)
     .when(liquibase)
     .update(any(Contexts.class));
    
    //when
    ResponseEntity<Void> response = dbSchemaMigrationControllerApi.migrateDb();
    
    //then
    verify(liquibase).tag(anyString());
    verify(liquibase).update(any(Contexts.class));
    verify(liquibase).rollback(anyString(), any(Contexts.class));
    verify(liquibase).getDatabase();
    verifyNoMoreInteractions(liquibase);
    assertThat(response).isNotNull();
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
    assertThat(response.getBody()).isNull();
  }
}