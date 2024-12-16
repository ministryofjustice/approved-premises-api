package uk.gov.justice.digital.hmpps.approvedpremisesapi.integration

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import org.junit.jupiter.params.provider.MethodSource
import org.springframework.beans.factory.annotation.Autowired
import uk.gov.justice.digital.hmpps.approvedpremisesapi.config.DomainEventUrlConfig
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.DomainEventCas
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.DomainEventSchemaVersion
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.DomainEventType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.createCas1DomainEventEnvelopeForSchemaVersion
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.roundNanosToMillisToAccountForLossOfPrecisionInPostgres
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.util.UUID

class DomainEventTest : InitialiseDatabasePerClassTestBase() {
  @Autowired
  lateinit var domainEventUrlConfig: DomainEventUrlConfig

  private fun generateUrlForDomainEventType(domainEventType: DomainEventType, uuid: UUID) = domainEventUrlConfig.getUrlForDomainEventId(domainEventType, uuid)
    .replace("http://api", "")

  @ParameterizedTest
  @EnumSource(DomainEventType::class, names = ["APPROVED_PREMISES_.+"], mode = EnumSource.Mode.MATCH_ANY)
  fun `Get event without JWT returns 401`(domainEventType: DomainEventType) {
    val url = generateUrlForDomainEventType(domainEventType, UUID.randomUUID())

    webTestClient.get()
      .uri(url)
      .exchange()
      .expectStatus()
      .isUnauthorized
  }

  @ParameterizedTest
  @EnumSource(DomainEventType::class, names = ["APPROVED_PREMISES_.+"], mode = EnumSource.Mode.MATCH_ANY)
  fun `Get event without ROLE_APPROVED_PREMISES_EVENTS returns 403`(domainEventType: DomainEventType) {
    val url = generateUrlForDomainEventType(domainEventType, UUID.randomUUID())
    val jwt = jwtAuthHelper.createClientCredentialsJwt(
      username = "username",
    )

    webTestClient.get()
      .uri(url)
      .header("Authorization", "Bearer $jwt")
      .exchange()
      .expectStatus()
      .isForbidden
  }

  private companion object {
    @JvmStatic
    fun allCas1DomainEventTypes() = DomainEventType.values().filter { it.cas == DomainEventCas.CAS1 }

    @JvmStatic
    fun allDomainEventTypesAndVersions() = DomainEventType
      .entries
      .filter { it.cas == DomainEventCas.CAS1 }
      .flatMap { type -> type.schemaVersions.map { DomainEventTypeAndVersion(type, it) } }
  }

  data class DomainEventTypeAndVersion(val type: DomainEventType, val version: DomainEventSchemaVersion)

  @ParameterizedTest
  @MethodSource("allDomainEventTypesAndVersions")
  fun `Get event returns 200 with correct body`(domainEventTypeAndVersion: DomainEventTypeAndVersion) {
    val domainEventType = domainEventTypeAndVersion.type
    val version = domainEventTypeAndVersion.version

    val jwt = jwtAuthHelper.createClientCredentialsJwt(
      username = "username",
      roles = listOf("ROLE_APPROVED_PREMISES_EVENTS"),
    )

    val now = LocalDateTime.now()
    clock.setNow(now.roundNanosToMillisToAccountForLossOfPrecisionInPostgres())

    val domainEventAndJson = createCas1DomainEventEnvelopeForSchemaVersion(
      type = domainEventType,
      objectMapper = objectMapper,
      occurredAt = clock.instant(),
      schemaVersion = version,
    )

    val event = domainEventFactory.produceAndPersist {
      withType(domainEventType)
      withData(domainEventAndJson.persistedJson)
      withOccurredAt(clock.instant().atOffset(ZoneOffset.UTC))
      withSchemaVersion(version.versionNo)
    }

    val url = generateUrlForDomainEventType(domainEventType, event.id)

    val response = webTestClient.get()
      .uri(url)
      .header("Authorization", "Bearer $jwt")
      .exchange()
      .expectStatus()
      .isOk
      .expectBody(domainEventAndJson.envelope::class.java)
      .returnResult()

    val xxx = response.responseBody

    assertThat(response.responseBody).isEqualTo(domainEventAndJson.envelope)
  }
}
