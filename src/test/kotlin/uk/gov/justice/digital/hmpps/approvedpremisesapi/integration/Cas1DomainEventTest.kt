package uk.gov.justice.digital.hmpps.approvedpremisesapi.integration

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import org.junit.jupiter.params.provider.MethodSource
import org.springframework.beans.factory.annotation.Autowired
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model.BookingCancelledEnvelope
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model.PersonArrivedEnvelope
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model.PersonDepartedEnvelope
import uk.gov.justice.digital.hmpps.approvedpremisesapi.config.DomainEventUrlConfig
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenAUser
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.DomainEventCas
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.DomainEventSchemaVersion
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.DomainEventType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.Cas1DomainEventsFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.bodyAsObject
import java.time.ZoneOffset
import java.util.UUID

class Cas1DomainEventTest : InitialiseDatabasePerClassTestBase() {
  @Autowired
  lateinit var domainEventUrlConfig: DomainEventUrlConfig

  lateinit var domainEventsFactory: Cas1DomainEventsFactory

  private fun generateUrlForDomainEventType(domainEventType: DomainEventType, uuid: UUID) = domainEventUrlConfig.getUrlForDomainEventId(domainEventType, uuid)
    .replace("http://api", "")

  @BeforeEach
  fun setup() {
    clock.setToNowWithoutMillis()

    domainEventsFactory = Cas1DomainEventsFactory(objectMapper)
  }

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
  }

  data class DomainEventTypeAndVersion(val type: DomainEventType, val version: DomainEventSchemaVersion)

  @ParameterizedTest
  @MethodSource("allCas1DomainEventTypes")
  fun `Get event returns 200 with correct body for all types and latest schema versions`(type: DomainEventType) {
    val jwt = jwtAuthHelper.createClientCredentialsJwt(
      username = "username",
      roles = listOf("ROLE_APPROVED_PREMISES_EVENTS"),
    )

    val domainEventAndJson = domainEventsFactory.createEnvelopeLatestVersion(
      type = type,
      occurredAt = clock.instant(),
    )

    val event = domainEventFactory.produceAndPersist {
      withType(type)
      withData(domainEventAndJson.persistedJson)
      withOccurredAt(clock.instant().atOffset(ZoneOffset.UTC))
      withSchemaVersion(domainEventAndJson.schemaVersion.versionNo)
    }

    val url = generateUrlForDomainEventType(type, event.id)

    val response = webTestClient.get()
      .uri(url)
      .header("Authorization", "Bearer $jwt")
      .exchange()
      .expectStatus()
      .isOk
      .expectBody(domainEventAndJson.envelope::class.java)
      .returnResult()

    assertThat(response.responseBody).isEqualTo(domainEventAndJson.envelope)
  }

  @Test
  fun `Get APPROVED_PREMISES_BOOKING_CANCELLED v1 is correctly migrated to v2 by deriving cancelled at date fields`() {
    val jwt = jwtAuthHelper.createClientCredentialsJwt(
      username = "username",
      roles = listOf("ROLE_APPROVED_PREMISES_EVENTS"),
    )

    val domainEventAndJson = domainEventsFactory.createEnvelopeLatestVersion(
      type = DomainEventType.APPROVED_PREMISES_BOOKING_CANCELLED,
      occurredAt = clock.instant(),
    )

    val persistedJson = domainEventsFactory.removeEventDetails(
      objectMapper.writeValueAsString(domainEventAndJson.envelope),
      listOf("cancelledAtDate", "cancellationRecordedAt"),
    )

    val event = domainEventFactory.produceAndPersist {
      withType(DomainEventType.APPROVED_PREMISES_BOOKING_CANCELLED)
      withData(persistedJson)
      withOccurredAt(clock.instant().atOffset(ZoneOffset.UTC))
      withSchemaVersion(1)
    }

    val url = generateUrlForDomainEventType(DomainEventType.APPROVED_PREMISES_BOOKING_CANCELLED, event.id)

    val response = webTestClient.get()
      .uri(url)
      .header("Authorization", "Bearer $jwt")
      .exchange()
      .expectStatus()
      .isOk
      .bodyAsObject<BookingCancelledEnvelope>()

    val expectedEnvelope = (domainEventAndJson.envelope as BookingCancelledEnvelope)
    assertThat(response.eventDetails.cancelledAtDate).isEqualTo(expectedEnvelope.eventDetails.cancelledAtDate)
    assertThat(response.eventDetails.cancellationRecordedAt).isEqualTo(expectedEnvelope.eventDetails.cancellationRecordedAt)
  }

  @Test
  fun `Get APPROVED_PREMISES_PERSON_ARRIVED v1 is correctly migrated to v2 by populating recordedBy from triggered by user`() {
    val (user, _) = givenAUser()

    val jwt = jwtAuthHelper.createClientCredentialsJwt(
      username = "username",
      roles = listOf("ROLE_APPROVED_PREMISES_EVENTS"),
    )

    val domainEventAndJson = domainEventsFactory.createEnvelopeLatestVersion(
      type = DomainEventType.APPROVED_PREMISES_PERSON_ARRIVED,
      occurredAt = clock.instant(),
    )

    val persistedJson = domainEventsFactory.removeEventDetails(
      objectMapper.writeValueAsString(domainEventAndJson.envelope),
      listOf("recordedBy"),
    )

    val event = domainEventFactory.produceAndPersist {
      withType(DomainEventType.APPROVED_PREMISES_PERSON_ARRIVED)
      withData(persistedJson)
      withOccurredAt(clock.instant().atOffset(ZoneOffset.UTC))
      withSchemaVersion(1)
      withTriggeredByUserId(user.id)
    }

    val url = generateUrlForDomainEventType(DomainEventType.APPROVED_PREMISES_PERSON_ARRIVED, event.id)

    val response = webTestClient.get()
      .uri(url)
      .header("Authorization", "Bearer $jwt")
      .exchange()
      .expectStatus()
      .isOk
      .bodyAsObject<PersonArrivedEnvelope>()

    val recordedBy = response.eventDetails.recordedBy
    assertThat(recordedBy.username).isEqualTo(user.deliusUsername)
    assertThat(recordedBy.staffCode).isEqualTo(user.deliusStaffCode)
    assertThat(recordedBy.forenames).isEqualTo(user.name)
    assertThat(recordedBy.surname).isEqualTo("unknown")
  }

  @Test
  fun `Get APPROVED_PREMISES_PERSON_DEPARTED v1 is correctly migrated to v2 by populating recordedBy from triggered by user`() {
    val (user, _) = givenAUser()

    val jwt = jwtAuthHelper.createClientCredentialsJwt(
      username = "username",
      roles = listOf("ROLE_APPROVED_PREMISES_EVENTS"),
    )

    val domainEventAndJson = domainEventsFactory.createEnvelopeLatestVersion(
      type = DomainEventType.APPROVED_PREMISES_PERSON_DEPARTED,
      occurredAt = clock.instant(),
    )

    val persistedJson = domainEventsFactory.removeEventDetails(
      objectMapper.writeValueAsString(domainEventAndJson.envelope),
      listOf("recordedBy"),
    )

    val event = domainEventFactory.produceAndPersist {
      withType(DomainEventType.APPROVED_PREMISES_PERSON_DEPARTED)
      withData(persistedJson)
      withOccurredAt(clock.instant().atOffset(ZoneOffset.UTC))
      withSchemaVersion(1)
      withTriggeredByUserId(user.id)
    }

    val url = generateUrlForDomainEventType(DomainEventType.APPROVED_PREMISES_PERSON_DEPARTED, event.id)

    val response = webTestClient.get()
      .uri(url)
      .header("Authorization", "Bearer $jwt")
      .exchange()
      .expectStatus()
      .isOk
      .bodyAsObject<PersonDepartedEnvelope>()

    val recordedBy = response.eventDetails.recordedBy
    assertThat(recordedBy.username).isEqualTo(user.deliusUsername)
    assertThat(recordedBy.staffCode).isEqualTo(user.deliusStaffCode)
    assertThat(recordedBy.forenames).isEqualTo(user.name)
    assertThat(recordedBy.surname).isEqualTo("unknown")
  }
}
