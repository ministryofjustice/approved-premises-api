package uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.cas2v2

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas2StatusUpdate
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas2StatusUpdateDetail
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.LatestCas2StatusUpdate
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.cas2v2.Cas2v2ApplicationSummaryEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.cas2v2.Cas2v2StatusUpdateDetailEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.cas2v2.Cas2v2StatusUpdateEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.ExternalUserTransformer
import java.util.UUID

@Component("Cas2v2StatusUpdateTransformer")
class Cas2v2StatusUpdateTransformer(
  private val externalUserTransformer: ExternalUserTransformer,
) {

  fun transformJpaToApi(
      jpa: Cas2v2StatusUpdateEntity,
  ): Cas2StatusUpdate {
    return Cas2StatusUpdate(
      id = jpa.id,
      name = jpa.status().name,
      label = jpa.label,
      description = jpa.description,
      updatedBy = externalUserTransformer.transformJpaToApi(jpa.assessor),
      updatedAt = jpa.createdAt?.toInstant(),
      statusUpdateDetails = jpa.statusUpdateDetails?.map { detail -> transformStatusUpdateDetailsJpaToApi(detail) },
    )
  }

  fun transformStatusUpdateDetailsJpaToApi(jpa: Cas2v2StatusUpdateDetailEntity): Cas2StatusUpdateDetail {
    return Cas2StatusUpdateDetail(
      id = jpa.id,
      name = jpa.statusDetail(jpa.statusUpdate.statusId, jpa.statusDetailId).name,
      label = jpa.label,
    )
  }

  fun transformJpaSummaryToLatestStatusUpdateApi(jpa: Cas2v2ApplicationSummaryEntity): LatestCas2StatusUpdate? {
    if (jpa.latestStatusUpdateStatusId !== null) {
      return LatestCas2StatusUpdate(
        statusId = UUID.fromString(jpa.latestStatusUpdateStatusId!!),
        label = jpa.latestStatusUpdateLabel!!,
      )
    } else {
      return null
    }
  }
}
