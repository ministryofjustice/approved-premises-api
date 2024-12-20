package uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.service.cas2

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.ApOASysContextApiClient
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.ClientResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.ClientResult.Failure.StatusCode
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.PrisonsApiClient
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.ProbationOffenderSearchApiClient
import uk.gov.justice.digital.hmpps.approvedpremisesapi.datasource.OffenderDetailsDataSource
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.InmateDetailFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.NomisUserEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.OffenderDetailsSummaryFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ProbationOffenderDetailFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.RoshRatingsFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.PersonInfoResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.ProbationOffenderSearchResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.RiskStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.oasyscontext.RiskLevel
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.oasyscontext.RoshRatings
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.prisonsapi.AssignedLivingUnit
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.prisonsapi.InmateDetail
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.prisonsapi.InmateStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.probationoffendersearchapi.IDs
import uk.gov.justice.digital.hmpps.approvedpremisesapi.problem.NotFoundProblem
import uk.gov.justice.digital.hmpps.approvedpremisesapi.results.AuthorisableActionResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas2.OffenderService
import java.time.LocalDate
import java.time.OffsetDateTime

class OffenderServiceTest {
  private val mockPrisonsApiClient = mockk<PrisonsApiClient>()
  private val mockApOASysContextApiClient = mockk<ApOASysContextApiClient>()
  private val mockProbationOffenderSearchClient = mockk<ProbationOffenderSearchApiClient>()
  private val mockOffenderDetailsDataSource = mockk<OffenderDetailsDataSource>()

  private val objectMapper = ObjectMapper().apply {
    registerModule(Jdk8Module())
    registerModule(JavaTimeModule())
    registerKotlinModule()
  }

  private val offenderService = OffenderService(
    mockPrisonsApiClient,
    mockProbationOffenderSearchClient,
    mockApOASysContextApiClient,
    mockOffenderDetailsDataSource,
    2,
  )

  @Nested
  inner class GetRisksByCrn {
    // Note that Tier, Mappa and Flags are all hardcoded to NotFound
    // and these unused 'envelopes' will be removed.

    @Test
    fun `returns NotFound result when Community API Client returns 404`() {
      every { mockOffenderDetailsDataSource.getOffenderDetailSummary("a-crn") } returns StatusCode(HttpMethod.GET, "/secure/offenders/crn/a-crn", HttpStatus.NOT_FOUND, null)

      assertThat(offenderService.getRiskByCrn("a-crn") is AuthorisableActionResult.NotFound).isTrue
    }

    @Test
    fun `throws when Community API Client returns other non-2xx status code`() {
      every { mockOffenderDetailsDataSource.getOffenderDetailSummary("a-crn") } returns StatusCode(HttpMethod.GET, "/secure/offenders/crn/a-crn", HttpStatus.BAD_REQUEST, null)

      val exception = assertThrows<RuntimeException> { offenderService.getRiskByCrn("a-crn") }
      assertThat(exception.message).isEqualTo("Unable to complete GET request to /secure/offenders/crn/a-crn: 400 BAD_REQUEST")
    }

    @Test
    fun `returns NotFound envelope for RoSH when client returns 404`() {
      val crn = "a-crn"

      mockExistingNonLaoOffender()
      mock404RoSH(crn)

      val result = offenderService.getRiskByCrn(crn)
      assertThat(result is AuthorisableActionResult.Success).isTrue
      result as AuthorisableActionResult.Success
      assertThat(result.entity.roshRisks.status).isEqualTo(RiskStatus.NotFound)

      assertThat(result.entity.tier.status).isEqualTo(RiskStatus.NotFound)
      assertThat(result.entity.mappa.status).isEqualTo(RiskStatus.NotFound)
      assertThat(result.entity.flags.status).isEqualTo(RiskStatus.NotFound)
    }

    @Test
    fun `returns Error envelope for RoSH when client returns 500`() {
      val crn = "a-crn"

      mockExistingNonLaoOffender()
      mock500RoSH(crn)

      val result = offenderService.getRiskByCrn(crn)
      assertThat(result is AuthorisableActionResult.Success).isTrue
      result as AuthorisableActionResult.Success
      assertThat(result.entity.roshRisks.status).isEqualTo(RiskStatus.Error)

      assertThat(result.entity.tier.status).isEqualTo(RiskStatus.NotFound)
      assertThat(result.entity.mappa.status).isEqualTo(RiskStatus.NotFound)
      assertThat(result.entity.flags.status).isEqualTo(RiskStatus.NotFound)
    }

    @Test
    fun `returns Retrieved envelopes with expected contents for RoSH when client returns 200`() {
      val crn = "a-crn"

      mockExistingNonLaoOffender()

      mock200RoSH(
        crn,
        RoshRatingsFactory().apply {
          withDateCompleted(OffsetDateTime.parse("2022-09-06T13:45:00Z"))
          withAssessmentId(34853487)
          withRiskChildrenCommunity(RiskLevel.LOW)
          withRiskPublicCommunity(RiskLevel.MEDIUM)
          withRiskKnownAdultCommunity(RiskLevel.HIGH)
          withRiskStaffCommunity(RiskLevel.VERY_HIGH)
        }.produce(),
      )

      val result = offenderService.getRiskByCrn(crn)
      assertThat(result is AuthorisableActionResult.Success).isTrue
      result as AuthorisableActionResult.Success

      assertThat(result.entity.roshRisks.status).isEqualTo(RiskStatus.Retrieved)
      result.entity.roshRisks.value!!.let {
        assertThat(it.lastUpdated).isEqualTo(LocalDate.parse("2022-09-06"))
        assertThat(it.overallRisk).isEqualTo("Very High")
        assertThat(it.riskToChildren).isEqualTo("Low")
        assertThat(it.riskToPublic).isEqualTo("Medium")
        assertThat(it.riskToKnownAdult).isEqualTo("High")
        assertThat(it.riskToStaff).isEqualTo("Very High")
      }

      assertThat(result.entity.tier.status).isEqualTo(RiskStatus.NotFound)
      assertThat(result.entity.tier.value).isNull()

      assertThat(result.entity.mappa.status).isEqualTo(RiskStatus.NotFound)
      assertThat(result.entity.mappa.value).isNull()

      assertThat(result.entity.flags.status).isEqualTo(RiskStatus.NotFound)
      assertThat(result.entity.flags.value).isNull()
    }
  }

