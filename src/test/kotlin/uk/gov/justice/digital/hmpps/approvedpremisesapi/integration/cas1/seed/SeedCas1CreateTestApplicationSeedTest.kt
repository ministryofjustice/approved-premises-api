package uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.cas1.seed

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.SeedFileType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.NeedsDetailsFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenAUser
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenAnOffender
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.httpmocks.apDeliusContextMockSuccessfulTeamsManagingCaseCall
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.httpmocks.apOASysContextMockSuccessfulNeedsDetailsCall
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.seed.SeedTestBase
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.deliuscontext.ManagingTeamsResponse
import uk.gov.justice.digital.hmpps.approvedpremisesapi.seed.CsvBuilder
import uk.gov.justice.digital.hmpps.approvedpremisesapi.seed.cas1.Cas1CreateTestApplicationsSeedCsvRow

class SeedCas1CreateTestApplicationSeedTest : SeedTestBase() {

  @Test
  fun `Create test application`() {
    val (user) = givenAUser()
    val (offender) = givenAnOffender()

    val crn = offender.otherIds.crn

    apDeliusContextMockSuccessfulTeamsManagingCaseCall(crn, ManagingTeamsResponse(teamCodes = listOf("TEAM1")))
    apOASysContextMockSuccessfulNeedsDetailsCall(crn, NeedsDetailsFactory().produce())

    withCsv(
      csvName = "valid-csv",
      contents = listOf(
        Cas1CreateTestApplicationsSeedCsvRow(
          creatorUsername = user.deliusUsername,
          crn = crn,
          count = 1,
        ),
      ).toCsv(),
    )

    seedService.seedData(SeedFileType.approvedPremisesCreateTestApplications, "valid-csv.csv")

    val applications = approvedPremisesApplicationRepository.findAll()
    assertThat(applications).hasSize(1)
  }

  private fun List<Cas1CreateTestApplicationsSeedCsvRow>.toCsv(): String {
    val builder = CsvBuilder()
      .withUnquotedFields(
        "creator_username",
        "crn",
        "count",
      )
      .newRow()

    this.forEach {
      builder
        .withQuotedField(it.creatorUsername)
        .withQuotedField(it.crn)
        .withQuotedField(it.count)
        .newRow()
    }

    return builder.build()
  }
}
