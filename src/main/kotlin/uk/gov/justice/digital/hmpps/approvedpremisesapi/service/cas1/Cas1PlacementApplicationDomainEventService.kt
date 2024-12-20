package uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1

import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model.DatePeriod
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model.EventType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model.PersonReference
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model.PlacementApplicationAllocated
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model.PlacementApplicationAllocatedEnvelope
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model.PlacementApplicationWithdrawn
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model.PlacementApplicationWithdrawnEnvelope
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model.RequestForPlacementAssessed
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model.RequestForPlacementAssessedEnvelope
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model.RequestForPlacementCreated
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model.RequestForPlacementCreatedEnvelope
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model.RequestForPlacementType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.PlacementApplicationDecisionEnvelope
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.ApDeliusContextApiClient
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.ClientResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.MetaDataName
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PlacementApplicationDecision
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PlacementApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PlacementType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.DomainEvent
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.DomainEventTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.UrlTemplate
import java.time.Instant
import java.util.UUID

@Component
class Cas1PlacementApplicationDomainEventService(
  private val domainEventService: Cas1DomainEventService,
  private val domainEventTransformer: DomainEventTransformer,
  private val apDeliusContextApiClient: ApDeliusContextApiClient,
  @Value("\${url-templates.frontend.application}") private val applicationUrlTemplate: UrlTemplate,
) {

  fun placementApplicationSubmitted(
    placementApplication: PlacementApplicationEntity,
    username: String,
  ) {
    checkNotNull(placementApplication.placementType)
    require(placementApplication.placementDates.size == 1)

    val domainEventId = UUID.randomUUID()
    val eventOccurredAt = Instant.now()
    val application = placementApplication.application
    val dates = placementApplication.placementDates[0]

    val placementType = when (placementApplication.placementType!!) {
      PlacementType.ROTL -> RequestForPlacementType.rotl
      PlacementType.RELEASE_FOLLOWING_DECISION -> RequestForPlacementType.releaseFollowingDecisions
      PlacementType.ADDITIONAL_PLACEMENT -> RequestForPlacementType.additionalPlacement
    }

    val staffDetails = when (val staffDetailsResult = apDeliusContextApiClient.getStaffDetail(username)) {
      is ClientResult.Success -> staffDetailsResult.body
      is ClientResult.Failure -> staffDetailsResult.throwException()
    }

    val eventDetails = RequestForPlacementCreated(
      applicationId = application.id,
      applicationUrl = applicationUrlTemplate.resolve("id", application.id.toString()),
      requestForPlacementId = placementApplication.id,
      personReference = PersonReference(
        crn = application.crn,
        noms = application.nomsNumber ?: "Unknown NOMS Number",
      ),
      deliusEventNumber = application.eventNumber,
      createdAt = eventOccurredAt,
      createdBy = staffDetails.toStaffMember(),
      expectedArrival = dates.expectedArrival,
      duration = dates.duration,
      requestForPlacementType = placementType,
    )

    domainEventService.saveRequestForPlacementCreatedEvent(
      DomainEvent(
        id = domainEventId,
        applicationId = application.id,
        crn = application.crn,
        nomsNumber = application.nomsNumber,
        occurredAt = eventOccurredAt,
        data = RequestForPlacementCreatedEnvelope(
          id = domainEventId,
          timestamp = eventOccurredAt,
          eventType = EventType.requestForPlacementCreated,
          eventDetails = eventDetails,
        ),
      ),
      emit = false,
    )
  }

  fun placementApplicationWithdrawn(placementApplication: PlacementApplicationEntity, withdrawalContext: WithdrawalContext) {
    require(withdrawalContext.withdrawalTriggeredBy is WithdrawalTriggeredByUser) { "Only withdrawals triggered by users are supported" }
    val user = withdrawalContext.withdrawalTriggeredBy.user

    val domainEventId = UUID.randomUUID()
    val eventOccurredAt = Instant.now()
    val application = placementApplication.application

    val eventDetails = PlacementApplicationWithdrawn(
      applicationId = application.id,
      applicationUrl = applicationUrlTemplate.resolve("id", application.id.toString()),
      placementApplicationId = placementApplication.id,
      personReference = PersonReference(
        crn = application.crn,
        noms = application.nomsNumber ?: "Unknown NOMS Number",
      ),
      deliusEventNumber = application.eventNumber,
      withdrawnAt = eventOccurredAt,
      withdrawnBy = domainEventTransformer.toWithdrawnBy(user),
      withdrawalReason = placementApplication.withdrawalReason!!.name,
      placementDates = placementApplication.placementDates.map {
        DatePeriod(
          it.expectedArrival,
          it.expectedDeparture(),
        )
      },
    )

    domainEventService.savePlacementApplicationWithdrawnEvent(
      DomainEvent(
        id = domainEventId,
        applicationId = application.id,
        crn = application.crn,
        nomsNumber = application.nomsNumber,
        occurredAt = eventOccurredAt,
        data = PlacementApplicationWithdrawnEnvelope(
          id = domainEventId,
          timestamp = eventOccurredAt,
          eventType = EventType.placementApplicationWithdrawn,
          eventDetails = eventDetails,
        ),
        metadata = mapOf(
          MetaDataName.CAS1_PLACEMENT_APPLICATION_ID to placementApplication.id.toString(),
        ),
      ),
    )
  }

  fun placementApplicationAllocated(placementApplication: PlacementApplicationEntity, allocatedByUser: UserEntity?) {
    val allocatedAt = requireNotNull(placementApplication.allocatedAt)
    val allocatedToUser = requireNotNull(placementApplication.allocatedToUser)

    val domainEventId = UUID.randomUUID()
    val eventOccurredAt = Instant.now()
    val application = placementApplication.application

    val placementApplicationAllocated = PlacementApplicationAllocated(
      applicationId = application.id,
      applicationUrl = applicationUrlTemplate.resolve("id", application.id.toString()),
      placementApplicationId = placementApplication.id,
      personReference = PersonReference(
        crn = application.crn,
        noms = application.nomsNumber ?: "Unknown NOMS Number",
      ),
      allocatedAt = allocatedAt.toInstant(),
      allocatedTo = domainEventTransformer.toStaffMember(allocatedToUser),
      allocatedBy = allocatedByUser?.let { domainEventTransformer.toStaffMember(it) },
      placementDates = placementApplication.placementDates.map {
        DatePeriod(
          it.expectedArrival,
          it.expectedDeparture(),
        )
      },
    )

    domainEventService.savePlacementApplicationAllocatedEvent(
      DomainEvent(
        id = domainEventId,
        applicationId = application.id,
        crn = application.crn,
        nomsNumber = application.nomsNumber,
        occurredAt = eventOccurredAt,
        data = PlacementApplicationAllocatedEnvelope(
          id = domainEventId,
          timestamp = eventOccurredAt,
          eventType = EventType.placementApplicationAllocated,
          eventDetails = placementApplicationAllocated,
        ),
      ),
    )
  }

  fun placementApplicationAssessed(
    placementApplication: PlacementApplicationEntity,
    assessedByUser: UserEntity,
    placementApplicationDecision: PlacementApplicationDecisionEnvelope,
  ) {
    val domainEventId = UUID.randomUUID()
    val eventOccurredAt = Instant.now()
    val application = placementApplication.application
    val dates = placementApplication.placementDates[0]

    val assessor = assessedByUser.let { domainEventTransformer.toStaffMember(it) }

    val assessmentDecision = when (placementApplication.decision) {
      PlacementApplicationDecision.ACCEPTED -> RequestForPlacementAssessed.Decision.accepted
      PlacementApplicationDecision.REJECTED -> RequestForPlacementAssessed.Decision.rejected
      PlacementApplicationDecision.WITHDRAW,
      PlacementApplicationDecision.WITHDRAWN_BY_PP,
      -> throw IllegalArgumentException("PlacementApplicationDecision '${placementApplication.decision}' has been deprecated")
      null -> throw IllegalArgumentException("PlacementApplicationDecision was null")
    }

    val requestPlacementApplicationAssessed = RequestForPlacementAssessed(
      applicationId = application.id,
      applicationUrl = applicationUrlTemplate.resolve("id", application.id.toString()),
      placementApplicationId = placementApplication.id,
      assessedBy = assessor,
      decision = assessmentDecision,
      decisionSummary = placementApplicationDecision.decisionSummary,
      expectedArrival = dates.expectedArrival,
      duration = dates.duration,
    )

    domainEventService.saveRequestForPlacementAssessedEvent(
      DomainEvent(
        id = domainEventId,
        applicationId = application.id,
        crn = application.crn,
        nomsNumber = application.nomsNumber,
        occurredAt = eventOccurredAt,
        data = RequestForPlacementAssessedEnvelope(
          id = domainEventId,
          timestamp = eventOccurredAt,
          eventType = EventType.requestForPlacementAssessed,
          eventDetails = requestPlacementApplicationAssessed,
        ),
        metadata = mapOf(
          MetaDataName.CAS1_PLACEMENT_APPLICATION_ID to placementApplication.id.toString(),
        ),
      ),
    )
  }
}
