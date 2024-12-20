package uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.ArrayNode
import com.fasterxml.jackson.databind.node.ObjectNode
import com.fasterxml.jackson.databind.node.TextNode
import com.fasterxml.jackson.module.kotlin.convertValue
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model.StaffMember
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.DomainEventEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.UserService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.toLocalDate

/**
This is tested by the [DomainEventTest] integration test
 */
@Service
class Cas1DomainEventMigrationService(
  val objectMapper: ObjectMapper,
  val userService: UserService,
) {

  fun bookingCancelledJson(entity: DomainEventEntity) =
    when (entity.schemaVersion) {
      2 -> entity.data
      else -> bookingCancelledV1JsonToV2Json(entity)
    }

  fun personArrivedJson(entity: DomainEventEntity) =
    when (entity.schemaVersion) {
      2 -> entity.data
      else -> personArrivedDepartedV1JsonToV2Json(entity)
    }

  fun personDepartedJson(entity: DomainEventEntity) =
    when (entity.schemaVersion) {
      2 -> entity.data
      else -> personArrivedDepartedV1JsonToV2Json(entity)
    }

  private fun bookingCancelledV1JsonToV2Json(domainEventEntity: DomainEventEntity): String {
    return modifyEventDetails(domainEventEntity) { eventDetailsNode ->
      val cancellationRecordedAt = objectMapper.convertValue(domainEventEntity.occurredAt, TextNode::class.java)
      eventDetailsNode.set<TextNode>("cancellationRecordedAt", cancellationRecordedAt)

      val cancelledAt =
        objectMapper.convertValue(eventDetailsNode["cancelledAt"], java.time.Instant::class.java)
      val cancelledAtDate = objectMapper.convertValue(cancelledAt.toLocalDate(), TextNode::class.java)
      eventDetailsNode.set<ArrayNode>("cancelledAtDate", cancelledAtDate)
    }
  }

  private fun personArrivedDepartedV1JsonToV2Json(domainEventEntity: DomainEventEntity) = modifyEventDetails(domainEventEntity) { eventDetailsNode ->
    val triggeredByUser = domainEventEntity.triggeredByUserId?.let {
      userService.findByIdOrNull(it)
    }

      /*
      The primary purpose of this migration is to make v1 domain events schema valid.
      `recordedBy` is not used to render the timeline, and these old domain events
      will not be consumed externally, so the imperfect nature of the back-fill is acceptable
       */
    eventDetailsNode.set<ObjectNode>(
      "recordedBy",
      objectMapper.convertValue(
        StaffMember(
          staffCode = triggeredByUser?.deliusStaffCode ?: "unknown",
          forenames = triggeredByUser?.name ?: "unknown",
          surname = "unknown",
          username = triggeredByUser?.deliusUsername ?: "unknown",
        ),
      ),
    )
  }

  private fun modifyEventDetails(
    domainEventEntity: DomainEventEntity,
    mutator: (eventDetailsNode: ObjectNode) -> Unit,
  ): String {
    val dataModel: JsonNode = objectMapper.readTree(domainEventEntity.data)
    val eventDetails = dataModel["eventDetails"] as ObjectNode
    mutator(eventDetails)
    return objectMapper.writeValueAsString(dataModel)
  }
}
