package uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.service.cas2v2

import com.fasterxml.jackson.databind.ObjectMapper
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.mockito.kotlin.any
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.data.repository.findByIdOrNull
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.SortDirection
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.SubmitCas2Application
import uk.gov.justice.digital.hmpps.approvedpremisesapi.config.NotifyConfig
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.Cas2v2ApplicationJsonSchemaEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.InmateDetailFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.NomisUserEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.OffenderDetailsSummaryFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.cas2v2.Cas2v2ApplicationEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.Cas2v2ApplicationJsonSchemaEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.cas2v2.Cas2v2ApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.cas2v2.Cas2v2ApplicationRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.cas2v2.Cas2v2ApplicationSummaryEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.cas2v2.Cas2v2ApplicationSummaryRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.cas2v2.Cas2v2LockableApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.cas2v2.Cas2v2LockableApplicationRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.prisonsapi.AssignedLivingUnit
import uk.gov.justice.digital.hmpps.approvedpremisesapi.results.AuthorisableActionResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.results.CasResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.results.ValidatableActionResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.EmailNotificationService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas2.DomainEventService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas2.JsonSchemaService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas2.OffenderService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas2v2.Cas2v2ApplicationService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas2v2.Cas2v2AssessmentService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas2v2.Cas2v2UserAccessService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.PageCriteria
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.PaginationConfig
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.extractEntityFromCasResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.getPageableOrAllPages
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.randomStringMultiCaseWithNumbers
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.UUID

class Cas2v2ApplicationServiceTest {
  private val mockCas2v2ApplicationRepository = mockk<Cas2v2ApplicationRepository>()
  private val mockCas2v2LockableApplicationRepository = mockk<Cas2v2LockableApplicationRepository>()
  private val mockCas2v2ApplicationSummaryRepository = mockk<Cas2v2ApplicationSummaryRepository>()
  private val mockJsonSchemaService = mockk<JsonSchemaService>()
  private val mockOffenderService = mockk<OffenderService>()
  private val mockCas2v2UserAccessService = mockk<Cas2v2UserAccessService>()
  private val mockDomainEventService = mockk<DomainEventService>()
  private val mockEmailNotificationService = mockk<EmailNotificationService>()
  private val mockCas2v2AssessmentService = mockk<Cas2v2AssessmentService>()
  private val mockObjectMapper = mockk<ObjectMapper>()
  private val mockNotifyConfig = mockk<NotifyConfig>()

  private val cas2v2ApplicationService = Cas2v2ApplicationService(
    mockCas2v2ApplicationRepository,
    mockCas2v2LockableApplicationRepository,
    mockCas2v2ApplicationSummaryRepository,
    mockJsonSchemaService,
    mockOffenderService,
    mockCas2v2UserAccessService,
    mockDomainEventService,
    mockEmailNotificationService,
    mockCas2v2AssessmentService,
    mockNotifyConfig,
    mockObjectMapper,
    "http://frontend/applications/#id",
    "http://frontend/assess/applications/#applicationId/overview",
  )

  @Nested
  inner class GetAllSubmittedCas2v2ApplicationsForAssessor {
    @Test
    fun `returns Success result with entity from db`() {
      val cas2v2ApplicationSummary = Cas2v2ApplicationSummaryEntity(
        id = UUID.fromString("2f838a8c-dffc-48a3-9536-f0e95985e809"),
        crn = randomStringMultiCaseWithNumbers(6),
        nomsNumber = randomStringMultiCaseWithNumbers(6),
        userId = "836a9460-b177-433a-a0d9-262509092c9f",
        userName = "first last",
        createdAt = OffsetDateTime.parse("2023-04-19T13:25:00+01:00"),
        submittedAt = OffsetDateTime.parse("2023-04-19T13:25:30+01:00"),
        hdcEligibilityDate = LocalDate.parse("2023-04-29"),
        latestStatusUpdateLabel = null,
        latestStatusUpdateStatusId = null,
        prisonCode = "BRI",
      )

      PaginationConfig(defaultPageSize = 10).postInit()
      val page = mockk<Page<Cas2v2ApplicationSummaryEntity>>()
      val pageRequest = mockk<PageRequest>()
      val pageCriteria = PageCriteria(sortBy = "submitted_at", sortDirection = SortDirection.asc, page = 3)

      mockkStatic(PageRequest::class)

      every { PageRequest.of(2, 10, Sort.by("submitted_at").ascending()) } returns pageRequest
      every { page.content } returns listOf(cas2v2ApplicationSummary)
      every { page.totalPages } returns 10
      every { page.totalElements } returns 100

      every {
        mockCas2v2ApplicationSummaryRepository.findBySubmittedAtIsNotNull(
          PageRequest.of(
            2,
            10,
            Sort.by(Sort.Direction.ASC, "submitted_at"),
          ),
        )
      } returns page

      val (applicationSummaries, metadata) = cas2v2ApplicationService.getAllSubmittedCas2v2ApplicationsForAssessor(pageCriteria)

      assertThat(applicationSummaries).isEqualTo(listOf(cas2v2ApplicationSummary))
      assertThat(metadata?.currentPage).isEqualTo(3)
      assertThat(metadata?.pageSize).isEqualTo(10)
      assertThat(metadata?.totalPages).isEqualTo(10)
      assertThat(metadata?.totalResults).isEqualTo(100)
    }
  }

