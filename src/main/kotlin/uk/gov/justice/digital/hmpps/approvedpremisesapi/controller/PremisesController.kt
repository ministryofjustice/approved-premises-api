package uk.gov.justice.digital.hmpps.approvedpremisesapi.controller

import arrow.core.Ior
import jakarta.transaction.Transactional
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.PremisesApiDelegate
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Arrival
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.BedDetail
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.BedSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Booking
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cancellation
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Confirmation
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.DateChange
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Departure
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ExtendedPremisesSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Extension
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.LostBed
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.LostBedCancellation
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.NewArrival
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.NewBooking
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.NewCancellation
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.NewCas2Arrival
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.NewCas3Arrival
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.NewConfirmation
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.NewDateChange
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.NewDeparture
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.NewExtension
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.NewLostBed
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.NewLostBedCancellation
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.NewPremises
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.NewRoom
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.NewTurnaround
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Premises
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.PremisesSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Room
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ServiceName
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.StaffMember
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Turnaround
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.UpdateLostBed
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.UpdatePremises
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.UpdateRoom
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApprovedPremisesEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.TemporaryAccommodationPremisesEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserQualification
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.PersonInfoResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.PersonSummaryInfoResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.problem.BadRequestProblem
import uk.gov.justice.digital.hmpps.approvedpremisesapi.problem.ConflictProblem
import uk.gov.justice.digital.hmpps.approvedpremisesapi.problem.ForbiddenProblem
import uk.gov.justice.digital.hmpps.approvedpremisesapi.problem.InternalServerErrorProblem
import uk.gov.justice.digital.hmpps.approvedpremisesapi.problem.NotFoundProblem
import uk.gov.justice.digital.hmpps.approvedpremisesapi.problem.NotImplementedProblem
import uk.gov.justice.digital.hmpps.approvedpremisesapi.results.AuthorisableActionResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.results.ValidatableActionResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.BedService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.BookingService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.GetBookingForPremisesResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.LostBedService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.OffenderService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.PremisesService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.RoomService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.StaffMemberService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.UserAccessService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.UserService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.Cas1WithdrawableService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.ArrivalTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.BedDetailTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.BedSummaryTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.BookingTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.CancellationTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.ConfirmationTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.DateChangeTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.DepartureTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.ExtensionTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.LostBedCancellationTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.LostBedsTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.PremisesSummaryTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.PremisesTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.RoomTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.StaffMemberTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.TurnaroundTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.extractEntityFromAuthorisableActionResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.extractEntityFromCasResult
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.util.UUID

