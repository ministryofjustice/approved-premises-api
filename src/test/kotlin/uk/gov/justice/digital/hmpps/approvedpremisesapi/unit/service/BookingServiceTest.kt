package uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.service

import io.mockk.Called
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.slot
import io.mockk.verify
import io.sentry.Sentry
import io.sentry.protocol.SentryId
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatExceptionOfType
import org.assertj.core.api.Assertions.entry
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.junit.jupiter.params.provider.EnumSource
import org.springframework.data.repository.findByIdOrNull
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Booking
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.BookingStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ReleaseTypeOption
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.SentenceTypeOption
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ServiceName
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.SituationOption
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ApAreaEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ApprovedPremisesApplicationEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ApprovedPremisesAssessmentEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ApprovedPremisesEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ArrivalEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.BedEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.BookingEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.CancellationEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.CancellationReasonEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.Cas1SpaceBookingEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ConfirmationEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ContextStaffMemberFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.DepartureEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.DepartureReasonEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.DestinationProviderEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.InmateDetailFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.LocalAuthorityEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.LostBedReasonEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.LostBedsEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.MoveOnCategoryEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.OffenderDetailsSummaryFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.OfflineApplicationEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.PlacementApplicationEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.PlacementRequestEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.PlacementRequirementsEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ProbationRegionEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.RoomEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.TemporaryAccommodationApplicationEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.TemporaryAccommodationAssessmentEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.TemporaryAccommodationPremisesEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.UserEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.UserRoleAssignmentEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApprovedPremisesApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApprovedPremisesEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ArrivalEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ArrivalRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.AssessmentRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.BedRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.BookingEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.BookingRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.CancellationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.CancellationReasonRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.CancellationRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ConfirmationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ConfirmationRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.DateChangeEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.DateChangeRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.DepartureEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.DepartureReasonRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.DepartureRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.DestinationProviderRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ExtensionEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ExtensionRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.LostBedsRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.MoveOnCategoryRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PlacementRequestEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PlacementRequestRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PremisesRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ProbationRegionEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.TemporaryAccommodationApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.TurnaroundEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.TurnaroundRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserQualification
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserRole
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.PersonInfoResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.results.AuthorisableActionResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.results.CasResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.results.ValidatableActionResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.AssessmentService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.BookingService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.DeliusService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.GetBookingForPremisesResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.OffenderService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.PremisesService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.UserAccessService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.UserService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.WorkingDayService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.BlockingReason
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.Cas1ApplicationStatusService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.Cas1BookingDomainEventService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.Cas1BookingEmailService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.WithdrawableEntityType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.WithdrawalContext
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.WithdrawalTriggeredByUser
import java.time.Instant
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.UUID
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas3.DomainEventService as Cas3DomainEventService

class BookingServiceTest {
  private val mockPremisesService = mockk<PremisesService>()
  private val mockOffenderService = mockk<OffenderService>()
  private val mockCas3DomainEventService = mockk<Cas3DomainEventService>()
  private val mockWorkingDayService = mockk<WorkingDayService>()

  private val mockBookingRepository = mockk<BookingRepository>()
  private val mockArrivalRepository = mockk<ArrivalRepository>()
  private val mockCancellationRepository = mockk<CancellationRepository>()
  private val mockConfirmationRepository = mockk<ConfirmationRepository>()
  private val mockExtensionRepository = mockk<ExtensionRepository>()
  private val mockDateChangeRepository = mockk<DateChangeRepository>()
  private val mockDepartureRepository = mockk<DepartureRepository>()
  private val mockDepartureReasonRepository = mockk<DepartureReasonRepository>()
  private val mockMoveOnCategoryRepository = mockk<MoveOnCategoryRepository>()
  private val mockDestinationProviderRepository = mockk<DestinationProviderRepository>()
  private val mockCancellationReasonRepository = mockk<CancellationReasonRepository>()
  private val mockBedRepository = mockk<BedRepository>()
  private val mockPlacementRequestRepository = mockk<PlacementRequestRepository>()
  private val mockLostBedsRepository = mockk<LostBedsRepository>()
  private val mockTurnaroundRepository = mockk<TurnaroundRepository>()
  private val mockPremisesRepository = mockk<PremisesRepository>()
  private val mockAssessmentRepository = mockk<AssessmentRepository>()
  private val mockUserService = mockk<UserService>()
  private val mockUserAccessService = mockk<UserAccessService>()
  private val mockAssessmentService = mockk<AssessmentService>()
  private val mockCas1BookingEmailService = mockk<Cas1BookingEmailService>()
  private val mockDeliusService = mockk<DeliusService>()
  private val mockCas1ApplicationStatusService = mockk<Cas1ApplicationStatusService>()
  private val mockCas1BookingDomainEventService = mockk<Cas1BookingDomainEventService>()

  fun createBookingService(): BookingService {
    return BookingService(
      premisesService = mockPremisesService,
      offenderService = mockOffenderService,
      cas3DomainEventService = mockCas3DomainEventService,
      workingDayService = mockWorkingDayService,
      bookingRepository = mockBookingRepository,
      arrivalRepository = mockArrivalRepository,
      cancellationRepository = mockCancellationRepository,
      confirmationRepository = mockConfirmationRepository,
      extensionRepository = mockExtensionRepository,
      dateChangeRepository = mockDateChangeRepository,
      departureRepository = mockDepartureRepository,
      departureReasonRepository = mockDepartureReasonRepository,
      moveOnCategoryRepository = mockMoveOnCategoryRepository,
      cancellationReasonRepository = mockCancellationReasonRepository,
      bedRepository = mockBedRepository,
      placementRequestRepository = mockPlacementRequestRepository,
      lostBedsRepository = mockLostBedsRepository,
      turnaroundRepository = mockTurnaroundRepository,
      premisesRepository = mockPremisesRepository,
      assessmentRepository = mockAssessmentRepository,
      userService = mockUserService,
      userAccessService = mockUserAccessService,
      assessmentService = mockAssessmentService,
      cas1BookingEmailService = mockCas1BookingEmailService,
      deliusService = mockDeliusService,
      cas1BookingDomainEventService = mockCas1BookingDomainEventService,
      cas1ApplicationStatusService = mockCas1ApplicationStatusService,
    )
  }

  private val bookingService = createBookingService()

  private val user = UserEntityFactory()
    .withUnitTestControlProbationRegion()
    .produce()

  @Test
  fun `getBookingForPremises returns PremisesNotFound when premises with provided ID does not exist`() {
    val premisesId = UUID.fromString("8461d08b-0e3f-426a-a941-0ada4160e6db")
    val bookingId = UUID.fromString("75ed7091-1767-4901-8c2b-371dd0f5864c")

    every { mockPremisesService.getPremises(premisesId) } returns null

    assertThat(bookingService.getBookingForPremises(premisesId, bookingId))
      .isEqualTo(GetBookingForPremisesResult.PremisesNotFound)
  }

  @Test
  fun `getBookingForPremises returns BookingNotFound when booking with provided ID does not exist`() {
    val premisesId = UUID.fromString("8461d08b-0e3f-426a-a941-0ada4160e6db")
    val bookingId = UUID.fromString("75ed7091-1767-4901-8c2b-371dd0f5864c")

    every { mockPremisesService.getPremises(premisesId) } returns ApprovedPremisesEntityFactory()
      .withId(premisesId)
      .withYieldedProbationRegion {
        ProbationRegionEntityFactory()
          .withYieldedApArea { ApAreaEntityFactory().produce() }
          .produce()
      }
      .withYieldedLocalAuthorityArea { LocalAuthorityEntityFactory().produce() }
      .produce()

    every { mockBookingRepository.findByIdOrNull(bookingId) } returns null

    assertThat(bookingService.getBookingForPremises(premisesId, bookingId))
      .isEqualTo(GetBookingForPremisesResult.BookingNotFound)
  }

  @Test
  fun `getBookingForPremises returns BookingNotFound when booking does not belong to Premises`() {
    val premisesId = UUID.fromString("8461d08b-0e3f-426a-a941-0ada4160e6db")
    val bookingId = UUID.fromString("75ed7091-1767-4901-8c2b-371dd0f5864c")

    val premisesEntityFactory = ApprovedPremisesEntityFactory()
      .withId(premisesId)
      .withYieldedProbationRegion {
        ProbationRegionEntityFactory()
          .withYieldedApArea { ApAreaEntityFactory().produce() }
          .produce()
      }
      .withYieldedLocalAuthorityArea { LocalAuthorityEntityFactory().produce() }

    every { mockPremisesService.getPremises(premisesId) } returns premisesEntityFactory.produce()

    val keyWorker = ContextStaffMemberFactory().produce()

    every { mockBookingRepository.findByIdOrNull(bookingId) } returns BookingEntityFactory()
      .withId(bookingId)
      .withPremises(premisesEntityFactory.withId(UUID.randomUUID()).produce())
      .withStaffKeyWorkerCode(keyWorker.code)
      .produce()

    assertThat(bookingService.getBookingForPremises(premisesId, bookingId))
      .isEqualTo(GetBookingForPremisesResult.BookingNotFound)
  }

  @Test
  fun `getBookingForPremises returns Success when booking does belong to Premises`() {
    val premisesId = UUID.fromString("8461d08b-0e3f-426a-a941-0ada4160e6db")
    val bookingId = UUID.fromString("75ed7091-1767-4901-8c2b-371dd0f5864c")

    val premisesEntity = ApprovedPremisesEntityFactory()
      .withId(premisesId)
      .withYieldedProbationRegion {
        ProbationRegionEntityFactory()
          .withYieldedApArea { ApAreaEntityFactory().produce() }
          .produce()
      }
      .withYieldedLocalAuthorityArea { LocalAuthorityEntityFactory().produce() }
      .produce()

    every { mockPremisesService.getPremises(premisesId) } returns premisesEntity

    val keyWorker = ContextStaffMemberFactory().produce()

    val bookingEntity = BookingEntityFactory()
      .withId(bookingId)
      .withPremises(premisesEntity)
      .withStaffKeyWorkerCode(keyWorker.code)
      .produce()

    every { mockBookingRepository.findByIdOrNull(bookingId) } returns bookingEntity

    assertThat(bookingService.getBookingForPremises(premisesId, bookingId))
      .isEqualTo(GetBookingForPremisesResult.Success(bookingEntity))
  }

  @Nested
  inner class GetBooking {
    val premises = ApprovedPremisesEntityFactory()
      .withYieldedProbationRegion {
        ProbationRegionEntityFactory()
          .withYieldedApArea { ApAreaEntityFactory().produce() }
          .produce()
      }
      .withYieldedLocalAuthorityArea { LocalAuthorityEntityFactory().produce() }
      .produce()

    private val bookingEntity = BookingEntityFactory()
      .withArrivalDate(LocalDate.parse("2022-08-25"))
      .withPremises(premises)
      .produce()

    private val personInfo = PersonInfoResult.Success.Full(
      crn = bookingEntity.crn,
      offenderDetailSummary = OffenderDetailsSummaryFactory().produce(),
      inmateDetail = InmateDetailFactory().produce(),
    )

    @Test
    fun `returns a booking`() {
      every { mockBookingRepository.findByIdOrNull(bookingEntity.id) } returns bookingEntity
      every { mockUserService.getUserForRequest() } returns user
      every { mockUserAccessService.userCanViewBooking(user, bookingEntity) } returns true
      every { mockOffenderService.getPersonInfoResult(bookingEntity.crn, user.deliusUsername, user.hasQualification(UserQualification.LAO)) } returns personInfo

      val result = bookingService.getBooking(bookingEntity.id)

      assertThat(result is AuthorisableActionResult.Success).isTrue()
      result as AuthorisableActionResult.Success

      assertThat(result.entity).isEqualTo(BookingService.BookingAndPersons(bookingEntity, personInfo))
    }

    @Test
    fun `returns NotFound if the booking cannot be found`() {
      every { mockBookingRepository.findByIdOrNull(bookingEntity.id) } returns null

      val result = bookingService.getBooking(bookingEntity.id)

      assertThat(result is AuthorisableActionResult.NotFound).isTrue()
      result as AuthorisableActionResult.NotFound

      assertThat(result.id).isEqualTo(bookingEntity.id.toString())
      assertThat(result.entityType).isEqualTo("Booking")
    }

    @Test
    fun `returns Unauthorised if the user cannot view the booking`() {
      every { mockBookingRepository.findByIdOrNull(bookingEntity.id) } returns bookingEntity
      every { mockUserService.getUserForRequest() } returns user
      every { mockUserAccessService.userCanViewBooking(user, bookingEntity) } returns false

      val result = bookingService.getBooking(bookingEntity.id)

      assertThat(result is AuthorisableActionResult.Unauthorised).isTrue()
    }
  }

  @Nested
  inner class CreateDeparture {
    val premises = TemporaryAccommodationPremisesEntityFactory()
      .withYieldedProbationRegion {
        ProbationRegionEntityFactory()
          .withYieldedApArea { ApAreaEntityFactory().produce() }
          .produce()
      }
      .withYieldedLocalAuthorityArea { LocalAuthorityEntityFactory().produce() }
      .produce()

    val keyWorker = ContextStaffMemberFactory().produce()

    private val bookingEntity = BookingEntityFactory()
      .withArrivalDate(LocalDate.parse("2022-08-25"))
      .withPremises(premises)
      .withStaffKeyWorkerCode(keyWorker.code)
      .produce()

    private val departureReasonId = UUID.fromString("6f3dad50-7246-492c-8f92-6e20540a3631")
    private val moveOnCategoryId = UUID.fromString("cb29c66d-8abc-4583-8a41-e28a43fc65c3")
    private val destinationProviderId = UUID.fromString("a6f5377e-e0c8-4122-b348-b30ba7e9d7a2")

    private val reasonEntity = DepartureReasonEntityFactory()
      .withServiceScope("temporary-accommodation")
      .produce()
    private val moveOnCategoryEntity = MoveOnCategoryEntityFactory()
      .withServiceScope("temporary-accommodation")
      .produce()
    private val destinationProviderEntity = DestinationProviderEntityFactory().produce()

    @BeforeEach
    fun setup() {
      every { mockDepartureReasonRepository.findByIdOrNull(departureReasonId) } returns reasonEntity
      every { mockMoveOnCategoryRepository.findByIdOrNull(moveOnCategoryId) } returns moveOnCategoryEntity
      every { mockDestinationProviderRepository.findByIdOrNull(destinationProviderId) } returns destinationProviderEntity

      every { mockDepartureRepository.save(any()) } answers { it.invocation.args[0] as DepartureEntity }
      every { mockBookingRepository.save(any()) } answers { it.invocation.args[0] as BookingEntity }
    }

    @Test
    fun `createDeparture returns FieldValidationError with correct param to message map when dateTime in past supplied`() {
      val result = bookingService.createDeparture(
        booking = bookingEntity,
        dateTime = OffsetDateTime.parse("2022-08-24T15:00:00+01:00"),
        reasonId = departureReasonId,
        moveOnCategoryId = moveOnCategoryId,
        notes = "notes",
        user = UserEntityFactory()
          .withUnitTestControlProbationRegion()
          .produce(),
      )

      assertThat(result).isInstanceOf(ValidatableActionResult.FieldValidationError::class.java)
      assertThat((result as ValidatableActionResult.FieldValidationError).validationMessages).contains(
        entry("$.dateTime", "beforeBookingArrivalDate"),
      )
    }

    @Test
    fun `createDeparture returns FieldValidationError with correct param to message map when invalid departure reason supplied`() {
      every { mockDepartureReasonRepository.findByIdOrNull(any()) } returns null

      val result = bookingService.createDeparture(
        booking = bookingEntity,
        dateTime = OffsetDateTime.now().minusMinutes(1),
        reasonId = UUID.randomUUID(),
        moveOnCategoryId = moveOnCategoryId,
        notes = "notes",
        user = UserEntityFactory()
          .withUnitTestControlProbationRegion()
          .produce(),
      )

      assertThat(result).isInstanceOf(ValidatableActionResult.FieldValidationError::class.java)
      assertThat((result as ValidatableActionResult.FieldValidationError).validationMessages).contains(
        entry("$.reasonId", "doesNotExist"),
      )
    }

    @Test
    fun `createDeparture returns FieldValidationError with correct param to message map when the departure reason has the wrong service scope`() {
      every { mockDepartureReasonRepository.findByIdOrNull(any()) } returns DepartureReasonEntityFactory()
        .withServiceScope(ServiceName.temporaryAccommodation.value)
        .produce()

      val result = bookingService.createDeparture(
        booking = bookingEntity,
        dateTime = OffsetDateTime.now().minusMinutes(1),
        reasonId = departureReasonId,
        moveOnCategoryId = moveOnCategoryId,
        notes = "notes",
        user = UserEntityFactory()
          .withUnitTestControlProbationRegion()
          .produce(),
      )

      assertThat(result).isInstanceOf(ValidatableActionResult.FieldValidationError::class.java)
      assertThat((result as ValidatableActionResult.FieldValidationError).validationMessages).contains(
        entry("$.reasonId", "incorrectDepartureReasonServiceScope"),
      )
    }

    @Test
    fun `createDeparture returns FieldValidationError with correct param to message map when invalid move on category supplied`() {
      every { mockMoveOnCategoryRepository.findByIdOrNull(any()) } returns null

      val result = bookingService.createDeparture(
        booking = bookingEntity,
        dateTime = OffsetDateTime.now().plusMinutes(1),
        reasonId = departureReasonId,
        moveOnCategoryId = UUID.randomUUID(),
        notes = "notes",
        user = UserEntityFactory()
          .withUnitTestControlProbationRegion()
          .produce(),
      )

      assertThat(result).isInstanceOf(ValidatableActionResult.FieldValidationError::class.java)
      assertThat((result as ValidatableActionResult.FieldValidationError).validationMessages).contains(
        entry("$.moveOnCategoryId", "doesNotExist"),
      )
    }

    @Test
    fun `createDeparture returns FieldValidationError with correct param to message map when the move-on category has the wrong service scope`() {
      every { mockDepartureReasonRepository.findByIdOrNull(departureReasonId) } returns DepartureReasonEntityFactory()
        .withServiceScope("*")
        .produce()
      every { mockMoveOnCategoryRepository.findByIdOrNull(moveOnCategoryId) } returns MoveOnCategoryEntityFactory()
        .withServiceScope(ServiceName.temporaryAccommodation.value)
        .produce()
      every { mockDestinationProviderRepository.findByIdOrNull(destinationProviderId) } returns DestinationProviderEntityFactory().produce()

      val result = bookingService.createDeparture(
        booking = bookingEntity,
        dateTime = OffsetDateTime.now().plusMinutes(1),
        reasonId = departureReasonId,
        moveOnCategoryId = moveOnCategoryId,
        notes = "notes",
        user = UserEntityFactory()
          .withUnitTestControlProbationRegion()
          .produce(),
      )

      assertThat(result).isInstanceOf(ValidatableActionResult.FieldValidationError::class.java)
      assertThat((result as ValidatableActionResult.FieldValidationError).validationMessages).contains(
        entry("$.moveOnCategoryId", "incorrectMoveOnCategoryServiceScope"),
      )
    }

    @Test
    fun `createDeparture for a CAS3 booking returns Success with correct result when validation passed and saves a domain event`() {
      val departureReasonId = UUID.fromString("6f3dad50-7246-492c-8f92-6e20540a3631")
      val moveOnCategoryId = UUID.fromString("cb29c66d-8abc-4583-8a41-e28a43fc65c3")

      val probationRegion = ProbationRegionEntityFactory()
        .withYieldedApArea { ApAreaEntityFactory().produce() }
        .produce()

      val bookingEntity = createTemporaryAccommodationBooking(probationRegion)

      val reasonEntity = DepartureReasonEntityFactory()
        .withServiceScope("temporary-accommodation")
        .produce()
      val moveOnCategoryEntity = MoveOnCategoryEntityFactory()
        .withServiceScope("temporary-accommodation")
        .produce()

      every { mockDepartureReasonRepository.findByIdOrNull(departureReasonId) } returns reasonEntity
      every { mockMoveOnCategoryRepository.findByIdOrNull(moveOnCategoryId) } returns moveOnCategoryEntity

      every { mockDepartureRepository.save(any()) } answers { it.invocation.args[0] as DepartureEntity }

      every { mockArrivalRepository.save(any()) } answers { it.invocation.args[0] as ArrivalEntity }
      every { mockBookingRepository.save(any()) } answers { it.invocation.args[0] as BookingEntity }

      val user = UserEntityFactory()
        .withProbationRegion(probationRegion)
        .produce()

      val offenderDetails = OffenderDetailsSummaryFactory()
        .withCrn(bookingEntity.crn)
        .produce()

      every { mockOffenderService.getOffenderByCrn(bookingEntity.crn, user.deliusUsername) } returns AuthorisableActionResult.Success(offenderDetails)

      every { mockCas3DomainEventService.savePersonDepartedEvent(any(), user) } just Runs

      val result = bookingService.createDeparture(
        booking = bookingEntity,
        dateTime = OffsetDateTime.parse("2022-08-24T15:00:00+01:00"),
        reasonId = departureReasonId,
        moveOnCategoryId = moveOnCategoryId,
        notes = "notes",
        user = user,
      )

      assertThat(result).isInstanceOf(ValidatableActionResult.Success::class.java)
      result as ValidatableActionResult.Success
      assertThat(result.entity.booking).isEqualTo(bookingEntity)
      assertThat(result.entity.dateTime).isEqualTo(OffsetDateTime.parse("2022-08-24T15:00:00+01:00"))
      assertThat(result.entity.reason).isEqualTo(reasonEntity)
      assertThat(result.entity.moveOnCategory).isEqualTo(moveOnCategoryEntity)
      assertThat(result.entity.destinationProvider).isEqualTo(null)
      assertThat(result.entity.reason).isEqualTo(reasonEntity)
      assertThat(result.entity.notes).isEqualTo("notes")
      assertThat(result.entity.booking.status).isEqualTo(BookingStatus.departed)

      verify(exactly = 1) {
        mockCas3DomainEventService.savePersonDepartedEvent(bookingEntity, user)
      }
      verify(exactly = 1) {
        mockCas3DomainEventService.savePersonDepartedEvent(any(), user)
      }
      verify(exactly = 0) {
        mockCas3DomainEventService.savePersonDepartureUpdatedEvent(any(), user)
      }
    }

    @Test
    fun `createDeparture for a CAS3 booking successfully create departure update event and saves a domain event`() {
      val departureReasonId = UUID.fromString("6f3dad50-7246-492c-8f92-6e20540a3631")
      val moveOnCategoryId = UUID.fromString("cb29c66d-8abc-4583-8a41-e28a43fc65c3")
      val departureDateTime = OffsetDateTime.parse("2022-08-24T15:00:00+01:00")
      val notes = "Some notes about the departure"
      val probationRegion = ProbationRegionEntityFactory()
        .withYieldedApArea { ApAreaEntityFactory().produce() }
        .produce()

      val bookingEntity = createTemporaryAccommodationBooking(probationRegion)

      val reasonEntity = DepartureReasonEntityFactory()
        .withServiceScope("temporary-accommodation")
        .produce()
      val moveOnCategoryEntity = MoveOnCategoryEntityFactory()
        .withServiceScope("temporary-accommodation")
        .produce()

      val user = UserEntityFactory()
        .withProbationRegion(probationRegion)
        .produce()

      val offenderDetails = OffenderDetailsSummaryFactory()
        .withCrn(bookingEntity.crn)
        .produce()

      val departureEntity = DepartureEntityFactory()
        .withBooking(bookingEntity)
        .withDateTime(departureDateTime)
        .withReason(reasonEntity)
        .withMoveOnCategory(moveOnCategoryEntity)
        .withNotes(notes)
        .produce()
      bookingEntity.departures += departureEntity

      every { mockDepartureReasonRepository.findByIdOrNull(departureReasonId) } returns reasonEntity
      every { mockMoveOnCategoryRepository.findByIdOrNull(moveOnCategoryId) } returns moveOnCategoryEntity
      every { mockDepartureRepository.save(any()) } answers { it.invocation.args[0] as DepartureEntity }
      every { mockArrivalRepository.save(any()) } answers { it.invocation.args[0] as ArrivalEntity }
      every { mockBookingRepository.save(any()) } answers { it.invocation.args[0] as BookingEntity }
      every { mockOffenderService.getOffenderByCrn(bookingEntity.crn, user.deliusUsername) } returns AuthorisableActionResult.Success(offenderDetails)
      every { mockCas3DomainEventService.savePersonDepartureUpdatedEvent(any(), user) } just Runs

      val result = bookingService.createDeparture(
        booking = bookingEntity,
        dateTime = departureDateTime,
        reasonId = departureReasonId,
        moveOnCategoryId = moveOnCategoryId,
        notes = notes,
        user = user,
      )

      assertThat(result).isInstanceOf(ValidatableActionResult.Success::class.java)
      result as ValidatableActionResult.Success
      assertThat(result.entity.booking).isEqualTo(bookingEntity)
      assertThat(result.entity.dateTime).isEqualTo(departureDateTime)
      assertThat(result.entity.reason).isEqualTo(reasonEntity)
      assertThat(result.entity.moveOnCategory).isEqualTo(moveOnCategoryEntity)
      assertThat(result.entity.destinationProvider).isEqualTo(null)
      assertThat(result.entity.reason).isEqualTo(reasonEntity)
      assertThat(result.entity.notes).isEqualTo(notes)
      assertThat(result.entity.booking.status).isEqualTo(BookingStatus.departed)

      verify(exactly = 1) {
        mockCas3DomainEventService.savePersonDepartureUpdatedEvent(any(), user)
      }
      verify(exactly = 0) {
        mockCas3DomainEventService.savePersonDepartedEvent(any(), user)
      }
    }
  }

