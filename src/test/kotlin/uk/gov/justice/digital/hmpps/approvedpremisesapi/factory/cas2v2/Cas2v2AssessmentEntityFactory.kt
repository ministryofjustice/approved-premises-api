package uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.cas2v2

import io.github.bluegroundltd.kfactory.Factory
import io.github.bluegroundltd.kfactory.Yielded
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.NomisUserEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.cas2v2.Cas2v2ApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.cas2v2.Cas2v2AssessmentEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.cas2v2.Cas2v2StatusUpdateEntity
import java.time.OffsetDateTime
import java.util.UUID

class Cas2v2AssessmentEntityFactory : Factory<Cas2v2AssessmentEntity> {
  private var id: Yielded<UUID> = { UUID.randomUUID() }
  private var createdAt: Yielded<OffsetDateTime> = { OffsetDateTime.now() }
  private var application: Yielded<Cas2v2ApplicationEntity> = {
    Cas2v2ApplicationEntityFactory()
      .withCreatedByUser(NomisUserEntityFactory().produce())
      .produce()
  }
  private var nacroReferralId: String? = null
  private var assessorName: String? = null
  private var statusUpdates: MutableList<Cas2v2StatusUpdateEntity> = mutableListOf()

  fun withId(id: UUID) = apply {
    this.id = { id }
  }

  fun withApplication(application: Cas2v2ApplicationEntity) = apply {
    this.application = { application }
  }

  fun withStatusUpdates(statusUpdates: MutableList<Cas2v2StatusUpdateEntity>) = apply {
    this.statusUpdates = statusUpdates
  }

  fun withNacroReferralId(id: String) = apply {
    this.nacroReferralId = id
  }

  fun withAssessorName(name: String) = apply {
    this.assessorName = name
  }

  fun withCreatedAt(createdAt: OffsetDateTime) = apply {
    this.createdAt = { createdAt }
  }

  override fun produce(): Cas2v2AssessmentEntity = Cas2v2AssessmentEntity(
    id = this.id(),
    createdAt = this.createdAt(),
    application = this.application(),
    nacroReferralId = this.nacroReferralId,
    assessorName = this.assessorName,
    statusUpdates = this.statusUpdates,
  )
}