  @Nested
  inner class GetOffenderByCrn {
    @Test
    fun `returns NotFound result when Client returns 404`() {
      every { mockOffenderDetailsDataSource.getOffenderDetailSummary("a-crn") } returns StatusCode(HttpMethod.GET, "/secure/offenders/crn/a-crn", HttpStatus.NOT_FOUND, null)

      assertThat(offenderService.getOffenderByCrn("a-crn") is AuthorisableActionResult.NotFound).isTrue
    }

    @Test
    fun `throws when Client returns other non-2xx status code except 403`() {
      every { mockOffenderDetailsDataSource.getOffenderDetailSummary("a-crn") } returns StatusCode(HttpMethod.GET, "/secure/offenders/crn/a-crn", HttpStatus.BAD_REQUEST, null)

      val exception = assertThrows<RuntimeException> { offenderService.getOffenderByCrn("a-crn") }
      assertThat(exception.message).isEqualTo("Unable to complete GET request to /secure/offenders/crn/a-crn: 400 BAD_REQUEST")
    }
  }

  @Nested
  inner class GetPersonByNomsNumber {
    val nomsNumber = "DEF123"
    private val currentUser = NomisUserEntityFactory().withActiveCaseloadId("my-prison").produce()

    @Test
    fun `returns NotFound result when Probation Offender Search returns 404`() {
      every { mockProbationOffenderSearchClient.searchOffenderByNomsNumber(nomsNumber) } returns StatusCode(HttpMethod.POST, "/search", HttpStatus.NOT_FOUND, null)

      assertThat(offenderService.getPersonByNomsNumber(nomsNumber, currentUser) is ProbationOffenderSearchResult.NotFound).isTrue
    }

    @Test
    fun `returns NotFound result when Probation Offender Search does not return a matching offender`() {
      every { mockProbationOffenderSearchClient.searchOffenderByNomsNumber(nomsNumber) } returns ClientResult.Success(HttpStatus.OK, emptyList())

      assertThat(offenderService.getPersonByNomsNumber(nomsNumber, currentUser) is ProbationOffenderSearchResult.NotFound).isTrue
    }

    @Test
    fun `returns Forbidden result when the matching offender has exclusion`() {
      val offenderDetails = ProbationOffenderDetailFactory()
        .withCurrentExclusion(true)
        .withOtherIds(otherIds = IDs(nomsNumber))
        .produce()

      every { mockProbationOffenderSearchClient.searchOffenderByNomsNumber(nomsNumber) } returns ClientResult.Success(HttpStatus.OK, listOf(offenderDetails))

      assertThat(offenderService.getPersonByNomsNumber(nomsNumber, currentUser) is ProbationOffenderSearchResult.Forbidden).isTrue
    }

    @Test
    fun `returns Forbidden result when the matching offender has restriction`() {
      val offenderDetails = ProbationOffenderDetailFactory()
        .withCurrentRestriction(true)
        .withOtherIds(otherIds = IDs(nomsNumber))
        .produce()

      every { mockProbationOffenderSearchClient.searchOffenderByNomsNumber(nomsNumber) } returns ClientResult.Success(HttpStatus.OK, listOf(offenderDetails))

      assertThat(offenderService.getPersonByNomsNumber(nomsNumber, currentUser) is ProbationOffenderSearchResult.Forbidden).isTrue
    }

    @Test
    fun `returns Forbidden result when the matching offender is not in the same prison as the user`() {
      val crn = "ABC123"

      val offenderDetails = ProbationOffenderDetailFactory()
        .withOtherIds(otherIds = IDs(crn = crn, nomsNumber = nomsNumber))
        .produce()

      every { mockProbationOffenderSearchClient.searchOffenderByNomsNumber(nomsNumber) } returns ClientResult.Success(
        status = HttpStatus.OK,
        body = listOf(offenderDetails),
      )

      val inmateDetail = InmateDetailFactory()
        .withOffenderNo(nomsNumber)
        .withAssignedLivingUnit(
          AssignedLivingUnit(
            agencyId = "not-my-prison",
            agencyName = "Not My Prison",
            locationId = 5,
            description = "",
          ),
        )
        .produce()

      every { mockPrisonsApiClient.getInmateDetailsWithWait(nomsNumber) } returns ClientResult.Success(
        status = HttpStatus.OK,
        body = inmateDetail,
      )

      assertThat(offenderService.getPersonByNomsNumber(nomsNumber, currentUser) is ProbationOffenderSearchResult.Forbidden).isTrue
    }

    @Test
    fun `returns Unknown if Probation Offender Search responds with a 500`() {
      every { mockProbationOffenderSearchClient.searchOffenderByNomsNumber(nomsNumber) } returns StatusCode(HttpMethod.GET, "/search", HttpStatus.INTERNAL_SERVER_ERROR, null, true)

      val result = offenderService.getPersonByNomsNumber(nomsNumber, currentUser)

      assertThat(result is ProbationOffenderSearchResult.Unknown).isTrue
      result as ProbationOffenderSearchResult.Unknown
      assertThat(result.throwable).isNotNull()
    }

    @Test
    fun `returns not found if Prison API does not find a matching offender`() {
      val crn = "ABC123"

      val offenderDetails = ProbationOffenderDetailFactory()
        .withOtherIds(otherIds = IDs(crn = crn, nomsNumber = nomsNumber))
        .produce()

      every { mockProbationOffenderSearchClient.searchOffenderByNomsNumber(nomsNumber) } returns ClientResult.Success(
        status = HttpStatus.OK,
        body = listOf(offenderDetails),
      )

      every { mockPrisonsApiClient.getInmateDetailsWithWait(nomsNumber) } returns StatusCode(HttpMethod.GET, "/api/offenders/$nomsNumber", HttpStatus.BAD_GATEWAY, null)

      val result = offenderService.getPersonByNomsNumber(nomsNumber, currentUser)

      assertThat(result is ProbationOffenderSearchResult.NotFound).isTrue
    }

    @Test
    fun `returns Full Person when Probation Offender Search and Prison API returns a matching offender`() {
      val crn = "ABC123"

      val offenderDetails = ProbationOffenderDetailFactory()
        .withOtherIds(otherIds = IDs(crn = crn, nomsNumber = nomsNumber))
        .produce()

      every { mockProbationOffenderSearchClient.searchOffenderByNomsNumber(nomsNumber) } returns ClientResult.Success(
        status = HttpStatus.OK,
        body = listOf(offenderDetails),
      )

      val inmateDetail = InmateDetailFactory()
        .withOffenderNo(nomsNumber)
        .withAssignedLivingUnit(
          AssignedLivingUnit(
            agencyId = "my-prison",
            agencyName = "My Prison",
            locationId = 6,
            description = "",
          ),
        )
        .produce()

      every { mockPrisonsApiClient.getInmateDetailsWithWait(nomsNumber) } returns ClientResult.Success(
        status = HttpStatus.OK,
        body = inmateDetail,
      )

      val result = offenderService.getPersonByNomsNumber(nomsNumber, currentUser)

      assertThat(result is ProbationOffenderSearchResult.Success.Full).isTrue
      result as ProbationOffenderSearchResult.Success.Full
      assertThat(result.probationOffenderDetail.otherIds.crn).isEqualTo(crn)
      assertThat(result.probationOffenderDetail.otherIds.nomsNumber).isEqualTo(offenderDetails.otherIds.nomsNumber)
      assertThat(result.inmateDetail).isEqualTo(inmateDetail)
      assertThat(result.inmateDetail?.offenderNo).isEqualTo(nomsNumber)
    }
  }