  private fun createTemporaryAccommodationBooking(probationRegion: ProbationRegionEntity): BookingEntity {
    val bookingEntity = BookingEntityFactory()
      .withArrivalDate(LocalDate.parse("2022-08-22"))
      .withOfflineApplication(OfflineApplicationEntityFactory().produce())
      .withYieldedPremises {
        TemporaryAccommodationPremisesEntityFactory()
          .withProbationRegion(probationRegion)
          .withYieldedLocalAuthorityArea { LocalAuthorityEntityFactory().produce() }
          .produce()
      }
      .withApplication(
        TemporaryAccommodationApplicationEntityFactory()
          .withSubmittedAt(OffsetDateTime.parse("2023-02-15T15:00:00Z"))
          .withProbationRegion(probationRegion)
          .withCreatedByUser(
            UserEntityFactory()
              .withProbationRegion(probationRegion)
              .produce(),
          )
          .produce(),
      )
      .withServiceName(ServiceName.temporaryAccommodation)
      .produce()
    return bookingEntity
  }

  @Nested
  inner class CreateArrival {
    @Test
    fun `createArrival returns GeneralValidationError with correct message when Booking already has an Arrival`() {
      val bookingEntity = BookingEntityFactory()
        .withYieldedPremises {
          ApprovedPremisesEntityFactory()
            .withYieldedProbationRegion {
              ProbationRegionEntityFactory()
                .withYieldedApArea { ApAreaEntityFactory().produce() }
                .produce()
            }
            .withYieldedLocalAuthorityArea { LocalAuthorityEntityFactory().produce() }
            .produce()
        }
        .withStaffKeyWorkerCode("123")
        .produce()

      val arrivalEntity = ArrivalEntityFactory()
        .withBooking(bookingEntity)
        .produce()

      bookingEntity.arrivals += arrivalEntity

      val result = bookingService.createArrival(
        booking = bookingEntity,
        arrivalDate = LocalDate.parse("2022-08-25"),
        expectedDepartureDate = LocalDate.parse("2022-08-26"),
        notes = "notes",
        keyWorkerStaffCode = "123",
        user = UserEntityFactory()
          .withUnitTestControlProbationRegion()
          .produce(),
      )

      assertThat(result).isInstanceOf(ValidatableActionResult.GeneralValidationError::class.java)
      assertThat((result as ValidatableActionResult.GeneralValidationError).message).isEqualTo("This Booking already has an Arrival set")
    }

    @Test
    fun `createArrival returns FieldValidationError with correct param to message map when invalid parameters supplied`() {
      val keyWorker = ContextStaffMemberFactory().produce()

      val bookingEntity = BookingEntityFactory()
        .withYieldedPremises {
          ApprovedPremisesEntityFactory()
            .withYieldedProbationRegion {
              ProbationRegionEntityFactory()
                .withYieldedApArea { ApAreaEntityFactory().produce() }
                .produce()
            }
            .withYieldedLocalAuthorityArea { LocalAuthorityEntityFactory().produce() }
            .produce()
        }
        .produce()

      val result = bookingService.createArrival(
        booking = bookingEntity,
        arrivalDate = LocalDate.parse("2022-08-27"),
        expectedDepartureDate = LocalDate.parse("2022-08-26"),
        notes = "notes",
        keyWorkerStaffCode = keyWorker.code,
        user = UserEntityFactory()
          .withUnitTestControlProbationRegion()
          .produce(),
      )

      assertThat(result).isInstanceOf(ValidatableActionResult.FieldValidationError::class.java)
      assertThat((result as ValidatableActionResult.FieldValidationError).validationMessages).contains(
        entry("$.expectedDepartureDate", "beforeBookingArrivalDate"),
      )
    }

    @Test
    fun `createArrival returns GeneralValidationError with correct message when Booking is CAS3 `() {
      val bookingEntity = BookingEntityFactory()
        .withYieldedPremises {
          TemporaryAccommodationPremisesEntityFactory()
            .withYieldedProbationRegion {
              ProbationRegionEntityFactory()
                .withYieldedApArea { ApAreaEntityFactory().produce() }
                .produce()
            }
            .withYieldedLocalAuthorityArea { LocalAuthorityEntityFactory().produce() }
            .produce()
        }
        .withStaffKeyWorkerCode(null)
        .produce()

      every { mockArrivalRepository.save(any()) } answers { it.invocation.args[0] as ArrivalEntity }
      every { mockBookingRepository.save(any()) } answers { it.invocation.args[0] as BookingEntity }
      every { mockCas3DomainEventService.savePersonArrivedEvent(any(), user) } just Runs

      val result = bookingService.createArrival(
        booking = bookingEntity,
        arrivalDate = LocalDate.parse("2022-08-27"),
        expectedDepartureDate = LocalDate.parse("2022-08-29"),
        notes = "notes",
        keyWorkerStaffCode = null,
        user = UserEntityFactory()
          .withUnitTestControlProbationRegion()
          .produce(),
      )

      assertThat(result).isInstanceOf(ValidatableActionResult.GeneralValidationError::class.java)
      assertThat((result as ValidatableActionResult.GeneralValidationError).message).isEqualTo("CAS3 booking arrival not supported here, preferred method is createCas3Arrival")
      verify(exactly = 0) {
        mockCas3DomainEventService.savePersonArrivedEvent(bookingEntity, user)
      }
    }
  }

  @Nested
  inner class CreateCas3Arrival {
    private val bookingEntity = createTemporaryAccommodationBooking()

    @BeforeEach
    fun setup() {
      every { mockArrivalRepository.save(any()) } answers { it.invocation.args[0] as ArrivalEntity }
      every { mockBookingRepository.save(any()) } answers { it.invocation.args[0] as BookingEntity }
      every { mockCas3DomainEventService.savePersonArrivedEvent(any(), any(UserEntity::class)) } just Runs
    }

    @Test
    fun `createCas3Arrival should throw GeneralValidationError when premises is not temporary accommodation`() {
      val keyWorker = ContextStaffMemberFactory().produce()
      val approvedPremisesBooking = createApprovedPremisesBooking(createApprovedPremises(), keyWorker)

      val result = bookingService.createCas3Arrival(
        booking = approvedPremisesBooking,
        arrivalDate = LocalDate.parse("2022-08-25"),
        expectedDepartureDate = LocalDate.parse("2022-08-26"),
        notes = "notes",
        keyWorkerStaffCode = "158",
        user = UserEntityFactory()
          .withUnitTestControlProbationRegion()
          .produce(),
      )

      assertThat(result).isInstanceOf(ValidatableActionResult.GeneralValidationError::class.java)
      assertThat((result as ValidatableActionResult.GeneralValidationError).message).isEqualTo("CAS3 Arrivals cannot be set on non-CAS3 premise")
      verify(exactly = 0) { mockArrivalRepository.save(any()) }
      verify(exactly = 0) { mockBookingRepository.save(any()) }
      verify(exactly = 0) { mockCas3DomainEventService.savePersonArrivedEvent(bookingEntity, user) }
    }

    @Test
    fun `createArrival returns FieldValidationError with correct param to message map when invalid parameters supplied`() {
      val result = bookingService.createCas3Arrival(
        booking = bookingEntity,
        arrivalDate = LocalDate.parse("2022-08-27"),
        expectedDepartureDate = LocalDate.parse("2022-08-26"),
        notes = "notes",
        keyWorkerStaffCode = null,
        user = UserEntityFactory()
          .withUnitTestControlProbationRegion()
          .produce(),
      )

      assertThat(result).isInstanceOf(ValidatableActionResult.FieldValidationError::class.java)
      assertThat((result as ValidatableActionResult.FieldValidationError).validationMessages).contains(
        entry("$.expectedDepartureDate", "beforeBookingArrivalDate"),
      )
      verify(exactly = 0) { mockArrivalRepository.save(any()) }
      verify(exactly = 0) { mockBookingRepository.save(any()) }
      verify(exactly = 0) { mockCas3DomainEventService.savePersonArrivedEvent(bookingEntity, user) }
    }

    @Test
    fun `createArrival should return success response when arrival exists for a Booking and save and emit the event`() {
      val userEntity = UserEntityFactory()
        .withUnitTestControlProbationRegion()
        .produce()
      val arrivalEntity = ArrivalEntityFactory()
        .withBooking(bookingEntity)
        .produce()
      bookingEntity.arrivals += arrivalEntity

      every { mockCas3DomainEventService.savePersonArrivedUpdatedEvent(any(), any(UserEntity::class)) } just Runs

      val result = bookingService.createCas3Arrival(
        booking = bookingEntity,
        arrivalDate = LocalDate.parse("2022-08-25"),
        expectedDepartureDate = LocalDate.parse("2022-08-26"),
        notes = "notes",
        keyWorkerStaffCode = null,
        user = userEntity,
      )

      assertThat(result).isInstanceOf(ValidatableActionResult.Success::class.java)
      result as ValidatableActionResult.Success
      assertThat(result.entity.arrivalDate).isEqualTo(LocalDate.parse("2022-08-25"))
      assertThat(result.entity.arrivalDateTime).isEqualTo(Instant.parse("2022-08-25T00:00:00Z"))
      assertThat(result.entity.expectedDepartureDate).isEqualTo(LocalDate.parse("2022-08-26"))
      assertThat(result.entity.notes).isEqualTo("notes")
      assertThat(result.entity.booking.status).isEqualTo(BookingStatus.arrived)

      verify(exactly = 1) { mockArrivalRepository.save(any()) }
      verify(exactly = 1) { mockBookingRepository.save(any()) }
      verify(exactly = 1) {
        mockCas3DomainEventService.savePersonArrivedUpdatedEvent(bookingEntity, userEntity)
      }
      verify(exactly = 0) {
        mockCas3DomainEventService.savePersonArrivedEvent(bookingEntity, userEntity)
      }
    }

    @Test
    fun `createArrival returns Success with correct result for CAS3 when validation passed and saves domain event`() {
      val userEntity = UserEntityFactory()
        .withUnitTestControlProbationRegion()
        .produce()
      val result = bookingService.createCas3Arrival(
        booking = bookingEntity,
        arrivalDate = LocalDate.parse("2022-08-27"),
        expectedDepartureDate = LocalDate.parse("2022-08-29"),
        notes = "notes",
        keyWorkerStaffCode = null,
        user = userEntity,
      )

      assertThat(result).isInstanceOf(ValidatableActionResult.Success::class.java)
      result as ValidatableActionResult.Success
      assertThat(result.entity.arrivalDate).isEqualTo(LocalDate.parse("2022-08-27"))
      assertThat(result.entity.arrivalDateTime).isEqualTo(Instant.parse("2022-08-27T00:00:00Z"))
      assertThat(result.entity.expectedDepartureDate).isEqualTo(LocalDate.parse("2022-08-29"))
      assertThat(result.entity.notes).isEqualTo("notes")
      assertThat(result.entity.booking.status).isEqualTo(BookingStatus.arrived)

      verify(exactly = 1) { mockArrivalRepository.save(any()) }
      verify(exactly = 1) { mockBookingRepository.save(any()) }
      verify(exactly = 1) { mockCas3DomainEventService.savePersonArrivedEvent(bookingEntity, userEntity) }
    }

    @Test
    fun `createArrival returns Success with correct result for CAS3 when validation passed and saves domain event without staff detail`() {
      every { mockCas3DomainEventService.savePersonArrivedEvent(any(), user) } just Runs

      val result = bookingService.createCas3Arrival(
        booking = bookingEntity,
        arrivalDate = LocalDate.parse("2022-08-27"),
        expectedDepartureDate = LocalDate.parse("2022-08-29"),
        notes = "notes",
        keyWorkerStaffCode = null,
        user = user,
      )

      assertThat(result).isInstanceOf(ValidatableActionResult.Success::class.java)
      result as ValidatableActionResult.Success
      assertThat(result.entity.arrivalDate).isEqualTo(LocalDate.parse("2022-08-27"))
      assertThat(result.entity.arrivalDateTime).isEqualTo(Instant.parse("2022-08-27T00:00:00Z"))
      assertThat(result.entity.expectedDepartureDate).isEqualTo(LocalDate.parse("2022-08-29"))
      assertThat(result.entity.notes).isEqualTo("notes")
      assertThat(result.entity.booking.status).isEqualTo(BookingStatus.arrived)

      verify(exactly = 1) { mockArrivalRepository.save(any()) }
      verify(exactly = 1) { mockBookingRepository.save(any()) }
      verify(exactly = 1) { mockCas3DomainEventService.savePersonArrivedEvent(bookingEntity, user) }
    }

    private fun createTemporaryAccommodationBooking() = BookingEntityFactory()
      .withYieldedPremises {
        TemporaryAccommodationPremisesEntityFactory()
          .withYieldedProbationRegion {
            ProbationRegionEntityFactory()
              .withYieldedApArea { ApAreaEntityFactory().produce() }
              .produce()
          }
          .withYieldedLocalAuthorityArea { LocalAuthorityEntityFactory().produce() }
          .produce()
      }
      .withStaffKeyWorkerCode(null)
      .produce()

    private fun createApprovedPremisesBooking(
      premises: ApprovedPremisesEntity,
      keyWorker: uk.gov.justice.digital.hmpps.approvedpremisesapi.model.deliuscontext.StaffMember,
    ) = BookingEntityFactory()
      .withPremises(premises)
      .withStaffKeyWorkerCode(keyWorker.code)
      .withApplication(
        ApprovedPremisesApplicationEntityFactory()
          .withSubmittedAt(OffsetDateTime.parse("2023-02-15T15:00:00Z"))
          .withCreatedByUser(
            UserEntityFactory()
              .withUnitTestControlProbationRegion()
              .produce(),
          )
          .produce(),
      )
      .produce()

    private fun createApprovedPremises() = ApprovedPremisesEntityFactory()
      .withYieldedProbationRegion {
        ProbationRegionEntityFactory()
          .withYieldedApArea { ApAreaEntityFactory().produce() }
          .produce()
      }
      .withYieldedLocalAuthorityArea { LocalAuthorityEntityFactory().produce() }
      .produce()
  }

