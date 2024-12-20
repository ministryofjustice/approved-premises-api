package uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.events

import io.github.bluegroundltd.kfactory.Factory
import io.github.bluegroundltd.kfactory.Yielded
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model.DestinationPremises
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model.DestinationProvider
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model.MoveOnCategory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model.PersonDepartedDestination

class PersonDepartedDestinationFactory : Factory<PersonDepartedDestination> {
  private var premises: Yielded<DestinationPremises?> = { null }
  private var moveOnCategory: Yielded<MoveOnCategory> = { MoveOnCategoryFactory().produce() }
  private var destinationProvider: Yielded<DestinationProvider> = { DestinationProviderFactory().produce() }

  fun withPremises(premises: DestinationPremises?) = apply {
    this.premises = { premises }
  }

  fun withMoveOnCategory(moveOnCategory: MoveOnCategory) = apply {
    this.moveOnCategory = { moveOnCategory }
  }

  fun withDestinationProvider(destinationProvider: DestinationProvider) = apply {
    this.destinationProvider = { destinationProvider }
  }

  override fun produce() = PersonDepartedDestination(
    premises = this.premises(),
    moveOnCategory = this.moveOnCategory(),
    destinationProvider = this.destinationProvider(),
  )
}
