package uk.gov.justice.digital.hmpps.approvedpremisesapi.integration

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.repository.findByIdOrNull
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ApArea
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ApprovedPremisesUser
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ApprovedPremisesUserRole
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ProbationDeliveryUnit
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ProbationRegion
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ServiceName
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.TemporaryAccommodationUser
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.UserRolesAndQualifications
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.StaffDetailFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.TeamFactoryDeliusContext
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenACas1CruManagementArea
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenAProbationRegion
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenAUser
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenAnApArea
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.httpmocks.apDeliusContextAddStaffDetailResponse
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserPermission
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserQualification
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserRole
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.deliuscontext.Borough
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.deliuscontext.PersonName
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.UserTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.randomStringMultiCaseWithNumbers
import java.util.UUID
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.UserQualification as APIUserQualification

class UsersTest : InitialiseDatabasePerClassTestBase() {
  @Autowired
  lateinit var userTransformer: UserTransformer

  val id: UUID = UUID.fromString("aff9a4dc-e208-4e4b-abe6-99aff7f6af8a")

  @Test
  fun `Getting a user without a JWT returns 401`() {
    webTestClient.get()
      .uri("/users/$id")
      .exchange()
      .expectStatus()
      .isUnauthorized
  }

  @Test
  fun `Getting a user with a non-Delius JWT returns 403`() {
    val jwt = jwtAuthHelper.createClientCredentialsJwt(
      username = "username",
      authSource = "other-auth-source",
    )

    webTestClient.get()
      .uri("/users/$id")
      .header("Authorization", "Bearer $jwt")
      .exchange()
      .expectStatus()
      .isForbidden
  }

  @Test
  fun `Getting a user with a Nomis JWT returns 403`() {
    val jwt = jwtAuthHelper.createClientCredentialsJwt(
      username = "username",
      authSource = "nomis",
    )

    webTestClient.get()
      .uri("/users/$id")
      .header("Authorization", "Bearer $jwt")
      .exchange()
      .expectStatus()
      .isForbidden
  }

  @Test
  fun `Getting a user with the POM role returns 403`() {
    val jwt = jwtAuthHelper.createClientCredentialsJwt(
      username = "username",
      authSource = "nomis",
      roles = listOf("ROLE_POM"),
    )

    webTestClient.get()
      .uri("/users/$id")
      .header("Authorization", "Bearer $jwt")
      .exchange()
      .expectStatus()
      .isForbidden
  }

  @Test
  fun `Getting a user with no X-Service-Name header returns 400`() {
    val deliusUsername = "JimJimmerson"
    val forename = "Jim"
    val surname = "Jimmerson"
    val name = "$forename $surname"
    val email = "foo@bar.com"
    val telephoneNumber = "123445677"

    val jwt = jwtAuthHelper.createAuthorizationCodeJwt(
      subject = deliusUsername,
      authSource = "delius",
      roles = listOf("ROLE_PROBATION"),
    )

    mockClientCredentialsJwtRequest("username", listOf("ROLE_COMMUNITY"), authSource = "delius")

    webTestClient.get()
      .uri("/users/$id")
      .header("Authorization", "Bearer $jwt")
      .exchange()
      .expectStatus()
      .is4xxClientError
      .expectBody()
      .jsonPath("title").isEqualTo("Bad Request")
      .jsonPath("detail").isEqualTo("Missing required header X-Service-Name")
  }

  @Test
  fun `Getting an Approved Premises user returns OK with correct body`() {
    val deliusUsername = "JimJimmerson"
    val forename = "Jim"
    val surname = "Jimmerson"
    val name = "$forename $surname"
    val email = "foo@bar.com"
    val telephoneNumber = "123445677"

    val jwt = jwtAuthHelper.createAuthorizationCodeJwt(
      subject = deliusUsername,
      authSource = "delius",
      roles = listOf("ROLE_PROBATION"),
    )

    val region = givenAProbationRegion()

    userEntityFactory.produceAndPersist {
      withId(id)
      withDeliusUsername(deliusUsername)
      withName(name)
      withEmail(email)
      withTelephoneNumber(telephoneNumber)
      withYieldedProbationRegion { region }
    }

    apDeliusContextAddStaffDetailResponse(
      StaffDetailFactory.staffDetail(
        name = PersonName(forename, surname),
        deliusUsername = deliusUsername,
        email = email,
        telephoneNumber = telephoneNumber,
      ),
    )

    mockClientCredentialsJwtRequest("username", listOf("ROLE_COMMUNITY"), authSource = "delius")

    val result = webTestClient.get()
      .uri("/users/$id")
      .header("Authorization", "Bearer $jwt")
      .header("X-Service-Name", ServiceName.approvedPremises.value)
      .exchange()
      .expectStatus()
      .isOk
      .returnResult(ApprovedPremisesUser::class.java)
      .responseBody
      .blockFirst()

    assertThat(result.id).isEqualTo(id)
    assertThat(result.region).isEqualTo(ProbationRegion(region.id, region.name))
    assertThat(result.deliusUsername).isEqualTo(deliusUsername)
    assertThat(result.name).isEqualTo(name)
    assertThat(result.email).isEqualTo(email)
    assertThat(result.telephoneNumber).isEqualTo(telephoneNumber)
    assertThat(result.roles).isEqualTo(emptyList<ApprovedPremisesUserRole>())
    assertThat(result.qualifications).isEqualTo(emptyList<UserQualification>())
    assertThat(result.service).isEqualTo("CAS1")
    assertThat(result.isActive).isEqualTo(true)
    assertThat(result.apArea).isEqualTo(ApArea(region.apArea!!.id, region.apArea!!.identifier, region.apArea!!.name))
    assertThat(result.permissions).isEqualTo(emptyList<UserPermission>())
    assertThat(result.version).isNotZero()
  }

