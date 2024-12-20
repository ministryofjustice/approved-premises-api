package uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.cas2bail

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.NullNode
import com.ninjasquad.springmockk.SpykBean
import io.mockk.clearMocks
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.springframework.test.web.reactive.server.returnResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Application
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas2Application
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas2BailApplicationSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas2StatusUpdate
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.FullPerson
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.NewApplication
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.PersonStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ServiceName
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.UpdateApplicationType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.UpdateCas2Application
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenACas2Assessor
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenACas2LicenceCaseAdminUser
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenACas2PomUser
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenAnOffender
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.httpmocks.prisonAPIMockNotFoundInmateDetailsCall
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApplicationRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ExternalUserEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.NomisUserEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.cas2bail.Cas2BailApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.cas2bail.Cas2BailStatusUpdateEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.community.OffenderDetailSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.randomDateAfter
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.randomDateBefore
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.randomDateTimeBefore
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.UUID
import kotlin.math.sign

class Cas2BailApplicationTest : IntegrationTestBase() {

  @SpykBean
  lateinit var realApplicationRepository: ApplicationRepository

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
    fun `creating a cas2bail application is forbidden to external users based on role`(role: String) {
      val jwt = jwtAuthHelper.createClientCredentialsJwt(
        username = "username",
        authSource = "nomis",
        roles = listOf(role),
      )

      webTestClient.post()
        .uri("/cas2bail/applications")
        .header("Authorization", "Bearer $jwt")
        .exchange()
        .expectStatus()
        .isForbidden
    }

    @ParameterizedTest
    @ValueSource(strings = ["ROLE_CAS2_ASSESSOR", "ROLE_CAS2_MI"])
    fun `updating a cas2bail application is forbidden to external users based on role`(role: String) {
      val jwt = jwtAuthHelper.createClientCredentialsJwt(
        username = "username",
        authSource = "nomis",
        roles = listOf(role),
      )

      webTestClient.put()
        .uri("/cas2bail/applications/66911cf0-75b1-4361-84bd-501b176fd4fd")
        .header("Authorization", "Bearer $jwt")
        .exchange()
        .expectStatus()
        .isForbidden
    }

    @Test
    fun `viewing list of cas2bail applications is forbidden to external users based on role`() {
      val jwt = jwtAuthHelper.createClientCredentialsJwt(
        username = "username",
        authSource = "nomis",
        roles = listOf("ROLE_CAS2_ASSESSOR"),
      )

      webTestClient.get()
        .uri("/cas2bail/applications")
        .header("Authorization", "Bearer $jwt")
        .exchange()
        .expectStatus()
        .isForbidden
    }

