package uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1

import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.model.BookingKeyWorkerAssigned
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.model.BookingKeyWorkerAssignedEnvelope
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.model.EventType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.model.MoveOnCategory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.model.PersonArrived
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.model.PersonArrivedEnvelope
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.model.PersonDeparted
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.model.PersonDepartedDestination
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.model.PersonDepartedEnvelope
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.model.PersonNotArrived
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.model.PersonNotArrivedEnvelope
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.model.PersonReference
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.model.Premises
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.model.StaffMember
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.TimelineEvent
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApprovedPremisesEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.Cas1SpaceBookingEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.DepartureReasonEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.MoveOnCategoryEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.NonArrivalReasonEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.DomainEvent
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.PersonSummaryInfoResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.deliuscontext.CaseSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.results.CasResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.DomainEventService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.OffenderService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.StaffMemberService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.ApplicationTimelineTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.UrlTemplate
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.toLocalDateTime
import java.time.OffsetDateTime
import java.util.UUID

@Service
class Cas1SpaceBookingManagementDomainEventServiceConfig(
  @Value("\${url-templates.frontend.application}") val applicationUrlTemplate: UrlTemplate,
)

@Service
class Cas1SpaceBookingManagementDomainEventService(
  val domainEventService: DomainEventService,
  val offenderService: OffenderService,
  private val cas1SpaceBookingManagementConfig: Cas1SpaceBookingManagementDomainEventServiceConfig,
  private val applicationTimelineTransformer: ApplicationTimelineTransformer,
  private val staffMemberService: StaffMemberService,
) {

  fun arrivalRecorded(
    updatedCas1SpaceBooking: Cas1SpaceBookingEntity,
  ) {
    val domainEventId = UUID.randomUUID()

    val premises = mapApprovedPremisesEntityToPremises(updatedCas1SpaceBooking.premises)
    val offenderDetails = getOffenderForCrn(updatedCas1SpaceBooking.crn)
    val keyWorker = getStaffMemberDetails(updatedCas1SpaceBooking.keyWorkerStaffCode)
    val eventNumber = updatedCas1SpaceBooking.deliusEventNumber!!
    val applicationId = updatedCas1SpaceBooking.applicationIdForDomainEvent()
    val applicationSubmittedAt = updatedCas1SpaceBooking.applicationSubmittedOnForDomainEvent()

    val actualArrivalDate = updatedCas1SpaceBooking.actualArrivalDateTime!!

    domainEventService.savePersonArrivedEvent(
      emit = false,
      domainEvent = DomainEvent(
        id = domainEventId,
        applicationId = applicationId,
        crn = updatedCas1SpaceBooking.crn,
        nomsNumber = offenderDetails?.nomsId,
        occurredAt = actualArrivalDate,
        cas1SpaceBookingId = updatedCas1SpaceBooking.id,
        bookingId = null,
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
            arrivedAt = actualArrivalDate,
            expectedDepartureOn = updatedCas1SpaceBooking.expectedDepartureDate,
            notes = null,
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

    val application = updatedCas1SpaceBooking.application
    val premises = mapApprovedPremisesEntityToPremises(updatedCas1SpaceBooking.premises)
    val offenderDetails = getOffenderForCrn(updatedCas1SpaceBooking.crn)
    val staffUser = getStaffMemberDetails(user.deliusStaffCode)

    domainEventService.savePersonNotArrivedEvent(
      emit = false,
      domainEvent = DomainEvent(
        id = domainEventId,
        applicationId = application!!.id,
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
            applicationId = application.id,
            applicationUrl = cas1SpaceBookingManagementConfig.applicationUrlTemplate.resolve("id", application.id.toString()),
            bookingId = updatedCas1SpaceBooking.id,
            personReference = PersonReference(
              crn = updatedCas1SpaceBooking.crn,
              noms = offenderDetails?.nomsId ?: "Unknown NOMS Id",
            ),
            deliusEventNumber = application.eventNumber,
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

  fun departureRecorded(
    departedCas1SpaceBooking: Cas1SpaceBookingEntity,
    departureReason: DepartureReasonEntity,
    moveOnCategory: MoveOnCategoryEntity,
  ) {
    val domainEventId = UUID.randomUUID()

    val applicationId = departedCas1SpaceBooking.applicationIdForDomainEvent()
    val premises = mapApprovedPremisesEntityToPremises(departedCas1SpaceBooking.premises)
    val offenderDetails = getOffenderForCrn(departedCas1SpaceBooking.crn)
    val keyWorker = getStaffMemberDetails(departedCas1SpaceBooking.keyWorkerStaffCode)
    val eventNumber = departedCas1SpaceBooking.deliusEventNumber!!

    val actualDepartureDate = departedCas1SpaceBooking.actualDepartureDateTime!!

    domainEventService.savePersonDepartedEvent(
      emit = false,
      domainEvent = DomainEvent(
        id = domainEventId,
        applicationId = applicationId,
        crn = departedCas1SpaceBooking.crn,
        nomsNumber = offenderDetails?.nomsId,
        occurredAt = actualDepartureDate,
        cas1SpaceBookingId = departedCas1SpaceBooking.id,
        bookingId = null,
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
            departedAt = actualDepartureDate,
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
    assignedKeyWorkerName: String,
    previousKeyWorkerName: String?,
  ) {
    val domainEventId = UUID.randomUUID()
    val eventOccurredAt = OffsetDateTime.now().toInstant()

    val applicationId = updatedCas1SpaceBooking.applicationIdForDomainEvent()
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

  @SuppressWarnings("TooGenericExceptionThrown")
  private fun getStaffMemberDetails(staffCode: String?): StaffMember? {
    return staffCode?.let {
      when (val result = staffMemberService.getStaffDetailByCode(staffCode)) {
        // the blunt error handling is temporary, it will be resolved as part of domain event structure updates
        is CasResult.Error -> throw RuntimeException("Could not load keyworker $staffCode")
        is CasResult.Success -> result.value.toStaffMember()
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

  fun Cas1SpaceBookingEntity.applicationIdForDomainEvent() = application?.id ?: offlineApplication!!.id
  fun Cas1SpaceBookingEntity.applicationSubmittedOnForDomainEvent() = application?.submittedAt ?: offlineApplication!!.createdAt
}