  @Nested
  inner class GetCas2v2ApplicationsWithPrisonCode {
    val cas2v2ApplicationSummary = Cas2v2ApplicationSummaryEntity(
      id = UUID.fromString("2f838a8c-dffc-48a3-9536-f0e95985e809"),
      crn = randomStringMultiCaseWithNumbers(6),
      nomsNumber = randomStringMultiCaseWithNumbers(6),
      userId = "836a9460-b177-433a-a0d9-262509092c9f",
      userName = "first last",
      createdAt = OffsetDateTime.parse("2023-04-19T13:25:00+01:00"),
      submittedAt = OffsetDateTime.parse("2023-04-19T13:25:30+01:00"),
      hdcEligibilityDate = LocalDate.parse("2023-04-29"),
      latestStatusUpdateLabel = null,
      latestStatusUpdateStatusId = null,
      prisonCode = "BRI",
    )
    val page = mockk<Page<Cas2v2ApplicationSummaryEntity>>()
    val pageCriteria = PageCriteria(sortBy = "submitted_at", sortDirection = SortDirection.asc, page = 3)
    val user = NomisUserEntityFactory().produce()
    val prisonCode = "BRI"

    private fun testPrisonCodeWithIsSubmitted(isSubmitted: Boolean?) {
      every { page.content } returns listOf(cas2v2ApplicationSummary)
      every { page.totalPages } returns 10
      every { page.totalElements } returns 100

      val (applicationSummaries, _) = cas2v2ApplicationService.getCas2v2Applications(prisonCode, isSubmitted, user, pageCriteria)

      assertThat(applicationSummaries).isEqualTo(listOf(cas2v2ApplicationSummary))
    }

    @Test
    fun `return all applications when prisonCode is specified and isSubmitted is null`() {
      PaginationConfig(defaultPageSize = 10).postInit()

      every {
        mockCas2v2ApplicationSummaryRepository.findByPrisonCode(
          prisonCode,
          getPageableOrAllPages(pageCriteria),
        )
      } returns page

      testPrisonCodeWithIsSubmitted(null)
    }

    @Test
    fun `return submitted prison applications when prisonCode is specified and isSubmitted is true`() {
      PaginationConfig(defaultPageSize = 10).postInit()

      every {
        mockCas2v2ApplicationSummaryRepository.findByPrisonCodeAndSubmittedAtIsNotNull(
          prisonCode,
          getPageableOrAllPages(pageCriteria),
        )
      } returns page

      testPrisonCodeWithIsSubmitted(true)
    }

    @Test
    fun `return unsubmitted prison applications when prisonCode is specified and isSubmitted is false`() {
      PaginationConfig(defaultPageSize = 10).postInit()

      every {
        mockCas2v2ApplicationSummaryRepository.findByPrisonCodeAndSubmittedAtIsNull(
          prisonCode,
          getPageableOrAllPages(pageCriteria),
        )
      } returns page

      testPrisonCodeWithIsSubmitted(false)
    }
  }

  @Nested
  inner class GetCas2v2ApplicationForUser {
    @Test
    fun `where cas2v2 application does not exist returns NotFound result`() {
      val user = NomisUserEntityFactory().produce()
      val applicationId = UUID.fromString("c1750938-19fc-48a1-9ae9-f2e119ffc1f4")

      every { mockCas2v2ApplicationRepository.findByIdOrNull(applicationId) } returns null

      assertThat(cas2v2ApplicationService.getCas2v2ApplicationForUser(applicationId, user) is AuthorisableActionResult.NotFound).isTrue
    }

    @Test
    fun `where cas2v2 application is abandoned returns NotFound result`() {
      val user = NomisUserEntityFactory().produce()
      val applicationId = UUID.fromString("c1750938-19fc-48a1-9ae9-f2e119ffc1f4")

      every { mockCas2v2ApplicationRepository.findByIdOrNull(any()) } returns
        Cas2v2ApplicationEntityFactory()
          .withCreatedByUser(
            NomisUserEntityFactory()
              .produce(),
          )
          .withAbandonedAt(OffsetDateTime.now())
          .produce()

      assertThat(cas2v2ApplicationService.getCas2v2ApplicationForUser(applicationId, user) is AuthorisableActionResult.NotFound).isTrue
    }

    @Test
    fun `where user cannot access the cas2v2 application returns Unauthorised result`() {
      val user = NomisUserEntityFactory()
        .produce()
      val applicationId = UUID.fromString("c1750938-19fc-48a1-9ae9-f2e119ffc1f4")

      every { mockCas2v2ApplicationRepository.findByIdOrNull(any()) } returns
        Cas2v2ApplicationEntityFactory()
          .withCreatedByUser(
            NomisUserEntityFactory()
              .produce(),
          )
          .produce()

      every { mockCas2v2UserAccessService.userCanViewCas2v2Application(any(), any()) } returns false

      assertThat(cas2v2ApplicationService.getCas2v2ApplicationForUser(applicationId, user) is AuthorisableActionResult.Unauthorised).isTrue
    }

    @Test
    fun `where user can access the cas2v2 application returns Success result with entity from db`() {
      val distinguishedName = "SOMEPERSON"
      val userId = UUID.fromString("239b5e41-f83e-409e-8fc0-8f1e058d417e")
      val applicationId = UUID.fromString("c1750938-19fc-48a1-9ae9-f2e119ffc1f4")

      val newestJsonSchema = Cas2v2ApplicationJsonSchemaEntityFactory()
        .withSchema("{}")
        .produce()

      val userEntity = NomisUserEntityFactory()
        .withId(userId)
        .withNomisUsername(distinguishedName)
        .produce()

      val cas2v2ApplicationEntity = Cas2v2ApplicationEntityFactory()
        .withCreatedByUser(userEntity)
        .withApplicationSchema(newestJsonSchema)
        .produce()

      every { mockJsonSchemaService.checkCas2v2SchemaOutdated(any()) } answers {
        it.invocation
          .args[0] as Cas2v2ApplicationEntity
      }
      every { mockCas2v2ApplicationRepository.findByIdOrNull(any()) } returns cas2v2ApplicationEntity
      every { mockCas2v2UserAccessService.userCanViewCas2v2Application(any(), any()) } returns true

      val result = cas2v2ApplicationService.getCas2v2ApplicationForUser(applicationId, userEntity)

      assertThat(result is AuthorisableActionResult.Success).isTrue
      result as AuthorisableActionResult.Success

      assertThat(result.entity).isEqualTo(cas2v2ApplicationEntity)
    }
  }

