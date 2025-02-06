package uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.health

import com.ninjasquad.springmockk.SpykBean
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.IntegrationTestBase
import uk.gov.justice.hmpps.sqs.HmppsSqsProperties
import uk.gov.justice.hmpps.sqs.MissingQueueException

class QueueHealthCheckTest : IntegrationTestBase() {

  @SpykBean
  private lateinit var hmppsSqsPropertiesSpy: HmppsSqsProperties

  fun HmppsSqsProperties.cas2DomainEventsQueueConfig() = queues["castwodomaineventsqueue"]
    ?: throw MissingQueueException("castwodomaineventsqueue has not been loaded from configuration properties")

  @Test
  fun `CAS 2 Domain Events queue health ok`() {
    webTestClient.get().uri("/health").exchange().expectStatus().isOk.expectBody().jsonPath("status").isEqualTo("UP")
      .jsonPath("components.castwodomaineventsqueue-health.status").isEqualTo("UP")
      .jsonPath("components.castwodomaineventsqueue-health.details.queueName")
      .isEqualTo(hmppsSqsPropertiesSpy.cas2DomainEventsQueueConfig().queueName)
      .jsonPath("components.castwodomaineventsqueue-health.details.messagesOnQueue").isEqualTo(0)
      .jsonPath("components.castwodomaineventsqueue-health.details.messagesInFlight").isEqualTo(0)
      .jsonPath("components.castwodomaineventsqueue-health.details.dlqName")
      .isEqualTo(hmppsSqsPropertiesSpy.cas2DomainEventsQueueConfig().dlqName)
      .jsonPath("components.castwodomaineventsqueue-health.details.dlqStatus").isEqualTo("UP")
      .jsonPath("components.castwodomaineventsqueue-health.details.messagesOnDlq").isEqualTo(0)
  }
}
