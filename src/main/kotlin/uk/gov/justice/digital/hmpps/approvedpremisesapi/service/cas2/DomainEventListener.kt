package uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas2

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import io.awspring.cloud.sqs.annotation.SqsListener
import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.HmppsDomainEvent
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.SQSMessage

@ConditionalOnProperty(prefix = "feature-flags", name = ["cas2-sqs-listener-enabled"], havingValue = "true")
@Service
class DomainEventListener(
  private val pomAllocationService: PomAllocationService,
  private val objectMapper: ObjectMapper,
) {

  private val log = LoggerFactory.getLogger(this::class.java)

  @SqsListener("castwodomaineventsqueue", factory = "hmppsQueueContainerFactoryProxy")
  fun processMessage(msg: String) {
    val (message) = objectMapper.readValue<SQSMessage>(msg)
    val hmppsDomainEvent = objectMapper.readValue<HmppsDomainEvent>(message)
    val prisonNumber = hmppsDomainEvent.personReference.findNomsNumber()
    log.info("Request received to process domain event type ${hmppsDomainEvent.eventType} for prisoner number $prisonNumber")
    handleMessage(hmppsDomainEvent)
  }

  private fun handleMessage(message: HmppsDomainEvent) {
    when (message.eventType) {
      "offender-management.allocation-changed" -> pomAllocationService.handlePomAllocationChangedMessage(
        message,
      )

      else -> log.error("Unknown event type: ${message.eventType}")
    }
  }

}
