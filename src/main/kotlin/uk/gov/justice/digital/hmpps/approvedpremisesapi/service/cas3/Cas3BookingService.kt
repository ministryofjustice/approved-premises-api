package uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas3

import io.sentry.Sentry
import jakarta.transaction.Transactional
import org.slf4j.LoggerFactory
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.BookingStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ServiceName
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ArrivalEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ArrivalRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.AssessmentEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.AssessmentRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.BedRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.BookingEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.BookingRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.CancellationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.CancellationReasonRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.CancellationRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ConfirmationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ConfirmationRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.DepartureEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.DepartureReasonRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.DepartureRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ExtensionEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ExtensionRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.MoveOnCategoryRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.TemporaryAccommodationPremisesEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.TurnaroundEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.TurnaroundRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.cas3.Cas3VoidBedspacesRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.serviceScopeMatches
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.PersonSummaryInfoResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.forCrn
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.validated
import uk.gov.justice.digital.hmpps.approvedpremisesapi.problem.InternalServerErrorProblem
import uk.gov.justice.digital.hmpps.approvedpremisesapi.results.AuthorisableActionResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.results.CasResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.results.ValidatableActionResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.AssessmentService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.OffenderService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.UserAccessService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.WorkingDayService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas3LimitedAccessStrategy
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.extractEntityFromCasResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.toLocalDateTime
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.UUID

