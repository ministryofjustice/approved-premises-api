package uk.gov.justice.digital.hmpps.approvedpremisesapi.factory

import io.github.bluegroundltd.kfactory.Factory
import io.github.bluegroundltd.kfactory.Yielded
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.Cas2v2ApplicationJsonSchemaEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.randomDateTimeBefore
import java.time.OffsetDateTime
import java.util.UUID

class Cas2v2ApplicationJsonSchemaEntityFactory : Factory<Cas2v2ApplicationJsonSchemaEntity> {
  private var id: Yielded<UUID> = { UUID.randomUUID() }
  private var addedAt: Yielded<OffsetDateTime> = { OffsetDateTime.now().randomDateTimeBefore(7) }
  private var schema: Yielded<String> = { "{}" }

  fun withId(id: UUID) = apply {
    this.id = { id }
  }

  fun withAddedAt(addedAt: OffsetDateTime) = apply {
    this.addedAt = { addedAt }
  }

  fun withSchema(schema: String) = apply {
    this.schema = { schema }
  }

  fun withPermissiveSchema() = apply {
    withSchema(
      """
        {
          "${"\$schema"}": "https://json-schema.org/draft/2020-12/schema",
          "${"\$id"}": "https://example.com/product.schema.json",
          "title": "Thing",
          "description": "A thing",
          "type": "object",
          "properties": { }
        }
        """,
    )
  }

  override fun produce(): Cas2v2ApplicationJsonSchemaEntity = Cas2v2ApplicationJsonSchemaEntity(
    id = this.id(),
    addedAt = this.addedAt(),
    schema = this.schema(),
  )
}
