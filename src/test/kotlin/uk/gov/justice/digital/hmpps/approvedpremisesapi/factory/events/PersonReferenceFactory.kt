package uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.events

import io.github.bluegroundltd.kfactory.Factory
import io.github.bluegroundltd.kfactory.Yielded
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model.PersonReference
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.randomStringMultiCaseWithNumbers

class PersonReferenceFactory : Factory<PersonReference> {
  private var crn: Yielded<String> = { randomStringMultiCaseWithNumbers(6) }
  private var noms: Yielded<String> = { randomStringMultiCaseWithNumbers(10) }

  fun withCrn(crn: String) = apply {
    this.crn = { crn }
  }

  fun withNoms(noms: String) = apply {
    this.noms = { noms }
  }

  override fun produce() = PersonReference(
    crn = this.crn(),
    noms = this.noms(),
  )
}
