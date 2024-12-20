package uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.cas1.seed

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.springframework.beans.factory.annotation.Autowired
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model.ApplicationAssessed
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model.ApplicationAssessedAssessedBy
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model.ApplicationAssessedEnvelope
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model.ApplicationSubmitted
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model.ApplicationSubmittedEnvelope
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model.ApplicationSubmittedSubmittedBy
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model.BookingMade
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model.BookingMadeBookedBy
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model.BookingMadeEnvelope
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model.Cru
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model.EventType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model.Ldu
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model.PersonReference
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model.Premises
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model.ProbationArea
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model.Region
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model.Team
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.SeedFileType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.StaffDetailFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.cas1.seed.SeedCasUpdateEventNumberTest.CONSTANTS.NEW_CONVICTION_ID
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.cas1.seed.SeedCasUpdateEventNumberTest.CONSTANTS.NEW_EVENT_NUMBER
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.cas1.seed.SeedCasUpdateEventNumberTest.CONSTANTS.NEW_OFFENCE_ID
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.cas1.seed.SeedCasUpdateEventNumberTest.CONSTANTS.OLD_CONVICTION_ID
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.cas1.seed.SeedCasUpdateEventNumberTest.CONSTANTS.OLD_EVENT_NUMBER
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.cas1.seed.SeedCasUpdateEventNumberTest.CONSTANTS.OLD_OFFENCE_ID
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenACas1SpaceBooking
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenAUser
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenAnApArea
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenAnOffender
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.seed.SeedTestBase
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApprovedPremisesApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.DomainEvent
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.community.OffenderDetailSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.seed.CsvBuilder
import uk.gov.justice.digital.hmpps.approvedpremisesapi.seed.cas1.Cas1UpdateEventNumberSeedJobCsvRow
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.Cas1DomainEventService
import java.time.Instant
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.Period
import java.util.UUID

@TestInstance(TestInstance.Lifecycle.PER_METHOD)
class SeedCasUpdateEventNumberTest : SeedTestBase() {

  @Autowired
  lateinit var domainEventService: Cas1DomainEventService

  private object CONSTANTS {
    const val OLD_EVENT_NUMBER = "1010101"
    const val OLD_OFFENCE_ID = "2020202"
    const val OLD_CONVICTION_ID: Long = 3030303
    const val NEW_EVENT_NUMBER = "999990000"
    const val NEW_OFFENCE_ID = "888880000"
    const val NEW_CONVICTION_ID: Long = 777770000
  }

  @Test
  fun `Update Application event number and add note`() {
    val (application, _) = createApplication()

    callSeedJob(application = application)

    val updatedApplication = approvedPremisesApplicationRepository.findById(application.id).get()
    assertThat(updatedApplication.eventNumber).isEqualTo(NEW_EVENT_NUMBER)
    assertThat(updatedApplication.offenceId).isEqualTo(NEW_OFFENCE_ID)
    assertThat(updatedApplication.convictionId).isEqualTo(NEW_CONVICTION_ID)

    val notes = applicationTimelineNoteRepository.findApplicationTimelineNoteEntitiesByApplicationId(application.id)
    assertThat(notes).hasSize(1)
    assertThat(notes)
      .extracting("body")
      .contains(
        "Application Support have updated the application to use event number '999990000'. Previous event number was '1010101'",
      )
  }

