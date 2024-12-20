package uk.gov.justice.digital.hmpps.approvedpremisesapi.integration

import com.github.tomakehurst.wiremock.client.WireMock
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ContextStaffMemberFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.OffenderDetailsSummaryFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenAProbationRegion
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenAUser
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.httpmocks.apDeliusContextMockSuccessfulStaffMembersCall
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserRole
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.community.OffenderDetailSummary
import java.util.UUID

@Disabled("flaky")
class CacheClearTest : IntegrationTestBase() {
  @Test
  fun `Clearing a regular cache results in upstream calls being made again`() {
    givenAUser(roles = listOf(UserRole.CAS1_FUTURE_MANAGER)) { _, jwt ->
      val premisesId = UUID.fromString("d687f947-ff80-431e-9c72-75f704730978")
      val qCode = "FOUND"

      setupQCodeUpstreamResponse(premisesId, qCode)

      // Given I call the qCode endpoint twice
      repeat(2) {
        webTestClient.get()
          .uri("/premises/$premisesId/staff")
          .header("Authorization", "Bearer $jwt")
          .exchange()
          .expectStatus()
          .isOk
      }

      // Then the upstream request should only have been made once
      validateUpstreamQCodeRequestMade(qCode, times = 1)

      // When I clear the qCode cache
      webTestClient.delete()
        .uri("/cache/qCodeStaffMembers")
        .exchange()
        .expectStatus()
        .isOk

      // And I call the endpoint again
      webTestClient.get()
        .uri("/premises/$premisesId/staff")
        .header("Authorization", "Bearer $jwt")
        .exchange()
        .expectStatus()
        .isOk

      // Then the upstream request should be made again
      validateUpstreamQCodeRequestMade(qCode, times = 2)
    }
  }

  @Test
  fun `Clearing a preemptive cache results in upstream calls being made again`() {
    val offenderDetails = OffenderDetailsSummaryFactory()
      .produce()

    // Given a Booking for an Offender
    setupBookingForOffender(offenderDetails)

    // Then OffenderDetailsCacheRefreshWorker should pick up the CRN and preemptively load it
    validateUpstreamOffenderDetailsRequestEventuallyMade(offenderDetails.otherIds.crn, times = 1)

    // When I clear the offenderDetails cache
    webTestClient.delete()
      .uri("/cache/offenderDetails")
      .exchange()
      .expectStatus()
      .isOk

    // Then OffenderDetailsCacheRefreshWorker should pick up the CRN and preemptively load it again
    validateUpstreamOffenderDetailsRequestEventuallyMade(offenderDetails.otherIds.crn, times = 2)
  }

  private fun setupBookingForOffender(offenderDetails: OffenderDetailSummary) {
    val premises = approvedPremisesEntityFactory.produceAndPersist {
      withProbationRegion(givenAProbationRegion())
      withLocalAuthorityArea(localAuthorityEntityFactory.produceAndPersist())
    }

    val room = roomEntityFactory.produceAndPersist {
      withPremises(premises)
    }

    val bed = bedEntityFactory.produceAndPersist {
      withRoom(room)
    }

    bookingEntityFactory.produceAndPersist {
      withPremises(premises)
      withBed(bed)
      withCrn(offenderDetails.otherIds.crn)
    }
  }

  private fun validateUpstreamOffenderDetailsRequestEventuallyMade(crn: String, times: Int) {
    var attempts = 1
    while (attempts <= 200) {
      try {
        validateUpstreamOffenderDetailsRequestMade(crn, times)
        break
      } catch (throwable: Throwable) {
        if (attempts == 200) {
          throw RuntimeException("Upstream endpoint was never called by OffenderDetailsCacheRefreshWorker", throwable)
        }
      }

      attempts += 1
      Thread.sleep(500)
    }
  }

  private fun validateUpstreamQCodeRequestMade(qCode: String, times: Int) =
    wiremockServer.verify(WireMock.exactly(times), WireMock.getRequestedFor(WireMock.urlEqualTo("/approved-premises/$qCode/staff")))

  private fun setupQCodeUpstreamResponse(premisesId: UUID, qCode: String) {
    approvedPremisesEntityFactory.produceAndPersist {
      withId(premisesId)
      withYieldedLocalAuthorityArea { localAuthorityEntityFactory.produceAndPersist() }
      withYieldedProbationRegion { givenAProbationRegion() }
      withQCode(qCode)
    }

    apDeliusContextMockSuccessfulStaffMembersCall(ContextStaffMemberFactory().produce(), qCode)
  }

  private fun validateUpstreamOffenderDetailsRequestMade(crn: String, times: Int) =
    wiremockServer.verify(WireMock.exactly(times), WireMock.getRequestedFor(WireMock.urlEqualTo("/secure/offenders/crn/$crn")))
}
