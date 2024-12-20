package uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.seed

import org.assertj.core.api.Assertions.assertThat
import org.jetbrains.kotlinx.dataframe.api.emptyDataFrame
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.SeedFileType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.SeedFromExcelFileType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.SeedFromExcelRequest
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.SeedRequest
import java.util.UUID

@TestInstance(TestInstance.Lifecycle.PER_METHOD)
class SeedScaffoldingTest : SeedTestBase() {
  @Test
  fun `Requesting a Seed operation returns 202`() {
    webTestClient.post()
      .uri("/seed")
      .bodyValue(
        SeedRequest(
          seedType = SeedFileType.approvedPremises,
          fileName = "file.csv",
        ),
      )
      .exchange()
      .expectStatus()
      .isAccepted
  }

  @Test
  fun `Requesting a seed from Excel operation returns 202`() {
    webTestClient.post()
      .uri("/seedFromExcel")
      .bodyValue(
        SeedFromExcelRequest(
          seedType = SeedFromExcelFileType.approvedPremisesRoom,
          premisesId = UUID.randomUUID(),
          fileName = "file.xlsx",
        ),
      )
      .exchange()
      .expectStatus()
      .isAccepted
  }

  @Test
  fun `Attempting to process a file containing forward slashes logs an error`() {
    seedService.seedData(SeedFileType.approvedPremises, "/afile")

    assertThat(logEntries).anyMatch {
      it.level == "error" &&
        it.message == "Unable to complete Seed Job" &&
        it.throwable != null &&
        it.throwable.message!!.contains(
          "Filename must be just the filename of a .csv file in the /seed directory, e.g. for /seed/upload.csv, just `upload` should be supplied",
        )
    }
  }

  @Test
  fun `Attempting to process an xlsx file containing forward slashes logs an error`() {
    seedService.seedExcelData(SeedFromExcelFileType.approvedPremisesRoom, UUID.randomUUID(), "/afile")

    assertThat(logEntries).anyMatch {
      it.level == "error" &&
        it.message == "Unable to complete Excel seed job" &&
        it.throwable != null &&
        it.throwable.message!!.contains(
          "Filename must be just the filename of a .xlsx file in the /seed directory, e.g. for /seed/upload.xlsx, just `upload` should be supplied",
        )
    }
  }

  @Test
  fun `Attempting to process a file containing backward slashes logs an error`() {
    seedService.seedData(SeedFileType.approvedPremises, "\\afile")

    assertThat(logEntries).anyMatch {
      it.level == "error" &&
        it.message == "Unable to complete Seed Job" &&
        it.throwable != null &&
        it.throwable.message!!.contains(
          "Filename must be just the filename of a .csv file in the /seed directory, e.g. for /seed/upload.csv, just `upload` should be supplied",
        )
    }
  }

  @Test
  fun `Attempting to process an xlsx file containing backward slashes logs an error`() {
    seedService.seedExcelData(SeedFromExcelFileType.approvedPremisesRoom, UUID.randomUUID(), "\\afile")

    assertThat(logEntries).anyMatch {
      it.level == "error" &&
        it.message == "Unable to complete Excel seed job" &&
        it.throwable != null &&
        it.throwable.message!!.contains(
          "Filename must be just the filename of a .xlsx file in the /seed directory, e.g. for /seed/upload.xlsx, just `upload` should be supplied",
        )
    }
  }

  @Test
  fun `Attempting to process a non-existent file logs an error`() {
    seedService.seedData(SeedFileType.approvedPremises, "non-existent")

    assertThat(logEntries).anyMatch {
      it.level == "error" &&
        it.message == "Unable to complete Seed Job" &&
        it.throwable != null &&
        it.throwable.message!!.contains(
          "There was an issue opening the CSV file",
        )
    }
  }

  @Test
  fun `Attempting to process a non-existent xlsx file logs an error`() {
    seedService.seedExcelData(SeedFromExcelFileType.approvedPremisesRoom, UUID.randomUUID(), "non-existent")

    assertThat(logEntries).anyMatch {
      it.level == "error" &&
        it.message == "Unable to complete Excel seed job" &&
        it.throwable != null &&
        it.throwable.message!!.contains(
          "Unable to process XLSX file",
        )
    }
  }

  @Test
  fun `Attempting to process a malformed file logs an error`() {
    withCsv(
      "malformed",
      """
deliusUsername,roles,qualifications
RogerSmith,CAS1_FUTURE_MANAGER,
,
      """.trimIndent(),
    )

    seedService.seedData(SeedFileType.user, "malformed.csv")

    assertThat(logEntries).anyMatch {
      it.level == "error" &&
        it.message == "Unable to complete Seed Job" &&
        it.throwable != null &&
        it.throwable.cause!!.message == "Fields num seems to be 3 on each row, but on 2th csv row, fields num is 2."
    }
  }

  @Test
  fun `Attempting to process an xlsx file without Sheet3 logs an error`() {
    withXlsx(
      "wrongSheetName",
      "wrongSheetName",
      emptyDataFrame<Any>(),
    )

    seedService.seedExcelData(SeedFromExcelFileType.approvedPremisesRoom, UUID.randomUUID(), "wrongSheetName.xlsx")

    assertThat(logEntries).anyMatch {
      it.level == "error" &&
        it.message == "Unable to complete Excel seed job" &&
        it.throwable != null &&
        it.throwable.cause!!.message == "Sheet with name Sheet3 not found"
    }
  }
}
