package uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.cas2bail

import com.fasterxml.jackson.core.type.TypeReference
import com.ninjasquad.springmockk.SpykBean
import io.mockk.clearMocks
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.test.web.reactive.server.returnResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas2Assessment
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ServiceName
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.UpdateCas2Assessment
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenACas2Admin
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenACas2Assessor
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenACas2PomUser
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.NomisUserEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.cas2bail.Cas2BailApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.cas2bail.Cas2BailAssessmentRepository
import java.time.OffsetDateTime
import java.util.UUID

class Cas2BailAssessmentTest : IntegrationTestBase() {

  @SpykBean
  lateinit var realAssessmentRepository: Cas2BailAssessmentRepository

  @AfterEach
  fun afterEach() {
    // SpringMockK does not correctly clear mocks for @SpyKBeans that are also a @Repository, causing mocked behaviour
    // in one test to show up in another (see https://github.com/Ninja-Squad/springmockk/issues/85)
    // Manually clearing after each test seems to fix this.
    clearMocks(realAssessmentRepository)
  }

  @Nested
  inner class PutToUpdate {
    @Nested
    inner class MissingJwt {
      @Test
      fun `updating a cas2bail assessment without JWT returns 401`() {
        webTestClient.put()
          .uri("/cas2bail/assessments/de6512fc-a225-4109-bdcd-86c6307a5237")
          .exchange()
          .expectStatus()
          .isUnauthorized
      }
    }

    @Nested
    inner class ControlsOnExternalUsers {
      @Test
      fun `updating a cas2bail assessment is forbidden to external users who are not Assessors`() {
        val jwt = jwtAuthHelper.createClientCredentialsJwt(
          username = "username",
          authSource = "auth",
          roles = listOf("ROLE_CAS2_ADMIN"),
        )

        webTestClient.put()
          .uri("/cas2bail/assessments/de6512fc-a225-4109-bdcd-86c6307a5237")
          .header("Authorization", "Bearer $jwt")
          .exchange()
          .expectStatus()
          .isForbidden
      }
    }

    @Nested
    inner class ControlsOnInternalUsers {
      @Test
      fun `updating a cas2bail assessment is forbidden to nomis users`() {
        val jwt = jwtAuthHelper.createClientCredentialsJwt(
          username = "username",
          authSource = "nomis",
          roles = listOf("ROLE_POM"),
        )

        webTestClient.put()
          .uri("/cas2bail/assessments/de6512fc-a225-4109-bdcd-86c6307a5237")
          .header("Authorization", "Bearer $jwt")
          .exchange()
          .expectStatus()
          .isForbidden
      }
    }

    @Test
    fun `assessors create cas2bail note returns 201`() {
      val applicationId = UUID.fromString("22ceda56-98b2-411d-91cc-ace0ab8be872")

      givenACas2PomUser { referrer, _ ->
        givenACas2Assessor { assessor, jwt ->
          val submittedApplication = createSubmittedApplication(applicationId, referrer)

          // with an assessment
          val assessment = cas2BailAssessmentEntityFactory.produceAndPersist {
            withApplication(submittedApplication)
            withNacroReferralId("someID")
            withAssessorName("some name")
          }

          val updatedNacroReferralId = "123N"
          val updatedAssessorName = "Anne Assessor"

          val rawResponseBody = webTestClient.put()
            .uri("/cas2bail/assessments/${assessment.id}")
            .header("Authorization", "Bearer $jwt")
            .header("X-Service-Name", ServiceName.cas2.value)
            .bodyValue(
              UpdateCas2Assessment(
                nacroReferralId = updatedNacroReferralId,
                assessorName = updatedAssessorName,
              ),
            )
            .exchange()
            .expectStatus()
            .isOk
            .returnResult<String>()
            .responseBody
            .blockFirst()

          val responseBody =
            objectMapper.readValue(rawResponseBody, object : TypeReference<Cas2Assessment>() {})

          Assertions.assertThat(responseBody.nacroReferralId).isEqualTo(updatedNacroReferralId)
          Assertions.assertThat(responseBody.assessorName).isEqualTo(updatedAssessorName)
        }
      }
    }

    private fun createSubmittedApplication(applicationId: UUID, referrer: NomisUserEntity): Cas2BailApplicationEntity {
      val applicationSchema =
        cas2BailApplicationJsonSchemaEntityFactory.produceAndPersist()

      return cas2BailApplicationEntityFactory.produceAndPersist {
        withId(applicationId)
        withCreatedByUser(referrer)
        withApplicationSchema(applicationSchema)
        withSubmittedAt(OffsetDateTime.now())
      }
    }
  }