  @Test
  fun `Update Application Assessed Domain Event`() {
    val (application, offenderDetails) = createApplication()

    val staffUserDetails = StaffDetailFactory.staffDetail()

    domainEventService.saveApplicationAssessedDomainEvent(
      DomainEvent(
        id = UUID.randomUUID(),
        applicationId = application.id,
        assessmentId = UUID.randomUUID(),
        crn = application.crn,
        nomsNumber = offenderDetails.otherIds.nomsNumber,
        occurredAt = Instant.now(),
        data = ApplicationAssessedEnvelope(
          id = UUID.randomUUID(),
          timestamp = Instant.now(),
          eventType = EventType.applicationAssessed,
          eventDetails = ApplicationAssessed(
            applicationId = application.id,
            applicationUrl = "theUrl",
            personReference = PersonReference(
              crn = offenderDetails.otherIds.crn,
              noms = offenderDetails.otherIds.nomsNumber ?: "Unknown NOMS Number",
            ),
            deliusEventNumber = application.eventNumber,
            assessedAt = Instant.now(),
            assessedBy = ApplicationAssessedAssessedBy(
              staffMember = staffUserDetails.toStaffMember(),
              probationArea = ProbationArea(
                code = staffUserDetails.probationArea.code,
                name = staffUserDetails.probationArea.description,
              ),
              cru = Cru(
                name = "the CRU name",
              ),
            ),
            decision = "theDecision",
            decisionRationale = "theDecisionRationale",
            arrivalDate = Instant.now(),
          ),
        ),
      ),
    )

    val domainEventBeforeUpdate = domainEventRepository.findByApplicationId(application.id)[0]

    callSeedJob(application = application)

    val domainEventAfterUpdate = domainEventRepository.findByApplicationId(application.id)[0]

    val unmarshalledData = objectMapper.readValue(domainEventAfterUpdate.data, ApplicationAssessedEnvelope::class.java)
    assertThat(unmarshalledData.eventDetails.deliusEventNumber).isEqualTo(NEW_EVENT_NUMBER)

    assertThat(domainEventBeforeUpdate.data.replace(OLD_EVENT_NUMBER, NEW_EVENT_NUMBER)).isEqualTo(domainEventAfterUpdate.data)
  }

  @Test
  fun `Update Application Submitted Domain Event`() {
    val (application, offenderDetails) = createApplication()

    val staffUserDetails = StaffDetailFactory.staffDetail()

    domainEventService.saveApplicationSubmittedDomainEvent(
      DomainEvent(
        id = UUID.randomUUID(),
        applicationId = application.id,
        crn = application.crn,
        nomsNumber = offenderDetails.otherIds.nomsNumber,
        occurredAt = Instant.now(),
        data = ApplicationSubmittedEnvelope(
          id = UUID.randomUUID(),
          timestamp = Instant.now(),
          eventType = EventType.applicationSubmitted,
          eventDetails = ApplicationSubmitted(
            applicationId = application.id,
            applicationUrl = "theUrl",
            personReference = PersonReference(
              crn = application.crn,
              noms = offenderDetails.otherIds.nomsNumber ?: "Unknown NOMS Number",
            ),
            deliusEventNumber = application.eventNumber,
            mappa = "mappa1",
            offenceId = application.offenceId,
            releaseType = "releaseTpye",
            age = Period.between(offenderDetails.dateOfBirth, LocalDate.now()).years,
            gender = ApplicationSubmitted.Gender.male,
            targetLocation = "target location",
            submittedAt = Instant.now(),
            submittedBy = ApplicationSubmittedSubmittedBy(
              staffMember = staffUserDetails.toStaffMember(),
              probationArea = ProbationArea("theCode", "theName"),
              team = Team("theCode", "theName"),
              ldu = Ldu("theCode", "theName"),
              region = Region("theCode", "theName"),
            ),
            sentenceLengthInMonths = null,
          ),
        ),
        metadata = mapOf(),
      ),
    )

    val domainEventBeforeUpdate = domainEventRepository.findByApplicationId(application.id)[0]

    callSeedJob(application = application)

    val domainEventAfterUpdate = domainEventRepository.findByApplicationId(application.id)[0]

    val unmarshalledData = objectMapper.readValue(domainEventAfterUpdate.data, ApplicationSubmittedEnvelope::class.java)
    assertThat(unmarshalledData.eventDetails.deliusEventNumber).isEqualTo(NEW_EVENT_NUMBER)

    assertThat(domainEventBeforeUpdate.data.replace(OLD_EVENT_NUMBER, NEW_EVENT_NUMBER)).isEqualTo(domainEventAfterUpdate.data)
  }

