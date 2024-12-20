package uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.service

import io.mockk.Called
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ArgumentsSource
import org.junit.jupiter.params.provider.CsvSource
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.ApDeliusContextApiClient
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.ApOASysContextApiClient
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.CaseNotesClient
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.ClientResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.ClientResult.Failure.StatusCode
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.PrisonsApiClient
import uk.gov.justice.digital.hmpps.approvedpremisesapi.config.ExcludedCategoryBindingModel
import uk.gov.justice.digital.hmpps.approvedpremisesapi.config.PrisonAdjudicationsConfigBindingModel
import uk.gov.justice.digital.hmpps.approvedpremisesapi.config.PrisonCaseNotesConfigBindingModel
import uk.gov.justice.digital.hmpps.approvedpremisesapi.datasource.OffenderDetailsDataSource
import uk.gov.justice.digital.hmpps.approvedpremisesapi.datasource.OffenderRisksDataSource
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.AdjudicationFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.AdjudicationsPageFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.AgencyFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.CaseAccessFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.CaseNoteFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.CaseSummaryFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.InmateDetailFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.OffenderDetailsSummaryFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.PersonRisksFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.UserEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.PersonInfoResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.PersonSummaryInfoResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.deliuscontext.CaseSummaries
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.deliuscontext.UserAccess
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.deliuscontext.UserOffenderAccess
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.prisonsapi.AssignedLivingUnit
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.prisonsapi.CaseNotesPage
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.prisonsapi.InmateDetail
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.prisonsapi.InmateStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.problem.InternalServerErrorProblem
import uk.gov.justice.digital.hmpps.approvedpremisesapi.results.AuthorisableActionResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.results.CasResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.OffenderService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.OffenderService.LimitedAccessStrategy
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.OffenderService.LimitedAccessStrategy.ReturnRestrictedIfLimitedAccess
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.PersonTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.util.ObjectMapperFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.ClientResultFailureArgumentsProvider
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.asCaseSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.randomNumberChars
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.randomStringMultiCaseWithNumbers
import java.time.LocalDate

class OffenderServiceTest {
  private val mockPrisonsApiClient = mockk<PrisonsApiClient>()
  private val mockCaseNotesClient = mockk<CaseNotesClient>()
  private val mockApOASysContextApiClient = mockk<ApOASysContextApiClient>()
  private val mockApDeliusContextApiClient = mockk<ApDeliusContextApiClient>()
  private val mockOffenderDetailsDataSource = mockk<OffenderDetailsDataSource>()
  private val mockOffenderRisksDataSource = mockk<OffenderRisksDataSource>()
  private val mockPersonTransformer = mockk<PersonTransformer>()

  private val prisonCaseNotesConfigBindingModel = PrisonCaseNotesConfigBindingModel().apply {
    lookbackDays = 30
    prisonApiPageSize = 2
    excludedCategories = listOf(
      ExcludedCategoryBindingModel().apply {
        this.category = "CATEGORY"
        this.subcategory = "EXCLUDED_SUBTYPE"
      },
      ExcludedCategoryBindingModel().apply {
        this.category = "EXCLUDED_CATEGORY"
        this.subcategory = null
      },
    )
  }
  private val adjudicationsConfigBindingModel = PrisonAdjudicationsConfigBindingModel().apply {
    prisonApiPageSize = 2
  }

  private val objectMapper = ObjectMapperFactory.createRuntimeLikeObjectMapper()

  private val offenderService = OffenderService(
    mockPrisonsApiClient,
    mockCaseNotesClient,
    mockApOASysContextApiClient,
    mockApDeliusContextApiClient,
    mockOffenderDetailsDataSource,
    mockOffenderRisksDataSource,
    mockPersonTransformer,
    prisonCaseNotesConfigBindingModel,
    adjudicationsConfigBindingModel,
  )

  @Test
  fun `getOffenderByCrn returns NotFound result when Client returns 404`() {
    every { mockOffenderDetailsDataSource.getOffenderDetailSummary("a-crn") } returns StatusCode(HttpMethod.GET, "/secure/offenders/crn/a-crn", HttpStatus.NOT_FOUND, null)

    assertThat(offenderService.getOffenderByCrn("a-crn", "distinguished.name") is AuthorisableActionResult.NotFound).isTrue
  }

  @Test
  fun `getOffenderByCrn throws when Client returns other non-2xx status code except 403`() {
    every { mockOffenderDetailsDataSource.getOffenderDetailSummary("a-crn") } returns StatusCode(HttpMethod.GET, "/secure/offenders/crn/a-crn", HttpStatus.BAD_REQUEST, null)

    val exception = assertThrows<RuntimeException> { offenderService.getOffenderByCrn("a-crn", "distinguished.name") }
    assertThat(exception.message).isEqualTo("Unable to complete GET request to /secure/offenders/crn/a-crn: 400 BAD_REQUEST")
  }

  @Test
  fun `getOffenderByCrn returns OffenderDetails without further checks when Offender has no LAO constraints`() {
    val resultBody = OffenderDetailsSummaryFactory()
      .withCrn("a-crn")
      .withFirstName("Bob")
      .withLastName("Doe")
      .produce()

    every { mockOffenderDetailsDataSource.getOffenderDetailSummary("a-crn") } returns ClientResult.Success(HttpStatus.OK, resultBody)

    val result = offenderService.getOffenderByCrn("a-crn", "distinguished.name")

    assertThat(result is AuthorisableActionResult.Success).isTrue
    result as AuthorisableActionResult.Success
    assertThat(result.entity.otherIds.crn).isEqualTo("a-crn")
    assertThat(result.entity.firstName).isEqualTo("Bob")
    assertThat(result.entity.surname).isEqualTo("Doe")
  }

  @Test
  fun `getOffenderByCrn does not enforce LAO when ignoreLaoRestrictions is enabled (because user has LAO qualification)`() {
    val resultBody = OffenderDetailsSummaryFactory()
      .withCrn("a-crn")
      .withFirstName("Bob")
      .withLastName("Doe")
      .withCurrentExclusion(true)
      .produce()

    val accessBody = UserOffenderAccess(userRestricted = false, userExcluded = true, restrictionMessage = null)

    every { mockOffenderDetailsDataSource.getOffenderDetailSummary("a-crn") } returns ClientResult.Success(HttpStatus.OK, resultBody)
    every { mockOffenderDetailsDataSource.getUserAccessForOffenderCrn("distinguished.name", "a-crn") } returns ClientResult.Success(HttpStatus.OK, accessBody)

    assertThat(offenderService.getOffenderByCrn("a-crn", "distinguished.name", ignoreLaoRestrictions = true) is AuthorisableActionResult.Success).isTrue
  }

  @Test
  fun `getOffenderByCrn returns Unauthorised result when distinguished name is excluded from viewing`() {
    val resultBody = OffenderDetailsSummaryFactory()
      .withCrn("a-crn")
      .withFirstName("Bob")
      .withLastName("Doe")
      .withCurrentExclusion(true)
      .produce()

    val accessBody = UserOffenderAccess(userRestricted = false, userExcluded = true, restrictionMessage = null)

    every { mockOffenderDetailsDataSource.getOffenderDetailSummary("a-crn") } returns ClientResult.Success(HttpStatus.OK, resultBody)
    every { mockOffenderDetailsDataSource.getUserAccessForOffenderCrn("distinguished.name", "a-crn") } returns ClientResult.Success(HttpStatus.OK, accessBody)

    assertThat(offenderService.getOffenderByCrn("a-crn", "distinguished.name") is AuthorisableActionResult.Unauthorised).isTrue
  }

  @Test
  fun `getOffenderByCrn returns Unauthorised result when Client returns 403 with valid user access response body`() {
    val resultBody = OffenderDetailsSummaryFactory()
      .withCrn("a-crn")
      .withFirstName("Bob")
      .withLastName("Doe")
      .withCurrentExclusion(true)
      .produce()

    val accessBody = UserOffenderAccess(userRestricted = false, userExcluded = true, restrictionMessage = null)

    every { mockOffenderDetailsDataSource.getOffenderDetailSummary("a-crn") } returns ClientResult.Success(HttpStatus.OK, resultBody)
    every { mockOffenderDetailsDataSource.getUserAccessForOffenderCrn("distinguished.name", "a-crn") } returns StatusCode(
      HttpMethod.GET,
      "/secure/crn/a-crn/user/distinguished.name/userAccess",
      HttpStatus.FORBIDDEN,
      ObjectMapperFactory.createRuntimeLikeObjectMapper().writeValueAsString(accessBody),
    )

    assertThat(offenderService.getOffenderByCrn("a-crn", "distinguished.name") is AuthorisableActionResult.Unauthorised).isTrue
  }

