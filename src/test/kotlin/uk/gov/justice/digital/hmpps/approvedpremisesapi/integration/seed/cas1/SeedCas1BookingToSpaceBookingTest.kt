package uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.seed.cas1

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.repository.findByIdOrNull
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model.BookingMade
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model.BookingMadeBookedBy
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model.BookingMadeEnvelope
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model.Cru
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model.EventType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model.PersonReference
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model.Premises
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model.StaffMember
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.SeedFileType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ServiceName
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenABooking
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenABookingForAnOfflineApplication
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenACas1Application
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenAPlacementRequest
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenAUser
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenAnApprovedPremises
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenAnOfflineApplication
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.seed.SeedTestBase
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApprovedPremisesEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.BookingEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.Cas1SpaceBookingEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.CharacteristicEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.DepartureReasonEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ManagementInfoSource
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.MoveOnCategoryEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.NonArrivalReasonEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.OfflineApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.cas1.Cas1DeliusBookingImportEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.cas1.Cas1DeliusBookingImportRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.DomainEvent
import uk.gov.justice.digital.hmpps.approvedpremisesapi.repository.Cas1SpaceBookingTestRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.seed.CsvBuilder
import uk.gov.justice.digital.hmpps.approvedpremisesapi.seed.cas1.Cas1BookingToSpaceBookingSeedCsvRow
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.Cas1BookingDomainEventService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.Cas1DomainEventService
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.util.UUID

@TestInstance(TestInstance.Lifecycle.PER_METHOD)
class SeedCas1BookingToSpaceBookingTest : SeedTestBase() {

  @Autowired
  lateinit var cas1SpaceBookingTestRepository: Cas1SpaceBookingTestRepository

  @Autowired
  lateinit var cas1BookingDomainEventSet: Cas1BookingDomainEventService

  @Autowired
  lateinit var cas1DomainEventService: Cas1DomainEventService

  @Autowired
  lateinit var deliusBookingImportRepository: Cas1DeliusBookingImportRepository

  lateinit var premises: ApprovedPremisesEntity
  lateinit var otherPremise: ApprovedPremisesEntity
  lateinit var otherUser: UserEntity
  lateinit var roomCriteriaOfInterest: List<CharacteristicEntity>
  lateinit var roomCriterionNotOfInstant: CharacteristicEntity
  lateinit var premiseCriterion: CharacteristicEntity

  lateinit var departureReasonActive: DepartureReasonEntity
  lateinit var moveOnCategory: MoveOnCategoryEntity
  lateinit var nonArrivalReasonCode: NonArrivalReasonEntity

  @BeforeEach
  fun setupReferenceData() {
    premises = givenAnApprovedPremises(
      name = "Premises 1",
      supportsSpaceBookings = true,
    )
    otherPremise = givenAnApprovedPremises(
      name = "Premises 2",
      supportsSpaceBookings = true,
    )
    otherUser = givenAUser().first
    roomCriteriaOfInterest = Cas1SpaceBookingEntity.Constants.CRITERIA_CHARACTERISTIC_PROPERTY_NAMES_OF_INTEREST.map {
      characteristicRepository.findByPropertyName(it, ServiceName.approvedPremises.value)!!
    }
    roomCriterionNotOfInstant = characteristicEntityFactory.produceAndPersist {
      withModelScope("room")
      withPropertyName("not of interest")
    }
    premiseCriterion = characteristicEntityFactory.produceAndPersist { withModelScope("premises") }

    departureReasonEntityFactory.produceAndPersist {
      withLegacyDeliusCategoryCode("dr1inactive")
      withServiceScope(ServiceName.approvedPremises.value)
    }
    departureReasonActive = departureReasonEntityFactory.produceAndPersist {
      withLegacyDeliusCategoryCode("dr1")
      withServiceScope(ServiceName.approvedPremises.value)
    }
    moveOnCategory = moveOnCategoryEntityFactory.produceAndPersist {
      withLegacyDeliusCategoryCode("moc1")
      withServiceScope(ServiceName.approvedPremises.value)
    }
    nonArrivalReasonCode = nonArrivalReasonEntityFactory.produceAndPersist {
      withLegacyDeliusReasonCode("narc1")
    }
  }

