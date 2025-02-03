package uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.cas1

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1ApplicationTimeline
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1PersonalTimeline
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1TimelineEvent
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ServiceName
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApprovedPremisesApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.OfflineApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.PersonInfoResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.PersonTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.UserTransformer

@Component
class Cas1PersonalTimelineTransformer(
  private val userTransformer: UserTransformer,
  private val personTransformer: PersonTransformer,
) {
  fun transformApplicationTimelineModels(
    personInfoResult: PersonInfoResult,
    applicationTimelineModels: List<Cas1ApplicationTimelineModel>,
  ) = Cas1PersonalTimeline(
    person = personTransformer.transformModelToPersonApi(personInfoResult),
    applications = applicationTimelineModels.map { transformApplication(it.application, it.cas1TimelineEvents) },
  )

  private fun transformApplication(
    application: BoxedApplication,
    cas1TimelineEvents: List<Cas1TimelineEvent>,
  ) = application.map(
    { transformRegularApplication(it, cas1TimelineEvents) },
    { transformOfflineApplication(it, cas1TimelineEvents) },
  )

  private fun transformRegularApplication(
    application: ApprovedPremisesApplicationEntity,
    cas1TimelineEvents: List<Cas1TimelineEvent>,
  ) = Cas1ApplicationTimeline(
    id = application.id,
    createdAt = application.createdAt.toInstant(),
    status = application.status.toCas1Status(),
    isOfflineApplication = false,
    createdBy = userTransformer.transformJpaToApi(application.createdByUser, ServiceName.approvedPremises),
    timelineEvents = cas1TimelineEvents,
  )

  private fun transformOfflineApplication(
    application: OfflineApplicationEntity,
    cas1TimelineEvents: List<Cas1TimelineEvent>,
  ) = Cas1ApplicationTimeline(
    id = application.id,
    createdAt = application.createdAt.toInstant(),
    status = null,
    isOfflineApplication = true,
    createdBy = null,
    timelineEvents = cas1TimelineEvents,
  )
}

data class Cas1ApplicationTimelineModel(
  val application: BoxedApplication,
  val cas1TimelineEvents: List<Cas1TimelineEvent>,
)

sealed interface BoxedApplication {
  val value: Any

  fun <T> map(
    regularApplicationFunc: (ApprovedPremisesApplicationEntity) -> T,
    offlineApplicationFunc: (OfflineApplicationEntity) -> T,
  ) = when (this) {
    is Regular -> regularApplicationFunc(this.value)
    is Offline -> offlineApplicationFunc(this.value)
  }

  data class Regular(override val value: ApprovedPremisesApplicationEntity) : BoxedApplication
  data class Offline(override val value: OfflineApplicationEntity) : BoxedApplication

  companion object {
    fun of(value: ApprovedPremisesApplicationEntity) = Regular(value)
    fun of(value: OfflineApplicationEntity) = Offline(value)
  }
}
