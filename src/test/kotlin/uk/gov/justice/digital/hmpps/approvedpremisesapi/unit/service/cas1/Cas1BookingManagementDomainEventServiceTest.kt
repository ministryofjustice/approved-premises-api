package uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.service.cas1

import io.mockk.Runs
import io.mockk.every
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import io.mockk.just
import io.mockk.slot
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.http.HttpStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model.BookingKeyWorkerAssignedEnvelope
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model.EventType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model.PersonArrivedEnvelope
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model.PersonDepartedEnvelope
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.ApDeliusContextApiClient
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.ClientResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ApprovedPremisesApplicationEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ApprovedPremisesEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.Cas1SpaceBookingEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.CaseSummaryFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.OfflineApplicationEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.StaffDetailFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.UserEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.Cas1SpaceBookingEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.DepartureReasonEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.MoveOnCategoryEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.DomainEvent
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.PersonSummaryInfoResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.OffenderService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.Cas1DomainEventService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.Cas1SpaceBookingManagementDomainEventService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.Cas1SpaceBookingManagementDomainEventServiceConfig
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.ApplicationTimelineTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.UrlTemplate
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.isWithinTheLastMinute
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.toLocalDate
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.util.UUID

@ExtendWith(MockKExtension::class)
class Cas1BookingManagementDomainEventServiceTest {

  @MockK
  lateinit var apDeliusContextApiClient: ApDeliusContextApiClient

  @MockK
  lateinit var domainEventService: Cas1DomainEventService

  @MockK
  lateinit var offenderService: OffenderService

  @MockK
  lateinit var applicationTimelineTransformer: ApplicationTimelineTransformer

  @MockK
  lateinit var cas1SpaceBookingManagementDomainEventServiceConfig: Cas1SpaceBookingManagementDomainEventServiceConfig

  @InjectMockKs
  lateinit var service: Cas1SpaceBookingManagementDomainEventService

  companion object Constants {
    const val DELIUS_EVENT_NUMBER = "52"
  }

  @BeforeEach
  fun before() {
    every { cas1SpaceBookingManagementDomainEventServiceConfig.applicationUrlTemplate } returns UrlTemplate("http://frontend/applications/#id")
  }