  @Test
  fun `Update Booking Made Domain Event`() {
    val (application, offenderDetails) = createApplication()

    val staffUserDetails = StaffDetailFactory.staffDetail()

    domainEventService.saveBookingMadeDomainEvent(
      DomainEvent(
        id = UUID.randomUUID(),
        applicationId = application.id,
        crn = application.crn,
        nomsNumber = offenderDetails.otherIds.nomsNumber,
        occurredAt = Instant.now(),
        bookingId = UUID.randomUUID(),
        data = BookingMadeEnvelope(
          id = UUID.randomUUID(),
          timestamp = Instant.now(),
          eventType = EventType.bookingMade,
          eventDetails = BookingMade(
            applicationId = application.id,
            applicationUrl = "theUrl",
            bookingId = UUID.randomUUID(),
            personReference = PersonReference(
              crn = application.crn,
              noms = offenderDetails.otherIds.nomsNumber ?: "Unknown NOMS Number",
            ),
            deliusEventNumber = application.eventNumber,
            createdAt = Instant.now(),
            bookedBy = BookingMadeBookedBy(
              staffMember = staffUserDetails.toStaffMember(),
              cru = Cru(
                name = "theCruName",
              ),
            ),
            premises = Premises(
              id = UUID.randomUUID(),
              name = "premisesName",
              apCode = "premisesApCode",
              legacyApCode = "premisesQCode",
              localAuthorityAreaName = "laAreaName",
            ),
            arrivalOn = LocalDate.now(),
            departureOn = LocalDate.now(),
            applicationSubmittedOn = Instant.now(),
            releaseType = "releaseType",
            sentenceType = "sentenceType",
            situation = "situation",
          ),
        ),
      ),
    )

    val domainEventBeforeUpdate = domainEventRepository.findByApplicationId(application.id)[0]

    callSeedJob(application = application)

    val domainEventAfterUpdate = domainEventRepository.findByApplicationId(application.id)[0]

    val unmarshalledData = objectMapper.readValue(domainEventAfterUpdate.data, BookingMadeEnvelope::class.java)
    assertThat(unmarshalledData.eventDetails.deliusEventNumber).isEqualTo(NEW_EVENT_NUMBER)

    assertThat(domainEventBeforeUpdate.data.replace(OLD_EVENT_NUMBER, NEW_EVENT_NUMBER)).isEqualTo(domainEventAfterUpdate.data)
  }

  @Test
  fun `Update Space Booking event number`() {
    val (application, _) = createApplication()

    val spaceBooking = givenACas1SpaceBooking(
      crn = application.crn,
      application = application,
      deliusEventNumber = OLD_EVENT_NUMBER,
    )

    callSeedJob(application = application)

    val updatedSpaceBooking = cas1SpaceBookingRepository.findById(spaceBooking.id).get()
    assertThat(updatedSpaceBooking.deliusEventNumber).isEqualTo(NEW_EVENT_NUMBER)
  }

  private fun callSeedJob(application: ApprovedPremisesApplicationEntity) {
    withCsv(
      "valid-csv",
      rowsToCsv(
        listOf(
          Cas1UpdateEventNumberSeedJobCsvRow(
            application.id,
            NEW_EVENT_NUMBER.toInt(),
            NEW_OFFENCE_ID,
            NEW_CONVICTION_ID,
          ),
        ),
      ),
    )

    seedService.seedData(SeedFileType.approvedPremisesUpdateEventNumber, "valid-csv.csv")
  }

  private fun createApplication(): Pair<ApprovedPremisesApplicationEntity, OffenderDetailSummary> {
    val (applicant, _) = givenAUser()
    val (offenderDetails, _) = givenAnOffender()

    return Pair(
      approvedPremisesApplicationEntityFactory.produceAndPersist {
        withCrn(offenderDetails.otherIds.crn)
        withCreatedByUser(applicant)
        withApplicationSchema(
          approvedPremisesApplicationJsonSchemaEntityFactory.produceAndPersist {
            withPermissiveSchema()
          },
        )
        withSubmittedAt(OffsetDateTime.now())
        withApArea(givenAnApArea())
        withReleaseType("licence")
        withEventNumber(OLD_EVENT_NUMBER)
        withOffenceId(OLD_OFFENCE_ID)
        withConvictionId(OLD_CONVICTION_ID)
      },
      offenderDetails,
    )
  }

  private fun rowsToCsv(rows: List<Cas1UpdateEventNumberSeedJobCsvRow>): String {
    val builder = CsvBuilder()
      .withUnquotedFields(
        "application_id",
        "event_number",
        "offence_id",
        "conviction_id",
      )
      .newRow()

    rows.forEach {
      builder
        .withQuotedField(it.applicationId.toString())
        .withQuotedField(it.eventNumber.toString())
        .withQuotedField(it.offenceId)
        .withQuotedField(it.convictionId.toString())
        .newRow()
    }

    return builder.build()
  }
}