  @Nested
  inner class CreateCas1Cancellation {
    val premises = ApprovedPremisesEntityFactory()
      .withYieldedProbationRegion {
        ProbationRegionEntityFactory()
          .withYieldedApArea { ApAreaEntityFactory().produce() }
          .produce()
      }
      .withYieldedLocalAuthorityArea { LocalAuthorityEntityFactory().produce() }
      .produce()

    val application = ApprovedPremisesApplicationEntityFactory()
      .withCreatedByUser(user)
      .withSubmittedAt(OffsetDateTime.now())
      .produce()

    val reason = CancellationReasonEntityFactory().withServiceScope("*").produce()
    val reasonId = reason.id

    @Test
    fun `createCancellation is idempotent if the booking is already cancelled`() {
      val bookingEntity = BookingEntityFactory()
        .withPremises(premises)
        .produce()

      val cancellationEntity = CancellationEntityFactory()
        .withBooking(bookingEntity)
        .withReason(reason)
        .produce()

      bookingEntity.cancellations = mutableListOf(cancellationEntity)

      val result = bookingService.createCas1Cancellation(
        booking = bookingEntity,
        cancelledAt = LocalDate.parse("2022-08-25"),
        userProvidedReason = UUID.randomUUID(),
        notes = "notes",
        otherReason = null,
        withdrawalContext = WithdrawalContext(
          WithdrawalTriggeredByUser(user),
          WithdrawableEntityType.Booking,
          bookingEntity.id,
        ),
      )

      assertThat(result).isInstanceOf(CasResult.Success::class.java)
      result as CasResult.Success

      assertThat(result.value).isEqualTo(cancellationEntity)
      verify(exactly = 0) { mockBookingRepository.save(any()) }
    }

    @Test
    fun `createCancellation returns FieldValidationError with correct param to message map when invalid parameters supplied`() {
      val bookingEntity = BookingEntityFactory()
        .withArrivalDate(LocalDate.parse("2022-08-26"))
        .withPremises(premises)
        .produce()

      every { mockCancellationReasonRepository.findByIdOrNull(reasonId) } returns null

      val result = bookingService.createCas1Cancellation(
        booking = bookingEntity,
        cancelledAt = LocalDate.parse("2022-08-25"),
        userProvidedReason = reasonId,
        notes = "notes",
        otherReason = null,
        withdrawalContext = WithdrawalContext(
          WithdrawalTriggeredByUser(user),
          WithdrawableEntityType.Booking,
          bookingEntity.id,
        ),
      )

      assertThat(result).isInstanceOf(CasResult.FieldValidationError::class.java)
      assertThat((result as CasResult.FieldValidationError).validationMessages).contains(
        entry("$.reason", "doesNotExist"),
      )
    }

    @Test
    fun `createCancellation returns FieldValidationError with correct param to message map when reason is 'Other' and 'otherReason' is blank`() {
      val bookingEntity = BookingEntityFactory()
        .withArrivalDate(LocalDate.parse("2022-08-26"))
        .withPremises(premises)
        .produce()

      val reasonEntity = CancellationReasonEntityFactory()
        .withServiceScope(ServiceName.approvedPremises.value)
        .withName("Other")
        .produce()

      every { mockCancellationReasonRepository.findByIdOrNull(reasonId) } returns reasonEntity

      val result = bookingService.createCas1Cancellation(
        booking = bookingEntity,
        cancelledAt = LocalDate.parse("2022-08-25"),
        userProvidedReason = reasonId,
        notes = "notes",
        otherReason = null,
        withdrawalContext = WithdrawalContext(
          WithdrawalTriggeredByUser(user),
          WithdrawableEntityType.Booking,
          bookingEntity.id,
        ),
      )

      assertThat(result).isInstanceOf(CasResult.FieldValidationError::class.java)
      assertThat((result as CasResult.FieldValidationError).validationMessages).contains(
        entry("$.otherReason", "empty"),
      )
    }

    @Test
    fun `createCancellation returns FieldValidationError with correct param to message map when the cancellation reason has the wrong service scope`() {
      val bookingEntity = BookingEntityFactory()
        .withPremises(premises)
        .withServiceName(ServiceName.approvedPremises)
        .produce()

      val otherReason = CancellationReasonEntityFactory()
        .withServiceScope(ServiceName.temporaryAccommodation.value)
        .produce()

      every { mockCancellationReasonRepository.findByIdOrNull(reasonId) } returns otherReason
      every { mockCancellationRepository.save(any()) } answers { it.invocation.args[0] as CancellationEntity }

      val result = bookingService.createCas1Cancellation(
        booking = bookingEntity,
        cancelledAt = LocalDate.parse("2022-08-25"),
        userProvidedReason = reasonId,
        notes = "notes",
        otherReason = null,
        withdrawalContext = WithdrawalContext(
          WithdrawalTriggeredByUser(user),
          WithdrawableEntityType.Booking,
          bookingEntity.id,
        ),
      )

      assertThat(result).isInstanceOf(CasResult.FieldValidationError::class.java)
      assertThat((result as CasResult.FieldValidationError).validationMessages).contains(
        entry("$.reason", "incorrectCancellationReasonServiceScope"),
      )
    }

    @Test
    fun `createCancellation returns Success with correct result when validation passed`() {
      val bookingEntity = BookingEntityFactory()
        .withPremises(premises)
        .produce()

      every { mockCas1ApplicationStatusService.lastBookingCancelled(bookingEntity, true) } returns Unit
      every { mockCancellationReasonRepository.findByIdOrNull(reasonId) } returns reason
      every { mockCancellationRepository.save(any()) } answers { it.invocation.args[0] as CancellationEntity }
      every { mockBookingRepository.save(any()) } answers { it.invocation.args[0] as BookingEntity }

      val result = bookingService.createCas1Cancellation(
        booking = bookingEntity,
        cancelledAt = LocalDate.parse("2022-08-25"),
        userProvidedReason = reasonId,
        notes = "notes",
        otherReason = null,
        withdrawalContext = WithdrawalContext(
          WithdrawalTriggeredByUser(user),
          WithdrawableEntityType.Booking,
          bookingEntity.id,
        ),
      )

      assertThat(result).isInstanceOf(CasResult.Success::class.java)
      result as CasResult.Success
      assertThat(result.value.date).isEqualTo(LocalDate.parse("2022-08-25"))
      assertThat(result.value.reason).isEqualTo(reason)
      assertThat(result.value.notes).isEqualTo("notes")
      assertThat(result.value.booking.status).isEqualTo(BookingStatus.cancelled)

      verify(exactly = 1) {
        mockBookingRepository.save(bookingEntity)
      }
    }

    @Test
    fun `createCancellation returns Success with correct result with other reason validation passed`() {
      val bookingEntity = BookingEntityFactory()
        .withPremises(premises)
        .produce()

      val otherReason = CancellationReasonEntityFactory()
        .withServiceScope(ServiceName.approvedPremises.value)
        .withName("Other")
        .produce()

      every { mockCas1ApplicationStatusService.lastBookingCancelled(bookingEntity, true) } returns Unit
      every { mockCancellationReasonRepository.findByIdOrNull(otherReason.id) } returns otherReason
      every { mockCancellationRepository.save(any()) } answers { it.invocation.args[0] as CancellationEntity }
      every { mockBookingRepository.save(any()) } answers { it.invocation.args[0] as BookingEntity }

      val result = bookingService.createCas1Cancellation(
        booking = bookingEntity,
        cancelledAt = LocalDate.parse("2022-08-25"),
        userProvidedReason = otherReason.id,
        notes = "notes",
        otherReason = "Other reason",
        withdrawalContext = WithdrawalContext(
          WithdrawalTriggeredByUser(user),
          WithdrawableEntityType.Booking,
          bookingEntity.id,
        ),
      )

      assertThat(result).isInstanceOf(CasResult.Success::class.java)
      result as CasResult.Success
      assertThat(result.value.date).isEqualTo(LocalDate.parse("2022-08-25"))
      assertThat(result.value.reason).isEqualTo(otherReason)
      assertThat(result.value.notes).isEqualTo("notes")
      assertThat(result.value.otherReason).isEqualTo("Other reason")
      assertThat(result.value.booking.status).isEqualTo(BookingStatus.cancelled)

      verify(exactly = 1) {
        mockBookingRepository.save(bookingEntity)
      }
    }

    @Test
    fun `createCancellation emits domain event when linked to Application`() {
      val application = ApprovedPremisesApplicationEntityFactory()
        .withCreatedByUser(user)
        .withSubmittedAt(OffsetDateTime.now())
        .produce()

      val bookingEntity = BookingEntityFactory()
        .withPremises(premises)
        .withApplication(application)
        .withCrn(application.crn)
        .produce()
      every { mockCancellationReasonRepository.findByIdOrNull(reasonId) } returns reason

      val cancellationSaveArgument = slot<CancellationEntity>()
      every { mockCas1ApplicationStatusService.lastBookingCancelled(bookingEntity, true) } returns Unit
      every { mockCancellationRepository.save(capture(cancellationSaveArgument)) } answers { it.invocation.args[0] as CancellationEntity }
      every { mockCas1BookingDomainEventService.bookingCancelled(any(), any(), any(), any()) } just Runs
      every { mockBookingRepository.save(any()) } answers { it.invocation.args[0] as BookingEntity }
      every { mockBookingRepository.findAllByApplication(application) } returns emptyList()

      every { mockCas1BookingEmailService.bookingWithdrawn(application, bookingEntity, null, WithdrawalTriggeredByUser(user)) } returns Unit

      val cancelledAt = LocalDate.parse("2022-08-25")
      val notes = "notes"

      val result = bookingService.createCas1Cancellation(
        booking = bookingEntity,
        cancelledAt = cancelledAt,
        userProvidedReason = reasonId,
        notes = notes,
        otherReason = null,
        withdrawalContext = WithdrawalContext(
          WithdrawalTriggeredByUser(user),
          WithdrawableEntityType.Booking,
          bookingEntity.id,
        ),
      )

      assertThat(result).isInstanceOf(CasResult.Success::class.java)
      result as CasResult.Success
      assertThat(result.value.date).isEqualTo(LocalDate.parse("2022-08-25"))
      assertThat(result.value.reason).isEqualTo(reason)
      assertThat(result.value.notes).isEqualTo("notes")
      assertThat(result.value.booking.status).isEqualTo(BookingStatus.cancelled)

      verify(exactly = 1) {
        mockCas1BookingDomainEventService.bookingCancelled(
          bookingEntity,
          user,
          cancellationSaveArgument.captured,
          reason,
        )
      }

      verify(exactly = 1) {
        mockBookingRepository.save(bookingEntity)
      }
    }

    @Test
    fun `createCancellation triggers emails when linked to Application`() {
      val application = ApprovedPremisesApplicationEntityFactory()
        .withCreatedByUser(user)
        .withSubmittedAt(OffsetDateTime.now())
        .produce()

      val placementApplication = PlacementApplicationEntityFactory()
        .withDefaults()
        .produce()

      val bookingEntity = BookingEntityFactory()
        .withPremises(premises)
        .withApplication(application)
        .withCrn(application.crn)
        .withPlacementRequest(
          PlacementRequestEntityFactory()
            .withDefaults()
            .withPlacementApplication(placementApplication)
            .produce(),
        )
        .produce()

      every { mockCas1ApplicationStatusService.lastBookingCancelled(bookingEntity, true) } returns Unit
      every { mockCancellationReasonRepository.findByIdOrNull(reasonId) } returns reason
      every { mockCancellationRepository.save(any()) } answers { it.invocation.args[0] as CancellationEntity }
      every { mockCas1BookingDomainEventService.bookingCancelled(any(), any(), any(), any()) } just Runs
      every { mockBookingRepository.save(any()) } answers { it.invocation.args[0] as BookingEntity }
      every { mockBookingRepository.findAllByApplication(application) } returns emptyList()
      every { mockCas1BookingEmailService.bookingWithdrawn(application, bookingEntity, placementApplication, WithdrawalTriggeredByUser(user)) } returns Unit

      val cancelledAt = LocalDate.parse("2022-08-25")
      val notes = "notes"

      val result = bookingService.createCas1Cancellation(
        booking = bookingEntity,
        cancelledAt = cancelledAt,
        userProvidedReason = reasonId,
        notes = notes,
        otherReason = null,
        withdrawalContext = WithdrawalContext(
          WithdrawalTriggeredByUser(user),
          WithdrawableEntityType.Booking,
          bookingEntity.id,
        ),
      )

      assertThat(result).isInstanceOf(CasResult.Success::class.java)

      verify(exactly = 1) { mockCas1BookingEmailService.bookingWithdrawn(application, bookingEntity, placementApplication, WithdrawalTriggeredByUser(user)) }
    }

    @Test
    fun `createCancellation emits domain event when linked to an offline application with an eventNumber`() {
      val application = OfflineApplicationEntityFactory()
        .withEventNumber("123")
        .produce()

      val bookingEntity = BookingEntityFactory()
        .withPremises(premises)
        .withOfflineApplication(application)
        .withCrn(application.crn)
        .produce()

      every { mockCas1ApplicationStatusService.lastBookingCancelled(bookingEntity, true) } returns Unit
      every { mockCancellationReasonRepository.findByIdOrNull(reasonId) } returns reason
      val cancellationSaveArgument = slot<CancellationEntity>()
      every { mockCancellationRepository.save(capture(cancellationSaveArgument)) } answers { it.invocation.args[0] as CancellationEntity }
      every { mockCas1BookingDomainEventService.bookingCancelled(any(), any(), any(), any()) } just Runs
      every { mockBookingRepository.save(any()) } answers { it.invocation.args[0] as BookingEntity }

      val cancelledAt = LocalDate.parse("2022-08-25")
      val notes = "notes"

      val result = bookingService.createCas1Cancellation(
        booking = bookingEntity,
        cancelledAt = cancelledAt,
        userProvidedReason = reasonId,
        notes = notes,
        otherReason = null,
        withdrawalContext = WithdrawalContext(
          WithdrawalTriggeredByUser(user),
          WithdrawableEntityType.Booking,
          bookingEntity.id,
        ),
      )

      assertThat(result).isInstanceOf(CasResult.Success::class.java)
      result as CasResult.Success
      assertThat(result.value.date).isEqualTo(LocalDate.parse("2022-08-25"))
      assertThat(result.value.reason).isEqualTo(reason)
      assertThat(result.value.notes).isEqualTo("notes")
      assertThat(result.value.booking.status).isEqualTo(BookingStatus.cancelled)

      verify(exactly = 1) {
        mockCas1BookingDomainEventService.bookingCancelled(
          bookingEntity,
          user,
          cancellationSaveArgument.captured,
          reason,
        )
      }

      verify(exactly = 1) {
        mockBookingRepository.save(bookingEntity)
      }
    }

    @Test
    fun `createCancellation does not emit domain event when linked to an offline application without a eventNumber`() {
      val application = OfflineApplicationEntityFactory()
        .withEventNumber(null)
        .produce()

      val bookingEntity = BookingEntityFactory()
        .withPremises(premises)
        .withOfflineApplication(application)
        .withCrn(application.crn)
        .produce()

      every { mockCas1ApplicationStatusService.lastBookingCancelled(bookingEntity, true) } returns Unit
      every { mockCancellationReasonRepository.findByIdOrNull(reasonId) } returns reason
      every { mockCancellationRepository.save(any()) } answers { it.invocation.args[0] as CancellationEntity }
      every { mockBookingRepository.save(any()) } answers { it.invocation.args[0] as BookingEntity }

      val cancelledAt = LocalDate.parse("2022-08-25")
      val notes = "notes"

      val result = bookingService.createCas1Cancellation(
        booking = bookingEntity,
        cancelledAt = cancelledAt,
        userProvidedReason = reasonId,
        notes = notes,
        otherReason = null,
        withdrawalContext = WithdrawalContext(
          WithdrawalTriggeredByUser(user),
          WithdrawableEntityType.Booking,
          bookingEntity.id,
        ),
      )

      assertThat(result).isInstanceOf(CasResult.Success::class.java)
      result as CasResult.Success
      assertThat(result.value.date).isEqualTo(LocalDate.parse("2022-08-25"))
      assertThat(result.value.reason).isEqualTo(reason)
      assertThat(result.value.notes).isEqualTo("notes")
      assertThat(result.value.booking.status).isEqualTo(BookingStatus.cancelled)

      verify(exactly = 0) {
        mockCas1BookingDomainEventService.bookingCancelled(any(), any(), any(), any())
      }
      verify(exactly = 1) {
        mockBookingRepository.save(bookingEntity)
      }
    }

    @ParameterizedTest
    @EnumSource(WithdrawableEntityType::class, names = ["SpaceBooking"], mode = EnumSource.Mode.EXCLUDE)
    fun `createCancellation sets correct reason if withdrawal triggered by other entity`(triggeringEntity: WithdrawableEntityType) {
      val bookingEntity = BookingEntityFactory()
        .withPremises(premises)
        .produce()

      val expectedReasonId = when (triggeringEntity) {
        WithdrawableEntityType.Application -> CancellationReasonRepository.CAS1_RELATED_APP_WITHDRAWN_ID
        WithdrawableEntityType.PlacementApplication -> CancellationReasonRepository.CAS1_RELATED_PLACEMENT_APP_WITHDRAWN_ID
        WithdrawableEntityType.PlacementRequest -> CancellationReasonRepository.CAS1_RELATED_PLACEMENT_REQ_WITHDRAWN_ID
        WithdrawableEntityType.Booking -> reasonId
        WithdrawableEntityType.SpaceBooking -> reasonId
      }

      every { mockCas1ApplicationStatusService.lastBookingCancelled(bookingEntity, triggeringEntity == WithdrawableEntityType.Booking) } returns Unit
      every { mockCancellationReasonRepository.findByIdOrNull(expectedReasonId) } returns reason
      every { mockCancellationRepository.save(any()) } answers { it.invocation.args[0] as CancellationEntity }
      every { mockBookingRepository.save(any()) } answers { it.invocation.args[0] as BookingEntity }

      val result = bookingService.createCas1Cancellation(
        booking = bookingEntity,
        cancelledAt = LocalDate.parse("2022-08-25"),
        userProvidedReason = reasonId,
        notes = "notes",
        otherReason = null,
        withdrawalContext = WithdrawalContext(
          WithdrawalTriggeredByUser(user),
          triggeringEntity,
          bookingEntity.id,
        ),
      )

      assertThat(result).isInstanceOf(CasResult.Success::class.java)
      result as CasResult.Success
      assertThat(result.value.reason).isEqualTo(reason)

      verify(exactly = 1) {
        mockBookingRepository.save(bookingEntity)
      }
    }
  }

  @Nested
  inner class CreateCas3Cancellation {

    @Test
    fun `createCancellation returns Success with correct result and emits a domain event for CAS3`() {
      val reasonId = UUID.fromString("9ce3cd23-8e2b-457a-94d9-477d9ec63629")

      val bookingEntity = createTemporaryAccommodationBookingEntity()

      val reasonEntity = CancellationReasonEntityFactory().withServiceScope("*").produce()

      every { mockCancellationReasonRepository.findByIdOrNull(reasonId) } returns reasonEntity
      every { mockCancellationRepository.save(any()) } answers { it.invocation.args[0] as CancellationEntity }
      every { mockCas3DomainEventService.saveBookingCancelledEvent(any(), user) } just Runs
      every { mockBookingRepository.save(any()) } answers { it.invocation.args[0] as BookingEntity }

      val result = bookingService.createCas3Cancellation(
        booking = bookingEntity,
        cancelledAt = LocalDate.parse("2022-08-25"),
        reasonId = reasonId,
        notes = "notes",
        user = user,
      )

      assertThat(result).isInstanceOf(ValidatableActionResult.Success::class.java)
      result as ValidatableActionResult.Success
      assertThat(result.entity.date).isEqualTo(LocalDate.parse("2022-08-25"))
      assertThat(result.entity.reason).isEqualTo(reasonEntity)
      assertThat(result.entity.notes).isEqualTo("notes")
      assertThat(bookingEntity.cancellations).contains(result.entity)
      assertThat(result.entity.booking.status).isEqualTo(BookingStatus.cancelled)

      verify(exactly = 1) {
        mockCas3DomainEventService.saveBookingCancelledEvent(bookingEntity, user)
      }
      verify(exactly = 0) {
        mockCas3DomainEventService.saveBookingCancelledUpdatedEvent(any(), user)
      }
      verify(exactly = 1) {
        mockCancellationRepository.save(any())
      }
      verify(exactly = 1) {
        mockBookingRepository.save(bookingEntity)
      }
    }

    @Test
    fun `createCancellation returns Success with correct result and emits cancelled-updated domain event for CAS3`() {
      val reasonId = UUID.fromString("9ce3cd23-8e2b-457a-94d9-477d9ec63629")
      val bookingEntity = createTemporaryAccommodationBookingEntity()
      val reasonEntity = CancellationReasonEntityFactory().withServiceScope("*").produce()
      val cancellationEntity = CancellationEntityFactory().withBooking(bookingEntity).withReason(reasonEntity).produce()
      bookingEntity.cancellations += cancellationEntity
      every { mockCancellationReasonRepository.findByIdOrNull(reasonId) } returns reasonEntity
      every { mockCancellationRepository.save(any()) } answers { it.invocation.args[0] as CancellationEntity }
      every { mockCas3DomainEventService.saveBookingCancelledUpdatedEvent(any(), user) } just Runs
      every { mockBookingRepository.save(any()) } answers { it.invocation.args[0] as BookingEntity }

      val result = bookingService.createCas3Cancellation(
        booking = bookingEntity,
        cancelledAt = LocalDate.parse("2022-08-25"),
        reasonId = reasonId,
        notes = "notes",
        user = user,
      )

      assertThat(result).isInstanceOf(ValidatableActionResult.Success::class.java)
      result as ValidatableActionResult.Success
      assertThat(result.entity.date).isEqualTo(LocalDate.parse("2022-08-25"))
      assertThat(result.entity.reason).isEqualTo(reasonEntity)
      assertThat(result.entity.notes).isEqualTo("notes")
      assertThat(bookingEntity.cancellations).contains(result.entity)
      assertThat(result.entity.booking.status).isEqualTo(BookingStatus.cancelled)

      verify(exactly = 1) {
        mockCas3DomainEventService.saveBookingCancelledUpdatedEvent(bookingEntity, user)
      }
      verify(exactly = 0) {
        mockCas3DomainEventService.saveBookingCancelledEvent(any(), user)
      }
      verify(exactly = 1) {
        mockBookingRepository.save(bookingEntity)
      }
    }

    @Test
    fun `createCancellation returns Success and move the assessment to read-to-place state`() {
      val reasonId = UUID.fromString("9ce3cd23-8e2b-457a-94d9-477d9ec63629")
      val bookingEntity = createTemporaryAccommodationBookingWithAssessment()
      val assessmentEntity = bookingEntity.application?.getLatestAssessment()
      val reasonEntity = CancellationReasonEntityFactory().withServiceScope("*").produce()

      every { mockCancellationReasonRepository.findByIdOrNull(reasonId) } returns reasonEntity
      every { mockCancellationRepository.save(any()) } answers { it.invocation.args[0] as CancellationEntity }
      every { mockCas3DomainEventService.saveBookingCancelledEvent(any(), any()) } just Runs
      every { mockBookingRepository.save(any()) } answers { it.invocation.args[0] as BookingEntity }
      every { mockAssessmentService.acceptAssessment(user, any(), any(), any(), any(), any(), any()) } returns CasResult.Success(assessmentEntity!!)
      mockkStatic(Sentry::class)

      val result = bookingService.createCas3Cancellation(
        booking = bookingEntity,
        cancelledAt = LocalDate.parse("2022-08-25"),
        reasonId = reasonId,
        notes = "notes",
        user = user,
      )

      assertThat(result).isInstanceOf(ValidatableActionResult.Success::class.java)
      result as ValidatableActionResult.Success
      assertThat(result.entity.date).isEqualTo(LocalDate.parse("2022-08-25"))
      assertThat(result.entity.reason).isEqualTo(reasonEntity)
      assertThat(result.entity.notes).isEqualTo("notes")
      assertThat(bookingEntity.cancellations).contains(result.entity)
      assertThat(result.entity.booking.status).isEqualTo(BookingStatus.cancelled)

      verify(exactly = 1) {
        mockCas3DomainEventService.saveBookingCancelledEvent(bookingEntity, user)
      }
      verify(exactly = 0) {
        mockCas3DomainEventService.saveBookingCancelledUpdatedEvent(any(), user)
      }
      verify(exactly = 1) {
        mockCancellationRepository.save(any())
      }
      verify(exactly = 1) {
        mockBookingRepository.save(bookingEntity)
      }
      verify(exactly = 1) {
        mockAssessmentService.acceptAssessment(user, assessmentEntity.id, assessmentEntity.document, null, null, null, any())
      }
      verify(exactly = 0) {
        Sentry.captureException(any())
      }
    }

    @Test
    fun `createCancellation returns Success and not throwing error when acceptAssessment is failed with forbidden error`() {
      val reasonId = UUID.fromString("9ce3cd23-8e2b-457a-94d9-477d9ec63629")
      val bookingEntity = createTemporaryAccommodationBookingWithAssessment()
      val assessmentEntity = bookingEntity.application?.getLatestAssessment()!!
      val reasonEntity = CancellationReasonEntityFactory().withServiceScope("*").produce()

      every { mockCancellationReasonRepository.findByIdOrNull(reasonId) } returns reasonEntity
      every { mockCancellationRepository.save(any()) } answers { it.invocation.args[0] as CancellationEntity }
      every { mockCas3DomainEventService.saveBookingCancelledEvent(any(), any()) } just Runs
      every { mockBookingRepository.save(any()) } answers { it.invocation.args[0] as BookingEntity }
      every { mockAssessmentService.acceptAssessment(user, any(), any(), any(), any(), any(), any()) } returns CasResult.Unauthorised()
      mockkStatic(Sentry::class)
      every { Sentry.captureException(any()) } returns SentryId.EMPTY_ID

      val result = bookingService.createCas3Cancellation(
        booking = bookingEntity,
        cancelledAt = LocalDate.parse("2022-08-25"),
        reasonId = reasonId,
        notes = "notes",
        user = user,
      )

      assertThat(result).isInstanceOf(ValidatableActionResult.Success::class.java)
      result as ValidatableActionResult.Success
      assertThat(result.entity.date).isEqualTo(LocalDate.parse("2022-08-25"))
      assertThat(result.entity.reason).isEqualTo(reasonEntity)
      assertThat(result.entity.notes).isEqualTo("notes")
      assertThat(bookingEntity.cancellations).contains(result.entity)
      assertThat(result.entity.booking.status).isEqualTo(BookingStatus.cancelled)

      verify(exactly = 1) {
        mockCas3DomainEventService.saveBookingCancelledEvent(bookingEntity, user)
      }
      verify(exactly = 0) {
        mockCas3DomainEventService.saveBookingCancelledUpdatedEvent(any(), user)
      }
      verify(exactly = 1) {
        mockCancellationRepository.save(any())
      }
      verify(exactly = 1) {
        mockBookingRepository.save(bookingEntity)
      }
      verify(exactly = 1) {
        mockAssessmentService.acceptAssessment(user, assessmentEntity.id, assessmentEntity.document, null, null, null, any())
      }
      verify(exactly = 1) {
        Sentry.captureException(any())
      }
    }

    @Test
    fun `createCancellation returns Success and not throwing error when acceptAssessment is failed with runtime exception`() {
      val reasonId = UUID.fromString("9ce3cd23-8e2b-457a-94d9-477d9ec63629")
      val bookingEntity = createTemporaryAccommodationBookingWithAssessment()
      val assessmentEntity = bookingEntity.application?.getLatestAssessment()!!
      val reasonEntity = CancellationReasonEntityFactory().withServiceScope("*").produce()

      every { mockCancellationReasonRepository.findByIdOrNull(reasonId) } returns reasonEntity
      every { mockCancellationRepository.save(any()) } answers { it.invocation.args[0] as CancellationEntity }
      every { mockCas3DomainEventService.saveBookingCancelledEvent(any(), any()) } just Runs
      every { mockBookingRepository.save(any()) } answers { it.invocation.args[0] as BookingEntity }
      every { mockAssessmentService.acceptAssessment(user, any(), any(), any(), any(), any(), any()) } throws RuntimeException("some-exception")
      mockkStatic(Sentry::class)

      val result = bookingService.createCas3Cancellation(
        booking = bookingEntity,
        cancelledAt = LocalDate.parse("2022-08-25"),
        reasonId = reasonId,
        notes = "notes",
        user = user,
      )

      assertThat(result).isInstanceOf(ValidatableActionResult.Success::class.java)
      result as ValidatableActionResult.Success
      assertThat(result.entity.date).isEqualTo(LocalDate.parse("2022-08-25"))
      assertThat(result.entity.reason).isEqualTo(reasonEntity)
      assertThat(result.entity.notes).isEqualTo("notes")
      assertThat(bookingEntity.cancellations).contains(result.entity)
      assertThat(result.entity.booking.status).isEqualTo(BookingStatus.cancelled)

      verify(exactly = 1) {
        mockCas3DomainEventService.saveBookingCancelledEvent(bookingEntity, user)
      }
      verify(exactly = 0) {
        mockCas3DomainEventService.saveBookingCancelledUpdatedEvent(any(), user)
      }
      verify(exactly = 1) {
        mockCancellationRepository.save(any())
      }
      verify(exactly = 1) {
        mockBookingRepository.save(bookingEntity)
      }
      verify(exactly = 1) {
        mockAssessmentService.acceptAssessment(user, assessmentEntity.id, assessmentEntity.document, null, null, null, any())
      }
      verify(exactly = 1) {
        Sentry.captureException(any())
      }
    }

    @Test
    fun `createCancellation returns exception when acceptAssessment is failed with unexpected exception`() {
      val reasonId = UUID.fromString("9ce3cd23-8e2b-457a-94d9-477d9ec63629")
      val bookingEntity = createTemporaryAccommodationBookingWithAssessment()
      val assessmentEntity = bookingEntity.application?.getLatestAssessment()!!
      val reasonEntity = CancellationReasonEntityFactory().withServiceScope("*").produce()

      every { mockCancellationReasonRepository.findByIdOrNull(reasonId) } returns reasonEntity
      every { mockCancellationRepository.save(any()) } answers { it.invocation.args[0] as CancellationEntity }
      every { mockCas3DomainEventService.saveBookingCancelledEvent(any(), any()) } just Runs
      every { mockBookingRepository.save(any()) } answers { it.invocation.args[0] as BookingEntity }
      every { mockAssessmentService.acceptAssessment(user, any(), any(), any(), any(), any(), any()) } throws Throwable("some-exception")
      mockkStatic(Sentry::class)

      assertThatExceptionOfType(Throwable::class.java)
        .isThrownBy {
          bookingService.createCas3Cancellation(
            booking = bookingEntity,
            cancelledAt = LocalDate.parse("2022-08-25"),
            reasonId = reasonId,
            notes = "notes",
            user = user,
          )
        }

      verify(exactly = 1) {
        mockCas3DomainEventService.saveBookingCancelledEvent(bookingEntity, user)
      }
      verify(exactly = 0) {
        mockCas3DomainEventService.saveBookingCancelledUpdatedEvent(any(), user)
      }
      verify(exactly = 1) {
        mockCancellationRepository.save(any())
      }
      verify(exactly = 1) {
        mockBookingRepository.save(bookingEntity)
      }
      verify(exactly = 1) {
        mockAssessmentService.acceptAssessment(user, assessmentEntity.id, assessmentEntity.document, null, null, null, any())
      }
      verify(exactly = 0) {
        Sentry.captureException(any())
      }
    }

    @Test
    fun `createCancellation returns exception when unexpected exception occurred and acceptAssessment is not invoked`() {
      val reasonId = UUID.fromString("9ce3cd23-8e2b-457a-94d9-477d9ec63629")
      val bookingEntity = createTemporaryAccommodationBookingWithAssessment()
      val assessmentEntity = bookingEntity.application?.getLatestAssessment()!!
      val reasonEntity = CancellationReasonEntityFactory().withServiceScope("*").produce()

      every { mockCancellationReasonRepository.findByIdOrNull(reasonId) } returns reasonEntity
      every { mockCancellationRepository.save(any()) } throws RuntimeException("som-exception")
      mockkStatic(Sentry::class)

      assertThatExceptionOfType(RuntimeException::class.java)
        .isThrownBy {
          bookingService.createCas3Cancellation(
            booking = bookingEntity,
            cancelledAt = LocalDate.parse("2022-08-25"),
            reasonId = reasonId,
            notes = "notes",
            user = user,
          )
        }

      verify(exactly = 0) {
        mockCas3DomainEventService.saveBookingCancelledEvent(bookingEntity, user)
      }
      verify(exactly = 0) {
        mockCas3DomainEventService.saveBookingCancelledUpdatedEvent(any(), user)
      }
      verify(exactly = 1) {
        mockCancellationRepository.save(any())
      }
      verify(exactly = 0) {
        mockBookingRepository.save(bookingEntity)
      }
      verify(exactly = 0) {
        mockAssessmentService.acceptAssessment(user, assessmentEntity.id, assessmentEntity.document, null, null, null, any())
      }
      verify(exactly = 0) {
        Sentry.captureException(any())
      }
    }

    @Test
    fun `createCancellation returns Success and not throwing error when accept assessment is failed with bad request error`() {
      val reasonId = UUID.fromString("9ce3cd23-8e2b-457a-94d9-477d9ec63629")
      val bookingEntity = createTemporaryAccommodationBookingWithAssessment()
      val assessmentEntity = bookingEntity.application?.getLatestAssessment()!!
      val reasonEntity = CancellationReasonEntityFactory().withServiceScope("*").produce()

      every { mockCancellationReasonRepository.findByIdOrNull(reasonId) } returns reasonEntity
      every { mockCancellationRepository.save(any()) } answers { it.invocation.args[0] as CancellationEntity }
      every { mockCas3DomainEventService.saveBookingCancelledEvent(any(), any()) } just Runs
      every { mockBookingRepository.save(any()) } answers { it.invocation.args[0] as BookingEntity }
      every { mockAssessmentService.acceptAssessment(user, any(), any(), any(), any(), any(), any()) } returns CasResult.GeneralValidationError("Error")
      mockkStatic(Sentry::class)
      every { Sentry.captureException(any()) } returns SentryId.EMPTY_ID

      val result = bookingService.createCas3Cancellation(
        booking = bookingEntity,
        cancelledAt = LocalDate.parse("2022-08-25"),
        reasonId = reasonId,
        notes = "notes",
        user = user,
      )

      assertThat(result).isInstanceOf(ValidatableActionResult.Success::class.java)
      result as ValidatableActionResult.Success
      assertThat(result.entity.date).isEqualTo(LocalDate.parse("2022-08-25"))
      assertThat(result.entity.reason).isEqualTo(reasonEntity)
      assertThat(result.entity.notes).isEqualTo("notes")
      assertThat(bookingEntity.cancellations).contains(result.entity)
      assertThat(result.entity.booking.status).isEqualTo(BookingStatus.cancelled)

      verify(exactly = 1) {
        mockCas3DomainEventService.saveBookingCancelledEvent(bookingEntity, user)
      }
      verify(exactly = 0) {
        mockCas3DomainEventService.saveBookingCancelledUpdatedEvent(any(), user)
      }
      verify(exactly = 1) {
        mockBookingRepository.save(bookingEntity)
      }
      verify(exactly = 1) {
        mockAssessmentService.acceptAssessment(user, assessmentEntity.id, assessmentEntity.document, null, null, null, any())
      }
      verify(exactly = 1) {
        Sentry.captureException(any())
      }
    }

    @Test
    fun `createCancellation returns Success and not invoke acceptAssessment when assessment is empty`() {
      val reasonId = UUID.fromString("9ce3cd23-8e2b-457a-94d9-477d9ec63629")
      val bookingEntity = createTemporaryAccommodationBookingWithAssessment(false)
      val reasonEntity = CancellationReasonEntityFactory().withServiceScope("*").produce()

      every { mockCancellationReasonRepository.findByIdOrNull(reasonId) } returns reasonEntity
      every { mockCancellationRepository.save(any()) } answers { it.invocation.args[0] as CancellationEntity }
      every { mockCas3DomainEventService.saveBookingCancelledEvent(any(), any()) } just Runs
      every { mockBookingRepository.save(any()) } answers { it.invocation.args[0] as BookingEntity }

      val result = bookingService.createCas3Cancellation(
        booking = bookingEntity,
        cancelledAt = LocalDate.parse("2022-08-25"),
        reasonId = reasonId,
        notes = "notes",
        user = user,
      )

      assertThat(result).isInstanceOf(ValidatableActionResult.Success::class.java)
      result as ValidatableActionResult.Success
      assertThat(result.entity.date).isEqualTo(LocalDate.parse("2022-08-25"))
      assertThat(result.entity.reason).isEqualTo(reasonEntity)
      assertThat(result.entity.notes).isEqualTo("notes")
      assertThat(bookingEntity.cancellations).contains(result.entity)
      assertThat(result.entity.booking.status).isEqualTo(BookingStatus.cancelled)

      verify(exactly = 1) {
        mockCas3DomainEventService.saveBookingCancelledEvent(bookingEntity, user)
      }
      verify(exactly = 0) {
        mockCas3DomainEventService.saveBookingCancelledUpdatedEvent(any(), user)
      }
      verify(exactly = 1) {
        mockBookingRepository.save(bookingEntity)
      }
      verify(exactly = 0) {
        mockAssessmentService.acceptAssessment(user, any(), any(), null, null, null, any())
      }
      verify(exactly = 0) {
        Sentry.captureException(any())
      }
    }

    private fun createTemporaryAccommodationBookingEntity(): BookingEntity {
      val bookingEntity = BookingEntityFactory()
        .withYieldedPremises {
          TemporaryAccommodationPremisesEntityFactory()
            .withYieldedProbationRegion {
              ProbationRegionEntityFactory()
                .withYieldedApArea { ApAreaEntityFactory().produce() }
                .produce()
            }
            .withYieldedLocalAuthorityArea { LocalAuthorityEntityFactory().produce() }
            .produce()
        }
        .withServiceName(ServiceName.temporaryAccommodation)
        .produce()
      return bookingEntity
    }

    private fun createTemporaryAccommodationBookingWithAssessment(withAssessment: Boolean = true): BookingEntity {
      val user = UserEntityFactory()
        .withUnitTestControlProbationRegion()
        .produce()

      val application = TemporaryAccommodationApplicationEntityFactory()
        .withCreatedByUser(user)
        .withProbationRegion(user.probationRegion)
        .produce()

      val assessmentEntity = TemporaryAccommodationAssessmentEntityFactory()
        .withApplication(application)
        .produce()

      if (withAssessment) {
        application.assessments += assessmentEntity
      }

      val bookingEntity = BookingEntityFactory()
        .withYieldedPremises {
          TemporaryAccommodationPremisesEntityFactory()
            .withYieldedProbationRegion {
              ProbationRegionEntityFactory()
                .withYieldedApArea { ApAreaEntityFactory().produce() }
                .produce()
            }
            .withYieldedLocalAuthorityArea { LocalAuthorityEntityFactory().produce() }
            .produce()
        }
        .withServiceName(ServiceName.temporaryAccommodation)
        .withApplication(application)
        .produce()

      return bookingEntity
    }
  }

