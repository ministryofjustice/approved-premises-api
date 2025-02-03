package uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.cas1

import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.client.WireMock.aResponse
import com.github.tomakehurst.wiremock.client.WireMock.get
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ApArea
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ApprovedPremisesUser
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1ApplicationStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1ApplicationTimeline
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1PersonalTimeline
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1TimelineEvent
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1TimelineEventAssociatedUrl
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1TimelineEventType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1TimelineEventUrlType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.NamedId
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ProbationRegion
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.InitialiseDatabasePerClassTestBase
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenAUser
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenAnApplication
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenAnOffender
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserQualification
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.PersonInfoResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.PersonTransformer
import kotlin.collections.listOf

class Cas1PersonalTimelineTest : InitialiseDatabasePerClassTestBase() {
  @Autowired
  lateinit var personTransformer: PersonTransformer

  @Test
  fun `Getting a personal timeline for a CRN without a JWT returns 401`() {
    webTestClient.get()
      .uri("/cas1/people/CRN/timeline")
      .exchange()
      .expectStatus()
      .isUnauthorized
  }

  @Test
  fun `Getting a personal timeline for a CRN with a non-Delius JWT returns 403`() {
    val jwt = jwtAuthHelper.createClientCredentialsJwt(
      username = "username",
      authSource = "other-auth-source",
    )

    webTestClient.get()
      .uri("/cas1/people/CRN/timeline")
      .header("Authorization", "Bearer $jwt")
      .exchange()
      .expectStatus()
      .isForbidden
  }

  @Test
  fun `Getting a personal timeline for a CRN with a NOMIS JWT returns 403`() {
    val jwt = jwtAuthHelper.createClientCredentialsJwt(
      username = "username",
      authSource = "nomis",
    )

    webTestClient.get()
      .uri("/cas1/people/CRN/timeline")
      .header("Authorization", "Bearer $jwt")
      .exchange()
      .expectStatus()
      .isForbidden
  }

  @Test
  fun `Getting a personal timeline for a CRN with ROLE_POM returns 403`() {
    val jwt = jwtAuthHelper.createAuthorizationCodeJwt(
      subject = "username",
      authSource = "delius",
      roles = listOf("ROLE_POM"),
    )

    webTestClient.get()
      .uri("/cas1/people/CRN/timeline")
      .header("Authorization", "Bearer $jwt")
      .exchange()
      .expectStatus()
      .isForbidden
  }

  @Test
  fun `Getting a personal timeline for a CRN without ROLE_PROBATION returns 403`() {
    val jwt = jwtAuthHelper.createAuthorizationCodeJwt(
      subject = "username",
      authSource = "delius",
      roles = listOf("ROLE_OTHER"),
    )

    webTestClient.get()
      .uri("/cas1/people/CRN/timeline")
      .header("Authorization", "Bearer $jwt")
      .exchange()
      .expectStatus()
      .isForbidden
  }

  @Test
  fun `Getting a personal timeline for a CRN that does not exist returns 404`() {
    givenAUser { _, jwt ->
      wiremockServer.stubFor(
        get(WireMock.urlEqualTo("/secure/offenders/crn/CRN"))
          .willReturn(
            aResponse()
              .withHeader("Content-Type", "application/json")
              .withStatus(404),
          ),
      )

      webTestClient.get()
        .uri("/cas1/people/CRN/timeline")
        .header("Authorization", "Bearer $jwt")
        .exchange()
        .expectStatus()
        .isNotFound
    }
  }