  @Nested
  inner class CreateApplication {
    @Test
    fun `returns FieldValidationError when Offender is not found`() {
      val crn = "CRN345"
      val username = "SOMEPERSON"

      every { mockOffenderService.getOffenderByCrn(crn) } returns AuthorisableActionResult.NotFound()

      val user = userWithUsername(username)

      val result = cas2v2ApplicationService.createCas2v2Application(crn, user)

      assertThat(result is ValidatableActionResult.FieldValidationError).isTrue
      result as ValidatableActionResult.FieldValidationError
      assertThat(result.validationMessages).containsEntry("$.crn", "doesNotExist")
    }

    @Test
    fun `returns FieldValidationError when user is not authorised to view CRN`() {
      val crn = "CRN345"
      val username = "SOMEPERSON"

      every { mockOffenderService.getOffenderByCrn(crn) } returns AuthorisableActionResult.Unauthorised()

      val user = userWithUsername(username)

      val result = cas2v2ApplicationService.createCas2v2Application(crn, user)

      assertThat(result is ValidatableActionResult.FieldValidationError).isTrue
      result as ValidatableActionResult.FieldValidationError
      assertThat(result.validationMessages).containsEntry("$.crn", "userPermission")
    }

    @Test
    fun `returns Success with created Application`() {
      val crn = "CRN345"
      val username = "SOMEPERSON"

      val cas2v2ApplicationSchema = Cas2v2ApplicationJsonSchemaEntityFactory().produce()

      val user = userWithUsername(username)

      every { mockOffenderService.getOffenderByCrn(crn) } returns AuthorisableActionResult.Success(
        OffenderDetailsSummaryFactory().produce(),
      )

      every { mockJsonSchemaService.getNewestSchema(Cas2v2ApplicationJsonSchemaEntity::class.java) } returns cas2v2ApplicationSchema
      every { mockCas2v2ApplicationRepository.save(any()) } answers {
        it.invocation.args[0] as
          Cas2v2ApplicationEntity
      }

      val result = cas2v2ApplicationService.createCas2v2Application(crn, user)

      assertThat(result is ValidatableActionResult.Success).isTrue
      result as ValidatableActionResult.Success
      assertThat(result.entity.crn).isEqualTo(crn)
//      assertThat(result.entity.createdByUser).isEqualTo(user)
    }
  }