  @Nested
  inner class CreateExtension {
    val approvedPremises = ApprovedPremisesEntityFactory()
      .withYieldedProbationRegion {
        ProbationRegionEntityFactory()
          .withYieldedApArea { ApAreaEntityFactory().produce() }
          .produce()
      }
      .withYieldedLocalAuthorityArea { LocalAuthorityEntityFactory().produce() }
      .produce()

    private val approvedPremisesRoom = RoomEntityFactory()
      .withPremises(approvedPremises)
      .produce()

    private val approvedPremisesBed = BedEntityFactory()
      .withRoom(approvedPremisesRoom)
      .produce()

    private val temporaryAccommodationPremises = TemporaryAccommodationPremisesEntityFactory()
      .withYieldedProbationRegion {
        ProbationRegionEntityFactory()
          .withYieldedApArea { ApAreaEntityFactory().produce() }
          .produce()
      }
      .withYieldedLocalAuthorityArea { LocalAuthorityEntityFactory().produce() }
      .produce()

    private val temporaryAccommodationRoom = RoomEntityFactory()
      .withPremises(temporaryAccommodationPremises)
      .produce()

    private val temporaryAccommodationBed = BedEntityFactory()
      .withRoom(temporaryAccommodationRoom)
      .produce()

    @BeforeEach
    fun setup() {
      every { mockBookingRepository.save(any()) } answers { it.invocation.args[0] as BookingEntity }
      every { mockExtensionRepository.save(any()) } answers { it.invocation.args[0] as ExtensionEntity }

      every { mockWorkingDayService.addWorkingDays(any(), any()) } answers { it.invocation.args[0] as LocalDate }
      every { mockBookingRepository.findByBedIdAndArrivingBeforeDate(any(), any(), any()) } returns listOf()
      every { mockLostBedsRepository.findByBedIdAndOverlappingDate(any(), any(), any(), any()) } returns listOf()
    }

    @Test
    fun `Error if CAS1 booking provided`() {
      val bookingEntity = BookingEntityFactory()
        .withArrivalDate(LocalDate.parse("2022-08-10"))
        .withDepartureDate(LocalDate.parse("2022-08-26"))
        .withPremises(temporaryAccommodationPremises)
        .withBed(temporaryAccommodationBed)
        .withServiceName(ServiceName.approvedPremises)
        .produce()

      val result = bookingService.createExtension(
        booking = bookingEntity,
        newDepartureDate = LocalDate.parse("2022-08-25"),
        notes = "notes",
      )

      assertThat(result).isInstanceOf(ValidatableActionResult.GeneralValidationError::class.java)
      result as ValidatableActionResult.GeneralValidationError
      assertThat(result.message).isEqualTo("Extensions are only supported by CAS3")
    }

    @Test
    fun `Success with correct result when a CAS3 booking has a new departure date before the existing departure date`() {
      val bookingEntity = BookingEntityFactory()
        .withArrivalDate(LocalDate.parse("2022-08-10"))
        .withDepartureDate(LocalDate.parse("2022-08-26"))
        .withPremises(temporaryAccommodationPremises)
        .withBed(temporaryAccommodationBed)
        .withServiceName(ServiceName.temporaryAccommodation)
        .produce()

      val result = bookingService.createExtension(
        booking = bookingEntity,
        newDepartureDate = LocalDate.parse("2022-08-25"),
        notes = "notes",
      )

      assertThat(result).isInstanceOf(ValidatableActionResult.Success::class.java)
      result as ValidatableActionResult.Success
      assertThat(result.entity.newDepartureDate).isEqualTo(LocalDate.parse("2022-08-25"))
      assertThat(result.entity.previousDepartureDate).isEqualTo(LocalDate.parse("2022-08-26"))
      assertThat(result.entity.notes).isEqualTo("notes")
    }

    @Test
    fun `Success with correct result when a CAS3 booking has a new departure date after the existing departure date`() {
      val bookingEntity = BookingEntityFactory()
        .withArrivalDate(LocalDate.parse("2022-08-10"))
        .withDepartureDate(LocalDate.parse("2022-08-26"))
        .withPremises(temporaryAccommodationPremises)
        .withBed(temporaryAccommodationBed)
        .withServiceName(ServiceName.temporaryAccommodation)
        .produce()

      val result = bookingService.createExtension(
        booking = bookingEntity,
        newDepartureDate = LocalDate.parse("2022-08-27"),
        notes = "notes",
      )

      assertThat(result).isInstanceOf(ValidatableActionResult.Success::class.java)
      result as ValidatableActionResult.Success
      assertThat(result.entity.newDepartureDate).isEqualTo(LocalDate.parse("2022-08-27"))
      assertThat(result.entity.previousDepartureDate).isEqualTo(LocalDate.parse("2022-08-26"))
      assertThat(result.entity.notes).isEqualTo("notes")
    }

    @Test
    fun `FieldValidationError when a CAS3 booking has a new departure date before the arrival date`() {
      val bookingEntity = BookingEntityFactory()
        .withArrivalDate(LocalDate.parse("2022-08-26"))
        .withPremises(temporaryAccommodationPremises)
        .withBed(temporaryAccommodationBed)
        .withServiceName(ServiceName.temporaryAccommodation)
        .produce()

      val result = bookingService.createExtension(
        booking = bookingEntity,
        newDepartureDate = LocalDate.parse("2022-08-25"),
        notes = "notes",
      )

      assertThat(result).isInstanceOf(ValidatableActionResult.FieldValidationError::class.java)
      assertThat((result as ValidatableActionResult.FieldValidationError).validationMessages).contains(
        entry("$.newDepartureDate", "beforeBookingArrivalDate"),
      )
    }
  }

  @Test
  fun `createConfirmation returns GeneralValidationError with correct message when Booking already has a Confirmation`() {
    val bookingEntity = BookingEntityFactory()
      .withYieldedPremises {
        ApprovedPremisesEntityFactory()
          .withYieldedProbationRegion {
            ProbationRegionEntityFactory()
              .withYieldedApArea { ApAreaEntityFactory().produce() }
              .produce()
          }
          .withService(ServiceName.temporaryAccommodation.value)
          .withYieldedLocalAuthorityArea { LocalAuthorityEntityFactory().produce() }
          .produce()
      }
      .produce()

    val confirmationEntity = ConfirmationEntityFactory()
      .withBooking(bookingEntity)
      .produce()

    bookingEntity.confirmation = confirmationEntity

    val result = bookingService.createConfirmation(
      booking = bookingEntity,
      dateTime = OffsetDateTime.parse("2022-08-25T12:34:56.789Z"),
      notes = "notes",
      user = user,
    )

    assertThat(result).isInstanceOf(ValidatableActionResult.GeneralValidationError::class.java)
    assertThat((result as ValidatableActionResult.GeneralValidationError).message).isEqualTo("This Booking already has a Confirmation set")
  }

  @Test
  fun `createConfirmation returns Success with correct result when validation passed and emits domain event`() {
    val bookingEntity = createTemporaryAccommodationBooking(null)

    every { mockConfirmationRepository.save(any()) } answers { it.invocation.args[0] as ConfirmationEntity }
    every { mockBookingRepository.save(any()) } answers { it.invocation.args[0] as BookingEntity }

    every { mockCas3DomainEventService.saveBookingConfirmedEvent(any(), user) } just Runs

    val result = bookingService.createConfirmation(
      booking = bookingEntity,
      dateTime = OffsetDateTime.parse("2022-08-25T12:34:56.789Z"),
      notes = "notes",
      user = user,
    )

    assertThat(result).isInstanceOf(ValidatableActionResult.Success::class.java)
    result as ValidatableActionResult.Success
    assertThat(result.entity.dateTime).isEqualTo(OffsetDateTime.parse("2022-08-25T12:34:56.789Z"))
    assertThat(result.entity.notes).isEqualTo("notes")
    assertThat(bookingEntity.confirmation).isEqualTo(result.entity)
    assertThat(result.entity.booking.status).isEqualTo(BookingStatus.confirmed)

    verify(exactly = 1) {
      mockCas3DomainEventService.saveBookingConfirmedEvent(bookingEntity, user)
    }
    verify(exactly = 1) {
      mockBookingRepository.save(bookingEntity)
    }
  }

  @Test
  fun `createConfirmation returns Success with correct result and not closing the referral when booking is done without application`() {
    val bookingEntity = createTemporaryAccommodationBooking(null)

    every { mockConfirmationRepository.save(any()) } answers { it.invocation.args[0] as ConfirmationEntity }
    every { mockBookingRepository.save(any()) } answers { it.invocation.args[0] as BookingEntity }

    every { mockCas3DomainEventService.saveBookingConfirmedEvent(any(), user) } just Runs

    val result = bookingService.createConfirmation(
      booking = bookingEntity,
      dateTime = OffsetDateTime.parse("2022-08-25T12:34:56.789Z"),
      notes = "notes",
      user = user,
    )

    assertThat(result).isInstanceOf(ValidatableActionResult.Success::class.java)
    result as ValidatableActionResult.Success
    assertThat(result.entity.dateTime).isEqualTo(OffsetDateTime.parse("2022-08-25T12:34:56.789Z"))
    assertThat(result.entity.notes).isEqualTo("notes")
    assertThat(bookingEntity.confirmation).isEqualTo(result.entity)
    assertThat(result.entity.booking.status).isEqualTo(BookingStatus.confirmed)

    verify(exactly = 1) {
      mockCas3DomainEventService.saveBookingConfirmedEvent(bookingEntity, user)
    }
    verify(exactly = 1) {
      mockBookingRepository.save(bookingEntity)
    }
    verify(exactly = 0) {
      mockAssessmentService.closeAssessment(user, any())
    }
    verify(exactly = 0) {
      mockAssessmentRepository.findByApplicationIdAndReallocatedAtNull(any())
    }
  }

  @Test
  fun `createConfirmation returns Success with correct result when validation passed and emits domain event and close referral`() {
    val application = TemporaryAccommodationApplicationEntityFactory()
      .withProbationRegion(user.probationRegion)
      .withCreatedByUser(user)
      .produce()

    val assessment = TemporaryAccommodationAssessmentEntityFactory()
      .withApplication(application)
      .produce()

    val bookingEntity = createTemporaryAccommodationBooking(application)

    every { mockConfirmationRepository.save(any()) } answers { it.invocation.args[0] as ConfirmationEntity }
    every { mockBookingRepository.save(any()) } answers { it.invocation.args[0] as BookingEntity }

    every { mockCas3DomainEventService.saveBookingConfirmedEvent(any(), user) } just Runs
    every { mockAssessmentRepository.findByApplicationIdAndReallocatedAtNull(bookingEntity.application!!.id) } returns assessment
    every { mockAssessmentService.closeAssessment(user, assessment.id) } returns CasResult.Success(assessment)
    mockkStatic(Sentry::class)

    val result = bookingService.createConfirmation(
      booking = bookingEntity,
      dateTime = OffsetDateTime.parse("2022-08-25T12:34:56.789Z"),
      notes = "notes",
      user = user,
    )

    assertThat(result).isInstanceOf(ValidatableActionResult.Success::class.java)
    result as ValidatableActionResult.Success
    assertThat(result.entity.dateTime).isEqualTo(OffsetDateTime.parse("2022-08-25T12:34:56.789Z"))
    assertThat(result.entity.notes).isEqualTo("notes")
    assertThat(bookingEntity.confirmation).isEqualTo(result.entity)
    assertThat(result.entity.booking.status).isEqualTo(BookingStatus.confirmed)

    verify(exactly = 1) {
      mockCas3DomainEventService.saveBookingConfirmedEvent(bookingEntity, user)
    }
    verify(exactly = 1) {
      mockBookingRepository.save(bookingEntity)
    }
    verify(exactly = 1) {
      mockAssessmentService.closeAssessment(user, assessment.id)
    }
    verify(exactly = 1) {
      mockAssessmentRepository.findByApplicationIdAndReallocatedAtNull(bookingEntity.application!!.id)
    }
    verify(exactly = 0) {
      Sentry.captureException(any())
    }
  }

  @Test
  fun `createConfirmation returns Success with correct result and not closing the referral when assessment not found`() {
    val application = TemporaryAccommodationApplicationEntityFactory()
      .withProbationRegion(user.probationRegion)
      .withCreatedByUser(user)
      .produce()
    val bookingEntity = createTemporaryAccommodationBooking(application)

    every { mockConfirmationRepository.save(any()) } answers { it.invocation.args[0] as ConfirmationEntity }
    every { mockBookingRepository.save(any()) } answers { it.invocation.args[0] as BookingEntity }
    every { mockCas3DomainEventService.saveBookingConfirmedEvent(any(), user) } just Runs
    every { mockAssessmentRepository.findByApplicationIdAndReallocatedAtNull(bookingEntity.application!!.id) } returns null

    val result = bookingService.createConfirmation(
      booking = bookingEntity,
      dateTime = OffsetDateTime.parse("2022-08-25T12:34:56.789Z"),
      notes = "notes",
      user = user,
    )

    assertThat(result).isInstanceOf(ValidatableActionResult.Success::class.java)
    result as ValidatableActionResult.Success
    assertThat(result.entity.dateTime).isEqualTo(OffsetDateTime.parse("2022-08-25T12:34:56.789Z"))
    assertThat(result.entity.notes).isEqualTo("notes")
    assertThat(bookingEntity.confirmation).isEqualTo(result.entity)
    assertThat(result.entity.booking.status).isEqualTo(BookingStatus.confirmed)

    verify(exactly = 1) {
      mockCas3DomainEventService.saveBookingConfirmedEvent(bookingEntity, user)
    }
    verify(exactly = 1) {
      mockBookingRepository.save(bookingEntity)
    }
    verify(exactly = 1) {
      mockAssessmentRepository.findByApplicationIdAndReallocatedAtNull(bookingEntity.application!!.id)
    }
    verify(exactly = 0) {
      mockAssessmentService.closeAssessment(user, any())
    }
  }