    @Test
    fun `viewing a cas2bail application is forbidden to external users based on role`() {
      val jwt = jwtAuthHelper.createClientCredentialsJwt(
        username = "username",
        authSource = "nomis",
        roles = listOf("ROLE_CAS2_ASSESSOR"),
      )

      webTestClient.get()
        .uri("/cas2bail/applications/66911cf0-75b1-4361-84bd-501b176fd4")
        .header("Authorization", "Bearer $jwt")
        .exchange()
        .expectStatus()
        .isForbidden
    }
  }

  @Nested
  inner class MissingJwt {
    @Test
    fun `Get all cas2bail applications without JWT returns 401`() {
      webTestClient.get()
        .uri("/cas2bail/applications")
        .exchange()
        .expectStatus()
        .isUnauthorized
    }

    @Test
    fun `Get single cas2bail application without JWT returns 401`() {
      webTestClient.get()
        .uri("/cas2bail/applications/9b785e59-b85c-4be0-b271-d9ac287684b6")
        .exchange()
        .expectStatus()
        .isUnauthorized
    }

    @Test
    fun `Create new cas2bail application without JWT returns 401`() {
      webTestClient.post()
        .uri("/cas2bail/applications")
        .exchange()
        .expectStatus()
        .isUnauthorized
    }
  }

  @Nested
  inner class GetToIndex {

    @Test
    fun `return unexpired cas2bail applications when applications GET is requested`() {
      val unexpiredSubset = setOf(
        Pair("More information requested", UUID.fromString("f5cd423b-08eb-4efb-96ff-5cc6bb073905")),
        Pair("Awaiting decision", UUID.fromString("ba4d8432-250b-4ab9-81ec-7eb4b16e5dd1")),
        Pair("On waiting list", UUID.fromString("a919097d-b324-471c-9834-756f255e87ea")),
        Pair("Place offered", UUID.fromString("176bbda0-0766-4d77-8d56-18ed8f9a4ef2")),
        Pair("Offer accepted", UUID.fromString("fe254d88-ce1d-4cd8-8bd6-88de88f39019")),
        Pair("Could not be placed", UUID.fromString("758eee61-2a6d-46b9-8bdd-869536d77f1b")),
        Pair("Incomplete", UUID.fromString("4ad9bbfa-e5b0-456f-b746-146f7fd511dd")),
        Pair("Offer declined or withdrawn", UUID.fromString("9a381bc6-22d3-41d6-804d-4e49f428c1de")),
      )

      val expiredSubset = setOf(
        Pair("Referral withdrawn", UUID.fromString("004e2419-9614-4c1e-a207-a8418009f23d")),
        Pair("Referral cancelled", UUID.fromString("f13bbdd6-44f1-4362-b9d3-e6f1298b1bf9")),
        Pair("Awaiting arrival", UUID.fromString("89458555-3219-44a2-9584-c4f715d6b565")),
      )

      val applicationSchema = cas2BailApplicationJsonSchemaEntityFactory.produceAndPersist {
        withAddedAt(OffsetDateTime.now())
        withId(UUID.randomUUID())
      }

      fun createApplication(userEntity: NomisUserEntity, offenderDetails: OffenderDetailSummary): Cas2BailApplicationEntity {
        return cas2BailApplicationEntityFactory.produceAndPersist {
          withApplicationSchema(applicationSchema)
          withCreatedByUser(userEntity)
          withCrn(offenderDetails.otherIds.crn)
          withCreatedAt(OffsetDateTime.now().minusDays(28))
          withConditionalReleaseDate(LocalDate.now().plusDays(1))
        }
      }

      fun createStatusUpdate(status: Pair<String, UUID>, application: Cas2BailApplicationEntity): Cas2BailStatusUpdateEntity {
        return cas2BailStatusUpdateEntityFactory.produceAndPersist {
          withLabel(status.first)
          withStatusId(status.second)
          withApplication(application)
          withAssessor(externalUserEntityFactory.produceAndPersist())
        }
      }

      fun unexpiredDateTime() = OffsetDateTime.now().randomDateTimeBefore(32)
      fun expiredDateTime() = unexpiredDateTime().minusDays(33)

      val unexpiredApplicationIds = mutableSetOf<UUID>()
      val expiredApplicationIds = mutableSetOf<UUID>()

      givenACas2PomUser { userEntity, jwt ->
        givenAnOffender { offenderDetails, _ ->

          repeat(2) {
            unexpiredApplicationIds.add(createApplication(userEntity, offenderDetails).id)
          }

          unexpiredSubset.union(expiredSubset).forEach {
            val application = createApplication(userEntity, offenderDetails)
            val statusUpdate = createStatusUpdate(it, application)
            statusUpdate.createdAt = unexpiredDateTime()
            cas2BailStatusUpdateRepository.save(statusUpdate)
            unexpiredApplicationIds.add(application.id)
          }

          unexpiredSubset.forEach {
            val application = createApplication(userEntity, offenderDetails)
            val statusUpdate = createStatusUpdate(it, application)
            statusUpdate.createdAt = unexpiredDateTime()
            cas2BailStatusUpdateRepository.save(statusUpdate)
            unexpiredApplicationIds.add(application.id)
          }

          expiredSubset.forEach {
            val application = createApplication(userEntity, offenderDetails)
            val statusUpdate = createStatusUpdate(it, application)
            statusUpdate.createdAt = expiredDateTime()
            cas2BailStatusUpdateRepository.save(statusUpdate)
            expiredApplicationIds.add(application.id)
          }

          val rawResponseBody = webTestClient.get()
            .uri("/cas2bail/applications")
            .header("Authorization", "Bearer $jwt")
            .header("X-Service-Name", ServiceName.cas2.value)
            .exchange()
            .expectStatus()
            .isOk
            .returnResult<String>()
            .responseBody
            .blockFirst()

          val responseBody =
            objectMapper.readValue(rawResponseBody, object : TypeReference<List<Cas2BailApplicationSummary>>() {})

          val returnedApplicationIds = responseBody.map { it.id }.toSet()

          Assertions.assertThat(returnedApplicationIds.equals(unexpiredApplicationIds)).isTrue()
        }
      }
    }

    @Test
    fun `Get all cas2bail applications returns 200 with correct body`() {
      givenACas2PomUser { userEntity, jwt ->
        givenACas2PomUser { otherUser, _ ->
          givenAnOffender { offenderDetails, _ ->
            cas2BailApplicationJsonSchemaRepository.deleteAll()

            val applicationSchema = cas2BailApplicationJsonSchemaEntityFactory.produceAndPersist {
              withAddedAt(OffsetDateTime.now())
              withId(UUID.randomUUID())
            }

            // abandoned application
            val abandonedApplicationEntity = cas2BailApplicationEntityFactory.produceAndPersist {
              withApplicationSchema(applicationSchema)
              withCreatedByUser(userEntity)
              withCrn(offenderDetails.otherIds.crn)
              withData("{}")
              withCreatedAt(OffsetDateTime.parse("2024-01-03T16:10:00+01:00"))
              withAbandonedAt(OffsetDateTime.now())
            }

            // unsubmitted application
            val firstApplicationEntity = cas2BailApplicationEntityFactory.produceAndPersist {
              withApplicationSchema(applicationSchema)
              withCreatedByUser(userEntity)
              withCrn(offenderDetails.otherIds.crn)
              withData("{}")
              withCreatedAt(OffsetDateTime.parse("2024-01-03T16:10:00+01:00"))
              withHdcEligibilityDate(LocalDate.now().plusMonths(3))
            }

            // submitted application, CRD >= today so should be returned
            val secondApplicationEntity = cas2BailApplicationEntityFactory.produceAndPersist {
              withApplicationSchema(applicationSchema)
              withCreatedByUser(userEntity)
              withCrn(offenderDetails.otherIds.crn)
              withData("{}")
              withCreatedAt(OffsetDateTime.parse("2024-02-29T09:00:00+01:00"))
              withSubmittedAt(OffsetDateTime.now())
              withConditionalReleaseDate(LocalDate.now())
            }

            // submitted application, CRD = yesterday, so should not be returned
            val thirdApplicationEntity = cas2BailApplicationEntityFactory.produceAndPersist {
              withApplicationSchema(applicationSchema)
              withCreatedByUser(userEntity)
              withCrn(offenderDetails.otherIds.crn)
              withData("{}")
              withCreatedAt(OffsetDateTime.parse("2024-02-29T09:00:00+01:00"))
              withSubmittedAt(OffsetDateTime.now())
              withConditionalReleaseDate(LocalDate.now().minusDays(1))
            }

            val statusUpdate = cas2BailStatusUpdateEntityFactory.produceAndPersist {
              withLabel("More information requested")
              withApplication(secondApplicationEntity)
              withAssessor(externalUserEntityFactory.produceAndPersist())
            }

            val othercas2BailApplicationEntity = cas2BailApplicationEntityFactory.produceAndPersist {
              withApplicationSchema(applicationSchema)
              withCreatedByUser(otherUser)
              withCrn(offenderDetails.otherIds.crn)
              withData("{}")
            }

            val rawResponseBody = webTestClient.get()
              .uri("/cas2bail/applications")
              .header("Authorization", "Bearer $jwt")
              .header("X-Service-Name", ServiceName.cas2.value)
              .exchange()
              .expectStatus()
              .isOk
              .returnResult<String>()
              .responseBody
              .blockFirst()

            val responseBody =
              objectMapper.readValue(rawResponseBody, object : TypeReference<List<Cas2BailApplicationSummary>>() {})

            // check transformers were able to return all fields
            Assertions.assertThat(responseBody).anyMatch {
              firstApplicationEntity.id == it.id &&
                firstApplicationEntity.crn == it.crn &&
                firstApplicationEntity.nomsNumber == it.nomsNumber &&
                "${offenderDetails.firstName} ${offenderDetails.surname}" == it.personName &&
                firstApplicationEntity.createdAt.toInstant() == it.createdAt &&
                firstApplicationEntity.createdByUser.id == it.createdByUserId &&
                firstApplicationEntity.submittedAt?.toInstant() == it.submittedAt &&
                firstApplicationEntity.hdcEligibilityDate == it.hdcEligibilityDate &&
                firstApplicationEntity.createdByUser.name == it.createdByUserName
            }

            Assertions.assertThat(responseBody).noneMatch {
              thirdApplicationEntity.id == it.id
            }

            Assertions.assertThat(responseBody).noneMatch {
              othercas2BailApplicationEntity.id == it.id
            }

            Assertions.assertThat(responseBody).noneMatch {
              abandonedApplicationEntity.id == it.id
            }

            Assertions.assertThat(responseBody[0].createdAt)
              .isEqualTo(secondApplicationEntity.createdAt.toInstant())

            Assertions.assertThat(responseBody[0].latestStatusUpdate!!.label)
              .isEqualTo(statusUpdate.label)

            Assertions.assertThat(responseBody[1].createdAt)
              .isEqualTo(firstApplicationEntity.createdAt.toInstant())
          }
        }
      }
    }

    @Test
    fun `Get all cas2bail applications with pagination returns 200 with correct body and header`() {
      givenACas2PomUser { userEntity, jwt ->
        givenACas2PomUser { otherUser, _ ->
          givenAnOffender { offenderDetails, _ ->
            cas2BailApplicationJsonSchemaRepository.deleteAll()

            val applicationSchema = cas2BailApplicationJsonSchemaEntityFactory.produceAndPersist {
              withAddedAt(OffsetDateTime.now())
              withId(UUID.randomUUID())
            }

            repeat(12) {
              cas2BailApplicationEntityFactory.produceAndPersist {
                withApplicationSchema(applicationSchema)
                withCreatedByUser(userEntity)
                withCrn(offenderDetails.otherIds.crn)
                withData("{}")
                withCreatedAt(OffsetDateTime.now().randomDateTimeBefore(14))
              }
            }

            val rawResponseBodyPage1 = webTestClient.get()
              .uri("/cas2bail/applications?page=1")
              .header("Authorization", "Bearer $jwt")
              .header("X-Service-Name", ServiceName.cas2.value)
              .exchange()
              .expectStatus()
              .isOk
              .expectHeader().valueEquals("X-Pagination-CurrentPage", 1)
              .expectHeader().valueEquals("X-Pagination-TotalPages", 2)
              .expectHeader().valueEquals("X-Pagination-TotalResults", 12)
              .expectHeader().valueEquals("X-Pagination-PageSize", 10)
              .returnResult<String>()
              .responseBody
              .blockFirst()

            val responseBodyPage1 =
              objectMapper.readValue(rawResponseBodyPage1, object : TypeReference<List<Cas2BailApplicationSummary>>() {})

            Assertions.assertThat(responseBodyPage1).size().isEqualTo(10)

            Assertions.assertThat(isOrderedByCreatedAtDescending(responseBodyPage1)).isTrue()

            val rawResponseBodyPage2 = webTestClient.get()
              .uri("/cas2bail/applications?page=2")
              .header("Authorization", "Bearer $jwt")
              .header("X-Service-Name", ServiceName.cas2.value)
              .exchange()
              .expectStatus()
              .isOk
              .expectHeader().valueEquals("X-Pagination-CurrentPage", 2)
              .expectHeader().valueEquals("X-Pagination-TotalPages", 2)
              .expectHeader().valueEquals("X-Pagination-TotalResults", 12)
              .expectHeader().valueEquals("X-Pagination-PageSize", 10)
              .returnResult<String>()
              .responseBody
              .blockFirst()

            val responseBodyPage2 =
              objectMapper.readValue(rawResponseBodyPage2, object : TypeReference<List<Cas2BailApplicationSummary>>() {})

            Assertions.assertThat(responseBodyPage2).size().isEqualTo(2)
          }
        }
      }
    }

    @Test
    fun `When a person is not found in cas2bail, returns 200 with placeholder text`() {
      givenACas2PomUser { userEntity, jwt ->
        val crn = "X1234"

        produceAndPersistBasicApplication(crn, userEntity)

        webTestClient.get()
          .uri("/cas2bail/applications")
          .header("Authorization", "Bearer $jwt")
          .exchange()
          .expectStatus()
          .isOk
          .expectBody()
          .jsonPath("$[0].personName").isEqualTo("Person Not Found")
      }
    }

    /**
     * Returns true if the list of application summaries is sorted by descending created_at
     * or false if not.
     *
     * Works by calculating the difference in seconds between two dates and using the sign
     * of this difference.  If two dates are descending then the difference will be positive.
     * If two dates are ascending the difference will be negative (which is set to 0).
     *
     * For a list of dates, the cumulative multiple of these signs will be 1 if all
     * dates in the range are descending (= 1 x 1 x 1 etc.).
     *
     * If any dates are ascending the multiple will be 0 ( = 1 x 1 x 0 etc.).
     *
     * If all dates are ascending the multiple will also be 0 ( = 1 x 0 x 0 etc.).
     */
    private fun isOrderedByCreatedAtDescending(responseBody: List<Cas2BailApplicationSummary>): Boolean {
      var allDescending = 1
      for (i in 1..(responseBody.size - 1)) {
        val isDescending = (responseBody[i - 1].createdAt.epochSecond - responseBody[i].createdAt.epochSecond).sign
        allDescending *= if (isDescending > 0) 1 else 0
      }
      return allDescending == 1
    }

    @Nested
    inner class WithPrisonCode {
      @Test
      fun `Get all applications using prisonCode returns 200 with correct body`() {
        givenACas2Assessor { assessor, _ ->
          givenACas2PomUser { userAPrisonA, jwt ->
            givenAnOffender { offenderDetails, _ ->
              cas2BailApplicationJsonSchemaRepository.deleteAll()

              val applicationSchema = cas2BailApplicationJsonSchemaEntityFactory.produceAndPersist {
                withAddedAt(OffsetDateTime.now())
                withId(UUID.randomUUID())
              }

              val userAPrisonAApplicationIds = mutableListOf<UUID>()

              repeat(5) {
                userAPrisonAApplicationIds.add(
                  cas2BailApplicationEntityFactory.produceAndPersist {
                    withApplicationSchema(applicationSchema)
                    withCreatedByUser(userAPrisonA)
                    withCrn(offenderDetails.otherIds.crn)
                    withData("{}")
                    withCreatedAt(OffsetDateTime.now().randomDateTimeBefore(14))
                    withReferringPrisonCode(userAPrisonA.activeCaseloadId!!)
                  }.id,
                )
              }

              val userBPrisonA = nomisUserEntityFactory.produceAndPersist {
                withActiveCaseloadId(userAPrisonA.activeCaseloadId!!)
              }

              val userBPrisonAApplicationIds = mutableListOf<UUID>()

              // submitted applications with conditional release dates in the future
              repeat(6) {
                userBPrisonAApplicationIds.add(
                  cas2BailApplicationEntityFactory.produceAndPersist {
                    withApplicationSchema(applicationSchema)
                    withCreatedByUser(userBPrisonA)
                    withCrn(offenderDetails.otherIds.crn)
                    withData("{}")
                    withCreatedAt(OffsetDateTime.now().minusDays(it.toLong()))
                    withReferringPrisonCode(userAPrisonA.activeCaseloadId!!)
                    withSubmittedAt(OffsetDateTime.now().minusDays(it.toLong()))
                    withConditionalReleaseDate(LocalDate.now().randomDateAfter(14))
                  }.id,
                )
              }

              // submitted applications with conditional release dates today
              repeat(2) {
                userBPrisonAApplicationIds.add(
                  cas2BailApplicationEntityFactory.produceAndPersist {
                    withApplicationSchema(applicationSchema)
                    withCreatedByUser(userBPrisonA)
                    withCrn(offenderDetails.otherIds.crn)
                    withData("{}")
                    withCreatedAt(OffsetDateTime.now().minusDays(it.toLong() + 6))
                    withReferringPrisonCode(userAPrisonA.activeCaseloadId!!)
                    withSubmittedAt(OffsetDateTime.now().minusDays(it.toLong() + 6))
                    withConditionalReleaseDate(LocalDate.now())
                  }.id,
                )
              }

              // submitted application with a conditional release date before today
              val excludedApplicationId = cas2BailApplicationEntityFactory.produceAndPersist {
                withApplicationSchema(applicationSchema)
                withCreatedByUser(userBPrisonA)
                withCrn(offenderDetails.otherIds.crn)
                withData("{}")
                withCreatedAt(OffsetDateTime.now().minusDays(14))
                withReferringPrisonCode(userAPrisonA.activeCaseloadId!!)
                withSubmittedAt(OffsetDateTime.now())
                withConditionalReleaseDate(LocalDate.now().randomDateBefore(14))
              }.id

              addStatusUpdates(userBPrisonAApplicationIds.first(), assessor)

              val userCPrisonB = nomisUserEntityFactory.produceAndPersist {
                withActiveCaseloadId("another prison")
              }

              val otherPrisonApplication = cas2BailApplicationEntityFactory.produceAndPersist {
                withApplicationSchema(applicationSchema)
                withCreatedByUser(userCPrisonB)
                withCrn(offenderDetails.otherIds.crn)
                withData("{}")
                withCreatedAt(OffsetDateTime.now().randomDateTimeBefore(14))
                withReferringPrisonCode(userCPrisonB.activeCaseloadId!!)
              }

              val rawResponseBody = webTestClient.get()
                .uri("/cas2bail/applications?prisonCode=${userAPrisonA.activeCaseloadId}")
                .header("Authorization", "Bearer $jwt")
                .header("X-Service-Name", ServiceName.cas2.value)
                .exchange()
                .expectStatus()
                .isOk
                .returnResult<String>()
                .responseBody
                .blockFirst()

              val responseBody =
                objectMapper.readValue(rawResponseBody, object : TypeReference<List<Cas2BailApplicationSummary>>() {})

              Assertions.assertThat(responseBody).noneMatch {
                excludedApplicationId == it.id
              }

              val returnedApplicationIds = responseBody.map { it.id }.toSet()

              Assertions.assertThat(returnedApplicationIds).isEqualTo(
                userAPrisonAApplicationIds.toSet().union(userBPrisonAApplicationIds.toSet()),
              )

              Assertions.assertThat(responseBody).noneMatch {
                otherPrisonApplication.id == it.id
              }

              Assertions.assertThat(responseBody[0].latestStatusUpdate?.label).isEqualTo("Awaiting decision")
              Assertions.assertThat(responseBody[0].latestStatusUpdate?.statusId).isEqualTo(UUID.fromString("c74c3e54-52d8-4aa2-86f6-05190985efee"))
            }
          }
        }
      }

      @Test
      fun `Get all submitted applications using prisonCode returns 200 with correct body`() {
        givenACas2Assessor { assessor, _ ->
          givenACas2PomUser { userAPrisonA, jwt ->
            givenAnOffender { offenderDetails, _ ->
              cas2BailApplicationJsonSchemaRepository.deleteAll()

              val applicationSchema = cas2BailApplicationJsonSchemaEntityFactory.produceAndPersist {
                withAddedAt(OffsetDateTime.now())
                withId(UUID.randomUUID())
              }

              val userAPrisonAApplicationIds = mutableListOf<UUID>()

              repeat(5) {
                userAPrisonAApplicationIds.add(
                  cas2BailApplicationEntityFactory.produceAndPersist {
                    withApplicationSchema(applicationSchema)
                    withCreatedByUser(userAPrisonA)
                    withCrn(offenderDetails.otherIds.crn)
                    withData("{}")
                    withCreatedAt(OffsetDateTime.now().minusDays(it.toLong()))
                    withReferringPrisonCode(userAPrisonA.activeCaseloadId!!)
                  }.id,
                )
              }

              val userBPrisonA = nomisUserEntityFactory.produceAndPersist {
                withActiveCaseloadId(userAPrisonA.activeCaseloadId!!)
              }

              val userBPrisonAApplicationIds = mutableListOf<UUID>()

              repeat(6) {
                userBPrisonAApplicationIds.add(
                  cas2BailApplicationEntityFactory.produceAndPersist {
                    withApplicationSchema(applicationSchema)
                    withCreatedByUser(userBPrisonA)
                    withCrn(offenderDetails.otherIds.crn)
                    withData("{}")
                    withCreatedAt(OffsetDateTime.now().minusDays(it.toLong()))
                    withSubmittedAt(OffsetDateTime.now().randomDateTimeBefore(14))
                    withReferringPrisonCode(userAPrisonA.activeCaseloadId!!)
                    withConditionalReleaseDate(LocalDate.now().randomDateAfter(14))
                  }.id,
                )
              }

              // submitted applications with conditional release dates today
              repeat(2) {
                userBPrisonAApplicationIds.add(
                  cas2BailApplicationEntityFactory.produceAndPersist {
                    withApplicationSchema(applicationSchema)
                    withCreatedByUser(userBPrisonA)
                    withCrn(offenderDetails.otherIds.crn)
                    withData("{}")
                    withCreatedAt(OffsetDateTime.now().minusDays(it.toLong() + 6))
                    withReferringPrisonCode(userAPrisonA.activeCaseloadId!!)
                    withSubmittedAt(OffsetDateTime.now().minusDays(it.toLong() + 6))
                    withConditionalReleaseDate(LocalDate.now())
                  }.id,
                )
              }

              // submitted application with a conditional release date before today
              val excludedApplicationId = cas2BailApplicationEntityFactory.produceAndPersist {
                withApplicationSchema(applicationSchema)
                withCreatedByUser(userBPrisonA)
                withCrn(offenderDetails.otherIds.crn)
                withData("{}")
                withCreatedAt(OffsetDateTime.now().minusDays(14))
                withReferringPrisonCode(userAPrisonA.activeCaseloadId!!)
                withSubmittedAt(OffsetDateTime.now())
                withConditionalReleaseDate(LocalDate.now().randomDateBefore(14))
              }.id

              addStatusUpdates(userBPrisonAApplicationIds.first(), assessor)

              val rawResponseBody = webTestClient.get()
                .uri("/cas2bail/applications?isSubmitted=true&prisonCode=${userAPrisonA.activeCaseloadId}")
                .header("Authorization", "Bearer $jwt")
                .header("X-Service-Name", ServiceName.cas2.value)
                .exchange()
                .expectStatus()
                .isOk
                .returnResult<String>()
                .responseBody
                .blockFirst()

              val responseBody =
                objectMapper.readValue(rawResponseBody, object : TypeReference<List<Cas2BailApplicationSummary>>() {})

              Assertions.assertThat(responseBody).noneMatch {
                excludedApplicationId == it.id
              }

              val returnedApplicationIds = responseBody.map { it.id }.toSet()

              Assertions.assertThat(returnedApplicationIds).isEqualTo(userBPrisonAApplicationIds.toSet())
              Assertions.assertThat(responseBody[0].latestStatusUpdate?.label).isEqualTo("Awaiting decision")
              Assertions.assertThat(responseBody[0].latestStatusUpdate?.statusId).isEqualTo(UUID.fromString("c74c3e54-52d8-4aa2-86f6-05190985efee"))
            }
          }
        }
      }

      @Test
      fun `Get all unsubmitted applications using prisonCode returns 200 with correct body`() {
        givenACas2PomUser { userAPrisonA, jwt ->
          givenAnOffender { offenderDetails, _ ->
            cas2BailApplicationJsonSchemaRepository.deleteAll()

            val applicationSchema = cas2BailApplicationJsonSchemaEntityFactory.produceAndPersist {
              withAddedAt(OffsetDateTime.now())
              withId(UUID.randomUUID())
            }

            val userAPrisonAApplicationIds = mutableListOf<UUID>()

            repeat(5) {
              userAPrisonAApplicationIds.add(
                cas2BailApplicationEntityFactory.produceAndPersist {
                  withApplicationSchema(applicationSchema)
                  withCreatedByUser(userAPrisonA)
                  withCrn(offenderDetails.otherIds.crn)
                  withData("{}")
                  withCreatedAt(OffsetDateTime.now().randomDateTimeBefore(14))
                  withReferringPrisonCode(userAPrisonA.activeCaseloadId!!)
                }.id,
              )
            }

            val userBPrisonA = nomisUserEntityFactory.produceAndPersist {
              withActiveCaseloadId(userAPrisonA.activeCaseloadId!!)
            }

            val userBPrisonAApplicationIds = mutableListOf<UUID>()

            repeat(6) {
              userBPrisonAApplicationIds.add(
                cas2BailApplicationEntityFactory.produceAndPersist {
                  withApplicationSchema(applicationSchema)
                  withCreatedByUser(userBPrisonA)
                  withCrn(offenderDetails.otherIds.crn)
                  withData("{}")
                  withCreatedAt(OffsetDateTime.now().randomDateTimeBefore(14))
                  withSubmittedAt(OffsetDateTime.now().randomDateTimeBefore(14))
                  withReferringPrisonCode(userAPrisonA.activeCaseloadId!!)
                }.id,
              )
            }

            val rawResponseBody = webTestClient.get()
              .uri("/cas2bail/applications?isSubmitted=false&prisonCode=${userAPrisonA.activeCaseloadId}")
              .header("Authorization", "Bearer $jwt")
              .header("X-Service-Name", ServiceName.cas2.value)
              .exchange()
              .expectStatus()
              .isOk
              .returnResult<String>()
              .responseBody
              .blockFirst()

            val responseBody =
              objectMapper.readValue(rawResponseBody, object : TypeReference<List<Cas2BailApplicationSummary>>() {})

            val returnedApplicationIds = responseBody.map { it.id }.toSet()

            Assertions.assertThat(returnedApplicationIds).isEqualTo(userAPrisonAApplicationIds.toSet())
          }
        }
      }

      @Test
      fun `Get applications using another prisonCode returns Forbidden 403`() {
        givenACas2PomUser { userAPrisonA, jwt ->
          val rawResponseBody = webTestClient.get()
            .uri("/cas2bail/applications?prisonCode=${userAPrisonA.activeCaseloadId!!.reversed()}")
            .header("Authorization", "Bearer $jwt")
            .header("X-Service-Name", ServiceName.cas2.value)
            .exchange()
            .expectStatus()
            .isForbidden()
        }
      }
    }

    @Nested
    inner class AsLicenceCaseAdminUser {
      @Test
      fun `Get all submitted cas2bail applications using prisonCode returns 200 with correct body`() {
        givenACas2Assessor { assessor, _ ->
          givenACas2LicenceCaseAdminUser { caseAdminPrisonA, jwt ->
            givenAnOffender { offenderDetails, _ ->
              cas2BailApplicationJsonSchemaRepository.deleteAll()

              val applicationSchema = cas2BailApplicationJsonSchemaEntityFactory.produceAndPersist {
                withAddedAt(OffsetDateTime.now())
                withId(UUID.randomUUID())
              }

              val pomUserPrisonA = nomisUserEntityFactory.produceAndPersist {
                withActiveCaseloadId(caseAdminPrisonA.activeCaseloadId!!)
              }

              val userBPrisonAApplicationIds = mutableListOf<UUID>()

              // submitted applications with conditional release dates in the future
              repeat(6) {
                userBPrisonAApplicationIds.add(
                  cas2BailApplicationEntityFactory.produceAndPersist {
                    withApplicationSchema(applicationSchema)
                    withCreatedByUser(pomUserPrisonA)
                    withCrn(offenderDetails.otherIds.crn)
                    withData("{}")
                    withCreatedAt(OffsetDateTime.now().minusDays(it.toLong()))
                    withSubmittedAt(OffsetDateTime.now().randomDateTimeBefore(14))
                    withReferringPrisonCode(caseAdminPrisonA.activeCaseloadId!!)
                    withConditionalReleaseDate(LocalDate.now().randomDateAfter(14))
                  }.id,
                )
              }

              // submitted applications with conditional release date of today
              repeat(2) {
                userBPrisonAApplicationIds.add(
                  cas2BailApplicationEntityFactory.produceAndPersist {
                    withApplicationSchema(applicationSchema)
                    withCreatedByUser(pomUserPrisonA)
                    withCrn(offenderDetails.otherIds.crn)
                    withData("{}")
                    withCreatedAt(OffsetDateTime.now().minusDays(it.toLong() + 6))
                    withSubmittedAt(OffsetDateTime.now().randomDateTimeBefore(14))
                    withReferringPrisonCode(caseAdminPrisonA.activeCaseloadId!!)
                    withConditionalReleaseDate(LocalDate.now())
                  }.id,
                )
              }

              // submitted application with a conditional release date before today
              val excludedApplicationId = cas2BailApplicationEntityFactory.produceAndPersist {
                withApplicationSchema(applicationSchema)
                withCreatedByUser(pomUserPrisonA)
                withCrn(offenderDetails.otherIds.crn)
                withData("{}")
                withCreatedAt(OffsetDateTime.now().randomDateTimeBefore(14))
                withSubmittedAt(OffsetDateTime.now().randomDateTimeBefore(14))
                withReferringPrisonCode(caseAdminPrisonA.activeCaseloadId!!)
                withConditionalReleaseDate(LocalDate.now().randomDateBefore(14))
              }.id

              val pomUserPrisonB = nomisUserEntityFactory.produceAndPersist {
                withActiveCaseloadId("other_prison")
              }

              val otherPrisonApplication = cas2BailApplicationEntityFactory.produceAndPersist {
                withApplicationSchema(applicationSchema)
                withCreatedByUser(pomUserPrisonB)
                withCrn(offenderDetails.otherIds.crn)
                withData("{}")
                withCreatedAt(OffsetDateTime.now())
                withSubmittedAt(OffsetDateTime.now().randomDateTimeBefore(14))
                withReferringPrisonCode("other_prison")
              }

              val rawResponseBody = webTestClient.get()
                .uri("/cas2bail/applications?isSubmitted=true&prisonCode=${caseAdminPrisonA.activeCaseloadId}")
                .header("Authorization", "Bearer $jwt")
                .header("X-Service-Name", ServiceName.cas2.value)
                .exchange()
                .expectStatus()
                .isOk
                .returnResult<String>()
                .responseBody
                .blockFirst()

              val responseBody =
                objectMapper.readValue(rawResponseBody, object : TypeReference<List<Cas2BailApplicationSummary>>() {})

              Assertions.assertThat(responseBody).noneMatch {
                excludedApplicationId == it.id
              }
              val returnedApplicationIds = responseBody.map { it.id }.toSet()

              Assertions.assertThat(returnedApplicationIds).isEqualTo(userBPrisonAApplicationIds.toSet())
              Assertions.assertThat(returnedApplicationIds).noneMatch {
                otherPrisonApplication.id == it
              }
            }
          }
        }
      }
    }
  }

  private fun addStatusUpdates(applicationId: UUID, assessor: ExternalUserEntity) {
    cas2BailStatusUpdateEntityFactory.produceAndPersist {
      withLabel("More information requested")
      withApplication(cas2BailApplicationRepository.findById(applicationId).get())
      withAssessor(assessor)
    }
    // this is the one that should be returned as latestStatusUpdate
    cas2BailStatusUpdateEntityFactory.produceAndPersist {
      withStatusId(UUID.fromString("c74c3e54-52d8-4aa2-86f6-05190985efee"))
      withLabel("Awaiting decision")
      withApplication(cas2BailApplicationRepository.findById(applicationId).get())
      withAssessor(assessor)
    }
  }

  @Nested
  inner class GetToIndexUsingIsSubmitted {

    var jwtForUser: String? = null
    val submittedIds = mutableSetOf<UUID>()
    val unSubmittedIds = mutableSetOf<UUID>()
    lateinit var excludedApplicationId: UUID

    @BeforeEach
    fun setup() {
      givenACas2Assessor { assessor, _ ->
        givenACas2PomUser { userEntity, jwt ->
          givenACas2PomUser { otherUser, _ ->
            givenAnOffender { offenderDetails, _ ->
              cas2BailApplicationJsonSchemaRepository.deleteAll()

              val applicationSchema = cas2BailApplicationJsonSchemaEntityFactory.produceAndPersist {
                withAddedAt(OffsetDateTime.now())
                withId(UUID.randomUUID())
              }

              // create 3 x submitted applications for this user
              // with most recent first and conditional release dates in the future
              repeat(3) {
                submittedIds.add(
                  cas2BailApplicationEntityFactory.produceAndPersist {
                    withCreatedAt(OffsetDateTime.now().minusDays(it.toLong()))
                    withApplicationSchema(applicationSchema)
                    withCreatedByUser(userEntity)
                    withCrn(offenderDetails.otherIds.crn)
                    withData("{}")
                    withSubmittedAt(OffsetDateTime.now().minusDays(it.toLong()))
                    withConditionalReleaseDate(LocalDate.now().randomDateAfter(14))
                  }.id,
                )
              }

              // create 2 x submitted applications for this user
              // with most recent first and conditional release dates of today
              repeat(2) {
                submittedIds.add(
                  cas2BailApplicationEntityFactory.produceAndPersist {
                    withCreatedAt(OffsetDateTime.now().minusDays(it.toLong() + 3))
                    withApplicationSchema(applicationSchema)
                    withCreatedByUser(userEntity)
                    withCrn(offenderDetails.otherIds.crn)
                    withData("{}")
                    withSubmittedAt(OffsetDateTime.now().minusDays(it.toLong() + 3))
                    withConditionalReleaseDate(LocalDate.now())
                  }.id,
                )
              }

              // submitted application with a conditional release date before today
              excludedApplicationId = cas2BailApplicationEntityFactory.produceAndPersist {
                withCreatedAt(OffsetDateTime.now().minusDays(14))
                withApplicationSchema(applicationSchema)
                withCreatedByUser(userEntity)
                withCrn(offenderDetails.otherIds.crn)
                withData("{}")
                withSubmittedAt(OffsetDateTime.now())
                withConditionalReleaseDate(LocalDate.now().randomDateBefore(14))
              }.id

              addStatusUpdates(submittedIds.first(), assessor)

              // create 4 x un-submitted in-progress applications for this user
              repeat(4) {
                unSubmittedIds.add(
                  cas2BailApplicationEntityFactory.produceAndPersist {
                    withApplicationSchema(applicationSchema)
                    withCreatedByUser(userEntity)
                    withCrn(offenderDetails.otherIds.crn)
                    withData("{}")
                  }.id,
                )
              }

              // create a submitted application by another user which should not be in results
              cas2BailApplicationEntityFactory.produceAndPersist {
                withApplicationSchema(applicationSchema)
                withCreatedByUser(otherUser)
                withCrn(offenderDetails.otherIds.crn)
                withData("{}")
                withSubmittedAt(OffsetDateTime.now())
              }

              // create an unsubmitted application by another user which should not be in results
              cas2BailApplicationEntityFactory.produceAndPersist {
                withApplicationSchema(applicationSchema)
                withCreatedByUser(otherUser)
                withCrn(offenderDetails.otherIds.crn)
                withData("{}")
              }

              jwtForUser = jwt
            }
          }
        }
      }
    }

    @Test
    fun `returns all cas2bail applications for user when isSubmitted is null`() {
      val rawResponseBody = webTestClient.get()
        .uri("/cas2bail/applications")
        .header("Authorization", "Bearer $jwtForUser")
        .header("X-Service-Name", ServiceName.cas2.value)
        .exchange()
        .expectStatus()
        .isOk
        .returnResult<String>()
        .responseBody
        .blockFirst()

      val responseBody =
        objectMapper.readValue(rawResponseBody, object : TypeReference<List<Cas2BailApplicationSummary>>() {})

      Assertions.assertThat(responseBody).noneMatch {
        excludedApplicationId == it.id
      }

      val uuids = responseBody.map { it.id }.toSet()
      Assertions.assertThat(uuids).isEqualTo(submittedIds.union(unSubmittedIds))
    }

    @Test
    fun `returns submitted cas2bail applications for user when isSubmitted is true`() {
      val rawResponseBody = webTestClient.get()
        .uri("/cas2bail/applications?isSubmitted=true")
        .header("Authorization", "Bearer $jwtForUser")
        .header("X-Service-Name", ServiceName.cas2.value)
        .exchange()
        .expectStatus()
        .isOk
        .returnResult<String>()
        .responseBody
        .blockFirst()

      val responseBody =
        objectMapper.readValue(rawResponseBody, object : TypeReference<List<Cas2BailApplicationSummary>>() {})

      Assertions.assertThat(responseBody).noneMatch {
        excludedApplicationId == it.id
      }

      val uuids = responseBody.map { it.id }.toSet()
      Assertions.assertThat(uuids).isEqualTo(submittedIds)
      Assertions.assertThat(responseBody[0].latestStatusUpdate?.label).isEqualTo("Awaiting decision")
      Assertions.assertThat(responseBody[0].latestStatusUpdate?.statusId).isEqualTo(UUID.fromString("c74c3e54-52d8-4aa2-86f6-05190985efee"))
    }

    @Test
    fun `returns submitted cas2bail applications for user when isSubmitted is true and page specified`() {
      val rawResponseBody = webTestClient.get()
        .uri("/cas2bail/applications?isSubmitted=true&page=1")
        .header("Authorization", "Bearer $jwtForUser")
        .header("X-Service-Name", ServiceName.cas2.value)
        .exchange()
        .expectStatus()
        .isOk
        .returnResult<String>()
        .responseBody
        .blockFirst()

      val responseBody =
        objectMapper.readValue(rawResponseBody, object : TypeReference<List<Cas2BailApplicationSummary>>() {})

      val uuids = responseBody.map { it.id }.toSet()
      Assertions.assertThat(uuids).isEqualTo(submittedIds)
      Assertions.assertThat(responseBody[0].latestStatusUpdate?.label).isEqualTo("Awaiting decision")
      Assertions.assertThat(responseBody[0].latestStatusUpdate?.statusId).isEqualTo(UUID.fromString("c74c3e54-52d8-4aa2-86f6-05190985efee"))
    }

    @Test
    fun `returns unsubmitted cas2bail applications for user when isSubmitted is false`() {
      val rawResponseBody = webTestClient.get()
        .uri("/cas2bail/applications?isSubmitted=false")
        .header("Authorization", "Bearer $jwtForUser")
        .header("X-Service-Name", ServiceName.cas2.value)
        .exchange()
        .expectStatus()
        .isOk
        .returnResult<String>()
        .responseBody
        .blockFirst()

      val responseBody =
        objectMapper.readValue(rawResponseBody, object : TypeReference<List<Cas2BailApplicationSummary>>() {})

      val uuids = responseBody.map { it.id }.toSet()
      Assertions.assertThat(uuids).isEqualTo(unSubmittedIds)
    }
  }

  @Nested
  inner class GetToShow {

    @Nested
    inner class WhenCreatedBySameUser {
      // When the application requested was created by the logged-in user
      @Test
      fun `Get single in progress cas2bail application returns 200 with correct body`() {
        givenACas2PomUser { userEntity, jwt ->
          givenAnOffender { offenderDetails, inmateDetails ->
            cas2BailApplicationJsonSchemaRepository.deleteAll()

            val newestJsonSchema = cas2BailApplicationJsonSchemaEntityFactory
              .produceAndPersist {
                withAddedAt(OffsetDateTime.parse("2022-09-21T12:45:00+01:00"))
                withSchema(
                  schema,
                )
              }

            val applicationEntity = cas2BailApplicationEntityFactory.produceAndPersist {
              withApplicationSchema(newestJsonSchema)
              withCrn(offenderDetails.otherIds.crn)
              withCreatedByUser(userEntity)
              withData(
                data,
              )
            }

            val rawResponseBody = webTestClient.get()
              .uri("/cas2bail/applications/${applicationEntity.id}")
              .header("Authorization", "Bearer $jwt")
              .exchange()
              .expectStatus()
              .isOk
              .returnResult<String>()
              .responseBody
              .blockFirst()

            val responseBody = objectMapper.readValue(
              rawResponseBody,
              Cas2Application::class.java,
            )

            Assertions.assertThat(responseBody).matches {
              applicationEntity.id == it.id &&
                applicationEntity.crn == it.person.crn &&
                applicationEntity.createdAt.toInstant() == it.createdAt &&
                applicationEntity.createdByUser.id == it.createdBy.id &&
                applicationEntity.submittedAt?.toInstant() == it.submittedAt &&
                serializableToJsonNode(applicationEntity.data) == serializableToJsonNode(it.data) &&
                newestJsonSchema.id == it.schemaVersion && !it.outdatedSchema
            }
          }
        }
      }

      @Test
      fun `Get single cas2bail application returns successfully when the offender cannot be fetched from the prisons API`() {
        givenACas2PomUser { userEntity, jwt ->
          val crn = "X1234"

          givenAnOffender(
            offenderDetailsConfigBlock = {
              withCrn(crn)
              withNomsNumber("ABC123")
            },
          ) { offenderDetails, _ ->
            val application = produceAndPersistBasicApplication(crn, userEntity)

            prisonAPIMockNotFoundInmateDetailsCall(offenderDetails.otherIds.nomsNumber!!)
            loadPreemptiveCacheForInmateDetails(offenderDetails.otherIds.nomsNumber!!)

            val rawResponseBody = webTestClient.get()
              .uri("/cas2bail/applications/${application.id}")
              .header("Authorization", "Bearer $jwt")
              .exchange()
              .expectStatus()
              .isOk
              .returnResult<String>()
              .responseBody
              .blockFirst()

            val responseBody = objectMapper.readValue(
              rawResponseBody,
              Cas2Application::class.java,
            )

            Assertions.assertThat(responseBody.person is FullPerson).isTrue

            Assertions.assertThat(responseBody).matches {
              val person = it.person as FullPerson

              application.id == it.id &&
                application.crn == person.crn &&
                person.nomsNumber == null &&
                person.status == PersonStatus.unknown &&
                person.prisonName == null
            }
          }
        }
      }

      @Test
      fun `Get single submitted cas2bail application returns 200 with timeline events`() {
        givenACas2PomUser { userEntity, jwt ->
          givenAnOffender { offenderDetails, inmateDetails ->
            cas2BailApplicationJsonSchemaRepository.deleteAll()

            val newestJsonSchema = cas2BailApplicationJsonSchemaEntityFactory
              .produceAndPersist {
                withAddedAt(OffsetDateTime.parse("2022-09-21T12:45:00+01:00"))
                withSchema(
                  schema,
                )
              }

            val applicationEntity = cas2BailApplicationEntityFactory.produceAndPersist {
              withApplicationSchema(newestJsonSchema)
              withCrn(offenderDetails.otherIds.crn)
              withCreatedByUser(userEntity)
              withSubmittedAt(OffsetDateTime.now().minusDays(1))
            }

            cas2BailAssessmentEntityFactory.produceAndPersist {
              withApplication(applicationEntity)
            }

            val rawResponseBody = webTestClient.get()
              .uri("/cas2/applications/${applicationEntity.id}")
              .header("Authorization", "Bearer $jwt")
              .exchange()
              .expectStatus()
              .isOk
              .returnResult<String>()
              .responseBody
              .blockFirst()

            val responseBody = objectMapper.readValue(
              rawResponseBody,
              Cas2Application::class.java,
            )

            Assertions.assertThat(responseBody.assessment!!.statusUpdates).isEqualTo(emptyList<Cas2StatusUpdate>())

            Assertions.assertThat(responseBody.timelineEvents!!.map { event -> event.label })
              .isEqualTo(listOf("Application submitted"))
          }
        }
      }
    }

    @Nested
    inner class WhenCreatedByDifferentUser {

      @Nested
      inner class WhenDifferentPrison {
        @Test
        fun `Get single submitted cas2bail application is forbidden`() {
          givenACas2PomUser { userEntity, jwt ->
            givenAnOffender { offenderDetails, inmateDetails ->
              cas2BailApplicationJsonSchemaRepository.deleteAll()

              val otherUser = nomisUserEntityFactory.produceAndPersist {
                withActiveCaseloadId("other_caseload")
              }

              val newestJsonSchema = cas2BailApplicationJsonSchemaEntityFactory
                .produceAndPersist {
                  withAddedAt(OffsetDateTime.parse("2022-09-21T12:45:00+01:00"))
                  withSchema(
                    schema,
                  )
                }

              val applicationEntity = cas2BailApplicationEntityFactory.produceAndPersist {
                withApplicationSchema(newestJsonSchema)
                withCrn(offenderDetails.otherIds.crn)
                withSubmittedAt(OffsetDateTime.now())
                withCreatedByUser(otherUser)
                withData(
                  data,
                )
              }

              webTestClient.get()
                .uri("/cas2bail/applications/${applicationEntity.id}")
                .header("Authorization", "Bearer $jwt")
                .exchange()
                .expectStatus()
                .isForbidden
            }
          }
        }
      }

      @Nested
      inner class WhenSamePrison {
        @Test
        fun `Get single submitted cas2bail application returns 200 with timeline events`() {
          givenACas2PomUser { userEntity, jwt ->
            givenAnOffender { offenderDetails, inmateDetails ->
              cas2BailApplicationJsonSchemaRepository.deleteAll()

              val newestJsonSchema = cas2BailApplicationJsonSchemaEntityFactory
                .produceAndPersist {
                  withAddedAt(OffsetDateTime.parse("2022-09-21T12:45:00+01:00"))
                  withSchema(
                    schema,
                  )
                }

              val otherUser = nomisUserEntityFactory.produceAndPersist {
                withActiveCaseloadId(userEntity.activeCaseloadId!!)
              }

              val applicationEntity = cas2BailApplicationEntityFactory.produceAndPersist {
                withApplicationSchema(newestJsonSchema)
                withCrn(offenderDetails.otherIds.crn)
                withCreatedByUser(otherUser)
                withSubmittedAt(OffsetDateTime.now().minusDays(1))
                withReferringPrisonCode(userEntity.activeCaseloadId!!)
              }

              cas2BailAssessmentEntityFactory.produceAndPersist {
                withApplication(applicationEntity)
              }

              val rawResponseBody = webTestClient.get()
                .uri("/cas2bail/applications/${applicationEntity.id}")
                .header("Authorization", "Bearer $jwt")
                .exchange()
                .expectStatus()
                .isOk
                .returnResult<String>()
                .responseBody
                .blockFirst()

              val responseBody = objectMapper.readValue(
                rawResponseBody,
                Cas2Application::class.java,
              )

              Assertions.assertThat(responseBody.assessment!!.statusUpdates).isEqualTo(emptyList<Cas2StatusUpdate>())

              Assertions.assertThat(responseBody.timelineEvents!!.map { event -> event.label })
                .isEqualTo(listOf("Application submitted"))
            }
          }
        }

        @Test
        fun `Get single unsubmitted cas2bail application returns 403`() {
          givenACas2PomUser { userEntity, jwt ->
            givenAnOffender { offenderDetails, inmateDetails ->
              cas2BailApplicationJsonSchemaRepository.deleteAll()

              val newestJsonSchema = cas2BailApplicationJsonSchemaEntityFactory
                .produceAndPersist {
                  withAddedAt(OffsetDateTime.parse("2022-09-21T12:45:00+01:00"))
                  withSchema(
                    schema,
                  )
                }

              val otherUser = nomisUserEntityFactory.produceAndPersist {
                withActiveCaseloadId(userEntity.activeCaseloadId!!)
              }

              val applicationEntity = cas2BailApplicationEntityFactory.produceAndPersist {
                withApplicationSchema(newestJsonSchema)
                withCrn(offenderDetails.otherIds.crn)
                withCreatedByUser(otherUser)
                withReferringPrisonCode(userEntity.activeCaseloadId!!)
              }

              webTestClient.get()
                .uri("/cas2bail/applications/${applicationEntity.id}")
                .header("Authorization", "Bearer $jwt")
                .exchange()
                .expectStatus()
                .isForbidden
            }
          }
        }
      }
    }
  }

  @Nested
  inner class PostToCreate {

    @Nested
    inner class PomUsers {
      @Test
      fun `Create new application for cas2bail returns 201 with correct body and Location header`() {
        givenACas2PomUser { userEntity, jwt ->
          givenAnOffender { offenderDetails, _ ->
            val applicationSchema =
              cas2BailApplicationJsonSchemaEntityFactory.produceAndPersist {
                withAddedAt(OffsetDateTime.now())
                withId(UUID.randomUUID())
              }

            val result = webTestClient.post()
              .uri("/cas2bail/applications")
              .header("Authorization", "Bearer $jwt")
              .header("X-Service-Name", ServiceName.cas2.value)
              .bodyValue(
                NewApplication(
                  crn = offenderDetails.otherIds.crn,
                ),
              )
              .exchange()
              .expectStatus()
              .isCreated
              .returnResult(Cas2Application::class.java)

            Assertions.assertThat(result.responseHeaders["Location"]).anyMatch {
              it.matches(Regex("/cas2bail/applications/.+"))
            }

            Assertions.assertThat(result.responseBody.blockFirst()).matches {
              it.person.crn == offenderDetails.otherIds.crn &&
                it.schemaVersion == applicationSchema.id
            }
          }
        }
      }

      @Test
      fun `Create new cas2bail application returns 404 when a person cannot be found`() {
        givenACas2PomUser { userEntity, jwt ->
          val crn = "X1234"

          cas2BailApplicationJsonSchemaEntityFactory.produceAndPersist {
            withAddedAt(OffsetDateTime.now())
            withId(UUID.randomUUID())
          }

          webTestClient.post()
            .uri("/cas2bail/applications")
            .header("Authorization", "Bearer $jwt")
            .bodyValue(
              NewApplication(
                crn = crn,
              ),
            )
            .exchange()
            .expectStatus()
            .isNotFound
            .expectBody()
            .jsonPath("$.detail").isEqualTo("No Offender with an ID of $crn could be found")
        }
      }
    }

    @Nested
    inner class LicenceCaseAdminUsers {
      @Test
      fun `Create new cas2bail application for CAS-2 returns 201 with correct body and Location header`() {
        givenACas2LicenceCaseAdminUser { _, jwt ->
          givenAnOffender { offenderDetails, _ ->
            val applicationSchema =
              cas2BailApplicationJsonSchemaEntityFactory.produceAndPersist {
                withAddedAt(OffsetDateTime.now())
                withId(UUID.randomUUID())
              }

            val result = webTestClient.post()
              .uri("/cas2bail/applications")
              .header("Authorization", "Bearer $jwt")
              .header("X-Service-Name", ServiceName.cas2.value)
              .bodyValue(
                NewApplication(
                  crn = offenderDetails.otherIds.crn,
                ),
              )
              .exchange()
              .expectStatus()
              .isCreated
              .returnResult(Cas2Application::class.java)

            Assertions.assertThat(result.responseHeaders["Location"]).anyMatch {
              it.matches(Regex("/cas2/applications/.+"))
            }

            Assertions.assertThat(result.responseBody.blockFirst()).matches {
              it.person.crn == offenderDetails.otherIds.crn &&
                it.schemaVersion == applicationSchema.id
            }
          }
        }
      }

      @Test
      fun `Create new cas2bail application returns 404 when a person cannot be found`() {
        givenACas2LicenceCaseAdminUser { _, jwt ->
          val crn = "X1234"

          cas2BailApplicationJsonSchemaEntityFactory.produceAndPersist {
            withAddedAt(OffsetDateTime.now())
            withId(UUID.randomUUID())
          }

          webTestClient.post()
            .uri("/cas2bail/applications")
            .header("Authorization", "Bearer $jwt")
            .bodyValue(
              NewApplication(
                crn = crn,
              ),
            )
            .exchange()
            .expectStatus()
            .isNotFound
            .expectBody()
            .jsonPath("$.detail").isEqualTo("No Offender with an ID of $crn could be found")
        }
      }
    }
  }

  @Nested
  inner class PutToUpdate {

    @Nested
    inner class PomUsers {
      @Test
      fun `Update existing cas2bail application returns 200 with correct body`() {
        givenACas2PomUser { submittingUser, jwt ->
          givenAnOffender { offenderDetails, _ ->
            val applicationId = UUID.fromString("22ceda56-98b2-411d-91cc-ace0ab8be872")

            val applicationSchema =
              cas2BailApplicationJsonSchemaEntityFactory.produceAndPersist {
                withAddedAt(OffsetDateTime.now())
                withId(UUID.randomUUID())
                withSchema(
                  schema,
                )
              }

            cas2BailApplicationEntityFactory.produceAndPersist {
              withCrn(offenderDetails.otherIds.crn)
              withId(applicationId)
              withApplicationSchema(applicationSchema)
              withCreatedByUser(submittingUser)
            }
// TODO what is a UpdateCas2Application?
            val resultBody = webTestClient.put()
              .uri("/cas2bail/applications/$applicationId")
              .header("Authorization", "Bearer $jwt")
              .bodyValue(
                UpdateCas2Application(
                  data = mapOf("thingId" to 123),
                  type = UpdateApplicationType.CAS2,
                ),
              )
              .exchange()
              .expectStatus()
              .isOk
              .returnResult(String::class.java)
              .responseBody
              .blockFirst()

            val result = objectMapper.readValue(resultBody, Application::class.java)

            Assertions.assertThat(result.person.crn).isEqualTo(offenderDetails.otherIds.crn)
          }
        }
      }
    }

    @Nested
    inner class LicenceCaseAdminUsers {
      @Test
      fun `Update existing cas2bail application returns 200 with correct body`() {
        givenACas2LicenceCaseAdminUser { submittingUser, jwt ->
          givenAnOffender { offenderDetails, _ ->
            val applicationId = UUID.fromString("22ceda56-98b2-411d-91cc-ace0ab8be872")

            val applicationSchema =
              cas2BailApplicationJsonSchemaEntityFactory.produceAndPersist {
                withAddedAt(OffsetDateTime.now())
                withId(UUID.randomUUID())
                withSchema(
                  schema,
                )
              }

            cas2BailApplicationEntityFactory.produceAndPersist {
              withCrn(offenderDetails.otherIds.crn)
              withId(applicationId)
              withApplicationSchema(applicationSchema)
              withCreatedByUser(submittingUser)
            }

            val resultBody = webTestClient.put()
              .uri("/cas2bail/applications/$applicationId")
              .header("Authorization", "Bearer $jwt")
              .bodyValue(
                UpdateCas2Application(
                  data = mapOf("thingId" to 123),
                  type = UpdateApplicationType.CAS2,
                ),
              )
              .exchange()
              .expectStatus()
              .isOk
              .returnResult(String::class.java)
              .responseBody
              .blockFirst()

            val result = objectMapper.readValue(resultBody, Application::class.java)

            Assertions.assertThat(result.person.crn).isEqualTo(offenderDetails.otherIds.crn)
          }
        }
      }
    }
  }

  private fun serializableToJsonNode(serializable: Any?): JsonNode {
    if (serializable == null) return NullNode.instance
    if (serializable is String) return objectMapper.readTree(serializable)

    return objectMapper.readTree(objectMapper.writeValueAsString(serializable))
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
