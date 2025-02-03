package uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.transformer.cas1

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1ApplicationTimeline
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1TimelineEvent
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Person
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ServiceName
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.User
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ApprovedPremisesApplicationEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.OffenderDetailsSummaryFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.OfflineApplicationEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.UserEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.cas1.Cas1TimelineEventFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.events.ApprovedPremisesUserFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApprovedPremisesApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.OfflineApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.PersonInfoResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.PersonTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.UserTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.cas1.BoxedApplication
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.cas1.Cas1ApplicationTimelineModel
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.cas1.Cas1PersonalTimelineTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.randomOf
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.ApprovedPremisesApplicationStatus as DomainApplicationStatus

class Cas1PersonalTimelineTransformerTest {
  private val userTransformer = mockk<UserTransformer>()
  private val personTransformer = mockk<PersonTransformer>()

  private val cas1PersonalTimelineTransformer = Cas1PersonalTimelineTransformer(userTransformer, personTransformer)

  companion object {
    private fun assertApplicationMatches(
      actual: Cas1ApplicationTimeline,
      expectedApplication: ApprovedPremisesApplicationEntity,
      expectedUser: User,
      expectedTimelineEvents: List<Cas1TimelineEvent>,
    ) {
      assertThat(actual.id).isEqualTo(expectedApplication.id)
      assertThat(actual.createdAt).isEqualTo(expectedApplication.createdAt.toInstant())
      assertThat(actual.status).isEqualTo(expectedApplication.status.toCas1Status())
      assertThat(actual.isOfflineApplication).isFalse
      assertThat(actual.createdBy).isEqualTo(expectedUser)
      assertThat(actual.timelineEvents).isEqualTo(expectedTimelineEvents)
    }

    private fun assertApplicationMatches(
      actual: Cas1ApplicationTimeline,
      expectedApplication: OfflineApplicationEntity,
      expectedTimelineEvents: List<Cas1TimelineEvent>,
    ) {
      assertThat(actual.id).isEqualTo(expectedApplication.id)
      assertThat(actual.createdAt).isEqualTo(expectedApplication.createdAt.toInstant())
      assertThat(actual.status).isNull()
      assertThat(actual.isOfflineApplication).isTrue
      assertThat(actual.createdBy).isNull()
      assertThat(actual.timelineEvents).isEqualTo(expectedTimelineEvents)
    }
  }

  @SuppressWarnings("detekt:DestructuringDeclarationWithTooManyEntries")
  @Test
  fun `transformApplicationsAndTimelineEvents creates the expected PersonalTimeline`() {
    val (application0, application1, application2, application3) = ApprovedPremisesApplicationEntityFactory()
      .withYieldedCreatedByUser {
        UserEntityFactory()
          .withDefaultProbationRegion()
          .produce()
      }
      .withStatus(randomOf(DomainApplicationStatus.entries))
      .produceMany()
      .take(4)
      .toList()

    val timelineEvents0 = emptyList<Cas1TimelineEvent>()

    val timelineEvents1 = Cas1TimelineEventFactory()
      .produceMany()
      .take(1)
      .toList()

    val timelineEvents2 = Cas1TimelineEventFactory().produceMany()
      .take(2)
      .toList()

    val timelineEvents3 = Cas1TimelineEventFactory()
      .produceMany()
      .take(3)
      .toList()

    val offlineApplication = OfflineApplicationEntityFactory()
      .withService(ServiceName.approvedPremises.value)
      .produce()

    val offlineApplicationTimelineEvents = Cas1TimelineEventFactory()
      .produceMany()
      .take(1)
      .toList()

    val applicationTimelineModels = listOf(
      Cas1ApplicationTimelineModel(BoxedApplication.of(application0), timelineEvents0),
      Cas1ApplicationTimelineModel(BoxedApplication.of(application1), timelineEvents1),
      Cas1ApplicationTimelineModel(BoxedApplication.of(application2), timelineEvents2),
      Cas1ApplicationTimelineModel(BoxedApplication.of(application3), timelineEvents3),
      Cas1ApplicationTimelineModel(BoxedApplication.of(offlineApplication), offlineApplicationTimelineEvents),
    )

    val (user0, user1, user2, user3) = ApprovedPremisesUserFactory()
      .produceMany()
      .take(4)
      .toList()

    val offenderDetailSummary = OffenderDetailsSummaryFactory().produce()
    val personInfoResult = PersonInfoResult.Success.Full(
      crn = offenderDetailSummary.otherIds.crn,
      offenderDetailSummary = offenderDetailSummary,
      inmateDetail = null,
    )
    val mockPerson = mockk<Person>()

    every { userTransformer.transformJpaToApi(application0.createdByUser, ServiceName.approvedPremises) } returns user0
    every { userTransformer.transformJpaToApi(application1.createdByUser, ServiceName.approvedPremises) } returns user1
    every { userTransformer.transformJpaToApi(application2.createdByUser, ServiceName.approvedPremises) } returns user2
    every { userTransformer.transformJpaToApi(application3.createdByUser, ServiceName.approvedPremises) } returns user3

    every { personTransformer.transformModelToPersonApi(personInfoResult) } returns mockPerson

    val transformedPersonalTimeline = cas1PersonalTimelineTransformer
      .transformApplicationTimelineModels(personInfoResult, applicationTimelineModels)

    assertThat(transformedPersonalTimeline.applications).hasSize(5)

    assertApplicationMatches(transformedPersonalTimeline.applications[0], application0, user0, timelineEvents0)
    assertApplicationMatches(transformedPersonalTimeline.applications[1], application1, user1, timelineEvents1)
    assertApplicationMatches(transformedPersonalTimeline.applications[2], application2, user2, timelineEvents2)
    assertApplicationMatches(transformedPersonalTimeline.applications[3], application3, user3, timelineEvents3)
    assertApplicationMatches(transformedPersonalTimeline.applications[4], offlineApplication, offlineApplicationTimelineEvents)

    assertThat(transformedPersonalTimeline.person).isEqualTo(mockPerson)

    verify(exactly = 1) { personTransformer.transformModelToPersonApi(personInfoResult) }
  }
}