  @Test
  fun `Getting a Temporary Accommodation user returns OK with correct body`() {
    val deliusUsername = "JimJimmerson"
    val forename = "Jim"
    val surname = "Jimmerson"
    val name = "$forename $surname"
    val email = "foo@bar.com"
    val telephoneNumber = "123445677"
    val borough = Borough(
      code = randomStringMultiCaseWithNumbers(10),
      description = randomStringMultiCaseWithNumbers(10),
    )

    val jwt = jwtAuthHelper.createAuthorizationCodeJwt(
      subject = deliusUsername,
      authSource = "delius",
      roles = listOf("ROLE_PROBATION"),
    )

    val region = givenAProbationRegion()

    val probationDeliveryUnit = probationDeliveryUnitFactory.produceAndPersist {
      withDeliusCode(borough.code)
      withName(borough.description)
      withProbationRegion(region)
    }

    userEntityFactory.produceAndPersist {
      withId(id)
      withDeliusUsername(deliusUsername)
      withName(name)
      withEmail(email)
      withTelephoneNumber(telephoneNumber)
      withYieldedProbationRegion { region }
      withProbationDeliveryUnit { probationDeliveryUnit }
    }

    apDeliusContextAddStaffDetailResponse(
      StaffDetailFactory.staffDetail(
        name = PersonName(forename, surname),
        deliusUsername = deliusUsername,
        email = email,
        telephoneNumber = telephoneNumber,
        teams = listOf(TeamFactoryDeliusContext.team(borough = borough)),
      ),
    )

    mockClientCredentialsJwtRequest("username", listOf("ROLE_COMMUNITY"), authSource = "delius")

    webTestClient.get()
      .uri("/users/$id")
      .header("Authorization", "Bearer $jwt")
      .header("X-Service-Name", ServiceName.temporaryAccommodation.value)
      .exchange()
      .expectStatus()
      .isOk
      .expectBody()
      .json(
        objectMapper.writeValueAsString(
          TemporaryAccommodationUser(
            id = id,
            region = ProbationRegion(region.id, region.name),
            probationDeliveryUnit = ProbationDeliveryUnit(probationDeliveryUnit.id, probationDeliveryUnit.name),
            deliusUsername = deliusUsername,
            name = name,
            email = email,
            telephoneNumber = telephoneNumber,
            roles = emptyList(),
            service = "CAS3",
            isActive = true,
          ),
        ),
      )
  }

  @Nested
  inner class UserHeaders {

    @Test
    fun `Getting an Approved Premises user with no roles returns x cas user headers`() {
      val (user, jwt) = givenAUser()

      val headers = webTestClient.get()
        .uri("/users/${user.id}")
        .header("Authorization", "Bearer $jwt")
        .header("X-Service-Name", ServiceName.approvedPremises.value)
        .exchange()
        .expectStatus()
        .isOk
        .returnResult(ApprovedPremisesUser::class.java)
        .responseHeaders

      assertThat(headers["X-CAS-User-ID"]!![0]).isEqualTo(user.id.toString())
      assertThat(headers["X-CAS-User-Version"]!![0]).isEqualTo("993")
    }

    @Test
    fun `Getting an Approved Premises user with roles returns x cas user headers`() {
      val (user, jwt) = givenAUser(roles = listOf(UserRole.CAS1_REPORT_VIEWER))

      val headers = webTestClient.get()
        .uri("/users/${user.id}")
        .header("Authorization", "Bearer $jwt")
        .header("X-Service-Name", ServiceName.approvedPremises.value)
        .exchange()
        .expectStatus()
        .isOk
        .returnResult(ApprovedPremisesUser::class.java)
        .responseHeaders

      assertThat(headers["X-CAS-User-ID"]!![0]).isEqualTo(user.id.toString())
      assertThat(headers["X-CAS-User-Version"]!![0]).isEqualTo("1173003502")
    }

    @Test
    fun `Getting a non Approved Premises user does not return x cas user headers`() {
      val (user, jwt) = givenAUser()

      val headers = webTestClient.get()
        .uri("/users/${user.id}")
        .header("Authorization", "Bearer $jwt")
        .header("X-Service-Name", ServiceName.temporaryAccommodation.value)
        .exchange()
        .expectStatus()
        .isOk
        .returnResult(ApprovedPremisesUser::class.java)
        .responseHeaders

      assertThat(headers.containsKey("X-CAS-User-ID")).isFalse()
      assertThat(headers.containsKey("X-CAS-User-Version")).isFalse()
    }
  }