  @Nested
  inner class GetInmateDetailByNomsNumber {
    @Test
    fun `returns not found result when for Offender without Application or Booking and Client responds with 404`() {
      val crn = "CRN123"
      val nomsNumber = "NOMS321"

      every { mockPrisonsApiClient.getInmateDetailsWithWait(nomsNumber) } returns StatusCode(HttpMethod.GET, "/api/offenders/$nomsNumber", HttpStatus.NOT_FOUND, null)

      val result = offenderService.getInmateDetailByNomsNumber(crn, nomsNumber)

      assertThat(result is AuthorisableActionResult.NotFound).isTrue
    }

    @Test
    fun `returns not found result when for Offender with Application or Booking and Client responds with 404`() {
      val crn = "CRN123"
      val nomsNumber = "NOMS321"

      every { mockPrisonsApiClient.getInmateDetailsWithWait(nomsNumber) } returns StatusCode(HttpMethod.GET, "/api/offenders/$nomsNumber", HttpStatus.NOT_FOUND, null)

      val result = offenderService.getInmateDetailByNomsNumber(crn, nomsNumber)

      assertThat(result is AuthorisableActionResult.NotFound).isTrue
    }

    @Test
    fun `returns unauthorised result when Client responds with 403`() {
      val crn = "CRN123"
      val nomsNumber = "NOMS321"

      every { mockPrisonsApiClient.getInmateDetailsWithWait(nomsNumber) } returns StatusCode(HttpMethod.GET, "/api/offenders/$nomsNumber", HttpStatus.FORBIDDEN, null)

      val result = offenderService.getInmateDetailByNomsNumber(crn, nomsNumber)

      assertThat(result is AuthorisableActionResult.Unauthorised).isTrue
    }

    @Test
    fun `returns successfully when Client responds with 200`() {
      val crn = "CRN123"
      val nomsNumber = "NOMS321"

      every { mockPrisonsApiClient.getInmateDetailsWithWait(nomsNumber) } returns ClientResult.Success(
        HttpStatus.OK,
        InmateDetail(
          offenderNo = nomsNumber,
          custodyStatus = InmateStatus.IN,
          assignedLivingUnit = AssignedLivingUnit(
            agencyId = "AGY",
            locationId = 89,
            description = "AGENCY DESCRIPTION",
            agencyName = "AGENCY NAME",
          ),
        ),
      )

      val result = offenderService.getInmateDetailByNomsNumber(crn, nomsNumber)

      assertThat(result is AuthorisableActionResult.Success)
      result as AuthorisableActionResult.Success
      assertThat(result.entity).isNotNull
      assertThat(result.entity!!.offenderNo).isEqualTo(nomsNumber)
      assertThat(result.entity!!.custodyStatus).isEqualTo(InmateStatus.IN)
      assertThat(result.entity!!.assignedLivingUnit).isEqualTo(
        AssignedLivingUnit(
          agencyId = "AGY",
          locationId = 89,
          description = "AGENCY DESCRIPTION",
          agencyName = "AGENCY NAME",
        ),
      )
    }
  }

