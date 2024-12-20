package uk.gov.justice.digital.hmpps.approvedpremisesapi.controller.cas1

import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.cas1.SpaceBookingsCas1Delegate
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1AssignKeyWorker
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1NewArrival
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1NewDeparture
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1NewSpaceBooking
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1NewSpaceBookingCancellation
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1NonArrival
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1SpaceBooking
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1SpaceBookingResidency
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1SpaceBookingSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1SpaceBookingSummarySortField
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1UpdateSpaceBooking
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ServiceName
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.SortDirection
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.TimelineEvent
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.Cas1SpaceBookingEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserPermission
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserPermission.CAS1_SPACE_BOOKING_LIST
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserPermission.CAS1_SPACE_BOOKING_RECORD_NON_ARRIVAL
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserPermission.CAS1_SPACE_BOOKING_VIEW
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserQualification
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.forCrn
import uk.gov.justice.digital.hmpps.approvedpremisesapi.problem.BadRequestProblem
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.CharacteristicService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.OffenderService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.UserAccessService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.UserService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.Cas1SpaceBookingManagementDomainEventService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.Cas1SpaceBookingService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.Cas1SpaceBookingService.DepartureInfo
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.Cas1SpaceBookingService.SpaceBookingFilterCriteria
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.Cas1WithdrawableService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1LimitedAccessStrategy
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.cas1.Cas1SpaceBookingTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.PageCriteria
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.ensureEntityFromCasResultIsSuccess
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.extractEntityFromCasResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.toLocalDate
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.toLocalDateTime
import java.time.LocalTime
import java.util.UUID