  @Nested
  inner class UpdateApplication {
    val user = NomisUserEntityFactory().produce()

    @Test
    fun `returns NotFound when cas2v2 application doesn't exist`() {
      val applicationId = UUID.fromString("fa6e97ce-7b9e-473c-883c-83b1c2af773d")

      every { mockCas2v2ApplicationRepository.findByIdOrNull(applicationId) } returns null

      assertThat(
        cas2v2ApplicationService.updateCas2v2Application(
          applicationId = applicationId,
          data = "{}",
          user = user,
        ) is AuthorisableActionResult.NotFound,
      ).isTrue
    }

    @Test
    fun `returns Unauthorised when application doesn't belong to request user`() {
      val applicationId = UUID.fromString("fa6e97ce-7b9e-473c-883c-83b1c2af773d")

      val cas2v2Application = Cas2v2ApplicationEntityFactory()
        .withId(applicationId)
        .withYieldedCreatedByUser {
          NomisUserEntityFactory()
            .produce()
        }
        .produce()

      every { mockCas2v2ApplicationRepository.findByIdOrNull(applicationId) } returns
        cas2v2Application
      every { mockJsonSchemaService.checkCas2v2SchemaOutdated(cas2v2Application) } returns
        cas2v2Application

      assertThat(
        cas2v2ApplicationService.updateCas2v2Application(
          applicationId = applicationId,
          data = "{}",
          user = user,
        ) is AuthorisableActionResult.Unauthorised,
      ).isTrue
    }

    @Test
    fun `returns GeneralValidationError when cas2v2 application has already been submitted`() {
      val applicationId = UUID.fromString("fa6e97ce-7b9e-473c-883c-83b1c2af773d")

      val newestSchema = Cas2v2ApplicationJsonSchemaEntityFactory().produce()

      val cas2v2Application = Cas2v2ApplicationEntityFactory()
        .withApplicationSchema(newestSchema)
        .withId(applicationId)
        .withCreatedByUser(user)
        .withSubmittedAt(OffsetDateTime.now())
        .produce()
        .apply {
          schemaUpToDate = true
        }

      every { mockCas2v2ApplicationRepository.findByIdOrNull(applicationId) } returns
        cas2v2Application
      every { mockJsonSchemaService.checkCas2v2SchemaOutdated(cas2v2Application) } returns
        cas2v2Application

      val result = cas2v2ApplicationService.updateCas2v2Application(
        applicationId = applicationId,
        data = "{}",
        user = user,
      )

      assertThat(result is AuthorisableActionResult.Success).isTrue
      result as AuthorisableActionResult.Success

      assertThat(result.entity is ValidatableActionResult.GeneralValidationError).isTrue
      val validatableActionResult = result.entity as ValidatableActionResult.GeneralValidationError

      assertThat(validatableActionResult.message).isEqualTo("This application has already been submitted")
    }

    @Test
    fun `returns GeneralValidationError when application has been abandoned`() {
      val applicationId = UUID.fromString("fa6e97ce-7b9e-473c-883c-83b1c2af773d")

      val newestSchema = Cas2v2ApplicationJsonSchemaEntityFactory().produce()

      val cas2v2Application = Cas2v2ApplicationEntityFactory()
        .withApplicationSchema(newestSchema)
        .withId(applicationId)
        .withCreatedByUser(user)
        .withAbandonedAt(OffsetDateTime.now())
        .produce()
        .apply {
          schemaUpToDate = true
        }

      every { mockCas2v2ApplicationRepository.findByIdOrNull(applicationId) } returns cas2v2Application
      every { mockJsonSchemaService.checkCas2v2SchemaOutdated(cas2v2Application) } returns cas2v2Application

      val result = cas2v2ApplicationService.updateCas2v2Application(
        applicationId = applicationId,
        data = "{}",
        user = user,
      )

      assertThat(result is AuthorisableActionResult.Success).isTrue
      result as AuthorisableActionResult.Success

      assertThat(result.entity is ValidatableActionResult.GeneralValidationError).isTrue
      val validatableActionResult = result.entity as ValidatableActionResult.GeneralValidationError

      assertThat(validatableActionResult.message).isEqualTo("This application has been abandoned")
    }

    @Test
    fun `returns GeneralValidationError when application schema is outdated`() {
      val applicationId = UUID.fromString("fa6e97ce-7b9e-473c-883c-83b1c2af773d")

      val cas2v2Application = Cas2v2ApplicationEntityFactory()
        .withId(applicationId)
        .withCreatedByUser(user)
        .withSubmittedAt(null)
        .produce()
        .apply {
          schemaUpToDate = false
        }

      every { mockCas2v2ApplicationRepository.findByIdOrNull(applicationId) } returns cas2v2Application
      every { mockJsonSchemaService.checkCas2v2SchemaOutdated(cas2v2Application) } returns cas2v2Application

      val result = cas2v2ApplicationService.updateCas2v2Application(
        applicationId = applicationId,
        data = "{}",
        user = user,
      )

      assertThat(result is AuthorisableActionResult.Success).isTrue
      result as AuthorisableActionResult.Success

      assertThat(result.entity is ValidatableActionResult.GeneralValidationError).isTrue
      val validatableActionResult = result.entity as ValidatableActionResult.GeneralValidationError

      assertThat(validatableActionResult.message).isEqualTo("The schema version is outdated")
    }

    @ParameterizedTest
    @ValueSource(strings = ["<", "＜", "〈", "〈", ">", "＞", "〉", "〉", "<＜〈〈>＞〉〉"])
    fun `returns Success when an application, that contains removed malicious characters, is updated`(str: String) {
      val applicationId = UUID.fromString("dced02b1-8e3b-4ea5-bf99-1fba0ca1b87c")

      val newestSchema = Cas2v2ApplicationJsonSchemaEntityFactory().produce()
      val updatedData = """
      {
        "aProperty": "val${str}ue"
      }
    """

      val application = Cas2v2ApplicationEntityFactory()
        .withApplicationSchema(newestSchema)
        .withId(applicationId)
        .withCreatedByUser(user)
        .produce()
        .apply {
          schemaUpToDate = true
        }

      every { mockCas2v2ApplicationRepository.findByIdOrNull(applicationId) } returns
        application
      every { mockJsonSchemaService.checkCas2v2SchemaOutdated(application) } returns
        application
      every {
        mockJsonSchemaService.getNewestSchema(
          Cas2v2ApplicationJsonSchemaEntity::class
            .java,
        )
      } returns newestSchema
      every { mockJsonSchemaService.validate(newestSchema, updatedData) } returns true
      every { mockCas2v2ApplicationRepository.save(any()) } answers {
        it.invocation.args[0]
          as Cas2v2ApplicationEntity
      }

      val result = cas2v2ApplicationService.updateCas2v2Application(
        applicationId = applicationId,
        data = updatedData,
        user = user,
      )

      assertThat(result is AuthorisableActionResult.Success).isTrue
      result as AuthorisableActionResult.Success

      assertThat(result.entity is ValidatableActionResult.Success).isTrue
      val validatableActionResult = result.entity as ValidatableActionResult.Success

      val cas2v2Application = validatableActionResult.entity

      assertThat(cas2v2Application.data).isEqualTo(
        """
      {
        "aProperty": "value"
      }
    """,
      )
    }

    @Test
    fun `returns Success with updated Application`() {
      val applicationId = UUID.fromString("fa6e97ce-7b9e-473c-883c-83b1c2af773d")

      val newestSchema = Cas2v2ApplicationJsonSchemaEntityFactory().produce()
      val updatedData = """
      {
        "aProperty": "value"
      }
    """

      val application = Cas2v2ApplicationEntityFactory()
        .withApplicationSchema(newestSchema)
        .withId(applicationId)
        .withCreatedByUser(user)
        .produce()
        .apply {
          schemaUpToDate = true
        }

      every { mockCas2v2ApplicationRepository.findByIdOrNull(applicationId) } returns
        application
      every { mockJsonSchemaService.checkCas2v2SchemaOutdated(application) } returns
        application
      every {
        mockJsonSchemaService.getNewestSchema(
          Cas2v2ApplicationJsonSchemaEntity::class
            .java,
        )
      } returns newestSchema
      every { mockJsonSchemaService.validate(newestSchema, updatedData) } returns true
      every { mockCas2v2ApplicationRepository.save(any()) } answers {
        it.invocation.args[0]
          as Cas2v2ApplicationEntity
      }

      val result = cas2v2ApplicationService.updateCas2v2Application(
        applicationId = applicationId,
        data = updatedData,
        user = user,
      )

      assertThat(result is AuthorisableActionResult.Success).isTrue
      result as AuthorisableActionResult.Success

      assertThat(result.entity is ValidatableActionResult.Success).isTrue
      val validatableActionResult = result.entity as ValidatableActionResult.Success

      val cas2v2Application = validatableActionResult.entity

      assertThat(cas2v2Application.data).isEqualTo(updatedData)
    }
  }