  @Nested
  inner class GetUsers {
    @ParameterizedTest
    @EnumSource(value = UserRole::class, names = ["CAS1_WORKFLOW_MANAGER", "CAS1_JANITOR", "CAS1_USER_MANAGER"])
    fun `GET to users with X-Service-Name other than approved-premises is forbidden`(role: UserRole) {
      givenAUser(roles = listOf(role)) { _, jwt ->
        webTestClient.get()
          .uri("/users")
          .header("Authorization", "Bearer $jwt")
          .header("X-Service-Name", ServiceName.temporaryAccommodation.value)
          .exchange()
          .expectStatus()
          .isForbidden
      }
    }

    @ParameterizedTest
    @EnumSource(
      value = UserRole::class,
      names = ["CAS1_CRU_MEMBER", "CAS1_CRU_MEMBER_FIND_AND_BOOK_BETA", "CAS1_WORKFLOW_MANAGER", "CAS1_JANITOR", "CAS1_USER_MANAGER"],
      mode = EnumSource.Mode.EXCLUDE,
    )
    fun `GET users with an unapproved role is forbidden`(role: UserRole) {
      givenAUser(roles = listOf(role)) { _, jwt ->
        webTestClient.get()
          .uri("/users")
          .header("Authorization", "Bearer $jwt")
          .header("X-Service-Name", ServiceName.approvedPremises.value)
          .exchange()
          .expectStatus()
          .isForbidden
      }
    }

    @Test
    fun `GET users with no internal role (aka the Applicant pseudo-role) is forbidden`() {
      givenAUser { _, jwt ->
        webTestClient.get()
          .uri("/users")
          .header("Authorization", "Bearer $jwt")
          .header("X-Service-Name", ServiceName.approvedPremises.value)
          .exchange()
          .expectStatus()
          .isForbidden
      }
    }

    @ParameterizedTest
    @EnumSource(value = UserRole::class, names = ["CAS1_WORKFLOW_MANAGER", "CAS1_JANITOR", "CAS1_USER_MANAGER"])
    fun `GET to users with an approved role returns full list ordered by name`(role: UserRole) {
      givenAUser(roles = listOf(UserRole.CAS1_MATCHER)) { matcher, _ ->
        givenAUser(roles = listOf(UserRole.CAS1_FUTURE_MANAGER)) { manager, _ ->
          givenAUser { userWithNoRole, _ ->
            givenAUser(roles = listOf(role)) { requestUser, jwt ->
              webTestClient.get()
                .uri("/users")
                .header("Authorization", "Bearer $jwt")
                .header("X-Service-Name", ServiceName.approvedPremises.value)
                .exchange()
                .expectStatus()
                .isOk
                .expectHeader().doesNotExist("X-Pagination-CurrentPage")
                .expectHeader().doesNotExist("X-Pagination-TotalPages")
                .expectHeader().doesNotExist("X-Pagination-TotalResults")
                .expectHeader().doesNotExist("X-Pagination-PageSize")
                .expectBody()
                .json(
                  objectMapper.writeValueAsString(
                    listOf(requestUser, userWithNoRole, matcher, manager).map {
                      userTransformer.transformJpaToApi(it, ServiceName.approvedPremises)
                    },
                  ),
                )
            }
          }
        }
      }
    }

    @ParameterizedTest
    @EnumSource(value = UserRole::class, names = ["CAS1_WORKFLOW_MANAGER", "CAS1_JANITOR", "CAS1_USER_MANAGER"])
    fun `GET to users with an approved role returns list filtered by region`(role: UserRole) {
      givenAUser(roles = listOf(UserRole.CAS1_MATCHER)) { matcher, _ ->
        givenAUser(roles = listOf(UserRole.CAS1_FUTURE_MANAGER)) { manager, _ ->
          givenAUser { userWithNoRole, _ ->
            givenAUser(roles = listOf(role)) { requestUser, jwt ->
              val apArea = givenAnApArea()
              val probationRegion = probationRegionEntityFactory.produceAndPersist {
                withApArea(apArea)
              }

              val userOne = userEntityFactory.produceAndPersist {
                withProbationRegion(probationRegion)
                withApArea(apArea)
                withCruManagementArea(givenACas1CruManagementArea())
              }

              val userTwo = userEntityFactory.produceAndPersist {
                withProbationRegion(probationRegion)
                withApArea(apArea)
                withCruManagementArea(givenACas1CruManagementArea())
              }

              webTestClient.get()
                .uri("/users?probationRegionId=${probationRegion.id}")
                .header("Authorization", "Bearer $jwt")
                .header("X-Service-Name", ServiceName.approvedPremises.value)
                .exchange()
                .expectStatus()
                .isOk
                .expectHeader().doesNotExist("X-Pagination-CurrentPage")
                .expectHeader().doesNotExist("X-Pagination-TotalPages")
                .expectHeader().doesNotExist("X-Pagination-TotalResults")
                .expectHeader().doesNotExist("X-Pagination-PageSize")
                .expectBody()
                .json(
                  objectMapper.writeValueAsString(
                    listOf(userOne, userTwo).map {
                      userTransformer.transformJpaToApi(it, ServiceName.approvedPremises)
                    },
                  ),
                )
            }
          }
        }
      }
    }

    @ParameterizedTest
    @EnumSource(value = UserRole::class, names = ["CAS1_WORKFLOW_MANAGER", "CAS1_JANITOR", "CAS1_USER_MANAGER"])
    fun `GET to users with an approved role returns list filtered by user's AP area`(role: UserRole) {
      givenAUser(roles = listOf(UserRole.CAS1_MATCHER)) { matcher, _ ->
        givenAUser(roles = listOf(UserRole.CAS1_FUTURE_MANAGER)) { manager, _ ->
          givenAUser { userWithNoRole, _ ->
            givenAUser(roles = listOf(role)) { requestUser, jwt ->
              val apArea = givenAnApArea()

              val probationRegionApArea = givenAnApArea()
              val probationRegion = probationRegionEntityFactory.produceAndPersist {
                withApArea(probationRegionApArea)
              }

              val userOne = userEntityFactory.produceAndPersist {
                withProbationRegion(probationRegion)
                withApArea(apArea)
                withCruManagementArea(givenACas1CruManagementArea())
              }

              val userTwo = userEntityFactory.produceAndPersist {
                withProbationRegion(probationRegion)
                withApArea(apArea)
                withCruManagementArea(givenACas1CruManagementArea())
              }

              webTestClient.get()
                .uri("/users?apAreaId=${apArea.id}")
                .header("Authorization", "Bearer $jwt")
                .header("X-Service-Name", ServiceName.approvedPremises.value)
                .exchange()
                .expectStatus()
                .isOk
                .expectHeader().doesNotExist("X-Pagination-CurrentPage")
                .expectHeader().doesNotExist("X-Pagination-TotalPages")
                .expectHeader().doesNotExist("X-Pagination-TotalResults")
                .expectHeader().doesNotExist("X-Pagination-PageSize")
                .expectBody()
                .json(
                  objectMapper.writeValueAsString(
                    listOf(userOne, userTwo).map {
                      userTransformer.transformJpaToApi(it, ServiceName.approvedPremises)
                    },
                  ),
                )
            }
          }
        }
      }
    }

    @ParameterizedTest
    @EnumSource(value = UserRole::class, names = ["CAS1_WORKFLOW_MANAGER", "CAS1_JANITOR", "CAS1_USER_MANAGER"])
    fun `GET to users with an approved role returns list filtered by user's CRU management area`(role: UserRole) {
      givenAUser(roles = listOf(UserRole.CAS1_MATCHER)) { matcher, _ ->
        givenAUser(roles = listOf(UserRole.CAS1_FUTURE_MANAGER)) { manager, _ ->
          givenAUser { userWithNoRole, _ ->
            givenAUser(roles = listOf(role)) { requestUser, jwt ->
              val apArea = givenAnApArea()

              val probationRegionApArea = givenAnApArea()
              val probationRegion = probationRegionEntityFactory.produceAndPersist {
                withApArea(probationRegionApArea)
              }

              val cruManagementArea = givenACas1CruManagementArea()

              val userOne = userEntityFactory.produceAndPersist {
                withProbationRegion(probationRegion)
                withApArea(apArea)
                withCruManagementArea(cruManagementArea)
              }

              val userTwo = userEntityFactory.produceAndPersist {
                withProbationRegion(probationRegion)
                withApArea(apArea)
                withCruManagementArea(cruManagementArea)
              }

              userEntityFactory.produceAndPersist {
                withProbationRegion(probationRegion)
                withApArea(apArea)
                withCruManagementArea(givenACas1CruManagementArea())
              }

              webTestClient.get()
                .uri("/users?cruManagementAreaId=${cruManagementArea.id}")
                .header("Authorization", "Bearer $jwt")
                .header("X-Service-Name", ServiceName.approvedPremises.value)
                .exchange()
                .expectStatus()
                .isOk
                .expectHeader().doesNotExist("X-Pagination-CurrentPage")
                .expectHeader().doesNotExist("X-Pagination-TotalPages")
                .expectHeader().doesNotExist("X-Pagination-TotalResults")
                .expectHeader().doesNotExist("X-Pagination-PageSize")
                .expectBody()
                .json(
                  objectMapper.writeValueAsString(
                    listOf(userOne, userTwo).map {
                      userTransformer.transformJpaToApi(it, ServiceName.approvedPremises)
                    },
                  ),
                )
            }
          }
        }
      }
    }

    @ParameterizedTest
    @EnumSource(value = UserRole::class, names = ["CAS1_WORKFLOW_MANAGER", "CAS1_JANITOR", "CAS1_USER_MANAGER"])
    fun `GET to users with an approved role returns paginated list ordered by name`(role: UserRole) {
      givenAUser(roles = listOf(UserRole.CAS1_MATCHER)) { matcher, _ ->
        givenAUser(roles = listOf(UserRole.CAS1_FUTURE_MANAGER)) { manager, _ ->
          givenAUser { userWithNoRole, _ ->
            givenAUser(roles = listOf(role)) { requestUser, jwt ->
              webTestClient.get()
                .uri("/users?page=1")
                .header("Authorization", "Bearer $jwt")
                .header("X-Service-Name", ServiceName.approvedPremises.value)
                .exchange()
                .expectStatus()
                .isOk
                .expectHeader().valueEquals("X-Pagination-CurrentPage", 1)
                .expectHeader().valueEquals("X-Pagination-TotalPages", 1)
                .expectHeader().valueEquals("X-Pagination-TotalResults", 4)
                .expectHeader().valueEquals("X-Pagination-PageSize", 10)
                .expectBody()
                .json(
                  objectMapper.writeValueAsString(
                    listOf(requestUser, userWithNoRole, matcher, manager).map {
                      userTransformer.transformJpaToApi(it, ServiceName.approvedPremises)
                    },
                  ),
                )
            }
          }
        }
      }
    }

    @ParameterizedTest
    @EnumSource(value = UserRole::class, names = ["CAS1_WORKFLOW_MANAGER", "CAS1_JANITOR", "CAS1_USER_MANAGER"])
    fun `GET to users with an approved role allows filtering by roles`(role: UserRole) {
      givenAUser(roles = listOf(UserRole.CAS1_MATCHER, UserRole.CAS1_MATCHER)) { matcher, _ ->
        givenAUser(roles = listOf(UserRole.CAS1_FUTURE_MANAGER, UserRole.CAS1_FUTURE_MANAGER, UserRole.CAS1_FUTURE_MANAGER)) { future_manager, _ ->
          givenAUser { _, _ ->
            givenAUser(roles = listOf(role)) { _, jwt ->
              webTestClient.get()
                .uri("/users?roles=matcher,future_manager")
                .header("Authorization", "Bearer $jwt")
                .header("X-Service-Name", ServiceName.approvedPremises.value)
                .exchange()
                .expectStatus()
                .isOk
                .expectBody()
                .json(
                  objectMapper.writeValueAsString(
                    listOf(matcher, future_manager).map {
                      userTransformer.transformJpaToApi(it, ServiceName.approvedPremises)
                    },
                  ),
                )
            }
          }
        }
      }
    }

    @ParameterizedTest
    @EnumSource(value = UserRole::class, names = ["CAS1_WORKFLOW_MANAGER", "CAS1_JANITOR", "CAS1_USER_MANAGER"])
    fun `GET to users with an approved role allows filtering by qualifications`(role: UserRole) {
      givenAUser(qualifications = listOf(UserQualification.EMERGENCY)) { emergencyUser, _ ->
        givenAUser(qualifications = listOf(UserQualification.PIPE)) { _, _ ->
          givenAUser { _, _ ->
            givenAUser(roles = listOf(role)) { _, jwt ->
              webTestClient.get()
                .uri("/users?qualifications=emergency")
                .header("Authorization", "Bearer $jwt")
                .header("X-Service-Name", ServiceName.approvedPremises.value)
                .exchange()
                .expectStatus()
                .isOk
                .expectBody()
                .json(
                  objectMapper.writeValueAsString(
                    listOf(emergencyUser).map {
                      userTransformer.transformJpaToApi(it, ServiceName.approvedPremises)
                    },
                  ),
                )
            }
          }
        }
      }
    }

    @ParameterizedTest
    @EnumSource(value = UserRole::class, names = ["CAS1_WORKFLOW_MANAGER", "CAS1_JANITOR", "CAS1_USER_MANAGER"])
    fun `GET to users with an approved role allows filtering by role and qualifications`(
      role: UserRole,
    ) {
      givenAUser(
        roles = listOf(UserRole.CAS1_ASSESSOR),
        qualifications = listOf(UserQualification.EMERGENCY),
      ) { emergencyAssessor1, _ ->
        givenAUser(
          roles = listOf(UserRole.CAS1_ASSESSOR),
          qualifications = listOf(UserQualification.EMERGENCY),
        ) { emergencyAssessor2, _ ->
          givenAUser(roles = listOf(UserRole.CAS1_ASSESSOR)) { _, _ ->
            givenAUser { _, _ ->
              givenAUser(roles = listOf(role)) { _, jwt ->
                webTestClient.get()
                  .uri("/users?roles=assessor&qualifications=emergency")
                  .header("Authorization", "Bearer $jwt")
                  .header("X-Service-Name", ServiceName.approvedPremises.value)
                  .exchange()
                  .expectStatus()
                  .isOk
                  .expectBody()
                  .json(
                    objectMapper.writeValueAsString(
                      listOf(emergencyAssessor1, emergencyAssessor2).map {
                        userTransformer.transformJpaToApi(it, ServiceName.approvedPremises)
                      },
                    ),
                  )
              }
            }
          }
        }
      }
    }
  }

