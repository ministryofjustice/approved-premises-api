package uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.cas1.seed

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.springframework.beans.factory.annotation.Autowired
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.SeedFileType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.seed.SeedTestBase
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.cas1.Cas1DeliusBookingImportRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.seed.CsvBuilder
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.UUID

@TestInstance(TestInstance.Lifecycle.PER_METHOD)
class SeedCas1ImportDeliusBookingDataSeedJobTest : SeedTestBase() {

  @Autowired
  lateinit var cas1DeliusBookingImportRepository: Cas1DeliusBookingImportRepository

  @Test
  fun `Row with only mandatory fields can be processed`() {
    val bookingId = UUID.randomUUID()

    withCsv(
      csvName = "valid-csv",
      contents = listOf(
        Cas1DeliusBookingManagementDataRowRaw(
          bookingId = bookingId.toString(),
          crn = "CRN1",
          eventNumber = "1",
          expectedArrivalDate = "2024-06-15 00:00:00",
        ),
      ).toCsv(),
    )

    seedService.seedData(SeedFileType.approvedPremisesImportDeliusBookingManagementData, "valid-csv.csv")

    val bookingImport = cas1DeliusBookingImportRepository.findAll()[0]

    assertThat(bookingImport.bookingId).isEqualTo(bookingId)
    assertThat(bookingImport.crn).isEqualTo("CRN1")
    assertThat(bookingImport.eventNumber).isEqualTo("1")
    assertThat(bookingImport.keyWorkerStaffCode).isNull()
    assertThat(bookingImport.keyWorkerForename).isNull()
    assertThat(bookingImport.keyWorkerMiddleName).isNull()
    assertThat(bookingImport.keyWorkerSurname).isNull()
    assertThat(bookingImport.departureReasonCode).isNull()
    assertThat(bookingImport.moveOnCategoryCode).isNull()
    assertThat(bookingImport.moveOnCategoryDescription).isNull()
    assertThat(bookingImport.expectedArrivalDate).isEqualTo(LocalDate.of(2024, 6, 15))
    assertThat(bookingImport.arrivalDate).isNull()
    assertThat(bookingImport.expectedDepartureDate).isNull()
    assertThat(bookingImport.departureDate).isNull()
    assertThat(bookingImport.nonArrivalDate).isNull()
    assertThat(bookingImport.nonArrivalContactDatetime).isNull()
    assertThat(bookingImport.nonArrivalReasonCode).isNull()
    assertThat(bookingImport.nonArrivalReasonDescription).isNull()
    assertThat(bookingImport.nonArrivalNotes).isNull()
  }

  @Test
  fun `Row with all available fields can be processed`() {
    val bookingId = UUID.randomUUID()

    withCsv(
      csvName = "valid-csv",
      contents = listOf(
        Cas1DeliusBookingManagementDataRowRaw(
          bookingId = bookingId.toString(),
          crn = "CRN2",
          eventNumber = "2",
          keyWorkerStaffCode = "kw1",
          keyWorkerForename = "kwForename",
          keyWorkerMiddleName = "kwMiddle",
          keyWorkerSurname = "kwSurname",
          departureReasonCode = "drc",
          moveOnCategoryCode = "mocc",
          moveOnCategoryDescription = "moccDescription",
          expectedArrivalDate = "2024-06-16 00:00:00",
          arrivalDate = "2024-06-17 00:00:00",
          expectedDepartureDate = "2025-09-13 00:00:00",
          departureDate = "2025-09-12 00:00:00",
          nonArrivalDate = "2021-01-01 00:00:00",
          nonArrivalContactDateTimeList = "2023-07-17 00:00:00,2023-07-17 11:36:02",
          nonArrivalReasonCode = "non arrival reason code",
          nonArrivalReasonDescription = "non arrival reason description",
          nonArrivalNotes = "non arrival notes",
        ),
      ).toCsv(),
    )

    seedService.seedData(SeedFileType.approvedPremisesImportDeliusBookingManagementData, "valid-csv.csv")

    val bookingImport = cas1DeliusBookingImportRepository.findAll()[0]

    assertThat(bookingImport.bookingId).isEqualTo(bookingId)
    assertThat(bookingImport.crn).isEqualTo("CRN2")
    assertThat(bookingImport.eventNumber).isEqualTo("2")
    assertThat(bookingImport.keyWorkerStaffCode).isEqualTo("kw1")
    assertThat(bookingImport.keyWorkerForename).isEqualTo("kwForename")
    assertThat(bookingImport.keyWorkerMiddleName).isEqualTo("kwMiddle")
    assertThat(bookingImport.keyWorkerSurname).isEqualTo("kwSurname")
    assertThat(bookingImport.departureReasonCode).isEqualTo("drc")
    assertThat(bookingImport.moveOnCategoryCode).isEqualTo("mocc")
    assertThat(bookingImport.moveOnCategoryDescription).isEqualTo("moccDescription")
    assertThat(bookingImport.expectedArrivalDate).isEqualTo(LocalDate.of(2024, 6, 16))
    assertThat(bookingImport.arrivalDate).isEqualTo(LocalDate.of(2024, 6, 17))
    assertThat(bookingImport.expectedDepartureDate).isEqualTo(LocalDate.of(2025, 9, 13))
    assertThat(bookingImport.departureDate).isEqualTo(LocalDate.of(2025, 9, 12))
    assertThat(bookingImport.nonArrivalDate).isEqualTo(LocalDate.of(2021, 1, 1))
    assertThat(bookingImport.nonArrivalContactDatetime).isEqualTo(OffsetDateTime.parse("2023-07-17T11:36:02+01:00"))
    assertThat(bookingImport.nonArrivalReasonCode).isEqualTo("non arrival reason code")
    assertThat(bookingImport.nonArrivalReasonDescription).isEqualTo("non arrival reason description")
    assertThat(bookingImport.nonArrivalNotes).isEqualTo("non arrival notes")
  }

