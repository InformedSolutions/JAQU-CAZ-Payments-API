package uk.gov.caz.psr.controller;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

class ExampleControllerTest {

  private ExampleController exampleController = new ExampleController();

  @Test
  public void shouldReturnNonNullEntity() {
    // given
    String someId = "some id";

    // when
    ExampleController.Entity entity = exampleController.getById(someId);

    // then
    assertThat(entity).isNotNull();
  }

  @ParameterizedTest
  @ValueSource(strings = {"id1", "id2", "id3"})
  public void shouldReturnEntityWithFixedName(String id) {
    // given param "id"

    // when
    ExampleController.Entity entity = exampleController.getById(id);

    // then
    assertThat(entity.getName()).isEqualTo(ExampleController.NAME);
  }

  @Test
  public void shouldReturnEntityWithPassedId() {
    // given
    String id = "1";

    // when
    ExampleController.Entity entity = exampleController.getById(id);

    // then
    assertThat(entity.getId()).isEqualTo(id);
  }
}