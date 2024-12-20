package uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.events

import io.github.bluegroundltd.kfactory.Factory
import io.github.bluegroundltd.kfactory.Yielded
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model.DestinationProvider
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.randomStringMultiCaseWithNumbers
import java.util.UUID

class DestinationProviderFactory : Factory<DestinationProvider> {
  private var id: Yielded<UUID> = { UUID.randomUUID() }
  private var description: Yielded<String> = { randomStringMultiCaseWithNumbers(10) }

  fun withId(id: UUID) = apply {
    this.id = { id }
  }

  fun withName(description: String) = apply {
    this.description = { description }
  }

  override fun produce() = DestinationProvider(
    id = this.id(),
    description = this.description(),
  )
}