  @Test
  fun `createConfirmation returns Success with correct result when closing referral failed`() {
    val application = TemporaryAccommodationApplicationEntityFactory()
      .withProbationRegion(user.probationRegion)
      .withCreatedByUser(user)
      .produce()

    val assessment = TemporaryAccommodationAssessmentEntityFactory()
      .withApplication(application)
      .produce()

    val bookingEntity = createTemporaryAccommodationBooking(application)

    every { mockConfirmationRepository.save(any()) } answers { it.invocation.args[0] as ConfirmationEntity }
    every { mockBookingRepository.save(any()) } answers { it.invocation.args[0] as BookingEntity }
    every { mockCas3DomainEventService.saveBookingConfirmedEvent(any(), user) } just Runs
    every { mockAssessmentRepository.findByApplicationIdAndReallocatedAtNull(bookingEntity.application!!.id) } returns assessment
    every { mockAssessmentService.closeAssessment(user, assessment.id) } returns CasResult.Unauthorised()
    mockkStatic(Sentry::class)
    every { Sentry.captureException(any()) } returns SentryId.EMPTY_ID

    val result = bookingService.createConfirmation(
      booking = bookingEntity,
      dateTime = OffsetDateTime.parse("2022-08-25T12:34:56.789Z"),
      notes = "notes",
      user = user,
    )

    assertThat(result).isInstanceOf(ValidatableActionResult.Success::class.java)
    result as ValidatableActionResult.Success
    assertThat(result.entity.dateTime).isEqualTo(OffsetDateTime.parse("2022-08-25T12:34:56.789Z"))
    assertThat(result.entity.notes).isEqualTo("notes")
    assertThat(bookingEntity.confirmation).isEqualTo(result.entity)
    assertThat(result.entity.booking.status).isEqualTo(BookingStatus.confirmed)

    verify(exactly = 1) {
      mockCas3DomainEventService.saveBookingConfirmedEvent(bookingEntity, user)
    }
    verify(exactly = 1) {
      mockBookingRepository.save(bookingEntity)
    }
    verify(exactly = 1) {
      mockAssessmentService.closeAssessment(user, assessment.id)
    }
    verify(exactly = 1) {
      mockAssessmentRepository.findByApplicationIdAndReallocatedAtNull(bookingEntity.application!!.id)
    }
    verify(exactly = 1) {
      Sentry.captureException(any())
    }
  }

  @Test
  fun `createConfirmation returns Success with correct result not closing referral when the request is not Temporary premises accommodation`() {
    val application = ApprovedPremisesApplicationEntityFactory()
      .withCreatedByUser(user)
      .produce()
    val bookingEntity = createApprovedPremisesAccommodationBooking(application)

    every { mockConfirmationRepository.save(any()) } answers { it.invocation.args[0] as ConfirmationEntity }
    every { mockBookingRepository.save(any()) } answers { it.invocation.args[0] as BookingEntity }
    every { mockCas3DomainEventService.saveBookingConfirmedEvent(any(), user) } just Runs

    val result = bookingService.createConfirmation(
      booking = bookingEntity,
      dateTime = OffsetDateTime.parse("2022-08-25T12:34:56.789Z"),
      notes = "notes",
      user = user,
    )

    assertThat(result).isInstanceOf(ValidatableActionResult.Success::class.java)
    result as ValidatableActionResult.Success
    assertThat(result.entity.dateTime).isEqualTo(OffsetDateTime.parse("2022-08-25T12:34:56.789Z"))
    assertThat(result.entity.notes).isEqualTo("notes")
    assertThat(bookingEntity.confirmation).isEqualTo(result.entity)
    assertThat(result.entity.booking.status).isEqualTo(BookingStatus.confirmed)

    verify(exactly = 0) {
      mockCas3DomainEventService.saveBookingConfirmedEvent(bookingEntity, user)
    }
    verify(exactly = 1) {
      mockBookingRepository.save(bookingEntity)
    }
    verify(exactly = 0) {
      mockAssessmentService.closeAssessment(user, any())
    }
    verify(exactly = 0) {
      mockAssessmentRepository.findByApplicationIdAndReallocatedAtNull(bookingEntity.application!!.id)
    }
  }

  private fun createTemporaryAccommodationBooking(application: TemporaryAccommodationApplicationEntity?) =
    BookingEntityFactory()
      .withYieldedPremises {
        TemporaryAccommodationPremisesEntityFactory()
          .withYieldedProbationRegion {
            ProbationRegionEntityFactory()
              .withYieldedApArea { ApAreaEntityFactory().produce() }
              .produce()
          }
          .withService(ServiceName.temporaryAccommodation.value)
          .withYieldedLocalAuthorityArea { LocalAuthorityEntityFactory().produce() }
          .produce()
      }
      .withApplication(application.let { application })
      .produce()

  private fun createApprovedPremisesAccommodationBooking(application: ApprovedPremisesApplicationEntity?) =
    BookingEntityFactory()
      .withYieldedPremises {
        ApprovedPremisesEntityFactory()
          .withYieldedProbationRegion {
            ProbationRegionEntityFactory()
              .withYieldedApArea { ApAreaEntityFactory().produce() }
              .produce()
          }
          .withService(ServiceName.approvedPremises.value)
          .withYieldedLocalAuthorityArea { LocalAuthorityEntityFactory().produce() }
          .produce()
      }
      .withApplication(application.let { application })
      .produce()

  @Nested
  inner class CreateTemporaryAccommodationBooking {

    @BeforeEach
    fun setup() {
      every { mockBedRepository.findArchivedBedByBedIdAndDate(any(), any()) } returns null
    }

    @Test
    fun `createTemporaryAccommodationBooking returns FieldValidationError if Bed ID is not provided`() {
      val crn = "CRN123"
      val assessmentId = UUID.randomUUID()

      val premises = TemporaryAccommodationPremisesEntityFactory()
        .withUnitTestControlTestProbationAreaAndLocalAuthority()
        .produce()

      val authorisableResult = bookingService.createTemporaryAccommodationBooking(
        user,
        premises,
        crn,
        "NOMS123",
        LocalDate.parse("2023-02-23"),
        LocalDate.parse("2023-02-22"),
        bedId = null,
        assessmentId,
        false,
      )
      assertThat(authorisableResult is AuthorisableActionResult.Success).isTrue

      val validatableResult = (authorisableResult as AuthorisableActionResult.Success).entity
      assertThat(validatableResult is ValidatableActionResult.FieldValidationError)

      assertThat((validatableResult as ValidatableActionResult.FieldValidationError).validationMessages).contains(
        entry("$.bedId", "empty"),
      )
    }

    @Test
    fun `createTemporaryAccommodationBooking returns FieldValidationError if Departure Date is before Arrival Date`() {
      val crn = "CRN123"
      val bedId = UUID.fromString("3b2f46de-a785-45ab-ac02-5e532c600647")
      val arrivalDate = LocalDate.parse("2023-02-23")
      val departureDate = LocalDate.parse("2023-02-22")
      val assessmentId = UUID.randomUUID()

      every { mockBedRepository.findByIdOrNull(bedId) } returns null

      every { mockBookingRepository.findByBedIdAndArrivingBeforeDate(bedId, departureDate, null) } returns listOf()
      every {
        mockLostBedsRepository.findByBedIdAndOverlappingDate(
          bedId,
          arrivalDate,
          departureDate,
          null,
        )
      } returns listOf()
      every { mockAssessmentRepository.findByIdOrNull(assessmentId) } returns null

      every { mockWorkingDayService.addWorkingDays(any(), any()) } answers { it.invocation.args[0] as LocalDate }

      val user = UserEntityFactory()
        .withUnitTestControlProbationRegion()
        .produce()

      val premises = TemporaryAccommodationPremisesEntityFactory()
        .withUnitTestControlTestProbationAreaAndLocalAuthority()
        .produce()

      val authorisableResult = bookingService.createTemporaryAccommodationBooking(
        user,
        premises,
        crn,
        "NOMS123",
        LocalDate.parse("2023-02-23"),
        LocalDate.parse("2023-02-22"),
        bedId,
        assessmentId,
        false,
      )
      assertThat(authorisableResult is AuthorisableActionResult.Success).isTrue

      val validatableResult = (authorisableResult as AuthorisableActionResult.Success).entity
      assertThat(validatableResult is ValidatableActionResult.FieldValidationError)

      assertThat((validatableResult as ValidatableActionResult.FieldValidationError).validationMessages).contains(
        entry("$.departureDate", "beforeBookingArrivalDate"),
      )
    }

    @Test
    fun `createTemporaryAccommodationBooking returns FieldValidationError if Bed does not exist`() {
      val crn = "CRN123"
      val bedId = UUID.fromString("3b2f46de-a785-45ab-ac02-5e532c600647")
      val arrivalDate = LocalDate.parse("2023-02-23")
      val departureDate = LocalDate.parse("2023-02-22")
      val assessmentId = UUID.randomUUID()

      every { mockBedRepository.findByIdOrNull(bedId) } returns null
      every { mockBookingRepository.findByBedIdAndArrivingBeforeDate(bedId, departureDate, null) } returns listOf()
      every {
        mockLostBedsRepository.findByBedIdAndOverlappingDate(
          bedId,
          arrivalDate,
          departureDate,
          null,
        )
      } returns listOf()
      every { mockAssessmentRepository.findByIdOrNull(assessmentId) } returns null

      every { mockWorkingDayService.addWorkingDays(any(), any()) } answers { it.invocation.args[0] as LocalDate }

      val user = UserEntityFactory()
        .withUnitTestControlProbationRegion()
        .produce()

      val premises = TemporaryAccommodationPremisesEntityFactory()
        .withUnitTestControlTestProbationAreaAndLocalAuthority()
        .produce()

      val authorisableResult = bookingService.createTemporaryAccommodationBooking(
        user,
        premises,
        crn,
        "NOMS123",
        arrivalDate,
        departureDate,
        bedId,
        assessmentId,
        false,
      )
      assertThat(authorisableResult is AuthorisableActionResult.Success).isTrue

      val validatableResult = (authorisableResult as AuthorisableActionResult.Success).entity
      assertThat(validatableResult is ValidatableActionResult.FieldValidationError)

      assertThat((validatableResult as ValidatableActionResult.FieldValidationError).validationMessages).contains(
        entry("$.bedId", "doesNotExist"),
      )
    }

    @Test
    fun `createTemporaryAccommodationBooking returns FieldValidationError if Application is provided and does not exist`() {
      val crn = "CRN123"
      val arrivalDate = LocalDate.parse("2023-02-23")
      val departureDate = LocalDate.parse("2023-02-24")
      val assessmentId = UUID.randomUUID()

      val user = UserEntityFactory()
        .withUnitTestControlProbationRegion()
        .produce()

      val premises = TemporaryAccommodationPremisesEntityFactory()
        .withUnitTestControlTestProbationAreaAndLocalAuthority()
        .produce()

      val room = RoomEntityFactory()
        .withPremises(premises)
        .produce()

      val bed = BedEntityFactory()
        .withRoom(room)
        .produce()

      every { mockBedRepository.findByIdOrNull(bed.id) } returns bed

      every { mockBookingRepository.save(any()) } answers { it.invocation.args[0] as BookingEntity }

      every { mockBookingRepository.findByBedIdAndArrivingBeforeDate(bed.id, departureDate, null) } returns listOf()
      every {
        mockLostBedsRepository.findByBedIdAndOverlappingDate(
          bed.id,
          arrivalDate,
          departureDate,
          null,
        )
      } returns listOf()
      every { mockAssessmentRepository.findByIdOrNull(assessmentId) } returns null
      every { mockTurnaroundRepository.save(any()) } answers { it.invocation.args[0] as TurnaroundEntity }

      every { mockWorkingDayService.addWorkingDays(any(), any()) } answers { it.invocation.args[0] as LocalDate }

      val authorisableResult = bookingService.createTemporaryAccommodationBooking(
        user,
        premises,
        crn,
        "NOMS123",
        arrivalDate,
        departureDate,
        bed.id,
        assessmentId,
        false,
      )
      assertThat(authorisableResult is AuthorisableActionResult.Success).isTrue

      val validatableResult = (authorisableResult as AuthorisableActionResult.Success).entity
      assertThat(validatableResult is ValidatableActionResult.FieldValidationError)

      assertThat((validatableResult as ValidatableActionResult.FieldValidationError).validationMessages).contains(
        entry("$.assessmentId", "doesNotExist"),
      )
    }

    @Test
    fun `createTemporaryAccommodationBooking saves Booking and creates domain event`() {
      val crn = "CRN123"
      val arrivalDate = LocalDate.parse("2023-02-23")
      val departureDate = LocalDate.parse("2023-02-24")

      val user = UserEntityFactory()
        .withUnitTestControlProbationRegion()
        .produce()

      val premises = TemporaryAccommodationPremisesEntityFactory()
        .withUnitTestControlTestProbationAreaAndLocalAuthority()
        .produce()

      val room = RoomEntityFactory()
        .withPremises(premises)
        .produce()

      val bed = BedEntityFactory()
        .withRoom(room)
        .produce()

      val application = TemporaryAccommodationApplicationEntityFactory()
        .withProbationRegion(user.probationRegion)
        .withCreatedByUser(user)
        .produce()

      val assessment = TemporaryAccommodationAssessmentEntityFactory()
        .withApplication(application)
        .produce()

      every { mockBedRepository.findByIdOrNull(bed.id) } returns bed

      every { mockBookingRepository.save(any()) } answers { it.invocation.args[0] as BookingEntity }

      every { mockBookingRepository.findByBedIdAndArrivingBeforeDate(bed.id, departureDate, null) } returns listOf()
      every {
        mockLostBedsRepository.findByBedIdAndOverlappingDate(
          bed.id,
          arrivalDate,
          departureDate,
          null,
        )
      } returns listOf()
      every { mockAssessmentRepository.findByIdOrNull(application.id) } returns assessment

      every { mockTurnaroundRepository.save(any()) } answers { it.invocation.args[0] as TurnaroundEntity }

      every { mockWorkingDayService.addWorkingDays(any(), any()) } answers { it.invocation.args[0] as LocalDate }

      every { mockCas3DomainEventService.saveBookingProvisionallyMadeEvent(any(), user) } just Runs

      val authorisableResult = bookingService.createTemporaryAccommodationBooking(
        user,
        premises,
        crn,
        "NOMS123",
        arrivalDate,
        departureDate,
        bed.id,
        application.id,
        false,
      )
      assertThat(authorisableResult is AuthorisableActionResult.Success).isTrue

      val validatableResult = (authorisableResult as AuthorisableActionResult.Success).entity
      assertThat(validatableResult is ValidatableActionResult.Success)

      verify(exactly = 1) {
        mockBookingRepository.save(
          match {
            it.crn == crn &&
              it.premises == premises &&
              it.arrivalDate == arrivalDate &&
              it.departureDate == departureDate &&
              it.application == application &&
              it.status == BookingStatus.provisional
          },
        )
      }

      verify(exactly = 1) {
        mockCas3DomainEventService.saveBookingProvisionallyMadeEvent(
          match {
            it.crn == crn &&
              it.premises == premises &&
              it.arrivalDate == arrivalDate &&
              it.departureDate == departureDate &&
              it.application == application
          },
          user,
        )
      }
    }

    @Test
    fun `createTemporaryAccommodationBooking does not attach the application if no ID is provided`() {
      val crn = "CRN123"
      val arrivalDate = LocalDate.parse("2023-02-23")
      val departureDate = LocalDate.parse("2023-02-24")

      val user = UserEntityFactory()
        .withUnitTestControlProbationRegion()
        .produce()

      val premises = TemporaryAccommodationPremisesEntityFactory()
        .withUnitTestControlTestProbationAreaAndLocalAuthority()
        .produce()

      val room = RoomEntityFactory()
        .withPremises(premises)
        .produce()

      val bed = BedEntityFactory()
        .withRoom(room)
        .produce()

      every { mockBedRepository.findByIdOrNull(bed.id) } returns bed

      every { mockBookingRepository.save(any()) } answers { it.invocation.args[0] as BookingEntity }

      every { mockBookingRepository.findByBedIdAndArrivingBeforeDate(bed.id, departureDate, null) } returns listOf()
      every {
        mockLostBedsRepository.findByBedIdAndOverlappingDate(
          bed.id,
          arrivalDate,
          departureDate,
          null,
        )
      } returns listOf()

      every { mockTurnaroundRepository.save(any()) } answers { it.invocation.args[0] as TurnaroundEntity }

      every { mockWorkingDayService.addWorkingDays(any(), any()) } answers { it.invocation.args[0] as LocalDate }

      every { mockCas3DomainEventService.saveBookingProvisionallyMadeEvent(any(), user) } just Runs

      mockkStatic(Sentry::class)

      val authorisableResult = bookingService.createTemporaryAccommodationBooking(
        user,
        premises,
        crn,
        "NOMS123",
        arrivalDate,
        departureDate,
        bed.id,
        null,
        false,
      )
      assertThat(authorisableResult is AuthorisableActionResult.Success).isTrue

      val validatableResult = (authorisableResult as AuthorisableActionResult.Success).entity
      assertThat(validatableResult is ValidatableActionResult.Success)

      verify(exactly = 1) {
        mockBookingRepository.save(
          match {
            it.crn == crn &&
              it.premises == premises &&
              it.arrivalDate == arrivalDate &&
              it.departureDate == departureDate &&
              it.application == null &&
              it.status == BookingStatus.provisional
          },
        )
      }

      verify(exactly = 0) {
        mockAssessmentRepository.findByIdOrNull(any())
      }
    }

    @Test
    fun `createTemporaryAccommodationBooking automatically creates a Turnaround of zero days if 'enableTurnarounds' is false`() {
      val crn = "CRN123"
      val arrivalDate = LocalDate.parse("2023-02-23")
      val departureDate = LocalDate.parse("2023-02-24")

      val user = UserEntityFactory()
        .withUnitTestControlProbationRegion()
        .produce()

      val premises = TemporaryAccommodationPremisesEntityFactory()
        .withUnitTestControlTestProbationAreaAndLocalAuthority()
        .withTurnaroundWorkingDayCount(4)
        .produce()

      val room = RoomEntityFactory()
        .withPremises(premises)
        .produce()

      val bed = BedEntityFactory()
        .withRoom(room)
        .produce()

      val application = TemporaryAccommodationApplicationEntityFactory()
        .withProbationRegion(user.probationRegion)
        .withCreatedByUser(user)
        .produce()

      val assessment = TemporaryAccommodationAssessmentEntityFactory()
        .withApplication(application)
        .produce()

      every { mockBedRepository.findByIdOrNull(bed.id) } returns bed

      val bookingSlot = slot<BookingEntity>()
      every { mockBookingRepository.save(capture(bookingSlot)) } answers { bookingSlot.captured }

      every { mockBookingRepository.findByBedIdAndArrivingBeforeDate(bed.id, departureDate, null) } returns listOf()
      every {
        mockLostBedsRepository.findByBedIdAndOverlappingDate(
          bed.id,
          arrivalDate,
          departureDate,
          null,
        )
      } returns listOf()
      every { mockAssessmentRepository.findByIdOrNull(application.id) } returns assessment

      every { mockTurnaroundRepository.save(any()) } answers { it.invocation.args[0] as TurnaroundEntity }

      every { mockWorkingDayService.addWorkingDays(any(), any()) } answers { it.invocation.args[0] as LocalDate }

      every { mockCas3DomainEventService.saveBookingProvisionallyMadeEvent(any(), user) } just Runs

      val authorisableResult = bookingService.createTemporaryAccommodationBooking(
        user,
        premises,
        crn,
        "NOMS123",
        arrivalDate,
        departureDate,
        bed.id,
        application.id,
        false,
      )
      assertThat(authorisableResult is AuthorisableActionResult.Success).isTrue

      val validatableResult = (authorisableResult as AuthorisableActionResult.Success).entity
      assertThat(validatableResult is ValidatableActionResult.Success)

      verify(exactly = 1) {
        mockBookingRepository.save(
          match {
            it.crn == crn &&
              it.premises == premises &&
              it.arrivalDate == arrivalDate &&
              it.departureDate == departureDate &&
              it.application == application &&
              it.status == BookingStatus.provisional
          },
        )
      }

      verify(exactly = 1) {
        mockTurnaroundRepository.save(
          match {
            it.booking == bookingSlot.captured &&
              it.workingDayCount == 0
          },
        )
      }
    }

    @Test
    fun `createTemporaryAccommodationBooking automatically creates a Turnaround of the number of working days specified on the premises if 'enableTurnarounds' is true`() {
      val crn = "CRN123"
      val arrivalDate = LocalDate.parse("2023-02-23")
      val departureDate = LocalDate.parse("2023-02-24")

      val user = UserEntityFactory()
        .withUnitTestControlProbationRegion()
        .produce()

      val premises = TemporaryAccommodationPremisesEntityFactory()
        .withUnitTestControlTestProbationAreaAndLocalAuthority()
        .withTurnaroundWorkingDayCount(4)
        .produce()

      val room = RoomEntityFactory()
        .withPremises(premises)
        .produce()

      val bed = BedEntityFactory()
        .withRoom(room)
        .produce()

      val application = TemporaryAccommodationApplicationEntityFactory()
        .withProbationRegion(user.probationRegion)
        .withCreatedByUser(user)
        .produce()

      val assessment = TemporaryAccommodationAssessmentEntityFactory()
        .withApplication(application)
        .produce()

      every { mockBedRepository.findByIdOrNull(bed.id) } returns bed

      val bookingSlot = slot<BookingEntity>()
      every { mockBookingRepository.save(capture(bookingSlot)) } answers { bookingSlot.captured }

      every { mockBookingRepository.findByBedIdAndArrivingBeforeDate(bed.id, departureDate, null) } returns listOf()
      every {
        mockLostBedsRepository.findByBedIdAndOverlappingDate(
          bed.id,
          arrivalDate,
          departureDate,
          null,
        )
      } returns listOf()
      every { mockAssessmentRepository.findByIdOrNull(application.id) } returns assessment

      every { mockTurnaroundRepository.save(any()) } answers { it.invocation.args[0] as TurnaroundEntity }

      every { mockWorkingDayService.addWorkingDays(any(), any()) } answers { it.invocation.args[0] as LocalDate }

      every { mockCas3DomainEventService.saveBookingProvisionallyMadeEvent(any(), user) } just Runs

      val authorisableResult = bookingService.createTemporaryAccommodationBooking(
        user,
        premises,
        crn,
        "NOMS123",
        arrivalDate,
        departureDate,
        bed.id,
        application.id,
        true,
      )
      assertThat(authorisableResult is AuthorisableActionResult.Success).isTrue

      val validatableResult = (authorisableResult as AuthorisableActionResult.Success).entity
      assertThat(validatableResult is ValidatableActionResult.Success)

      verify(exactly = 1) {
        mockBookingRepository.save(
          match {
            it.crn == crn &&
              it.premises == premises &&
              it.arrivalDate == arrivalDate &&
              it.departureDate == departureDate &&
              it.application == application &&
              it.status == BookingStatus.provisional
          },
        )
      }

      verify(exactly = 1) {
        mockTurnaroundRepository.save(
          match {
            it.booking == bookingSlot.captured &&
              it.workingDayCount == premises.turnaroundWorkingDayCount
          },
        )
      }
    }

    @Test
    fun `createTemporaryAccommodationBooking saves Booking and creates domain event and log error when closing assessment failed with authentication error`() {
      val crn = "CRN123"
      val arrivalDate = LocalDate.parse("2023-02-23")
      val departureDate = LocalDate.parse("2023-02-24")

      val user = UserEntityFactory()
        .withUnitTestControlProbationRegion()
        .produce()

      val premises = TemporaryAccommodationPremisesEntityFactory()
        .withUnitTestControlTestProbationAreaAndLocalAuthority()
        .produce()

      val room = RoomEntityFactory()
        .withPremises(premises)
        .produce()

      val bed = BedEntityFactory()
        .withRoom(room)
        .produce()

      val application = TemporaryAccommodationApplicationEntityFactory()
        .withProbationRegion(user.probationRegion)
        .withCreatedByUser(user)
        .produce()

      val assessment = TemporaryAccommodationAssessmentEntityFactory()
        .withApplication(application)
        .produce()

      every { mockBedRepository.findByIdOrNull(bed.id) } returns bed

      every { mockBookingRepository.save(any()) } answers { it.invocation.args[0] as BookingEntity }

      every { mockBookingRepository.findByBedIdAndArrivingBeforeDate(bed.id, departureDate, null) } returns listOf()
      every {
        mockLostBedsRepository.findByBedIdAndOverlappingDate(
          bed.id,
          arrivalDate,
          departureDate,
          null,
        )
      } returns listOf()
      every { mockAssessmentRepository.findByIdOrNull(application.id) } returns assessment

      every { mockTurnaroundRepository.save(any()) } answers { it.invocation.args[0] as TurnaroundEntity }

      every { mockWorkingDayService.addWorkingDays(any(), any()) } answers { it.invocation.args[0] as LocalDate }

      every { mockCas3DomainEventService.saveBookingProvisionallyMadeEvent(any(), user) } just Runs

      mockkStatic(Sentry::class)
      every { Sentry.captureException(any()) } returns SentryId.EMPTY_ID

      val authorisableResult = bookingService.createTemporaryAccommodationBooking(
        user,
        premises,
        crn,
        "NOMS123",
        arrivalDate,
        departureDate,
        bed.id,
        application.id,
        false,
      )
      assertThat(authorisableResult is AuthorisableActionResult.Success).isTrue

      val validatableResult = (authorisableResult as AuthorisableActionResult.Success).entity
      assertThat(validatableResult is ValidatableActionResult.Success)

      verify(exactly = 1) {
        mockBookingRepository.save(
          match {
            it.crn == crn &&
              it.premises == premises &&
              it.arrivalDate == arrivalDate &&
              it.departureDate == departureDate &&
              it.application == application &&
              it.status == BookingStatus.provisional
          },
        )
      }

      verify(exactly = 1) {
        mockCas3DomainEventService.saveBookingProvisionallyMadeEvent(
          match {
            it.crn == crn &&
              it.premises == premises &&
              it.arrivalDate == arrivalDate &&
              it.departureDate == departureDate &&
              it.application == application
          },
          user,
        )
      }
    }

    @Test
    fun `createTemporaryAccommodationBooking saves Booking and creates domain event and log error when closing assessment failed with field validation error`() {
      val crn = "CRN123"
      val arrivalDate = LocalDate.parse("2023-02-23")
      val departureDate = LocalDate.parse("2023-02-24")

      val user = UserEntityFactory()
        .withUnitTestControlProbationRegion()
        .produce()

      val premises = TemporaryAccommodationPremisesEntityFactory()
        .withUnitTestControlTestProbationAreaAndLocalAuthority()
        .produce()

      val room = RoomEntityFactory()
        .withPremises(premises)
        .produce()

      val bed = BedEntityFactory()
        .withRoom(room)
        .produce()

      val application = TemporaryAccommodationApplicationEntityFactory()
        .withProbationRegion(user.probationRegion)
        .withCreatedByUser(user)
        .produce()

      val assessment = TemporaryAccommodationAssessmentEntityFactory()
        .withApplication(application)
        .produce()

      every { mockBedRepository.findByIdOrNull(bed.id) } returns bed

      every { mockBookingRepository.save(any()) } answers { it.invocation.args[0] as BookingEntity }

      every { mockBookingRepository.findByBedIdAndArrivingBeforeDate(bed.id, departureDate, null) } returns listOf()
      every {
        mockLostBedsRepository.findByBedIdAndOverlappingDate(
          bed.id,
          arrivalDate,
          departureDate,
          null,
        )
      } returns listOf()
      every { mockAssessmentRepository.findByIdOrNull(application.id) } returns assessment

      every { mockTurnaroundRepository.save(any()) } answers { it.invocation.args[0] as TurnaroundEntity }

      every { mockWorkingDayService.addWorkingDays(any(), any()) } answers { it.invocation.args[0] as LocalDate }

      every { mockCas3DomainEventService.saveBookingProvisionallyMadeEvent(any(), user) } just Runs

      val authorisableResult = bookingService.createTemporaryAccommodationBooking(
        user,
        premises,
        crn,
        "NOMS123",
        arrivalDate,
        departureDate,
        bed.id,
        application.id,
        false,
      )
      assertThat(authorisableResult is AuthorisableActionResult.Success).isTrue

      val validatableResult = (authorisableResult as AuthorisableActionResult.Success).entity
      assertThat(validatableResult is ValidatableActionResult.Success)

      verify(exactly = 1) {
        mockBookingRepository.save(
          match {
            it.crn == crn &&
              it.premises == premises &&
              it.arrivalDate == arrivalDate &&
              it.departureDate == departureDate &&
              it.application == application &&
              it.status == BookingStatus.provisional
          },
        )
      }

      verify(exactly = 1) {
        mockCas3DomainEventService.saveBookingProvisionallyMadeEvent(
          match {
            it.crn == crn &&
              it.premises == premises &&
              it.arrivalDate == arrivalDate &&
              it.departureDate == departureDate &&
              it.application == application
          },
          user,
        )
      }
    }

    @Test
    fun `createTemporaryAccommodationBooking saves Booking and creates domain event and log error when closing assessment failed with runtime exception`() {
      val crn = "CRN123"
      val arrivalDate = LocalDate.parse("2023-02-23")
      val departureDate = LocalDate.parse("2023-02-24")

      val user = UserEntityFactory()
        .withUnitTestControlProbationRegion()
        .produce()

      val premises = TemporaryAccommodationPremisesEntityFactory()
        .withUnitTestControlTestProbationAreaAndLocalAuthority()
        .produce()

      val room = RoomEntityFactory()
        .withPremises(premises)
        .produce()

      val bed = BedEntityFactory()
        .withRoom(room)
        .produce()

      val application = TemporaryAccommodationApplicationEntityFactory()
        .withProbationRegion(user.probationRegion)
        .withCreatedByUser(user)
        .produce()

      val assessment = TemporaryAccommodationAssessmentEntityFactory()
        .withApplication(application)
        .produce()

      every { mockBedRepository.findByIdOrNull(bed.id) } returns bed

      every { mockBookingRepository.save(any()) } answers { it.invocation.args[0] as BookingEntity }

      every { mockBookingRepository.findByBedIdAndArrivingBeforeDate(bed.id, departureDate, null) } returns listOf()
      every {
        mockLostBedsRepository.findByBedIdAndOverlappingDate(
          bed.id,
          arrivalDate,
          departureDate,
          null,
        )
      } returns listOf()
      every { mockAssessmentRepository.findByIdOrNull(application.id) } returns assessment

      every { mockTurnaroundRepository.save(any()) } answers { it.invocation.args[0] as TurnaroundEntity }

      every { mockWorkingDayService.addWorkingDays(any(), any()) } answers { it.invocation.args[0] as LocalDate }

      every { mockCas3DomainEventService.saveBookingProvisionallyMadeEvent(any(), user) } just Runs

      val authorisableResult = bookingService.createTemporaryAccommodationBooking(
        user,
        premises,
        crn,
        "NOMS123",
        arrivalDate,
        departureDate,
        bed.id,
        application.id,
        false,
      )
      assertThat(authorisableResult is AuthorisableActionResult.Success).isTrue

      val validatableResult = (authorisableResult as AuthorisableActionResult.Success).entity
      assertThat(validatableResult is ValidatableActionResult.Success)

      verify(exactly = 1) {
        mockBookingRepository.save(
          match {
            it.crn == crn &&
              it.premises == premises &&
              it.arrivalDate == arrivalDate &&
              it.departureDate == departureDate &&
              it.application == application &&
              it.status == BookingStatus.provisional
          },
        )
      }

      verify(exactly = 1) {
        mockCas3DomainEventService.saveBookingProvisionallyMadeEvent(
          match {
            it.crn == crn &&
              it.premises == premises &&
              it.arrivalDate == arrivalDate &&
              it.departureDate == departureDate &&
              it.application == application
          },
          user,
        )
      }
    }

    @Test
    fun `createTemporaryAccommodationBooking not closing assessments and returns FieldValidationError if Application is provided and does not exist`() {
      val crn = "CRN123"
      val arrivalDate = LocalDate.parse("2023-02-23")
      val departureDate = LocalDate.parse("2023-02-24")
      val assessmentId = UUID.randomUUID()

      val user = UserEntityFactory()
        .withUnitTestControlProbationRegion()
        .produce()

      val premises = TemporaryAccommodationPremisesEntityFactory()
        .withUnitTestControlTestProbationAreaAndLocalAuthority()
        .produce()

      val room = RoomEntityFactory()
        .withPremises(premises)
        .produce()

      val bed = BedEntityFactory()
        .withRoom(room)
        .produce()

      every { mockBedRepository.findByIdOrNull(bed.id) } returns bed

      every { mockBookingRepository.save(any()) } answers { it.invocation.args[0] as BookingEntity }

      every { mockBookingRepository.findByBedIdAndArrivingBeforeDate(bed.id, departureDate, null) } returns listOf()
      every {
        mockLostBedsRepository.findByBedIdAndOverlappingDate(
          bed.id,
          arrivalDate,
          departureDate,
          null,
        )
      } returns listOf()
      every { mockAssessmentRepository.findByIdOrNull(assessmentId) } returns null
      every { mockTurnaroundRepository.save(any()) } answers { it.invocation.args[0] as TurnaroundEntity }

      every { mockWorkingDayService.addWorkingDays(any(), any()) } answers { it.invocation.args[0] as LocalDate }

      val authorisableResult = bookingService.createTemporaryAccommodationBooking(
        user,
        premises,
        crn,
        "NOMS123",
        arrivalDate,
        departureDate,
        bed.id,
        assessmentId,
        false,
      )
      assertThat(authorisableResult is AuthorisableActionResult.Success).isTrue

      val validatableResult = (authorisableResult as AuthorisableActionResult.Success).entity
      assertThat(validatableResult is ValidatableActionResult.FieldValidationError)

      assertThat((validatableResult as ValidatableActionResult.FieldValidationError).validationMessages).contains(
        entry("$.assessmentId", "doesNotExist"),
      )
    }

    @Test
    fun `createTemporaryAccommodationBooking fail to save booking and not closing assessment when saving booking triggered Database exception`() {
      val crn = "CRN123"
      val arrivalDate = LocalDate.parse("2023-02-23")
      val departureDate = LocalDate.parse("2023-02-24")

      val user = UserEntityFactory()
        .withUnitTestControlProbationRegion()
        .produce()

      val premises = TemporaryAccommodationPremisesEntityFactory()
        .withUnitTestControlTestProbationAreaAndLocalAuthority()
        .produce()

      val room = RoomEntityFactory()
        .withPremises(premises)
        .produce()

      val bed = BedEntityFactory()
        .withRoom(room)
        .produce()

      val application = TemporaryAccommodationApplicationEntityFactory()
        .withProbationRegion(user.probationRegion)
        .withCreatedByUser(user)
        .produce()

      val assessment = TemporaryAccommodationAssessmentEntityFactory()
        .withApplication(application)
        .produce()

      every { mockBedRepository.findByIdOrNull(bed.id) } returns bed

      every { mockBookingRepository.save(any()) } throws RuntimeException("DB exception")

      every { mockBookingRepository.findByBedIdAndArrivingBeforeDate(bed.id, departureDate, null) } returns listOf()
      every {
        mockLostBedsRepository.findByBedIdAndOverlappingDate(
          bed.id,
          arrivalDate,
          departureDate,
          null,
        )
      } returns listOf()
      every { mockAssessmentRepository.findByIdOrNull(application.id) } returns assessment

      every { mockTurnaroundRepository.save(any()) } answers { it.invocation.args[0] as TurnaroundEntity }

      every { mockWorkingDayService.addWorkingDays(any(), any()) } answers { it.invocation.args[0] as LocalDate }

      every { mockCas3DomainEventService.saveBookingProvisionallyMadeEvent(any(), user) } just Runs

      assertThatExceptionOfType(RuntimeException::class.java)
        .isThrownBy {
          bookingService.createTemporaryAccommodationBooking(
            user,
            premises,
            crn,
            "NOMS123",
            arrivalDate,
            departureDate,
            bed.id,
            application.id,
            false,
          )
        }

      verify(exactly = 1) {
        mockBookingRepository.save(
          match {
            it.crn == crn &&
              it.premises == premises &&
              it.arrivalDate == arrivalDate &&
              it.departureDate == departureDate &&
              it.application == application &&
              it.status == BookingStatus.provisional
          },
        )
      }

      verify(exactly = 0) {
        mockCas3DomainEventService.saveBookingProvisionallyMadeEvent(
          match {
            it.crn == crn &&
              it.premises == premises &&
              it.arrivalDate == arrivalDate &&
              it.departureDate == departureDate &&
              it.application == application
          },
          user,
        )
      }
    }

    @Test
    fun `createTemporaryAccommodationBooking throws conflict error when bedspace is archived for the requested bed`() {
      val crn = "CRN123"
      val arrivalDate = LocalDate.parse("2023-02-23")
      val departureDate = LocalDate.parse("2023-02-24")

      val user = UserEntityFactory()
        .withUnitTestControlProbationRegion()
        .produce()

      val premises = TemporaryAccommodationPremisesEntityFactory()
        .withUnitTestControlTestProbationAreaAndLocalAuthority()
        .produce()

      val room = RoomEntityFactory()
        .withPremises(premises)
        .produce()

      val bed = BedEntityFactory()
        .withRoom(room)
        .withEndDate { LocalDate.now() }
        .produce()

      val application = TemporaryAccommodationApplicationEntityFactory()
        .withProbationRegion(user.probationRegion)
        .withCreatedByUser(user)
        .produce()

      val assessment = TemporaryAccommodationAssessmentEntityFactory()
        .withApplication(application)
        .produce()

      every { mockBedRepository.findByIdOrNull(bed.id) } returns bed

      every { mockBookingRepository.save(any()) } answers { it.invocation.args[0] as BookingEntity }

      every { mockBookingRepository.findByBedIdAndArrivingBeforeDate(bed.id, departureDate, null) } returns listOf()
      every {
        mockLostBedsRepository.findByBedIdAndOverlappingDate(
          bed.id,
          arrivalDate,
          departureDate,
          null,
        )
      } returns listOf()
      every { mockAssessmentRepository.findByIdOrNull(application.id) } returns assessment

      every { mockTurnaroundRepository.save(any()) } answers { it.invocation.args[0] as TurnaroundEntity }

      every { mockWorkingDayService.addWorkingDays(any(), any()) } answers { it.invocation.args[0] as LocalDate }

      every { mockCas3DomainEventService.saveBookingProvisionallyMadeEvent(any(), user) } just Runs

      every { mockBedRepository.findArchivedBedByBedIdAndDate(any(), any()) } returns bed

      val authorisableResult = bookingService.createTemporaryAccommodationBooking(
        user,
        premises,
        crn,
        "NOMS123",
        arrivalDate,
        departureDate,
        bed.id,
        application.id,
        false,
      )

      assertThat(authorisableResult is AuthorisableActionResult.Success).isTrue
      authorisableResult as AuthorisableActionResult.Success
      assertThat(authorisableResult.entity is ValidatableActionResult.ConflictError).isTrue
      val validationError = authorisableResult.entity as ValidatableActionResult.ConflictError
      assertThat(validationError.message).isEqualTo("BedSpace is archived from ${LocalDate.now()} which overlaps with the desired dates")

      verify(exactly = 0) {
        mockBookingRepository.save(
          match {
            it.crn == crn &&
              it.premises == premises &&
              it.arrivalDate == arrivalDate &&
              it.departureDate == departureDate &&
              it.application == application &&
              it.status == BookingStatus.provisional
          },
        )
      }

      verify(exactly = 0) {
        mockCas3DomainEventService.saveBookingProvisionallyMadeEvent(
          match {
            it.crn == crn &&
              it.premises == premises &&
              it.arrivalDate == arrivalDate &&
              it.departureDate == departureDate &&
              it.application == application
          },
          user,
        )
      }

      verify {
        mockBedRepository.findArchivedBedByBedIdAndDate(any(), any())
      }
    }
  }

