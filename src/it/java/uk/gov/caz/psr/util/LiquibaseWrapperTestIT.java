package uk.gov.caz.psr.util;

import java.sql.SQLException;

import liquibase.exception.LiquibaseException;

import org.junit.Assert;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import uk.gov.caz.psr.annotation.IntegrationTest;

@IntegrationTest
public class LiquibaseWrapperTestIT {

  @Autowired
  private LiquibaseWrapper liquibaseWrapper;
  
  @Test
  void canApplyLiquibaseUpdatesUsingWrapperUtility() {
    try {
      liquibaseWrapper.update();
    } catch (LiquibaseException e) {
      Assert.fail();
    } catch (SQLException e) {
      Assert.fail();
    }
  }
}