  @Nested
  inner class GetUsersSummary {
    @ParameterizedTest
    @EnumSource(
      value = UserRole::class,
      names = ["CAS1_WORKFLOW_MANAGER", "CAS1_JANITOR", "CAS1_USER_MANAGER"],
    )
    fun `GET user summary with X-Service-Name other than approved-premises is forbidden`(role: UserRole) {
      givenAUser(roles = listOf(role)) { _, jwt ->
        webTestClient.get()
          .uri("/users/summary")
          .header("Authorization", "Bearer $jwt")
          .header("X-Service-Name", ServiceName.temporaryAccommodation.value)
          .exchange()
          .expectStatus()
          .isForbidden
      }
    }

    @ParameterizedTest
    @EnumSource(
      value = UserRole::class,
      names = ["CAS1_CRU_MEMBER", "CAS1_CRU_MEMBER_FIND_AND_BOOK_BETA", "CAS1_WORKFLOW_MANAGER", "CAS1_JANITOR", "CAS1_USER_MANAGER"],
      mode = EnumSource.Mode.EXCLUDE,
    )
    fun `GET user summary with an unapproved role is forbidden`(role: UserRole) {
      givenAUser(roles = listOf(role)) { _, jwt ->
        webTestClient.get()
          .uri("/users/summary")
          .header("Authorization", "Bearer $jwt")
          .header("X-Service-Name", ServiceName.approvedPremises.value)
          .exchange()
          .expectStatus()
          .isForbidden
      }
    }

    @Test
    fun `GET user summary with no internal role (aka the Applicant pseudo-role) is forbidden`() {
      givenAUser { _, jwt ->
        webTestClient.get()
          .uri("/users/summary")
          .header("Authorization", "Bearer $jwt")
          .header("X-Service-Name", ServiceName.approvedPremises.value)
          .exchange()
          .expectStatus()
          .isForbidden
      }
    }

    @ParameterizedTest
    @EnumSource(value = UserRole::class, names = ["CAS1_WORKFLOW_MANAGER", "CAS1_JANITOR", "CAS1_USER_MANAGER"])
    fun `GET user summary with an approved role returns full list ordered by name`(role: UserRole) {
      givenAUser(roles = listOf(UserRole.CAS1_MATCHER)) { matcher, _ ->
        givenAUser(roles = listOf(UserRole.CAS1_FUTURE_MANAGER)) { manager, _ ->
          givenAUser { userWithNoRole, _ ->
            givenAUser(roles = listOf(role)) { requestUser, jwt ->
              webTestClient.get()
                .uri("/users/summary")
                .header("Authorization", "Bearer $jwt")
                .header("X-Service-Name", ServiceName.approvedPremises.value)
                .exchange()
                .expectStatus()
                .isOk
                .expectHeader().doesNotExist("X-Pagination-CurrentPage")
                .expectHeader().doesNotExist("X-Pagination-TotalPages")
                .expectHeader().doesNotExist("X-Pagination-TotalResults")
                .expectHeader().doesNotExist("X-Pagination-PageSize")
                .expectBody()
                .json(
                  objectMapper.writeValueAsString(
                    listOf(requestUser, userWithNoRole, matcher, manager).map {
                      userTransformer.transformJpaToSummaryApi(it)
                    },
                  ),
                )
            }
          }
        }
      }
    }

    @ParameterizedTest
    @EnumSource(value = UserRole::class, names = ["CAS1_WORKFLOW_MANAGER", "CAS1_JANITOR", "CAS1_USER_MANAGER"])
    fun `GET user summary with an approved role returns list filtered by region`(role: UserRole) {
      givenAUser(roles = listOf(UserRole.CAS1_MATCHER)) { matcher, _ ->
        givenAUser(roles = listOf(UserRole.CAS1_FUTURE_MANAGER)) { manager, _ ->
          givenAUser { userWithNoRole, _ ->
            givenAUser(roles = listOf(role)) { requestUser, jwt ->
              val apArea = givenAnApArea()
              val probationRegion = probationRegionEntityFactory.produceAndPersist {
                withApArea(apArea)
              }

              val userOne = userEntityFactory.produceAndPersist {
                withProbationRegion(probationRegion)
                withApArea(apArea)
              }

              val userTwo = userEntityFactory.produceAndPersist {
                withProbationRegion(probationRegion)
                withApArea(apArea)
              }

              webTestClient.get()
                .uri("/users/summary?probationRegionId=${probationRegion.id}")
                .header("Authorization", "Bearer $jwt")
                .header("X-Service-Name", ServiceName.approvedPremises.value)
                .exchange()
                .expectStatus()
                .isOk
                .expectHeader().doesNotExist("X-Pagination-CurrentPage")
                .expectHeader().doesNotExist("X-Pagination-TotalPages")
                .expectHeader().doesNotExist("X-Pagination-TotalResults")
                .expectHeader().doesNotExist("X-Pagination-PageSize")
                .expectBody()
                .json(
                  objectMapper.writeValueAsString(
                    listOf(userOne, userTwo).map {
                      userTransformer.transformJpaToSummaryApi(it)
                    },
                  ),
                )
            }
          }
        }
      }
    }

    @ParameterizedTest
    @EnumSource(value = UserRole::class, names = ["CAS1_WORKFLOW_MANAGER", "CAS1_JANITOR", "CAS1_USER_MANAGER"])
    fun `GET to users with an approved role returns list filtered by user's AP area`(role: UserRole) {
      givenAUser(roles = listOf(UserRole.CAS1_MATCHER)) { matcher, _ ->
        givenAUser(roles = listOf(UserRole.CAS1_FUTURE_MANAGER)) { manager, _ ->
          givenAUser { userWithNoRole, _ ->
            givenAUser(roles = listOf(role)) { requestUser, jwt ->
              val apArea = givenAnApArea()

              val probationRegionApArea = givenAnApArea()
              val probationRegion = probationRegionEntityFactory.produceAndPersist {
                withApArea(probationRegionApArea)
              }

              val userOne = userEntityFactory.produceAndPersist {
                withProbationRegion(probationRegion)
                withApArea(apArea)
              }

              val userTwo = userEntityFactory.produceAndPersist {
                withProbationRegion(probationRegion)
                withApArea(apArea)
              }

              webTestClient.get()
                .uri("/users/summary?apAreaId=${apArea.id}")
                .header("Authorization", "Bearer $jwt")
                .header("X-Service-Name", ServiceName.approvedPremises.value)
                .exchange()
                .expectStatus()
                .isOk
                .expectHeader().doesNotExist("X-Pagination-CurrentPage")
                .expectHeader().doesNotExist("X-Pagination-TotalPages")
                .expectHeader().doesNotExist("X-Pagination-TotalResults")
                .expectHeader().doesNotExist("X-Pagination-PageSize")
                .expectBody()
                .json(
                  objectMapper.writeValueAsString(
                    listOf(userOne, userTwo).map {
                      userTransformer.transformJpaToSummaryApi(it)
                    },
                  ),
                )
            }
          }
        }
      }
    }

    @ParameterizedTest
    @EnumSource(value = UserRole::class, names = ["CAS1_WORKFLOW_MANAGER", "CAS1_JANITOR", "CAS1_USER_MANAGER"])
    fun `GET to users with an approved role returns paginated list ordered by name`(role: UserRole) {
      givenAUser(roles = listOf(UserRole.CAS1_MATCHER)) { matcher, _ ->
        givenAUser(roles = listOf(UserRole.CAS1_FUTURE_MANAGER)) { manager, _ ->
          givenAUser { userWithNoRole, _ ->
            givenAUser(roles = listOf(role)) { requestUser, jwt ->
              webTestClient.get()
                .uri("/users/summary?page=1")
                .header("Authorization", "Bearer $jwt")
                .header("X-Service-Name", ServiceName.approvedPremises.value)
                .exchange()
                .expectStatus()
                .isOk
                .expectHeader().valueEquals("X-Pagination-CurrentPage", 1)
                .expectHeader().valueEquals("X-Pagination-TotalPages", 1)
                .expectHeader().valueEquals("X-Pagination-TotalResults", 4)
                .expectHeader().valueEquals("X-Pagination-PageSize", 10)
                .expectBody()
                .json(
                  objectMapper.writeValueAsString(
                    listOf(requestUser, userWithNoRole, matcher, manager).map {
                      userTransformer.transformJpaToSummaryApi(it)
                    },
                  ),
                )
            }
          }
        }
      }
    }

    @ParameterizedTest
    @EnumSource(value = UserRole::class, names = ["CAS1_WORKFLOW_MANAGER", "CAS1_JANITOR", "CAS1_USER_MANAGER"])
    fun `GET to users with an approved role allows filtering by roles`(role: UserRole) {
      givenAUser(roles = listOf(UserRole.CAS1_MATCHER, UserRole.CAS1_MATCHER)) { matcher, _ ->
        givenAUser(roles = listOf(UserRole.CAS1_FUTURE_MANAGER, UserRole.CAS1_FUTURE_MANAGER, UserRole.CAS1_FUTURE_MANAGER)) { future_manager, _ ->
          givenAUser { _, _ ->
            givenAUser(roles = listOf(role)) { _, jwt ->
              webTestClient.get()
                .uri("/users/summary?roles=matcher,future_manager")
                .header("Authorization", "Bearer $jwt")
                .header("X-Service-Name", ServiceName.approvedPremises.value)
                .exchange()
                .expectStatus()
                .isOk
                .expectBody()
                .json(
                  objectMapper.writeValueAsString(
                    listOf(matcher, future_manager).map {
                      userTransformer.transformJpaToSummaryApi(it)
                    },
                  ),
                )
            }
          }
        }
      }
    }

    @ParameterizedTest
    @EnumSource(value = UserRole::class, names = ["CAS1_WORKFLOW_MANAGER", "CAS1_JANITOR", "CAS1_USER_MANAGER"])
    fun `GET to users with an approved role allows filtering by qualifications`(role: UserRole) {
      givenAUser(qualifications = listOf(UserQualification.EMERGENCY)) { emergencyUser, _ ->
        givenAUser(qualifications = listOf(UserQualification.PIPE)) { _, _ ->
          givenAUser { _, _ ->
            givenAUser(roles = listOf(role)) { _, jwt ->
              webTestClient.get()
                .uri("/users/summary?qualifications=emergency")
                .header("Authorization", "Bearer $jwt")
                .header("X-Service-Name", ServiceName.approvedPremises.value)
                .exchange()
                .expectStatus()
                .isOk
                .expectBody()
                .json(
                  objectMapper.writeValueAsString(
                    listOf(emergencyUser).map {
                      userTransformer.transformJpaToSummaryApi(it)
                    },
                  ),
                )
            }
          }
        }
      }
    }

    @ParameterizedTest
    @EnumSource(value = UserRole::class, names = ["CAS1_WORKFLOW_MANAGER", "CAS1_JANITOR", "CAS1_USER_MANAGER"])
    fun `GET to users with an approved role allows filtering by role and qualifications`(
      role: UserRole,
    ) {
      givenAUser(
        roles = listOf(UserRole.CAS1_ASSESSOR),
        qualifications = listOf(UserQualification.EMERGENCY),
      ) { emergencyAssessor1, _ ->
        givenAUser(
          roles = listOf(UserRole.CAS1_ASSESSOR),
          qualifications = listOf(UserQualification.EMERGENCY),
        ) { emergencyAssessor2, _ ->
          givenAUser(roles = listOf(UserRole.CAS1_ASSESSOR)) { _, _ ->
            givenAUser { _, _ ->
              givenAUser(roles = listOf(role)) { _, jwt ->
                webTestClient.get()
                  .uri("/users/summary?roles=assessor&qualifications=emergency")
                  .header("Authorization", "Bearer $jwt")
                  .header("X-Service-Name", ServiceName.approvedPremises.value)
                  .exchange()
                  .expectStatus()
                  .isOk
                  .expectBody()
                  .json(
                    objectMapper.writeValueAsString(
                      listOf(emergencyAssessor1, emergencyAssessor2).map {
                        userTransformer.transformJpaToSummaryApi(it)
                      },
                    ),
                  )
              }
            }
          }
        }
      }
    }
  }