  @Nested
  inner class CreateApprovedPremisesBookingFromPlacementRequest {
    val user = UserEntityFactory()
      .withUnitTestControlProbationRegion()
      .produce()

    private val otherUser = UserEntityFactory()
      .withUnitTestControlProbationRegion()
      .produce()

    val application = ApprovedPremisesApplicationEntityFactory()
      .withCreatedByUser(otherUser)
      .withSubmittedAt(OffsetDateTime.now())
      .withReleaseType(ReleaseTypeOption.licence.toString())
      .withSentenceType(SentenceTypeOption.nonStatutory.toString())
      .withSituation(SituationOption.bailSentence.toString())
      .produce()

    val assessment = ApprovedPremisesAssessmentEntityFactory()
      .withApplication(application)
      .withAllocatedToUser(otherUser)
      .produce()

    @Test
    fun `returns Not Found if Placement Request can't be found`() {
      val placementRequestId = UUID.fromString("43d5ba3c-3eb1-4966-bcd1-c6d16be9f178")
      val bedId = UUID.fromString("d69c0e07-f362-4727-86a6-45aaa73c14af")

      val user = UserEntityFactory()
        .withUnitTestControlProbationRegion()
        .produce()

      every { mockPlacementRequestRepository.findByIdOrNull(placementRequestId) } returns null

      val result = bookingService.createApprovedPremisesBookingFromPlacementRequest(
        user = user,
        placementRequestId = placementRequestId,
        bedId = bedId,
        premisesId = null,
        arrivalDate = LocalDate.parse("2023-03-28"),
        departureDate = LocalDate.parse("2023-03-30"),
      )

      assertThat(result is AuthorisableActionResult.NotFound).isTrue
    }

    @Test
    fun `returns Unauthorised if Placement Request is not allocated to the User`() {
      val placementRequest = PlacementRequestEntityFactory()
        .withPlacementRequirements(
          PlacementRequirementsEntityFactory()
            .withApplication(application)
            .withAssessment(assessment)
            .produce(),
        )
        .withApplication(application)
        .withAssessment(assessment)
        .withAllocatedToUser(otherUser)
        .produce()

      val bedId = UUID.fromString("d69c0e07-f362-4727-86a6-45aaa73c14af")

      every { mockPlacementRequestRepository.findByIdOrNull(placementRequest.id) } returns placementRequest

      val result = bookingService.createApprovedPremisesBookingFromPlacementRequest(
        user = user,
        placementRequestId = placementRequest.id,
        bedId = bedId,
        premisesId = null,
        arrivalDate = LocalDate.parse("2023-03-28"),
        departureDate = LocalDate.parse("2023-03-30"),
      )

      assertThat(result is AuthorisableActionResult.Unauthorised).isTrue
    }

    @Test
    fun `returns GeneralValidationError if Bed does not belong to an Approved Premises`() {
      val arrivalDate = LocalDate.parse("2023-03-28")
      val departureDate = LocalDate.parse("2023-03-30")

      val placementRequest = PlacementRequestEntityFactory()
        .withApplication(application)
        .withPlacementRequirements(
          PlacementRequirementsEntityFactory()
            .withApplication(application)
            .withAssessment(assessment)
            .produce(),
        )
        .withAssessment(assessment)
        .withAllocatedToUser(user)
        .produce()

      val premises = TemporaryAccommodationPremisesEntityFactory()
        .withUnitTestControlTestProbationAreaAndLocalAuthority()
        .produce()

      val room = RoomEntityFactory()
        .withPremises(premises)
        .produce()

      val bed = BedEntityFactory()
        .withRoom(room)
        .produce()

      every { mockPlacementRequestRepository.findByIdOrNull(placementRequest.id) } returns placementRequest
      every { mockBedRepository.findByIdOrNull(bed.id) } returns bed

      every { mockBookingRepository.findByBedIdAndArrivingBeforeDate(bed.id, departureDate, null) } returns listOf()
      every { mockLostBedsRepository.findByBedIdAndOverlappingDate(bed.id, arrivalDate, departureDate, null) } returns listOf()

      val result = bookingService.createApprovedPremisesBookingFromPlacementRequest(
        user = user,
        placementRequestId = placementRequest.id,
        bedId = bed.id,
        premisesId = null,
        arrivalDate = arrivalDate,
        departureDate = departureDate,
      )

      assertThat(result is AuthorisableActionResult.Success).isTrue
      result as AuthorisableActionResult.Success
      assertThat(result.entity is ValidatableActionResult.FieldValidationError).isTrue
      val validationError = result.entity as ValidatableActionResult.FieldValidationError

      assertThat(validationError.validationMessages["$.bedId"]).isEqualTo("mustBelongToApprovedPremises")
    }

    @Test
    fun `returns GeneralValidationError if a premisesId is specified and the Premises is not an Approved Premises`() {
      val arrivalDate = LocalDate.parse("2023-03-28")
      val departureDate = LocalDate.parse("2023-03-30")

      val placementRequest = PlacementRequestEntityFactory()
        .withApplication(application)
        .withPlacementRequirements(
          PlacementRequirementsEntityFactory()
            .withApplication(application)
            .withAssessment(assessment)
            .produce(),
        )
        .withAssessment(assessment)
        .withAllocatedToUser(user)
        .produce()

      val premises = TemporaryAccommodationPremisesEntityFactory()
        .withUnitTestControlTestProbationAreaAndLocalAuthority()
        .produce()

      every { mockPlacementRequestRepository.findByIdOrNull(placementRequest.id) } returns placementRequest
      every { mockPremisesRepository.findByIdOrNull(premises.id) } returns premises

      val result = bookingService.createApprovedPremisesBookingFromPlacementRequest(
        user = user,
        placementRequestId = placementRequest.id,
        bedId = null,
        premisesId = premises.id,
        arrivalDate = arrivalDate,
        departureDate = departureDate,
      )

      assertThat(result is AuthorisableActionResult.Success).isTrue
      result as AuthorisableActionResult.Success
      assertThat(result.entity is ValidatableActionResult.FieldValidationError).isTrue
      val validationError = result.entity as ValidatableActionResult.FieldValidationError

      assertThat(validationError.validationMessages["$.premisesId"]).isEqualTo("mustBeAnApprovedPremises")
    }

    @Test
    fun `returns GeneralValidationError if a premisesId is specified and the Premises does not exist`() {
      val arrivalDate = LocalDate.parse("2023-03-28")
      val departureDate = LocalDate.parse("2023-03-30")

      val placementRequest = PlacementRequestEntityFactory()
        .withApplication(application)
        .withPlacementRequirements(
          PlacementRequirementsEntityFactory()
            .withApplication(application)
            .withAssessment(assessment)
            .produce(),
        )
        .withAssessment(assessment)
        .withAllocatedToUser(user)
        .produce()

      val premisesId = UUID.randomUUID()

      every { mockPlacementRequestRepository.findByIdOrNull(placementRequest.id) } returns placementRequest
      every { mockPremisesRepository.findByIdOrNull(premisesId) } returns null

      val result = bookingService.createApprovedPremisesBookingFromPlacementRequest(
        user = user,
        placementRequestId = placementRequest.id,
        bedId = null,
        premisesId = premisesId,
        arrivalDate = arrivalDate,
        departureDate = departureDate,
      )

      assertThat(result is AuthorisableActionResult.Success).isTrue
      result as AuthorisableActionResult.Success
      assertThat(result.entity is ValidatableActionResult.FieldValidationError).isTrue
      val validationError = result.entity as ValidatableActionResult.FieldValidationError

      assertThat(validationError.validationMessages["$.premisesId"]).isEqualTo("doesNotExist")
    }

    @Test
    fun `returns GeneralValidationError if a bedId or premisesId is not specified`() {
      val arrivalDate = LocalDate.parse("2023-03-28")
      val departureDate = LocalDate.parse("2023-03-30")

      val placementRequest = PlacementRequestEntityFactory()
        .withApplication(application)
        .withPlacementRequirements(
          PlacementRequirementsEntityFactory()
            .withApplication(application)
            .withAssessment(assessment)
            .produce(),
        )
        .withAssessment(assessment)
        .withAllocatedToUser(user)
        .produce()

      every { mockPlacementRequestRepository.findByIdOrNull(placementRequest.id) } returns placementRequest

      val result = bookingService.createApprovedPremisesBookingFromPlacementRequest(
        user = user,
        placementRequestId = placementRequest.id,
        bedId = null,
        premisesId = null,
        arrivalDate = arrivalDate,
        departureDate = departureDate,
      )

      assertThat(result is AuthorisableActionResult.Success).isTrue
      result as AuthorisableActionResult.Success
      assertThat(result.entity is ValidatableActionResult.GeneralValidationError).isTrue
      val error = result.entity as ValidatableActionResult.GeneralValidationError

      assertThat(error.message).isEqualTo("You must identify the AP Area and Premises name")
    }

    @Test
    fun `createApprovedPremisesBookingFromPlacementRequest returns ConflictError if a Space Booking has been made from the Placement Request`() {
      val arrivalDate = LocalDate.parse("2023-03-28")
      val departureDate = LocalDate.parse("2023-03-30")

      val premises = ApprovedPremisesEntityFactory()
        .withYieldedProbationRegion {
          ProbationRegionEntityFactory()
            .withYieldedApArea { ApAreaEntityFactory().produce() }
            .produce()
        }
        .withYieldedLocalAuthorityArea { LocalAuthorityEntityFactory().produce() }
        .produce()

      val room = RoomEntityFactory()
        .withPremises(premises)
        .produce()

      val bed = BedEntityFactory()
        .withRoom(room)
        .produce()

      val spaceBooking = Cas1SpaceBookingEntityFactory()
        .withPremises(premises)
        .produce()

      val placementRequest = PlacementRequestEntityFactory()
        .withApplication(application)
        .withPlacementRequirements(
          PlacementRequirementsEntityFactory()
            .withApplication(application)
            .withAssessment(assessment)
            .produce(),
        )
        .withAssessment(assessment)
        .withAllocatedToUser(user)
        .withSpaceBookings(
          mutableListOf(spaceBooking),
        )
        .produce()

      every { mockPlacementRequestRepository.findByIdOrNull(placementRequest.id) } returns placementRequest
      every { mockBedRepository.findByIdOrNull(bed.id) } returns bed

      every { mockBookingRepository.findByBedIdAndArrivingBeforeDate(bed.id, departureDate, null) } returns listOf()
      every { mockLostBedsRepository.findByBedIdAndOverlappingDate(bed.id, arrivalDate, departureDate, null) } returns listOf()

      val result = bookingService.createApprovedPremisesBookingFromPlacementRequest(
        user = user,
        placementRequestId = placementRequest.id,
        bedId = bed.id,
        premisesId = null,
        arrivalDate = arrivalDate,
        departureDate = departureDate,
      )

      assertThat(result is AuthorisableActionResult.Success).isTrue
      result as AuthorisableActionResult.Success
      assertThat(result.entity is ValidatableActionResult.ConflictError).isTrue
      val validationError = result.entity as ValidatableActionResult.ConflictError

      assertThat(validationError.message).isEqualTo("A Space Booking has already been made for this Placement Request")
    }

    @Test
    fun `returns ConflictError if a Booking has already been made from the Placement Request`() {
      val arrivalDate = LocalDate.parse("2023-03-28")
      val departureDate = LocalDate.parse("2023-03-30")

      val premises = TemporaryAccommodationPremisesEntityFactory()
        .withUnitTestControlTestProbationAreaAndLocalAuthority()
        .produce()

      val room = RoomEntityFactory()
        .withPremises(premises)
        .produce()

      val bed = BedEntityFactory()
        .withRoom(room)
        .produce()

      val placementRequest = PlacementRequestEntityFactory()
        .withApplication(application)
        .withPlacementRequirements(
          PlacementRequirementsEntityFactory()
            .withApplication(application)
            .withAssessment(assessment)
            .produce(),
        )
        .withAssessment(assessment)
        .withAllocatedToUser(user)
        .withBooking(
          BookingEntityFactory()
            .withBed(bed)
            .withPremises(premises)
            .produce(),
        )
        .produce()

      every { mockPlacementRequestRepository.findByIdOrNull(placementRequest.id) } returns placementRequest
      every { mockBedRepository.findByIdOrNull(bed.id) } returns bed

      every { mockBookingRepository.findByBedIdAndArrivingBeforeDate(bed.id, departureDate, null) } returns listOf()
      every { mockLostBedsRepository.findByBedIdAndOverlappingDate(bed.id, arrivalDate, departureDate, null) } returns listOf()

      val result = bookingService.createApprovedPremisesBookingFromPlacementRequest(
        user = user,
        placementRequestId = placementRequest.id,
        bedId = bed.id,
        premisesId = null,
        arrivalDate = arrivalDate,
        departureDate = departureDate,
      )

      assertThat(result is AuthorisableActionResult.Success).isTrue
      result as AuthorisableActionResult.Success
      assertThat(result.entity is ValidatableActionResult.ConflictError).isTrue
      val validationError = result.entity as ValidatableActionResult.ConflictError

      assertThat(validationError.message).isEqualTo("A Booking has already been made for this Placement Request")
    }

    @Test
    fun `returns GeneralError if the Placement Request has been withdrawn`() {
      val arrivalDate = LocalDate.parse("2023-03-28")
      val departureDate = LocalDate.parse("2023-03-30")

      val premises = TemporaryAccommodationPremisesEntityFactory()
        .withUnitTestControlTestProbationAreaAndLocalAuthority()
        .produce()

      val room = RoomEntityFactory()
        .withPremises(premises)
        .produce()

      val bed = BedEntityFactory()
        .withRoom(room)
        .produce()

      val placementRequest = PlacementRequestEntityFactory()
        .withApplication(application)
        .withIsWithdrawn(true)
        .withPlacementRequirements(
          PlacementRequirementsEntityFactory()
            .withApplication(application)
            .withAssessment(assessment)
            .produce(),
        )
        .withAssessment(assessment)
        .withAllocatedToUser(user)
        .withBooking(
          BookingEntityFactory()
            .withBed(bed)
            .withPremises(premises)
            .produce(),
        )
        .produce()

      every { mockPlacementRequestRepository.findByIdOrNull(placementRequest.id) } returns placementRequest
      every { mockBedRepository.findByIdOrNull(bed.id) } returns bed

      every { mockBookingRepository.findByBedIdAndArrivingBeforeDate(bed.id, departureDate, null) } returns listOf()
      every { mockLostBedsRepository.findByBedIdAndOverlappingDate(bed.id, arrivalDate, departureDate, null) } returns listOf()

      val result = bookingService.createApprovedPremisesBookingFromPlacementRequest(
        user = user,
        placementRequestId = placementRequest.id,
        bedId = bed.id,
        premisesId = null,
        arrivalDate = arrivalDate,
        departureDate = departureDate,
      )

      assertThat(result is AuthorisableActionResult.Success).isTrue
      result as AuthorisableActionResult.Success
      assertThat(result.entity is ValidatableActionResult.GeneralValidationError).isTrue
      val validationError = result.entity as ValidatableActionResult.GeneralValidationError

      assertThat(validationError.message).isEqualTo("placementRequestIsWithdrawn")
    }

    @Test
    fun `returns ConflictError if Bed has a conflicting Booking`() {
      val arrivalDate = LocalDate.parse("2023-03-28")
      val departureDate = LocalDate.parse("2023-03-30")

      val placementRequest = PlacementRequestEntityFactory()
        .withApplication(application)
        .withPlacementRequirements(
          PlacementRequirementsEntityFactory()
            .withApplication(application)
            .withAssessment(assessment)
            .produce(),
        )
        .withAssessment(assessment)
        .withAllocatedToUser(user)
        .produce()

      val premises = ApprovedPremisesEntityFactory()
        .withUnitTestControlTestProbationAreaAndLocalAuthority()
        .produce()

      val room = RoomEntityFactory()
        .withPremises(premises)
        .produce()

      val bed = BedEntityFactory()
        .withRoom(room)
        .produce()

      every { mockPlacementRequestRepository.findByIdOrNull(placementRequest.id) } returns placementRequest
      every { mockBedRepository.findByIdOrNull(bed.id) } returns bed

      every { mockBookingRepository.findByBedIdAndArrivingBeforeDate(bed.id, departureDate, null) } returns listOf(
        BookingEntityFactory()
          .withArrivalDate(arrivalDate)
          .withDepartureDate(departureDate)
          .withPremises(premises)
          .withBed(bed)
          .produce(),
      )
      every { mockLostBedsRepository.findByBedIdAndOverlappingDate(bed.id, arrivalDate, departureDate, null) } returns listOf()

      every { mockWorkingDayService.addWorkingDays(any(), any()) } answers { it.invocation.args[0] as LocalDate }

      val result = bookingService.createApprovedPremisesBookingFromPlacementRequest(
        user = user,
        placementRequestId = placementRequest.id,
        bedId = bed.id,
        premisesId = null,
        arrivalDate = arrivalDate,
        departureDate = departureDate,
      )

      assertThat(result is AuthorisableActionResult.Success).isTrue
      result as AuthorisableActionResult.Success
      assertThat(result.entity is ValidatableActionResult.ConflictError).isTrue
      val conflictError = result.entity as ValidatableActionResult.ConflictError

      assertThat(conflictError.message).isEqualTo("A Booking already exists for dates from $arrivalDate to $departureDate which overlaps with the desired dates")
    }

    @Test
    fun `returns ConflictError if Bed has a conflicting Lost Bed`() {
      val arrivalDate = LocalDate.parse("2023-03-28")
      val departureDate = LocalDate.parse("2023-03-30")

      val placementRequest = PlacementRequestEntityFactory()
        .withPlacementRequirements(
          PlacementRequirementsEntityFactory()
            .withApplication(application)
            .withAssessment(assessment)
            .produce(),
        )
        .withApplication(application)
        .withAssessment(assessment)
        .withAllocatedToUser(user)
        .produce()

      val premises = ApprovedPremisesEntityFactory()
        .withUnitTestControlTestProbationAreaAndLocalAuthority()
        .produce()

      val room = RoomEntityFactory()
        .withPremises(premises)
        .produce()

      val bed = BedEntityFactory()
        .withRoom(room)
        .produce()

      every { mockPlacementRequestRepository.findByIdOrNull(placementRequest.id) } returns placementRequest
      every { mockBedRepository.findByIdOrNull(bed.id) } returns bed

      every { mockBookingRepository.findByBedIdAndArrivingBeforeDate(bed.id, departureDate, null) } returns listOf()
      every { mockLostBedsRepository.findByBedIdAndOverlappingDate(bed.id, arrivalDate, departureDate, null) } returns listOf(
        LostBedsEntityFactory()
          .withStartDate(arrivalDate)
          .withEndDate(departureDate)
          .withPremises(premises)
          .withBed(bed)
          .withYieldedReason { LostBedReasonEntityFactory().produce() }
          .produce(),
      )

      val result = bookingService.createApprovedPremisesBookingFromPlacementRequest(
        user = user,
        placementRequestId = placementRequest.id,
        bedId = bed.id,
        premisesId = null,
        arrivalDate = arrivalDate,
        departureDate = departureDate,
      )

      assertThat(result is AuthorisableActionResult.Success).isTrue
      result as AuthorisableActionResult.Success
      assertThat(result.entity is ValidatableActionResult.ConflictError).isTrue
      val conflictError = result.entity as ValidatableActionResult.ConflictError

      assertThat(conflictError.message).isEqualTo("A Lost Bed already exists for dates from $arrivalDate to $departureDate which overlaps with the desired dates")
    }

    @Test
    fun `saves Booking, creates Domain Event and sends email`() {
      val arrivalDate = LocalDate.parse("2023-02-22")
      val departureDate = LocalDate.parse("2023-02-23")

      val placementRequest = PlacementRequestEntityFactory()
        .withPlacementRequirements(
          PlacementRequirementsEntityFactory()
            .withApplication(application)
            .withAssessment(assessment)
            .produce(),
        )
        .withApplication(application)
        .withAssessment(assessment)
        .withAllocatedToUser(user)
        .produce()

      every { mockPlacementRequestRepository.findByIdOrNull(placementRequest.id) } returns placementRequest

      val premises = ApprovedPremisesEntityFactory()
        .withUnitTestControlTestProbationAreaAndLocalAuthority()
        .produce()

      val room = RoomEntityFactory()
        .withPremises(premises)
        .produce()

      val bed = BedEntityFactory()
        .withRoom(room)
        .produce()

      every { mockBookingRepository.findByBedIdAndArrivingBeforeDate(bed.id, departureDate, null) } returns listOf()
      every { mockLostBedsRepository.findByBedIdAndOverlappingDate(bed.id, arrivalDate, departureDate, null) } returns listOf()

      every { mockBedRepository.findByIdOrNull(bed.id) } returns bed

      every { mockCas1ApplicationStatusService.bookingMade(any()) } returns Unit
      every { mockBookingRepository.save(any()) } answers { it.invocation.args[0] as BookingEntity }
      every { mockPlacementRequestRepository.save(any()) } answers { it.invocation.args[0] as PlacementRequestEntity }
      every { mockCas1BookingDomainEventService.bookingMade(any(), any(), any(), any()) } just Runs
      every { mockCas1BookingEmailService.bookingMade(any(), any(), any()) } just Runs

      val authorisableResult = bookingService.createApprovedPremisesBookingFromPlacementRequest(
        user = user,
        placementRequestId = placementRequest.id,
        bedId = bed.id,
        premisesId = null,
        arrivalDate = arrivalDate,
        departureDate = departureDate,
      )

      val booking = assertBookingCreated(authorisableResult, application, premises, arrivalDate, departureDate)

      verify(exactly = 1) {
        mockCas1BookingDomainEventService.bookingMade(
          application,
          booking,
          user,
          placementRequest,
        )
      }

      assertEmailsSent(application, booking)
    }

    @Test
    fun `saves Booking, creates Domain Event and sends email when a premisesId is provided`() {
      val arrivalDate = LocalDate.parse("2023-02-22")
      val departureDate = LocalDate.parse("2023-02-23")

      val placementRequest = PlacementRequestEntityFactory()
        .withPlacementRequirements(
          PlacementRequirementsEntityFactory()
            .withApplication(application)
            .withAssessment(assessment)
            .produce(),
        )
        .withApplication(application)
        .withAssessment(assessment)
        .withAllocatedToUser(user)
        .produce()

      every { mockPlacementRequestRepository.findByIdOrNull(placementRequest.id) } returns placementRequest

      val premises = ApprovedPremisesEntityFactory()
        .withUnitTestControlTestProbationAreaAndLocalAuthority()
        .produce()

      every { mockPremisesRepository.findByIdOrNull(premises.id) } returns premises

      every { mockCas1ApplicationStatusService.bookingMade(any()) } returns Unit
      every { mockBookingRepository.save(any()) } answers { it.invocation.args[0] as BookingEntity }
      every { mockPlacementRequestRepository.save(any()) } answers { it.invocation.args[0] as PlacementRequestEntity }
      every { mockCas1BookingDomainEventService.bookingMade(any(), any(), any(), any()) } just Runs
      every { mockCas1BookingEmailService.bookingMade(any(), any(), any()) } just Runs

      val authorisableResult = bookingService.createApprovedPremisesBookingFromPlacementRequest(
        user = user,
        placementRequestId = placementRequest.id,
        bedId = null,
        premisesId = premises.id,
        arrivalDate = arrivalDate,
        departureDate = departureDate,
      )

      val booking = assertBookingCreated(authorisableResult, application, premises, arrivalDate, departureDate)

      verify(exactly = 1) {
        mockCas1BookingDomainEventService.bookingMade(
          application,
          booking,
          user,
          placementRequest,
        )
      }

      assertEmailsSent(application, booking)
    }

    @Test
    fun `saves Booking, creates Domain Event and sends email when a cancelled booking exists`() {
      val arrivalDate = LocalDate.parse("2023-02-22")
      val departureDate = LocalDate.parse("2023-02-23")

      val existingPremises = TemporaryAccommodationPremisesEntityFactory()
        .withUnitTestControlTestProbationAreaAndLocalAuthority()
        .produce()

      val existingRoom = RoomEntityFactory()
        .withPremises(existingPremises)
        .produce()

      val existingBed = BedEntityFactory()
        .withRoom(existingRoom)
        .produce()

      val existingBooking = BookingEntityFactory()
        .withPremises(existingPremises)
        .withBed(existingBed)
        .produce()

      val cancellation = CancellationEntityFactory()
        .withYieldedReason { CancellationReasonEntityFactory().produce() }
        .withBooking(existingBooking)
        .produce()

      existingBooking.cancellations = mutableListOf(cancellation)

      val placementRequest = PlacementRequestEntityFactory()
        .withPlacementRequirements(
          PlacementRequirementsEntityFactory()
            .withApplication(application)
            .withAssessment(assessment)
            .produce(),
        )
        .withApplication(application)
        .withAssessment(assessment)
        .withAllocatedToUser(user)
        .withBooking(existingBooking)
        .produce()

      every { mockPlacementRequestRepository.findByIdOrNull(placementRequest.id) } returns placementRequest

      val premises = ApprovedPremisesEntityFactory()
        .withUnitTestControlTestProbationAreaAndLocalAuthority()
        .produce()

      every { mockPremisesRepository.findByIdOrNull(premises.id) } returns premises

      every { mockCas1ApplicationStatusService.bookingMade(any()) } returns Unit
      every { mockBookingRepository.save(any()) } answers { it.invocation.args[0] as BookingEntity }
      every { mockPlacementRequestRepository.save(any()) } answers { it.invocation.args[0] as PlacementRequestEntity }
      every { mockCas1BookingDomainEventService.bookingMade(any(), any(), any(), any()) } just Runs
      every { mockCas1BookingEmailService.bookingMade(any(), any(), any()) } just Runs

      val authorisableResult = bookingService.createApprovedPremisesBookingFromPlacementRequest(
        user = user,
        placementRequestId = placementRequest.id,
        bedId = null,
        premisesId = premises.id,
        arrivalDate = arrivalDate,
        departureDate = departureDate,
      )

      val booking = assertBookingCreated(authorisableResult, application, premises, arrivalDate, departureDate)

      verify(exactly = 1) {
        mockCas1BookingDomainEventService.bookingMade(
          application,
          booking,
          user,
          placementRequest,
        )
      }

      assertEmailsSent(application, booking)
    }

    @Test
    fun `saves successfully when the user is not assigned to the placement request and has create booking permission`() {
      val arrivalDate = LocalDate.parse("2023-02-22")
      val departureDate = LocalDate.parse("2023-02-23")

      val workflowManager = UserEntityFactory()
        .withUnitTestControlProbationRegion()
        .produce().apply {
          this.roles.add(
            UserRoleAssignmentEntityFactory()
              .withUser(this)
              .withRole(UserRole.CAS1_CRU_MEMBER)
              .produce(),
          )
        }

      val placementRequest = PlacementRequestEntityFactory()
        .withPlacementRequirements(
          PlacementRequirementsEntityFactory()
            .withApplication(application)
            .withAssessment(assessment)
            .produce(),
        )
        .withApplication(application)
        .withAssessment(assessment)
        .withAllocatedToUser(user)
        .produce()

      every { mockPlacementRequestRepository.findByIdOrNull(placementRequest.id) } returns placementRequest

      val premises = ApprovedPremisesEntityFactory()
        .withUnitTestControlTestProbationAreaAndLocalAuthority()
        .produce()

      every { mockPremisesRepository.findByIdOrNull(premises.id) } returns premises

      every { mockCas1ApplicationStatusService.bookingMade(any()) } returns Unit
      every { mockBookingRepository.save(any()) } answers { it.invocation.args[0] as BookingEntity }
      every { mockPlacementRequestRepository.save(any()) } answers { it.invocation.args[0] as PlacementRequestEntity }
      every { mockCas1BookingDomainEventService.bookingMade(any(), any(), any(), any()) } just Runs
      every { mockCas1BookingEmailService.bookingMade(any(), any(), any()) } just Runs

      val authorisableResult = bookingService.createApprovedPremisesBookingFromPlacementRequest(
        user = workflowManager,
        placementRequestId = placementRequest.id,
        bedId = null,
        premisesId = premises.id,
        arrivalDate = arrivalDate,
        departureDate = departureDate,
      )

      val booking = assertBookingCreated(authorisableResult, application, premises, arrivalDate, departureDate)

      verify(exactly = 1) {
        mockCas1BookingDomainEventService.bookingMade(
          application,
          booking,
          workflowManager,
          placementRequest,
        )
      }

      assertEmailsSent(application, booking)
    }

    private fun assertBookingCreated(
      authorisableResult: AuthorisableActionResult<ValidatableActionResult<BookingEntity>>,
      application: ApprovedPremisesApplicationEntity,
      premises: ApprovedPremisesEntity,
      arrivalDate: LocalDate,
      departureDate: LocalDate,
    ): BookingEntity {
      assertThat(authorisableResult is AuthorisableActionResult.Success).isTrue
      val validatableResult = (authorisableResult as AuthorisableActionResult.Success).entity
      assertThat(validatableResult is ValidatableActionResult.Success).isTrue

      val createdBooking = (validatableResult as ValidatableActionResult.Success).entity

      verify(exactly = 1) {
        mockBookingRepository.save(
          match {
            it.crn == application.crn &&
              it.premises == premises &&
              it.arrivalDate == arrivalDate &&
              it.departureDate == departureDate &&
              it.status == BookingStatus.confirmed &&
              it.adhoc == false
          },
        )
      }

      verify(exactly = 1) {
        mockPlacementRequestRepository.save(
          match {
            it.booking == createdBooking
          },
        )
      }

      return createdBooking
    }

    private fun assertEmailsSent(application: ApprovedPremisesApplicationEntity, booking: BookingEntity) {
      verify(exactly = 1) {
        mockCas1BookingEmailService.bookingMade(
          application = application,
          booking = booking,
          placementApplication = null,
        )
      }
    }
  }

