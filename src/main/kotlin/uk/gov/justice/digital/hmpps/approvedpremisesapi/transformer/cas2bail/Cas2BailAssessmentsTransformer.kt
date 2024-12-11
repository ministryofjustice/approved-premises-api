package uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.cas2bail

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas2Assessment
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.cas2bail.Cas2BailAssessmentEntity

@Component("Cas2BailAssessmentsTransformer")
class Cas2BailAssessmentsTransformer(
  private val statusUpdateTransformer: Cas2BailStatusUpdateTransformer,
) {
  fun transformJpaToApiRepresentation(
    jpaAssessment: Cas2BailAssessmentEntity,
  ): Cas2Assessment {
    return Cas2Assessment(
      jpaAssessment.id,
      jpaAssessment.nacroReferralId,
      jpaAssessment.assessorName,
      jpaAssessment.statusUpdates?.map { update -> statusUpdateTransformer.transformJpaToApi(update) },
    )
  }
}