  @Nested
  inner class ArrivalRecorded {

    private val arrivalDate = LocalDate.of(2023, 3, 12)
    private val arrivalTime = LocalTime.of(11, 30, 0, 0)
    private val departureDate = LocalDate.now().plusMonths(3)

    private val application = ApprovedPremisesApplicationEntityFactory()
      .withSubmittedAt(OffsetDateTime.parse("2024-10-01T12:00:00Z"))
      .withCreatedByUser(
        UserEntityFactory()
          .withUnitTestControlProbationRegion()
          .produce(),
      )
      .produce()

    private val caseSummary = CaseSummaryFactory()
      .produce()

    private val premises = ApprovedPremisesEntityFactory()
      .withDefaults()
      .produce()

    private val recordedByUser = UserEntityFactory().withDefaults().produce()
    private val recordedByStaffDetails = StaffDetailFactory.staffDetail(deliusUsername = recordedByUser.deliusUsername)
    private val keyWorker = StaffDetailFactory.staffDetail(deliusUsername = null)

    private val spaceBookingFactory = Cas1SpaceBookingEntityFactory()
      .withApplication(null)
      .withOfflineApplication(null)
      .withPremises(premises)
      .withCanonicalArrivalDate(arrivalDate)
      .withExpectedDepartureDate(departureDate)
      .withCanonicalDepartureDate(departureDate)
      .withKeyworkerStaffCode(keyWorker.code)
      .withDeliusEventNumber(DELIUS_EVENT_NUMBER)

    @BeforeEach
    fun before() {
      every { domainEventService.savePersonArrivedEvent(any()) } just Runs

      every { offenderService.getPersonSummaryInfoResults(any(), any()) } returns
        listOf(PersonSummaryInfoResult.Success.Full("THEBOOKINGCRN", caseSummary))

      every { apDeliusContextApiClient.getStaffDetailByStaffCode(keyWorker.code) } returns ClientResult.Success(
        HttpStatus.OK,
        keyWorker,
      )

      every { apDeliusContextApiClient.getStaffDetail(recordedByUser.deliusUsername) } returns ClientResult.Success(
        HttpStatus.OK,
        recordedByStaffDetails,
      )
    }

    @Test
    fun `record arrival and emits domain event`() {
      val spaceBooking = spaceBookingFactory.withApplication(application).produce()

      service.arrivalRecorded(
        Cas1SpaceBookingManagementDomainEventService.ArrivalInfo(
          spaceBooking,
          arrivalDate,
          arrivalTime,
          recordedByUser,
        ),
      )

      val domainEventArgument = slot<DomainEvent<PersonArrivedEnvelope>>()

      verify(exactly = 1) {
        domainEventService.savePersonArrivedEvent(
          capture(domainEventArgument),
        )
      }

      val domainEvent = domainEventArgument.captured

      assertThat(domainEvent.data.eventType).isEqualTo(EventType.personArrived)
      assertThat(domainEvent.data.timestamp).isWithinTheLastMinute()
      assertThat(domainEvent.applicationId).isEqualTo(application.id)
      assertThat(domainEvent.bookingId).isNull()
      assertThat(domainEvent.cas1SpaceBookingId).isEqualTo(spaceBooking.id)
      assertThat(domainEvent.schemaVersion).isEqualTo(2)
      assertThat(domainEvent.crn).isEqualTo(spaceBooking.crn)
      assertThat(domainEvent.nomsNumber).isEqualTo(caseSummary.nomsId)
      assertThat(domainEvent.occurredAt).isWithinTheLastMinute()
      val data = domainEvent.data.eventDetails
      assertThat(data.previousExpectedDepartureOn).isNull()
      assertThat(data.applicationId).isEqualTo(application.id)
      assertThat(data.applicationSubmittedOn).isEqualTo(application.submittedAt!!.toLocalDate())
      assertThat(data.applicationUrl).isEqualTo("http://frontend/applications/${application.id}")
      assertThat(data.arrivedAt).isEqualTo(Instant.parse("2023-03-12T11:30:00Z"))
      assertThat(data.deliusEventNumber).isEqualTo(DELIUS_EVENT_NUMBER)
      assertThat(data.premises.id).isEqualTo(premises.id)
      assertThat(data.premises.name).isEqualTo(premises.name)
      assertThat(data.premises.apCode).isEqualTo(premises.apCode)
      assertThat(data.premises.legacyApCode).isEqualTo(premises.qCode)
      assertThat(data.premises.localAuthorityAreaName).isEqualTo(premises.localAuthorityArea!!.name)
      assertThat(data.keyWorker!!.staffCode).isEqualTo(keyWorker.code)
      assertThat(data.keyWorker!!.surname).isEqualTo(keyWorker.name.surname)
      assertThat(data.keyWorker!!.forenames).isEqualTo(keyWorker.name.forenames())
      assertThat(data.recordedBy.staffCode).isEqualTo(recordedByStaffDetails.code)
      assertThat(data.recordedBy.username).isEqualTo(recordedByStaffDetails.username)
      assertThat(data.recordedBy.forenames).isEqualTo(recordedByStaffDetails.name.forenames())
      assertThat(data.recordedBy.surname).isEqualTo(recordedByStaffDetails.name.surname)
    }

    @Test
    fun `record arrival and emits domain event, with offline application`() {
      val offlineApplication = OfflineApplicationEntityFactory().produce()

      val spaceBooking = spaceBookingFactory.withOfflineApplication(offlineApplication).produce()

      service.arrivalRecorded(
        Cas1SpaceBookingManagementDomainEventService.ArrivalInfo(
          spaceBooking,
          arrivalDate,
          arrivalTime,
          recordedByUser,
        ),
      )

      val domainEventArgument = slot<DomainEvent<PersonArrivedEnvelope>>()

      verify(exactly = 1) {
        domainEventService.savePersonArrivedEvent(
          capture(domainEventArgument),
        )
      }

      val domainEvent = domainEventArgument.captured

      assertThat(domainEvent.applicationId).isEqualTo(offlineApplication.id)
      assertThat(domainEvent.crn).isEqualTo(spaceBooking.crn)
      val data = domainEvent.data.eventDetails
      assertThat(data.applicationId).isEqualTo(offlineApplication.id)
      assertThat(data.applicationSubmittedOn).isEqualTo(offlineApplication.createdAt.toLocalDate())
      assertThat(data.applicationUrl).isEqualTo("http://frontend/applications/${offlineApplication.id}")
    }

    @Test
    fun `record arrival and emits domain event with no keyWorker information if keyWorker is not present in original booking`() {
      val existingSpaceBooking = Cas1SpaceBookingEntityFactory()
        .withApplication(application)
        .withDeliusEventNumber(DELIUS_EVENT_NUMBER)
        .withPremises(premises)
        .withCanonicalArrivalDate(arrivalDate)
        .withExpectedDepartureDate(departureDate)
        .withCanonicalDepartureDate(departureDate)
        .withKeyworkerStaffCode(null)
        .produce()

      service.arrivalRecorded(
        Cas1SpaceBookingManagementDomainEventService.ArrivalInfo(
          existingSpaceBooking,
          arrivalDate,
          arrivalTime,
          recordedByUser,
        ),
      )

      val domainEventArgument = slot<DomainEvent<PersonArrivedEnvelope>>()

      verify(exactly = 1) {
        domainEventService.savePersonArrivedEvent(
          capture(domainEventArgument),
        )
      }

      val domainEvent = domainEventArgument.captured

      val data = domainEvent.data.eventDetails
      assertThat(data.keyWorker).isNull()
    }
  }