  @Nested
  inner class AbandonApplication {
    val user = NomisUserEntityFactory().produce()

    @Test
    fun `returns NotFound when application doesn't exist`() {
      val applicationId = UUID.fromString("fa6e97ce-7b9e-473c-883c-83b1c2af773d")

      every { mockCas2v2ApplicationRepository.findByIdOrNull(applicationId) } returns null

      assertThat(
        cas2v2ApplicationService.abandonCas2v2Application(
          applicationId = applicationId,
          user = user,
        ) is AuthorisableActionResult.NotFound,
      ).isTrue
    }

    @Test
    fun `returns Unauthorised when application doesn't belong to request user`() {
      val applicationId = UUID.fromString("fa6e97ce-7b9e-473c-883c-83b1c2af773d")

      val application = Cas2v2ApplicationEntityFactory()
        .withId(applicationId)
        .withYieldedCreatedByUser {
          NomisUserEntityFactory()
            .produce()
        }
        .produce()

      every { mockCas2v2ApplicationRepository.findByIdOrNull(applicationId) } returns
        application

      assertThat(
        cas2v2ApplicationService.abandonCas2v2Application(
          applicationId = applicationId,
          user = user,
        ) is AuthorisableActionResult.Unauthorised,
      ).isTrue
    }

    @Test
    fun `returns Conflict Error when application has already been submitted`() {
      val applicationId = UUID.fromString("fa6e97ce-7b9e-473c-883c-83b1c2af773d")

      val newestSchema = Cas2v2ApplicationJsonSchemaEntityFactory().produce()

      val application = Cas2v2ApplicationEntityFactory()
        .withApplicationSchema(newestSchema)
        .withId(applicationId)
        .withCreatedByUser(user)
        .withSubmittedAt(OffsetDateTime.now())
        .produce()
        .apply {
          schemaUpToDate = true
        }

      every { mockCas2v2ApplicationRepository.findByIdOrNull(applicationId) } returns
        application

      val result = cas2v2ApplicationService.abandonCas2v2Application(
        applicationId = applicationId,
        user = user,
      )

      assertThat(result is AuthorisableActionResult.Success).isTrue
      result as AuthorisableActionResult.Success

      assertThat(result.entity is ValidatableActionResult.ConflictError).isTrue
      val validatableActionResult = result.entity as ValidatableActionResult.ConflictError

      assertThat(validatableActionResult.message).isEqualTo("This application has already been submitted")
    }

    @Test
    fun `returns Success when application has already been abandoned`() {
      val applicationId = UUID.fromString("fa6e97ce-7b9e-473c-883c-83b1c2af773d")

      val newestSchema = Cas2v2ApplicationJsonSchemaEntityFactory().produce()

      val application = Cas2v2ApplicationEntityFactory()
        .withApplicationSchema(newestSchema)
        .withId(applicationId)
        .withCreatedByUser(user)
        .withAbandonedAt(OffsetDateTime.now())
        .produce()
        .apply {
          schemaUpToDate = true
        }

      every { mockCas2v2ApplicationRepository.findByIdOrNull(applicationId) } returns
        application

      val result = cas2v2ApplicationService.abandonCas2v2Application(
        applicationId = applicationId,
        user = user,
      )

      assertThat(result is AuthorisableActionResult.Success).isTrue
      result as AuthorisableActionResult.Success

      assertThat(result.entity is ValidatableActionResult.Success).isTrue
    }

    @Test
    fun `returns Success and deletes the application data`() {
      val applicationId = UUID.fromString("dced02b1-8e3b-4ea5-bf99-1fba0ca1b87c")

      val newestSchema = Cas2v2ApplicationJsonSchemaEntityFactory().produce()
      val data = """
            {
              "aProperty": "value"
            }
      """

      val application = Cas2v2ApplicationEntityFactory()
        .withApplicationSchema(newestSchema)
        .withId(applicationId)
        .withCreatedByUser(user)
        .withData(data)
        .produce()
        .apply {
          schemaUpToDate = true
        }

      every { mockCas2v2ApplicationRepository.findByIdOrNull(applicationId) } returns
        application

      every { mockCas2v2ApplicationRepository.save(any()) } answers {
        it.invocation.args[0] as Cas2v2ApplicationEntity
      }

      val result = cas2v2ApplicationService.abandonCas2v2Application(
        applicationId = applicationId,
        user = user,
      )

      assertThat(result is AuthorisableActionResult.Success).isTrue
      result as AuthorisableActionResult.Success

      assertThat(result.entity is ValidatableActionResult.Success).isTrue
      val validatableActionResult = result.entity as ValidatableActionResult.Success

      val cas2v2Application = validatableActionResult.entity

      assertThat(cas2v2Application.data).isNull()
    }
  }