  @Test
  fun `createTurnaround returns FieldValidationError if the number of working days is a negative integer`() {
    val premises = TemporaryAccommodationPremisesEntityFactory()
      .withProbationRegion(
        ProbationRegionEntityFactory()
          .withApArea(
            ApAreaEntityFactory()
              .produce(),
          )
          .produce(),
      )
      .withLocalAuthorityArea(
        LocalAuthorityEntityFactory()
          .produce(),
      )
      .produce()

    val room = RoomEntityFactory()
      .withPremises(premises)
      .produce()

    val bed = BedEntityFactory()
      .withRoom(room)
      .produce()

    val booking = BookingEntityFactory()
      .withPremises(premises)
      .withBed(bed)
      .produce()

    every { mockWorkingDayService.addWorkingDays(any(), any()) } answers { it.invocation.args[0] as LocalDate }
    every { mockBookingRepository.findByBedIdAndArrivingBeforeDate(any(), any(), any()) } returns listOf()
    every { mockLostBedsRepository.findByBedIdAndOverlappingDate(any(), any(), any(), any()) } returns listOf()
    every { mockTurnaroundRepository.save(any()) } answers { it.invocation.args[0] as TurnaroundEntity }

    val negativeDaysResult = bookingService.createTurnaround(booking, -1)

    assertThat(negativeDaysResult).isInstanceOf(ValidatableActionResult.FieldValidationError::class.java)
    assertThat((negativeDaysResult as ValidatableActionResult.FieldValidationError).validationMessages).contains(
      entry("$.workingDays", "isNotAPositiveInteger"),
    )
  }

