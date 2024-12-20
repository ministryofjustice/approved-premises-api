package uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1

import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model.BookingKeyWorkerAssigned
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model.BookingKeyWorkerAssignedEnvelope
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model.EventType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model.MoveOnCategory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model.PersonArrived
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model.PersonArrivedEnvelope
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model.PersonDeparted
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model.PersonDepartedDestination
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model.PersonDepartedEnvelope
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model.PersonNotArrived
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model.PersonNotArrivedEnvelope
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model.PersonReference
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model.Premises
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model.StaffMember
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.TimelineEvent
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.ApDeliusContextApiClient
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.ClientResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApprovedPremisesEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.Cas1SpaceBookingEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.DepartureReasonEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.MoveOnCategoryEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.NonArrivalReasonEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.DomainEvent
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.PersonSummaryInfoResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.deliuscontext.CaseSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.OffenderService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.ApplicationTimelineTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.UrlTemplate
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.toInstant
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.toLocalDateTime
import java.time.LocalDate
import java.time.LocalTime
import java.time.OffsetDateTime
import java.util.UUID

@Service
class Cas1SpaceBookingManagementDomainEventServiceConfig(
  @Value("\${url-templates.frontend.application}") val applicationUrlTemplate: UrlTemplate,
)

@Service
class Cas1SpaceBookingManagementDomainEventService(
  val domainEventService: Cas1DomainEventService,
  val offenderService: OffenderService,
  private val cas1SpaceBookingManagementConfig: Cas1SpaceBookingManagementDomainEventServiceConfig,
  private val applicationTimelineTransformer: ApplicationTimelineTransformer,
  private val apDeliusContextApiClient: ApDeliusContextApiClient,
) {

  data class ArrivalInfo(
    val updatedCas1SpaceBooking: Cas1SpaceBookingEntity,
    val actualArrivalDate: LocalDate,
    val actualArrivalTime: LocalTime,
    val recordedBy: UserEntity,
  )

  fun arrivalRecorded(arrivalInfo: ArrivalInfo) {
    val updatedCas1SpaceBooking = arrivalInfo.updatedCas1SpaceBooking

    val domainEventId = UUID.randomUUID()

    val premises = mapApprovedPremisesEntityToPremises(updatedCas1SpaceBooking.premises)
    val offenderDetails = getOffenderForCrn(updatedCas1SpaceBooking.crn)
    val recordedByStaffDetails = getStaffDetailsByUsername(arrivalInfo.recordedBy.deliusUsername)
    val keyWorker = getStaffDetailsByStaffCode(updatedCas1SpaceBooking.keyWorkerStaffCode)
    val eventNumber = updatedCas1SpaceBooking.deliusEventNumber!!
    val applicationId = updatedCas1SpaceBooking.applicationFacade.id
    val applicationSubmittedAt = updatedCas1SpaceBooking.applicationFacade.submittedAt

    val actualArrivalDateTime = arrivalInfo.actualArrivalDate.atTime(arrivalInfo.actualArrivalTime).toInstant()

    domainEventService.savePersonArrivedEvent(
      domainEvent = DomainEvent(
        id = domainEventId,
        applicationId = applicationId,
        crn = updatedCas1SpaceBooking.crn,
        nomsNumber = offenderDetails?.nomsId,
        occurredAt = OffsetDateTime.now().toInstant(),
        cas1SpaceBookingId = updatedCas1SpaceBooking.id,
        bookingId = null,
        schemaVersion = 2,
        data = PersonArrivedEnvelope(
          id = domainEventId,
          timestamp = OffsetDateTime.now().toInstant(),
          eventType = EventType.personArrived,
          eventDetails = PersonArrived(
            applicationId = applicationId,
            applicationUrl = cas1SpaceBookingManagementConfig.applicationUrlTemplate.resolve("id", applicationId.toString()),
            bookingId = updatedCas1SpaceBooking.id,
            personReference = PersonReference(
              crn = updatedCas1SpaceBooking.crn,
              noms = offenderDetails?.nomsId ?: "Unknown NOMS Id",
            ),
            deliusEventNumber = eventNumber,
            premises = premises,
            applicationSubmittedOn = applicationSubmittedAt.toLocalDate(),
            keyWorker = keyWorker,
            arrivedAt = actualArrivalDateTime,
            expectedDepartureOn = updatedCas1SpaceBooking.expectedDepartureDate,
            notes = null,
            recordedBy = recordedByStaffDetails.toStaffMember(),
          ),
        ),
      ),
    )
  }

  fun nonArrivalRecorded(
    user: UserEntity,
    updatedCas1SpaceBooking: Cas1SpaceBookingEntity,
    reason: NonArrivalReasonEntity,
    notes: String?,
  ) {
    val domainEventId = UUID.randomUUID()
    val eventOccurredAt = OffsetDateTime.now().toInstant()

    val applicationId = updatedCas1SpaceBooking.applicationFacade.id
    val premises = mapApprovedPremisesEntityToPremises(updatedCas1SpaceBooking.premises)
    val offenderDetails = getOffenderForCrn(updatedCas1SpaceBooking.crn)
    val staffUser = getStaffDetailsByStaffCode(user.deliusStaffCode)
    val eventNumber = updatedCas1SpaceBooking.deliusEventNumber!!

    domainEventService.savePersonNotArrivedEvent(
      emit = false,
      domainEvent = DomainEvent(
        id = domainEventId,
        applicationId = applicationId,
        crn = updatedCas1SpaceBooking.crn,
        nomsNumber = offenderDetails?.nomsId,
        occurredAt = eventOccurredAt,
        cas1SpaceBookingId = updatedCas1SpaceBooking.id,
        bookingId = null,
        data = PersonNotArrivedEnvelope(
          id = domainEventId,
          timestamp = eventOccurredAt,
          eventType = EventType.personNotArrived,
          eventDetails = PersonNotArrived(
            applicationId = applicationId,
            applicationUrl = cas1SpaceBookingManagementConfig.applicationUrlTemplate.resolve("id", applicationId.toString()),
            bookingId = updatedCas1SpaceBooking.id,
            personReference = PersonReference(
              crn = updatedCas1SpaceBooking.crn,
              noms = offenderDetails?.nomsId ?: "Unknown NOMS Id",
            ),
            deliusEventNumber = eventNumber,
            premises = premises,
            expectedArrivalOn = updatedCas1SpaceBooking.canonicalArrivalDate,
            recordedBy = staffUser!!,
            notes = notes,
            reason = reason.name,
            legacyReasonCode = reason.legacyDeliusReasonCode!!,
          ),
        ),
      ),
    )
  }

  data class DepartureInfo(
    val spaceBooking: Cas1SpaceBookingEntity,
    val departureReason: DepartureReasonEntity,
    val moveOnCategory: MoveOnCategoryEntity,
    val actualDepartureDate: LocalDate,
    val actualDepartureTime: LocalTime,
    val recordedBy: UserEntity,
  )

  fun departureRecorded(departureInfo: DepartureInfo) {
    val departedCas1SpaceBooking = departureInfo.spaceBooking
    val departureReason = departureInfo.departureReason
    val moveOnCategory = departureInfo.moveOnCategory

    val domainEventId = UUID.randomUUID()

    val applicationId = departedCas1SpaceBooking.applicationFacade.id
    val premises = mapApprovedPremisesEntityToPremises(departedCas1SpaceBooking.premises)
    val offenderDetails = getOffenderForCrn(departedCas1SpaceBooking.crn)
    val recordedByStaffDetails = getStaffDetailsByUsername(departureInfo.recordedBy.deliusUsername)
    val keyWorker = getStaffDetailsByStaffCode(departedCas1SpaceBooking.keyWorkerStaffCode)
    val eventNumber = departedCas1SpaceBooking.deliusEventNumber!!

    val actualDepartureDateTime = departureInfo.actualDepartureDate.atTime(departureInfo.actualDepartureTime).toInstant()

    domainEventService.savePersonDepartedEvent(
      domainEvent = DomainEvent(
        id = domainEventId,
        applicationId = applicationId,
        crn = departedCas1SpaceBooking.crn,
        nomsNumber = offenderDetails?.nomsId,
        occurredAt = OffsetDateTime.now().toInstant(),
        cas1SpaceBookingId = departedCas1SpaceBooking.id,
        bookingId = null,
        schemaVersion = 2,
        data = PersonDepartedEnvelope(
          id = domainEventId,
          timestamp = OffsetDateTime.now().toInstant(),
          eventType = EventType.personDeparted,
          eventDetails = PersonDeparted(
            applicationId = applicationId,
            applicationUrl = cas1SpaceBookingManagementConfig.applicationUrlTemplate.resolve("id", applicationId.toString()),
            bookingId = departedCas1SpaceBooking.id,
            personReference = PersonReference(
              crn = departedCas1SpaceBooking.crn,
              noms = offenderDetails?.nomsId ?: "Unknown NOMS Id",
            ),
            deliusEventNumber = eventNumber,
            premises = premises,
            keyWorker = keyWorker!!,
            recordedBy = recordedByStaffDetails.toStaffMember(),
            departedAt = actualDepartureDateTime,
            reason = departureReason.name,
            legacyReasonCode = departureReason.legacyDeliusReasonCode!!,
            destination = PersonDepartedDestination(
              moveOnCategory = MoveOnCategory(
                description = moveOnCategory.name,
                legacyMoveOnCategoryCode = moveOnCategory.legacyDeliusCategoryCode!!,
                id = moveOnCategory.id,
              ),
            ),
          ),
        ),
      ),
    )
  }

  fun getTimeline(bookingId: UUID): List<TimelineEvent> {
    val domainEvents = domainEventService.getAllDomainEventsForSpaceBooking(bookingId)
    return domainEvents.map {
      applicationTimelineTransformer.transformDomainEventSummaryToTimelineEvent(it)
    }
  }

  fun keyWorkerAssigned(
    updatedCas1SpaceBooking: Cas1SpaceBookingEntity,
    assignedKeyWorker: StaffMember,
    assignedKeyWorkerName: String,
    previousKeyWorkerName: String?,
  ) {
    val domainEventId = UUID.randomUUID()
    val eventOccurredAt = OffsetDateTime.now().toInstant()

    val applicationId = updatedCas1SpaceBooking.applicationFacade.id
    val premises = mapApprovedPremisesEntityToPremises(updatedCas1SpaceBooking.premises)
    val offenderDetails = getOffenderForCrn(updatedCas1SpaceBooking.crn)
    val eventNumber = updatedCas1SpaceBooking.deliusEventNumber!!

    domainEventService.saveKeyWorkerAssignedEvent(
      emit = false,
      domainEvent = DomainEvent(
        id = domainEventId,
        applicationId = applicationId,
        crn = updatedCas1SpaceBooking.crn,
        nomsNumber = offenderDetails?.nomsId,
        occurredAt = eventOccurredAt,
        cas1SpaceBookingId = updatedCas1SpaceBooking.id,
        bookingId = null,
        data = BookingKeyWorkerAssignedEnvelope(
          id = domainEventId,
          timestamp = eventOccurredAt,
          eventType = EventType.bookingKeyWorkerAssigned,
          eventDetails = BookingKeyWorkerAssigned(
            applicationId = applicationId,
            applicationUrl = cas1SpaceBookingManagementConfig.applicationUrlTemplate.resolve("id", applicationId.toString()),
            bookingId = updatedCas1SpaceBooking.id,
            keyWorker = assignedKeyWorker,
            personReference = PersonReference(
              crn = updatedCas1SpaceBooking.crn,
              noms = offenderDetails?.nomsId ?: "Unknown NOMS Id",
            ),
            deliusEventNumber = eventNumber,
            premises = premises,
            previousKeyWorkerName = previousKeyWorkerName,
            assignedKeyWorkerName = assignedKeyWorkerName,
            arrivalDate = updatedCas1SpaceBooking.canonicalArrivalDate.toLocalDateTime().toLocalDate(),
            departureDate = updatedCas1SpaceBooking.canonicalDepartureDate.toLocalDateTime().toLocalDate(),
          ),
        ),
      ),
    )
  }

  private fun getStaffDetailsByUsername(deliusUsername: String) =
    when (val staffDetailsResult = apDeliusContextApiClient.getStaffDetail(deliusUsername)) {
      is ClientResult.Success -> staffDetailsResult.body
      is ClientResult.Failure -> staffDetailsResult.throwException()
    }

  private fun getStaffDetailsByStaffCode(staffCode: String?): StaffMember? {
    return staffCode?.let {
      when (val staffDetailResponse = apDeliusContextApiClient.getStaffDetailByStaffCode(staffCode)) {
        is ClientResult.Success -> staffDetailResponse.body.toStaffMember()
        is ClientResult.Failure -> staffDetailResponse.throwException()
      }
    }
  }

  private fun getOffenderForCrn(offenderCrn: String): CaseSummary? {
    val offenderDetails =
      when (
        val offenderDetailsResult =
          offenderService.getPersonSummaryInfoResults(
            setOf(offenderCrn),
            OffenderService.LimitedAccessStrategy.IgnoreLimitedAccess,
          )
            .firstOrNull()
      ) {
        is PersonSummaryInfoResult.Success.Full -> offenderDetailsResult.summary
        else -> null
      }
    return offenderDetails
  }

  private fun mapApprovedPremisesEntityToPremises(aPEntity: ApprovedPremisesEntity) =
    Premises(
      id = aPEntity.id,
      name = aPEntity.name,
      apCode = aPEntity.apCode,
      legacyApCode = aPEntity.qCode,
      localAuthorityAreaName = aPEntity.localAuthorityArea!!.name,
    )
}
