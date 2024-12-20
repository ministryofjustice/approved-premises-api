package uk.gov.justice.digital.hmpps.approvedpremisesapi.util

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.ObjectNode
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model.ApplicationAssessedEnvelope
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model.ApplicationExpiredEnvelope
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model.ApplicationSubmittedEnvelope
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model.ApplicationWithdrawnEnvelope
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model.AssessmentAllocatedEnvelope
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model.AssessmentAppealedEnvelope
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model.BookingCancelledEnvelope
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model.BookingChangedEnvelope
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model.BookingKeyWorkerAssignedEnvelope
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model.BookingMadeEnvelope
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model.BookingNotMadeEnvelope
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model.EventType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model.FurtherInformationRequestedEnvelope
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model.MatchRequestWithdrawnEnvelope
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model.PersonArrivedEnvelope
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model.PersonDepartedEnvelope
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model.PersonNotArrivedEnvelope
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model.PlacementApplicationAllocatedEnvelope
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model.PlacementApplicationWithdrawnEnvelope
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model.RequestForPlacementAssessedEnvelope
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model.RequestForPlacementCreatedEnvelope
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.events.ApplicationAssessedFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.events.ApplicationExpiredFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.events.ApplicationSubmittedFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.events.ApplicationWithdrawnFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.events.AssessmentAllocatedFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.events.AssessmentAppealedFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.events.BookingCancelledFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.events.BookingChangedFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.events.BookingKeyWorkerAssignedFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.events.BookingMadeFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.events.BookingNotMadeFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.events.FurtherInformationRequestedFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.events.MatchRequestWithdrawnFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.events.PersonArrivedFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.events.PersonDepartedFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.events.PersonNotArrivedFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.events.PlacementApplicationAllocatedFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.events.PlacementApplicationWithdrawnFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.events.RequestForPlacementAssessedFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.events.RequestForPlacementCreatedFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.DomainEventSchemaVersion
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.DomainEventType
import java.time.Instant
import java.util.UUID

class Cas1DomainEventsFactory(val objectMapper: ObjectMapper) {

  fun createEnvelopeLatestVersion(
    type: DomainEventType,
    requestId: UUID = UUID.randomUUID(),
    occurredAt: Instant = Instant.now(),
  ): DomainEventEnvelopeAndPersistedJson {
    val envelope = createEnvelopeForLatestSchemaVersion(
      type,
      requestId,
      occurredAt = occurredAt,
    )

    return DomainEventEnvelopeAndPersistedJson(
      envelope = envelope,
      persistedJson = objectMapper.writeValueAsString(envelope),
      schemaVersion = type.schemaVersions.last(),
    )
  }