@Service
class Cas1SpaceBookingController(
  private val userAccessService: UserAccessService,
  private val userService: UserService,
  private val offenderService: OffenderService,
  private val spaceBookingService: Cas1SpaceBookingService,
  private val spaceBookingTransformer: Cas1SpaceBookingTransformer,
  private val cas1SpaceBookingService: Cas1SpaceBookingService,
  private val cas1WithdrawableService: Cas1WithdrawableService,
  private val cas1SpaceBookingManagementDomainEventService: Cas1SpaceBookingManagementDomainEventService,
  private val characteristicService: CharacteristicService,
) : SpaceBookingsCas1Delegate {

  override fun getSpaceBookingTimeline(premisesId: UUID, bookingId: UUID): ResponseEntity<List<TimelineEvent>> {
    val events = cas1SpaceBookingManagementDomainEventService.getTimeline(bookingId)
    return ResponseEntity(events, HttpStatus.OK)
  }

  override fun createSpaceBooking(
    placementRequestId: UUID,
    body: Cas1NewSpaceBooking,
  ): ResponseEntity<Cas1SpaceBooking> {
    userAccessService.ensureCurrentUserHasPermission(UserPermission.CAS1_SPACE_BOOKING_CREATE)

    val user = userService.getUserForRequest()

    val characteristics = characteristicService.getCharacteristicsByPropertyNames(
      body.requirements.essentialCharacteristics.map { it.value },
      ServiceName.approvedPremises,
    )

    val booking = extractEntityFromCasResult(
      spaceBookingService.createNewBooking(
        body.premisesId,
        placementRequestId,
        body.arrivalDate,
        body.departureDate,
        user,
        characteristics,
      ),
    )

    return ResponseEntity.ok(toCas1SpaceBooking(booking))
  }

  override fun getSpaceBookings(
    premisesId: UUID,
    residency: Cas1SpaceBookingResidency?,
    crnOrName: String?,
    keyWorkerStaffCode: String?,
    sortDirection: SortDirection?,
    sortBy: Cas1SpaceBookingSummarySortField?,
    page: Int?,
    perPage: Int?,
  ): ResponseEntity<List<Cas1SpaceBookingSummary>> {
    userAccessService.ensureCurrentUserHasPermission(CAS1_SPACE_BOOKING_LIST)

    val result = spaceBookingService.search(
      premisesId,
      SpaceBookingFilterCriteria(
        residency = residency,
        crnOrName = crnOrName,
        keyWorkerStaffCode = keyWorkerStaffCode,
      ),
      PageCriteria(
        sortBy = sortBy ?: Cas1SpaceBookingSummarySortField.personName,
        sortDirection = sortDirection ?: SortDirection.desc,
        page = page,
        perPage = perPage,
      ),
    )

    val (searchResults, metadata) = extractEntityFromCasResult(result)

    val user = userService.getUserForRequest()
    val offenderSummaries = offenderService.getPersonSummaryInfoResults(
      crns = searchResults.map { it.crn }.toSet(),
      limitedAccessStrategy = user.cas1LimitedAccessStrategy(),
    )

    val summaries = searchResults.map {
      spaceBookingTransformer.transformSearchResultToSummary(
        it,
        offenderSummaries.forCrn(it.crn),
      )
    }

    return ResponseEntity.ok()
      .headers(metadata?.toHeaders())
      .body(summaries)
  }

  override fun getSpaceBookingById(bookingId: UUID): ResponseEntity<Cas1SpaceBooking> {
    userAccessService.ensureCurrentUserHasPermission(CAS1_SPACE_BOOKING_VIEW)

    val booking = extractEntityFromCasResult(spaceBookingService.getBooking(bookingId))

    return ResponseEntity
      .ok()
      .body(toCas1SpaceBooking(booking))
  }

  override fun getSpaceBookingByPremiseAndId(premisesId: UUID, bookingId: UUID): ResponseEntity<Cas1SpaceBooking> {
    userAccessService.ensureCurrentUserHasPermission(CAS1_SPACE_BOOKING_VIEW)

    val booking = extractEntityFromCasResult(spaceBookingService.getBooking(premisesId, bookingId))

    return ResponseEntity
      .ok()
      .body(toCas1SpaceBooking(booking))
  }

  override fun updateSpaceBooking(
    premisesId: UUID,
    bookingId: UUID,
    cas1UpdateSpaceBooking: Cas1UpdateSpaceBooking,
  ): ResponseEntity<Unit> {
    return super.updateSpaceBooking(premisesId, bookingId, cas1UpdateSpaceBooking)
  }

  override fun recordArrival(
    premisesId: UUID,
    bookingId: UUID,
    cas1NewArrival: Cas1NewArrival,
  ): ResponseEntity<Unit> {
    userAccessService.ensureCurrentUserHasPermission(UserPermission.CAS1_SPACE_BOOKING_RECORD_ARRIVAL)

    val arrivalDateAndTime = if (cas1NewArrival.arrivalDateTime != null) {
      Pair(
        cas1NewArrival.arrivalDateTime!!.toLocalDate(),
        cas1NewArrival.arrivalDateTime!!.toLocalDateTime().toLocalTime(),
      )
    } else {
      val arrivalDate = cas1NewArrival.arrivalDate
        ?: throw BadRequestProblem(invalidParams = mapOf("arrivalDate" to "is required"))
      val arrivalTime = cas1NewArrival.arrivalTime
        ?: throw BadRequestProblem(invalidParams = mapOf("arrivalTime" to "is required"))
      Pair(
        arrivalDate,
        LocalTime.parse(arrivalTime),
      )
    }

    ensureEntityFromCasResultIsSuccess(
      cas1SpaceBookingService.recordArrivalForBooking(
        premisesId,
        bookingId,
        arrivalDate = arrivalDateAndTime.first,
        arrivalTime = arrivalDateAndTime.second,
      ),
    )
    return ResponseEntity(HttpStatus.OK)
  }

  override fun recordDeparture(
    premisesId: UUID,
    bookingId: UUID,
    cas1NewDeparture: Cas1NewDeparture,
  ): ResponseEntity<Unit> {
    userAccessService.ensureCurrentUserHasPermission(UserPermission.CAS1_SPACE_BOOKING_RECORD_DEPARTURE)

    val departureDateAndTime = if (cas1NewDeparture.departureDateTime != null) {
      Pair(
        cas1NewDeparture.departureDateTime!!.toLocalDate(),
        cas1NewDeparture.departureDateTime!!.toLocalDateTime().toLocalTime(),
      )
    } else {
      val departureDate = cas1NewDeparture.departureDate
        ?: throw BadRequestProblem(invalidParams = mapOf("departureDate" to "is required"))
      val departureTime = cas1NewDeparture.departureTime
        ?: throw BadRequestProblem(invalidParams = mapOf("departureTime" to "is required"))
      Pair(
        departureDate,
        LocalTime.parse(departureTime),
      )
    }

    ensureEntityFromCasResultIsSuccess(
      cas1SpaceBookingService.recordDepartureForBooking(
        premisesId,
        bookingId,
        DepartureInfo(
          departureDate = departureDateAndTime.first,
          departureTime = departureDateAndTime.second,
          reasonId = cas1NewDeparture.reasonId,
          moveOnCategoryId = cas1NewDeparture.moveOnCategoryId,
          notes = cas1NewDeparture.notes,
        ),
      ),
    )
    return ResponseEntity(HttpStatus.OK)
  }

  override fun assignKeyworker(
    premisesId: UUID,
    bookingId: UUID,
    cas1AssignKeyWorker: Cas1AssignKeyWorker,
  ): ResponseEntity<Unit> {
    userAccessService.ensureCurrentUserHasPermission(UserPermission.CAS1_SPACE_BOOKING_RECORD_KEYWORKER)

    ensureEntityFromCasResultIsSuccess(
      cas1SpaceBookingService.recordKeyWorkerAssignedForBooking(
        premisesId,
        bookingId,
        cas1AssignKeyWorker,
      ),
    )
    return ResponseEntity(HttpStatus.OK)
  }

  override fun cancelSpaceBooking(
    premisesId: UUID,
    bookingId: UUID,
    body: Cas1NewSpaceBookingCancellation,
  ): ResponseEntity<Unit> {
    userAccessService.ensureCurrentUserHasPermission(UserPermission.CAS1_SPACE_BOOKING_WITHDRAW)

    val spaceBooking = extractEntityFromCasResult(
      cas1SpaceBookingService.getBooking(premisesId, bookingId),
    )

    return ResponseEntity
      .ok()
      .body(
        extractEntityFromCasResult(
          cas1WithdrawableService.withdrawSpaceBooking(
            spaceBooking = spaceBooking,
            user = userService.getUserForRequest(),
            cancelledAt = body.occurredAt,
            body.reasonId,
            body.reasonNotes,
          ),
        ),
      )
  }

  override fun recordNonArrival(
    premisesId: UUID,
    bookingId: UUID,
    cas1NonArrival: Cas1NonArrival,
  ): ResponseEntity<Unit> {
    userAccessService.ensureCurrentUserHasPermission(CAS1_SPACE_BOOKING_RECORD_NON_ARRIVAL)

    val user = userService.getUserForRequest()

    ensureEntityFromCasResultIsSuccess(
      cas1SpaceBookingService.recordNonArrivalForBooking(
        premisesId,
        bookingId,
        cas1NonArrival,
        user,
      ),
    )
    return ResponseEntity(HttpStatus.OK)
  }

  private fun toCas1SpaceBooking(booking: Cas1SpaceBookingEntity): Cas1SpaceBooking {
    val user = userService.getUserForRequest()

    val person = offenderService.getPersonInfoResult(
      booking.crn,
      user.deliusUsername,
      user.hasQualification(UserQualification.LAO),
    )

    val otherBookingsInPremiseForCrn = spaceBookingService.getBookingsForPremisesAndCrn(
      premisesId = booking.premises.id,
      crn = booking.crn,
    ).filter { it.id != booking.id }

    return spaceBookingTransformer.transformJpaToApi(person, booking, otherBookingsInPremiseForCrn)
  }
}