  private fun mockExistingNonLaoOffender() {
    val resultBody = OffenderDetailsSummaryFactory()
      .withCrn("a-crn")
      .withFirstName("Bob")
      .withLastName("Doe")
      .withCurrentRestriction(false)
      .withCurrentExclusion(false)
      .produce()

    every {
      mockOffenderDetailsDataSource.getOffenderDetailSummary("a-crn")
    } returns ClientResult.Success(HttpStatus.OK, resultBody)
  }

  @Nested
  inner class GetFullInfoForPersonOrThrow {

    @Test
    fun `throws NotFoundProblem when offender is PersonInfoResult-NotFound, status 404`() {
      val crn = "ABC123"
      every { mockOffenderDetailsDataSource.getOffenderDetailSummary(crn) } returns StatusCode(
        HttpMethod.GET,
        "/secure/offenders/crn/ABC123",
        HttpStatus.NOT_FOUND,
        null,
        true,
      )
      assertThrows<NotFoundProblem> { offenderService.getFullInfoForPersonOrThrow(crn) }
    }

    @Test
    fun `throws NotFoundProblem exception when offender is PersonInfoResult-Unknown, status 500`() {
      val crn = "ABC123"
      every { mockOffenderDetailsDataSource.getOffenderDetailSummary(crn) } returns StatusCode(
        HttpMethod.GET,
        "/secure/offenders/crn/ABC123",
        HttpStatus.INTERNAL_SERVER_ERROR,
        null,
        true,
      )
      assertThrows<NotFoundProblem> { offenderService.getFullInfoForPersonOrThrow(crn) }
    }

    @Test
    fun `returns PersonInfoResult-Success-Full when offender and inmate details are found, status 200`() {
      val crn = "ABC123"
      val nomsNumber = "NOMSABC"

      val offenderDetails = OffenderDetailsSummaryFactory()
        .withCrn(crn)
        .withNomsNumber(nomsNumber)
        .produce()

      every { mockOffenderDetailsDataSource.getOffenderDetailSummary(crn) } returns ClientResult.Success(
        status = HttpStatus.OK,
        body = offenderDetails,
      )

      val inmateDetail = InmateDetailFactory()
        .withOffenderNo(nomsNumber)
        .produce()

      every { mockPrisonsApiClient.getInmateDetailsWithWait(nomsNumber) } returns ClientResult.Success(
        status = HttpStatus.OK,
        body = inmateDetail,
      )

      val result = offenderService.getFullInfoForPersonOrThrow(crn)

      assertThat(result is PersonInfoResult.Success.Full).isTrue
      assertThat(result.crn).isEqualTo(crn)
      assertThat(result.offenderDetailSummary).isEqualTo(offenderDetails)
      assertThat(result.inmateDetail).isEqualTo(inmateDetail)
    }
  }

