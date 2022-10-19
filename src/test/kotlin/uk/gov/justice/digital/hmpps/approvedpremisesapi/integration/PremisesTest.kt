package uk.gov.justice.digital.hmpps.approvedpremisesapi.integration

import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.client.WireMock.exactly
import com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor
import com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.NewPremises
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.StaffMemberFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.PremisesTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.StaffMemberTransformer
import java.time.LocalDate
import java.util.UUID

class PremisesTest : IntegrationTestBase() {
  @Autowired
  lateinit var premisesTransformer: PremisesTransformer

  @Autowired
  lateinit var staffMemberTransformer: StaffMemberTransformer

  @Test
  fun `Create new premises returns 201`() {

    val jwt = jwtAuthHelper.createValidAuthorizationCodeJwt("PROBATIONPERSON")

    webTestClient.post()
      .uri("/premises")
      .header("Authorization", "Bearer $jwt")
      .bodyValue(
        NewPremises(
          addressLine1 = "1 somewhere",
          postcode = "AB123CD",
          service = "CAS3"
        )
      )
      .exchange()
      .expectStatus()
      .isCreated
  }

  @Test
  fun `When a new premises is created then all field data is persisted`() {

    val jwt = jwtAuthHelper.createValidAuthorizationCodeJwt("PROBATIONPERSON")

    webTestClient.post()
      .uri("/premises")
      .header("Authorization", "Bearer $jwt")
      .bodyValue(
        NewPremises(
          addressLine1 = "1 somewhere",
          postcode = "AB123CD",
          service = "CAS3",
          notes = "some arbitrary notes",
          name = "some arbitrary name"
        )
      )
      .exchange()
      .expectStatus()
      .isCreated
      .expectBody()
      .jsonPath("addressLine1").isEqualTo("1 somewhere")
      .jsonPath("postcode").isEqualTo("AB123CD")
      .jsonPath("service").isEqualTo("CAS3")
      .jsonPath("notes").isEqualTo("some arbitrary notes")
      .jsonPath("name").isEqualTo("some arbitrary name")
  }

  @Test
  fun `When a new premises is created with no name then it defaults to unknown`() {

    val jwt = jwtAuthHelper.createValidAuthorizationCodeJwt("PROBATIONPERSON")

    webTestClient.post()
      .uri("/premises")
      .header("Authorization", "Bearer $jwt")
      .bodyValue(
        NewPremises(
          addressLine1 = "1 somewhere",
          postcode = "AB123CD",
          service = "CAS3",
          notes = "some arbitrary notes"
        )
      )
      .exchange()
      .expectStatus()
      .isCreated
      .expectBody()
      .jsonPath("name").isEqualTo("Unknown")
  }

  @Test
  fun `Get all Premises returns OK with correct body`() {
    val premises = premisesEntityFactory.produceAndPersistMultiple(10) {
      withYieldedLocalAuthorityArea { localAuthorityEntityFactory.produceAndPersist() }
      withYieldedProbationRegion {
        probationRegionEntityFactory.produceAndPersist { withYieldedApArea { apAreaEntityFactory.produceAndPersist() } }
      }
      withTotalBeds(20)
    }

    val expectedJson = objectMapper.writeValueAsString(
      premises.map {
        premisesTransformer.transformJpaToApi(it, 20)
      }
    )

    val jwt = jwtAuthHelper.createValidAuthorizationCodeJwt()

    webTestClient.get()
      .uri("/premises")
      .header("Authorization", "Bearer $jwt")
      .exchange()
      .expectStatus()
      .isOk
      .expectBody()
      .json(expectedJson)
  }

  @Test
  fun `Get Premises by ID returns OK with correct body`() {
    val premises = premisesEntityFactory.produceAndPersistMultiple(5) {
      withYieldedLocalAuthorityArea { localAuthorityEntityFactory.produceAndPersist() }
      withYieldedProbationRegion {
        probationRegionEntityFactory.produceAndPersist { withYieldedApArea { apAreaEntityFactory.produceAndPersist() } }
      }
      withTotalBeds(20)
    }

    val premisesToGet = premises[2]
    val expectedJson = objectMapper.writeValueAsString(premisesTransformer.transformJpaToApi(premises[2], 20))

    val jwt = jwtAuthHelper.createValidAuthorizationCodeJwt()

    webTestClient.get()
      .uri("/premises/${premisesToGet.id}")
      .header("Authorization", "Bearer $jwt")
      .exchange()
      .expectStatus()
      .isOk
      .expectBody()
      .json(expectedJson)
  }

  @Test
  fun `Get Premises by ID returns OK with correct body when capacity is used`() {
    val premises = premisesEntityFactory.produceAndPersistMultiple(5) {
      withYieldedLocalAuthorityArea { localAuthorityEntityFactory.produceAndPersist() }
      withYieldedProbationRegion {
        probationRegionEntityFactory.produceAndPersist { withYieldedApArea { apAreaEntityFactory.produceAndPersist() } }
      }
      withTotalBeds(20)
    }

    val keyWorker = StaffMemberFactory().produce()
    mockStaffMemberCommunityApiCall(keyWorker)

    bookingEntityFactory.produceAndPersist {
      withPremises(premises[2])
      withArrivalDate(LocalDate.now().minusDays(2))
      withDepartureDate(LocalDate.now().plusDays(4))
      withStaffKeyWorkerId(keyWorker.staffIdentifier)
    }

    val premisesToGet = premises[2]
    val expectedJson = objectMapper.writeValueAsString(premisesTransformer.transformJpaToApi(premises[2], 19))

    val jwt = jwtAuthHelper.createValidAuthorizationCodeJwt()

    webTestClient.get()
      .uri("/premises/${premisesToGet.id}")
      .header("Authorization", "Bearer $jwt")
      .exchange()
      .expectStatus()
      .isOk
      .expectBody()
      .json(expectedJson)
  }