  @Test
  fun `getOffenderByCrn throws when Client returns 403 without valid user access response body (problem with our service to service JWT)`() {
    val resultBody = OffenderDetailsSummaryFactory()
      .withCrn("a-crn")
      .withFirstName("Bob")
      .withLastName("Doe")
      .withCurrentExclusion(true)
      .produce()

    every { mockOffenderDetailsDataSource.getOffenderDetailSummary("a-crn") } returns ClientResult.Success(HttpStatus.OK, resultBody)
    every { mockOffenderDetailsDataSource.getUserAccessForOffenderCrn("distinguished.name", "a-crn") } returns StatusCode(
      HttpMethod.GET,
      "/secure/crn/a-crn/user/distinguished.name/userAccess",
      HttpStatus.FORBIDDEN,
      null,
    )

    val exception = assertThrows<RuntimeException> { offenderService.getOffenderByCrn("a-crn", "distinguished.name") }
    assertThat(exception.message).isEqualTo("Unable to complete GET request to /secure/crn/a-crn/user/distinguished.name/userAccess: 403 FORBIDDEN")
  }

  @Test
  fun `getOffenderByCrn returns Unauthorised result when distinguished name is not explicitly allowed to view`() {
    val resultBody = OffenderDetailsSummaryFactory()
      .withCrn("a-crn")
      .withFirstName("Bob")
      .withLastName("Doe")
      .withCurrentRestriction(true)
      .produce()

    val accessBody = UserOffenderAccess(userRestricted = true, userExcluded = false, restrictionMessage = null)

    every { mockOffenderDetailsDataSource.getOffenderDetailSummary("a-crn") } returns ClientResult.Success(HttpStatus.OK, resultBody)
    every { mockOffenderDetailsDataSource.getUserAccessForOffenderCrn("distinguished.name", "a-crn") } returns ClientResult.Success(HttpStatus.OK, accessBody)

    assertThat(offenderService.getOffenderByCrn("a-crn", "distinguished.name") is AuthorisableActionResult.Unauthorised).isTrue
  }

  @Test
  fun `getOffenderByCrn returns OffenderDetails when LAO restrictions are passed`() {
    val resultBody = OffenderDetailsSummaryFactory()
      .withCrn("a-crn")
      .withFirstName("Bob")
      .withLastName("Doe")
      .withCurrentRestriction(true)
      .produce()

    val accessBody = UserOffenderAccess(userRestricted = false, userExcluded = false, restrictionMessage = null)

    every { mockOffenderDetailsDataSource.getOffenderDetailSummary("a-crn") } returns ClientResult.Success(HttpStatus.OK, resultBody)
    every { mockOffenderDetailsDataSource.getUserAccessForOffenderCrn("distinguished.name", "a-crn") } returns ClientResult.Success(HttpStatus.OK, accessBody)

    every { mockOffenderDetailsDataSource.getOffenderDetailSummary("a-crn") } returns ClientResult.Success(HttpStatus.OK, resultBody)

    val result = offenderService.getOffenderByCrn("a-crn", "distinguished.name")

    assertThat(result is AuthorisableActionResult.Success).isTrue
    result as AuthorisableActionResult.Success
    assertThat(result.entity.otherIds.crn).isEqualTo("a-crn")
    assertThat(result.entity.firstName).isEqualTo("Bob")
    assertThat(result.entity.surname).isEqualTo("Doe")
  }

  @Test
  fun `getRisksByCrn returns NotFound result when Community API Client returns 404`() {
    every { mockOffenderDetailsDataSource.getOffenderDetailSummary("a-crn") } returns StatusCode(HttpMethod.GET, "/secure/offenders/crn/a-crn", HttpStatus.NOT_FOUND, null)

    assertThat(offenderService.getRiskByCrn("a-crn", "distinguished.name") is AuthorisableActionResult.NotFound).isTrue
  }

  @Test
  fun `getRisksByCrn throws when Community API Client returns other non-2xx status code`() {
    every { mockOffenderDetailsDataSource.getOffenderDetailSummary("a-crn") } returns StatusCode(HttpMethod.GET, "/secure/offenders/crn/a-crn", HttpStatus.BAD_REQUEST, null)

    val exception = assertThrows<RuntimeException> { offenderService.getRiskByCrn("a-crn", "distinguished.name") }
    assertThat(exception.message).isEqualTo("Unable to complete GET request to /secure/offenders/crn/a-crn: 400 BAD_REQUEST")
  }

  @Test
  fun `getRisksByCrn returns Unauthorised result when distinguished name is excluded from viewing`() {
    val resultBody = OffenderDetailsSummaryFactory()
      .withCrn("a-crn")
      .withFirstName("Bob")
      .withLastName("Doe")
      .withCurrentExclusion(true)
      .produce()

    val accessBody = UserOffenderAccess(userRestricted = false, userExcluded = true, restrictionMessage = null)

    every { mockOffenderDetailsDataSource.getOffenderDetailSummary("a-crn") } returns ClientResult.Success(HttpStatus.OK, resultBody)
    every { mockOffenderDetailsDataSource.getUserAccessForOffenderCrn("distinguished.name", "a-crn") } returns ClientResult.Success(HttpStatus.OK, accessBody)

    assertThat(offenderService.getRiskByCrn("a-crn", "distinguished.name") is AuthorisableActionResult.Unauthorised).isTrue
  }

  @Test
  fun `getRisksByCrn returns Success result with information from offender risks data source`() {
    val crn = "a-crn"
    val deliusUsername = "SOME-USER"

    val expectedRisks = PersonRisksFactory().produce()
    every { mockOffenderDetailsDataSource.getOffenderDetailSummary(crn) } returns
      ClientResult.Success(
        HttpStatus.OK,
        OffenderDetailsSummaryFactory().produce(),
      )

    every { mockOffenderDetailsDataSource.getUserAccessForOffenderCrn(crn, deliusUsername) } returns
      ClientResult.Success(
        HttpStatus.OK,
        UserOffenderAccess(userRestricted = false, userExcluded = false, restrictionMessage = null),
      )

    every { mockOffenderRisksDataSource.getPersonRisks(crn) } returns expectedRisks

    val result = offenderService.getRiskByCrn(crn, deliusUsername)
    assertThat(result is AuthorisableActionResult.Success).isTrue
    result as AuthorisableActionResult.Success
    assertThat(result.entity).isEqualTo(expectedRisks)
  }

  @Nested
  inner class CanAccessOffender {

    @Test
    fun `return true and dont make any external calls if IgnoreLimitedAccess strategy`() {
      val crn = randomNumberChars(8)

      val result = offenderService.canAccessOffender(crn, LimitedAccessStrategy.IgnoreLimitedAccess)
      assertThat(result).isTrue()

      verify { mockApDeliusContextApiClient wasNot Called }
    }

    @Test
    fun `return false when crn is user excluded from viewing, ReturnRestrictedIfLimitedAccess strategy`() {
      val crn = randomNumberChars(8)
      val caseAccess = CaseAccessFactory()
        .withCrn(crn)
        .withUserExcluded(true)
        .produce()

      every {
        mockApDeliusContextApiClient.getUserAccessForCrns(
          "distinguished.name",
          listOf(crn),
        )
      } returns ClientResult.Success(HttpStatus.OK, UserAccess(listOf(caseAccess)))

      val result = offenderService.canAccessOffender(crn, ReturnRestrictedIfLimitedAccess("distinguished.name"))
      assertThat(result).isFalse()
    }

    @Test
    fun `return false when crn is user restricted from viewing, ReturnRestrictedIfLimitedAccess strategy`() {
      val crn = randomNumberChars(8)
      val caseAccess = CaseAccessFactory()
        .withCrn(crn)
        .withUserRestricted(true)
        .produce()

      every {
        mockApDeliusContextApiClient.getUserAccessForCrns(
          "distinguished.name",
          listOf(crn),
        )
      } returns ClientResult.Success(HttpStatus.OK, UserAccess(listOf(caseAccess)))

      val result = offenderService.canAccessOffender(crn, ReturnRestrictedIfLimitedAccess("distinguished.name"))
      assertThat(result).isFalse()
    }

    @Test
    fun `return false when no access result returned for crn, ReturnRestrictedIfLimitedAccess strategy`() {
      val crn = randomNumberChars(8)

      every {
        mockApDeliusContextApiClient.getUserAccessForCrns(
          "distinguished.name",
          listOf(crn),
        )
      } returns ClientResult.Success(HttpStatus.OK, UserAccess(emptyList()))

      val result = offenderService.canAccessOffender(crn, ReturnRestrictedIfLimitedAccess("distinguished.name"))
      assertThat(result).isFalse()
    }

    @Test
    fun `return true when crn is not user restricted or excluded from viewing, ReturnRestrictedIfLimitedAccess strategy`() {
      val crn = randomNumberChars(8)
      val caseAccess = CaseAccessFactory()
        .withCrn(crn)
        .withUserRestricted(false)
        .withUserExcluded(false)
        .produce()

      every {
        mockApDeliusContextApiClient.getUserAccessForCrns(
          "distinguished.name",
          listOf(crn),
        )
      } returns ClientResult.Success(HttpStatus.OK, UserAccess(listOf(caseAccess)))

      val result = offenderService.canAccessOffender(crn, ReturnRestrictedIfLimitedAccess("distinguished.name"))
      assertThat(result).isTrue()
    }

    @ParameterizedTest
    @ArgumentsSource(ClientResultFailureArgumentsProvider::class)
    fun `throws exception when getUserAccessForCrns returns client result failure`(response: ClientResult.Failure<UserAccess>) {
      val crn = randomNumberChars(8)

      every { mockApDeliusContextApiClient.getUserAccessForCrns("distinguished.name", listOf(crn)) } returns response

      assertThrows<Throwable> { offenderService.canAccessOffender(crn, ReturnRestrictedIfLimitedAccess("distinguished.name")) }
    }
  }

