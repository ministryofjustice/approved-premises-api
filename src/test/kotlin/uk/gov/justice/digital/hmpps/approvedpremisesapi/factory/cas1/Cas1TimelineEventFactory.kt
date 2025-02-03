package uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.cas1

import io.github.bluegroundltd.kfactory.Factory
import io.github.bluegroundltd.kfactory.Yielded
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1TimelineEvent
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1TimelineEventAssociatedUrl
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1TimelineEventContentPayload
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1TimelineEventType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.User
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.events.ApprovedPremisesUserFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.randomDateTimeBefore
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.randomOf
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.randomStringMultiCaseWithNumbers
import java.time.Instant
import java.util.UUID

class Cas1TimelineEventFactory : Factory<Cas1TimelineEvent> {
  private var type: Yielded<Cas1TimelineEventType?> = { randomOf(Cas1TimelineEventType.entries) }
  private var id: Yielded<UUID?> = { UUID.randomUUID() }
  private var occurredAt: Yielded<Instant?> = { Instant.now().randomDateTimeBefore(7) }
  private var content: Yielded<String?> = { randomStringMultiCaseWithNumbers(20) }
  private var createdBy: Yielded<User?> = { null }
  private var associatedUrls: Yielded<List<Cas1TimelineEventAssociatedUrl>?> = { null }
  private var payload: Yielded<Cas1TimelineEventContentPayload?> = { null }

  fun withType(type: Cas1TimelineEventType?) = apply {
    this.type = { type }
  }

  fun withId(id: UUID?) = apply {
    this.id = { id }
  }

  fun withOccurredAt(occurredAt: Instant?) = apply {
    this.occurredAt = { occurredAt }
  }

  fun withContent(content: String?) = apply {
    this.content = { content }
  }

  fun withCreatedBy(createdBy: User?) = apply {
    this.createdBy = { createdBy }
  }

  fun withCreatedBy(configuration: ApprovedPremisesUserFactory.() -> Unit) = apply {
    this.createdBy = { ApprovedPremisesUserFactory().apply(configuration).produce() }
  }

  fun withAssociatedUrls(associatedUrls: List<Cas1TimelineEventAssociatedUrl>?) = apply {
    this.associatedUrls = { associatedUrls }
  }

  fun withPayload(payload: Cas1TimelineEventContentPayload?) = apply {
    this.payload = { payload }
  }

  override fun produce() = Cas1TimelineEvent(
    type = this.type()!!,
    id = this.id()?.toString()!!,
    occurredAt = this.occurredAt()!!,
    content = this.content(),
    createdBy = this.createdBy(),
    associatedUrls = this.associatedUrls(),
    payload = this.payload(),
  )
}
