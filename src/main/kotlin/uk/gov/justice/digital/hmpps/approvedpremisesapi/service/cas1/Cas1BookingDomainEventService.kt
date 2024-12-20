package uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1

import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model.BookingCancelled
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model.BookingCancelledEnvelope
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model.BookingChanged
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model.BookingChangedEnvelope
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model.BookingMade
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model.BookingMadeBookedBy
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model.BookingMadeEnvelope
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model.BookingNotMade
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model.BookingNotMadeEnvelope
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model.Cru
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model.EventType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model.PersonReference
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model.Premises
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.ApDeliusContextApiClient
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.ClientResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApprovedPremisesApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApprovedPremisesEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.BookingEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.CancellationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.CancellationReasonEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.Cas1SpaceBookingEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.MetaDataName
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PlacementRequestEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserQualification
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.cas1.Cas1ApplicationFacade
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.DomainEvent
import uk.gov.justice.digital.hmpps.approvedpremisesapi.results.AuthorisableActionResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.OffenderService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.UrlTemplate
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.mapOfNonNullValues
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.util.UUID

@Service
class Cas1BookingDomainEventService(
  val domainEventService: Cas1DomainEventService,
  val offenderService: OffenderService,
  val apDeliusContextApiClient: ApDeliusContextApiClient,
  @Value("\${url-templates.frontend.application}") private val applicationUrlTemplate: UrlTemplate,
) {

  fun spaceBookingMade(
    application: ApprovedPremisesApplicationEntity,
    booking: Cas1SpaceBookingEntity,
    user: UserEntity,
    placementRequest: PlacementRequestEntity,
  ) {
    bookingMade(
      applicationId = application.id,
      eventNumber = application.eventNumber,
      bookingInfo = booking.toBookingInfo(),
      user = user,
      applicationSubmittedOn = application.submittedAt,
      releaseType = application.releaseType,
      sentenceType = application.sentenceType,
      situation = application.situation,
      placementRequestId = placementRequest.id,
    )
  }

  fun bookingMade(
    application: ApprovedPremisesApplicationEntity,
    booking: BookingEntity,
    user: UserEntity,
    placementRequest: PlacementRequestEntity,
  ) {
    bookingMade(
      applicationId = application.id,
      eventNumber = application.eventNumber,
      bookingInfo = booking.toBookingInfo(),
      user = user,
      applicationSubmittedOn = application.submittedAt,
      releaseType = application.releaseType,
      sentenceType = application.sentenceType,
      situation = application.situation,
      placementRequestId = placementRequest.id,
    )
  }

  private fun BookingEntity.toBookingInfo() = BookingInfo(
    id = id,
    createdAt = createdAt,
    crn = crn,
    premises = premises as ApprovedPremisesEntity,
    arrivalDate = arrivalDate,
    departureDate = departureDate,
    isSpaceBooking = false,
  )

  private fun Cas1SpaceBookingEntity.toBookingInfo() = BookingInfo(
    id = id,
    createdAt = createdAt,
    crn = crn,
    premises = premises,
    arrivalDate = canonicalArrivalDate,
    departureDate = canonicalDepartureDate,
    isSpaceBooking = true,
  )

  fun bookingNotMade(
    user: UserEntity,
    placementRequest: PlacementRequestEntity,
    bookingNotCreatedAt: OffsetDateTime,
    notes: String?,
  ) {
    val domainEventId = UUID.randomUUID()

    val application = placementRequest.application

    val offenderDetails = getOffenderDetails(
      application.crn,
      user.deliusUsername,
      user.hasQualification(UserQualification.LAO),
    )

    val staffDetails = getStaffDetails(user.deliusUsername)

    domainEventService.saveBookingNotMadeEvent(
      DomainEvent(
        id = domainEventId,
        applicationId = application.id,
        crn = application.crn,
        nomsNumber = offenderDetails?.otherIds?.nomsNumber,
        occurredAt = bookingNotCreatedAt.toInstant(),
        data = BookingNotMadeEnvelope(
          id = domainEventId,
          timestamp = bookingNotCreatedAt.toInstant(),
          eventType = EventType.bookingNotMade,
          eventDetails = BookingNotMade(
            applicationId = application.id,
            applicationUrl = applicationUrlTemplate.resolve("id", application.id.toString()),
            personReference = PersonReference(
              crn = application.crn,
              noms = offenderDetails?.otherIds?.nomsNumber ?: "Unknown NOMS Number",
            ),
            deliusEventNumber = application.eventNumber,
            attemptedAt = bookingNotCreatedAt.toInstant(),
            attemptedBy = BookingMadeBookedBy(
              staffMember = staffDetails.toStaffMember(),
              cru = Cru(
                name = user.apArea?.name ?: "Unknown CRU",
              ),
            ),
            failureDescription = notes,
          ),
        ),
        metadata = mapOfNonNullValues(
          MetaDataName.CAS1_PLACEMENT_REQUEST_ID to placementRequest.id.toString(),
        ),
      ),
    )
  }

  private fun getOffenderDetails(
    crn: String,
    deliusUsername: String,
    ignoreLaoRestrictions: Boolean,
  ) = when (val offenderDetailsResult = offenderService.getOffenderByCrn(crn, deliusUsername, ignoreLaoRestrictions)) {
    is AuthorisableActionResult.Success -> offenderDetailsResult.entity
    else -> null
  }

  private fun getStaffDetails(deliusUsername: String) =
    when (val staffDetailsResult = apDeliusContextApiClient.getStaffDetail(deliusUsername)) {
      is ClientResult.Success -> staffDetailsResult.body
      is ClientResult.Failure -> staffDetailsResult.throwException()
    }

  data class BookingInfo(
    val id: UUID,
    val createdAt: OffsetDateTime,
    val crn: String,
    val premises: ApprovedPremisesEntity,
    val arrivalDate: LocalDate,
    val departureDate: LocalDate,
    val isSpaceBooking: Boolean,
  )

  private fun bookingMade(
    applicationId: UUID,
    eventNumber: String,
    bookingInfo: BookingInfo,
    user: UserEntity,
    applicationSubmittedOn: OffsetDateTime?,
    sentenceType: String?,
    releaseType: String?,
    situation: String?,
    placementRequestId: UUID?,
  ) {
    val domainEventId = UUID.randomUUID()
    val crn = bookingInfo.crn

    val offenderDetails =
      when (val offenderDetailsResult = offenderService.getOffenderByCrn(crn, user.deliusUsername, true)) {
        is AuthorisableActionResult.Success -> offenderDetailsResult.entity
        else -> null
      }

    val staffDetails = getStaffDetails(user.deliusUsername)

    val approvedPremises = bookingInfo.premises
    val bookingCreatedAt = bookingInfo.createdAt
    val isSpaceBooking = bookingInfo.isSpaceBooking

    domainEventService.saveBookingMadeDomainEvent(
      DomainEvent(
        id = domainEventId,
        applicationId = applicationId,
        crn = crn,
        nomsNumber = offenderDetails?.otherIds?.nomsNumber,
        occurredAt = bookingCreatedAt.toInstant(),
        bookingId = if (isSpaceBooking) {
          null
        } else {
          bookingInfo.id
        },
        cas1SpaceBookingId = if (isSpaceBooking) {
          bookingInfo.id
        } else {
          null
        },
        data = BookingMadeEnvelope(
          id = domainEventId,
          timestamp = bookingCreatedAt.toInstant(),
          eventType = EventType.bookingMade,
          eventDetails = BookingMade(
            applicationId = applicationId,
            applicationUrl = applicationUrlTemplate.resolve("id", applicationId.toString()),
            bookingId = bookingInfo.id,
            personReference = PersonReference(
              crn = crn,
              noms = offenderDetails?.otherIds?.nomsNumber ?: "Unknown NOMS Number",
            ),
            deliusEventNumber = eventNumber,
            createdAt = bookingCreatedAt.toInstant(),
            bookedBy = BookingMadeBookedBy(
              staffMember = staffDetails.toStaffMember(),
              cru = Cru(
                name = user.apArea?.name ?: "Unknown CRU",
              ),
            ),
            premises = Premises(
              id = approvedPremises.id,
              name = approvedPremises.name,
              apCode = approvedPremises.apCode,
              legacyApCode = approvedPremises.qCode,
              localAuthorityAreaName = approvedPremises.localAuthorityArea!!.name,
            ),
            arrivalOn = bookingInfo.arrivalDate,
            departureOn = bookingInfo.departureDate,
            applicationSubmittedOn = applicationSubmittedOn?.toInstant(),
            releaseType = releaseType,
            sentenceType = sentenceType,
            situation = situation,
          ),
        ),
        metadata = mapOfNonNullValues(
          MetaDataName.CAS1_PLACEMENT_REQUEST_ID to placementRequestId?.toString(),
        ),
      ),
    )
  }

  fun bookingChanged(
    booking: BookingEntity,
    changedBy: UserEntity,
    bookingChangedAt: OffsetDateTime,
  ) {
    val domainEventId = UUID.randomUUID()
    val applicationFacade = booking.cas1ApplicationFacade
    val applicationId = applicationFacade.id
    val eventNumber = applicationFacade.eventNumber!!

    val offenderDetails = getOffenderDetails(
      booking.crn,
      changedBy.deliusUsername,
      ignoreLaoRestrictions = true,
    )

    val staffDetails = when (val staffDetailsResult = apDeliusContextApiClient.getStaffDetail(changedBy.deliusUsername)) {
      is ClientResult.Success -> staffDetailsResult.body
      is ClientResult.Failure -> staffDetailsResult.throwException()
    }

    val approvedPremises = booking.premises as ApprovedPremisesEntity

    domainEventService.saveBookingChangedEvent(
      DomainEvent(
        id = domainEventId,
        applicationId = applicationId,
        crn = booking.crn,
        nomsNumber = offenderDetails?.otherIds?.nomsNumber,
        occurredAt = bookingChangedAt.toInstant(),
        bookingId = booking.id,
        data = BookingChangedEnvelope(
          id = domainEventId,
          timestamp = bookingChangedAt.toInstant(),
          eventType = EventType.bookingChanged,
          eventDetails = BookingChanged(
            applicationId = applicationId,
            applicationUrl = applicationUrlTemplate.resolve("id", applicationId.toString()),
            bookingId = booking.id,
            personReference = PersonReference(
              crn = booking.crn,
              noms = offenderDetails?.otherIds?.nomsNumber ?: "Unknown NOMS Number",
            ),
            deliusEventNumber = eventNumber,
            changedAt = bookingChangedAt.toInstant(),
            changedBy = staffDetails.toStaffMember(),
            premises = Premises(
              id = approvedPremises.id,
              name = approvedPremises.name,
              apCode = approvedPremises.apCode,
              legacyApCode = approvedPremises.qCode,
              localAuthorityAreaName = approvedPremises.localAuthorityArea!!.name,
            ),
            arrivalOn = booking.arrivalDate,
            departureOn = booking.departureDate,
          ),
        ),
      ),
    )
  }

  fun bookingCancelled(
    booking: BookingEntity,
    user: UserEntity,
    cancellation: CancellationEntity,
    reason: CancellationReasonEntity,
  ) = bookingCancelled(
    CancellationInfo(
      bookingId = booking.id,
      applicationFacade = booking.cas1ApplicationFacade,
      cancellationId = cancellation.id,
      crn = booking.crn,
      cancelledAt = cancellation.date,
      reason = reason,
      cancelledBy = user,
      premises = booking.premises as ApprovedPremisesEntity,
      isSpaceBooking = false,
    ),
  )

  fun spaceBookingCancelled(
    spaceBooking: Cas1SpaceBookingEntity,
    user: UserEntity,
    reason: CancellationReasonEntity,
  ) =
    bookingCancelled(
      CancellationInfo(
        bookingId = spaceBooking.id,
        applicationFacade = spaceBooking.applicationFacade,
        cancellationId = null,
        crn = spaceBooking.crn,
        cancelledAt = spaceBooking.cancellationOccurredAt!!,
        reason = reason,
        cancelledBy = user,
        premises = spaceBooking.premises,
        isSpaceBooking = true,
      ),
    )

  private fun bookingCancelled(
    cancellationInfo: CancellationInfo,
  ) {
    val bookingId = cancellationInfo.bookingId
    val now = OffsetDateTime.now()
    val user = cancellationInfo.cancelledBy
    val crn = cancellationInfo.crn
    val premises = cancellationInfo.premises
    val isSpaceBooking = cancellationInfo.isSpaceBooking

    val domainEventId = UUID.randomUUID()

    val offenderDetails =
      when (val offenderDetailsResult = offenderService.getOffenderByCrn(crn, user.deliusUsername, user.hasQualification(UserQualification.LAO))) {
        is AuthorisableActionResult.Success -> offenderDetailsResult.entity
        else -> null
      }

    val staffDetails = getStaffDetails(user.deliusUsername)

    val applicationId = cancellationInfo.applicationFacade.id
    val eventNumber = cancellationInfo.applicationFacade.eventNumber!!

    domainEventService.saveBookingCancelledEvent(
      DomainEvent(
        id = domainEventId,
        applicationId = applicationId,
        crn = crn,
        nomsNumber = offenderDetails?.otherIds?.nomsNumber,
        occurredAt = now.toInstant(),
        bookingId = if (isSpaceBooking) {
          null
        } else {
          bookingId
        },
        cas1SpaceBookingId = if (isSpaceBooking) {
          bookingId
        } else {
          null
        },
        schemaVersion = 2,
        data = BookingCancelledEnvelope(
          id = domainEventId,
          timestamp = now.toInstant(),
          eventType = EventType.bookingCancelled,
          eventDetails = BookingCancelled(
            applicationId = applicationId,
            applicationUrl = applicationUrlTemplate.resolve("id", applicationId.toString()),
            bookingId = bookingId,
            personReference = PersonReference(
              crn = crn,
              noms = offenderDetails?.otherIds?.nomsNumber ?: "Unknown NOMS Number",
            ),
            deliusEventNumber = eventNumber,
            premises = Premises(
              id = premises.id,
              name = premises.name,
              apCode = premises.apCode,
              legacyApCode = premises.qCode,
              localAuthorityAreaName = premises.localAuthorityArea!!.name,
            ),
            cancelledBy = staffDetails.toStaffMember(),
            cancelledAt = cancellationInfo.cancelledAt.atTime(0, 0).toInstant(ZoneOffset.UTC),
            cancelledAtDate = cancellationInfo.cancelledAt,
            cancellationReason = cancellationInfo.reason.name,
            cancellationRecordedAt = now.toInstant(),
          ),
        ),
        metadata = mapOfNonNullValues(
          MetaDataName.CAS1_CANCELLATION_ID to cancellationInfo.cancellationId?.toString(),
        ),
      ),
    )
  }

  private data class CancellationInfo(
    val bookingId: UUID,
    val applicationFacade: Cas1ApplicationFacade,
    val crn: String,
    val premises: ApprovedPremisesEntity,
    val cancellationId: UUID?,
    val cancelledBy: UserEntity,
    val cancelledAt: LocalDate,
    val reason: CancellationReasonEntity,
    val isSpaceBooking: Boolean,
  )
}