  @SuppressWarnings("LongMethod")
  @Test
  fun `Migrate bookings, removing existing`() {
    val application1 = givenACas1Application(createdByUser = otherUser, eventNumber = "25")
    val booking1ManagementInfoFromDelius = givenABooking(
      crn = "CRN1",
      premises = premises,
      application = application1,
      arrivalDate = LocalDate.of(2024, 5, 1),
      departureDate = LocalDate.of(2024, 5, 5),
    )
    val placementRequest1 = givenAPlacementRequest(
      application = application1,
      placementRequestAllocatedTo = otherUser,
      assessmentAllocatedTo = otherUser,
      createdByUser = otherUser,
      booking = booking1ManagementInfoFromDelius,
      essentialCriteria = roomCriteriaOfInterest + listOf(roomCriterionNotOfInstant) + listOf(premiseCriterion),
    ).first
    val (booking1CreatedByUser) = givenAUser()
    cas1BookingDomainEventSet.bookingMade(
      application1,
      booking1ManagementInfoFromDelius,
      booking1CreatedByUser,
      placementRequest1,
    )

    deliusBookingImportRepository.save(
      Cas1DeliusBookingImportEntity(
        bookingId = booking1ManagementInfoFromDelius.id,
        crn = "irrelevant",
        eventNumber = "irrelevant",
        keyWorkerStaffCode = "kw001",
        keyWorkerForename = "kay",
        keyWorkerMiddleName = "m",
        keyWorkerSurname = "werker",
        departureReasonCode = "dr1",
        moveOnCategoryCode = "moc1",
        moveOnCategoryDescription = null,
        expectedArrivalDate = LocalDate.of(3000, 1, 1),
        arrivalDate = LocalDate.of(2024, 5, 2),
        expectedDepartureDate = LocalDate.of(3001, 2, 2),
        departureDate = LocalDate.of(2024, 5, 4),
        nonArrivalDate = LocalDate.of(3000, 1, 1),
        nonArrivalContactDatetime = OffsetDateTime.of(LocalDateTime.of(2024, 2, 1, 9, 58, 23), ZoneOffset.UTC),
        nonArrivalReasonCode = "narc1",
        nonArrivalReasonDescription = null,
        nonArrivalNotes = "the non arrival notes",
      ),
    )

    val booking2MinimalManagementInfoFromDelius = givenABooking(
      crn = "CRN1",
      premises = premises,
      application = application1,
      arrivalDate = LocalDate.of(2025, 5, 1),
      departureDate = LocalDate.of(2025, 5, 5),
    )
    val placementRequest2 = givenAPlacementRequest(
      application = application1,
      placementRequestAllocatedTo = otherUser,
      assessmentAllocatedTo = otherUser,
      createdByUser = otherUser,
      booking = booking2MinimalManagementInfoFromDelius,
      essentialCriteria = emptyList(),
    ).first
    val (booking2CreatedByUser) = givenAUser()
    cas1BookingDomainEventSet.bookingMade(
      application1,
      booking2MinimalManagementInfoFromDelius,
      booking2CreatedByUser,
      placementRequest2,
    )

    deliusBookingImportRepository.save(
      Cas1DeliusBookingImportEntity(
        bookingId = booking2MinimalManagementInfoFromDelius.id,
        crn = "irrelevant",
        eventNumber = "irrelevant",
        keyWorkerStaffCode = null,
        keyWorkerForename = null,
        keyWorkerMiddleName = null,
        keyWorkerSurname = null,
        departureReasonCode = null,
        moveOnCategoryCode = null,
        moveOnCategoryDescription = null,
        expectedArrivalDate = LocalDate.of(3000, 1, 1),
        arrivalDate = null,
        expectedDepartureDate = LocalDate.of(3001, 2, 2),
        departureDate = null,
        nonArrivalDate = null,
        nonArrivalContactDatetime = null,
        nonArrivalReasonCode = null,
        nonArrivalReasonDescription = null,
        nonArrivalNotes = null,
      ),
    )

    val application2 = givenACas1Application(createdByUser = otherUser, eventNumber = "50")
    val booking3ManagementInfoFromLegacyBooking = givenABooking(
      crn = "CRN2",
      premises = premises,
      application = application2,
      arrivalDate = LocalDate.of(2024, 6, 1),
      departureDate = LocalDate.of(2024, 6, 5),
      departureReason = departureReasonEntityFactory.produceAndPersist(),
      departureNotes = "the legacy departure notes",
      departureMoveOnCategory = moveOnCategoryEntityFactory.produceAndPersist(),
      nonArrivalReason = nonArrivalReasonEntityFactory.produceAndPersist(),
      nonArrivalConfirmedAt = OffsetDateTime.of(LocalDateTime.of(2024, 5, 1, 2, 3, 4), ZoneOffset.UTC),
      nonArrivalNotes = "the legacy non arrival notes",
      actualArrivalDate = LocalDateTime.of(2024, 6, 3, 20, 0, 5),
      actualDepartureDate = LocalDateTime.of(2024, 6, 6, 9, 48, 0),
    )
    val placementRequest3 = givenAPlacementRequest(
      application = application2,
      placementRequestAllocatedTo = otherUser,
      assessmentAllocatedTo = otherUser,
      createdByUser = otherUser,
      booking = booking3ManagementInfoFromLegacyBooking,
      essentialCriteria = listOf(),
    ).first
    val (booking3CreatedByUser) = givenAUser()
    cas1BookingDomainEventSet.bookingMade(
      application2,
      booking3ManagementInfoFromLegacyBooking,
      booking3CreatedByUser,
      placementRequest3,
    )
    val cancellationReason = cancellationReasonEntityFactory.produceAndPersist()
    cancellationEntityFactory.produceAndPersist {
      withBooking(booking3ManagementInfoFromLegacyBooking)
      withCreatedAt(OffsetDateTime.of(LocalDateTime.of(2025, 1, 2, 3, 4, 5), ZoneOffset.UTC))
      withDate(LocalDate.of(2024, 1, 1))
      withReason(cancellationReason)
      withOtherReason("cancellation other reason")
    }
    dateChangeEntityFactory.produceAndPersist {
      withBooking(booking3ManagementInfoFromLegacyBooking)
      withChangedByUser(booking3CreatedByUser)
    }

    val offlineApplication = givenAnOfflineApplication(
      crn = "CRN3",
      eventNumber = "75",
    )
    val booking4OfflineApplicationNoManagementInfo = givenABookingForAnOfflineApplication(
      crn = "CRN3",
      premises = premises,
      offlineApplication = offlineApplication,
      arrivalDate = LocalDate.of(2024, 7, 1),
      departureDate = LocalDate.of(2024, 7, 5),
    )
    val (booking4CreatedByUser) = givenAUser()
    createBookingMadeDomainEventForAdhocBooking(
      offlineApplication = offlineApplication,
      booking = booking4OfflineApplicationNoManagementInfo,
      createdByUser = booking4CreatedByUser,
    )

    val booking5OfflineNoDomainEventOrManagementInfo = givenABookingForAnOfflineApplication(
      crn = "CRN4",
      premises = premises,
      offlineApplication = offlineApplication,
      arrivalDate = LocalDate.of(2024, 8, 1),
      departureDate = LocalDate.of(2024, 8, 5),
    )

    val booking6DifferentPremise = givenABookingForAnOfflineApplication(
      crn = "CRN5",
      premises = otherPremise,
      offlineApplication = offlineApplication,
      arrivalDate = LocalDate.of(2025, 8, 1),
      departureDate = LocalDate.of(2025, 8, 5),
    )

    withCsv(
      "valid-csv",
      rowsToCsv(listOf(Cas1BookingToSpaceBookingSeedCsvRow(premises.id))),
    )

    seedService.seedData(SeedFileType.approvedPremisesBookingToSpaceBooking, "valid-csv.csv")

    val premiseSpaceBookings = cas1SpaceBookingTestRepository.findByPremisesId(premises.id)
    assertThat(premiseSpaceBookings).hasSize(5)

    val migratedBooking1 = premiseSpaceBookings[0]
    assertBookingIsDeleted(booking1ManagementInfoFromDelius.id)
    assertThat(migratedBooking1.id).isEqualTo(booking1ManagementInfoFromDelius.id)
    assertThat(migratedBooking1.premises.id).isEqualTo(premises.id)
    assertThat(migratedBooking1.placementRequest!!.id).isEqualTo(placementRequest1.id)
    assertThat(migratedBooking1.createdBy!!.id).isEqualTo(booking1CreatedByUser.id)
    assertThat(migratedBooking1.expectedArrivalDate).isEqualTo(LocalDate.of(2024, 5, 1))
    assertThat(migratedBooking1.expectedDepartureDate).isEqualTo(LocalDate.of(2024, 5, 5))
    assertThat(migratedBooking1.actualArrivalDate).isEqualTo(LocalDate.parse("2024-05-02"))
    assertThat(migratedBooking1.actualArrivalTime).isNull()
    assertThat(migratedBooking1.actualDepartureDate).isEqualTo(LocalDate.parse("2024-05-04"))
    assertThat(migratedBooking1.actualDepartureTime).isNull()
    assertThat(migratedBooking1.canonicalArrivalDate).isEqualTo(LocalDate.of(2024, 5, 2))
    assertThat(migratedBooking1.canonicalDepartureDate).isEqualTo(LocalDate.of(2024, 5, 4))
    assertThat(migratedBooking1.crn).isEqualTo("CRN1")
    assertThat(migratedBooking1.keyWorkerName).isEqualTo("kay werker")
    assertThat(migratedBooking1.keyWorkerStaffCode).isEqualTo("kw001")
    assertThat(migratedBooking1.keyWorkerAssignedAt).isNull()
    assertThat(migratedBooking1.application!!.id).isEqualTo(application1.id)
    assertThat(migratedBooking1.offlineApplication).isNull()
    assertThat(migratedBooking1.cancellationReason).isNull()
    assertThat(migratedBooking1.cancellationOccurredAt).isNull()
    assertThat(migratedBooking1.cancellationRecordedAt).isNull()
    assertThat(migratedBooking1.cancellationReasonNotes).isNull()
    assertThat(migratedBooking1.departureReason).isEqualTo(departureReasonActive)
    assertThat(migratedBooking1.departureMoveOnCategory).isEqualTo(moveOnCategory)
    assertThat(migratedBooking1.criteria).containsOnly(*roomCriteriaOfInterest.toTypedArray())
    assertThat(migratedBooking1.nonArrivalReason).isEqualTo(nonArrivalReasonCode)
    assertThat(migratedBooking1.nonArrivalConfirmedAt).isEqualTo(Instant.parse("2024-02-01T09:58:23.00Z"))
    assertThat(migratedBooking1.nonArrivalNotes).isEqualTo("the non arrival notes")
    assertThat(migratedBooking1.deliusEventNumber).isEqualTo("25")
    assertThat(migratedBooking1.migratedManagementInfoFrom).isEqualTo(ManagementInfoSource.DELIUS)

    val migratedBooking2 = premiseSpaceBookings[1]
    assertBookingIsDeleted(booking1ManagementInfoFromDelius.id)
    assertThat(migratedBooking2.id).isEqualTo(booking2MinimalManagementInfoFromDelius.id)
    assertThat(migratedBooking2.premises.id).isEqualTo(premises.id)
    assertThat(migratedBooking2.placementRequest!!.id).isEqualTo(placementRequest2.id)
    assertThat(migratedBooking2.createdBy!!.id).isEqualTo(booking2CreatedByUser.id)
    assertThat(migratedBooking2.expectedArrivalDate).isEqualTo(LocalDate.of(2025, 5, 1))
    assertThat(migratedBooking2.expectedDepartureDate).isEqualTo(LocalDate.of(2025, 5, 5))
    assertThat(migratedBooking2.actualArrivalDate).isNull()
    assertThat(migratedBooking2.actualArrivalTime).isNull()
    assertThat(migratedBooking2.actualDepartureDate).isNull()
    assertThat(migratedBooking2.actualDepartureTime).isNull()
    assertThat(migratedBooking2.canonicalArrivalDate).isEqualTo(LocalDate.of(2025, 5, 1))
    assertThat(migratedBooking2.canonicalDepartureDate).isEqualTo(LocalDate.of(2025, 5, 5))
    assertThat(migratedBooking2.crn).isEqualTo("CRN1")
    assertThat(migratedBooking2.keyWorkerName).isNull()
    assertThat(migratedBooking2.keyWorkerStaffCode).isNull()
    assertThat(migratedBooking2.keyWorkerAssignedAt).isNull()
    assertThat(migratedBooking2.application!!.id).isEqualTo(application1.id)
    assertThat(migratedBooking2.offlineApplication).isNull()
    assertThat(migratedBooking2.cancellationReason).isNull()
    assertThat(migratedBooking2.cancellationOccurredAt).isNull()
    assertThat(migratedBooking2.cancellationRecordedAt).isNull()
    assertThat(migratedBooking2.cancellationReasonNotes).isNull()
    assertThat(migratedBooking2.departureReason).isNull()
    assertThat(migratedBooking2.departureMoveOnCategory).isNull()
    assertThat(migratedBooking2.criteria).isEmpty()
    assertThat(migratedBooking2.nonArrivalReason).isNull()
    assertThat(migratedBooking2.nonArrivalConfirmedAt).isNull()
    assertThat(migratedBooking2.nonArrivalNotes).isNull()
    assertThat(migratedBooking1.deliusEventNumber).isEqualTo("25")
    assertThat(migratedBooking2.migratedManagementInfoFrom).isEqualTo(ManagementInfoSource.DELIUS)

    val migratedBooking3 = premiseSpaceBookings[2]
    assertBookingIsDeleted(booking3ManagementInfoFromLegacyBooking.id)
    assertThat(migratedBooking3.id).isEqualTo(booking3ManagementInfoFromLegacyBooking.id)
    assertThat(migratedBooking3.premises.id).isEqualTo(premises.id)
    assertThat(migratedBooking3.placementRequest!!.id).isEqualTo(placementRequest3.id)
    assertThat(migratedBooking3.createdBy!!.id).isEqualTo(booking3CreatedByUser.id)
    assertThat(migratedBooking3.expectedArrivalDate).isEqualTo(LocalDate.of(2024, 6, 1))
    assertThat(migratedBooking3.expectedDepartureDate).isEqualTo(LocalDate.of(2024, 6, 5))
    assertThat(migratedBooking3.actualArrivalDate).isEqualTo(LocalDate.parse("2024-06-03"))
    assertThat(migratedBooking3.actualArrivalTime).isEqualTo(LocalTime.parse("20:00:05"))
    assertThat(migratedBooking3.actualDepartureDate).isEqualTo(LocalDate.parse("2024-06-06"))
    assertThat(migratedBooking3.actualDepartureTime).isEqualTo(LocalTime.parse("09:48:00"))
    assertThat(migratedBooking3.canonicalArrivalDate).isEqualTo(LocalDate.of(2024, 6, 3))
    assertThat(migratedBooking3.canonicalDepartureDate).isEqualTo(LocalDate.of(2024, 6, 6))
    assertThat(migratedBooking3.crn).isEqualTo("CRN2")
    assertThat(migratedBooking3.keyWorkerName).isNull()
    assertThat(migratedBooking3.keyWorkerStaffCode).isNull()
    assertThat(migratedBooking3.keyWorkerAssignedAt).isNull()
    assertThat(migratedBooking3.application!!.id).isEqualTo(application2.id)
    assertThat(migratedBooking3.offlineApplication).isNull()
    assertThat(migratedBooking3.cancellationReason).isEqualTo(cancellationReason)
    assertThat(migratedBooking3.cancellationOccurredAt).isEqualTo(LocalDate.of(2024, 1, 1))
    assertThat(migratedBooking3.cancellationRecordedAt).isEqualTo(LocalDateTime.of(2025, 1, 2, 3, 4, 5).toInstant(ZoneOffset.UTC))
    assertThat(migratedBooking3.cancellationReasonNotes).isEqualTo("cancellation other reason")
    assertThat(migratedBooking3.departureReason).isEqualTo(booking3ManagementInfoFromLegacyBooking.departure!!.reason)
    assertThat(migratedBooking3.departureMoveOnCategory).isEqualTo(booking3ManagementInfoFromLegacyBooking.departure!!.moveOnCategory)
    assertThat(migratedBooking3.departureNotes).isEqualTo("the legacy departure notes")
    assertThat(migratedBooking3.criteria).isEmpty()
    assertThat(migratedBooking3.nonArrivalReason).isEqualTo(booking3ManagementInfoFromLegacyBooking.nonArrival!!.reason)
    assertThat(migratedBooking3.nonArrivalConfirmedAt).isEqualTo(Instant.parse("2024-05-01T02:03:04Z"))
    assertThat(migratedBooking3.nonArrivalNotes).isEqualTo("the legacy non arrival notes")
    assertThat(migratedBooking3.deliusEventNumber).isEqualTo("50")
    assertThat(migratedBooking3.migratedManagementInfoFrom).isEqualTo(ManagementInfoSource.LEGACY_CAS_1)

    val migratedBooking4 = premiseSpaceBookings[3]
    assertBookingIsDeleted(booking4OfflineApplicationNoManagementInfo.id)
    assertThat(migratedBooking4.id).isEqualTo(booking4OfflineApplicationNoManagementInfo.id)
    assertThat(migratedBooking4.premises.id).isEqualTo(premises.id)
    assertThat(migratedBooking4.placementRequest).isNull()
    assertThat(migratedBooking4.createdBy!!.id).isEqualTo(booking4CreatedByUser.id)
    assertThat(migratedBooking4.expectedArrivalDate).isEqualTo(LocalDate.of(2024, 7, 1))
    assertThat(migratedBooking4.expectedDepartureDate).isEqualTo(LocalDate.of(2024, 7, 5))
    assertThat(migratedBooking4.actualArrivalDate).isNull()
    assertThat(migratedBooking4.actualArrivalTime).isNull()
    assertThat(migratedBooking4.actualDepartureDate).isNull()
    assertThat(migratedBooking4.actualDepartureTime).isNull()
    assertThat(migratedBooking4.canonicalArrivalDate).isEqualTo(LocalDate.of(2024, 7, 1))
    assertThat(migratedBooking4.canonicalDepartureDate).isEqualTo(LocalDate.of(2024, 7, 5))
    assertThat(migratedBooking4.crn).isEqualTo("CRN3")
    assertThat(migratedBooking4.keyWorkerName).isNull()
    assertThat(migratedBooking4.keyWorkerStaffCode).isNull()
    assertThat(migratedBooking4.keyWorkerAssignedAt).isNull()
    assertThat(migratedBooking4.application).isNull()
    assertThat(migratedBooking4.offlineApplication!!.id).isEqualTo(offlineApplication.id)
    assertThat(migratedBooking4.cancellationReason).isNull()
    assertThat(migratedBooking4.cancellationOccurredAt).isNull()
    assertThat(migratedBooking4.cancellationRecordedAt).isNull()
    assertThat(migratedBooking4.cancellationReasonNotes).isNull()
    assertThat(migratedBooking4.departureReason).isNull()
    assertThat(migratedBooking4.departureMoveOnCategory).isNull()
    assertThat(migratedBooking4.nonArrivalReason).isNull()
    assertThat(migratedBooking4.nonArrivalConfirmedAt).isNull()
    assertThat(migratedBooking4.nonArrivalNotes).isNull()
    assertThat(migratedBooking4.criteria).isEmpty()
    assertThat(migratedBooking4.deliusEventNumber).isEqualTo("75")
    assertThat(migratedBooking4.migratedManagementInfoFrom).isEqualTo(ManagementInfoSource.LEGACY_CAS_1)

    val migratedBooking5 = premiseSpaceBookings[4]
    assertBookingIsDeleted(booking5OfflineNoDomainEventOrManagementInfo.id)
    assertThat(migratedBooking5.id).isEqualTo(booking5OfflineNoDomainEventOrManagementInfo.id)
    assertThat(migratedBooking5.premises.id).isEqualTo(premises.id)
    assertThat(migratedBooking5.placementRequest).isNull()
    assertThat(migratedBooking5.createdBy).isNull()
    assertThat(migratedBooking5.expectedArrivalDate).isEqualTo(LocalDate.of(2024, 8, 1))
    assertThat(migratedBooking5.expectedDepartureDate).isEqualTo(LocalDate.of(2024, 8, 5))
    assertThat(migratedBooking5.actualArrivalDate).isNull()
    assertThat(migratedBooking5.actualArrivalTime).isNull()
    assertThat(migratedBooking5.actualDepartureDate).isNull()
    assertThat(migratedBooking5.actualDepartureTime).isNull()
    assertThat(migratedBooking5.canonicalArrivalDate).isEqualTo(LocalDate.of(2024, 8, 1))
    assertThat(migratedBooking5.canonicalDepartureDate).isEqualTo(LocalDate.of(2024, 8, 5))
    assertThat(migratedBooking5.crn).isEqualTo("CRN4")
    assertThat(migratedBooking5.keyWorkerName).isNull()
    assertThat(migratedBooking5.keyWorkerStaffCode).isNull()
    assertThat(migratedBooking5.keyWorkerAssignedAt).isNull()
    assertThat(migratedBooking5.application).isNull()
    assertThat(migratedBooking5.offlineApplication!!.id).isEqualTo(offlineApplication.id)
    assertThat(migratedBooking5.cancellationReason).isNull()
    assertThat(migratedBooking5.cancellationOccurredAt).isNull()
    assertThat(migratedBooking5.cancellationRecordedAt).isNull()
    assertThat(migratedBooking5.cancellationReasonNotes).isNull()
    assertThat(migratedBooking5.departureReason).isNull()
    assertThat(migratedBooking5.departureMoveOnCategory).isNull()
    assertThat(migratedBooking5.nonArrivalReason).isNull()
    assertThat(migratedBooking5.nonArrivalConfirmedAt).isNull()
    assertThat(migratedBooking5.nonArrivalNotes).isNull()
    assertThat(migratedBooking5.criteria).isEmpty()
    assertThat(migratedBooking5.deliusEventNumber).isNull()
    assertThat(migratedBooking5.migratedManagementInfoFrom).isEqualTo(ManagementInfoSource.LEGACY_CAS_1)

    assertBookingIsNotDeleted(booking6DifferentPremise.id)
  }

