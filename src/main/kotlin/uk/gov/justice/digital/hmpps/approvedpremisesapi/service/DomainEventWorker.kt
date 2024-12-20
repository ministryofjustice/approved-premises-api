package uk.gov.justice.digital.hmpps.approvedpremisesapi.service

import com.fasterxml.jackson.databind.ObjectMapper
import kotlinx.coroutines.runBlocking
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Primary
import org.springframework.retry.support.RetryTemplate
import org.springframework.stereotype.Component
import software.amazon.awssdk.services.sns.model.MessageAttributeValue
import software.amazon.awssdk.services.sns.model.PublishRequest
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.domainevent.SnsEvent
import uk.gov.justice.hmpps.sqs.HmppsQueueService
import uk.gov.justice.hmpps.sqs.HmppsTopic
import uk.gov.justice.hmpps.sqs.MissingTopicException
import java.util.UUID

interface DomainEventWorkerInterface {
  fun emitEvent(snsEvent: SnsEvent, domainEventId: UUID)
}

@Component
@Primary
class ConfiguredDomainEventWorker(
  private val hmppsQueueService: HmppsQueueService,
  private val objectMapper: ObjectMapper,
  @Value("\${domain-events.cas1.async-save-enabled}") private val asyncSaveEnabled: Boolean,
) : DomainEventWorkerInterface {
  val domainTopic by lazy {
    hmppsQueueService.findByTopicId("domainevents")
      ?: throw MissingTopicException("domainevents not found")
  }

  override fun emitEvent(snsEvent: SnsEvent, domainEventId: UUID) {
    val worker = if (this.asyncSaveEnabled) {
      AsyncDomainEventWorker(this.domainTopic, this.objectMapper)
    } else {
      SyncDomainEventWorker(this.domainTopic, this.objectMapper)
    }

    worker.emitEvent(snsEvent, domainEventId)
  }
}

class SyncDomainEventWorker(
  val domainTopic: HmppsTopic,
  val objectMapper: ObjectMapper,
) : DomainEventWorkerInterface {
  private val log = LoggerFactory.getLogger(this::class.java)

  override fun emitEvent(snsEvent: SnsEvent, domainEventId: UUID) {
    val publishRequest =
      PublishRequest.builder()
        .topicArn(domainTopic.arn)
        .message(objectMapper.writeValueAsString(snsEvent))
        .messageAttributes(
          mapOf(
            "eventType" to MessageAttributeValue.builder().dataType("String").stringValue(snsEvent.eventType).build(),
          ),
        ).build()
    val publishResult = domainTopic.snsClient.publish(publishRequest).get()

    log.info(
      "Emitted SNS event (Message Id: ${publishResult.messageId()}, " +
        "Sequence Id: ${publishResult.sequenceNumber()}) for Domain Event:" +
        " $domainEventId of type: ${snsEvent.eventType}",
    )
  }
}

class AsyncDomainEventWorker(
  val domainTopic: HmppsTopic,
  val objectMapper: ObjectMapper,
) : DomainEventWorkerInterface {
  private val log = LoggerFactory.getLogger(this::class.java)

  var retryTemplate: RetryTemplate = RetryTemplate.builder()
    .exponentialBackoff(INITIAL_INTERVAL, 2.0, MAX_INTERVAL)
    .maxAttempts(MAX_ATTEMPTS_RETRY_EXPONENTIAL)
    .build()

  override fun emitEvent(snsEvent: SnsEvent, domainEventId: UUID) {
    val publishRequest =
      PublishRequest.builder()
        .topicArn(domainTopic.arn)
        .message(objectMapper.writeValueAsString(snsEvent))
        .messageAttributes(
          mapOf(
            "eventType" to MessageAttributeValue.builder().dataType("String").stringValue(snsEvent.eventType).build(),
          ),
        ).build()
    runBlocking {
      publishSnsEventWithRetry(publishRequest, snsEvent, retryTemplate, domainEventId)
    }
  }

  fun publishSnsEventWithRetry(
    publishRequest: PublishRequest,
    snsEvent: SnsEvent,
    retryTemplate: RetryTemplate,
    domainEventId: UUID,
  ) {
    retryTemplate.execute<Any?, RuntimeException> {
      val publishResult = domainTopic.snsClient.publish(publishRequest).get()
      log.info(
        "Emitted SNS event (Message Id: ${publishResult.messageId()}, " +
          "Sequence Id: ${publishResult.sequenceNumber()}) for Domain Event: " +
          "$domainEventId of type: ${snsEvent.eventType}",
      )
    }
  }

  companion object {
    /*
     * The MAX_ATTEMPTS_RETRY_EXPONENTIAL of 166 is worked out like this:
     * the multiplier is the default of two so we have
     * 1 2 4 8 16 32 64 128 256 512 which gives us around 25 minutes. After that our max interval is ten minutes
     * so we have four of them to give us the first hour which is 14 attempts. We follow that with 23 * 6 for the rest
     * of the fist day which gives us 14 + 138 = 152. We want to retry for 7 days so we have another 6 * 6 * 24 = 864.
     * Total is 864 + 152 = 1016
     *
     * MAX_ATTEMPTS_RETRY is used in the tests with a FixedBackOffPolicy
     * */
    const val MAX_ATTEMPTS_RETRY_EXPONENTIAL = 1016
    const val MAX_ATTEMPTS_RETRY = 4
    const val BACK_OFF_PERIOD = 1000.toLong()
    const val INITIAL_INTERVAL = 1000.toLong()
    const val MAX_INTERVAL = (1000 * 60 * 10).toLong()
  }
}
