package uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.cas2bail

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas2StatusUpdate
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas2StatusUpdateDetail
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.LatestCas2StatusUpdate
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.cas2bail.Cas2BailApplicationSummaryEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.cas2bail.Cas2BailStatusUpdateDetailEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.cas2bail.Cas2BailStatusUpdateEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.ExternalUserTransformer
import java.util.UUID

@Component("Cas2BailStatusUpdateTransformer")
class Cas2BailStatusUpdateTransformer(
  private val externalUserTransformer: ExternalUserTransformer,
) {

  fun transformJpaToApi(
    jpa: Cas2BailStatusUpdateEntity,
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

  fun transformStatusUpdateDetailsJpaToApi(jpa: Cas2BailStatusUpdateDetailEntity): Cas2StatusUpdateDetail {
    return Cas2StatusUpdateDetail(
      id = jpa.id,
      name = jpa.statusDetail(jpa.statusUpdate.statusId, jpa.statusDetailId).name,
      label = jpa.label,
    )
  }

  fun transformJpaSummaryToLatestStatusUpdateApi(jpa: Cas2BailApplicationSummaryEntity): LatestCas2StatusUpdate? {
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