  private fun createBookingMadeDomainEventForAdhocBooking(
    offlineApplication: OfflineApplicationEntity,
    booking: BookingEntity,
    createdByUser: UserEntity,
  ) {
    val applicationId = offlineApplication.id
    val eventNumber = offlineApplication.eventNumber!!
    val domainEventId = UUID.randomUUID()
    val bookingId = booking.id

    cas1DomainEventService.saveBookingMadeDomainEvent(
      DomainEvent(
        id = domainEventId,
        applicationId = applicationId,
        crn = "irrelevant",
        nomsNumber = "irrelevant",
        occurredAt = Instant.now(),
        bookingId = bookingId,
        cas1SpaceBookingId = null,
        data = BookingMadeEnvelope(
          id = domainEventId,
          timestamp = Instant.now(),
          eventType = EventType.bookingMade,
          eventDetails = BookingMade(
            applicationId = applicationId,
            applicationUrl = "doesnt matter",
            bookingId = bookingId,
            personReference = PersonReference(
              crn = "irrelevant",
              noms = "irrelevant",
            ),
            deliusEventNumber = eventNumber,
            createdAt = Instant.now(),
            bookedBy = BookingMadeBookedBy(
              staffMember = StaffMember(
                staffCode = "doesnt matter",
                forenames = "doesnt matter",
                surname = "doesnt matter",
                username = createdByUser.deliusUsername,
              ),
              cru = Cru(
                name = "irrelevant",
              ),
            ),
            premises = Premises(
              id = UUID.randomUUID(),
              name = "irrelevant",
              apCode = "irrelevant",
              legacyApCode = "irrelevant",
              localAuthorityAreaName = "irrelevant",
            ),
            arrivalOn = LocalDate.now(),
            departureOn = LocalDate.now(),
            applicationSubmittedOn = Instant.now(),
            releaseType = "irrelevant",
            sentenceType = "irrelevant",
            situation = "irrelevant",
          ),
        ),
      ),
    )
  }

