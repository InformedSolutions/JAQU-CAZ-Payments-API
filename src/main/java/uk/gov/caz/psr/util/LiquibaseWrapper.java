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
import liquibase.exception.DatabaseException;
import liquibase.exception.LiquibaseException;
import liquibase.resource.ClassLoaderResourceAccessor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class LiquibaseWrapper {
  
  private final DataSource dataSource;
  private final LiquibaseFactory liquibaseFactory;
  private final String liquibaseChangelog;

  /**
   * Default constructor.
   * @param dataSource liquibase data source.
   */
  public LiquibaseWrapper(DataSource dataSource,
        LiquibaseFactory liquibaseFactory,
        @Value("${spring.liquibase.change-log:db/changelog/db.changelog-master.yaml}")
        String liquibaseChangelog) {
    this.dataSource = dataSource;
    this.liquibaseFactory = liquibaseFactory;
    this.liquibaseChangelog = liquibaseChangelog;
  }

  /**
   * Update db schema.
   * @throws SQLException when unable to connect to DB
   * @throws LiquibaseException when fail to perform DB upgrade
   */
  public void update() throws LiquibaseException, SQLException {
    String changeLog = liquibaseChangelog.startsWith("classpath")
                        ? liquibaseChangelog.substring(10) : liquibaseChangelog;
    String dbTag = UUID.randomUUID().toString();
    Contexts contexts = new Contexts();
    boolean isDbBeingUpdated = false;
    Liquibase liquibase = null;
   
    try {
      liquibase = liquibaseFactory.getInstance(dataSource, changeLog);
      // tag DB for future rollback when db update fail
      liquibase.tag(dbTag);
      isDbBeingUpdated = true;
      liquibase.update(contexts);
    } catch (LiquibaseException | SQLException ex) {
      log.error(ex.getMessage());
      if (isDbBeingUpdated) {
        log.info("Attempting to rollback db update");
        if (liquibase != null) {
          liquibase.rollback(dbTag, contexts);
        }
      }
      throw ex;
    } finally {
      if (liquibase != null && liquibase.getDatabase() != null) {
        liquibase.getDatabase().close();
      }
    }
  }

  @Component
  public static class LiquibaseFactory {

    /**
     * Create an instance of {@link Liquibase}.
     *
     * @param dataSource database that liquibase will connnect to
     * @param changeLog  where the log files are located.
     * @throws SQLException when unable to initiate a Liquibase instance
     * @throws DatabaseException when unable to initiate a Liquibase instance
     */
    public Liquibase getInstance(DataSource dataSource, String changeLog)
        throws SQLException, DatabaseException {
      Connection connection = dataSource.getConnection();
      Database database = DatabaseFactory.getInstance()
                              .findCorrectDatabaseImplementation(new JdbcConnection(connection));
      Liquibase liquibase = new Liquibase(changeLog,
                                new ClassLoaderResourceAccessor(),
                                database);
      return liquibase;
    }
  }
}