  @Test
  fun `Getting a personal timeline for a CRN returns OK with correct body`() {
    givenAUser { userEntity, jwt ->
      givenAnOffender { offenderDetails, inmateDetails ->
        givenAnApplication(userEntity, crn = offenderDetails.otherIds.crn) { application ->
          val domainEvents = domainEventFactory.produceAndPersistMultiple(2) {
            withCrn(offenderDetails.otherIds.crn)
            withApplicationId(application.id)
          }

          val personInfoResult = PersonInfoResult.Success.Full(
            crn = offenderDetails.otherIds.crn,
            offenderDetailSummary = offenderDetails,
            inmateDetail = inmateDetails,
          )

          val apArea = userEntity.apArea!!

          webTestClient.get()
            .uri("/cas1/people/${offenderDetails.otherIds.crn}/timeline")
            .header("Authorization", "Bearer $jwt")
            .exchange()
            .expectStatus()
            .isOk
            .expectBody()
            .json(
              objectMapper.writeValueAsString(
                Cas1PersonalTimeline(
                  person = personTransformer.transformModelToPersonApi(personInfoResult),
                  applications = listOf(
                    Cas1ApplicationTimeline(
                      id = application.id,
                      createdAt = application.createdAt.toInstant(),
                      status = Cas1ApplicationStatus.started,
                      isOfflineApplication = false,
                      createdBy = ApprovedPremisesUser(
                        qualifications = emptyList(),
                        roles = emptyList(),
                        permissions = emptyList(),
                        apArea = ApArea(
                          id = apArea.id,
                          identifier = apArea.identifier,
                          name = apArea.name,
                        ),
                        service = "CAS1",
                        id = userEntity.id,
                        name = userEntity.name,
                        deliusUsername = userEntity.deliusUsername,
                        region = ProbationRegion(
                          id = userEntity.probationRegion.id,
                          name = userEntity.probationRegion.name,
                        ),
                        email = userEntity.email,
                        telephoneNumber = userEntity.telephoneNumber,
                        isActive = userEntity.isActive,
                        version = 993,
                        cruManagementArea = NamedId(
                          id = userEntity.cruManagementArea!!.id,
                          name = userEntity.cruManagementArea!!.name,
                        ),
                        cruManagementAreaDefault = NamedId(
                          id = apArea.defaultCruManagementArea.id,
                          name = apArea.defaultCruManagementArea.name,
                        ),
                        cruManagementAreaOverride = null,
                      ),
                      timelineEvents = listOf(
                        Cas1TimelineEvent(
                          type = Cas1TimelineEventType.applicationSubmitted,
                          id = domainEvents[0].id.toString(),
                          occurredAt = domainEvents[0].occurredAt.toInstant(),
                          content = "The application was submitted",
                          createdBy = null,
                          associatedUrls = listOf(
                            Cas1TimelineEventAssociatedUrl(
                              type = Cas1TimelineEventUrlType.application,
                              url = "http://frontend/applications/${application.id}",
                            ),
                          ),
                        ),
                        Cas1TimelineEvent(
                          type = Cas1TimelineEventType.applicationSubmitted,
                          id = domainEvents[1].id.toString(),
                          occurredAt = domainEvents[1].occurredAt.toInstant(),
                          content = "The application was submitted",
                          createdBy = null,
                          associatedUrls = listOf(
                            Cas1TimelineEventAssociatedUrl(
                              type = Cas1TimelineEventUrlType.application,
                              url = "http://frontend/applications/${application.id}",
                            ),
                          ),
                        ),
                      ),
                    ),
                  ),
                ),
              ),
            )
        }
      }
    }
  }

  @Test
  fun `Getting a personal timeline for a CRN with no applications returns OK with correct body`() {
    givenAUser { _, jwt ->
      givenAnOffender { offenderDetails, inmateDetails ->
        val personInfoResult = PersonInfoResult.Success.Full(
          crn = offenderDetails.otherIds.crn,
          offenderDetailSummary = offenderDetails,
          inmateDetail = inmateDetails,
        )

        webTestClient.get()
          .uri("/cas1/people/${offenderDetails.otherIds.crn}/timeline")
          .header("Authorization", "Bearer $jwt")
          .exchange()
          .expectStatus()
          .isOk
          .expectBody()
          .json(
            objectMapper.writeValueAsString(
              Cas1PersonalTimeline(
                person = personTransformer.transformModelToPersonApi(personInfoResult),
                applications = emptyList(),
              ),
            ),
          )
      }
    }
  }

  @Test
  fun `Getting a personal timeline for a CRN without a NomsNumber returns OK with correct body`() {
    givenAUser { userEntity, jwt ->
      givenAnOffender(
        offenderDetailsConfigBlock = {
          withNomsNumber(null)
        },
      ) { offenderDetails, _ ->
        givenAnApplication(userEntity, crn = offenderDetails.otherIds.crn) { application ->
          val domainEvents = domainEventFactory.produceAndPersistMultiple(2) {
            withCrn(offenderDetails.otherIds.crn)
            withApplicationId(application.id)
          }

          val personInfoResult = PersonInfoResult.Success.Full(
            crn = offenderDetails.otherIds.crn,
            offenderDetailSummary = offenderDetails,
            inmateDetail = null,
          )

          val apArea = userEntity.apArea!!

          webTestClient.get()
            .uri("/cas1/people/${offenderDetails.otherIds.crn}/timeline")
            .header("Authorization", "Bearer $jwt")
            .exchange()
            .expectStatus()
            .isOk
            .expectBody()
            .json(
              objectMapper.writeValueAsString(
                Cas1PersonalTimeline(
                  person = personTransformer.transformModelToPersonApi(personInfoResult),
                  applications = listOf(
                    Cas1ApplicationTimeline(
                      id = application.id,
                      createdAt = application.createdAt.toInstant(),
                      status = Cas1ApplicationStatus.started,
                      isOfflineApplication = false,
                      createdBy = ApprovedPremisesUser(
                        qualifications = emptyList(),
                        roles = emptyList(),
                        permissions = emptyList(),
                        apArea = ApArea(
                          id = apArea.id,
                          identifier = apArea.identifier,
                          name = apArea.name,
                        ),
                        service = "CAS1",
                        id = userEntity.id,
                        name = userEntity.name,
                        deliusUsername = userEntity.deliusUsername,
                        region = ProbationRegion(
                          id = userEntity.probationRegion.id,
                          name = userEntity.probationRegion.name,
                        ),
                        email = userEntity.email,
                        telephoneNumber = userEntity.telephoneNumber,
                        isActive = userEntity.isActive,
                        version = 993,
                        cruManagementArea = NamedId(
                          id = userEntity.cruManagementArea!!.id,
                          name = userEntity.cruManagementArea!!.name,
                        ),
                        cruManagementAreaDefault = NamedId(
                          id = apArea.defaultCruManagementArea.id,
                          name = apArea.defaultCruManagementArea.name,
                        ),
                        cruManagementAreaOverride = null,
                      ),
                      timelineEvents = listOf(
                        Cas1TimelineEvent(
                          type = Cas1TimelineEventType.applicationSubmitted,
                          id = domainEvents[0].id.toString(),
                          occurredAt = domainEvents[0].occurredAt.toInstant(),
                          content = "The application was submitted",
                          createdBy = null,
                          associatedUrls = listOf(
                            Cas1TimelineEventAssociatedUrl(
                              type = Cas1TimelineEventUrlType.application,
                              url = "http://frontend/applications/${application.id}",
                            ),
                          ),
                        ),
                        Cas1TimelineEvent(
                          type = Cas1TimelineEventType.applicationSubmitted,
                          id = domainEvents[1].id.toString(),
                          occurredAt = domainEvents[1].occurredAt.toInstant(),
                          content = "The application was submitted",
                          createdBy = null,
                          associatedUrls = listOf(
                            Cas1TimelineEventAssociatedUrl(
                              type = Cas1TimelineEventUrlType.application,
                              url = "http://frontend/applications/${application.id}",
                            ),
                          ),
                        ),
                      ),
                    ),
                  ),
                ),
              ),
            )
        }
      }
    }
  }