  @Test
  fun `Update domain event links from booking to space booking when migrating`() {
    val user = givenAUser().first

    val application1 = givenACas1Application(createdByUser = user, eventNumber = "25")
    val booking1InPremises = givenABooking(
      crn = "CRN1",
      premises = premises,
      application = application1,
      arrivalDate = LocalDate.of(2024, 5, 1),
      departureDate = LocalDate.of(2024, 5, 5),
    )
    val placementRequest1 = givenAPlacementRequest(
      application = application1,
      placementRequestAllocatedTo = user,
      assessmentAllocatedTo = user,
      createdByUser = user,
      booking = booking1InPremises,
    ).first
    val (booking1CreatedByUser) = givenAUser()
    cas1BookingDomainEventSet.bookingMade(
      application1,
      booking1InPremises,
      booking1CreatedByUser,
      placementRequest1,
    )

    val booking2InOtherPremises = givenABooking(
      crn = "CRN1",
      premises = otherPremise,
      application = application1,
      arrivalDate = LocalDate.of(2024, 5, 1),
      departureDate = LocalDate.of(2024, 5, 5),
    )
    val placementRequest2 = givenAPlacementRequest(
      application = application1,
      placementRequestAllocatedTo = user,
      assessmentAllocatedTo = user,
      createdByUser = user,
      booking = booking2InOtherPremises,
    ).first
    val (booking2CreatedByUser) = givenAUser()
    cas1BookingDomainEventSet.bookingMade(
      application1,
      booking2InOtherPremises,
      booking2CreatedByUser,
      placementRequest2,
    )

    withCsv(
      "valid-csv",
      rowsToCsv(listOf(Cas1BookingToSpaceBookingSeedCsvRow(premises.id))),
    )

    seedService.seedData(SeedFileType.approvedPremisesBookingToSpaceBooking, "valid-csv.csv")

    val bookingCreatedDomainEvents = domainEventRepository.findByApplicationId(application1.id)
    assertThat(bookingCreatedDomainEvents).hasSize(2)

    val premise1SpaceBooking = cas1SpaceBookingTestRepository.findByPremisesId(premises.id)[0]
    val booking1CreatedDomainEvent = bookingCreatedDomainEvents.first { it.cas1SpaceBookingId == premise1SpaceBooking.id }
    assertThat(booking1CreatedDomainEvent.bookingId).isNull()
    assertThat(booking1CreatedDomainEvent.cas1SpaceBookingId).isEqualTo(premise1SpaceBooking.id)
    assertBookingIsDeleted(booking1InPremises.id)

    assertThat(cas1SpaceBookingTestRepository.findByPremisesId(otherPremise.id)).isEmpty()
    val booking2CreatedDomainEvent = bookingCreatedDomainEvents.first { it.bookingId == booking2InOtherPremises.id }
    assertThat(booking2CreatedDomainEvent.bookingId).isEqualTo(booking2InOtherPremises.id)
    assertThat(booking2CreatedDomainEvent.cas1SpaceBookingId).isNull()
    assertBookingIsNotDeleted(booking2InOtherPremises.id)
  }

  private fun assertBookingIsDeleted(bookingId: UUID) = assertThat(bookingRepository.findByIdOrNull(bookingId)).isNull()

  private fun assertBookingIsNotDeleted(bookingId: UUID) = assertThat(bookingRepository.findByIdOrNull(bookingId)).isNotNull

  private fun rowsToCsv(rows: List<Cas1BookingToSpaceBookingSeedCsvRow>): String {
    val builder = CsvBuilder()
      .withUnquotedFields(
        "premises_id",
      )
      .newRow()

    rows.forEach {
      builder
        .withQuotedField(it.premisesId)
        .newRow()
    }

    return builder.build()
  }
}
