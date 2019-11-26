package uk.gov.caz.psr.util;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.UUID;

import javax.sql.DataSource;

import liquibase.Contexts;
import liquibase.Liquibase;
import liquibase.database.Database;
import liquibase.database.DatabaseFactory;
import liquibase.database.jvm.JdbcConnection;
import liquibase.exception.LiquibaseException;
import liquibase.resource.ClassLoaderResourceAccessor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class LiquibaseWrapper {
  @Autowired
  private DataSource dataSource;

  @Value("${spring.liquibase.change-log:classpath:db/changelog/db.changelog-master.yaml}")
  private String liquibaseChangelog;

  /**
   * Update db schema.
   * @throws SQLException when unable to connect to DB
   * @throws LiquibaseException when fail to perform DB upgrade
   */
  public void update() throws LiquibaseException, SQLException {
    String changelog = liquibaseChangelog.startsWith("classpath")
                        ? liquibaseChangelog.substring(10) : liquibaseChangelog;
    String dbTag = UUID.randomUUID().toString();
    Contexts contexts = new Contexts();
    try (Connection connection = dataSource.getConnection()) {
      Database database = DatabaseFactory.getInstance()
                            .findCorrectDatabaseImplementation(new JdbcConnection(connection));
      Liquibase liquibase = new Liquibase(changelog,
                              new ClassLoaderResourceAccessor(),
                              database);
      // tag DB for future rollback when db update fail
      liquibase.tag(dbTag);
      try {
        liquibase.update(contexts);
      } catch (LiquibaseException lqbEx) {
        log.error(lqbEx.getMessage());
        log.info("Attempting to rollback db update");
        liquibase.rollback(dbTag, contexts);
        throw lqbEx;
      }
    }
  }
}