  @Nested
  inner class SearchByUserName {
    @ParameterizedTest
    @EnumSource(value = UserRole::class, names = ["CAS1_WORKFLOW_MANAGER", "CAS1_JANITOR", "CAS1_USER_MANAGER"])
    fun `GET to user search with X-Service-Name other than approved-premises is forbidden`(role: UserRole) {
      givenAUser(roles = listOf(role)) { _, jwt ->
        webTestClient.get()
          .uri("/users/search?name=som")
          .header("Authorization", "Bearer $jwt")
          .header("X-Service-Name", ServiceName.temporaryAccommodation.value)
          .exchange()
          .expectStatus()
          .isForbidden
      }
    }

    @ParameterizedTest
    @EnumSource(
      value = UserRole::class,
      names = ["CAS1_CRU_MEMBER", "CAS1_CRU_MEMBER_FIND_AND_BOOK_BETA", "CAS1_WORKFLOW_MANAGER", "CAS1_JANITOR", "CAS1_USER_MANAGER"],
      mode = EnumSource.Mode.EXCLUDE,
    )
    fun `GET to user search with an unapproved role is forbidden`(role: UserRole) {
      givenAUser(roles = listOf(role)) { _, jwt ->
        webTestClient.get()
          .uri("/users/search?name=some")
          .header("Authorization", "Bearer $jwt")
          .header("X-Service-Name", ServiceName.approvedPremises.value)
          .exchange()
          .expectStatus()
          .isForbidden
      }
    }

    @Test
    fun `GET to user search with no internal role (aka the Applicant pseudo-role) is forbidden`() {
      givenAUser { _, jwt ->
        webTestClient.get()
          .uri("/users/search?name=some")
          .header("Authorization", "Bearer $jwt")
          .header("X-Service-Name", ServiceName.approvedPremises.value)
          .exchange()
          .expectStatus()
          .isForbidden
      }
    }

    @ParameterizedTest
    @EnumSource(value = UserRole::class, names = ["CAS1_WORKFLOW_MANAGER", "CAS1_JANITOR", "CAS1_USER_MANAGER"])
    fun `GET to search users with approved role returns a user`(role: UserRole) {
      givenAUser(
        staffDetail = StaffDetailFactory.staffDetail(name = PersonName(forename = "SomeUserName", surname = "")),
      ) { user, _ ->
        givenAUser(
          staffDetail = StaffDetailFactory.staffDetail(name = PersonName(forename = "fail", surname = "")),
        ) { _, _ ->
          givenAUser(roles = listOf(role)) { _, jwt ->
            webTestClient.get()
              .uri("/users/search?name=some")
              .header("Authorization", "Bearer $jwt")
              .header("X-Service-Name", ServiceName.approvedPremises.value)
              .exchange()
              .expectStatus()
              .isOk
              .expectBody()
              .json(
                objectMapper.writeValueAsString(
                  listOf(user).map {
                    userTransformer.transformJpaToApi(it, ServiceName.approvedPremises)
                  },
                ),
              )
          }
        }
      }
    }
  }

