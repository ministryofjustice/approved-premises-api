package uk.gov.justice.digital.hmpps.approvedpremisesapi.model

import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApprovedPremisesAssessmentEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PlacementApplicationEntity
import java.time.OffsetDateTime
import java.util.UUID

sealed class TypedTask(
  val id: UUID,
  val createdAt: OffsetDateTime,
  val crn: String,
) {
  data class Assessment(val entity: ApprovedPremisesAssessmentEntity) : TypedTask(entity.id, entity.createdAt, entity.application.crn)
  data class PlacementApplication(val entity: PlacementApplicationEntity) : TypedTask(entity.id, entity.createdAt, entity.application.crn)
}
