package uk.gov.justice.digital.hmpps.approvedpremisesapi.factory

import io.github.bluegroundltd.kfactory.Factory
import io.github.bluegroundltd.kfactory.Yielded
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApprovedPremisesApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.JsonSchemaEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PlacementApplicationDecision
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PlacementApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PlacementDateEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PlacementType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.randomDateTimeBefore
import java.time.OffsetDateTime
import java.util.UUID

class PlacementApplicationEntityFactory : Factory<PlacementApplicationEntity> {
  private var id: Yielded<UUID> = { UUID.randomUUID() }
  private var createdByUser: Yielded<UserEntity>? = null
  private var allocatedToUser: Yielded<UserEntity?> = { null }
  private var application: Yielded<ApprovedPremisesApplicationEntity>? = null
  private var schemaVersion: Yielded<JsonSchemaEntity> = {
    ApprovedPremisesPlacementApplicationJsonSchemaEntityFactory().produce()
  }
  private var data: Yielded<String?> = { "{}" }
  private var document: Yielded<String?> = { "{}" }
  private var createdAt: Yielded<OffsetDateTime> = { OffsetDateTime.now().randomDateTimeBefore(30) }
  private var submittedAt: Yielded<OffsetDateTime?> = { null }
  private var decision: Yielded<PlacementApplicationDecision?> = { null }
  private var reallocatedAt: Yielded<OffsetDateTime?> = { null }
  private var placementDates: Yielded<MutableList<PlacementDateEntity>?> = { null }
  private var placementType: Yielded<PlacementType?> = { null }

  fun withId(id: UUID) = apply {
    this.id = { id }
  }

  fun withCreatedByUser(createdByUser: UserEntity) = apply {
    this.createdByUser = { createdByUser }
  }

  fun withAllocatedToUser(allocatedToUser: UserEntity?) = apply {
    this.allocatedToUser = { allocatedToUser }
  }

  fun withData(data: String?) = apply {
    this.data = { data }
  }

  fun withDocument(document: String?) = apply {
    this.document = { document }
  }

  fun withSchemaVersion(schemaVersion: JsonSchemaEntity) = apply {
    this.schemaVersion = { schemaVersion }
  }

  fun withCreatedAt(createdAt: OffsetDateTime) = apply {
    this.createdAt = { createdAt }
  }

  fun withSubmittedAt(submittedAt: OffsetDateTime?) = apply {
    this.submittedAt = { submittedAt }
  }

  fun withApplication(applicationEntity: ApprovedPremisesApplicationEntity) = apply {
    this.application = { applicationEntity }
  }

  fun withDecision(decision: PlacementApplicationDecision?) = apply {
    this.decision = { decision }
  }

  fun withReallocatedAt(reallocatedAt: OffsetDateTime?) = apply {
    this.reallocatedAt = { reallocatedAt }
  }

  fun withPlacementDates(placementDates: MutableList<PlacementDateEntity>) = apply {
    this.placementDates = { placementDates }
  }

  fun withPlacementType(placementType: PlacementType) = apply {
    this.placementType = { placementType }
  }

  override fun produce(): PlacementApplicationEntity = PlacementApplicationEntity(
    id = this.id(),
    application = this.application?.invoke() ?: throw RuntimeException("Must provide an application"),
    createdByUser = this.createdByUser?.invoke() ?: throw RuntimeException("Must provide a createdByUser"),
    schemaVersion = this.schemaVersion(),
    schemaUpToDate = false,
    data = this.data(),
    document = this.document(),
    createdAt = this.createdAt(),
    submittedAt = this.submittedAt(),
    allocatedToUser = this.allocatedToUser(),
    allocatedAt = null,
    reallocatedAt = this.reallocatedAt(),
    decision = this.decision(),
    placementType = this.placementType(),
    placementDates = this.placementDates() ?: mutableListOf(),
    placementRequests = mutableListOf(),
  )
}