  @Nested
  inner class DepartureRecorded {

    private val arrivedDate = LocalDateTime.now().toInstant(ZoneOffset.UTC)
    private val departedDate = LocalDate.of(2023, 11, 6)
    private val departureTime = LocalTime.of(1, 30, 30)

    private val application = ApprovedPremisesApplicationEntityFactory()
      .withSubmittedAt(OffsetDateTime.parse("2024-10-01T12:00:00Z"))
      .withCreatedByUser(
        UserEntityFactory()
          .withUnitTestControlProbationRegion()
          .produce(),
      )
      .produce()

    private val caseSummary = CaseSummaryFactory()
      .produce()

    private val premises = ApprovedPremisesEntityFactory()
      .withDefaults()
      .produce()

    private val recordedByUser = UserEntityFactory().withDefaults().produce()
    private val recordedByStaffDetails = StaffDetailFactory.staffDetail(deliusUsername = recordedByUser.deliusUsername)
    private val keyWorker = StaffDetailFactory.staffDetail(deliusUsername = null)

    private val departedSpaceBookingFactory = Cas1SpaceBookingEntityFactory()
      .withApplication(null)
      .withOfflineApplication(null)
      .withDeliusEventNumber(DELIUS_EVENT_NUMBER)
      .withPremises(premises)
      .withCanonicalArrivalDate(arrivedDate.toLocalDate())
      .withExpectedDepartureDate(departedDate)
      .withCanonicalDepartureDate(departedDate)
      .withKeyworkerStaffCode(keyWorker.code)

    private val departureReason = DepartureReasonEntity(
      id = UUID.randomUUID(),
      name = "departureReason",
      isActive = true,
      serviceScope = "approved-premises",
      legacyDeliusReasonCode = "legacyDeliusReasonCode",
      parentReasonId = null,
    )
    private val moveOnCategory = MoveOnCategoryEntity(
      id = UUID.randomUUID(),
      name = "moveOnCategory",
      isActive = true,
      serviceScope = "approved-premises",
      legacyDeliusCategoryCode = "legacyDeliusReasonCode",
    )

    @BeforeEach
    fun before() {
      every { domainEventService.savePersonDepartedEvent(any()) } just Runs

      every { offenderService.getPersonSummaryInfoResults(any(), any()) } returns
        listOf(PersonSummaryInfoResult.Success.Full("THEBOOKINGCRN", caseSummary))

      every { apDeliusContextApiClient.getStaffDetailByStaffCode(keyWorker.code) } returns ClientResult.Success(
        HttpStatus.OK,
        keyWorker,
      )

      every { apDeliusContextApiClient.getStaffDetail(recordedByUser.deliusUsername) } returns ClientResult.Success(
        HttpStatus.OK,
        recordedByStaffDetails,
      )
    }

    @Test
    fun `record departure and emits domain event`() {
      val departedSpaceBooking = departedSpaceBookingFactory.withApplication(application).produce()

      service.departureRecorded(
        Cas1SpaceBookingManagementDomainEventService.DepartureInfo(
          departedSpaceBooking,
          departureReason,
          moveOnCategory,
          departedDate,
          departureTime,
          recordedByUser,
        ),
      )

      val domainEventArgument = slot<DomainEvent<PersonDepartedEnvelope>>()

      verify(exactly = 1) {
        domainEventService.savePersonDepartedEvent(capture(domainEventArgument))
      }

      val domainEvent = domainEventArgument.captured

      assertThat(domainEvent.data.eventType).isEqualTo(EventType.personDeparted)
      assertThat(domainEvent.data.timestamp).isWithinTheLastMinute()
      assertThat(domainEvent.applicationId).isEqualTo(application.id)
      assertThat(domainEvent.bookingId).isNull()
      assertThat(domainEvent.schemaVersion).isEqualTo(2)
      assertThat(domainEvent.cas1SpaceBookingId).isEqualTo(departedSpaceBooking.id)
      assertThat(domainEvent.crn).isEqualTo(departedSpaceBooking.crn)
      assertThat(domainEvent.nomsNumber).isEqualTo(caseSummary.nomsId)
      assertThat(domainEvent.occurredAt).isWithinTheLastMinute()
      val domainEventEventDetails = domainEvent.data.eventDetails
      assertThat(domainEventEventDetails.applicationId).isEqualTo(application.id)
      assertThat(domainEventEventDetails.applicationUrl).isEqualTo("http://frontend/applications/${application.id}")
      assertThat(domainEventEventDetails.bookingId).isEqualTo(departedSpaceBooking.id)
      assertThat(domainEventEventDetails.personReference.crn).isEqualTo(departedSpaceBooking.crn)
      assertThat(domainEventEventDetails.personReference.noms).isEqualTo(caseSummary.nomsId)
      assertThat(domainEventEventDetails.deliusEventNumber).isEqualTo(DELIUS_EVENT_NUMBER)
      assertThat(domainEventEventDetails.departedAt).isEqualTo(Instant.parse("2023-11-06T01:30:30Z"))
      assertThat(domainEventEventDetails.reason).isEqualTo(departureReason.name)
      assertThat(domainEventEventDetails.legacyReasonCode).isEqualTo(departureReason.legacyDeliusReasonCode)
      val domainEventPremises = domainEventEventDetails.premises
      assertThat(domainEventPremises.id).isEqualTo(premises.id)
      assertThat(domainEventPremises.name).isEqualTo(premises.name)
      assertThat(domainEventPremises.apCode).isEqualTo(premises.apCode)
      assertThat(domainEventPremises.legacyApCode).isEqualTo(premises.qCode)
      assertThat(domainEventPremises.localAuthorityAreaName).isEqualTo(premises.localAuthorityArea!!.name)
      val domainEventKeyWorker = domainEventEventDetails.keyWorker
      assertThat(domainEventKeyWorker.staffCode).isEqualTo(keyWorker.code)
      assertThat(domainEventKeyWorker.surname).isEqualTo(keyWorker.name.surname)
      assertThat(domainEventKeyWorker.forenames).isEqualTo(keyWorker.name.forenames())
      val domainEventMoveOnCategory = domainEventEventDetails.destination.moveOnCategory
      assertThat(domainEventMoveOnCategory.id).isEqualTo(moveOnCategory.id)
      assertThat(domainEventMoveOnCategory.description).isEqualTo(moveOnCategory.name)
      assertThat(domainEventMoveOnCategory.legacyMoveOnCategoryCode).isEqualTo(moveOnCategory.legacyDeliusCategoryCode)
      assertThat(domainEventEventDetails.recordedBy.staffCode).isEqualTo(recordedByStaffDetails.code)
      assertThat(domainEventEventDetails.recordedBy.username).isEqualTo(recordedByStaffDetails.username)
      assertThat(domainEventEventDetails.recordedBy.forenames).isEqualTo(recordedByStaffDetails.name.forenames())
      assertThat(domainEventEventDetails.recordedBy.surname).isEqualTo(recordedByStaffDetails.name.surname)
    }

    @Test
    fun `record departure and emits domain event for offline application`() {
      val offlineApplication = OfflineApplicationEntityFactory().produce()

      val departedSpaceBooking = departedSpaceBookingFactory.withOfflineApplication(offlineApplication).produce()

      service.departureRecorded(
        Cas1SpaceBookingManagementDomainEventService.DepartureInfo(
          departedSpaceBooking,
          departureReason,
          moveOnCategory,
          departedDate,
          departureTime,
          recordedByUser,
        ),
      )

      val domainEventArgument = slot<DomainEvent<PersonDepartedEnvelope>>()

      verify(exactly = 1) {
        domainEventService.savePersonDepartedEvent(capture(domainEventArgument))
      }

      val domainEvent = domainEventArgument.captured

      assertThat(domainEvent.applicationId).isEqualTo(offlineApplication.id)
      assertThat(domainEvent.crn).isEqualTo(departedSpaceBooking.crn)
      val domainEventEventDetails = domainEvent.data.eventDetails
      assertThat(domainEventEventDetails.applicationId).isEqualTo(offlineApplication.id)
      assertThat(domainEventEventDetails.applicationUrl).isEqualTo("http://frontend/applications/${offlineApplication.id}")
    }
  }