  @Test
  fun `Getting a personal timeline for a CRN where the Offender is LAO, and the user does have LAO qualification returns OK with correct body`() {
    givenAUser(qualifications = listOf(UserQualification.LAO)) { _, jwt ->
      givenAnOffender(
        offenderDetailsConfigBlock = { withCurrentRestriction(true) },
      ) { offenderDetails, inmateDetails ->
        val personInfoResult = PersonInfoResult.Success.Full(
          crn = offenderDetails.otherIds.crn,
          offenderDetailSummary = offenderDetails,
          inmateDetail = inmateDetails,
        )

        val offlineApplication = offlineApplicationEntityFactory.produceAndPersist {
          withCrn(offenderDetails.otherIds.crn)
        }

        val domainEvents = domainEventFactory.produceAndPersistMultiple(2) {
          withCrn(offenderDetails.otherIds.crn)
          withApplicationId(offlineApplication.id)
        }

        webTestClient.get()
          .uri("/cas1/people/${offenderDetails.otherIds.crn}/timeline")
          .header("Authorization", "Bearer $jwt")
          .exchange()
          .expectStatus()
          .isOk
          .expectBody()
          .json(
            objectMapper.writeValueAsString(
              Cas1PersonalTimeline(
                person = personTransformer.transformModelToPersonApi(personInfoResult),
                applications = listOf(
                  Cas1ApplicationTimeline(
                    id = offlineApplication.id,
                    createdAt = offlineApplication.createdAt.toInstant(),
                    status = null,
                    isOfflineApplication = true,
                    createdBy = null,
                    timelineEvents = listOf(
                      Cas1TimelineEvent(
                        type = Cas1TimelineEventType.applicationSubmitted,
                        id = domainEvents[0].id.toString(),
                        occurredAt = domainEvents[0].occurredAt.toInstant(),
                        content = "The application was submitted",
                        createdBy = null,
                        associatedUrls = listOf(
                          Cas1TimelineEventAssociatedUrl(
                            type = Cas1TimelineEventUrlType.application,
                            url = "http://frontend/applications/${offlineApplication.id}",
                          ),
                        ),
                      ),
                      Cas1TimelineEvent(
                        type = Cas1TimelineEventType.applicationSubmitted,
                        id = domainEvents[1].id.toString(),
                        occurredAt = domainEvents[1].occurredAt.toInstant(),
                        content = "The application was submitted",
                        createdBy = null,
                        associatedUrls = listOf(
                          Cas1TimelineEventAssociatedUrl(
                            type = Cas1TimelineEventUrlType.application,
                            url = "http://frontend/applications/${offlineApplication.id}",
                          ),
                        ),
                      ),
                    ),
                  ),
                ),
              ),
            ),
          )
      }
    }
  }

  @Test
  fun `Getting a personal timeline for a CRN where the Offender is LAO,but the user does not have LAO qualification returns OK with empty timeline`() {
    givenAUser { _, jwt ->
      givenAnOffender(
        offenderDetailsConfigBlock = { withCurrentRestriction(true) },
      ) { offenderDetails, inmateDetails ->
        val personInfoResult = PersonInfoResult.Success.Restricted(
          crn = offenderDetails.otherIds.crn,
          nomsNumber = offenderDetails.otherIds.nomsNumber!!,
        )

        webTestClient.get()
          .uri("/cas1/people/${offenderDetails.otherIds.crn}/timeline")
          .header("Authorization", "Bearer $jwt")
          .exchange()
          .expectStatus()
          .isOk
          .expectBody()
          .json(
            objectMapper.writeValueAsString(
              Cas1PersonalTimeline(
                person = personTransformer.transformModelToPersonApi(personInfoResult),
                applications = emptyList(),
              ),
            ),
          )
      }
    }
  }
}
