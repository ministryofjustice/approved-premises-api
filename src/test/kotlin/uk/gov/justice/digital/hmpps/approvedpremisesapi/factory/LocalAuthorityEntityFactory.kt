package uk.gov.justice.digital.hmpps.approvedpremisesapi.factory

import io.github.bluegroundltd.kfactory.Yielded
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.LocalAuthorityAreaEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.repository.LocalAuthorityAreaTestRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.randomStringMultiCaseWithNumbers
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.randomStringUpperCase
import java.util.UUID

class LocalAuthorityEntityFactory(
  localAuthorityAreaTestRepository: LocalAuthorityAreaTestRepository?
) : PersistedFactory<LocalAuthorityAreaEntity, UUID>(localAuthorityAreaTestRepository) {
  constructor() : this(null)

  private var id: Yielded<UUID> = { UUID.randomUUID() }
  private var identifier: Yielded<String> = { randomStringUpperCase(5) }
  private var name: Yielded<String> = { randomStringMultiCaseWithNumbers(8) }

  fun withId(id: UUID) = apply {
    this.id = { id }
  }

  fun withIdentifier(identifier: String) = apply {
    this.identifier = { identifier }
  }

  fun withName(name: String) = apply {
    this.name = { name }
  }

  override fun produce(): LocalAuthorityAreaEntity = LocalAuthorityAreaEntity(
    id = this.id(),
    identifier = this.identifier(),
    name = this.name(),
    premises = mutableListOf()
  )
}