  @Nested
  inner class SubmitApplication {
    val applicationId: UUID = UUID.fromString("fa6e97ce-7b9e-473c-883c-83b1c2af773d")
    val username = "SOMEPERSON"
    val user = NomisUserEntityFactory()
      .withNomisUsername(this.username)
      .produce()
    val hdcEligibilityDate = LocalDate.parse("2023-03-30")
    val conditionalReleaseDate = LocalDate.parse("2023-04-29")

    private val submitCas2Application = SubmitCas2Application(
      translatedDocument = {},
      applicationId = applicationId,
      preferredAreas = "Leeds | Bradford",
      hdcEligibilityDate = hdcEligibilityDate,
      conditionalReleaseDate = conditionalReleaseDate,
      telephoneNumber = "123",
    )

    @BeforeEach
    fun setup() {
      every { mockCas2v2LockableApplicationRepository.acquirePessimisticLock(any()) } returns Cas2v2LockableApplicationEntity(UUID.randomUUID())
      every { mockObjectMapper.writeValueAsString(submitCas2Application.translatedDocument) } returns "{}"
      every { mockDomainEventService.saveCas2ApplicationSubmittedDomainEvent(any()) } just Runs
    }

    @Test
    fun `returns NotFound when application doesn't exist`() {
      val applicationId = UUID.fromString("fa6e97ce-7b9e-473c-883c-83b1c2af773d")

      every { mockCas2v2ApplicationRepository.findByIdOrNull(applicationId) } returns null

      assertThat(cas2v2ApplicationService.submitCas2v2Application(submitCas2Application, user) is CasResult.NotFound).isTrue

      assertEmailAndAssessmentsWereNotCreated()
    }

    @Test
    fun `returns Unauthorised when application doesn't belong to request user`() {
      val differentUser = NomisUserEntityFactory()
        .produce()

      val cas2v2Application = Cas2v2ApplicationEntityFactory()
        .withId(applicationId)
        .withCreatedByUser(differentUser)
        .produce()

      every { mockCas2v2ApplicationRepository.findByIdOrNull(applicationId) } returns cas2v2Application
      every { mockJsonSchemaService.checkCas2v2SchemaOutdated(cas2v2Application) } returns
        cas2v2Application

      assertThat(cas2v2ApplicationService.submitCas2v2Application(submitCas2Application, user) is CasResult.Unauthorised).isTrue

      assertEmailAndAssessmentsWereNotCreated()
    }

    @Test
    fun `returns GeneralValidationError when application schema is outdated`() {
      val cas2v2Application = Cas2v2ApplicationEntityFactory()
        .withId(applicationId)
        .withCreatedByUser(user)
        .withSubmittedAt(null)
        .produce()
        .apply {
          schemaUpToDate = false
        }

      every {
        mockCas2v2ApplicationRepository.findByIdOrNull(applicationId)
      } returns cas2v2Application
      every { mockJsonSchemaService.checkCas2v2SchemaOutdated(cas2v2Application) } returns cas2v2Application

      val result = cas2v2ApplicationService.submitCas2v2Application(submitCas2Application, user)

      assertThat(result is CasResult.GeneralValidationError).isTrue
      val validatableActionResult = result as CasResult.GeneralValidationError

      assertThat(validatableActionResult.message).isEqualTo("The schema version is outdated")

      assertEmailAndAssessmentsWereNotCreated()
    }

    @Test
    fun `returns GeneralValidationError when application has already been submitted`() {
      val newestSchema = Cas2v2ApplicationJsonSchemaEntityFactory().produce()

      val cas2v2Application = Cas2v2ApplicationEntityFactory()
        .withApplicationSchema(newestSchema)
        .withId(applicationId)
        .withCreatedByUser(user)
        .withSubmittedAt(OffsetDateTime.now())
        .produce()
        .apply {
          schemaUpToDate = true
        }

      every {
        mockCas2v2ApplicationRepository.findByIdOrNull(applicationId)
      } returns cas2v2Application
      every { mockJsonSchemaService.checkCas2v2SchemaOutdated(cas2v2Application) } returns cas2v2Application

      val result = cas2v2ApplicationService.submitCas2v2Application(submitCas2Application, user)

      assertThat(result is CasResult.GeneralValidationError).isTrue
      val validatableActionResult = result as CasResult.GeneralValidationError

      assertThat(validatableActionResult.message).isEqualTo("This application has already been submitted")

      assertEmailAndAssessmentsWereNotCreated()
    }

    @Test
    fun `returns GeneralValidationError when application has already been abandoned`() {
      val newestSchema = Cas2v2ApplicationJsonSchemaEntityFactory().produce()

      val cas2v2Application = Cas2v2ApplicationEntityFactory()
        .withApplicationSchema(newestSchema)
        .withId(applicationId)
        .withCreatedByUser(user)
        .withAbandonedAt(OffsetDateTime.now())
        .produce()

      every {
        mockCas2v2ApplicationRepository.findByIdOrNull(applicationId)
      } returns cas2v2Application
      every { mockJsonSchemaService.checkCas2v2SchemaOutdated(cas2v2Application) } returns cas2v2Application

      val result = cas2v2ApplicationService.submitCas2v2Application(submitCas2Application, user)

      assertThat(result is CasResult.GeneralValidationError).isTrue
      val validatableActionResult = result as CasResult.GeneralValidationError

      assertThat(validatableActionResult.message).isEqualTo("This application has already been abandoned")

      assertEmailAndAssessmentsWereNotCreated()
    }

    @Test
    fun `throws a validation error if InmateDetails (for prison code) are not available`() {
      val newestSchema = Cas2v2ApplicationJsonSchemaEntityFactory().produce()

      val cas2v2Application = Cas2v2ApplicationEntityFactory()
        .withApplicationSchema(newestSchema)
        .withId(applicationId)
        .withCreatedByUser(user)
        .withSubmittedAt(null)
        .produce()
        .apply {
          schemaUpToDate = true
        }

      every {
        mockCas2v2ApplicationRepository.findByIdOrNull(any())
      } returns cas2v2Application
      every { mockJsonSchemaService.checkCas2v2SchemaOutdated(any()) } returns
        cas2v2Application
      every { mockJsonSchemaService.validate(any(), any()) } returns true

      every { mockCas2v2ApplicationRepository.save(any()) } answers {
        it.invocation.args[0]
          as Cas2v2ApplicationEntity
      }

      val offenderDetails = OffenderDetailsSummaryFactory()
        .withCrn(cas2v2Application.crn)
        .produce()

      every { mockOffenderService.getOffenderByCrn(any()) } returns AuthorisableActionResult.Success(
        offenderDetails,
      )

      // this call to the Prison API to find the referringPrisonCode when saving
      // the application.submitted domain event *should* never 404 or otherwise fail,
      // as when creating  the application initially a similar call was made.
      // If there is a problem with accessing the Prison API, we fail hard and
      // abort our attempt to submit the application.
      every {
        mockOffenderService.getInmateDetailByNomsNumber(any(), any())
      } returns AuthorisableActionResult.NotFound()

      assertGeneralValidationError("Inmate Detail not found")

      assertEmailAndAssessmentsWereNotCreated()
    }

    @Test
    fun `throws an UpstreamApiException if prison code is null`() {
      val newestSchema = Cas2v2ApplicationJsonSchemaEntityFactory().produce()

      val cas2v2Application = Cas2v2ApplicationEntityFactory()
        .withApplicationSchema(newestSchema)
        .withId(applicationId)
        .withCreatedByUser(user)
        .withSubmittedAt(null)
        .produce()
        .apply {
          schemaUpToDate = true
        }

      every {
        mockCas2v2ApplicationRepository.findByIdOrNull(any())
      } returns cas2v2Application
      every { mockJsonSchemaService.checkCas2v2SchemaOutdated(any()) } returns
        cas2v2Application
      every { mockJsonSchemaService.validate(any(), any()) } returns true

      every { mockCas2v2ApplicationRepository.save(any()) } answers {
        it.invocation.args[0]
          as Cas2v2ApplicationEntity
      }

      val offenderDetails = OffenderDetailsSummaryFactory()
        .withCrn(cas2v2Application.crn)
        .produce()

      every { mockOffenderService.getOffenderByCrn(any()) } returns AuthorisableActionResult.Success(
        offenderDetails,
      )

      // this call to the Prison API to find the referringPrisonCode when saving
      // the application.submitted domain event *should* always have a prison code,
      // but we need to account for possibility it may be missing.
      // If there is a problem with accessing the Prison API, we fail hard and
      // abort our attempt to submit the application and return a validation message.
      every {
        mockOffenderService.getInmateDetailByNomsNumber(any(), any())
      } returns AuthorisableActionResult.Success(InmateDetailFactory().produce())

      assertGeneralValidationError("No prison code available")

      assertEmailAndAssessmentsWereNotCreated()
    }

    private fun assertGeneralValidationError(message: String) {
      val result = cas2v2ApplicationService.submitCas2v2Application(submitCas2Application, user)
      assertThat(result is CasResult.GeneralValidationError).isTrue
      val error = result as CasResult.GeneralValidationError

      assertThat(error.message).isEqualTo(message)
    }

    private fun assertEmailAndAssessmentsWereNotCreated() {
      verify(exactly = 0) { mockEmailNotificationService.sendEmail(any(), any(), any()) }
      verify(exactly = 0) { mockCas2v2AssessmentService.createCas2v2Assessment(any()) }
    }

    @Test
    fun `returns Success and stores event`() {
      val newestSchema = Cas2v2ApplicationJsonSchemaEntityFactory().produce()

      val cas2v2Application = Cas2v2ApplicationEntityFactory()
        .withApplicationSchema(newestSchema)
        .withId(applicationId)
        .withCreatedByUser(user)
        .withSubmittedAt(null)
        .produce()
        .apply {
          schemaUpToDate = true
        }

      every {
        mockCas2v2ApplicationRepository.findByIdOrNull(applicationId)
      } returns cas2v2Application
      every { mockJsonSchemaService.checkCas2v2SchemaOutdated(cas2v2Application) } returns
        cas2v2Application
      every { mockJsonSchemaService.validate(newestSchema, cas2v2Application.data!!) } returns true

      val inmateDetail = InmateDetailFactory()
        .withAssignedLivingUnit(
          AssignedLivingUnit(
            agencyId = "BRI",
            locationId = 1234,
            description = "description",
            agencyName = "HMP Bristol",
          ),
        )
        .produce()

      every {
        mockOffenderService.getInmateDetailByNomsNumber(
          cas2v2Application.crn,
          cas2v2Application.nomsNumber.toString(),
        )
      } returns AuthorisableActionResult.Success(inmateDetail)

      every { mockNotifyConfig.templates.cas2ApplicationSubmitted } returns "abc123"
      every { mockNotifyConfig.emailAddresses.cas2Assessors } returns "exampleAssessorInbox@example.com"
      every { mockNotifyConfig.emailAddresses.cas2ReplyToId } returns "def456"
      every { mockEmailNotificationService.sendEmail(any(), any(), any(), any()) } just Runs

      every { mockCas2v2ApplicationRepository.save(any()) } answers {
        it.invocation.args[0]
          as Cas2v2ApplicationEntity
      }

      val offenderDetails = OffenderDetailsSummaryFactory()
        .withGender("male")
        .withCrn(cas2v2Application.crn)
        .produce()

      every { mockOffenderService.getOffenderByCrn(cas2v2Application.crn) } returns AuthorisableActionResult.Success(
        offenderDetails,
      )

      every { mockCas2v2AssessmentService.createCas2v2Assessment(any()) } returns any()

      val result = cas2v2ApplicationService.submitCas2v2Application(submitCas2Application, user)

      assertThat(result is CasResult.Success).isTrue
      result as CasResult.Success

      assertThat(true).isTrue
      val persistedApplication = extractEntityFromCasResult(result)

      assertThat(persistedApplication.crn).isEqualTo(cas2v2Application.crn)
      assertThat(persistedApplication.preferredAreas).isEqualTo("Leeds | Bradford")
      assertThat(persistedApplication.hdcEligibilityDate).isEqualTo(hdcEligibilityDate)
      assertThat(persistedApplication.conditionalReleaseDate).isEqualTo(conditionalReleaseDate)

      verify { mockCas2v2ApplicationRepository.save(any()) }

      verify(exactly = 1) {
        mockDomainEventService.saveCas2ApplicationSubmittedDomainEvent(
          match {
            val data = it.data.eventDetails

            it.applicationId == cas2v2Application.id &&
              data.personReference.noms == cas2v2Application.nomsNumber &&
              data.personReference.crn == cas2v2Application.crn &&
              data.applicationUrl == "http://frontend/applications/${cas2v2Application.id}" &&
              data.submittedBy.staffMember.username == username &&
              data.referringPrisonCode == "BRI" &&
              data.preferredAreas == "Leeds | Bradford" &&
              data.hdcEligibilityDate == hdcEligibilityDate &&
              data.conditionalReleaseDate == conditionalReleaseDate
          },
        )
      }

      verify(exactly = 1) {
        mockEmailNotificationService.sendEmail(
          "exampleAssessorInbox@example.com",
          "abc123",
          match {
            it["name"] == user.name &&
              it["email"] == user.email &&
              it["prisonNumber"] == cas2v2Application.nomsNumber &&
              it["telephoneNumber"] == cas2v2Application.telephoneNumber &&
              it["applicationUrl"] == "http://frontend/assess/applications/$applicationId/overview"
          },
          "def456",
        )
      }

      verify(exactly = 1) { mockCas2v2AssessmentService.createCas2v2Assessment(persistedApplication) }
    }
  }

  private fun userWithUsername(username: String) = NomisUserEntityFactory()
    .withNomisUsername(username)
    .produce()
}