  private fun List<Cas1DeliusBookingManagementDataRowRaw>.toCsv(): String {
    val builder = CsvBuilder()
      .withUnquotedFields(
        "BOOKING_ID",
        "CRN",
        "EVENT_NUMBER",
        "KEY_WORKER_STAFF_CODE",
        "KEY_WORKER_FORENAME",
        "KEY_WORKER_MIDDLE_NAME",
        "KEY_WORKER_SURNAME",
        "DEPARTURE_REASON_CODE",
        "MOVE_ON_CATEGORY_CODE",
        "MOVE_ON_CATEGORY_DESCRIPTION",
        "EXPECTED_ARRIVAL_DATE",
        "ARRIVAL_DATE",
        "EXPECTED_DEPARTURE_DATE",
        "DEPARTURE_DATE",
        "NON_ARRIVAL_DATE",
        "NON_ARRIVAL_CONTACT_DATETIME_LIST",
        "NON_ARRIVAL_REASON_CODE",
        "NON_ARRIVAL_REASON_DESCRIPTION",
        "NON_ARRIVAL_NOTES",
      )
      .newRow()

    this.forEach {
      builder
        .withQuotedField(it.bookingId)
        .withQuotedField(it.crn)
        .withQuotedField(it.eventNumber)
        .withQuotedField(it.keyWorkerStaffCode)
        .withQuotedField(it.keyWorkerForename)
        .withQuotedField(it.keyWorkerMiddleName)
        .withQuotedField(it.keyWorkerSurname)
        .withQuotedField(it.departureReasonCode)
        .withQuotedField(it.moveOnCategoryCode)
        .withQuotedField(it.moveOnCategoryDescription)
        .withQuotedField(it.expectedArrivalDate)
        .withQuotedField(it.arrivalDate)
        .withQuotedField(it.expectedDepartureDate)
        .withQuotedField(it.departureDate)
        .withQuotedField(it.nonArrivalDate)
        .withQuotedField(it.nonArrivalContactDateTimeList)
        .withQuotedField(it.nonArrivalReasonCode)
        .withQuotedField(it.nonArrivalReasonDescription)
        .withQuotedField(it.nonArrivalNotes)
        .newRow()
    }

    return builder.build()
  }

  data class Cas1DeliusBookingManagementDataRowRaw(
    val bookingId: String = "",
    val crn: String = "",
    val eventNumber: String = "",
    val keyWorkerStaffCode: String = "",
    val keyWorkerForename: String = "",
    val keyWorkerMiddleName: String = "",
    val keyWorkerSurname: String = "",
    val departureReasonCode: String = "",
    val moveOnCategoryCode: String = "",
    val moveOnCategoryDescription: String = "",
    val expectedArrivalDate: String = "",
    val arrivalDate: String = "",
    val expectedDepartureDate: String = "",
    val departureDate: String = "",
    val nonArrivalDate: String = "",
    val nonArrivalContactDateTimeList: String = "",
    val nonArrivalReasonCode: String = "",
    val nonArrivalReasonDescription: String = "",
    val nonArrivalNotes: String = "",
  )
}