  @Nested
  inner class GetOffenderNameOrPlaceholder {
    @Test
    fun `returns Not Found when offender is PersonInfoResult-NotFound, status 404`() {
      every { mockOffenderDetailsDataSource.getOffenderDetailSummary("NOTFOUND") } returns StatusCode(
        HttpMethod.GET,
        "/secure/offenders/crn/ABC123",
        HttpStatus.NOT_FOUND,
        null,
        true,
      )

      val result = offenderService.getOffenderNameOrPlaceholder("NOTFOUND")
      assertThat(result).isEqualTo("Person Not Found")
    }

    @Test
    fun `returns Unknown when offender returns any other code except 404`() {
      every { mockOffenderDetailsDataSource.getOffenderDetailSummary("UNKNOWN") } returns StatusCode(
        HttpMethod.GET,
        "/secure/offenders/crn/ABC123",
        HttpStatus.FORBIDDEN,
        null,
        true,
      )

      val result = offenderService.getOffenderNameOrPlaceholder("UNKNOWN")
      assertThat(result).isEqualTo("Unknown")
    }

    @Test
    fun `returns the person's full name when offender is PersonInfoResult-Full`() {
      val offenderDetailSummary = OffenderDetailsSummaryFactory()
        .withFirstName("ExampleFirst")
        .withLastName("ExampleLast")
        .produce()
      every { mockOffenderDetailsDataSource.getOffenderDetailSummary("FULL") } returns ClientResult.Success(
        status = HttpStatus.OK,
        body = offenderDetailSummary,
      )

      val result = offenderService.getOffenderNameOrPlaceholder("FULL")
      assertThat(result).isEqualTo("ExampleFirst ExampleLast")
    }
  }

