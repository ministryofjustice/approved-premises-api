package uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.cas2bail

import com.ninjasquad.springmockk.SpykBean
import io.mockk.clearMocks
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenACas2LicenceCaseAdminUser
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenACas2PomUser
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenAnOffender
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.NomisUserEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.cas2bail.Cas2BailApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.cas2bail.Cas2BailApplicationRepository
import java.time.OffsetDateTime

class Cas2BailApplicationAbandonTest : IntegrationTestBase() {
  @SpykBean
  lateinit var realApplicationRepository: Cas2BailApplicationRepository

  val schema = """
        {
          "${"\$schema"}": "https://json-schema.org/draft/2020-12/schema",
          "${"\$id"}": "https://example.com/product.schema.json",
          "title": "Thing",
          "description": "A thing",
          "type": "object",
          "properties": {
            "thingId": {
              "description": "The unique identifier for a thing",
              "type": "integer"
            }
          },
          "required": [ "thingId" ]
        }
        """

  val data = """
          {
             "thingId": 123
          }
          """

  @AfterEach
  fun afterEach() {
    // SpringMockK does not correctly clear mocks for @SpyKBeans that are also a @Repository, causing mocked behaviour
    // in one test to show up in another (see https://github.com/Ninja-Squad/springmockk/issues/85)
    // Manually clearing after each test seems to fix this.
    clearMocks(realApplicationRepository)
  }

  @Nested
  inner class ControlsOnExternalUsers {

    @ParameterizedTest
    @ValueSource(strings = ["ROLE_CAS2_ASSESSOR", "ROLE_CAS2_MI"])
    fun `abandoning a cas2bail application is forbidden to external users based on role`(role: String) {
      val jwt = jwtAuthHelper.createClientCredentialsJwt(
        username = "username",
        authSource = "nomis",
        roles = listOf(role),
      )

      webTestClient.put()
        .uri("/cas2bail/applications/66911cf0-75b1-4361-84bd-501b176fd4fd/abandon")
        .header("Authorization", "Bearer $jwt")
        .exchange()
        .expectStatus()
        .isForbidden
    }
  }

  @Nested
  inner class MissingJwt {
    @Test
    fun `Abandon a cas2bail application without JWT returns 401`() {
      webTestClient.put()
        .uri("/cas2bail/applications/9b785e59-b85c-4be0-b271-d9ac287684b6/abandon")
        .exchange()
        .expectStatus()
        .isUnauthorized
    }
  }

  @Nested
  inner class PutToAbandon {

    @Nested
    inner class PomUsers {
      @Test
      fun `Abandon existing cas2bail application returns 200 with correct body`() {
        givenACas2PomUser { submittingUser, jwt ->
          givenAnOffender { offenderDetails, _ ->
            val application = produceAndPersistBasicApplication(offenderDetails.otherIds.crn, submittingUser)

            webTestClient.put()
              .uri("/cas2bail/applications/${application.id}/abandon")
              .header("Authorization", "Bearer $jwt")
              .exchange()
              .expectStatus()
              .isOk

            Assertions.assertNotNull(realApplicationRepository.findById(application.id).get().abandonedAt)
          }
        }
      }
    }

    @Nested
    inner class LicenceCaseAdminUsers {
      @Test
      fun `Abandon existing cas2bail application returns 200 with correct body`() {
        givenACas2LicenceCaseAdminUser { submittingUser, jwt ->
          givenAnOffender { offenderDetails, _ ->
            val application = produceAndPersistBasicApplication(offenderDetails.otherIds.crn, submittingUser)

            webTestClient.put()
              .uri("/cas2bail/applications/${application.id}/abandon")
              .header("Authorization", "Bearer $jwt")
              .exchange()
              .expectStatus()
              .isOk

            Assertions.assertNotNull(realApplicationRepository.findById(application.id).get().abandonedAt)
          }
        }
      }
    }
  }

  private fun produceAndPersistBasicApplication(
    crn: String,
    userEntity: NomisUserEntity,
  ): Cas2BailApplicationEntity {
    val jsonSchema = cas2BailApplicationJsonSchemaEntityFactory.produceAndPersist {
      withAddedAt(OffsetDateTime.parse("2022-09-21T12:45:00+01:00"))
      withSchema(
        schema,
      )
    }

    val application = cas2BailApplicationEntityFactory.produceAndPersist {
      withApplicationSchema(jsonSchema)
      withCrn(crn)
      withCreatedByUser(userEntity)
      withData(
        data,
      )
    }

    return application
  }
}