  @Nested
  inner class SearchByDeliusUserName {
    @ParameterizedTest
    @EnumSource(value = UserRole::class, names = ["CAS1_WORKFLOW_MANAGER", "CAS1_JANITOR", "CAS1_USER_MANAGER"])
    fun `GET to user search delius username with X-Service-Name other than approved-premises is forbidden`(role: UserRole) {
      givenAUser(roles = listOf(role)) { _, jwt ->
        webTestClient.get()
          .uri("/users/delius?name=som")
          .header("Authorization", "Bearer $jwt")
          .header("X-Service-Name", ServiceName.temporaryAccommodation.value)
          .exchange()
          .expectStatus()
          .isForbidden
      }
    }

    @ParameterizedTest
    @EnumSource(
      value = UserRole::class,
      names = ["CAS1_CRU_MEMBER", "CAS1_CRU_MEMBER_FIND_AND_BOOK_BETA", "CAS1_WORKFLOW_MANAGER", "CAS1_JANITOR", "CAS1_USER_MANAGER"],
      mode = EnumSource.Mode.EXCLUDE,
    )
    fun `GET user search delius username with an unapproved role is forbidden`(role: UserRole) {
      givenAUser(roles = listOf(role)) { _, jwt ->
        webTestClient.get()
          .uri("/users/delius?name=some")
          .header("Authorization", "Bearer $jwt")
          .header("X-Service-Name", ServiceName.approvedPremises.value)
          .exchange()
          .expectStatus()
          .isForbidden
      }
    }

    @Test
    fun `GET to user search delius username with no internal role (aka the Applicant pseudo-role) is forbidden`() {
      givenAUser { _, jwt ->
        webTestClient.get()
          .uri("/users/delius?name=some")
          .header("Authorization", "Bearer $jwt")
          .header("X-Service-Name", ServiceName.approvedPremises.value)
          .exchange()
          .expectStatus()
          .isForbidden
      }
    }

    @ParameterizedTest
    @EnumSource(value = UserRole::class, names = ["CAS1_WORKFLOW_MANAGER", "CAS1_JANITOR", "CAS1_USER_MANAGER"])
    fun `GET to search users delius username with an approved role returns a user`(role: UserRole) {
      givenAUser(
        staffDetail = StaffDetailFactory.staffDetail(deliusUsername = "SOME"),
      ) { user, _ ->
        givenAUser(
          staffDetail = StaffDetailFactory.staffDetail(name = PersonName(forename = "fail", surname = "r")),
        ) { _, _ ->
          givenAUser(roles = listOf(role)) { _, jwt ->
            webTestClient.get()
              .uri("/users/delius?name=some")
              .header("Authorization", "Bearer $jwt")
              .header("X-Service-Name", ServiceName.approvedPremises.value)
              .exchange()
              .expectStatus()
              .isOk
              .expectBody()
              .json(
                objectMapper.writeValueAsString(
                  userTransformer.transformJpaToApi(user, ServiceName.approvedPremises),
                ),
              )
          }
        }
      }
    }

    @ParameterizedTest
    @EnumSource(value = UserRole::class, names = ["CAS1_WORKFLOW_MANAGER", "CAS1_JANITOR", "CAS1_USER_MANAGER"])
    fun `GET to search for a delius username that does not exist with an approved role returns 404`(
      role: UserRole,
    ) {
      givenAUser(roles = listOf(role)) { _, jwt ->
        webTestClient.get()
          .uri("/users/delius?name=noone")
          .header("Authorization", "Bearer $jwt")
          .header("X-Service-Name", ServiceName.approvedPremises.value)
          .exchange()
          .expectHeader().contentType("application/problem+json")
          .expectStatus()
          .isNotFound
          .expectBody()
          .jsonPath("title").isEqualTo("Not Found")
          .jsonPath("status").isEqualTo(404)
          .jsonPath("detail").isEqualTo("No user with username of noone could be found")
      }
    }
  }

