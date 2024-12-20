package uk.gov.justice.digital.hmpps.approvedpremisesapi.factory

import io.github.bluegroundltd.kfactory.Factory
import io.github.bluegroundltd.kfactory.Yielded
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.OfflineApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.randomDateTimeBefore
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.randomStringMultiCaseWithNumbers
import java.time.OffsetDateTime
import java.util.UUID

class OfflineApplicationEntityFactory : Factory<OfflineApplicationEntity> {
  private var id: Yielded<UUID> = { UUID.randomUUID() }
  private var crn: Yielded<String> = { randomStringMultiCaseWithNumbers(6) }
  private var service: Yielded<String> = { "approved-premises" }
  private var createdAt: Yielded<OffsetDateTime> = { OffsetDateTime.now().randomDateTimeBefore(7) }
  private var eventNumber: Yielded<String?> = { randomStringMultiCaseWithNumbers(6) }
  private var name: Yielded<String?> = { randomStringMultiCaseWithNumbers(10) }

  fun withId(id: UUID) = apply {
    this.id = { id }
  }

  fun withCrn(crn: String) = apply {
    this.crn = { crn }
  }

  fun withService(service: String) = apply {
    this.service = { service }
  }

  fun withCreatedAt(createdAt: OffsetDateTime) = apply {
    this.createdAt = { createdAt }
  }

  fun withEventNumber(eventNumber: String?) = apply {
    this.eventNumber = { eventNumber }
  }

  fun withName(name: String?) = apply {
    this.name = { name }
  }

  override fun produce() = OfflineApplicationEntity(
    id = this.id(),
    crn = this.crn(),
    service = this.service(),
    createdAt = this.createdAt(),
    eventNumber = this.eventNumber(),
    name = this.name(),
  )
}