@Service
class PremisesController(
  private val usersService: UserService,
  private val userAccessService: UserAccessService,
  private val premisesService: PremisesService,
  private val offenderService: OffenderService,
  private val bookingService: BookingService,
  private val lostBedsService: LostBedService,
  private val bedService: BedService,
  private val premisesTransformer: PremisesTransformer,
  private val premisesSummaryTransformer: PremisesSummaryTransformer,
  private val bookingTransformer: BookingTransformer,
  private val lostBedsTransformer: LostBedsTransformer,
  private val arrivalTransformer: ArrivalTransformer,
  private val cancellationTransformer: CancellationTransformer,
  private val confirmationTransformer: ConfirmationTransformer,
  private val departureTransformer: DepartureTransformer,
  private val extensionTransformer: ExtensionTransformer,
  private val staffMemberTransformer: StaffMemberTransformer,
  private val staffMemberService: StaffMemberService,
  private val roomService: RoomService,
  private val roomTransformer: RoomTransformer,
  private val lostBedCancellationTransformer: LostBedCancellationTransformer,
  private val turnaroundTransformer: TurnaroundTransformer,
  private val bedSummaryTransformer: BedSummaryTransformer,
  private val bedDetailTransformer: BedDetailTransformer,
  private val dateChangeTransformer: DateChangeTransformer,
  private val cas1WithdrawableService: Cas1WithdrawableService,
) : PremisesApiDelegate {

  override fun premisesSummaryGet(
    xServiceName: ServiceName,
    probationRegionId: UUID?,
    apAreaId: UUID?,
  ): ResponseEntity<List<PremisesSummary>> {
    val transformedSummaries = when (xServiceName) {
      ServiceName.approvedPremises -> {
        val summaries = premisesService.getAllApprovedPremisesSummaries(probationRegionId, apAreaId)

        summaries.map(premisesSummaryTransformer::transformDomainToApi)
      }

      ServiceName.cas2 -> throw RuntimeException("CAS2 not supported")
      ServiceName.temporaryAccommodation -> {
        val user = usersService.getUserForRequest()
        val summaries = premisesService.getAllTemporaryAccommodationPremisesSummaries(user.probationRegion.id)

        summaries.map(premisesSummaryTransformer::transformDomainToApi)
      }
    }

    return ResponseEntity.ok(transformedSummaries)
  }

  @Transactional
  override fun premisesPremisesIdPut(premisesId: UUID, body: UpdatePremises): ResponseEntity<Premises> {
    val premises = premisesService.getPremises(premisesId)
      ?: throw NotFoundProblem(premisesId, "Premises")

    if (!userAccessService.currentUserCanManagePremises(premises) || !userAccessService.currentUserCanAccessRegion(body.probationRegionId)) {
      throw ForbiddenProblem()
    }

    val updatePremisesResult = premisesService
      .updatePremises(
        premisesId,
        body.addressLine1,
        body.addressLine2,
        body.town,
        body.postcode,
        body.localAuthorityAreaId,
        body.probationRegionId,
        body.characteristicIds,
        body.notes,
        body.status,
        Ior.fromNullables(body.pdu, body.probationDeliveryUnitId)?.toEither(),
        body.turnaroundWorkingDayCount,
      )

    var validationResult = when (updatePremisesResult) {
      is AuthorisableActionResult.NotFound -> throw NotFoundProblem(premisesId, "Premises")
      is AuthorisableActionResult.Unauthorised -> throw ForbiddenProblem()
      is AuthorisableActionResult.Success -> updatePremisesResult.entity
    }

    val bodyName = body.name
    if (bodyName != null && premises is TemporaryAccommodationPremisesEntity) {
      validationResult = when (val renamePremisesResult = premisesService.renamePremises(premisesId, bodyName)) {
        is AuthorisableActionResult.NotFound -> throw NotFoundProblem(premisesId, "Premises")
        is AuthorisableActionResult.Success -> renamePremisesResult.entity
        is AuthorisableActionResult.Unauthorised -> throw ForbiddenProblem()
      }
    }

    val updatedPremises = when (validationResult) {
      is ValidatableActionResult.GeneralValidationError -> throw BadRequestProblem(errorDetail = validationResult.message)
      is ValidatableActionResult.FieldValidationError -> throw BadRequestProblem(invalidParams = validationResult.validationMessages)
      is ValidatableActionResult.ConflictError -> throw ConflictProblem(
        id = validationResult.conflictingEntityId,
        conflictReason = validationResult.message,
      )

      is ValidatableActionResult.Success -> validationResult.entity
    }

    val totalBeds = premisesService.getBedCount(premises)
    return ResponseEntity.ok(
      premisesTransformer.transformJpaToApi(
        updatedPremises,
        totalBeds = totalBeds,
        availableBedsForToday = totalBeds,
      ),
    )
  }

  override fun premisesGet(xServiceName: ServiceName?, xUserRegion: UUID?): ResponseEntity<List<Premises>> {
    if (!userAccessService.currentUserCanAccessRegion(xUserRegion)) {
      throw ForbiddenProblem()
    }

    val premisesWithRoomCounts = when {
      xServiceName == null && xUserRegion == null -> premisesService.getAllPremises()
      xServiceName != null && xUserRegion != null -> premisesService.getAllPremisesInRegionForService(
        xUserRegion,
        xServiceName,
      )

      xServiceName != null -> premisesService.getAllPremisesForService(xServiceName)
      else -> premisesService.getAllPremisesInRegion(xUserRegion!!)
    }

    return ResponseEntity.ok(
      premisesWithRoomCounts.map {
        val premises = it.getPremises()
        val totalBeds = it.getBedCount()
        val availableBedsForToday =
          premisesService.getAvailabilityForRange(premises, LocalDate.now(), LocalDate.now().plusDays(1))
            .values.first().getFreeCapacity(totalBeds)

        premisesTransformer.transformJpaToApi(premises, totalBeds, availableBedsForToday)
      },
    )
  }

  override fun premisesPost(body: NewPremises, xServiceName: ServiceName?): ResponseEntity<Premises> {
    if (!userAccessService.currentUserCanAccessRegion(body.probationRegionId)) {
      throw ForbiddenProblem()
    }

    val serviceName = when (xServiceName == null) {
      true -> ServiceName.approvedPremises.value
      false -> xServiceName.value
    }

    val premises = extractResultEntityOrThrow(
      premisesService.createNewPremises(
        addressLine1 = body.addressLine1,
        addressLine2 = body.addressLine2,
        town = body.town,
        postcode = body.postcode,
        latitude = null,
        longitude = null,
        service = serviceName,
        localAuthorityAreaId = body.localAuthorityAreaId,
        probationRegionId = body.probationRegionId,
        name = body.name,
        notes = body.notes,
        characteristicIds = body.characteristicIds,
        status = body.status,
        probationDeliveryUnitIdentifier = Ior.fromNullables(body.pdu, body.probationDeliveryUnitId)?.toEither(),
        turnaroundWorkingDayCount = body.turnaroundWorkingDayCount,
      ),
    )

    val totalBeds = premisesService.getBedCount(premises)
    return ResponseEntity(
      premisesTransformer.transformJpaToApi(
        premises,
        totalBeds = totalBeds,
        availableBedsForToday = totalBeds,
      ),
      HttpStatus.CREATED,
    )
  }

  override fun premisesPremisesIdGet(premisesId: UUID): ResponseEntity<Premises> {
    val premises = premisesService.getPremises(premisesId)
      ?: throw NotFoundProblem(premisesId, "Premises")

    if (!userAccessService.currentUserCanViewPremises(premises)) {
      throw ForbiddenProblem()
    }

    val totalBeds = premisesService.getBedCount(premises)
    val availableBedsForToday =
      premisesService.getAvailabilityForRange(premises, LocalDate.now(), LocalDate.now().plusDays(1))
        .values.first().getFreeCapacity(totalBeds)

    return ResponseEntity.ok(premisesTransformer.transformJpaToApi(premises, totalBeds, availableBedsForToday))
  }

  override fun premisesPremisesIdBookingsGet(premisesId: UUID): ResponseEntity<List<Booking>> = runBlocking {
    val premises = premisesService.getPremises(premisesId)
      ?: throw NotFoundProblem(premisesId, "Premises")

    val user = usersService.getUserForRequest()

    if (!userAccessService.userCanManagePremisesBookings(user, premises)) {
      throw ForbiddenProblem()
    }

    val crns = premises.bookings.map { it.crn }
    val personInfoResults = async {
      offenderService.getPersonInfoResults(
        crns.toSet(),
        user.deliusUsername,
        user.hasQualification(UserQualification.LAO),
      )
    }.await()

    return@runBlocking ResponseEntity.ok(
      premises.bookings.map {
        val crn = it.crn
        val personInfo = personInfoResults.firstOrNull { it.crn == crn } ?: PersonInfoResult.Unknown(crn)
        bookingTransformer.transformJpaToApi(it, personInfo)
      },
    )
  }

  override fun premisesPremisesIdBookingsBookingIdGet(premisesId: UUID, bookingId: UUID): ResponseEntity<Booking> {
    val bookingResult = bookingService.getBooking(bookingId)

    val bookingAndPersons = when (bookingResult) {
      is AuthorisableActionResult.Unauthorised -> throw ForbiddenProblem()
      is AuthorisableActionResult.NotFound -> throw NotFoundProblem(bookingResult.id!!, bookingResult.entityType!!)
      is AuthorisableActionResult.Success -> bookingResult.entity
    }

    val apiBooking = bookingTransformer.transformJpaToApi(
      bookingAndPersons.booking,
      bookingAndPersons.personInfo,
    )

    if (apiBooking.premises.id != premisesId) {
      throw NotFoundProblem(premisesId, "Premises")
    }

    return ResponseEntity.ok(apiBooking)
  }

  @Transactional
  override fun premisesPremisesIdBookingsPost(premisesId: UUID, body: NewBooking): ResponseEntity<Booking> {
    val user = usersService.getUserForRequest()

    val premises = premisesService.getPremises(premisesId)
      ?: throw NotFoundProblem(premisesId, "Premises")

    if (!userAccessService.userCanManagePremisesBookings(user, premises)) {
      throw ForbiddenProblem()
    }

    val personInfo =
      offenderService.getPersonInfoResult(body.crn, user.deliusUsername, user.hasQualification(UserQualification.LAO))

    if (personInfo !is PersonInfoResult.Success) throw InternalServerErrorProblem("Unable to get Person Info for CRN: ${body.crn}")

    val authorisableResult = when (premises) {
      is TemporaryAccommodationPremisesEntity -> {
        bookingService.createTemporaryAccommodationBooking(
          user = user,
          premises = premises,
          crn = body.crn,
          nomsNumber = when (personInfo) {
            is PersonInfoResult.Success.Restricted -> personInfo.nomsNumber
            is PersonInfoResult.Success.Full -> personInfo.inmateDetail?.offenderNo
          },
          arrivalDate = body.arrivalDate,
          departureDate = body.departureDate,
          bedId = body.bedId,
          assessmentId = body.assessmentId,
          enableTurnarounds = body.enableTurnarounds ?: false,
        )
      }

      else -> error("This endpoint does not support creating bookings for premise type: ${premises::class.qualifiedName}")
    }

    val validatableResult = when (authorisableResult) {
      is AuthorisableActionResult.Unauthorised -> throw ForbiddenProblem()
      is AuthorisableActionResult.NotFound -> throw NotFoundProblem(body.crn, "Offender")
      is AuthorisableActionResult.Success -> authorisableResult.entity
    }

    val createdBooking = when (validatableResult) {
      is ValidatableActionResult.GeneralValidationError -> throw BadRequestProblem(errorDetail = validatableResult.message)
      is ValidatableActionResult.FieldValidationError -> throw BadRequestProblem(invalidParams = validatableResult.validationMessages)
      is ValidatableActionResult.ConflictError -> throw ConflictProblem(
        id = validatableResult.conflictingEntityId,
        conflictReason = validatableResult.message,
      )

      is ValidatableActionResult.Success -> validatableResult.entity
    }

    return ResponseEntity.ok(bookingTransformer.transformJpaToApi(createdBooking, personInfo))
  }

  override fun premisesPremisesIdBookingsBookingIdArrivalsPost(
    premisesId: UUID,
    bookingId: UUID,
    body: NewArrival,
  ): ResponseEntity<Arrival> {
    val booking = getBookingForPremisesOrThrow(premisesId, bookingId)

    val user = usersService.getUserForRequest()

    if (!userAccessService.userCanManagePremisesBookings(user, booking.premises)) {
      throw ForbiddenProblem()
    }

    val result = when (body) {
      is NewCas2Arrival -> {
        val bedId = booking.bed?.id
          ?: throw InternalServerErrorProblem("No bed ID present on Booking: $bookingId")

        throwIfBookingDatesConflict(body.arrivalDate, body.expectedDepartureDate, bookingId, bedId)
        throwIfLostBedDatesConflict(body.arrivalDate, body.expectedDepartureDate, null, bedId)

        bookingService.createArrival(
          booking = booking,
          arrivalDate = body.arrivalDate,
          expectedDepartureDate = body.expectedDepartureDate,
          notes = body.notes,
          keyWorkerStaffCode = body.keyWorkerStaffCode,
          user = user,
        )
      }

      is NewCas3Arrival -> {
        val bedId = booking.bed?.id
          ?: throw InternalServerErrorProblem("No bed ID present on Booking: $bookingId")

        throwIfBookingDatesConflict(body.arrivalDate, body.expectedDepartureDate, bookingId, bedId)
        throwIfLostBedDatesConflict(body.arrivalDate, body.expectedDepartureDate, null, bedId)

        bookingService.createCas3Arrival(
          booking = booking,
          arrivalDate = body.arrivalDate,
          expectedDepartureDate = body.expectedDepartureDate,
          notes = body.notes,
          keyWorkerStaffCode = body.keyWorkerStaffCode,
          user = user,
        )
      }

      else -> throw RuntimeException("Unsupported NewArrival type: ${body::class.qualifiedName}")
    }

    val arrival = extractResultEntityOrThrow(result)

    return ResponseEntity.ok(arrivalTransformer.transformJpaToApi(arrival))
  }

  override fun premisesPremisesIdBookingsBookingIdCancellationsPost(
    premisesId: UUID,
    bookingId: UUID,
    body: NewCancellation,
  ): ResponseEntity<Cancellation> {
    val user = usersService.getUserForRequest()

    val booking = getBookingForPremisesOrThrow(premisesId, bookingId)

    if (!userAccessService.userMayCancelBooking(user, booking)) {
      throw ForbiddenProblem()
    }

    when (booking.premises) {
      is ApprovedPremisesEntity -> {
        val result = cas1WithdrawableService.withdrawBooking(
          booking = booking,
          user = user,
          cancelledAt = body.date,
          userProvidedReason = body.reason,
          notes = body.notes,
          otherReason = body.otherReason,
        )
        val cancellation = extractEntityFromCasResult(result)
        return ResponseEntity.ok(cancellationTransformer.transformJpaToApi(cancellation))
      }

      is TemporaryAccommodationPremisesEntity -> {
        val result = bookingService.createCas3Cancellation(
          booking = booking,
          cancelledAt = body.date,
          reasonId = body.reason,
          notes = body.notes,
          user = user,
        )
        val cancellation = extractResultEntityOrThrow(result)
        return ResponseEntity.ok(cancellationTransformer.transformJpaToApi(cancellation))
      }

      else -> throw NotImplementedProblem("Unsupported premises type ${booking.premises::class.qualifiedName}")
    }
  }

  override fun premisesPremisesIdBookingsBookingIdConfirmationsPost(
    premisesId: UUID,
    bookingId: UUID,
    body: NewConfirmation,
  ): ResponseEntity<Confirmation> {
    val user = usersService.getUserForRequest()
    val booking = getBookingForPremisesOrThrow(premisesId, bookingId)

    if (!userAccessService.userCanManagePremisesBookings(user, booking.premises)) {
      throw ForbiddenProblem()
    }

    val result = bookingService.createConfirmation(
      booking = booking,
      dateTime = OffsetDateTime.now(),
      notes = body.notes,
      user,
    )

    val confirmation = extractResultEntityOrThrow(result)

    return ResponseEntity.ok(confirmationTransformer.transformJpaToApi(confirmation))
  }

  override fun premisesPremisesIdBookingsBookingIdDeparturesPost(
    premisesId: UUID,
    bookingId: UUID,
    body: NewDeparture,
  ): ResponseEntity<Departure> {
    val booking = getBookingForPremisesOrThrow(premisesId, bookingId)

    val user = usersService.getUserForRequest()

    if (!userAccessService.userCanManagePremisesBookings(user, booking.premises)) {
      throw ForbiddenProblem()
    }

    val result = bookingService.createDeparture(
      user = user,
      booking = booking,
      dateTime = body.dateTime.atOffset(ZoneOffset.UTC),
      reasonId = body.reasonId,
      moveOnCategoryId = body.moveOnCategoryId,
      notes = body.notes,
    )

    val departure = extractResultEntityOrThrow(result)

    return ResponseEntity.ok(departureTransformer.transformJpaToApi(departure))
  }

  override fun premisesPremisesIdBookingsBookingIdExtensionsPost(
    premisesId: UUID,
    bookingId: UUID,
    body: NewExtension,
  ): ResponseEntity<Extension> {
    val booking = getBookingForPremisesOrThrow(premisesId, bookingId)

    if (!userAccessService.currentUserCanManagePremisesBookings(booking.premises)) {
      throw ForbiddenProblem()
    }

    val result = bookingService.createExtension(
      booking = booking,
      newDepartureDate = body.newDepartureDate,
      notes = body.notes,
    )

    val extension = extractResultEntityOrThrow(result)

    return ResponseEntity.ok(extensionTransformer.transformJpaToApi(extension))
  }

  @Transactional
  override fun premisesPremisesIdBookingsBookingIdDateChangesPost(
    premisesId: UUID,
    bookingId: UUID,
    body: NewDateChange,
  ): ResponseEntity<DateChange> {
    val booking = getBookingForPremisesOrThrow(premisesId, bookingId)
    val user = usersService.getUserForRequest()

    if (!userAccessService.currentUserCanManagePremisesBookings(booking.premises)) {
      throw ForbiddenProblem()
    }

    val result = bookingService.createDateChange(
      booking = booking,
      user = user,
      newArrivalDate = body.newArrivalDate,
      newDepartureDate = body.newDepartureDate,
    )

    val dateChange = extractResultEntityOrThrow(result)

    return ResponseEntity.ok(dateChangeTransformer.transformJpaToApi(dateChange))
  }

  override fun premisesPremisesIdLostBedsPost(premisesId: UUID, body: NewLostBed): ResponseEntity<LostBed> {
    throwIfRequestIsForApprovedPremises("POST /cas1/premises/$premisesId/lost-beds")

    val premises = premisesService.getPremises(premisesId)
      ?: throw NotFoundProblem(premisesId, "Premises")

    if (!userAccessService.currentUserCanManagePremisesLostBeds(premises)) {
      throw ForbiddenProblem()
    }

    throwIfBookingDatesConflict(body.startDate, body.endDate, null, body.bedId)
    throwIfLostBedDatesConflict(body.startDate, body.endDate, null, body.bedId)

    val result = premisesService.createLostBeds(
      premises = premises,
      startDate = body.startDate,
      endDate = body.endDate,
      reasonId = body.reason,
      referenceNumber = body.referenceNumber,
      notes = body.notes,
      bedId = body.bedId,
    )

    val lostBeds = extractResultEntityOrThrow(result)

    return ResponseEntity.ok(lostBedsTransformer.transformJpaToApi(lostBeds))
  }

  override fun premisesPremisesIdLostBedsGet(premisesId: UUID): ResponseEntity<List<LostBed>> {
    throwIfRequestIsForApprovedPremises("GET /cas1/premises/$premisesId/lost-beds")

    val premises = premisesService.getPremises(premisesId)
      ?: throw NotFoundProblem(premisesId, "Premises")

    val lostBeds = lostBedsService.getActiveLostBedsForPremisesId(premisesId)

    if (!userAccessService.currentUserCanManagePremisesLostBeds(premises)) {
      throw ForbiddenProblem()
    }

    return ResponseEntity.ok(lostBeds.map(lostBedsTransformer::transformJpaToApi))
  }

  override fun premisesPremisesIdLostBedsLostBedIdGet(premisesId: UUID, lostBedId: UUID): ResponseEntity<LostBed> {
    throwIfRequestIsForApprovedPremises("GET /cas1/premises/$premisesId/lost-beds/$lostBedId")

    val premises = premisesService.getPremises(premisesId)
      ?: throw NotFoundProblem(premisesId, "Premises")

    if (!userAccessService.currentUserCanManagePremisesLostBeds(premises)) {
      throw ForbiddenProblem()
    }

    val lostBed = premises.lostBeds.firstOrNull { it.id == lostBedId }
      ?: throw NotFoundProblem(lostBedId, "LostBed")

    return ResponseEntity.ok(lostBedsTransformer.transformJpaToApi(lostBed))
  }

  override fun premisesPremisesIdLostBedsLostBedIdPut(
    premisesId: UUID,
    lostBedId: UUID,
    body: UpdateLostBed,
  ): ResponseEntity<LostBed> {
    throwIfRequestIsForApprovedPremises("PUT /cas1/premises/$premisesId/lost-beds/$lostBedId")

    val premises = premisesService.getPremises(premisesId) ?: throw NotFoundProblem(premisesId, "Premises")
    val lostBed = premises.lostBeds.firstOrNull { it.id == lostBedId } ?: throw NotFoundProblem(lostBedId, "LostBed")

    if (!userAccessService.currentUserCanManagePremisesLostBeds(premises)) {
      throw ForbiddenProblem()
    }

    throwIfBookingDatesConflict(body.startDate, body.endDate, null, lostBed.bed.id)
    throwIfLostBedDatesConflict(body.startDate, body.endDate, lostBedId, lostBed.bed.id)

    val updateLostBedResult = premisesService
      .updateLostBeds(
        lostBedId,
        body.startDate,
        body.endDate,
        body.reason,
        body.referenceNumber,
        body.notes,
      )

    val validationResult = when (updateLostBedResult) {
      is AuthorisableActionResult.NotFound -> throw NotFoundProblem(lostBedId, "LostBed")
      is AuthorisableActionResult.Unauthorised -> throw ForbiddenProblem()
      is AuthorisableActionResult.Success -> updateLostBedResult.entity
    }

    val updatedLostBed = when (validationResult) {
      is ValidatableActionResult.GeneralValidationError -> throw BadRequestProblem(errorDetail = validationResult.message)
      is ValidatableActionResult.FieldValidationError -> throw BadRequestProblem(invalidParams = validationResult.validationMessages)
      is ValidatableActionResult.ConflictError -> throw ConflictProblem(
        id = validationResult.conflictingEntityId,
        conflictReason = validationResult.message,
      )

      is ValidatableActionResult.Success -> validationResult.entity
    }

    return ResponseEntity.ok(lostBedsTransformer.transformJpaToApi(updatedLostBed))
  }

  override fun premisesPremisesIdLostBedsLostBedIdCancellationsPost(
    premisesId: UUID,
    lostBedId: UUID,
    body: NewLostBedCancellation,
  ): ResponseEntity<LostBedCancellation> {
    throwIfRequestIsForApprovedPremises("POST /cas1/premises/$premisesId/lost-beds/$lostBedId/cancellations")

    val premises = premisesService.getPremises(premisesId) ?: throw NotFoundProblem(premisesId, "Premises")
    val lostBed = premises.lostBeds.firstOrNull { it.id == lostBedId } ?: throw NotFoundProblem(lostBedId, "LostBed")

    if (!userAccessService.currentUserCanManagePremisesLostBeds(premises)) {
      throw ForbiddenProblem()
    }

    val cancelLostBedResult = premisesService.cancelLostBed(
      lostBed = lostBed,
      notes = body.notes,
    )

    val cancellation = when (cancelLostBedResult) {
      is ValidatableActionResult.GeneralValidationError -> throw BadRequestProblem(errorDetail = cancelLostBedResult.message)
      is ValidatableActionResult.FieldValidationError -> throw BadRequestProblem(invalidParams = cancelLostBedResult.validationMessages)
      is ValidatableActionResult.ConflictError -> throw ConflictProblem(
        id = cancelLostBedResult.conflictingEntityId,
        conflictReason = cancelLostBedResult.message,
      )

      is ValidatableActionResult.Success -> cancelLostBedResult.entity
    }

    return ResponseEntity.ok(lostBedCancellationTransformer.transformJpaToApi(cancellation))
  }

  override fun premisesPremisesIdStaffGet(premisesId: UUID): ResponseEntity<List<StaffMember>> {
    val premises = premisesService.getPremises(premisesId)
      ?: throw NotFoundProblem(premisesId, "Premises")

    if (!userAccessService.currentUserCanViewPremisesStaff(premises)) {
      throw ForbiddenProblem()
    }

    if (premises !is ApprovedPremisesEntity) {
      throw NotImplementedProblem("Fetching staff for non-AP Premises is not currently supported")
    }

    val staffMembersResult = staffMemberService.getStaffMembersForQCode(premises.qCode)

    val staffMembers = extractEntityFromCasResult(staffMembersResult)

    return ResponseEntity.ok(staffMembers.content.map(staffMemberTransformer::transformDomainToApi))
  }

  override fun premisesPremisesIdRoomsGet(premisesId: UUID): ResponseEntity<List<Room>> {
    val premises = premisesService.getPremises(premisesId) ?: throw NotFoundProblem(premisesId, "Premises")

    if (!userAccessService.currentUserCanViewPremises(premises)) {
      throw ForbiddenProblem()
    }

    return ResponseEntity.ok(premises.rooms.map(roomTransformer::transformJpaToApi))
  }

  override fun premisesPremisesIdRoomsPost(premisesId: UUID, newRoom: NewRoom): ResponseEntity<Room> {
    val premises = premisesService.getPremises(premisesId) ?: throw NotFoundProblem(premisesId, "Premises")

    if (!userAccessService.currentUserCanManagePremises(premises)) {
      throw ForbiddenProblem()
    }

    val room = extractResultEntityOrThrow(
      roomService.createRoom(premises, newRoom.name, newRoom.notes, newRoom.characteristicIds, newRoom.bedEndDate),
    )

    return ResponseEntity(roomTransformer.transformJpaToApi(room), HttpStatus.CREATED)
  }

  override fun premisesPremisesIdRoomsRoomIdGet(premisesId: UUID, roomId: UUID): ResponseEntity<Room> {
    val premises = premisesService.getPremises(premisesId) ?: throw NotFoundProblem(premisesId, "Premises")

    if (!userAccessService.currentUserCanViewPremises(premises)) {
      throw ForbiddenProblem()
    }

    val room = premises.rooms.find { it.id == roomId } ?: throw NotFoundProblem(roomId, "Room")

    return ResponseEntity.ok(roomTransformer.transformJpaToApi(room))
  }

  @Transactional
  override fun premisesPremisesIdRoomsRoomIdPut(
    premisesId: UUID,
    roomId: UUID,
    updateRoom: UpdateRoom,
  ): ResponseEntity<Room> {
    val premises = premisesService.getPremises(premisesId) ?: throw NotFoundProblem(premisesId, "Premises")

    if (!userAccessService.currentUserCanManagePremises(premises)) {
      throw ForbiddenProblem()
    }

    val updateRoomResult = roomService.updateRoom(premises, roomId, updateRoom.notes, updateRoom.characteristicIds)

    var validationResult = when (updateRoomResult) {
      is AuthorisableActionResult.NotFound -> throw NotFoundProblem(roomId, "Room")
      is AuthorisableActionResult.Success -> updateRoomResult.entity
      is AuthorisableActionResult.Unauthorised -> throw ForbiddenProblem()
    }

    if (premises is TemporaryAccommodationPremisesEntity) {
      val updateRoomName = updateRoom.name
      if (updateRoomName != null) {
        validationResult =
          extractEntityFromAuthorisableActionResult(roomService.renameRoom(premises, roomId, updateRoomName))
      }
      val updateRoomEndDate = updateRoom.bedEndDate
      if (updateRoomEndDate != null) {
        validationResult = extractEntityFromAuthorisableActionResult(
          roomService.updateBedEndDate(
            premises,
            roomId,
            updateRoomEndDate,
          ),
        )
      }
    }

    val room = extractResultEntityOrThrow(validationResult)

    return ResponseEntity.ok(roomTransformer.transformJpaToApi(room))
  }

  override fun premisesPremisesIdBookingsBookingIdTurnaroundsPost(
    premisesId: UUID,
    bookingId: UUID,
    body: NewTurnaround,
  ): ResponseEntity<Turnaround> {
    val booking = getBookingForPremisesOrThrow(premisesId, bookingId)

    if (!userAccessService.currentUserCanManagePremisesBookings(booking.premises)) {
      throw ForbiddenProblem()
    }

    val result = bookingService.createTurnaround(booking, body.workingDays)
    val turnaround = extractResultEntityOrThrow(result)

    return ResponseEntity.ok(turnaroundTransformer.transformJpaToApi(turnaround))
  }

  override fun premisesPremisesIdBedsGet(premisesId: UUID): ResponseEntity<List<BedSummary>> {
    val premises = premisesService.getPremises(premisesId)
      ?: throw NotFoundProblem(premisesId, "Premises")

    if (!userAccessService.currentUserCanViewPremises(premises)) {
      throw ForbiddenProblem()
    }

    return ResponseEntity.ok(premisesService.getBeds(premisesId).map(bedSummaryTransformer::transformToApi))
  }

  override fun premisesPremisesIdBedsBedIdGet(premisesId: UUID, bedId: UUID): ResponseEntity<BedDetail> {
    val premises = premisesService.getPremises(premisesId)
      ?: throw NotFoundProblem(premisesId, "Premises")

    if (!userAccessService.currentUserCanViewPremises(premises)) {
      throw ForbiddenProblem()
    }

    val validationResult = when (val bedResult = bedService.getBedAndRoomCharacteristics(bedId)) {
      is AuthorisableActionResult.NotFound -> throw NotFoundProblem(bedId, "Bed")
      is AuthorisableActionResult.Success -> bedResult.entity
      is AuthorisableActionResult.Unauthorised -> throw ForbiddenProblem()
    }

    return ResponseEntity.ok(bedDetailTransformer.transformToApi(validationResult))
  }

  override fun premisesPremisesIdSummaryGet(premisesId: UUID): ResponseEntity<ExtendedPremisesSummary> = runBlocking {
    val premises = premisesService.getPremises(premisesId)
      ?: throw NotFoundProblem(premisesId, "Premises")

    val user = usersService.getUserForRequest()

    if (!userAccessService.userCanManagePremisesBookings(user, premises)) {
      throw ForbiddenProblem()
    }

    val bookingsSummary = premisesService.getPremisesSummary(premisesId)
    val crns = bookingsSummary.map { it.getCrn() }
    val personSummary = offenderService.getOffenderSummariesByCrns(
      crns,
      user.deliusUsername,
      user.hasQualification(UserQualification.LAO),
    )
    val totalBeds = premisesService.getBedCount(premises)

    val bookingsSummaryMapped = bookingsSummary.map {
      val personInfo = personSummary.find { personSummary -> personSummary.crn == it.getCrn() }
        ?: PersonSummaryInfoResult.NotFound(it.getCrn())
      bookingTransformer.transformBookingSummary(it, personInfo)
    }

    val availableBedsForToday =
      premisesService.getAvailabilityForRange(premises, LocalDate.now(), LocalDate.now().plusDays(1))
        .values.first().getFreeCapacity(totalBeds)

    val dateCapacities = premisesService.getDateCapacities(premises)

    var apCode: String? = null
    if (premises is ApprovedPremisesEntity) {
      apCode = premises.apCode
    }

    return@runBlocking ResponseEntity.ok(
      ExtendedPremisesSummary(
        id = premisesId,
        name = premises.name,
        apCode = apCode,
        postcode = premises.postcode,
        bedCount = totalBeds,
        availableBedsForToday = availableBedsForToday,
        bookings = bookingsSummaryMapped,
        dateCapacities = dateCapacities,
      ),
    )
  }

  private fun getBookingForPremisesOrThrow(premisesId: UUID, bookingId: UUID) =
    when (val result = bookingService.getBookingForPremises(premisesId, bookingId)) {
      is GetBookingForPremisesResult.Success -> result.booking
      is GetBookingForPremisesResult.PremisesNotFound -> throw NotFoundProblem(premisesId, "Premises")
      is GetBookingForPremisesResult.BookingNotFound -> throw NotFoundProblem(bookingId, "Booking")
    }

  private fun <EntityType> extractResultEntityOrThrow(result: ValidatableActionResult<EntityType>) = when (result) {
    is ValidatableActionResult.Success -> result.entity
    is ValidatableActionResult.GeneralValidationError -> throw BadRequestProblem(errorDetail = result.message)
    is ValidatableActionResult.FieldValidationError -> throw BadRequestProblem(invalidParams = result.validationMessages)
    is ValidatableActionResult.ConflictError -> throw ConflictProblem(
      id = result.conflictingEntityId,
      conflictReason = result.message,
    )
  }

  private fun throwIfBookingDatesConflict(
    arrivalDate: LocalDate,
    departureDate: LocalDate,
    thisEntityId: UUID?,
    bedId: UUID,
  ) {
    bookingService.getBookingWithConflictingDates(arrivalDate, departureDate, thisEntityId, bedId)?.let {
      throw ConflictProblem(
        it.id,
        "A Booking already exists for dates from ${it.arrivalDate} to ${it.departureDate} which overlaps with the desired dates",
      )
    }
  }

  private fun throwIfLostBedDatesConflict(
    startDate: LocalDate,
    endDate: LocalDate,
    thisEntityId: UUID?,
    bedId: UUID,
  ) {
    bookingService.getLostBedWithConflictingDates(startDate, endDate, thisEntityId, bedId)?.let {
      throw ConflictProblem(
        it.id,
        "A Lost Bed already exists for dates from ${it.startDate} to ${it.endDate} which overlaps with the desired dates",
      )
    }
  }

  private fun throwIfRequestIsForApprovedPremises(endpoint: String) {
    getRequest().ifPresent {
      if (it.getHeader("X-Service-Name") == ServiceName.approvedPremises.value) {
        throw BadRequestProblem(errorDetail = "CAS1 not supported. Use `$endpoint` instead")
      }
    }
  }
}
