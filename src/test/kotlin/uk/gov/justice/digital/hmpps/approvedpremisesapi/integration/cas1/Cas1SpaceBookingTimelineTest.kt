package uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.cas1

import com.fasterxml.jackson.core.type.TypeReference
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.web.reactive.server.returnResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ServiceName
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.TimelineEvent
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.InitialiseDatabasePerClassTestBase
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenAPlacementRequest
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenAProbationRegion
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenAUser
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenAnOffender
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApprovedPremisesApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApprovedPremisesAssessmentEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApprovedPremisesEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.Cas1SpaceBookingEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.DomainEventCas
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.DomainEventEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.DomainEventType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.ApplicationTimelineTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.domainevents.DomainEventSummaryImpl
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.Cas1DomainEventsFactory
import java.time.LocalDate

class Cas1SpaceBookingTimelineTest : InitialiseDatabasePerClassTestBase() {
  @Autowired
  lateinit var applicationTimelineTransformer: ApplicationTimelineTransformer

  lateinit var user: UserEntity
  lateinit var domainEvents: List<DomainEventEntity>

  lateinit var premises: ApprovedPremisesEntity
  lateinit var spaceBooking: Cas1SpaceBookingEntity

  @BeforeAll
  fun setup() {
    val userArgs = givenAUser()

    user = userArgs.first

    val applicationSchema = approvedPremisesApplicationJsonSchemaEntityFactory.produceAndPersist {
      withPermissiveSchema()
    }

    val assessmentSchema = approvedPremisesAssessmentJsonSchemaEntityFactory.produceAndPersist {
      withPermissiveSchema()
    }

    val application = approvedPremisesApplicationEntityFactory.produceAndPersist {
      withApplicationSchema(applicationSchema)
      withCreatedByUser(user)
    }

    val otherApplication = approvedPremisesApplicationEntityFactory.produceAndPersist {
      withApplicationSchema(applicationSchema)
      withCreatedByUser(user)
    }

    val assessment = approvedPremisesAssessmentEntityFactory.produceAndPersist {
      withApplication(application)
      withAllocatedToUser(user)
      withAssessmentSchema(assessmentSchema)
    }

    val otherAssessment = approvedPremisesAssessmentEntityFactory.produceAndPersist {
      withApplication(otherApplication)
      withAllocatedToUser(user)
      withAssessmentSchema(assessmentSchema)
    }

    premises = approvedPremisesEntityFactory.produceAndPersist {
      withYieldedProbationRegion { givenAProbationRegion() }
      withYieldedLocalAuthorityArea { localAuthorityEntityFactory.produceAndPersist() }
    }

    val (offender) = givenAnOffender()
    val (placementRequest) = givenAPlacementRequest(
      placementRequestAllocatedTo = user,
      assessmentAllocatedTo = user,
      createdByUser = user,
    )

    spaceBooking = cas1SpaceBookingEntityFactory.produceAndPersist {
      withCrn(offender.otherIds.crn)
      withPremises(premises)
      withPlacementRequest(placementRequest)
      withApplication(application)
      withCreatedBy(user)
      withCanonicalArrivalDate(LocalDate.parse("2029-05-29"))
      withCanonicalDepartureDate(LocalDate.parse("2029-06-29"))
    }

    val otherSpaceBooking = cas1SpaceBookingEntityFactory.produceAndPersist {
      withCrn(offender.otherIds.crn)
      withPremises(premises)
      withPlacementRequest(placementRequest)
      withApplication(otherApplication)
      withCreatedBy(user)
      withCanonicalArrivalDate(LocalDate.parse("2029-06-29"))
      withCanonicalDepartureDate(LocalDate.parse("2029-07-29"))
    }

    domainEvents = DomainEventType.entries.filter { it.cas == DomainEventCas.CAS1 }.map {
      createDomainEvent(it, otherSpaceBooking, otherApplication, otherAssessment, user)
      return@map createDomainEvent(it, spaceBooking, application, assessment, user)
    }
  }

  @Test
  fun `Get space booking timeline without JWT returns 401`() {
    webTestClient.get()
      .uri("/premises/0/space-bookings/1/timeline")
      .exchange()
      .expectStatus()
      .isUnauthorized
  }

  @Test
  fun `Get space booking timeline with non existent id parameters returns 404`() {
    givenAUser { _, jwt ->
      webTestClient.get()
        .uri("/premises/1/space-bookings/2/timeline")
        .header("Authorization", "Bearer $jwt")
        .header("X-Service-Name", ServiceName.approvedPremises.value)
        .exchange()
        .expectStatus()
        .isNotFound
    }
  }

  @Test
  fun `Get space booking timeline returns all expected items`() {
    givenAUser { _, jwt ->

      val rawResponseBody = webTestClient.get()
        .uri("/cas1/premises/${premises.id}/space-bookings/${spaceBooking.id}/timeline")
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

      val expectedItems = mutableListOf<TimelineEvent>()

      expectedItems.addAll(
        domainEvents.map {
          applicationTimelineTransformer.transformDomainEventSummaryToTimelineEvent(
            DomainEventSummaryImpl(
              it.id.toString(),
              it.type,
              it.occurredAt,
              it.applicationId,
              it.assessmentId,
              null,
              premises.id,
              null,
              it.cas1SpaceBookingId,
              it.triggerSource,
              user,
            ),
          )
        },
      )
      assertThat(responseBody.count()).isEqualTo(expectedItems.count())
      assertThat(responseBody).hasSameElementsAs(expectedItems)
    }
  }

  private fun createDomainEvent(
    type: DomainEventType,
    spaceBookingEntity: Cas1SpaceBookingEntity,
    applicationEntity: ApprovedPremisesApplicationEntity,
    assessmentEntity: ApprovedPremisesAssessmentEntity,
    userEntity: UserEntity,
  ): DomainEventEntity {
    val domainEventsFactory = Cas1DomainEventsFactory(objectMapper)

    val data = if (type == DomainEventType.APPROVED_PREMISES_ASSESSMENT_INFO_REQUESTED) {
      val clarificationNote = assessmentClarificationNoteEntityFactory.produceAndPersist {
        withAssessment(assessmentEntity)
        withCreatedBy(userEntity)
      }
      domainEventsFactory.createEnvelopeForLatestSchemaVersion(type, clarificationNote.id)
    } else {
      domainEventsFactory.createEnvelopeForLatestSchemaVersion(type)
    }

    return domainEventFactory.produceAndPersist {
      withApplicationId(applicationEntity.id)
      withCas1SpaceBookingId(spaceBookingEntity.id)
      withData(data)
      withTriggeredByUserId(userEntity.id)
      withTriggerSourceSystem()
    }
  }
}
