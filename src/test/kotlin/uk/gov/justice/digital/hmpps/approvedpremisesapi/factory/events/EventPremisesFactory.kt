package uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.events

import io.github.bluegroundltd.kfactory.Factory
import io.github.bluegroundltd.kfactory.Yielded
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model.Premises
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.randomStringMultiCaseWithNumbers
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.randomStringUpperCase
import java.util.UUID

class EventPremisesFactory : Factory<Premises> {
  private var id: Yielded<UUID> = { UUID.randomUUID() }
  private var name: Yielded<String> = { randomStringMultiCaseWithNumbers(10) }
  private var apCode: Yielded<String> = { randomStringMultiCaseWithNumbers(10) }
  private var legacyApCode: Yielded<String> = { randomStringMultiCaseWithNumbers(10) }
  private var localAuthorityAreaName: Yielded<String> = { randomStringUpperCase(10) }

  fun withId(id: UUID) = apply {
    this.id = { id }
  }

  fun withName(name: String) = apply {
    this.name = { name }
  }

  fun withApCode(apCode: String) = apply {
    this.apCode = { apCode }
  }

  fun withLegacyApCode(legacyApCode: String) = apply {
    this.legacyApCode = { legacyApCode }
  }

  fun withLocalAuthorityAreaName(localAuthorityAreaName: String) = apply {
    this.localAuthorityAreaName = { localAuthorityAreaName }
  }

  override fun produce() = Premises(
    id = this.id(),
    name = this.name(),
    apCode = this.apCode(),
    legacyApCode = this.legacyApCode(),
    localAuthorityAreaName = this.localAuthorityAreaName(),
  )
}
