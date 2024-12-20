package uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.events

import io.github.bluegroundltd.kfactory.Factory
import io.github.bluegroundltd.kfactory.Yielded
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model.MoveOnCategory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.randomStringMultiCaseWithNumbers
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.randomStringUpperCase
import java.util.UUID

class MoveOnCategoryFactory : Factory<MoveOnCategory> {
  private var description: Yielded<String> = { randomStringMultiCaseWithNumbers(10) }
  private var legacyMoveOnCategoryCode: Yielded<String> = { randomStringUpperCase(1) }
  private var id: Yielded<UUID> = { UUID.randomUUID() }

  fun withDescription(description: String) = apply {
    this.description = { description }
  }

  fun withLegacyMoveOnCategoryCode(legacyMoveOnCategoryCode: String) = apply {
    this.legacyMoveOnCategoryCode = { legacyMoveOnCategoryCode }
  }

  fun withId(id: UUID) = apply {
    this.id = { id }
  }

  override fun produce() = MoveOnCategory(
    description = this.description(),
    legacyMoveOnCategoryCode = this.legacyMoveOnCategoryCode(),
    id = this.id(),
  )
}