  @Nested
  inner class GetMapOfPersonsNamesAndCrns {
    val offenderDetailSummary = OffenderDetailsSummaryFactory()
      .withFirstName("ExampleFirst")
      .withLastName("ExampleLast")
      .produce()

    @Test
    fun `returns Not Found when offender is PersonInfoResult-NotFound, status 404`() {
      every { mockOffenderDetailsDataSource.getOffenderDetailSummaries(listOf("NOTFOUND")) } returns mapOf(
        "NOTFOUND" to
          StatusCode(
            HttpMethod.GET,
            "/secure/offenders/crn/ABC123",
            HttpStatus.NOT_FOUND,
            null,
            true,
          ),
      )

      val result = offenderService.getMapOfPersonNamesAndCrns(listOf("NOTFOUND"))
      assertThat(result).isEqualTo(mapOf("NOTFOUND" to "Person Not Found"))
    }

    @Test
    fun `returns Unknown when offender returns any other code except 404`() {
      every { mockOffenderDetailsDataSource.getOffenderDetailSummaries(listOf("UNKNOWN")) } returns mapOf(
        "UNKNOWN" to
          StatusCode(
            HttpMethod.GET,
            "/secure/offenders/crn/ABC123",
            HttpStatus.FORBIDDEN,
            null,
            true,
          ),
      )

      val result = offenderService.getMapOfPersonNamesAndCrns(listOf("UNKNOWN"))
      assertThat(result).isEqualTo(mapOf("UNKNOWN" to "Unknown"))
    }

    @Test
    fun `returns the person's full name when offender is PersonInfoResult-Full`() {
      every { mockOffenderDetailsDataSource.getOffenderDetailSummaries(listOf("FULL")) } returns mapOf(
        "FULL" to ClientResult.Success(
          status = HttpStatus.OK,
          body = offenderDetailSummary,
        ),
      )
      val result = offenderService.getMapOfPersonNamesAndCrns(listOf("FULL"))
      assertThat(result).isEqualTo(mapOf("FULL" to "ExampleFirst ExampleLast"))
    }

    @Test
    fun `calls offender API 2 times when crn search limit exceed`() {
      every { mockOffenderDetailsDataSource.getOffenderDetailSummaries(any<List<String>>()) } returns mapOf(
        "CRN" to ClientResult.Success(
          status = HttpStatus.OK,
          body = offenderDetailSummary,
        ),
      )
      val crns = listOf("CRN1", "CRN2", "CRN3")
      offenderService.getMapOfPersonNamesAndCrns(crns)
      verify(exactly = 1) { mockOffenderDetailsDataSource.getOffenderDetailSummaries(listOf("CRN1", "CRN2")) }
      verify(exactly = 1) { mockOffenderDetailsDataSource.getOffenderDetailSummaries(listOf("CRN3")) }
    }
  }

  private fun mock404RoSH(crn: String) = every { mockApOASysContextApiClient.getRoshRatings(crn) } returns StatusCode(HttpMethod.GET, "/rosh/a-crn", HttpStatus.NOT_FOUND, body = null)

  private fun mock500RoSH(crn: String) = every { mockApOASysContextApiClient.getRoshRatings(crn) } returns StatusCode(HttpMethod.GET, "/rosh/a-crn", HttpStatus.INTERNAL_SERVER_ERROR, body = null)

  private fun mock200RoSH(crn: String, body: RoshRatings) = every { mockApOASysContextApiClient.getRoshRatings(crn) } returns ClientResult.Success(HttpStatus.OK, body = body)
}
