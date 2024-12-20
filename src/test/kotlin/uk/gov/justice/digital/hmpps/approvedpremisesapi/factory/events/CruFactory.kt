package uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.events

import io.github.bluegroundltd.kfactory.Factory
import io.github.bluegroundltd.kfactory.Yielded
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model.Cru
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.randomStringMultiCaseWithNumbers

class CruFactory : Factory<Cru> {
  private var name: Yielded<String> = { randomStringMultiCaseWithNumbers(10) }

  fun withName(name: String) = apply {
    this.name = { name }
  }

  override fun produce() = Cru(
    name = this.name(),
  )
}
