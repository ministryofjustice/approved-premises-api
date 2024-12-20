package uk.gov.justice.digital.hmpps.approvedpremisesapi.integration

import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.CaseDetailFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.CaseDetailOffenceFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.from
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenAUser
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenAnOffender
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.httpmocks.apDeliusContextMockSuccessfulCaseDetailCall
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.OffenceTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.asCaseDetail
import java.time.LocalDate

class PersonOffencesTest : InitialiseDatabasePerClassTestBase() {
  @Autowired
  lateinit var offenceTransformer: OffenceTransformer

  @Test
  fun `Getting offences by CRN without a JWT returns 401`() {
    webTestClient.get()
      .uri("/people/CRN/offences")
      .exchange()
      .expectStatus()
      .isUnauthorized
  }

  @Test
  fun `Getting offences for a CRN with a non-Delius JWT returns 403`() {
    val jwt = jwtAuthHelper.createClientCredentialsJwt(
      username = "username",
      authSource = "nomis",
    )

    webTestClient.get()
      .uri("/people/CRN/offences")
      .header("Authorization", "Bearer $jwt")
      .exchange()
      .expectStatus()
      .isForbidden
  }

  @Test
  fun `Getting offences for a CRN without ROLE_PROBATION returns 403`() {
    val jwt = jwtAuthHelper.createAuthorizationCodeJwt(
      subject = "username",
      authSource = "delius",
    )

    webTestClient.get()
      .uri("/people/CRN/offences")
      .header("Authorization", "Bearer $jwt")
      .exchange()
      .expectStatus()
      .isForbidden
  }

  @Test
  fun `Getting offences for a CRN that does not exist returns 404`() {
    givenAUser { userEntity, jwt ->
      val crn = "CRN123"

      webTestClient.get()
        .uri("/people/$crn/offences")
        .header("Authorization", "Bearer $jwt")
        .exchange()
        .expectStatus()
        .isNotFound
    }
  }

  @Test
  fun `Getting offences for a CRN - returns OK with correct body`() {
    givenAUser { userEntity, jwt ->
      givenAnOffender { offenderDetails, _ ->

        val caseDetail = CaseDetailFactory()
          .from(offenderDetails.asCaseDetail())
          .withOffences(
            listOf(
              CaseDetailOffenceFactory()
                .withId("M1")
                .withDescription("Test Offence 1")
                .withMainCategoryDescription("Main cat desc 1")
                .withSubCategoryDescription("Sub cat desc 1")
                .withDate(LocalDate.now().minusMonths(1))
                .withEventNumber("123")
                .withEventId(10)
                .produce(),
            ),
          )
          .produce()

        apDeliusContextMockSuccessfulCaseDetailCall(
          offenderDetails.otherIds.crn,
          caseDetail,
        )

        webTestClient.get()
          .uri("/people/${offenderDetails.otherIds.crn}/offences")
          .header("Authorization", "Bearer $jwt")
          .exchange()
          .expectStatus()
          .isOk
          .expectBody()
          .json(
            objectMapper.writeValueAsString(offenceTransformer.transformToApi(caseDetail)),
          )
      }
    }
  }
}