  @SuppressWarnings("CyclomaticComplexMethod", "TooGenericExceptionThrown")
  fun createEnvelopeForLatestSchemaVersion(
    type: DomainEventType,
    requestId: UUID = UUID.randomUUID(),
    occurredAt: Instant = Instant.now(),
  ): Any {
    val id = UUID.randomUUID()
    val eventType = EventType.entries.find { it.value == type.typeName } ?: throw RuntimeException("Cannot find EventType for $type")

    return when (type) {
      DomainEventType.APPROVED_PREMISES_APPLICATION_SUBMITTED -> ApplicationSubmittedEnvelope(
        id = id,
        timestamp = occurredAt,
        eventType = eventType,
        eventDetails = ApplicationSubmittedFactory().produce(),
      )
      DomainEventType.APPROVED_PREMISES_APPLICATION_ASSESSED -> ApplicationAssessedEnvelope(
        id = id,
        timestamp = occurredAt,
        eventType = eventType,
        eventDetails = ApplicationAssessedFactory().produce(),
      )
      DomainEventType.APPROVED_PREMISES_BOOKING_MADE -> BookingMadeEnvelope(
        id = id,
        timestamp = occurredAt,
        eventType = eventType,
        eventDetails = BookingMadeFactory().produce(),
      )
      DomainEventType.APPROVED_PREMISES_PERSON_ARRIVED -> PersonArrivedEnvelope(
        id = id,
        timestamp = occurredAt,
        eventType = eventType,
        eventDetails = PersonArrivedFactory().produce(),
      )
      DomainEventType.APPROVED_PREMISES_PERSON_NOT_ARRIVED -> PersonNotArrivedEnvelope(
        id = id,
        timestamp = occurredAt,
        eventType = eventType,
        eventDetails = PersonNotArrivedFactory().produce(),
      )
      DomainEventType.APPROVED_PREMISES_PERSON_DEPARTED -> PersonDepartedEnvelope(
        id = id,
        timestamp = occurredAt,
        eventType = eventType,
        eventDetails = PersonDepartedFactory().produce(),
      )
      DomainEventType.APPROVED_PREMISES_BOOKING_NOT_MADE -> BookingNotMadeEnvelope(
        id = id,
        timestamp = occurredAt,
        eventType = eventType,
        eventDetails = BookingNotMadeFactory().produce(),
      )
      DomainEventType.APPROVED_PREMISES_BOOKING_CANCELLED -> {
        return BookingCancelledEnvelope(
          id = id,
          timestamp = occurredAt,
          eventType = eventType,
          eventDetails = BookingCancelledFactory()
            .withCancellationRecordedAt(occurredAt)
            .produce(),
        )
      }
      DomainEventType.APPROVED_PREMISES_BOOKING_CHANGED -> BookingChangedEnvelope(
        id = id,
        timestamp = occurredAt,
        eventType = eventType,
        eventDetails = BookingChangedFactory().produce(),
      )
      DomainEventType.APPROVED_PREMISES_BOOKING_KEYWORKER_ASSIGNED -> BookingKeyWorkerAssignedEnvelope(
        id = id,
        timestamp = occurredAt,
        eventType = eventType,
        eventDetails = BookingKeyWorkerAssignedFactory().produce(),
      )
      DomainEventType.APPROVED_PREMISES_APPLICATION_WITHDRAWN -> ApplicationWithdrawnEnvelope(
        id = id,
        timestamp = occurredAt,
        eventType = eventType,
        eventDetails = ApplicationWithdrawnFactory().produce(),
      )
      DomainEventType.APPROVED_PREMISES_ASSESSMENT_APPEALED -> AssessmentAppealedEnvelope(
        id = id,
        timestamp = occurredAt,
        eventType = eventType,
        eventDetails = AssessmentAppealedFactory().produce(),
      )
      DomainEventType.APPROVED_PREMISES_ASSESSMENT_ALLOCATED -> AssessmentAllocatedEnvelope(
        id = id,
        timestamp = occurredAt,
        eventType = eventType,
        eventDetails = AssessmentAllocatedFactory().produce(),
      )
      DomainEventType.APPROVED_PREMISES_PLACEMENT_APPLICATION_WITHDRAWN -> PlacementApplicationWithdrawnEnvelope(
        id = id,
        timestamp = occurredAt,
        eventType = eventType,
        eventDetails = PlacementApplicationWithdrawnFactory().produce(),
      )
      DomainEventType.APPROVED_PREMISES_PLACEMENT_APPLICATION_ALLOCATED -> PlacementApplicationAllocatedEnvelope(
        id = id,
        timestamp = occurredAt,
        eventType = eventType,
        eventDetails = PlacementApplicationAllocatedFactory().produce(),
      )
      DomainEventType.APPROVED_PREMISES_MATCH_REQUEST_WITHDRAWN -> MatchRequestWithdrawnEnvelope(
        id = id,
        timestamp = occurredAt,
        eventType = eventType,
        eventDetails = MatchRequestWithdrawnFactory().produce(),
      )
      DomainEventType.APPROVED_PREMISES_REQUEST_FOR_PLACEMENT_CREATED -> RequestForPlacementCreatedEnvelope(
        id = id,
        timestamp = occurredAt,
        eventType = eventType,
        eventDetails = RequestForPlacementCreatedFactory().produce(),
      )
      DomainEventType.APPROVED_PREMISES_REQUEST_FOR_PLACEMENT_ASSESSED -> RequestForPlacementAssessedEnvelope(
        id = id,
        timestamp = occurredAt,
        eventType = eventType,
        eventDetails = RequestForPlacementAssessedFactory().produce(),
      )
      DomainEventType.APPROVED_PREMISES_ASSESSMENT_INFO_REQUESTED -> FurtherInformationRequestedEnvelope(
        id = id,
        timestamp = occurredAt,
        eventType = eventType,
        eventDetails = FurtherInformationRequestedFactory()
          .withRequestId(requestId)
          .produce(),
      )
      DomainEventType.APPROVED_PREMISES_APPLICATION_EXPIRED -> ApplicationExpiredEnvelope(
        id = id,
        timestamp = occurredAt,
        eventType = eventType,
        eventDetails = ApplicationExpiredFactory().produce(),
      )
      else -> throw RuntimeException("Domain event type $type not supported")
    }
  }

  fun removeEventDetails(json: String, fields: List<String>): String {
    val dataModel: JsonNode = objectMapper.readTree(json)
    fields.forEach {
      (dataModel["eventDetails"] as ObjectNode).remove(it)
    }
    return objectMapper.writeValueAsString(dataModel)
  }
}

data class DomainEventEnvelopeAndPersistedJson(
  val envelope: Any,
  val persistedJson: String,
  val schemaVersion: DomainEventSchemaVersion,
)