  @Nested
  inner class KeyWorkerAssigned {

    private val arrivalDate = LocalDate.now()
    private val departureDate = LocalDate.now().plusMonths(3)

    private val application = ApprovedPremisesApplicationEntityFactory()
      .withSubmittedAt(OffsetDateTime.parse("2024-10-01T12:00:00Z"))
      .withCreatedByUser(
        UserEntityFactory()
          .withUnitTestControlProbationRegion()
          .produce(),
      )
      .produce()

    private val caseSummary = CaseSummaryFactory()
      .produce()

    private val premises = ApprovedPremisesEntityFactory()
      .withDefaults()
      .produce()

    private val keyWorker = StaffDetailFactory.staffDetail(deliusUsername = null)

    private val keyWorkerName = keyWorker.name.deliusName()
    private val previousKeyWorkerName = "Previous $keyWorkerName"

    private val spaceBookingWithoutKeyWorkerFactory = Cas1SpaceBookingEntityFactory()
      .withApplication(null)
      .withOfflineApplication(null)
      .withDeliusEventNumber(DELIUS_EVENT_NUMBER)
      .withPremises(premises)
      .withActualArrivalDate(arrivalDate)
      .withCanonicalArrivalDate(arrivalDate)
      .withExpectedDepartureDate(departureDate)
      .withCanonicalDepartureDate(departureDate)

    private val spaceBookingWithKeyWorkerFactory = Cas1SpaceBookingEntityFactory()
      .withApplication(null)
      .withOfflineApplication(null)
      .withDeliusEventNumber(DELIUS_EVENT_NUMBER)
      .withPremises(premises)
      .withActualArrivalDate(arrivalDate)
      .withCanonicalArrivalDate(arrivalDate)
      .withExpectedDepartureDate(departureDate)
      .withCanonicalDepartureDate(departureDate)
      .withKeyworkerStaffCode(keyWorker.code)
      .withKeyworkerName(previousKeyWorkerName)

    @BeforeEach
    fun before() {
      every { domainEventService.saveKeyWorkerAssignedEvent(any(), any()) } just Runs

      every { offenderService.getPersonSummaryInfoResults(any(), any()) } returns
        listOf(PersonSummaryInfoResult.Success.Full("THEBOOKINGCRN", caseSummary))

      every { apDeliusContextApiClient.getStaffDetailByStaffCode(any()) } returns ClientResult.Success(
        HttpStatus.OK,
        keyWorker,
      )
    }

    @Test
    fun `record keyWorker assigned and emits domain event with newly assigned key worker`() {
      val spaceBookingWithoutKeyWorker = spaceBookingWithoutKeyWorkerFactory.withApplication(application).produce()

      service.keyWorkerAssigned(
        spaceBookingWithoutKeyWorker,
        assignedKeyWorker = keyWorker.toStaffMember(),
        assignedKeyWorkerName = keyWorkerName,
        previousKeyWorkerName = null,
      )

      val domainEventArgument = slot<DomainEvent<BookingKeyWorkerAssignedEnvelope>>()

      verify(exactly = 1) {
        domainEventService.saveKeyWorkerAssignedEvent(
          capture(domainEventArgument),
          emit = false,
        )
      }

      val domainEvent = domainEventArgument.captured

      checkDomainEventBookingProperties(domainEvent, spaceBookingWithoutKeyWorker, application.id)
      val domainEventEventDetails = domainEvent.data.eventDetails
      assertThat(domainEventEventDetails.previousKeyWorkerName).isEqualTo(null)
      assertThat(domainEventEventDetails.assignedKeyWorkerName).isEqualTo(keyWorkerName)
      val domainEventKeyWorker = domainEventEventDetails.keyWorker
      assertThat(domainEventKeyWorker.staffCode).isEqualTo(keyWorker.code)
      assertThat(domainEventKeyWorker.surname).isEqualTo(keyWorker.name.surname)
      assertThat(domainEventKeyWorker.forenames).isEqualTo(keyWorker.name.forenames())
    }

    @Test
    fun `record keyWorker assigned and emits domain event with previous and newly assigned key worker`() {
      val spaceBookingWithKeyWorker = spaceBookingWithKeyWorkerFactory.withApplication(application).produce()

      service.keyWorkerAssigned(
        spaceBookingWithKeyWorker,
        assignedKeyWorker = keyWorker.toStaffMember(),
        assignedKeyWorkerName = keyWorkerName,
        previousKeyWorkerName = previousKeyWorkerName,
      )

      val domainEventArgument = slot<DomainEvent<BookingKeyWorkerAssignedEnvelope>>()

      verify(exactly = 1) {
        domainEventService.saveKeyWorkerAssignedEvent(
          capture(domainEventArgument),
          emit = false,
        )
      }

      val domainEvent = domainEventArgument.captured

      checkDomainEventBookingProperties(domainEvent, spaceBookingWithKeyWorker, application.id)
      val domainEventEventDetails = domainEvent.data.eventDetails
      assertThat(domainEventEventDetails.previousKeyWorkerName).isEqualTo(previousKeyWorkerName)
      assertThat(domainEventEventDetails.assignedKeyWorkerName).isEqualTo(keyWorkerName)
    }

    @Test
    fun `record keyWorker assigned and emits domain event with offline application`() {
      val offlineApplication = OfflineApplicationEntityFactory().produce()

      val spaceBookingWithKeyWorker = spaceBookingWithKeyWorkerFactory.withOfflineApplication(offlineApplication).produce()

      service.keyWorkerAssigned(
        spaceBookingWithKeyWorker,
        assignedKeyWorker = keyWorker.toStaffMember(),
        assignedKeyWorkerName = keyWorkerName,
        previousKeyWorkerName = previousKeyWorkerName,
      )

      val domainEventArgument = slot<DomainEvent<BookingKeyWorkerAssignedEnvelope>>()

      verify(exactly = 1) {
        domainEventService.saveKeyWorkerAssignedEvent(
          capture(domainEventArgument),
          emit = false,
        )
      }

      val domainEvent = domainEventArgument.captured

      checkDomainEventBookingProperties(domainEvent, spaceBookingWithKeyWorker, offlineApplication.id)
      val domainEventEventDetails = domainEvent.data.eventDetails
      assertThat(domainEventEventDetails.previousKeyWorkerName).isEqualTo(previousKeyWorkerName)
      assertThat(domainEventEventDetails.assignedKeyWorkerName).isEqualTo(keyWorkerName)
    }

    private fun checkDomainEventBookingProperties(
      domainEvent: DomainEvent<BookingKeyWorkerAssignedEnvelope>,
      spaceBooking: Cas1SpaceBookingEntity,
      applicationId: UUID,
    ) {
      assertThat(domainEvent.data.eventType).isEqualTo(EventType.bookingKeyWorkerAssigned)
      assertThat(domainEvent.data.timestamp).isWithinTheLastMinute()
      assertThat(domainEvent.applicationId).isEqualTo(applicationId)
      assertThat(domainEvent.bookingId).isNull()
      assertThat(domainEvent.cas1SpaceBookingId).isEqualTo(spaceBooking.id)
      assertThat(domainEvent.crn).isEqualTo(spaceBooking.crn)
      assertThat(domainEvent.nomsNumber).isEqualTo(caseSummary.nomsId)
      assertThat(domainEvent.occurredAt).isWithinTheLastMinute()
      val domainEventEventDetails = domainEvent.data.eventDetails
      assertThat(domainEventEventDetails.applicationId).isEqualTo(applicationId)
      assertThat(domainEventEventDetails.applicationUrl).isEqualTo("http://frontend/applications/$applicationId")
      assertThat(domainEventEventDetails.bookingId).isEqualTo(spaceBooking.id)
      assertThat(domainEventEventDetails.personReference.crn).isEqualTo(spaceBooking.crn)
      assertThat(domainEventEventDetails.personReference.noms).isEqualTo(caseSummary.nomsId)
      assertThat(domainEventEventDetails.deliusEventNumber).isEqualTo(DELIUS_EVENT_NUMBER)
      assertThat(domainEventEventDetails.arrivalDate).isEqualTo(spaceBooking.canonicalArrivalDate)
      assertThat(domainEventEventDetails.departureDate).isEqualTo(spaceBooking.canonicalDepartureDate)
      val domainEventPremises = domainEventEventDetails.premises
      assertThat(domainEventPremises.id).isEqualTo(premises.id)
      assertThat(domainEventPremises.name).isEqualTo(premises.name)
      assertThat(domainEventPremises.apCode).isEqualTo(premises.apCode)
      assertThat(domainEventPremises.legacyApCode).isEqualTo(premises.qCode)
      assertThat(domainEventPremises.localAuthorityAreaName).isEqualTo(premises.localAuthorityArea!!.name)
    }
  }
}
