package uk.gov.justice.digital.hmpps.approvedpremisesapi.factory

import io.github.bluegroundltd.kfactory.Factory
import io.github.bluegroundltd.kfactory.Yielded
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApplicationTimelineNoteEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.randomDateTimeBefore
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.randomStringUpperCase
import java.time.OffsetDateTime
import java.util.UUID

class ApplicationTimelineNoteEntityFactory : Factory<ApplicationTimelineNoteEntity> {
  private var id: Yielded<UUID> = { UUID.randomUUID() }
  private var applicationId: Yielded<UUID> = { UUID.randomUUID() }
  private var createdBy: Yielded<UUID> = { UUID.randomUUID() }
  private var createdAt: Yielded<OffsetDateTime> = { OffsetDateTime.now().randomDateTimeBefore(30) }
  private var body: Yielded<String> = { randomStringUpperCase(12) }

  fun withId(id: UUID) = apply {
    this.id = { id }
  }

  fun withApplicationId(applicationId: UUID) = apply {
    this.applicationId = { applicationId }
  }

  fun withCreatedBy(createdBy: UserEntity) = apply {
    this.createdBy = { createdBy.id }
  }

  fun withCreatedAt(createdAt: OffsetDateTime) = apply {
    this.createdAt = { createdAt }
  }

  fun withBody(body: String) = apply {
    this.body = { body }
  }

  override fun produce(): ApplicationTimelineNoteEntity = ApplicationTimelineNoteEntity(
    id = this.id(),
    applicationId = this.applicationId(),
    createdBy = this.createdBy(),
    createdAt = this.createdAt(),
    body = this.body(),
  )
}