  @Nested
  inner class CanAccessOffenders {

    @Test
    fun `return empty result when crns list is empty`() {
      val result = offenderService.canAccessOffenders("distinguished.name", emptyList())
      assertThat(result.isEmpty()).isTrue()
    }

    @Test
    fun `return error if greater than 500 crns are requested`() {
      assertThatThrownBy {
        offenderService.canAccessOffenders("distinguished.name", (0..500).map { "$it" })
      }.isInstanceOf(InternalServerErrorProblem::class.java)
        .hasMessage("Internal Server Error: Cannot bulk request access details for more than 500 CRNs. 501 have been provided.")
    }

    @Test
    fun `return false when crn is user excluded from viewing`() {
      val crn = randomNumberChars(8)
      val caseAccess = CaseAccessFactory()
        .withCrn(crn)
        .withUserExcluded(true)
        .produce()

      every {
        mockApDeliusContextApiClient.getUserAccessForCrns(
          "distinguished.name",
          listOf(crn),
        )
      } returns ClientResult.Success(HttpStatus.OK, UserAccess(listOf(caseAccess)))

      val result = offenderService.canAccessOffenders("distinguished.name", listOf(crn))
      assertThat(result[crn]).isFalse()
    }

    @Test
    fun `return false when crn is user restricted from viewing`() {
      val crn = randomNumberChars(8)
      val caseAccess = CaseAccessFactory()
        .withCrn(crn)
        .withUserRestricted(true)
        .produce()

      every {
        mockApDeliusContextApiClient.getUserAccessForCrns(
          "distinguished.name",
          listOf(crn),
        )
      } returns ClientResult.Success(HttpStatus.OK, UserAccess(listOf(caseAccess)))

      val result = offenderService.canAccessOffenders("distinguished.name", listOf(crn))
      assertThat(result[crn]).isFalse()
    }

    @Test
    fun `return false when no access result returned for crn`() {
      val crn = randomNumberChars(8)

      every {
        mockApDeliusContextApiClient.getUserAccessForCrns(
          "distinguished.name",
          listOf(crn),
        )
      } returns ClientResult.Success(HttpStatus.OK, UserAccess(emptyList()))

      val result = offenderService.canAccessOffenders("distinguished.name", listOf(crn))
      assertThat(result[crn]).isFalse()
    }

    @Test
    fun `return true when crn is not user restricted or excluded from viewing`() {
      val crn = randomNumberChars(8)
      val caseAccess = CaseAccessFactory()
        .withCrn(crn)
        .withUserRestricted(false)
        .withUserExcluded(false)
        .produce()

      every {
        mockApDeliusContextApiClient.getUserAccessForCrns(
          "distinguished.name",
          listOf(crn),
        )
      } returns ClientResult.Success(HttpStatus.OK, UserAccess(listOf(caseAccess)))

      val result = offenderService.canAccessOffenders("distinguished.name", listOf(crn))
      assertThat(result[crn]).isTrue()
    }

    @Test
    fun `return expected results when multiple crns`() {
      val crns = mutableListOf<String>()
      repeat(5) { crns += randomStringMultiCaseWithNumbers(8) }

      val caseAccess1 = CaseAccessFactory()
        .withCrn(crns[0])
        .withUserRestricted(false)
        .withUserExcluded(false)
        .produce()

      val caseAccess2 = CaseAccessFactory()
        .withCrn(crns[1])
        .withUserRestricted(false)
        .withUserExcluded(true)
        .produce()

      val caseAccess3 = CaseAccessFactory()
        .withCrn(crns[2])
        .withUserRestricted(true)
        .withUserExcluded(false)
        .produce()

      val caseAccess4 = CaseAccessFactory()
        .withCrn(crns[3])
        .withUserRestricted(true)
        .withUserExcluded(false)
        .produce()

      val userAccess = UserAccess(listOf(caseAccess1, caseAccess2, caseAccess3, caseAccess4))

      every {
        mockApDeliusContextApiClient.getUserAccessForCrns(
          "distinguished.name",
          crns,
        )
      } returns ClientResult.Success(HttpStatus.OK, userAccess)

      val result = offenderService.canAccessOffenders("distinguished.name", crns)

      assertThat(result[crns[0]]).isTrue()
      assertThat(result[crns[1]]).isFalse()
      assertThat(result[crns[2]]).isFalse()
      assertThat(result[crns[3]]).isFalse()
      assertThat(result[crns[4]]).isFalse()
    }

    @ParameterizedTest
    @ArgumentsSource(ClientResultFailureArgumentsProvider::class)
    fun `throws exception when getUserAccessForCrns returns client result failure`(response: ClientResult.Failure<UserAccess>) {
      val crn = randomNumberChars(8)

      every { mockApDeliusContextApiClient.getUserAccessForCrns("distinguished.name", listOf(crn)) } returns response

      assertThrows<Throwable> { offenderService.canAccessOffenders("distinguished.name", listOf(crn)) }
    }
  }

  @Test
  fun `getInmateDetailByNomsNumber returns not found result when for Offender without Application or Booking and Client responds with 404`() {
    val crn = "CRN123"
    val nomsNumber = "NOMS321"

    every { mockPrisonsApiClient.getInmateDetailsWithWait(nomsNumber) } returns StatusCode(HttpMethod.GET, "/api/offenders/$nomsNumber", HttpStatus.NOT_FOUND, null)

    val result = offenderService.getInmateDetailByNomsNumber(crn, nomsNumber)

    assertThat(result is AuthorisableActionResult.NotFound).isTrue
  }

  @Test
  fun `getInmateDetailByNomsNumber returns not found result when for Offender with Application or Booking and Client responds with 404`() {
    val crn = "CRN123"
    val nomsNumber = "NOMS321"

    every { mockPrisonsApiClient.getInmateDetailsWithWait(nomsNumber) } returns StatusCode(HttpMethod.GET, "/api/offenders/$nomsNumber", HttpStatus.NOT_FOUND, null)

    val result = offenderService.getInmateDetailByNomsNumber(crn, nomsNumber)

    assertThat(result is AuthorisableActionResult.NotFound).isTrue
  }

  @Test
  fun `getInmateDetailByNomsNumber returns unauthorised result when Client responds with 403`() {
    val crn = "CRN123"
    val nomsNumber = "NOMS321"

    every { mockPrisonsApiClient.getInmateDetailsWithWait(nomsNumber) } returns StatusCode(HttpMethod.GET, "/api/offenders/$nomsNumber", HttpStatus.FORBIDDEN, null)

    val result = offenderService.getInmateDetailByNomsNumber(crn, nomsNumber)

    assertThat(result is AuthorisableActionResult.Unauthorised).isTrue
  }

