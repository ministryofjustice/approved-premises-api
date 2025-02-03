package uk.gov.justice.digital.hmpps.approvedpremisesapi.seed.cas1

import org.slf4j.LoggerFactory
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.cas1.Cas1DeliusBookingImportEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.cas1.Cas1DeliusBookingImportRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.seed.SeedColumns
import uk.gov.justice.digital.hmpps.approvedpremisesapi.seed.SeedJob
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.UUID

@Component
class Cas1ImportDeliusReferralsSeedJob(
  private val jdbcTemplate: NamedParameterJdbcTemplate,
  private val cas1DeliusBookingImportRepository: Cas1DeliusBookingImportRepository,
) : SeedJob<Cas1DeliusBookingManagementDataRow>(
  requiredHeaders = setOf(
    "BOOKING_ID",
    "APPROVED_PREMISES_REFERRAL_ID",
    "CRN",
    "EVENT_NUMBER",
    "KEY_WORKER_STAFF_CODE",
    "KEY_WORKER_FORENAME",
    "KEY_WORKER_MIDDLE_NAME",
    "KEY_WORKER_SURNAME",
    "DECISION_CODE",
    "DECISION_DESCRIPTION",
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
    "HOSTEL_CODE",
  ),
  runInTransaction = false,
  processRowsConcurrently = true,
) {
  private val log = LoggerFactory.getLogger(this::class.java)

  private companion object {
    val DELIUS_IMPORT_DATE_TIME_FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern("uuuu-MM-dd HH:mm:ss")
  }

  override fun preSeed() {
    log.info("Clearing table cas1_delius_booking_import before seeding new data")
    jdbcTemplate.update("TRUNCATE cas1_delius_booking_import", emptyMap<String, String>())
  }

  override fun deserializeRow(columns: Map<String, String>): Cas1DeliusBookingManagementDataRow {
    val seedColumns = SeedColumns(columns)

    return Cas1DeliusBookingManagementDataRow(
      bookingId = seedColumns.getUuidOrNull("BOOKING_ID"),
      approvedPremisesReferralId = seedColumns.getStringOrNull("APPROVED_PREMISES_REFERRAL_ID")!!,
      crn = seedColumns.getStringOrNull("CRN")!!,
      eventNumber = seedColumns.getStringOrNull("EVENT_NUMBER")!!,
      keyWorkerStaffCode = seedColumns.getStringOrNullMinus1IsNull("KEY_WORKER_STAFF_CODE"),
      keyWorkerForename = seedColumns.getStringOrNull("KEY_WORKER_FORENAME"),
      keyWorkerMiddleName = seedColumns.getStringOrNull("KEY_WORKER_MIDDLE_NAME"),
      keyWorkerSurname = seedColumns.getStringOrNull("KEY_WORKER_SURNAME"),
      decisionCode = seedColumns.getStringOrNull("DECISION_CODE")!!,
      decisionDescription = seedColumns.getStringOrNull("DECISION_DESCRIPTION")!!,
      departureReasonCode = seedColumns.getStringOrNull("DEPARTURE_REASON_CODE"),
      moveOnCategoryCode = seedColumns.getStringOrNullMinus1IsNull("MOVE_ON_CATEGORY_CODE"),
      moveOnCategoryDescription = seedColumns.getStringOrNull("MOVE_ON_CATEGORY_DESCRIPTION"),
      expectedArrivalDate = seedColumns.getDateFromUtcDateTimeOrNull1900IsNull("EXPECTED_ARRIVAL_DATE"),
      arrivalDate = seedColumns.getDateFromUtcDateTimeOrNull1900IsNull("ARRIVAL_DATE"),
      expectedDepartureDate = seedColumns.getDateFromUtcDateTimeOrNull1900IsNull("EXPECTED_DEPARTURE_DATE"),
      departureDate = seedColumns.getDateFromUtcDateTimeOrNull1900IsNull("DEPARTURE_DATE"),
      nonArrivalDate = seedColumns.getDateFromUtcDateTimeOrNull1900IsNull("NON_ARRIVAL_DATE"),
      nonArrivalContactDateTime = seedColumns.getLastDateTimeFromListOrNull("NON_ARRIVAL_CONTACT_DATETIME_LIST", DELIUS_IMPORT_DATE_TIME_FORMATTER),
      nonArrivalReasonCode = seedColumns.getStringOrNullMinus1IsNull("NON_ARRIVAL_REASON_CODE"),
      nonArrivalReasonDescription = seedColumns.getStringOrNull("NON_ARRIVAL_REASON_DESCRIPTION"),
      nonArrivalNotes = seedColumns.getStringOrNull("NON_ARRIVAL_NOTES"),
      premisesQCode = seedColumns.getStringOrNull("HOSTEL_CODE")!!,
      createdAt = seedColumns.getDateTimeFromUtcDateTimeOrNull("CREATED_DATETIME", DELIUS_IMPORT_DATE_TIME_FORMATTER),
    )
  }

  @SuppressWarnings("MagicNumber")
  private fun SeedColumns.getDateFromUtcDateTimeOrNull1900IsNull(label: String): LocalDate? {
    val date = getDateFromUtcDateTimeOrNull(label)
    if (date?.year == 1900) {
      return null
    }
    return date
  }

  private fun SeedColumns.getStringOrNullMinus1IsNull(label: String): String? {
    val value = getStringOrNull(label)
    return if (value == "-1") null else value
  }

  override fun processRow(row: Cas1DeliusBookingManagementDataRow) {
    if (row.expectedArrivalDate == null) {
      return
    }

    if (!isAccepted(row)) {
      return
    }

    cas1DeliusBookingImportRepository.save(
      Cas1DeliusBookingImportEntity(
        id = UUID.randomUUID(),
        row.bookingId,
        row.approvedPremisesReferralId,
        row.crn,
        row.eventNumber,
        row.keyWorkerStaffCode,
        row.keyWorkerForename,
        row.keyWorkerMiddleName,
        row.keyWorkerSurname,
        row.departureReasonCode,
        row.moveOnCategoryCode,
        row.moveOnCategoryDescription,
        row.expectedArrivalDate,
        row.arrivalDate,
        row.expectedDepartureDate,
        row.departureDate,
        row.nonArrivalDate,
        row.nonArrivalContactDateTime?.let {
          ZonedDateTime.of(it, ZoneId.of("Europe/London")).toOffsetDateTime()
        },
        row.nonArrivalReasonCode,
        row.nonArrivalReasonDescription,
        row.nonArrivalNotes,
        row.premisesQCode,
        row.createdAt?.let {
          ZonedDateTime.of(it, ZoneId.of("Europe/London")).toOffsetDateTime()
        },
      ),
    )
  }

  private fun isAccepted(row: Cas1DeliusBookingManagementDataRow) = row.decisionCode.startsWith("A")

  override fun postSeed() {
    log.info(
      """
        Seeding complete. Note that the reported row count may be lower than the number of lines in the CSV file. 
        This is because some CSV rows have line breaks in 'notes' values
      """.trimIndent(),
    )
  }
}

data class Cas1DeliusBookingManagementDataRow(
  val bookingId: UUID?,
  val approvedPremisesReferralId: String,
  val crn: String,
  val eventNumber: String,
  val keyWorkerStaffCode: String?,
  val keyWorkerForename: String?,
  val keyWorkerMiddleName: String?,
  val keyWorkerSurname: String?,
  val decisionCode: String,
  val decisionDescription: String,
  val departureReasonCode: String?,
  val moveOnCategoryCode: String?,
  val moveOnCategoryDescription: String?,
  val expectedArrivalDate: LocalDate?,
  val arrivalDate: LocalDate?,
  val expectedDepartureDate: LocalDate?,
  val departureDate: LocalDate?,
  val nonArrivalDate: LocalDate?,
  val nonArrivalContactDateTime: LocalDateTime?,
  val nonArrivalReasonCode: String?,
  val nonArrivalReasonDescription: String?,
  val nonArrivalNotes: String?,
  val premisesQCode: String,
  val createdAt: LocalDateTime?,
)