@Service
class Cas3BookingService(
  private val bookingRepository: BookingRepository,
  private val bedRepository: BedRepository,
  private val confirmationRepository: ConfirmationRepository,
  private val assessmentRepository: AssessmentRepository,
  private val arrivalRepository: ArrivalRepository,
  private val departureRepository: DepartureRepository,
  private val departureReasonRepository: DepartureReasonRepository,
  private val moveOnCategoryRepository: MoveOnCategoryRepository,
  private val cancellationRepository: CancellationRepository,
  private val cancellationReasonRepository: CancellationReasonRepository,
  private val cas3VoidBedspacesRepository: Cas3VoidBedspacesRepository,
  private val turnaroundRepository: TurnaroundRepository,
  private val extensionRepository: ExtensionRepository,
  private val cas3PremisesService: Cas3PremisesService,
  private val assessmentService: AssessmentService,
  private val userAccessService: UserAccessService,
  private val offenderService: OffenderService,
  private val workingDayService: WorkingDayService,
  private val cas3DomainEventService: DomainEventService,
) {
  private val log = LoggerFactory.getLogger(this::class.java)

  @Transactional
  fun createBooking(
    user: UserEntity,
    premises: TemporaryAccommodationPremisesEntity,
    crn: String,
    nomsNumber: String?,
    arrivalDate: LocalDate,
    departureDate: LocalDate,
    bedId: UUID?,
    assessmentId: UUID?,
    enableTurnarounds: Boolean,
  ): AuthorisableActionResult<ValidatableActionResult<BookingEntity>> {
    val validationResult = validated {
      if (bedId == null) {
        "$.bedId" hasValidationError "empty"
        return@validated fieldValidationError
      }

      val expectedLastUnavailableDate =
        workingDayService.addWorkingDays(departureDate, premises.turnaroundWorkingDayCount)
      getBookingWithConflictingDates(arrivalDate, expectedLastUnavailableDate, null, bedId)?.let {
        return@validated it.id hasConflictError "A Booking already exists for dates from ${it.arrivalDate} to ${it.lastUnavailableDate} which overlaps with the desired dates"
      }

      getVoidBedspaceWithConflictingDates(arrivalDate, expectedLastUnavailableDate, null, bedId)?.let {
        return@validated it.id hasConflictError "A Lost Bed already exists for dates from ${it.startDate} to ${it.endDate} which overlaps with the desired dates"
      }

      bedRepository.findArchivedBedByBedIdAndDate(bedId, departureDate)?.let {
        return@validated it.id hasConflictError "BedSpace is archived from ${it.endDate} which overlaps with the desired dates"
      }

      val bedspace = bedRepository.findByIdOrNull(bedId)

      if (bedspace == null) {
        "$.bedId" hasValidationError "doesNotExist"
      }

      if (departureDate.isBefore(arrivalDate)) {
        "$.departureDate" hasValidationError "beforeBookingArrivalDate"
      }

      val application = when (assessmentId) {
        null -> null
        else -> {
          val result = assessmentRepository.findByIdOrNull(assessmentId)
          if (result == null) {
            "$.assessmentId" hasValidationError "doesNotExist"
          }
          result?.application
        }
      }

      if (validationErrors.any()) {
        return@validated fieldValidationError
      }

      val bookingCreatedAt = OffsetDateTime.now()

      val booking = bookingRepository.save(
        BookingEntity(
          id = UUID.randomUUID(),
          crn = crn,
          nomsNumber = nomsNumber,
          arrivalDate = arrivalDate,
          departureDate = departureDate,
          keyWorkerStaffCode = null,
          arrivals = mutableListOf(),
          departures = mutableListOf(),
          nonArrival = null,
          cancellations = mutableListOf(),
          confirmation = null,
          extensions = mutableListOf(),
          dateChanges = mutableListOf(),
          premises = premises,
          bed = bedspace,
          service = ServiceName.temporaryAccommodation.value,
          originalArrivalDate = arrivalDate,
          originalDepartureDate = departureDate,
          createdAt = bookingCreatedAt,
          application = application,
          offlineApplication = null,
          turnarounds = mutableListOf(),
          placementRequest = null,
          status = BookingStatus.provisional,
        ),
      )

      val turnaround = turnaroundRepository.save(
        TurnaroundEntity(
          id = UUID.randomUUID(),
          workingDayCount = when (enableTurnarounds) {
            true -> premises.turnaroundWorkingDayCount
            else -> 0
          },
          createdAt = bookingCreatedAt,
          booking = booking,
        ),
      )

      booking.turnarounds += turnaround

      cas3DomainEventService.saveBookingProvisionallyMadeEvent(booking, user)

      success(booking)
    }

    return AuthorisableActionResult.Success(validationResult)
  }

  fun updateBooking(bookingEntity: BookingEntity): BookingEntity = bookingRepository.save(bookingEntity)

  @Transactional
  fun createConfirmation(
    booking: BookingEntity,
    dateTime: OffsetDateTime,
    notes: String?,
    user: UserEntity,
  ) = validated<ConfirmationEntity> {
    if (booking.confirmation != null) {
      return generalError("This Booking already has a Confirmation set")
    }

    val confirmationEntity = confirmationRepository.save(
      ConfirmationEntity(
        id = UUID.randomUUID(),
        dateTime = dateTime,
        notes = notes,
        booking = booking,
        createdAt = OffsetDateTime.now(),
      ),
    )
    booking.status = BookingStatus.confirmed
    updateBooking(booking)
    booking.confirmation = confirmationEntity

    cas3DomainEventService.saveBookingConfirmedEvent(booking, user)
    findAndCloseAssessment(booking, user)

    return success(confirmationEntity)
  }

  @Transactional
  fun createTurnaround(
    booking: BookingEntity,
    workingDays: Int,
  ) = validated {
    if (workingDays < 0) {
      "$.workingDays" hasValidationError "isNotAPositiveInteger"
    }

    val expectedLastUnavailableDate = workingDayService.addWorkingDays(booking.departureDate, workingDays)
    getBookingWithConflictingDates(
      booking.arrivalDate,
      expectedLastUnavailableDate,
      booking.id,
      booking.bed!!.id,
    )?.let {
      return@validated it.id hasConflictError "A Booking already exists for dates from ${it.arrivalDate} to ${it.lastUnavailableDate} which overlaps with the desired dates"
    }

    getVoidBedspaceWithConflictingDates(booking.arrivalDate, expectedLastUnavailableDate, null, booking.bed!!.id)?.let {
      return@validated it.id hasConflictError "A Lost Bed already exists for dates from ${it.startDate} to ${it.endDate} which overlaps with the desired dates"
    }

    if (validationErrors.any()) {
      return fieldValidationError
    }

    val turnaround = turnaroundRepository.save(
      TurnaroundEntity(
        id = UUID.randomUUID(),
        workingDayCount = workingDays,
        createdAt = OffsetDateTime.now(),
        booking = booking,
      ),
    )

    return success(turnaround)
  }

  @Transactional
  fun createArrival(
    user: UserEntity,
    booking: BookingEntity,
    arrivalDate: LocalDate,
    expectedDepartureDate: LocalDate,
    notes: String?,
  ) = validated<ArrivalEntity> {
    if (expectedDepartureDate.isBefore(arrivalDate)) {
      return "$.expectedDepartureDate" hasSingleValidationError "beforeBookingArrivalDate"
    }
    val isFirstArrival = booking.arrivals.isNullOrEmpty()
    val arrivalEntity = arrivalRepository.save(
      ArrivalEntity(
        id = UUID.randomUUID(),
        arrivalDate = arrivalDate,
        arrivalDateTime = arrivalDate.toLocalDateTime().toInstant(),
        expectedDepartureDate = expectedDepartureDate,
        notes = notes,
        booking = booking,
        createdAt = OffsetDateTime.now(),
      ),
    )

    booking.arrivalDate = arrivalDate
    booking.departureDate = expectedDepartureDate
    booking.status = BookingStatus.arrived
    updateBooking(booking)

    booking.arrivals += arrivalEntity

    when (isFirstArrival) {
      true -> cas3DomainEventService.savePersonArrivedEvent(booking, user)
      else -> cas3DomainEventService.savePersonArrivedUpdatedEvent(booking, user)
    }

    return success(arrivalEntity)
  }

  fun createDeparture(
    booking: BookingEntity,
    dateTime: OffsetDateTime,
    reasonId: UUID,
    moveOnCategoryId: UUID,
    notes: String?,
    user: UserEntity,
  ) = validated<DepartureEntity> {
    if (booking.arrivalDate.toLocalDateTime().isAfter(dateTime)) {
      "$.dateTime" hasValidationError "beforeBookingArrivalDate"
    }

    val reason = departureReasonRepository.findByIdOrNull(reasonId)
    if (reason == null) {
      "$.reasonId" hasValidationError "doesNotExist"
    } else if (!reason.serviceScopeMatches(booking.service)) {
      "$.reasonId" hasValidationError "incorrectDepartureReasonServiceScope"
    }

    val moveOnCategory = moveOnCategoryRepository.findByIdOrNull(moveOnCategoryId)
    if (moveOnCategory == null) {
      "$.moveOnCategoryId" hasValidationError "doesNotExist"
    } else if (!moveOnCategory.serviceScopeMatches(booking.service)) {
      "$.moveOnCategoryId" hasValidationError "incorrectMoveOnCategoryServiceScope"
    }

    if (validationErrors.any()) {
      return fieldValidationError
    }

    val isFirstDeparture = booking.departures.isNullOrEmpty()

    val departureEntity = departureRepository.save(
      DepartureEntity(
        id = UUID.randomUUID(),
        dateTime = dateTime,
        reason = reason!!,
        moveOnCategory = moveOnCategory!!,
        destinationProvider = null,
        notes = notes,
        booking = booking,
        createdAt = OffsetDateTime.now(),
      ),
    )
    booking.status = BookingStatus.departed
    booking.departureDate = dateTime.toLocalDate()
    updateBooking(booking)
    booking.departures += departureEntity

    when (isFirstDeparture) {
      true -> cas3DomainEventService.savePersonDepartedEvent(booking, user)
      else -> cas3DomainEventService.savePersonDepartureUpdatedEvent(booking, user)
    }

    return success(departureEntity)
  }

  @Transactional
  fun createCancellation(
    booking: BookingEntity,
    cancelledAt: LocalDate,
    reasonId: UUID,
    notes: String?,
    user: UserEntity,
  ) = validated<CancellationEntity> {
    val reason = cancellationReasonRepository.findByIdOrNull(reasonId)
    if (reason == null) {
      "$.reason" hasValidationError "doesNotExist"
    } else if (!reason.serviceScopeMatches(booking.service)) {
      "$.reason" hasValidationError "incorrectCancellationReasonServiceScope"
    }

    if (validationErrors.any()) {
      return fieldValidationError
    }
    val isFirstCancellations = booking.cancellations.isNullOrEmpty()
    val cancellationEntity = cancellationRepository.save(
      CancellationEntity(
        id = UUID.randomUUID(),
        date = cancelledAt,
        reason = reason!!,
        notes = notes,
        booking = booking,
        createdAt = OffsetDateTime.now(),
        otherReason = null,
      ),
    )
    booking.status = BookingStatus.cancelled
    updateBooking(booking)
    booking.cancellations += cancellationEntity

    when (isFirstCancellations) {
      true -> cas3DomainEventService.saveBookingCancelledEvent(booking, user)
      else -> cas3DomainEventService.saveBookingCancelledUpdatedEvent(booking, user)
    }

    booking.application?.getLatestAssessment()?.let { applicationEntity ->
      moveAssessmentToReadyToPlace(user, applicationEntity, booking.id)
    }

    return success(cancellationEntity)
  }

  @Transactional
  fun createExtension(
    booking: BookingEntity,
    newDepartureDate: LocalDate,
    notes: String?,
  ) = validated {
    val expectedLastUnavailableDate =
      workingDayService.addWorkingDays(newDepartureDate, booking.turnaround?.workingDayCount ?: 0)

    val bedId = booking.bed?.id
      ?: throw InternalServerErrorProblem("No bed ID present on Booking: ${booking.id}")

    getBookingWithConflictingDates(booking.arrivalDate, expectedLastUnavailableDate, booking.id, bedId)?.let {
      return@validated it.id hasConflictError "A Booking already exists for dates from ${it.arrivalDate} to ${it.lastUnavailableDate} which overlaps with the desired dates"
    }

    getVoidBedspaceWithConflictingDates(booking.arrivalDate, expectedLastUnavailableDate, null, bedId)?.let {
      return@validated it.id hasConflictError "A Lost Bed already exists for dates from ${it.startDate} to ${it.endDate} which overlaps with the desired dates"
    }

    if (booking.arrivalDate.isAfter(newDepartureDate)) {
      return "$.newDepartureDate" hasSingleValidationError "beforeBookingArrivalDate"
    }

    val extensionEntity = ExtensionEntity(
      id = UUID.randomUUID(),
      previousDepartureDate = booking.departureDate,
      newDepartureDate = newDepartureDate,
      notes = notes,
      booking = booking,
      createdAt = OffsetDateTime.now(),
    )

    val extension = extensionRepository.save(extensionEntity)
    booking.departureDate = extensionEntity.newDepartureDate
    booking.extensions.add(extension)
    updateBooking(booking)

    return success(extensionEntity)
  }

  fun findFutureBookingsForPremises(
    premisesId: UUID,
    statuses: List<BookingStatus>,
    user: UserEntity,
  ): CasResult<List<BookingAndPersons>> {
    val premises = cas3PremisesService.getPremises(premisesId)
      ?: return CasResult.NotFound("Premises", premisesId.toString())

    if (!userAccessService.userCanManagePremisesBookings(user, premises)) {
      return CasResult.Unauthorised()
    }

    val futureBookings = bookingRepository.findFutureBookingsByPremisesIdAndStatus(
      ServiceName.temporaryAccommodation.value,
      premisesId,
      LocalDate.now(),
      statuses,
    )

    val offenderSummaries = offenderService.getPersonSummaryInfoResults(
      crns = futureBookings.map { it.crn }.toSet(),
      limitedAccessStrategy = user.cas3LimitedAccessStrategy(),
    )

    return CasResult.Success(
      futureBookings.map { booking ->
        BookingAndPersons(
          booking,
          offenderSummaries.forCrn(booking.crn),
        )
      },
    )
  }

  private fun findAndCloseAssessment(booking: BookingEntity, user: UserEntity) {
    booking.application?.let {
      val assessment =
        assessmentRepository.findByApplicationIdAndReallocatedAtNull(booking.application!!.id)
      if (assessment != null) {
        closeAssessment(assessment.id, user, booking)
      }
    }
  }

  @SuppressWarnings("TooGenericExceptionCaught")
  private fun closeAssessment(
    assessmentId: UUID,
    user: UserEntity,
    booking: BookingEntity,
  ) {
    try {
      extractEntityFromCasResult(assessmentService.closeAssessment(user, assessmentId))
    } catch (exception: Exception) {
      log.error("Unable to close CAS3 assessment $assessmentId for booking ${booking.id} ", exception)
      Sentry.captureException(RuntimeException("Unable to close CAS3 assessment $assessmentId for booking ${booking.id} ", exception))
    }
  }

  @SuppressWarnings("TooGenericExceptionCaught")
  private fun moveAssessmentToReadyToPlace(
    user: UserEntity,
    applicationEntity: AssessmentEntity,
    bookingId: UUID,
  ) {
    try {
      extractEntityFromCasResult(
        assessmentService.acceptAssessment(
          user,
          applicationEntity.id,
          applicationEntity.document,
          null,
          null,
          null,
          "Automatically moved to ready-to-place after booking is cancelled",
        ),
      )
    } catch (exception: Exception) {
      log.error("Unable to move CAS3 assessment ${applicationEntity.id} to ready-to-place queue for $bookingId ")
      Sentry.captureException(
        RuntimeException(
          "Unable to move CAS3 assessment ${applicationEntity.id} for ready-to-place queue for $bookingId ",
          exception,
        ),
      )
    }
  }

  private fun getBookingWithConflictingDates(
    arrivalDate: LocalDate,
    closedDate: LocalDate,
    thisEntityId: UUID?,
    bedId: UUID,
  ): BookingEntity? {
    val candidateBookings = bookingRepository.findByBedIdAndArrivingBeforeDate(bedId, closedDate, thisEntityId)

    return candidateBookings.firstOrNull { it.lastUnavailableDate >= arrivalDate }
  }

  private fun getVoidBedspaceWithConflictingDates(
    startDate: LocalDate,
    endDate: LocalDate,
    thisEntityId: UUID?,
    bedId: UUID,
  ) = cas3VoidBedspacesRepository.findByBedspaceIdAndOverlappingDate(
    bedId,
    startDate,
    endDate,
    thisEntityId,
  ).firstOrNull()

  val BookingEntity.lastUnavailableDate: LocalDate
    get() = workingDayService.addWorkingDays(this.departureDate, this.turnaround?.workingDayCount ?: 0)

  data class BookingAndPersons(
    val booking: BookingEntity,
    val personInfo: PersonSummaryInfoResult,
  )
}

sealed interface GetBookingForPremisesResult {
  data class Success(val booking: BookingEntity) : GetBookingForPremisesResult
  object PremisesNotFound : GetBookingForPremisesResult
  object BookingNotFound : GetBookingForPremisesResult
}
