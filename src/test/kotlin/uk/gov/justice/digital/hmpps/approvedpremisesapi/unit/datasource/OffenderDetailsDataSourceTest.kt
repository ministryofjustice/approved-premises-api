package uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.datasource

import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.ApDeliusContextApiClient
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.ClientResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.datasource.OffenderDetailsDataSource
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.CaseAccessFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.CaseSummaryFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.community.OffenderDetailSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.deliuscontext.CaseSummaries
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.deliuscontext.UserAccess
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.deliuscontext.UserOffenderAccess
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.asCaseAccess
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.asCaseSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.asOffenderDetailSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.asUserOffenderAccess
import java.util.stream.Stream

class OffenderDetailsDataSourceTest {
  private val mockApDeliusContextApiClient = mockk<ApDeliusContextApiClient>()

  private val offenderDetailsDataSource = OffenderDetailsDataSource(mockApDeliusContextApiClient)

  @ParameterizedTest
  @MethodSource("cacheableOffenderDetailSummaryClientResults")
  fun `getOffenderDetailSummary returns response from AP Delius Context API call`(
    expectedResult: ClientResult<OffenderDetailSummary>,
  ) {
    every { mockApDeliusContextApiClient.getSummariesForCrns(listOf("SOME-CRN")) } returns
      expectedResult.map { CaseSummaries(listOf(it.asCaseSummary())) }

    val result = offenderDetailsDataSource.getOffenderDetailSummary("SOME-CRN")

    assertThat(result).isEqualTo(expectedResult)
  }

  @Test
  fun `getOffenderDetailSummary returns not found if CRN not returned in AP Delius Context API call`() {
    every {
      mockApDeliusContextApiClient.getSummariesForCrns(listOf("SOME-CRN"))
    } returns ClientResult.Success(HttpStatus.OK, CaseSummaries(cases = emptyList()), true)

    val result = offenderDetailsDataSource.getOffenderDetailSummary("SOME-CRN")

    assertThat(result).isInstanceOf(ClientResult.Failure.StatusCode::class.java)
    assertThat((result as ClientResult.Failure.StatusCode).status).isEqualTo(HttpStatus.NOT_FOUND)
  }

  @Test
  fun `getOffenderDetailSummaries returns transformed response from AP Delius Context API call`() {
    val crns = listOf("CRN-A", "CRN-B", "CRN-C")

    val caseSummaries = listOf(
      CaseSummaryFactory().withCrn("CRN-A").produce(),
      CaseSummaryFactory().withCrn("CRN-B").produce(),
      CaseSummaryFactory().withCrn("CRN-C").produce(),
    )

    val expectedResults = caseSummaries.map { it.crn to ClientResult.Success(HttpStatus.OK, it.asOffenderDetailSummary(), false) }.toMap()

    every { mockApDeliusContextApiClient.getSummariesForCrns(crns) } returns ClientResult.Success(
      HttpStatus.OK,
      CaseSummaries(caseSummaries),
      false,
    )

    val results = offenderDetailsDataSource.getOffenderDetailSummaries(crns)

    assertThat(results).isEqualTo(expectedResults)
  }

  @Test
  fun `getOffenderDetailSummaries returns an entry per CRN for response error`() {
    val crns = listOf("CRN-A", "CRN-B", "CRN-C")

    val cacheTimeoutClientResult = cacheTimeoutClientResult<CaseSummaries>()
    every { mockApDeliusContextApiClient.getSummariesForCrns(crns) } returns cacheTimeoutClientResult

    val results = offenderDetailsDataSource.getOffenderDetailSummaries(crns)
    assertThat(results).hasSize(3)
    assertThat(results["CRN-A"]).isEqualTo(cacheTimeoutClientResult)
    assertThat(results["CRN-B"]).isEqualTo(cacheTimeoutClientResult)
    assertThat(results["CRN-C"]).isEqualTo(cacheTimeoutClientResult)
  }

