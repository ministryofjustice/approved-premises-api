package uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.cas2v2

import io.github.bluegroundltd.kfactory.Factory
import io.github.bluegroundltd.kfactory.Yielded
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ExternalUserEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ExternalUserEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.cas2v2.Cas2v2ApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.cas2v2.Cas2v2AssessmentEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.cas2v2.Cas2v2StatusUpdateDetailEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.cas2v2.Cas2v2StatusUpdateEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.reference.Cas2ApplicationStatusSeeding
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.randomDateTimeBefore
import java.time.OffsetDateTime
import java.util.UUID

class Cas2v2StatusUpdateEntityFactory : Factory<Cas2v2StatusUpdateEntity> {
  private var id: Yielded<UUID> = { UUID.randomUUID() }
  private var assessor: Yielded<ExternalUserEntity> = { ExternalUserEntityFactory().produce() }
  private var assessment: Yielded<Cas2v2AssessmentEntity?> = { null }
  private var application: Yielded<Cas2v2ApplicationEntity>? = null
  private var statusId: Yielded<UUID> = { Cas2ApplicationStatusSeeding.statusList().random().id }
  private var createdAt: Yielded<OffsetDateTime> = { OffsetDateTime.now().randomDateTimeBefore(30) }
  private var label: Yielded<String> = { "More information requested" }
  private var description: Yielded<String> = { "More information about the application has been requested" }
  private var statusUpdateDetails: Yielded<List<Cas2v2StatusUpdateDetailEntity>?> = { null }

  fun withId(id: UUID) = apply {
    this.id = { id }
  }

  fun withAssessor(assessor: ExternalUserEntity) = apply {
    this.assessor = { assessor }
  }

  fun withAssessment(assessment: Cas2v2AssessmentEntity) = apply {
    this.assessment = { assessment }
  }

  fun withApplication(application: Cas2v2ApplicationEntity) = apply {
    this.application = { application }
  }

  fun withStatusId(statusId: UUID) = apply {
    this.statusId = { statusId }
  }

  fun withCreatedAt(createdAt: OffsetDateTime) = apply {
    this.createdAt = { createdAt }
  }

  fun withLabel(label: String) = apply {
    this.label = { label }
  }

  fun withDescription(description: String) = apply {
    this.description = { description }
  }

  fun withStatusUpdateDetails(details: List<Cas2v2StatusUpdateDetailEntity>) = apply {
    this.statusUpdateDetails = { details }
  }

  override fun produce(): Cas2v2StatusUpdateEntity = Cas2v2StatusUpdateEntity(
    id = this.id(),
    assessor = this.assessor(),
    application = this.application?.invoke() ?: error("Must provide a submitted application"),
    assessment = this.assessment(),
    statusId = this.statusId(),
    createdAt = this.createdAt(),
    label = this.label(),
    description = this.description(),
    statusUpdateDetails = this.statusUpdateDetails(),
  )
}