  @Nested
  inner class UpdateUser {
    @ParameterizedTest
    @EnumSource(value = UserRole::class, names = ["CAS1_WORKFLOW_MANAGER", "CAS1_JANITOR", "CAS1_USER_MANAGER"])
    fun `Updating a user update with X-Service-Name other than approved-premises is forbidden`(role: UserRole) {
      givenAUser(roles = listOf(role)) { _, jwt ->
        val id = UUID.randomUUID()
        webTestClient.put()
          .uri("/users/$id")
          .header("Authorization", "Bearer $jwt")
          .header("X-Service-Name", ServiceName.temporaryAccommodation.value)
          .header("Content-Type", "application/json")
          .bodyValue(
            UserRolesAndQualifications(
              listOf<ApprovedPremisesUserRole>(),
              listOf<APIUserQualification>(),
            ),
          )
          .exchange()
          .expectStatus()
          .isForbidden
      }
    }

    @ParameterizedTest
    @EnumSource(value = UserRole::class, names = ["CAS1_FUTURE_MANAGER", "CAS1_MATCHER", "CAS1_JANITOR", "CAS1_USER_MANAGER"], mode = EnumSource.Mode.EXCLUDE)
    fun `Updating a user without an approved role is forbidden`(role: UserRole) {
      givenAUser(roles = listOf(role)) { _, jwt ->
        val id = UUID.randomUUID()
        webTestClient.put()
          .uri("/users/$id")
          .header("Authorization", "Bearer $jwt")
          .header("X-Service-Name", ServiceName.temporaryAccommodation.value)
          .header("Content-Type", "application/json")
          .bodyValue(
            UserRolesAndQualifications(
              listOf<ApprovedPremisesUserRole>(),
              listOf<APIUserQualification>(),
            ),
          )
          .exchange()
          .expectStatus()
          .isForbidden
      }
    }

    @Test
    fun `Updating a users with no internal role (aka the Applicant pseudo-role) is forbidden`() {
      givenAUser { _, jwt ->
        val id = UUID.randomUUID()
        webTestClient.put()
          .uri("/users/$id")
          .header("Authorization", "Bearer $jwt")
          .header("X-Service-Name", ServiceName.approvedPremises.value)
          .bodyValue(
            UserRolesAndQualifications(
              listOf<ApprovedPremisesUserRole>(),
              listOf<APIUserQualification>(),
            ),
          )
          .exchange()
          .expectStatus()
          .isForbidden
      }
    }

    @ParameterizedTest
    @EnumSource(value = UserRole::class, names = ["CAS1_JANITOR", "CAS1_USER_MANAGER"])
    fun `Updating a user returns OK with correct body when user has an approved role`(role: UserRole) {
      val id = UUID.randomUUID()
      val qualifications = listOf(APIUserQualification.emergency, APIUserQualification.pipe)
      val roles = listOf(
        ApprovedPremisesUserRole.assessor,
        ApprovedPremisesUserRole.reportViewer,
        ApprovedPremisesUserRole.excludedFromAssessAllocation,
        ApprovedPremisesUserRole.excludedFromMatchAllocation,
        ApprovedPremisesUserRole.excludedFromPlacementApplicationAllocation,
      )
      val region = givenAProbationRegion()

      givenAUser(
        id = id,
        isActive = false,
        probationRegion = region,
      )

      givenAUser(roles = listOf(role)) { _, jwt ->
        webTestClient.put()
          .uri("/users/$id")
          .header("Authorization", "Bearer $jwt")
          .header("X-Service-Name", ServiceName.approvedPremises.value)
          .bodyValue(
            UserRolesAndQualifications(
              roles = roles,
              qualifications = qualifications,
            ),
          )
          .exchange()
          .expectStatus()
          .isOk
          .expectBody()
          .jsonPath(".qualifications").isArray
          .jsonPath(".qualifications[0]").isEqualTo("emergency")
          .jsonPath(".roles").isArray
          .jsonPath(".roles[0]").isEqualTo(ApprovedPremisesUserRole.assessor.value)
          .jsonPath(".roles[1]").isEqualTo(ApprovedPremisesUserRole.reportViewer.value)
          .jsonPath(".roles[2]").isEqualTo(ApprovedPremisesUserRole.excludedFromAssessAllocation.value)
          .jsonPath(".roles[3]").isEqualTo(ApprovedPremisesUserRole.excludedFromMatchAllocation.value)
          .jsonPath(".roles[4]").isEqualTo(ApprovedPremisesUserRole.excludedFromPlacementApplicationAllocation.value)
          .jsonPath(".isActive").isEqualTo(true)
      }
    }
  }

