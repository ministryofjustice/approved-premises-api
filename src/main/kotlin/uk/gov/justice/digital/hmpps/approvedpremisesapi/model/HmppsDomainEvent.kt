package uk.gov.justice.digital.hmpps.approvedpremisesapi.model

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonSetter
import com.fasterxml.jackson.annotation.Nulls
import java.time.ZonedDateTime

data class HmppsDomainEvent(
  val eventType: String,
  val version: Int,
  val detailUrl: String? = null,
  val occurredAt: ZonedDateTime = ZonedDateTime.now(),
  val description: String? = null,
  @JsonSetter(nulls = Nulls.SKIP)
  val additionalInformation: Map<String, Any>? = mapOf(),
  val personReference: HmppsDomainEventPersonReference = HmppsDomainEventPersonReference(),
) {
  val prisonId = additionalInformation?.get("prisonId") as String?
  val staffCode = additionalInformation?.get("staffCode") as String?
}

data class AllocationData(
  val prisonId: String?,
  val staffCode: String?
)

data class HmppsDomainEventPersonReference(val identifiers: List<PersonIdentifier> = listOf()) {
  fun findCrn() = get("CRN")
  fun findNomsNumber() = get("NOMS")
  operator fun get(key: String) = identifiers.find { it.type == key }?.value
}
data class PersonIdentifier(val type: String, val value: String)

data class SQSMessage(
  @JsonProperty("Message") val message: String,
)