  @Nested
  inner class GetToShow {
    @Nested
    inner class MissingJwt {
      @Test
      fun `getting a cas2bail assessment without JWT returns 401`() {
        webTestClient.get()
          .uri("/cas2bail/assessments/de6512fc-a225-4109-bdcd-86c6307a5237")
          .exchange()
          .expectStatus()
          .isUnauthorized
      }
    }

    @Nested
    inner class ControlsOnExternalUsers {
      @Test
      fun `getting a cas2bail assessment is forbidden to external users who are not Assessors or Admins`() {
        val jwt = jwtAuthHelper.createClientCredentialsJwt(
          username = "username",
          authSource = "auth",
          roles = listOf("ROLE_CAS2_EXAMPLE"),
        )

        webTestClient.get()
          .uri("/cas2bail/assessments/de6512fc-a225-4109-bdcd-86c6307a5237")
          .header("Authorization", "Bearer $jwt")
          .exchange()
          .expectStatus()
          .isForbidden
      }
    }

    @Nested
    inner class ControlsOnInternalUsers {
      @Test
      fun `getting a cas2bail assessment is forbidden to nomis users`() {
        val jwt = jwtAuthHelper.createClientCredentialsJwt(
          username = "username",
          authSource = "nomis",
          roles = listOf("ROLE_POM"),
        )

        webTestClient.get()
          .uri("/cas2bail/assessments/de6512fc-a225-4109-bdcd-86c6307a5237")
          .header("Authorization", "Bearer $jwt")
          .exchange()
          .expectStatus()
          .isForbidden
      }
    }

    @Test
    fun `assessors update cas2bail assessment returns 200`() {
      val applicationId = UUID.fromString("22ceda56-98b2-411d-91cc-ace0ab8be872")

      givenACas2PomUser { referrer, _ ->
        givenACas2Assessor { assessor, jwt ->
          val submittedApplication = createSubmittedApplication(applicationId, referrer)

          // with an assessment
          val assessment = cas2BailAssessmentEntityFactory.produceAndPersist {
            withApplication(submittedApplication)
            withNacroReferralId("someID")
            withAssessorName("some name")
          }

          val rawResponseBody = webTestClient.get()
            .uri("/cas2bail/assessments/${assessment.id}")
            .header("Authorization", "Bearer $jwt")
            .header("X-Service-Name", ServiceName.cas2.value)
            .exchange()
            .expectStatus()
            .isOk
            .returnResult<String>()
            .responseBody
            .blockFirst()

          val responseBody =
            objectMapper.readValue(rawResponseBody, object : TypeReference<Cas2Assessment>() {})

          Assertions.assertThat(responseBody.nacroReferralId).isEqualTo(assessment.nacroReferralId)
          Assertions.assertThat(responseBody.assessorName).isEqualTo(assessment.assessorName)
        }
      }
    }

    @Test
    fun `admins get cas2bail assessment returns 200`() {
      val applicationId = UUID.fromString("22ceda56-98b2-411d-91cc-ace0ab8be872")

      givenACas2PomUser { referrer, _ ->
        givenACas2Admin { admin, jwt ->
          val submittedApplication = createSubmittedApplication(applicationId, referrer)

          // with an assessment
          val assessment = cas2BailAssessmentEntityFactory.produceAndPersist {
            withApplication(submittedApplication)
            withNacroReferralId("someID")
            withAssessorName("some name")
          }

          val rawResponseBody = webTestClient.get()
            .uri("/cas2bail/assessments/${assessment.id}")
            .header("Authorization", "Bearer $jwt")
            .header("X-Service-Name", ServiceName.cas2.value)
            .exchange()
            .expectStatus()
            .isOk
            .returnResult<String>()
            .responseBody
            .blockFirst()

          val responseBody =
            objectMapper.readValue(rawResponseBody, object : TypeReference<Cas2Assessment>() {})

          Assertions.assertThat(responseBody.nacroReferralId).isEqualTo(assessment.nacroReferralId)
          Assertions.assertThat(responseBody.assessorName).isEqualTo(assessment.assessorName)
        }
      }
    }

    private fun createSubmittedApplication(applicationId: UUID, referrer: NomisUserEntity): Cas2BailApplicationEntity {
      val applicationSchema =
        cas2BailApplicationJsonSchemaEntityFactory.produceAndPersist()

      return cas2BailApplicationEntityFactory.produceAndPersist {
        withId(applicationId)
        withCreatedByUser(referrer)
        withApplicationSchema(applicationSchema)
        withSubmittedAt(OffsetDateTime.now())
      }
    }
  }
}