  @Test
  fun `createTurnaround returns Success with the persisted entity if the number of working days is a positive integer`() {
    val premises = TemporaryAccommodationPremisesEntityFactory()
      .withProbationRegion(
        ProbationRegionEntityFactory()
          .withApArea(
            ApAreaEntityFactory()
              .produce(),
          )
          .produce(),
      )
      .withLocalAuthorityArea(
        LocalAuthorityEntityFactory()
          .produce(),
      )
      .produce()

    val room = RoomEntityFactory()
      .withPremises(premises)
      .produce()

    val bed = BedEntityFactory()
      .withRoom(room)
      .produce()

    val booking = BookingEntityFactory()
      .withPremises(premises)
      .withBed(bed)
      .produce()

    every { mockWorkingDayService.addWorkingDays(any(), any()) } answers { it.invocation.args[0] as LocalDate }
    every { mockBookingRepository.findByBedIdAndArrivingBeforeDate(any(), any(), any()) } returns listOf()
    every { mockLostBedsRepository.findByBedIdAndOverlappingDate(any(), any(), any(), any()) } returns listOf()
    every { mockTurnaroundRepository.save(any()) } answers { it.invocation.args[0] as TurnaroundEntity }

    val result = bookingService.createTurnaround(booking, 2)

    assertThat(result).isInstanceOf(ValidatableActionResult.Success::class.java)
    result as ValidatableActionResult.Success
    assertThat(result.entity.booking).isEqualTo(booking)
    assertThat(result.entity.workingDayCount).isEqualTo(2)
  }

  @Nested
  inner class GetWithdrawableState {
    val user = UserEntityFactory()
      .withUnitTestControlProbationRegion()
      .produce()

    val premises = ApprovedPremisesEntityFactory()
      .withDefaultProbationRegion()
      .withDefaultLocalAuthorityArea()
      .produce()

    @Test
    fun `getWithdrawableState not withdrawable if has arrivals`() {
      val booking = BookingEntityFactory()
        .withPremises(premises)
        .produce()

      booking.arrivals.add(
        ArrivalEntityFactory().withBooking(booking).produce(),
      )

      every { mockUserAccessService.userMayCancelBooking(user, booking) } returns true

      val result = bookingService.getWithdrawableState(booking, user)

      assertThat(result.withdrawable).isFalse()
    }

    @Test
    fun `getWithdrawableState not withdrawable if already cancelled`() {
      val booking = BookingEntityFactory()
        .withPremises(premises)
        .produce()

      booking.cancellations.add(
        CancellationEntityFactory().withBooking(booking).withDefaults().produce(),
      )

      every { mockUserAccessService.userMayCancelBooking(user, booking) } returns true
      every { mockDeliusService.referralHasArrival(booking) } returns false

      val result = bookingService.getWithdrawableState(booking, user)

      assertThat(result.withdrawn).isTrue()
      assertThat(result.withdrawable).isFalse()
    }

    @Test
    fun `getWithdrawableState withdrawable if has no arrivals in CAS1 and Delius and not already cancelled`() {
      val booking = BookingEntityFactory()
        .withPremises(premises)
        .produce()

      every { mockUserAccessService.userMayCancelBooking(user, booking) } returns true
      every { mockDeliusService.referralHasArrival(booking) } returns false

      val result = bookingService.getWithdrawableState(booking, user)

      assertThat(result.withdrawn).isFalse()
      assertThat(result.withdrawable).isTrue()
    }

    @ParameterizedTest
    @CsvSource("true", "false")
    fun `getWithdrawableState userMayDirectlyWithdraw delegates to user access service`(canWithdraw: Boolean) {
      val booking = BookingEntityFactory()
        .withPremises(premises)
        .produce()

      every { mockUserAccessService.userMayCancelBooking(user, booking) } returns canWithdraw
      every { mockDeliusService.referralHasArrival(booking) } returns false

      val result = bookingService.getWithdrawableState(booking, user)

      assertThat(result.userMayDirectlyWithdraw).isEqualTo(canWithdraw)
    }

    @Test
    fun `getWithdrawableState blockingReason is null if no arrivals in CAS1 or Delius`() {
      val booking = BookingEntityFactory()
        .withPremises(premises)
        .withArrivals(mutableListOf())
        .produce()

      every { mockUserAccessService.userMayCancelBooking(user, booking) } returns true
      every { mockDeliusService.referralHasArrival(booking) } returns false

      val result = bookingService.getWithdrawableState(booking, user)

      assertThat(result.blockingReason).isNull()
    }

    @Test
    fun `getWithdrawableState blockingReason is ArrivalRecordedInCas1 if has arrival recorded in CAS1`() {
      val booking = BookingEntityFactory()
        .withPremises(premises)
        .produce()

      booking.arrivals.add(
        ArrivalEntityFactory()
          .withBooking(booking)
          .produce(),
      )

      every { mockUserAccessService.userMayCancelBooking(user, booking) } returns true

      val result = bookingService.getWithdrawableState(booking, user)

      assertThat(result.blockingReason).isEqualTo(BlockingReason.ArrivalRecordedInCas1)
    }

    @Test
    fun `getWithdrawableState blockingReason is ArrivalRecordedInDelius if has no CAS1 arrivals and arrival recorded in Delius`() {
      val booking = BookingEntityFactory()
        .withPremises(premises)
        .produce()

      every { mockUserAccessService.userMayCancelBooking(user, booking) } returns true
      every { mockDeliusService.referralHasArrival(booking) } returns true

      val result = bookingService.getWithdrawableState(booking, user)

      assertThat(result.blockingReason).isEqualTo(BlockingReason.ArrivalRecordedInDelius)
    }
  }

  @Nested
  inner class CreateDateChange {
    private val user = UserEntityFactory()
      .withUnitTestControlProbationRegion()
      .produce()

    private val approvedPremises = ApprovedPremisesEntityFactory()
      .withUnitTestControlTestProbationAreaAndLocalAuthority()
      .produce()

    private val approvedPremisesRoom = RoomEntityFactory()
      .withPremises(approvedPremises)
      .produce()

    private val approvedPremisesBed = BedEntityFactory()
      .withRoom(approvedPremisesRoom)
      .produce()

    private val temporaryAccommodationPremises = TemporaryAccommodationPremisesEntityFactory()
      .withUnitTestControlTestProbationAreaAndLocalAuthority()
      .produce()

    private val temporaryAccommodationRoom = RoomEntityFactory()
      .withPremises(temporaryAccommodationPremises)
      .produce()

    private val temporaryAccommodationBed = BedEntityFactory()
      .withRoom(temporaryAccommodationRoom)
      .produce()

    @Test
    fun `for non-AP Bookings, returns conflict error for conflicting Booking`() {
      val booking = BookingEntityFactory()
        .withPremises(temporaryAccommodationPremises)
        .withBed(temporaryAccommodationBed)
        .withServiceName(ServiceName.temporaryAccommodation)
        .produce()

      val conflictingBooking = BookingEntityFactory()
        .withPremises(temporaryAccommodationPremises)
        .withBed(temporaryAccommodationBed)
        .withServiceName(ServiceName.temporaryAccommodation)
        .withArrivalDate(LocalDate.parse("2023-07-10"))
        .withDepartureDate(LocalDate.parse("2023-07-12"))
        .produce()

      every { mockWorkingDayService.addWorkingDays(any(), any()) } answers { it.invocation.args[0] as LocalDate }
      every { mockBookingRepository.findByBedIdAndArrivingBeforeDate(temporaryAccommodationBed.id, LocalDate.parse("2023-07-14"), booking.id) } returns listOf(conflictingBooking)

      val result = bookingService.createDateChange(
        booking = booking,
        user = user,
        newArrivalDate = LocalDate.parse("2023-07-12"),
        newDepartureDate = LocalDate.parse("2023-07-14"),
      )

      assertThat(result is ValidatableActionResult.ConflictError).isTrue
      result as ValidatableActionResult.ConflictError
      assertThat(result.message).contains("A Booking already exists")
    }

    @Test
    fun `for non-AP Bookings, returns conflict error for conflicting Lost Bed`() {
      val booking = BookingEntityFactory()
        .withPremises(temporaryAccommodationPremises)
        .withBed(temporaryAccommodationBed)
        .withServiceName(ServiceName.temporaryAccommodation)
        .produce()

      val conflictingLostBed = LostBedsEntityFactory()
        .withPremises(temporaryAccommodationPremises)
        .withBed(temporaryAccommodationBed)
        .withStartDate(LocalDate.parse("2023-07-10"))
        .withEndDate(LocalDate.parse("2023-07-12"))
        .withYieldedReason { LostBedReasonEntityFactory().produce() }
        .produce()

      every { mockWorkingDayService.addWorkingDays(any(), any()) } answers { it.invocation.args[0] as LocalDate }
      every { mockBookingRepository.findByBedIdAndArrivingBeforeDate(temporaryAccommodationBed.id, LocalDate.parse("2023-07-14"), booking.id) } returns emptyList()
      every { mockLostBedsRepository.findByBedIdAndOverlappingDate(temporaryAccommodationBed.id, LocalDate.parse("2023-07-12"), LocalDate.parse("2023-07-14"), null) } returns listOf(conflictingLostBed)

      val result = bookingService.createDateChange(
        booking = booking,
        user = user,
        newArrivalDate = LocalDate.parse("2023-07-12"),
        newDepartureDate = LocalDate.parse("2023-07-14"),
      )

      assertThat(result is ValidatableActionResult.ConflictError).isTrue
      result as ValidatableActionResult.ConflictError
      assertThat(result.message).contains("A Lost Bed already exists")
    }

    @Test
    fun `returns validation error if new arrival date is after the new departure date`() {
      val booking = BookingEntityFactory()
        .withPremises(temporaryAccommodationPremises)
        .withBed(temporaryAccommodationBed)
        .withServiceName(ServiceName.temporaryAccommodation)
        .produce()

      every { mockWorkingDayService.addWorkingDays(any(), any()) } answers { it.invocation.args[0] as LocalDate }
      every { mockBookingRepository.findByBedIdAndArrivingBeforeDate(temporaryAccommodationBed.id, LocalDate.parse("2023-07-14"), booking.id) } returns emptyList()
      every { mockLostBedsRepository.findByBedIdAndOverlappingDate(temporaryAccommodationBed.id, LocalDate.parse("2023-07-16"), LocalDate.parse("2023-07-14"), null) } returns emptyList()

      val result = bookingService.createDateChange(
        booking = booking,
        user = user,
        newArrivalDate = LocalDate.parse("2023-07-16"),
        newDepartureDate = LocalDate.parse("2023-07-14"),
      )

      assertThat(result is ValidatableActionResult.FieldValidationError).isTrue
      result as ValidatableActionResult.FieldValidationError
      assertThat(result.validationMessages).containsEntry("$.newDepartureDate", "beforeBookingArrivalDate")
    }

    @Test
    fun `returns validation error if booking already has an arrival and attempting to change arrival date`() {
      val booking = BookingEntityFactory()
        .withPremises(temporaryAccommodationPremises)
        .withBed(temporaryAccommodationBed)
        .withServiceName(ServiceName.approvedPremises)
        .withArrivalDate(LocalDate.parse("2023-07-14"))
        .withDepartureDate(LocalDate.parse("2023-07-16"))
        .produce()
        .apply {
          arrivals += ArrivalEntityFactory()
            .withBooking(this)
            .produce()
        }

      every { mockWorkingDayService.addWorkingDays(any(), any()) } answers { it.invocation.args[0] as LocalDate }

      val result = bookingService.createDateChange(
        booking = booking,
        user = user,
        newArrivalDate = LocalDate.parse("2023-07-15"),
        newDepartureDate = LocalDate.parse("2023-07-16"),
      )

      assertThat(result is ValidatableActionResult.FieldValidationError).isTrue
      result as ValidatableActionResult.FieldValidationError
      assertThat(result.validationMessages).containsEntry("$.newArrivalDate", "arrivalDateCannotBeChangedOnArrivedBooking")
    }

    @Test
    fun `returns error if booking is cancelled`() {
      val booking = BookingEntityFactory()
        .withPremises(temporaryAccommodationPremises)
        .withBed(temporaryAccommodationBed)
        .withServiceName(ServiceName.approvedPremises)
        .withArrivalDate(LocalDate.parse("2023-07-14"))
        .withDepartureDate(LocalDate.parse("2023-07-16"))
        .produce()
        .apply {
          cancellations = mutableListOf(
            CancellationEntityFactory()
              .withDefaults()
              .withBooking(this)
              .produce(),
          )
        }

      every { mockWorkingDayService.addWorkingDays(any(), any()) } answers { it.invocation.args[0] as LocalDate }

      val result = bookingService.createDateChange(
        booking = booking,
        user = user,
        newArrivalDate = LocalDate.parse("2023-07-15"),
        newDepartureDate = LocalDate.parse("2023-07-16"),
      )

      assertThat(result is ValidatableActionResult.GeneralValidationError).isTrue
      result as ValidatableActionResult.GeneralValidationError
      assertThat(result.message).isEqualTo("This Booking is cancelled and as such cannot be modified")
    }

    @Test
    fun `returns success when changing arrived booking by reducing departure date`() {
      val booking = BookingEntityFactory()
        .withPremises(approvedPremises)
        .withBed(approvedPremisesBed)
        .withServiceName(ServiceName.approvedPremises)
        .withArrivalDate(LocalDate.parse("2023-07-14"))
        .withDepartureDate(LocalDate.parse("2023-07-16"))
        .produce()
        .apply {
          arrivals += ArrivalEntityFactory()
            .withBooking(this)
            .produce()
        }

      every { mockWorkingDayService.addWorkingDays(any(), any()) } answers { it.invocation.args[0] as LocalDate }
      every { mockDateChangeRepository.save(any()) } answers { it.invocation.args[0] as DateChangeEntity }
      every { mockBookingRepository.save(any()) } answers { it.invocation.args[0] as BookingEntity }

      val result = bookingService.createDateChange(
        booking = booking,
        user = user,
        newArrivalDate = null,
        newDepartureDate = LocalDate.parse("2023-07-15"),
      )

      assertThat(result is ValidatableActionResult.Success).isTrue
      result as ValidatableActionResult.Success

      verify {
        mockDateChangeRepository.save(
          match {
            it.booking.id == booking.id &&
              it.changedByUser == user &&
              it.newArrivalDate == LocalDate.parse("2023-07-14") &&
              it.newDepartureDate == LocalDate.parse("2023-07-15") &&
              it.previousArrivalDate == LocalDate.parse("2023-07-14") &&
              it.previousDepartureDate == LocalDate.parse("2023-07-16")
          },
        )
      }

      verify {
        mockBookingRepository.save(
          match {
            it.id == booking.id &&
              it.arrivalDate == LocalDate.parse("2023-07-14") &&
              it.departureDate == LocalDate.parse("2023-07-15")
          },
        )
      }
    }

    @Test
    fun `returns success when changing non-arrived booking`() {
      val booking = BookingEntityFactory()
        .withPremises(approvedPremises)
        .withBed(approvedPremisesBed)
        .withServiceName(ServiceName.approvedPremises)
        .withArrivalDate(LocalDate.parse("2023-07-14"))
        .withDepartureDate(LocalDate.parse("2023-07-16"))
        .produce()

      every { mockWorkingDayService.addWorkingDays(any(), any()) } answers { it.invocation.args[0] as LocalDate }
      every { mockDateChangeRepository.save(any()) } answers { it.invocation.args[0] as DateChangeEntity }
      every { mockBookingRepository.save(any()) } answers { it.invocation.args[0] as BookingEntity }

      val result = bookingService.createDateChange(
        booking = booking,
        user = user,
        newArrivalDate = LocalDate.parse("2023-07-18"),
        newDepartureDate = LocalDate.parse("2023-07-22"),
      )

      assertThat(result is ValidatableActionResult.Success).isTrue
      result as ValidatableActionResult.Success

      verify {
        mockDateChangeRepository.save(
          match {
            it.booking.id == booking.id &&
              it.changedByUser == user &&
              it.newArrivalDate == LocalDate.parse("2023-07-18") &&
              it.newDepartureDate == LocalDate.parse("2023-07-22") &&
              it.previousArrivalDate == LocalDate.parse("2023-07-14") &&
              it.previousDepartureDate == LocalDate.parse("2023-07-16")
          },
        )
      }

      verify {
        mockBookingRepository.save(
          match {
            it.id == booking.id &&
              it.arrivalDate == LocalDate.parse("2023-07-18") &&
              it.departureDate == LocalDate.parse("2023-07-22")
          },
        )
      }
    }

    @Test
    fun `emits domain event when booking has associated application`() {
      val application = ApprovedPremisesApplicationEntityFactory()
        .withCreatedByUser(user)
        .withSubmittedAt(OffsetDateTime.now())
        .produce()

      val booking = BookingEntityFactory()
        .withPremises(approvedPremises)
        .withBed(approvedPremisesBed)
        .withServiceName(ServiceName.approvedPremises)
        .withArrivalDate(LocalDate.parse("2023-07-14"))
        .withDepartureDate(LocalDate.parse("2023-07-16"))
        .withApplication(application)
        .withCrn(application.crn)
        .produce()

      every { mockWorkingDayService.addWorkingDays(any(), any()) } answers { it.invocation.args[0] as LocalDate }
      every { mockDateChangeRepository.save(any()) } answers { it.invocation.args[0] as DateChangeEntity }
      every { mockBookingRepository.save(any()) } answers { it.invocation.args[0] as BookingEntity }

      every { mockCas1BookingDomainEventService.bookingChanged(any(), any(), any()) } just Runs

      val newArrivalDate = LocalDate.parse("2023-07-18")
      val newDepartureDate = LocalDate.parse("2023-07-22")

      val result = bookingService.createDateChange(
        booking = booking,
        user = user,
        newArrivalDate = newArrivalDate,
        newDepartureDate = newDepartureDate,
      )

      assertThat(result is ValidatableActionResult.Success).isTrue
      result as ValidatableActionResult.Success

      verify {
        mockDateChangeRepository.save(
          match {
            it.booking.id == booking.id &&
              it.changedByUser == user &&
              it.newArrivalDate == LocalDate.parse("2023-07-18") &&
              it.newDepartureDate == LocalDate.parse("2023-07-22") &&
              it.previousArrivalDate == LocalDate.parse("2023-07-14") &&
              it.previousDepartureDate == LocalDate.parse("2023-07-16")
          },
        )
      }

      verify {
        mockBookingRepository.save(
          match {
            it.id == booking.id &&
              it.arrivalDate == LocalDate.parse("2023-07-18") &&
              it.departureDate == LocalDate.parse("2023-07-22")
          },
        )
      }

      verify(exactly = 1) {
        mockCas1BookingDomainEventService.bookingChanged(
          booking = booking,
          changedBy = user,
          bookingChangedAt = any(),
        )
      }
    }

    @Test
    fun `emits domain event when booking has associated offline application with an event number`() {
      val application = OfflineApplicationEntityFactory()
        .withEventNumber("123")
        .produce()

      val booking = BookingEntityFactory()
        .withPremises(approvedPremises)
        .withBed(approvedPremisesBed)
        .withServiceName(ServiceName.approvedPremises)
        .withArrivalDate(LocalDate.parse("2023-07-14"))
        .withDepartureDate(LocalDate.parse("2023-07-16"))
        .withOfflineApplication(application)
        .withCrn(application.crn)
        .produce()

      every { mockWorkingDayService.addWorkingDays(any(), any()) } answers { it.invocation.args[0] as LocalDate }
      every { mockDateChangeRepository.save(any()) } answers { it.invocation.args[0] as DateChangeEntity }
      every { mockBookingRepository.save(any()) } answers { it.invocation.args[0] as BookingEntity }

      every { mockCas1BookingDomainEventService.bookingChanged(any(), any(), any()) } just Runs

      val newArrivalDate = LocalDate.parse("2023-07-18")
      val newDepartureDate = LocalDate.parse("2023-07-22")

      val result = bookingService.createDateChange(
        booking = booking,
        user = user,
        newArrivalDate = newArrivalDate,
        newDepartureDate = newDepartureDate,
      )

      assertThat(result is ValidatableActionResult.Success).isTrue
      result as ValidatableActionResult.Success

      verify(exactly = 1) {
        mockCas1BookingDomainEventService.bookingChanged(
          booking = booking,
          changedBy = user,
          bookingChangedAt = any(),
        )
      }
    }

    @Test
    fun `does not emit domain event when booking has associated offline application without an event number`() {
      val application = OfflineApplicationEntityFactory()
        .withEventNumber(null)
        .produce()

      val booking = BookingEntityFactory()
        .withPremises(approvedPremises)
        .withBed(approvedPremisesBed)
        .withServiceName(ServiceName.approvedPremises)
        .withArrivalDate(LocalDate.parse("2023-07-14"))
        .withDepartureDate(LocalDate.parse("2023-07-16"))
        .withOfflineApplication(application)
        .withCrn(application.crn)
        .produce()

      every { mockWorkingDayService.addWorkingDays(any(), any()) } answers { it.invocation.args[0] as LocalDate }
      every { mockDateChangeRepository.save(any()) } answers { it.invocation.args[0] as DateChangeEntity }
      every { mockBookingRepository.save(any()) } answers { it.invocation.args[0] as BookingEntity }

      every { mockCas1BookingDomainEventService.bookingChanged(any(), any(), any()) } just Runs

      val newArrivalDate = LocalDate.parse("2023-07-18")
      val newDepartureDate = LocalDate.parse("2023-07-22")

      val result = bookingService.createDateChange(
        booking = booking,
        user = user,
        newArrivalDate = newArrivalDate,
        newDepartureDate = newDepartureDate,
      )

      assertThat(result is ValidatableActionResult.Success).isTrue
      result as ValidatableActionResult.Success

      verify {
        mockDateChangeRepository.save(
          match {
            it.booking.id == booking.id &&
              it.changedByUser == user &&
              it.newArrivalDate == LocalDate.parse("2023-07-18") &&
              it.newDepartureDate == LocalDate.parse("2023-07-22") &&
              it.previousArrivalDate == LocalDate.parse("2023-07-14") &&
              it.previousDepartureDate == LocalDate.parse("2023-07-16")
          },
        )
      }

      verify {
        mockBookingRepository.save(
          match {
            it.id == booking.id &&
              it.arrivalDate == LocalDate.parse("2023-07-18") &&
              it.departureDate == LocalDate.parse("2023-07-22")
          },
        )
      }

      verify(exactly = 0) { mockCas1BookingDomainEventService wasNot Called }
    }
  }
}