  @Test
  fun `getOffenderDetailSummaries returns not found if crn not included in results`() {
    val crns = listOf("CRN-A", "CRN-B", "CRN-C")
    val crnACaseSummary = CaseSummaryFactory().withCrn("CRN-A").produce()
    val crnCCaseSummary = CaseSummaryFactory().withCrn("CRN-C").produce()

    val caseSummaries = listOf(crnACaseSummary, crnCCaseSummary)

    every { mockApDeliusContextApiClient.getSummariesForCrns(crns) } returns ClientResult.Success(
      HttpStatus.OK,
      CaseSummaries(caseSummaries),
      false,
    )

    val results = offenderDetailsDataSource.getOffenderDetailSummaries(crns)

    assertThat(results).hasSize(3)

    val crnAResult = results["CRN-A"]
    assertThat(crnAResult).isInstanceOf(ClientResult.Success::class.java)
    assertThat((crnAResult as ClientResult.Success).body).isEqualTo(crnACaseSummary.asOffenderDetailSummary())

    val crnBResult = results["CRN-B"]
    assertThat(crnBResult).isInstanceOf(ClientResult.Failure.StatusCode::class.java)
    assertThat((crnBResult as ClientResult.Failure.StatusCode).status).isEqualTo(HttpStatus.NOT_FOUND)

    val crnCResult = results["CRN-C"]
    assertThat(crnCResult).isInstanceOf(ClientResult.Success::class.java)
    assertThat((crnCResult as ClientResult.Success).body).isEqualTo(crnCCaseSummary.asOffenderDetailSummary())
  }

  @ParameterizedTest
  @MethodSource("userOffenderAccessClientResults")
  fun `getUserAccessForOffenderCrn returns transformed response from AP Delius Context API call`(
    expectedResult: ClientResult<UserOffenderAccess>,
  ) {
    every { mockApDeliusContextApiClient.getUserAccessForCrns("DELIUS-USER", listOf("SOME-CRN")) } returns
      expectedResult.map { UserAccess(listOf(it.asCaseAccess("SOME-CRN"))) }

    val result = offenderDetailsDataSource.getUserAccessForOffenderCrn("DELIUS-USER", "SOME-CRN")

    assertThat(result).isEqualTo(expectedResult)
  }

  @Test
  fun `getUserAccessForOffenderCrn returns not found if CRN not returned in AP Delius Context API call`() {
    every {
      mockApDeliusContextApiClient.getUserAccessForCrns("DELIUS-USER", listOf("SOME-CRN"))
    } returns ClientResult.Success(HttpStatus.OK, UserAccess(access = emptyList()), true)

    val result = offenderDetailsDataSource.getUserAccessForOffenderCrn("DELIUS-USER", "SOME-CRN")

    assertThat(result).isInstanceOf(ClientResult.Failure.StatusCode::class.java)
    assertThat((result as ClientResult.Failure.StatusCode).status).isEqualTo(HttpStatus.NOT_FOUND)
  }

  @Test
  fun `getUserAccessForOffenderCrns returns transformed response from AP Delius Context API`() {
    val crns = listOf("CRN-A", "CRN-B", "CRN-C")
    val caseAccesses = listOf(
      CaseAccessFactory().withCrn("CRN-A").produce(),
      CaseAccessFactory().withCrn("CRN-B").produce(),
      CaseAccessFactory().withCrn("CRN-C").produce(),
    )

    val expectedResults = caseAccesses.map { it.crn to ClientResult.Success(HttpStatus.OK, it.asUserOffenderAccess(), false) }.toMap()

    every { mockApDeliusContextApiClient.getUserAccessForCrns("DELIUS-USER", crns) } returns ClientResult.Success(
      HttpStatus.OK,
      UserAccess(caseAccesses),
      false,
    )

    val results = offenderDetailsDataSource.getUserAccessForOffenderCrns("DELIUS-USER", crns)

    assertThat(results).isEqualTo(expectedResults)
  }

