package uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.cas2bail

import io.github.bluegroundltd.kfactory.Factory
import io.github.bluegroundltd.kfactory.Yielded
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.NomisUserEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.cas2bail.Cas2BailApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.cas2bail.Cas2BailAssessmentEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.cas2bail.Cas2BailStatusUpdateEntity
import java.time.OffsetDateTime
import java.util.UUID

class Cas2BailAssessmentEntityFactory : Factory<Cas2BailAssessmentEntity> {
  private var id: Yielded<UUID> = { UUID.randomUUID() }
  private var createdAt: Yielded<OffsetDateTime> = { OffsetDateTime.now() }
  private var application: Yielded<Cas2BailApplicationEntity> = {
    Cas2BailApplicationEntityFactory()
      .withCreatedByUser(NomisUserEntityFactory().produce())
      .produce()
  }
  private var nacroReferralId: String? = null
  private var assessorName: String? = null
  private var statusUpdates: MutableList<Cas2BailStatusUpdateEntity> = mutableListOf()

  fun withId(id: UUID) = apply {
    this.id = { id }
  }

  fun withApplication(application: Cas2BailApplicationEntity) = apply {
    this.application = { application }
  }

  fun withStatusUpdates(statusUpdates: MutableList<Cas2BailStatusUpdateEntity>) = apply {
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

  override fun produce(): Cas2BailAssessmentEntity = Cas2BailAssessmentEntity(
    id = this.id(),
    createdAt = this.createdAt(),
    application = this.application(),
    nacroReferralId = this.nacroReferralId,
    assessorName = this.assessorName,
    statusUpdates = this.statusUpdates,
  )
}