  @Nested
  inner class DeleteUser {
    @ParameterizedTest
    @EnumSource(value = UserRole::class, names = ["CAS1_WORKFLOW_MANAGER", "CAS1_JANITOR", "CAS1_USER_MANAGER"])
    fun `Deleting a user with X-Service-Name other than approved-premises is forbidden`(role: UserRole) {
      givenAUser(roles = listOf(role)) { _, jwt ->
        val id = UUID.randomUUID()
        webTestClient.delete()
          .uri("/users/$id")
          .header("Authorization", "Bearer $jwt")
          .header("X-Service-Name", ServiceName.temporaryAccommodation.value)
          .header("Content-Type", "application/json")
          .exchange()
          .expectStatus()
          .isForbidden
      }
    }

    @ParameterizedTest
    @EnumSource(value = UserRole::class, names = ["CAS1_WORKFLOW_MANAGER", "CAS1_JANITOR", "CAS1_USER_MANAGER"], mode = EnumSource.Mode.EXCLUDE)
    fun `Deleting a user with an unapproved role is forbidden`(role: UserRole) {
      givenAUser(roles = listOf(role)) { _, jwt ->
        val id = UUID.randomUUID()
        webTestClient.delete()
          .uri("/users/$id")
          .header("Authorization", "Bearer $jwt")
          .header("X-Service-Name", ServiceName.approvedPremises.value)
          .header("Content-Type", "application/json")
          .exchange()
          .expectStatus()
          .isForbidden
      }
    }

    @Test
    fun `Deleting a user with no internal role (aka the Applicant pseudo-role) is forbidden`() {
      givenAUser { _, jwt ->
        val id = UUID.randomUUID()
        webTestClient.delete()
          .uri("/users/$id")
          .header("Authorization", "Bearer $jwt")
          .header("X-Service-Name", ServiceName.approvedPremises.value)
          .exchange()
          .expectStatus()
          .isForbidden
      }
    }

    @ParameterizedTest
    @EnumSource(value = UserRole::class, names = ["CAS1_WORKFLOW_MANAGER", "CAS1_JANITOR", "CAS1_USER_MANAGER"])
    fun `Deleting a user with no X-Service-Name is forbidden`() {
      givenAUser { _, jwt ->
        val id = UUID.randomUUID()
        webTestClient.delete()
          .uri("/users/$id")
          .header("Authorization", "Bearer $jwt")
          .exchange()
          .expectStatus()
          .is4xxClientError
          .expectBody()
          .jsonPath("title").isEqualTo("Bad Request")
          .jsonPath("detail").isEqualTo("Missing required header X-Service-Name")
      }
    }

    @Test
    fun `Deleting a user with a non-Delius JWT returns 403`() {
      val jwt = jwtAuthHelper.createClientCredentialsJwt(
        username = "username",
        authSource = "nomis",
      )

      webTestClient.delete()
        .uri("/users/$id")
        .header("Authorization", "Bearer $jwt")
        .header("X-Service-Name", ServiceName.approvedPremises.value)
        .exchange()
        .expectStatus()
        .isForbidden
    }

    @ParameterizedTest
    @EnumSource(value = UserRole::class, names = ["CAS1_JANITOR", "CAS1_USER_MANAGER"])
    fun `Deleting a user with an approved role deletes successfully`(role: UserRole) {
      userEntityFactory.produceAndPersist {
        withId(id)
        withYieldedProbationRegion { givenAProbationRegion() }
      }

      givenAUser(roles = listOf(role)) { _, jwt ->
        webTestClient.delete()
          .uri("/users/$id")
          .header("Authorization", "Bearer $jwt")
          .header("X-Service-Name", ServiceName.approvedPremises.value)
          .exchange()
          .expectStatus()
          .isOk

        val userFromDatabase = userRepository.findByIdOrNull(id)
        assertThat(userFromDatabase?.isActive).isEqualTo(false)
      }
    }

    @ParameterizedTest
    @EnumSource(value = UserRole::class, names = ["CAS1_WORKFLOW_MANAGER", "CAS1_JANITOR", "CAS1_USER_MANAGER"], mode = EnumSource.Mode.EXCLUDE)
    fun `Deleting a user without an approved role is forbidden `(role: UserRole) {
      givenAUser(roles = listOf(role)) { _, jwt ->
        webTestClient.delete()
          .uri("/users/$id")
          .header("Authorization", "Bearer $jwt")
          .header("X-Service-Name", ServiceName.approvedPremises.value)
          .exchange()
          .expectStatus()
          .isForbidden
      }
    }
  }
}