  @Test
  fun `getUserAccessForOffenderCrns returns an entry per CRN for failures`() {
    val crns = listOf("CRN-A", "CRN-B", "CRN-C")

    val cacheTimeoutClientResult = cacheTimeoutClientResult<UserAccess>()
    every { mockApDeliusContextApiClient.getUserAccessForCrns("DELIUS-USER", crns) } returns cacheTimeoutClientResult

    val results = offenderDetailsDataSource.getUserAccessForOffenderCrns("DELIUS-USER", crns)
    assertThat(results).hasSize(3)
    assertThat(results["CRN-A"]).isEqualTo(cacheTimeoutClientResult)
    assertThat(results["CRN-B"]).isEqualTo(cacheTimeoutClientResult)
    assertThat(results["CRN-C"]).isEqualTo(cacheTimeoutClientResult)
  }

  @Test
  fun `getUserAccessForOffenderCrns returns not found if crn not included in results`() {
    val crns = listOf("CRN-A", "CRN-B", "CRN-C")
    val crnBCaseAccess = CaseAccessFactory().withCrn("CRN-B").produce()
    val crnCCaseAccess = CaseAccessFactory().withCrn("CRN-C").produce()

    val caseAccesses = listOf(crnBCaseAccess, crnCCaseAccess)

    every { mockApDeliusContextApiClient.getUserAccessForCrns("DELIUS-USER", crns) } returns ClientResult.Success(
      HttpStatus.OK,
      UserAccess(caseAccesses),
      false,
    )

    val results = offenderDetailsDataSource.getUserAccessForOffenderCrns("DELIUS-USER", crns)

    assertThat(results).hasSize(3)
    val crnAResult = results["CRN-A"]
    assertThat(crnAResult).isInstanceOf(ClientResult.Failure.StatusCode::class.java)
    assertThat((crnAResult as ClientResult.Failure.StatusCode).status).isEqualTo(HttpStatus.NOT_FOUND)

    val crnBResult = results["CRN-B"]
    assertThat(crnBResult).isInstanceOf(ClientResult.Success::class.java)
    assertThat((crnBResult as ClientResult.Success).body).isEqualTo(crnBCaseAccess.asUserOffenderAccess())

    val crnCResult = results["CRN-C"]
    assertThat(crnCResult).isInstanceOf(ClientResult.Success::class.java)
    assertThat((crnCResult as ClientResult.Success).body).isEqualTo(crnCCaseAccess.asUserOffenderAccess())
  }

  private companion object {
    @JvmStatic
    fun cacheableOffenderDetailSummaryClientResults(): Stream<Arguments> {
      val successBody = CaseSummaryFactory()
        .withCrn("SOME-CRN")
        .produce()
        .asOffenderDetailSummary()

      return allClientResults(successBody)
        .filter { it !is ClientResult.Failure.PreemptiveCacheTimeout }
        .intoArgumentStream()
    }

    @JvmStatic
    fun <T> cacheTimeoutClientResult() =
      ClientResult.Failure.PreemptiveCacheTimeout<T>("some-cache", "some-cache-key", 1000)

    @JvmStatic
    fun userOffenderAccessClientResults(): Stream<Arguments> {
      val successBody =
        UserOffenderAccess(
          userRestricted = false,
          userExcluded = false,
          restrictionMessage = null,
        )

      return allClientResults(successBody).intoArgumentStream()
    }

    private fun <T> allClientResults(successBody: T): List<ClientResult<T>> = listOf(
      ClientResult.Failure.CachedValueUnavailable("some-cache-key"),
      ClientResult.Failure.StatusCode(
        HttpMethod.GET,
        "/",
        HttpStatus.NOT_FOUND,
        null,
        false,
      ),
      ClientResult.Failure.Other(
        HttpMethod.POST,
        "/",
        RuntimeException("Some error"),
      ),
      cacheTimeoutClientResult(),
      ClientResult.Success(HttpStatus.OK, successBody, true),
    )

    private fun <T> List<ClientResult<T>>.intoArgumentStream(): Stream<Arguments> = this.stream().map { Arguments.of(it) }
  }
}
