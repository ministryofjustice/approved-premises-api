package uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.cas2v2

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas2Assessment
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.cas2v2.Cas2v2AssessmentEntity

@Component("Cas2v2AssessmentsTransformer")
class Cas2v2AssessmentsTransformer(
  private val statusUpdateTransformer: Cas2v2StatusUpdateTransformer,
) {
  fun transformJpaToApiRepresentation(
    jpaAssessment: Cas2v2AssessmentEntity,
  ): Cas2Assessment {
    return Cas2Assessment(
      jpaAssessment.id,
      jpaAssessment.nacroReferralId,
      jpaAssessment.assessorName,
      jpaAssessment.statusUpdates?.map { update -> statusUpdateTransformer.transformJpaToApi(update) },
    )
  }
}
