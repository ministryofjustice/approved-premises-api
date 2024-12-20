package uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.transformer

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ActiveOffence
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.CaseDetailFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.CaseDetailOffenceFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.OffenceTransformer
import java.time.LocalDate

class OffenceTransformerTest {
  private val offenceTransformer = OffenceTransformer()

  @Test
  fun `transformToApi from case-detail maps each Offence correctly`() {
    val caseDetail = CaseDetailFactory()
      .withOffences(
        listOf(
          CaseDetailOffenceFactory()
            .withId("M10")
            .withDescription("A first offence")
            .withMainCategoryDescription("Main Category 1")
            .withSubCategoryDescription("Sub Category 1")
            .withDate(LocalDate.parse("2024-11-01"))
            .withEventNumber("20")
            .withEventId(30)
            .produce(),
          CaseDetailOffenceFactory()
            .withId("A100")
            .withDescription("A second offence")
            .withMainCategoryDescription("Main Category 2")
            .withSubCategoryDescription("Sub Category 2")
            .withDate(LocalDate.parse("2024-11-11"))
            .withEventNumber("200")
            .withEventId(300)
            .produce(),
        ),
      )
      .produce()

    assertThat(offenceTransformer.transformToApi(caseDetail)).containsExactlyInAnyOrder(
      ActiveOffence(
        deliusEventNumber = "20",
        offenceDescription = "Main Category 1 - Sub Category 1",
        offenceId = "M10",
        convictionId = 30,
        offenceDate = LocalDate.parse("2024-11-01"),
      ),
      ActiveOffence(
        deliusEventNumber = "200",
        offenceDescription = "Main Category 2 - Sub Category 2",
        offenceId = "A100",
        convictionId = 300,
        offenceDate = LocalDate.parse("2024-11-11"),
      ),
    )
  }

  @Test
  fun `transformToApi from case-detail omits sub category description where it is the same as main category description`() {
    val caseDetail = CaseDetailFactory()
      .withOffences(
        listOf(
          CaseDetailOffenceFactory()
            .withId("M10")
            .withDescription("A first offence")
            .withMainCategoryDescription("A Description")
            .withSubCategoryDescription("A Description")
            .withDate(LocalDate.parse("2024-11-01"))
            .withEventNumber("20")
            .withEventId(30)
            .produce(),
        ),
      )
      .produce()

    assertThat(offenceTransformer.transformToApi(caseDetail)).containsExactlyInAnyOrder(
      ActiveOffence(
        deliusEventNumber = "20",
        offenceDescription = "A Description",
        offenceId = "M10",
        convictionId = 30,
        offenceDate = LocalDate.parse("2024-11-01"),
      ),
    )
  }
}