  @Test
  fun `Get Premises by ID returns Not Found with correct body`() {
    val idToRequest = UUID.randomUUID().toString()

    val jwt = jwtAuthHelper.createValidAuthorizationCodeJwt()

    webTestClient.get()
      .uri("/premises/$idToRequest")
      .header("Authorization", "Bearer $jwt")
      .exchange()
      .expectHeader().contentType("application/problem+json")
      .expectStatus()
      .isNotFound
      .expectBody()
      .jsonPath("title").isEqualTo("Not Found")
      .jsonPath("status").isEqualTo(404)
      .jsonPath("detail").isEqualTo("No Premises with an ID of $idToRequest could be found")
  }

  @Test
  fun `Get Premises Staff without JWT returns 401`() {
    webTestClient.get()
      .uri("/premises/e0f03aa2-1468-441c-aa98-0b98d86b67f9/staff")
      .exchange()
      .expectStatus()
      .isUnauthorized
  }

  @Test
  fun `Get Premises Staff where delius team cannot be found returns 500`() {
    val deliusTeamCode = "NOTFOUND"

    val premises = premisesEntityFactory.produceAndPersist {
      withYieldedLocalAuthorityArea { localAuthorityEntityFactory.produceAndPersist() }
      withYieldedProbationRegion {
        probationRegionEntityFactory.produceAndPersist { withYieldedApArea { apAreaEntityFactory.produceAndPersist() } }
      }
      withDeliusTeamCode(deliusTeamCode)
    }

    val jwt = jwtAuthHelper.createValidAuthorizationCodeJwt()

    mockClientCredentialsJwtRequest()

    wiremockServer.stubFor(
      WireMock.get(WireMock.urlEqualTo("/secure/teams/$deliusTeamCode/staff"))
        .willReturn(
          WireMock.aResponse()
            .withHeader("Content-Type", "application/json")
            .withStatus(404)
        )
    )

    webTestClient.get()
      .uri("/premises/${premises.id}/staff")
      .header("Authorization", "Bearer $jwt")
      .exchange()
      .expectStatus()
      .is5xxServerError
      .expectBody()
      .jsonPath("$.detail").isEqualTo("No team found for Delius team code: ${premises.deliusTeamCode}")
  }

  @Test
  fun `Get Premises Staff returns 200 with correct body`() {
    val deliusTeamCode = "FOUND"

    val premises = premisesEntityFactory.produceAndPersist {
      withYieldedLocalAuthorityArea { localAuthorityEntityFactory.produceAndPersist() }
      withYieldedProbationRegion {
        probationRegionEntityFactory.produceAndPersist { withYieldedApArea { apAreaEntityFactory.produceAndPersist() } }
      }
      withDeliusTeamCode(deliusTeamCode)
    }

    val staffMembers = listOf(
      StaffMemberFactory().produce(),
      StaffMemberFactory().produce(),
      StaffMemberFactory().produce(),
      StaffMemberFactory().produce(),
      StaffMemberFactory().produce()
    )

    mockClientCredentialsJwtRequest()

    val jwt = jwtAuthHelper.createValidAuthorizationCodeJwt()

    wiremockServer.stubFor(
      WireMock.get(WireMock.urlEqualTo("/secure/teams/$deliusTeamCode/staff"))
        .willReturn(
          WireMock.aResponse()
            .withHeader("Content-Type", "application/json")
            .withStatus(200)
            .withBody(
              objectMapper.writeValueAsString(staffMembers)
            )
        )
    )

    webTestClient.get()
      .uri("/premises/${premises.id}/staff")
      .header("Authorization", "Bearer $jwt")
      .exchange()
      .expectStatus()
      .isOk
      .expectBody()
      .json(
        objectMapper.writeValueAsString(
          staffMembers.map(staffMemberTransformer::transformDomainToApi)
        )
      )
  }

  @Test
  fun `Get Premises Staff caches response`() {
    val deliusTeamCode = "FOUND"

    val premises = premisesEntityFactory.produceAndPersist {
      withYieldedLocalAuthorityArea { localAuthorityEntityFactory.produceAndPersist() }
      withYieldedProbationRegion {
        probationRegionEntityFactory.produceAndPersist { withYieldedApArea { apAreaEntityFactory.produceAndPersist() } }
      }
      withDeliusTeamCode(deliusTeamCode)
    }

    val staffMembers = listOf(
      StaffMemberFactory().produce(),
      StaffMemberFactory().produce(),
      StaffMemberFactory().produce(),
      StaffMemberFactory().produce(),
      StaffMemberFactory().produce()
    )

    mockClientCredentialsJwtRequest()

    val jwt = jwtAuthHelper.createValidAuthorizationCodeJwt()

    wiremockServer.stubFor(
      WireMock.get(WireMock.urlEqualTo("/secure/teams/$deliusTeamCode/staff"))
        .willReturn(
          WireMock.aResponse()
            .withHeader("Content-Type", "application/json")
            .withStatus(200)
            .withBody(
              objectMapper.writeValueAsString(staffMembers)
            )
        )
    )

    repeat(2) {
      webTestClient.get()
        .uri("/premises/${premises.id}/staff")
        .header("Authorization", "Bearer $jwt")
        .exchange()
        .expectStatus()
        .isOk
        .expectBody()
        .json(
          objectMapper.writeValueAsString(
            staffMembers.map(staffMemberTransformer::transformDomainToApi)
          )
        )
    }

    wiremockServer.verify(exactly(1), getRequestedFor(urlEqualTo("/secure/teams/$deliusTeamCode/staff")))
  }
}
