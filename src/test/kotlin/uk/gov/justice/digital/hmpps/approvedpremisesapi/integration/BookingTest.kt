package uk.gov.justice.digital.hmpps.approvedpremisesapi.integration

import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.web.reactive.server.expectBodyList
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.community.Person
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.BookingTransformer

class BookingTest : IntegrationTestBase() {
  @Autowired
  lateinit var bookingTransformer: BookingTransformer

  @Test
  fun `Get all Bookings without JWT returns 401`() {
    webTestClient.get()
      .uri("/premises/e0f03aa2-1468-441c-aa98-0b98d86b67f9/bookings")
      .exchange()
      .expectStatus()
      .isUnauthorized
  }

  @Test
  fun `Get all Bookings on non existent Premises returns 404`() {
    val jwt = jwtAuthHelper.createValidJwt()

    webTestClient.get()
      .uri("/premises/9054b6a8-65ad-4d55-91ee-26ba65e05488/bookings")
      .header("Authorization", "Bearer $jwt")
      .exchange()
      .expectStatus()
      .isNotFound
  }

  @Test
  fun `Get all Bookings on Premises without any Bookings returns empty list`() {
    val premises = premisesEntityFactory
      .withYieldedApArea { apAreaEntityFactory.produceAndPersist() }
      .withYieldedLocalAuthorityArea { localAuthorityEntityFactory.produceAndPersist() }
      .withYieldedProbationRegion { probationRegionEntityFactory.produceAndPersist() }
      .produceAndPersist()

    val jwt = jwtAuthHelper.createValidJwt()

    webTestClient.get()
      .uri("/premises/${premises.id}/bookings")
      .header("Authorization", "Bearer $jwt")
      .exchange()
      .expectStatus()
      .isOk
      .expectBodyList<Any>()
      .hasSize(0)
  }

  @Test
  fun `Get all Bookings returns OK with correct body`() {
    val premises = premisesEntityFactory
      .withYieldedApArea { apAreaEntityFactory.produceAndPersist() }
      .withYieldedLocalAuthorityArea { localAuthorityEntityFactory.produceAndPersist() }
      .withYieldedProbationRegion { probationRegionEntityFactory.produceAndPersist() }
      .produceAndPersist()

    val bookings = bookingEntityFactory
      .withPremises(premises)
      .withYieldedKeyWorker { keyWorkerEntityFactory.produceAndPersist() }
      .produceAndPersistMultiple(5)

    bookings[1].let { it.arrival = arrivalEntityFactory.withBooking(it).produceAndPersist() }
    bookings[2].let {
      it.arrival = arrivalEntityFactory.withBooking(it).produceAndPersist()
      it.departure = departureEntityFactory
        .withBooking(it)
        .withYieldedDestinationProvider { destinationProviderEntityFactory.produceAndPersist() }
        .withYieldedReason { departureReasonEntityFactory.produceAndPersist() }
        .withYieldedMoveOnCategory { moveOnCategoryEntityFactory.produceAndPersist() }
        .produceAndPersist()
    }
    bookings[3].let { it.cancellation = cancellationEntityFactory.withBooking(it).produceAndPersist() }
    bookings[4].let { it.nonArrival = nonArrivalEntityFactory.withBooking(it).produceAndPersist() }

    val expectedJson = objectMapper.writeValueAsString(
      bookings.map {
        // TODO: Once client to Community API is in place, replace the Person with an entityFactory connected to a mock client
        bookingTransformer.transformJpaToApi(it, Person(crn = it.crn, name = "Mock Person", isActive = true))
      }
    )

    val jwt = jwtAuthHelper.createValidJwt()

    webTestClient.get()
      .uri("/premises/${premises.id}/bookings")
      .header("Authorization", "Bearer $jwt")
      .exchange()
      .expectStatus()
      .isOk
      .expectBody()
      .json(expectedJson)
  }
}