  @Test
  fun `getInmateDetailByNomsNumber returns successfully when Client responds with 200`() {
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

  @Test
  fun `getFilteredPrisonCaseNotesByNomsNumber returns NotFound when Case Notes request returns a 404`() {
    val nomsNumber = "NOMS456"

    every {
      mockCaseNotesClient.getCaseNotesPage(
        nomsNumber = nomsNumber,
        from = LocalDate.now().minusDays(prisonCaseNotesConfigBindingModel.lookbackDays!!.toLong()),
        page = 0,
        pageSize = 2,
      )
    } returns StatusCode(HttpMethod.GET, "/api/offenders/$nomsNumber/v2", HttpStatus.NOT_FOUND, null)

    var result = offenderService.getFilteredPrisonCaseNotesByNomsNumber(nomsNumber, false)
    assertThat(result).isInstanceOf(CasResult.NotFound::class.java)
  }

  @Test
  fun `getFilteredPrisonCaseNotesByNomsNumber returns Unauthorised when Case Notes request returns a 403`() {
    val nomsNumber = "NOMS456"

    every {
      mockCaseNotesClient.getCaseNotesPage(
        nomsNumber = nomsNumber,
        from = LocalDate.now().minusDays(prisonCaseNotesConfigBindingModel.lookbackDays!!.toLong()),
        page = 0,
        pageSize = 2,
      )
    } returns StatusCode(HttpMethod.GET, "/api/offenders/$nomsNumber/v2", HttpStatus.FORBIDDEN, null)

    var result = offenderService.getFilteredPrisonCaseNotesByNomsNumber(nomsNumber, false)
    assertThat(result).isInstanceOf(CasResult.Unauthorised::class.java)
  }

  @Test
  fun `getFilteredPrisonCaseNotesByNomsNumber returns Success, traverses pages from Client & excludes categories + subcategories`() {
    val nomsNumber = "NOMS456"

    val caseNotesPageOne = listOf(
      CaseNoteFactory().produce(),
      CaseNoteFactory().produce(),
      CaseNoteFactory().withType("EXCLUDED_TYPE").produce(),
    )

    val caseNotesPageTwo = listOf(
      CaseNoteFactory().produce(),
      CaseNoteFactory().produce(),
      CaseNoteFactory().withType("TYPE").withSubType("EXCLUDED_SUBTYPE").produce(),
    )

    every {
      mockCaseNotesClient.getCaseNotesPage(
        nomsNumber = nomsNumber,
        from = LocalDate.now().minusDays(prisonCaseNotesConfigBindingModel.lookbackDays!!.toLong()),
        page = 0,
        pageSize = 2,
      )
    } returns ClientResult.Success(
      HttpStatus.OK,
      CaseNotesPage(
        totalElements = 6,
        totalPages = 2,
        number = 1,
        content = caseNotesPageOne,
      ),
    )

    every {
      mockCaseNotesClient.getCaseNotesPage(
        nomsNumber = nomsNumber,
        from = LocalDate.now().minusDays(prisonCaseNotesConfigBindingModel.lookbackDays!!.toLong()),
        page = 1,
        pageSize = 2,
      )
    } returns ClientResult.Success(
      HttpStatus.OK,
      CaseNotesPage(
        totalElements = 4,
        totalPages = 2,
        number = 2,
        content = caseNotesPageTwo,
      ),
    )

    val result = offenderService.getFilteredPrisonCaseNotesByNomsNumber(nomsNumber, false)

    assertThat(result).isInstanceOf(CasResult.Success::class.java)
    result as CasResult.Success
    assertThat(result.value).containsAll(caseNotesPageOne.subList(0, 1) + caseNotesPageTwo.subList(0, 1))
  }

  @Test
  fun `getFilteredPrisonCaseNotesByNomsNumber returns specified case note types when getSpecificNoteTypes is true`() {
    val nomsNumber = "NOMS456"

    val caseNotesPageOne = listOf(
      CaseNoteFactory().produce(),
      CaseNoteFactory().produce(),
      CaseNoteFactory().withTypeDescription(null).withType("Enforcement").produce(),
    )

    val caseNotesPageTwo = listOf(
      CaseNoteFactory().produce(),
      CaseNoteFactory().produce(),
      CaseNoteFactory().withTypeDescription("Alert").produce(),
    )

    every {
      mockCaseNotesClient.getCaseNotesPage(
        nomsNumber = nomsNumber,
        from = LocalDate.now().minusDays(prisonCaseNotesConfigBindingModel.lookbackDays!!.toLong()),
        page = 0,
        pageSize = 2,
      )
    } returns ClientResult.Success(
      HttpStatus.OK,
      CaseNotesPage(
        totalElements = 6,
        totalPages = 2,
        number = 1,
        content = caseNotesPageOne,
      ),
    )

    every {
      mockCaseNotesClient.getCaseNotesPage(
        nomsNumber = nomsNumber,
        from = LocalDate.now().minusDays(prisonCaseNotesConfigBindingModel.lookbackDays!!.toLong()),
        page = 1,
        pageSize = 2,
      )
    } returns ClientResult.Success(
      HttpStatus.OK,
      CaseNotesPage(
        totalElements = 4,
        totalPages = 2,
        number = 2,
        content = caseNotesPageTwo,
      ),
    )

    val result = offenderService.getFilteredPrisonCaseNotesByNomsNumber(nomsNumber, true)

    assertThat(result).isInstanceOf(CasResult.Success::class.java)
    result as CasResult.Success
    assertThat(result.value).containsAll(caseNotesPageTwo.subList(2, 3) + caseNotesPageTwo.subList(2, 3))
  }

  @Test
  fun `getAdjudicationsByNomsNumber returns NotFound when Adjudications request returns a 404`() {
    val nomsNumber = "NOMS456"

    every {
      mockPrisonsApiClient.getAdjudicationsPage(
        nomsNumber = nomsNumber,
        pageSize = 2,
        offset = 0,
      )
    } returns StatusCode(HttpMethod.GET, "/api/offenders/$nomsNumber/adjudications", HttpStatus.NOT_FOUND, null)

    assertThat(offenderService.getAdjudicationsByNomsNumber(nomsNumber) is AuthorisableActionResult.NotFound).isTrue
  }

  @Test
  fun `getAdjudicationsByNomsNumber returns Unauthorised when Case Notes request returns a 403`() {
    val nomsNumber = "NOMS456"

    every {
      mockPrisonsApiClient.getAdjudicationsPage(
        nomsNumber = nomsNumber,
        pageSize = 2,
        offset = 0,
      )
    } returns StatusCode(HttpMethod.GET, "/api/offenders/$nomsNumber/adjudications", HttpStatus.FORBIDDEN, null)

    assertThat(offenderService.getAdjudicationsByNomsNumber(nomsNumber) is AuthorisableActionResult.Unauthorised).isTrue
  }

  @Test
  fun `getAdjudicationsByNomsNumber returns Success, traverses pages from Client`() {
    val nomsNumber = "NOMS456"

    val adjudicationsPageOne = AdjudicationsPageFactory()
      .withResults(
        listOf(
          AdjudicationFactory().withAgencyId("AGNCY1").produce(),
          AdjudicationFactory().withAgencyId("AGNCY2").produce(),
        ),
      )
      .withAgencies(
        listOf(
          AgencyFactory().withAgencyId("AGNCY1").produce(),
          AgencyFactory().withAgencyId("AGNCY2").produce(),
        ),
      )
      .produce()

    val adjudicationsPageTwo = AdjudicationsPageFactory()
      .withResults(
        listOf(
          AdjudicationFactory().withAgencyId("AGNCY3").produce(),
        ),
      )
      .withAgencies(
        listOf(
          AgencyFactory().withAgencyId("AGNCY3").produce(),
        ),
      )
      .produce()

    every {
      mockPrisonsApiClient.getAdjudicationsPage(
        nomsNumber = nomsNumber,
        pageSize = 2,
        offset = 0,
      )
    } returns ClientResult.Success(
      HttpStatus.OK,
      adjudicationsPageOne,
    )

    every {
      mockPrisonsApiClient.getAdjudicationsPage(
        nomsNumber = nomsNumber,
        pageSize = 2,
        offset = 2,
      )
    } returns ClientResult.Success(
      HttpStatus.OK,
      adjudicationsPageTwo,
    )

    val result = offenderService.getAdjudicationsByNomsNumber(nomsNumber)

    assertThat(result is AuthorisableActionResult.Success).isTrue
    result as AuthorisableActionResult.Success
    assertThat(result.entity.results).containsAll(
      adjudicationsPageOne.results.plus(adjudicationsPageTwo.results),
    )
    assertThat(result.entity.agencies).containsExactlyInAnyOrder(
      *adjudicationsPageOne.agencies.union(adjudicationsPageTwo.agencies).toTypedArray(),
    )
  }

  @Test
  fun `isLao returns true for Offender with current restriction`() {
    val offenderDetails = OffenderDetailsSummaryFactory()
      .withCurrentRestriction(true)
      .withCurrentExclusion(false)
      .produce()

    every { mockOffenderDetailsDataSource.getOffenderDetailSummary(offenderDetails.otherIds.crn) } returns ClientResult.Success(
      HttpStatus.OK,
      offenderDetails,
    )

    assertThat(offenderService.isLao(offenderDetails.otherIds.crn)).isTrue
  }

  @Test
  fun `isLao returns true for Offender with current exclusion`() {
    val offenderDetails = OffenderDetailsSummaryFactory()
      .withCurrentRestriction(false)
      .withCurrentExclusion(true)
      .produce()

    every { mockOffenderDetailsDataSource.getOffenderDetailSummary(offenderDetails.otherIds.crn) } returns ClientResult.Success(
      HttpStatus.OK,
      offenderDetails,
    )

    assertThat(offenderService.isLao(offenderDetails.otherIds.crn)).isTrue
  }

  @Test
  fun `isLao returns false for Offender without current exclusion or restriction`() {
    val offenderDetails = OffenderDetailsSummaryFactory()
      .withCurrentRestriction(false)
      .withCurrentExclusion(false)
      .produce()

    every { mockOffenderDetailsDataSource.getOffenderDetailSummary(offenderDetails.otherIds.crn) } returns ClientResult.Success(
      HttpStatus.OK,
      offenderDetails,
    )

    assertThat(offenderService.isLao(offenderDetails.otherIds.crn)).isFalse
  }

  @Nested
  inner class GetInfoForPerson {
    @Test
    fun `returns NotFound if Community API responds with a 404`() {
      val crn = "ABC123"
      val deliusUsername = "USER"

      every { mockOffenderDetailsDataSource.getOffenderDetailSummaries(listOf(crn)) } returns mapOf(
        crn to
          StatusCode(
            HttpMethod.GET,
            "/secure/offenders/crn/ABC123",
            HttpStatus.NOT_FOUND,
            null,
            true,
          ),
      )

      every { mockOffenderDetailsDataSource.getUserAccessForOffenderCrns(deliusUsername, listOf(crn)) } returns mapOf(
        crn to
          StatusCode(
            HttpMethod.GET,
            "/secure/offenders/crn/$crn/user/$deliusUsername/userAccess",
            HttpStatus.NOT_FOUND,
            null,
            true,
          ),
      )

      every { mockPersonTransformer.transformPersonSummaryInfoToPersonInfo(PersonSummaryInfoResult.NotFound(crn), null) } returns PersonInfoResult.NotFound(crn)

      val result = offenderService.getPersonInfoResult(crn, deliusUsername, false)
      assertThat(result is PersonInfoResult.NotFound).isTrue
    }

    @Test
    fun `getInfoForPerson throws runtime exception when Community API responds with a 500`() {
      val crn = "ABC123"
      val deliusUsername = "USER"

      every { mockOffenderDetailsDataSource.getOffenderDetailSummaries(listOf(crn)) } returns mapOf(
        crn to
          StatusCode(
            HttpMethod.GET,
            "/secure/offenders/crn/ABC123",
            HttpStatus.INTERNAL_SERVER_ERROR,
            null,
            true,
          ),
      )

      every {
        mockOffenderDetailsDataSource.getUserAccessForOffenderCrns(deliusUsername, listOf(crn))
      } returns mapOf(
        crn to
          StatusCode(
            HttpMethod.GET,
            "/secure/offenders/crn/$crn/user/$deliusUsername/userAccess",
            HttpStatus.NOT_FOUND,
            null,
            false,
          ),
      )

      val exception = assertThrows<RuntimeException> { offenderService.getPersonInfoResult(crn, deliusUsername, false) }
      assertThat(exception.message).isEqualTo("Unable to complete GET request to /secure/offenders/crn/ABC123: 500 INTERNAL_SERVER_ERROR")
    }

    @Test
    fun `getInfoForPerson throws runtime exception when LAO respond is Forbidden`() {
      val crn = "ABC123"
      val deliusUsername = "USER"

      val offenderDetails = OffenderDetailsSummaryFactory()
        .withCrn(crn)
        .withCurrentRestriction(true)
        .withoutNomsNumber()
        .produce()

      every { mockOffenderDetailsDataSource.getOffenderDetailSummaries(listOf(crn)) } returns mapOf(
        crn to ClientResult.Success(
          status = HttpStatus.OK,
          body = offenderDetails,
        ),
      )

      every { mockOffenderDetailsDataSource.getUserAccessForOffenderCrns(deliusUsername, listOf(crn)) } returns mapOf(
        crn to
          StatusCode(
            HttpMethod.GET,
            "/secure/offenders/crn/$crn/user/$deliusUsername/userAccess",
            HttpStatus.FORBIDDEN,
            null,
          ),
      )

      val exception = assertThrows<RuntimeException> { offenderService.getPersonInfoResult(crn, deliusUsername, false) }
      assertThat(exception.message).isEqualTo("Unable to complete GET request to /secure/offenders/crn/ABC123/user/USER/userAccess: 403 FORBIDDEN")
    }

    @Test
    fun `throws runtime exception when LAO calls fail with BadRequest exception`() {
      val crn = "ABC123"
      val deliusUsername = "USER"

      val offenderDetails = OffenderDetailsSummaryFactory()
        .withCrn(crn)
        .withCurrentRestriction(true)
        .withoutNomsNumber()
        .produce()

      every { mockOffenderDetailsDataSource.getOffenderDetailSummary(crn) } returns ClientResult.Success(
        status = HttpStatus.OK,
        body = offenderDetails,
      )

      every { mockOffenderDetailsDataSource.getUserAccessForOffenderCrn(deliusUsername, crn) } returns StatusCode(
        HttpMethod.GET,
        "/secure/offenders/crn/$crn/user/$deliusUsername/userAccess",
        HttpStatus.BAD_REQUEST,
        null,
      )

      assertThrows<RuntimeException> { offenderService.getPersonInfoResult(crn, deliusUsername, false) }
    }

    @Test
    fun `returns Restricted for LAO Offender where user does not pass check and ignoreLaoRestrictions is false`() {
      val crn = "ABC123"
      val deliusUsername = "USER"
      val nomsNumber = randomStringMultiCaseWithNumbers(10)

      every { mockOffenderDetailsDataSource.getOffenderDetailSummaries(listOf(crn)) } returns mapOf(
        crn to ClientResult.Success(
          status = HttpStatus.OK,
          body = OffenderDetailsSummaryFactory()
            .withCrn(crn)
            .withNomsNumber(nomsNumber)
            .withCurrentRestriction(true)
            .produce(),
        ),
      )

      every { mockOffenderDetailsDataSource.getUserAccessForOffenderCrns(deliusUsername, listOf(crn)) } returns mapOf(
        crn to
          StatusCode(
            status = HttpStatus.FORBIDDEN,
            method = HttpMethod.GET,
            path = "/secure/offenders/crn/$crn/user/$deliusUsername/userAccess",
            body = objectMapper.writeValueAsString(
              UserOffenderAccess(
                userRestricted = true,
                userExcluded = false,
                restrictionMessage = null,
              ),
            ),
          ),
      )

      every {
        mockPersonTransformer.transformPersonSummaryInfoToPersonInfo(
          PersonSummaryInfoResult.Success.Restricted(crn, nomsNumber),
          null,
        )
      } returns PersonInfoResult.Success.Restricted(crn, nomsNumber)

      val result = offenderService.getPersonInfoResult(crn, deliusUsername, ignoreLaoRestrictions = false)

      assertThat(result is PersonInfoResult.Success.Restricted).isTrue
      result as PersonInfoResult.Success.Restricted
      assertThat(result.crn).isEqualTo(crn)
    }

    @Test
    fun `returns Full for LAO Offender where user does pass check and ignoreLaoRestrictions is false`() {
      val crn = "ABC123"
      val deliusUsername = "USER"

      val offenderDetails = OffenderDetailsSummaryFactory()
        .withCrn(crn)
        .withCurrentRestriction(true)
        .withoutNomsNumber()
        .produce()

      every { mockOffenderDetailsDataSource.getOffenderDetailSummaries(listOf(crn)) } returns mapOf(
        crn to ClientResult.Success(
          status = HttpStatus.OK,
          body = offenderDetails,
        ),
      )

      every {
        mockOffenderDetailsDataSource.getUserAccessForOffenderCrns(deliusUsername, listOf(crn))
      } returns mapOf(
        crn to clientResultSuccess(false, false),
      )

      every {
        mockPersonTransformer.transformPersonSummaryInfoToPersonInfo(
          PersonSummaryInfoResult.Success.Full(
            crn,
            offenderDetails.asCaseSummary(),
          ),
          null,
        )
      } returns PersonInfoResult.Success.Full(crn, offenderDetails, null)

      val result = offenderService.getPersonInfoResult(crn, deliusUsername, false)

      assertThat(result is PersonInfoResult.Success.Full).isTrue
      result as PersonInfoResult.Success.Full
      assertThat(result.crn).isEqualTo(crn)
      assertThat(result.offenderDetailSummary).isEqualTo(offenderDetails)
      assertThat(result.inmateDetail).isEqualTo(null)
    }

    @Test
    fun `returns Full for LAO Offender where user does not pass check but ignoreLaoRestrictions is true`() {
      val crn = "ABC123"
      val deliusUsername = "USER"

      val offenderDetails = OffenderDetailsSummaryFactory()
        .withCrn(crn)
        .withCurrentRestriction(true)
        .withoutNomsNumber()
        .produce()

      every { mockOffenderDetailsDataSource.getOffenderDetailSummaries(listOf(crn)) } returns mapOf(
        crn to ClientResult.Success(
          status = HttpStatus.OK,
          body = offenderDetails,
        ),
      )

      every { mockOffenderDetailsDataSource.getUserAccessForOffenderCrns(deliusUsername, listOf(crn)) } returns mapOf(
        crn to
          StatusCode(
            HttpMethod.GET,
            "/secure/offenders/crn/$crn/user/$deliusUsername/userAccess",
            HttpStatus.FORBIDDEN,
            objectMapper.writeValueAsString(
              UserOffenderAccess(
                userRestricted = true,
                userExcluded = false,
                restrictionMessage = null,
              ),
            ),
            true,
          ),
      )

      every {
        mockPersonTransformer.transformPersonSummaryInfoToPersonInfo(
          PersonSummaryInfoResult.Success.Full(
            crn,
            offenderDetails.asCaseSummary(),
          ),
          null,
        )
      } returns PersonInfoResult.Success.Full(crn, offenderDetails, null)

      val result = offenderService.getPersonInfoResult(crn, deliusUsername, ignoreLaoRestrictions = true)

      assertThat(result is PersonInfoResult.Success.Full).isTrue
      result as PersonInfoResult.Success.Full
      assertThat(result.crn).isEqualTo(crn)
      assertThat(result.offenderDetailSummary).isEqualTo(offenderDetails)
      assertThat(result.inmateDetail).isEqualTo(null)
    }

    @Test
    fun `returns Full for CRN with both Community API and Prison API data where Community API links to Prison API`() {
      val crn = "ABC123"
      val nomsNumber = "NOMSABC"
      val deliusUsername = "USER"

      val offenderDetails = OffenderDetailsSummaryFactory()
        .withCrn(crn)
        .withNomsNumber(nomsNumber)
        .produce()

      every { mockOffenderDetailsDataSource.getOffenderDetailSummaries(listOf(crn)) } returns mapOf(
        crn to ClientResult.Success(
          status = HttpStatus.OK,
          body = offenderDetails,
        ),
      )

      every { mockOffenderDetailsDataSource.getUserAccessForOffenderCrns(deliusUsername, listOf(crn)) } returns mapOf(
        crn to clientResultSuccess(false, false),
      )

      val inmateDetail = InmateDetailFactory()
        .withOffenderNo(nomsNumber)
        .produce()

      every { mockPrisonsApiClient.getInmateDetailsWithWait(nomsNumber) } returns ClientResult.Success(
        status = HttpStatus.OK,
        body = inmateDetail,
      )

      every {
        mockPersonTransformer.transformPersonSummaryInfoToPersonInfo(
          PersonSummaryInfoResult.Success.Full(
            crn,
            offenderDetails.asCaseSummary(),
          ),
          inmateDetail,
        )
      } returns PersonInfoResult.Success.Full(crn, offenderDetails, inmateDetail)

      val result = offenderService.getPersonInfoResult(crn, deliusUsername, false)

      assertThat(result is PersonInfoResult.Success.Full).isTrue
      result as PersonInfoResult.Success.Full
      assertThat(result.crn).isEqualTo(crn)
      assertThat(result.offenderDetailSummary).isEqualTo(offenderDetails)
      assertThat(result.inmateDetail).isEqualTo(inmateDetail)
    }
  }

  @Nested
  inner class GetOffenderSummariesByCrns {
    private val user = UserEntityFactory()
      .withUnitTestControlProbationRegion()
      .produce()

    private val crns = listOf(
      "ABC1",
      "ABC2",
      "ABC3",
      "NOTFOUND",
    )

    private val offenderSummaryResultsByCrn = mapOf(
      crns[1] to
        ClientResult.Success(
          HttpStatus.OK,
          OffenderDetailsSummaryFactory()
            .withCrn(crns[1])
            .withCurrentExclusion(false)
            .withCurrentRestriction(true)
            .produce(),
          false,
        ),
      crns[0] to
        ClientResult.Success(
          HttpStatus.OK,
          OffenderDetailsSummaryFactory()
            .withCrn(crns[0])
            .withCurrentExclusion(true)
            .withCurrentRestriction(false)
            .produce(),
          false,
        ),
      crns[2] to
        ClientResult.Success(
          HttpStatus.OK,
          OffenderDetailsSummaryFactory()
            .withCrn(crns[2])
            .withCurrentExclusion(false)
            .withCurrentRestriction(false)
            .produce(),
          false,
        ),
      crns[3] to
        StatusCode(
          HttpMethod.GET,
          "/",
          HttpStatus.NOT_FOUND,
          null,
          false,
        ),
    )

    private val caseSummariesByCrn = offenderSummaryResultsByCrn.mapNotNull {
      val key = it.key
      val value = it.value
      when (value) {
        is ClientResult.Success -> key to value.body.asCaseSummary()
        else -> null
      }
    }.toMap()

    @Test
    fun `it returns an empty list when no CRNs are provided`() {
      val result = offenderService.getOffenderSummariesByCrns(emptySet(), user.deliusUsername, false)

      assertThat(result).isEmpty()
    }

    @Test
    fun `it returns not found for CRN if the CRN isn't included in offender details list`() {
      val requestCrns = setOf(crns[0], crns[1])

      every {
        mockOffenderDetailsDataSource.getOffenderDetailSummaries(requestCrns.toList())
      } returns mapOf(
        crns[0] to offenderSummaryResultsByCrn[crns[0]]!!,
      )

      every {
        mockOffenderDetailsDataSource.getUserAccessForOffenderCrns(user.deliusUsername, requestCrns.toList())
      } returns mapOf(
        crns[0] to clientResultSuccess(false, false),
        crns[1] to clientResultSuccess(false, false),
      )

      val result = offenderService.getOffenderSummariesByCrns(requestCrns, user.deliusUsername, false)

      assertThat(result[0]).isEqualTo(PersonSummaryInfoResult.Success.Full(crns[0], caseSummariesByCrn[crns[0]]!!))
      assertThat(result[1]).isEqualTo(PersonSummaryInfoResult.NotFound(crns[1]))
    }

    @Test
    fun `it returns not found for CRN if the CRN isn't included in user access list`() {
      val requestCrns = setOf(crns[0], crns[1])

      every {
        mockOffenderDetailsDataSource.getOffenderDetailSummaries(requestCrns.toList())
      } returns mapOf(
        crns[0] to offenderSummaryResultsByCrn[crns[0]]!!,
        crns[1] to offenderSummaryResultsByCrn[crns[1]]!!,
      )

      every {
        mockOffenderDetailsDataSource.getUserAccessForOffenderCrns(user.deliusUsername, requestCrns.toList())
      } returns mapOf(
        crns[0] to clientResultSuccess(false, false),
      )

      val result = offenderService.getOffenderSummariesByCrns(requestCrns, user.deliusUsername, false)

      assertThat(result[0]).isEqualTo(PersonSummaryInfoResult.Success.Full(crns[0], caseSummariesByCrn[crns[0]]!!))
      assertThat(result[1]).isEqualTo(PersonSummaryInfoResult.NotFound(crns[1]))
    }

    @Test
    fun `it returns full summaries when the user has the correct access`() {
      every { mockOffenderDetailsDataSource.getOffenderDetailSummaries(crns) } returns offenderSummaryResultsByCrn

      every {
        mockOffenderDetailsDataSource.getUserAccessForOffenderCrns(user.deliusUsername, crns)
      } returns crns.associateWith {
        clientResultSuccess(false, false)
      }

      val result = offenderService.getOffenderSummariesByCrns(crns.toSet(), user.deliusUsername, false)

      assertThat(result[0]).isEqualTo(PersonSummaryInfoResult.Success.Full(crns[0], caseSummariesByCrn[crns[0]]!!))
      assertThat(result[1]).isEqualTo(PersonSummaryInfoResult.Success.Full(crns[1], caseSummariesByCrn[crns[1]]!!))
      assertThat(result[2]).isEqualTo(PersonSummaryInfoResult.Success.Full(crns[2], caseSummariesByCrn[crns[2]]!!))
      assertThat(result[3]).isEqualTo(PersonSummaryInfoResult.NotFound(crns[3]))
    }

    @Test
    fun `it ignores LAO when ignoreLaoRestrictions is set to true`() {
      every { mockOffenderDetailsDataSource.getOffenderDetailSummaries(crns) } returns offenderSummaryResultsByCrn

      every {
        mockOffenderDetailsDataSource.getUserAccessForOffenderCrns(user.deliusUsername, crns)
      } returns mapOf(
        crns[0] to clientResultSuccess(false, true),
        crns[1] to clientResultSuccess(true, false),
        crns[2] to clientResultSuccess(false, false),
        crns[3] to
          StatusCode(
            HttpMethod.GET,
            "/",
            HttpStatus.NOT_FOUND,
            null,
            false,
          ),
      )

      val result = offenderService.getOffenderSummariesByCrns(crns.toSet(), user.deliusUsername, ignoreLaoRestrictions = true)

      assertThat(result[0]).isEqualTo(PersonSummaryInfoResult.Success.Full(crns[0], caseSummariesByCrn[crns[0]]!!))
      assertThat(result[1]).isEqualTo(PersonSummaryInfoResult.Success.Full(crns[1], caseSummariesByCrn[crns[1]]!!))
      assertThat(result[2]).isEqualTo(PersonSummaryInfoResult.Success.Full(crns[2], caseSummariesByCrn[crns[2]]!!))
      assertThat(result[3]).isEqualTo(PersonSummaryInfoResult.NotFound(crns[3]))
    }
  }

  companion object {
    const val OFFENDER_1_CRN = "CRN1"
    const val OFFENDER_1_NOMS: String = "NOMS1"
    const val OFFENDER_2_CRN = "CRN2"
    const val OFFENDER_2_NOMS = "NOMS2"
    const val OFFENDER_3_CRN = "CRN3"
    const val USERNAME = "deliusUsername"
  }

  @Nested
  inner class GetPersonSummaryInfoResults {

    @Test
    fun `if no crns provided immediately return empty list`() {
      val result = offenderService.getPersonSummaryInfoResults(
        crns = emptySet(),
        limitedAccessStrategy = ReturnRestrictedIfLimitedAccess(USERNAME),
      )

      assertThat(result).isEmpty()
    }

    @ParameterizedTest
    @ArgumentsSource(ClientResultFailureArgumentsProvider::class)
    fun `any error retrieving case summaries is rethrown`(response: ClientResult.Failure<CaseSummaries>) {
      every {
        mockApDeliusContextApiClient.getSummariesForCrns(listOf(OFFENDER_1_CRN))
      } returns response

      assertThrows<Throwable> {
        offenderService.getPersonSummaryInfoResults(
          crns = setOf(OFFENDER_1_CRN),
          limitedAccessStrategy = ReturnRestrictedIfLimitedAccess(USERNAME),
        )
      }
    }

    @ParameterizedTest
    @ArgumentsSource(ClientResultFailureArgumentsProvider::class)
    fun `any error retrieving case access is rethrown`(response: ClientResult.Failure<UserAccess>) {
      every {
        mockApDeliusContextApiClient.getSummariesForCrns(listOf(OFFENDER_1_CRN))
      } returns ClientResult.Success(HttpStatus.OK, CaseSummaries(emptyList()))

      every {
        mockApDeliusContextApiClient.getUserAccessForCrns(USERNAME, listOf(OFFENDER_1_CRN))
      } returns response

      assertThrows<Throwable> {
        offenderService.getPersonSummaryInfoResults(
          crns = setOf(OFFENDER_1_CRN),
          limitedAccessStrategy = ReturnRestrictedIfLimitedAccess(USERNAME),
        )
      }
    }

    @Test
    fun `single crn, case summary not found, return NotFound`() {
      every {
        mockApDeliusContextApiClient.getSummariesForCrns(listOf(OFFENDER_1_CRN))
      } returns ClientResult.Success(HttpStatus.OK, CaseSummaries(emptyList()))

      every {
        mockApDeliusContextApiClient.getUserAccessForCrns(USERNAME, listOf(OFFENDER_1_CRN))
      } returns ClientResult.Success(HttpStatus.OK, UserAccess(emptyList()))

      val results = offenderService.getPersonSummaryInfoResults(
        crns = setOf(OFFENDER_1_CRN),
        limitedAccessStrategy = ReturnRestrictedIfLimitedAccess(USERNAME),
      )

      assertThat(results).hasSize(1)
      assertThat(results[0]).isInstanceOf(PersonSummaryInfoResult.NotFound::class.java)
      val result0 = results[0] as PersonSummaryInfoResult.NotFound
      assertThat(result0.crn).isEqualTo(OFFENDER_1_CRN)
    }

    @Test
    fun `single crn with no limited access, return Success`() {
      val offender1CaseSummary = CaseSummaryFactory()
        .withCrn(OFFENDER_1_CRN)
        .withNomsId(OFFENDER_1_NOMS)
        .withCurrentExclusion(false)
        .withCurrentRestriction(false)
        .produce()

      every {
        mockApDeliusContextApiClient.getSummariesForCrns(listOf(OFFENDER_1_CRN))
      } returns ClientResult.Success(HttpStatus.OK, CaseSummaries(listOf(offender1CaseSummary)))

      every {
        mockApDeliusContextApiClient.getUserAccessForCrns(USERNAME, listOf(OFFENDER_1_CRN))
      } returns ClientResult.Success(HttpStatus.OK, UserAccess(emptyList()))

      val results = offenderService.getPersonSummaryInfoResults(
        crns = setOf(OFFENDER_1_CRN),
        limitedAccessStrategy = ReturnRestrictedIfLimitedAccess(USERNAME),
      )

      assertThat(results).hasSize(1)
      assertThat(results[0]).isInstanceOf(PersonSummaryInfoResult.Success.Full::class.java)
      val result0 = results[0] as PersonSummaryInfoResult.Success.Full
      assertThat(result0.crn).isEqualTo(OFFENDER_1_CRN)
      assertThat(result0.summary).isSameAs(offender1CaseSummary)
    }

    @ParameterizedTest
    @CsvSource(
      "true,false",
      "false,true",
      "true,true",
    )
    fun `single crn with limited access, ReturnRestrictedIfLimitedAccess strategy, no limited access record returned, return NotFound`(
      currentExclusion: Boolean,
      currentRestriction: Boolean,
    ) {
      val offender1CaseSummary = CaseSummaryFactory()
        .withCrn(OFFENDER_1_CRN)
        .withCurrentExclusion(currentExclusion)
        .withCurrentRestriction(currentRestriction)
        .produce()

      every {
        mockApDeliusContextApiClient.getSummariesForCrns(listOf(OFFENDER_1_CRN))
      } returns ClientResult.Success(HttpStatus.OK, CaseSummaries(listOf(offender1CaseSummary)))

      every {
        mockApDeliusContextApiClient.getUserAccessForCrns(USERNAME, listOf(OFFENDER_1_CRN))
      } returns ClientResult.Success(HttpStatus.OK, UserAccess(emptyList()))

      val results = offenderService.getPersonSummaryInfoResults(
        crns = setOf(OFFENDER_1_CRN),
        limitedAccessStrategy = ReturnRestrictedIfLimitedAccess(USERNAME),
      )

      assertThat(results).hasSize(1)
      assertThat(results[0]).isInstanceOf(PersonSummaryInfoResult.NotFound::class.java)
      val result0 = results[0] as PersonSummaryInfoResult.NotFound
      assertThat(result0.crn).isEqualTo(OFFENDER_1_CRN)
    }

    @ParameterizedTest
    @CsvSource(
      "true,false",
      "false,true",
      "true,true",
    )
    fun `single crn with limited access, ReturnRestrictedIfLimitedAccess strategy, user is not limited, return Success`(
      currentExclusion: Boolean,
      currentRestriction: Boolean,
    ) {
      val offender1CaseSummary = CaseSummaryFactory()
        .withCrn(OFFENDER_1_CRN)
        .withNomsId(OFFENDER_1_NOMS)
        .withCurrentExclusion(currentExclusion)
        .withCurrentRestriction(currentRestriction)
        .produce()

      every {
        mockApDeliusContextApiClient.getSummariesForCrns(listOf(OFFENDER_1_CRN))
      } returns ClientResult.Success(HttpStatus.OK, CaseSummaries(listOf(offender1CaseSummary)))

      every {
        mockApDeliusContextApiClient.getUserAccessForCrns(USERNAME, listOf(OFFENDER_1_CRN))
      } returns ClientResult.Success(
        HttpStatus.OK,
        UserAccess(
          listOf(
            CaseAccessFactory()
              .withCrn(OFFENDER_1_CRN)
              .withUserExcluded(false)
              .withUserRestricted(false)
              .produce(),
          ),
        ),
      )

      val results = offenderService.getPersonSummaryInfoResults(
        crns = setOf(OFFENDER_1_CRN),
        limitedAccessStrategy = ReturnRestrictedIfLimitedAccess(USERNAME),
      )

      assertThat(results).hasSize(1)
      assertThat(results[0]).isInstanceOf(PersonSummaryInfoResult.Success.Full::class.java)
      val result0 = results[0] as PersonSummaryInfoResult.Success.Full
      assertThat(result0.crn).isEqualTo(OFFENDER_1_CRN)
      assertThat(result0.summary).isSameAs(offender1CaseSummary)
    }

    @ParameterizedTest
    @CsvSource(
      "true,false",
      "false,true",
      "true,true",
    )
    fun `single crn with limited access, ReturnRestrictedIfLimitedAccess strategy, user is limited, return Restricted`(
      currentExclusion: Boolean,
      currentRestriction: Boolean,
    ) {
      val offender1CaseSummary = CaseSummaryFactory()
        .withCrn(OFFENDER_1_CRN)
        .withNomsId(OFFENDER_1_NOMS)
        .withCurrentExclusion(currentExclusion)
        .withCurrentRestriction(currentRestriction)
        .produce()

      every {
        mockApDeliusContextApiClient.getSummariesForCrns(listOf(OFFENDER_1_CRN))
      } returns ClientResult.Success(HttpStatus.OK, CaseSummaries(listOf(offender1CaseSummary)))

      every {
        mockApDeliusContextApiClient.getUserAccessForCrns(USERNAME, listOf(OFFENDER_1_CRN))
      } returns ClientResult.Success(
        HttpStatus.OK,
        UserAccess(
          listOf(
            CaseAccessFactory()
              .withCrn(OFFENDER_1_CRN)
              .withUserExcluded(currentExclusion)
              .withUserRestricted(currentRestriction)
              .produce(),
          ),
        ),
      )

      val results = offenderService.getPersonSummaryInfoResults(
        crns = setOf(OFFENDER_1_CRN),
        limitedAccessStrategy = ReturnRestrictedIfLimitedAccess(USERNAME),
      )

      assertThat(results).hasSize(1)
      assertThat(results[0]).isInstanceOf(PersonSummaryInfoResult.Success.Restricted::class.java)
      val result0 = results[0] as PersonSummaryInfoResult.Success.Restricted
      assertThat(result0.crn).isEqualTo(OFFENDER_1_CRN)
      assertThat(result0.nomsNumber).isEqualTo(OFFENDER_1_NOMS)
    }

    @ParameterizedTest
    @CsvSource(
      "true,false",
      "false,true",
      "true,true",
    )
    fun `single crn with limited access, IgnoreLimitedAccess strategy, user is limited, return Success`(
      currentExclusion: Boolean,
      currentRestriction: Boolean,
    ) {
      val offender1CaseSummary = CaseSummaryFactory()
        .withCrn(OFFENDER_1_CRN)
        .withNomsId(OFFENDER_1_NOMS)
        .withCurrentExclusion(currentExclusion)
        .withCurrentRestriction(currentRestriction)
        .produce()

      every {
        mockApDeliusContextApiClient.getSummariesForCrns(listOf(OFFENDER_1_CRN))
      } returns ClientResult.Success(HttpStatus.OK, CaseSummaries(listOf(offender1CaseSummary)))

      val results = offenderService.getPersonSummaryInfoResults(
        crns = setOf(OFFENDER_1_CRN),
        limitedAccessStrategy = LimitedAccessStrategy.IgnoreLimitedAccess,
      )

      verify(exactly = 0) { mockApDeliusContextApiClient.getUserAccessForCrns(any(), any()) }

      assertThat(results).hasSize(1)
      assertThat(results[0]).isInstanceOf(PersonSummaryInfoResult.Success.Full::class.java)
      val result0 = results[0] as PersonSummaryInfoResult.Success.Full
      assertThat(result0.crn).isEqualTo(OFFENDER_1_CRN)
      assertThat(result0.summary).isSameAs(offender1CaseSummary)
    }

    @Test
    fun `multiple crns with all possible return types`() {
      val offender1Success = CaseSummaryFactory()
        .withCrn(OFFENDER_1_CRN)
        .withNomsId(OFFENDER_1_NOMS)
        .withCurrentExclusion(false)
        .withCurrentRestriction(false)
        .produce()

      val offender2Restricted = CaseSummaryFactory()
        .withCrn(OFFENDER_2_CRN)
        .withNomsId(OFFENDER_2_NOMS)
        .withCurrentExclusion(true)
        .withCurrentRestriction(false)
        .produce()

      CaseSummaryFactory()
        .withCrn(OFFENDER_3_CRN)
        .produce()

      every {
        mockApDeliusContextApiClient.getSummariesForCrns(listOf(OFFENDER_1_CRN, OFFENDER_2_CRN, OFFENDER_3_CRN))
      } returns ClientResult.Success(
        HttpStatus.OK,
        CaseSummaries(
          listOf(
            offender1Success,
            offender2Restricted,
          ),
        ),
      )

      every {
        mockApDeliusContextApiClient.getUserAccessForCrns(USERNAME, listOf(OFFENDER_1_CRN, OFFENDER_2_CRN, OFFENDER_3_CRN))
      } returns ClientResult.Success(
        HttpStatus.OK,
        UserAccess(
          listOf(
            CaseAccessFactory()
              .withCrn(OFFENDER_1_CRN)
              .withUserRestricted(false)
              .withUserExcluded(false)
              .produce(),
            CaseAccessFactory()
              .withCrn(OFFENDER_2_CRN)
              .withUserRestricted(false)
              .withUserExcluded(true)
              .produce(),
          ),
        ),
      )

      val results = offenderService.getPersonSummaryInfoResults(
        crns = setOf(OFFENDER_1_CRN, OFFENDER_2_CRN, OFFENDER_3_CRN),
        limitedAccessStrategy = ReturnRestrictedIfLimitedAccess(USERNAME),
      )

      assertThat(results).hasSize(3)

      assertThat(results[0]).isInstanceOf(PersonSummaryInfoResult.Success.Full::class.java)
      val result0 = results[0] as PersonSummaryInfoResult.Success.Full
      assertThat(result0.crn).isEqualTo(OFFENDER_1_CRN)
      assertThat(result0.summary).isSameAs(offender1Success)

      assertThat(results[1]).isInstanceOf(PersonSummaryInfoResult.Success.Restricted::class.java)
      val result1 = results[1] as PersonSummaryInfoResult.Success.Restricted
      assertThat(result1.crn).isEqualTo(OFFENDER_2_CRN)
      assertThat(result1.nomsNumber).isEqualTo(OFFENDER_2_NOMS)

      assertThat(results[2]).isInstanceOf(PersonSummaryInfoResult.NotFound::class.java)
      val result2 = results[2] as PersonSummaryInfoResult.NotFound
      assertThat(result2.crn).isEqualTo(OFFENDER_3_CRN)
    }
  }

  private fun clientResultSuccess(
    userRestricted: Boolean,
    userExcluded: Boolean,
  ) = ClientResult.Success(
    HttpStatus.OK,
    UserOffenderAccess(userRestricted, userExcluded, restrictionMessage = null),
    false,
  )
}
