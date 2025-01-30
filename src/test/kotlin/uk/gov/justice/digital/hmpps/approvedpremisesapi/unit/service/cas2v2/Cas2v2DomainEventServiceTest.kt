package uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.service.cas2v2

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import software.amazon.awssdk.services.sns.model.PublishRequest
import software.amazon.awssdk.services.sns.model.PublishResponse
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas2.model.Cas2ApplicationSubmittedEvent
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas2.model.EventType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.config.DomainEventUrlConfig
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.events.cas2.Cas2ApplicationSubmittedEventDetailsFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.Cas2ApplicationRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.DomainEventEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.DomainEventRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.DomainEventType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.DomainEvent
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.domainevent.SnsEvent
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas2.DomainEventService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.util.ObjectMapperFactory
import uk.gov.justice.hmpps.sqs.HmppsQueueService
import uk.gov.justice.hmpps.sqs.HmppsTopic
import java.time.OffsetDateTime
import java.util.UUID
import java.util.concurrent.CompletableFuture

@SuppressWarnings("CyclomaticComplexMethod")
class Cas2v2DomainEventServiceTest {

  private val domainEventRepositoryMock = mockk<DomainEventRepository>()
  private val cas2ApplicationRepositoryMock = mockk<Cas2ApplicationRepository>()

  private val hmppsQueueServiceMock = mockk<HmppsQueueService>()
  private val mockDomainEventUrlConfig = mockk<DomainEventUrlConfig>()

  private val objectMapper = ObjectMapperFactory.createRuntimeLikeObjectMapper()

  private val detailUrl = "http://example.com/123"

  private val domainEventService = DomainEventService(
    objectMapper = objectMapper,
    domainEventRepository = domainEventRepositoryMock,
    hmppsQueueService = hmppsQueueServiceMock,
    emitDomainEventsEnabled = true,
    mockDomainEventUrlConfig,
    cas2ApplicationRepositoryMock
  )

  @BeforeEach
  fun setup() {
    every { mockDomainEventUrlConfig.getUrlForDomainEventId(any(), any()) } returns detailUrl
  }

  @Nested
  inner class GetCas2ApplicationSubmittedDomainEvent {
    @Test
    fun `persists event with cas2v, emits event to SNS`() {
      val id = UUID.fromString("c3b98c67-065a-408d-abea-a252f1d70981")
      val applicationId = UUID.fromString("a831ead2-31ae-4907-8e1c-cad74cb9667b")
      val occurredAt = OffsetDateTime.parse("2023-02-01T14:03:00+00:00")
      val crn = "CRN"
      val nomsNumber = "theNomsNumber"

      every { domainEventRepositoryMock.save(any()) } answers { it.invocation.args[0] as DomainEventEntity }

      val mockHmppsTopic = mockk<HmppsTopic>()

      every { hmppsQueueServiceMock.findByTopicId("domainevents") } returns mockHmppsTopic

      val domainEventToSave = DomainEvent(
        id = id,
        applicationId = applicationId,
        crn = crn,
        nomsNumber = nomsNumber,
        occurredAt = occurredAt.toInstant(),
        data = Cas2ApplicationSubmittedEvent(
          id = id,
          timestamp = occurredAt.toInstant(),
          eventType = EventType.applicationSubmitted,
          eventDetails = Cas2ApplicationSubmittedEventDetailsFactory().produce(),
        ),
      )

      every { mockHmppsTopic.arn } returns "arn:aws:sns:eu-west-2:000000000000:domain-events"
      every { mockHmppsTopic.snsClient.publish(any<PublishRequest>()) } returns CompletableFuture.completedFuture(
        PublishResponse.builder().build(),
      )

      domainEventService.saveCas2ApplicationSubmittedDomainEvent(domainEventToSave)

      verify(exactly = 1) {
        domainEventRepositoryMock.save(
          match {
            it.id == domainEventToSave.id &&
              it.type == DomainEventType.CAS2_APPLICATION_SUBMITTED &&
              it.crn == domainEventToSave.crn &&
              it.nomsNumber == domainEventToSave.nomsNumber &&
              it.occurredAt.toInstant() == domainEventToSave.occurredAt &&
              it.data == objectMapper.writeValueAsString(domainEventToSave.data) &&
              it.triggeredByUserId == null
          },
        )
      }

      verify(exactly = 1) {
        mockHmppsTopic.snsClient.publish(
          match<PublishRequest> {
            val deserializedMessage = objectMapper.readValue(it.message(), SnsEvent::class.java)

            deserializedMessage.eventType == "applications.cas2.application.submitted" &&
              deserializedMessage.version == 1 &&
              deserializedMessage.description == "An application has been submitted for a CAS2 placement" &&
              deserializedMessage.detailUrl == detailUrl &&
              deserializedMessage.occurredAt.toInstant() == domainEventToSave.occurredAt &&
              deserializedMessage.additionalInformation.applicationId == applicationId &&
              deserializedMessage.personReference.identifiers.any { it.type == "NOMS" && it.value == domainEventToSave.data.eventDetails.personReference.noms } &&
              deserializedMessage.personReference.identifiers.any { it.type == "CRN" && it.value == domainEventToSave.data.eventDetails.personReference.crn }
          },
        )
      }

      verify(exactly = 1) {
        mockDomainEventUrlConfig.getUrlForDomainEventId(DomainEventType.CAS2_APPLICATION_SUBMITTED, domainEventToSave.id)
      }
    }
  }
}
