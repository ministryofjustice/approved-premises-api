package uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.cas1

import com.fasterxml.jackson.core.type.TypeReference
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.repository.findByIdOrNull
import org.springframework.test.web.reactive.server.returnResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model.ApplicationAssessedEnvelope
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model.ApplicationExpiredEnvelope
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model.EventType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ServiceName
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.TimelineEvent
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.TimelineEventType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.events.ApplicationAssessedFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenAProbationRegion
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenAUser
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApplicationRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApprovedPremisesApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.DomainEventType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.TriggerSourceType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.ApprovedPremisesApplicationStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.Cas1DomainEventService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.scheduled.Cas1ExpiredApplicationsScheduledJob
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.ApplicationTimelineTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.domainevents.DomainEventSummaryImpl
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.randomDateTimeBefore
import java.time.Instant
import java.time.OffsetDateTime
import java.util.UUID

class Cas1ExpiredApplicationsScheduledJobTest : IntegrationTestBase() {

  @Autowired
  lateinit var applicationRepository: ApplicationRepository

  @Autowired
  lateinit var cas1ExpiredApplicationsScheduledJob: Cas1ExpiredApplicationsScheduledJob

  @Autowired
  lateinit var domainEventService: Cas1DomainEventService

  @Autowired
  lateinit var applicationTimelineTransformer: ApplicationTimelineTransformer

  @Test
  fun `sets the application status to EXPIRED for the correct applications when the job is run`() {
    givenAProbationRegion { probationRegion ->
      givenAUser(probationRegion = probationRegion) { user, jwt ->

        val unexpiredOccurredAt = OffsetDateTime.now().randomDateTimeBefore(365) // between 1 and 365 days
        val expiredOccurredAt = unexpiredOccurredAt.minusYears(1) // between 366 and 730 days
        val rejectedDecisionStatus = "REJECTED"
        val acceptedDecisionStatus = "ACCEPTED"

        val unexpiredApplications = createApplications(user, unexpiredOccurredAt, rejectedDecisionStatus, 2)
        unexpiredApplications.addAll(createApplications(user, unexpiredOccurredAt, acceptedDecisionStatus, 2))
        unexpiredApplications.addAll(createApplications(user, expiredOccurredAt, rejectedDecisionStatus, 2))
        val expiredApplications = createApplications(user, expiredOccurredAt, acceptedDecisionStatus, 2)

        cas1ExpiredApplicationsScheduledJob.expireApplications()

        expiredApplications.forEach {
          assertThat((applicationRepository.findByIdOrNull(it.id)!! as ApprovedPremisesApplicationEntity).status.name).isEqualTo(
            ApprovedPremisesApplicationStatus.EXPIRED.name,
          )

          // test domain events
          domainEventAsserter.assertDomainEventStoreCount(it.id, 2)
          val persistedApplicationExpiredEvent = domainEventAsserter.assertDomainEventOfTypeStored(
            it.id,
            DomainEventType.APPROVED_PREMISES_APPLICATION_EXPIRED,
          )

          assertThat(persistedApplicationExpiredEvent).isNotNull
          assertThat(persistedApplicationExpiredEvent.applicationId).isEqualTo(it.id)
          assertThat(persistedApplicationExpiredEvent.crn).isEqualTo(it.crn)
          assertThat(persistedApplicationExpiredEvent.nomsNumber).isEqualTo(it.nomsNumber)
          assertThat(persistedApplicationExpiredEvent.occurredAt).isNotNull()
          assertThat(persistedApplicationExpiredEvent.createdAt).isNotNull()
          assertThat(persistedApplicationExpiredEvent.service).isEqualTo("CAS1")
          assertThat(persistedApplicationExpiredEvent.triggerSource).isEqualTo(TriggerSourceType.SYSTEM)

          val envelope =
            objectMapper.readValue(persistedApplicationExpiredEvent.data, ApplicationExpiredEnvelope::class.java)
          assertThat(envelope.eventDetails.previousStatus).isEqualTo(it.status.name)
          assertThat(envelope.eventDetails.updatedStatus).isEqualTo(ApprovedPremisesApplicationStatus.EXPIRED.name)
        }

        unexpiredApplications.forEach {
          assertThat((applicationRepository.findByIdOrNull(it.id)!! as ApprovedPremisesApplicationEntity).status).isEqualTo(
            ApprovedPremisesApplicationStatus.STARTED,
          )

          domainEventAsserter.assertDomainEventStoreCount(it.id, 1)
          domainEventAsserter.assertDomainEventOfTypeStored(
            it.id,
            DomainEventType.APPROVED_PREMISES_APPLICATION_ASSESSED,
          )
        }

        // test the expired application event is included in the timeline for the first expired Application
        expiredApplications.first().id

        val rawResponseBody = webTestClient.get()
          .uri("/applications/${expiredApplications.first().id}/timeline")
          .header("Authorization", "Bearer $jwt")
          .header("X-Service-Name", ServiceName.approvedPremises.value)
          .exchange()
          .expectStatus()
          .isOk
          .returnResult<String>()
          .responseBody
          .blockFirst()

        val responseBody =
          objectMapper.readValue(
            rawResponseBody,
            object : TypeReference<List<TimelineEvent>>() {},
          )

        val domainEvents = domainEventService.getAllDomainEventsForApplication(expiredApplications.first().id)

        val expectedItems = mutableListOf<TimelineEvent>()
        expectedItems.addAll(
          domainEvents.map {
            applicationTimelineTransformer.transformDomainEventSummaryToTimelineEvent(
              DomainEventSummaryImpl(
                it.id,
                it.type,
                it.occurredAt,
                it.applicationId,
                it.assessmentId,
                null,
                null,
                null,
                null,
                it.triggerSource,
                null,
              ),
            )
          },
        )

        assertThat(responseBody.count()).isEqualTo(expectedItems.count())
        assertThat(responseBody).hasSameElementsAs(expectedItems)
        assertThat(responseBody.last().type).isEqualTo(TimelineEventType.approvedPremisesApplicationExpired)
      }
    }
  }

  private fun createApplications(user: UserEntity, occurredAt: OffsetDateTime, status: String, number: Int): MutableList<ApprovedPremisesApplicationEntity> {
    val applications = mutableListOf<ApprovedPremisesApplicationEntity>()
    repeat(number) {
      val application = approvedPremisesApplicationEntityFactory.produceAndPersist {
        withCreatedByUser(user)
        withApplicationSchema(approvedPremisesApplicationJsonSchemaEntityFactory.produceAndPersist())
      }

      domainEventFactory.produceAndPersist {
        withOccurredAt(occurredAt)
        withType(DomainEventType.APPROVED_PREMISES_APPLICATION_ASSESSED)
        withApplicationId(application.id)
        withData(
          objectMapper.writeValueAsString(
            ApplicationAssessedEnvelope(
              id = UUID.randomUUID(),
              timestamp = Instant.now(),
              eventType = EventType.applicationSubmitted,
              eventDetails = ApplicationAssessedFactory()
                .withDecision(status)
                .produce(),
            ),
          ),
        )
      }
      applications.add(application)
    }
    return applications
  }
}
