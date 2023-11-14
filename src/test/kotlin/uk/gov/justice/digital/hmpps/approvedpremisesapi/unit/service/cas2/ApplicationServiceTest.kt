package uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.service.cas2

import com.fasterxml.jackson.databind.ObjectMapper
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.data.repository.findByIdOrNull
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas2.model.Cas2ApplicationSubmittedEvent
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.SubmitCas2Application
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ApAreaEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.Cas2ApplicationEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.Cas2ApplicationJsonSchemaEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.NomisUserEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.OffenderDetailsSummaryFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ProbationRegionEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.Cas2ApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.Cas2ApplicationJsonSchemaEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.Cas2ApplicationRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.NomisUserRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.results.AuthorisableActionResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.results.ValidatableActionResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.NomisUserService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas2.DomainEventService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas2.JsonSchemaService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas2.OffenderService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas2.UserAccessService
import java.time.OffsetDateTime
import java.util.UUID

class ApplicationServiceTest {
  private val mockUserRepository = mockk<NomisUserRepository>()
  private val mockApplicationRepository = mockk<Cas2ApplicationRepository>()
  private val mockJsonSchemaService = mockk<JsonSchemaService>()
  private val mockOffenderService = mockk<OffenderService>()
  private val mockUserService = mockk<NomisUserService>()
  private val mockUserAccessService = mockk<UserAccessService>()
  private val mockDomainEventService = mockk<DomainEventService>()
  private val mockObjectMapper = mockk<ObjectMapper>()

  private val applicationService = uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas2.ApplicationService(
    mockUserRepository,
    mockApplicationRepository,
    mockJsonSchemaService,
    mockOffenderService,
    mockUserService,
    mockUserAccessService,
    mockDomainEventService,
    mockObjectMapper,
    "http://frontend/applications/#id",
  )

  @Nested
  inner class GetApplicationForUsername {
    @Test
    fun `where application does not exist returns NotFound result`() {
      val distinguishedName = "SOMEPERSON"
      val applicationId = UUID.fromString("c1750938-19fc-48a1-9ae9-f2e119ffc1f4")

      every { mockApplicationRepository.findByIdOrNull(applicationId) } returns null

      assertThat(applicationService.getApplicationForUsername(applicationId, distinguishedName) is AuthorisableActionResult.NotFound).isTrue
    }

    @Test
    fun `where user cannot access the application returns Unauthorised result`() {
      val distinguishedName = "SOMEPERSON"
      val applicationId = UUID.fromString("c1750938-19fc-48a1-9ae9-f2e119ffc1f4")

      every { mockUserRepository.findByNomisUsername(any()) } returns NomisUserEntityFactory()
        .withNomisUsername(distinguishedName)
        .produce()

      every { mockApplicationRepository.findByIdOrNull(any()) } returns
        Cas2ApplicationEntityFactory()
          .withCreatedByUser(
            NomisUserEntityFactory()
              .produce(),
          )
          .produce()

      every { mockUserAccessService.userCanViewApplication(any(), any()) } returns false

      assertThat(applicationService.getApplicationForUsername(applicationId, distinguishedName) is AuthorisableActionResult.Unauthorised).isTrue
    }

    @Test
    fun `where user can access the application returns Success result with entity from db`() {
      val distinguishedName = "SOMEPERSON"
      val userId = UUID.fromString("239b5e41-f83e-409e-8fc0-8f1e058d417e")
      val applicationId = UUID.fromString("c1750938-19fc-48a1-9ae9-f2e119ffc1f4")

      val newestJsonSchema = Cas2ApplicationJsonSchemaEntityFactory()
        .withSchema("{}")
        .produce()

      val userEntity = NomisUserEntityFactory()
        .withId(userId)
        .withNomisUsername(distinguishedName)
        .produce()

      val applicationEntity = Cas2ApplicationEntityFactory()
        .withCreatedByUser(userEntity)
        .withApplicationSchema(newestJsonSchema)
        .produce()

      every { mockJsonSchemaService.checkSchemaOutdated(any()) } answers {
        it.invocation
          .args[0] as Cas2ApplicationEntity
      }
      every { mockApplicationRepository.findByIdOrNull(any()) } returns applicationEntity
      every { mockUserRepository.findByNomisUsername(any()) } returns userEntity
      every { mockUserAccessService.userCanViewApplication(any(), any()) } returns true

      val result = applicationService.getApplicationForUsername(applicationId, distinguishedName)

      assertThat(result is AuthorisableActionResult.Success).isTrue
      result as AuthorisableActionResult.Success

      assertThat(result.entity).isEqualTo(applicationEntity)
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

      val result = applicationService.createApplication(crn, user, "jwt")

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

      val result = applicationService.createApplication(crn, user, "jwt")

      assertThat(result is ValidatableActionResult.FieldValidationError).isTrue
      result as ValidatableActionResult.FieldValidationError
      assertThat(result.validationMessages).containsEntry("$.crn", "userPermission")
    }

    @Test
    fun `returns Success with created Application`() {
      val crn = "CRN345"
      val username = "SOMEPERSON"

      val schema = Cas2ApplicationJsonSchemaEntityFactory().produce()

      val user = userWithUsername(username)

      every { mockOffenderService.getOffenderByCrn(crn) } returns AuthorisableActionResult.Success(
        OffenderDetailsSummaryFactory().produce(),
      )

      every { mockJsonSchemaService.getNewestSchema(Cas2ApplicationJsonSchemaEntity::class.java) } returns schema
      every { mockApplicationRepository.save(any()) } answers {
        it.invocation.args[0] as
          Cas2ApplicationEntity
      }

      val result = applicationService.createApplication(crn, user, "jwt")

      assertThat(result is ValidatableActionResult.Success).isTrue
      result as ValidatableActionResult.Success
      assertThat(result.entity.crn).isEqualTo(crn)
      assertThat(result.entity.createdByUser).isEqualTo(user)
    }
  }

  @Nested
  inner class UpdateApplication {
    @Test
    fun `returns NotFound when application doesn't exist`() {
      val applicationId = UUID.fromString("fa6e97ce-7b9e-473c-883c-83b1c2af773d")
      val username = "SOMEPERSON"

      every { mockApplicationRepository.findByIdOrNull(applicationId) } returns null

      assertThat(
        applicationService.updateApplication(
          applicationId = applicationId,
          data = "{}",
          username = username,
        ) is AuthorisableActionResult.NotFound,
      ).isTrue
    }

    @Test
    fun `returns Unauthorised when application doesn't belong to request user`() {
      val applicationId = UUID.fromString("fa6e97ce-7b9e-473c-883c-83b1c2af773d")
      val username = "SOMEPERSON"

      val probationRegion = ProbationRegionEntityFactory()
        .withYieldedApArea { ApAreaEntityFactory().produce() }
        .produce()
      val application = Cas2ApplicationEntityFactory()
        .withId(applicationId)
        .withYieldedCreatedByUser {
          NomisUserEntityFactory()
            .produce()
        }
        .produce()

      every { mockUserService.getUserForRequest() } returns NomisUserEntityFactory()
        .withNomisUsername(username)
        .produce()
      every { mockApplicationRepository.findByIdOrNull(applicationId) } returns
        application
      every { mockJsonSchemaService.checkSchemaOutdated(application) } returns
        application

      assertThat(
        applicationService.updateApplication(
          applicationId = applicationId,
          data = "{}",
          username = username,
        ) is AuthorisableActionResult.Unauthorised,
      ).isTrue
    }

    @Test
    fun `returns GeneralValidationError when application schema is outdated`() {
      val applicationId = UUID.fromString("fa6e97ce-7b9e-473c-883c-83b1c2af773d")
      val username = "SOMEPERSON"

      val user = NomisUserEntityFactory()
        .withNomisUsername(username)
        .produce()

      val application = Cas2ApplicationEntityFactory()
        .withId(applicationId)
        .withCreatedByUser(user)
        .withSubmittedAt(null)
        .produce()
        .apply {
          schemaUpToDate = false
        }

      every { mockUserService.getUserForRequest() } returns user
      every { mockApplicationRepository.findByIdOrNull(applicationId) } returns
        application
      every { mockJsonSchemaService.checkSchemaOutdated(application) } returns
        application

      val result = applicationService.updateApplication(
        applicationId = applicationId,
        data = "{}",
        username = username,
      )

      assertThat(result is AuthorisableActionResult.Success).isTrue
      result as AuthorisableActionResult.Success

      assertThat(result.entity is ValidatableActionResult.GeneralValidationError).isTrue
      val validatableActionResult = result.entity as ValidatableActionResult.GeneralValidationError

      assertThat(validatableActionResult.message).isEqualTo("The schema version is outdated")
    }

    @Test
    fun `returns GeneralValidationError when application has already been submitted`() {
      val applicationId = UUID.fromString("fa6e97ce-7b9e-473c-883c-83b1c2af773d")
      val username = "SOMEPERSON"

      val newestSchema = Cas2ApplicationJsonSchemaEntityFactory().produce()

      val user = NomisUserEntityFactory()
        .withNomisUsername(username)
        .produce()

      val application = Cas2ApplicationEntityFactory()
        .withApplicationSchema(newestSchema)
        .withId(applicationId)
        .withCreatedByUser(user)
        .withSubmittedAt(OffsetDateTime.now())
        .produce()
        .apply {
          schemaUpToDate = true
        }

      every { mockUserService.getUserForRequest() } returns user
      every { mockApplicationRepository.findByIdOrNull(applicationId) } returns
        application
      every { mockJsonSchemaService.checkSchemaOutdated(application) } returns
        application

      val result = applicationService.updateApplication(
        applicationId = applicationId,
        data = "{}",
        username = username,
      )

      assertThat(result is AuthorisableActionResult.Success).isTrue
      result as AuthorisableActionResult.Success

      assertThat(result.entity is ValidatableActionResult.GeneralValidationError).isTrue
      val validatableActionResult = result.entity as ValidatableActionResult.GeneralValidationError

      assertThat(validatableActionResult.message).isEqualTo("This application has already been submitted")
    }

    @Test
    fun `returns Success with updated Application`() {
      val applicationId = UUID.fromString("fa6e97ce-7b9e-473c-883c-83b1c2af773d")
      val username = "SOMEPERSON"

      val user = NomisUserEntityFactory()
        .withNomisUsername(username)
        .produce()

      val newestSchema = Cas2ApplicationJsonSchemaEntityFactory().produce()
      val updatedData = """
      {
        "aProperty": "value"
      }
    """

      val application = Cas2ApplicationEntityFactory()
        .withApplicationSchema(newestSchema)
        .withId(applicationId)
        .withCreatedByUser(user)
        .produce()
        .apply {
          schemaUpToDate = true
        }

      every { mockUserService.getUserForRequest() } returns user
      every { mockApplicationRepository.findByIdOrNull(applicationId) } returns
        application
      every { mockJsonSchemaService.checkSchemaOutdated(application) } returns
        application
      every {
        mockJsonSchemaService.getNewestSchema(
          Cas2ApplicationJsonSchemaEntity::class
            .java,
        )
      } returns newestSchema
      every { mockJsonSchemaService.validate(newestSchema, updatedData) } returns true
      every { mockApplicationRepository.save(any()) } answers {
        it.invocation.args[0]
          as Cas2ApplicationEntity
      }

      val result = applicationService.updateApplication(
        applicationId = applicationId,
        data = updatedData,
        username = username,
      )

      assertThat(result is AuthorisableActionResult.Success).isTrue
      result as AuthorisableActionResult.Success

      assertThat(result.entity is ValidatableActionResult.Success).isTrue
      val validatableActionResult = result.entity as ValidatableActionResult.Success

      val cas2Application = validatableActionResult.entity

      assertThat(cas2Application.data).isEqualTo(updatedData)
    }
  }

  @Nested
  inner class SubmitApplication {
    val applicationId: UUID = UUID.fromString("fa6e97ce-7b9e-473c-883c-83b1c2af773d")
    val username = "SOMEPERSON"
    val user = NomisUserEntityFactory()
      .withNomisUsername(this.username)
      .produce()

    private val submitCas2Application = SubmitCas2Application(
      translatedDocument = {},
      applicationId = applicationId,
    )

    @BeforeEach
    fun setup() {
      every { mockObjectMapper.writeValueAsString(submitCas2Application.translatedDocument) } returns "{}"
      every { mockDomainEventService.saveCas2ApplicationSubmittedDomainEvent(any()) } just Runs
    }

    @Test
    fun `returns NotFound when application doesn't exist`() {
      val applicationId = UUID.fromString("fa6e97ce-7b9e-473c-883c-83b1c2af773d")

      every { mockApplicationRepository.findByIdOrNullWithWriteLock(applicationId) } returns null

      assertThat(applicationService.submitApplication(submitCas2Application) is AuthorisableActionResult.NotFound).isTrue
    }

    @Test
    fun `returns Unauthorised when application doesn't belong to request user`() {
      val user = NomisUserEntityFactory()
        .produce()

      val application = Cas2ApplicationEntityFactory()
        .withId(applicationId)
        .withCreatedByUser(user)
        .produce()

      every { mockUserService.getUserForRequest() } returns NomisUserEntityFactory()
        .withNomisUsername(username)
        .produce()
      every { mockApplicationRepository.findByIdOrNullWithWriteLock(applicationId) } returns application
      every { mockJsonSchemaService.checkSchemaOutdated(application) } returns
        application

      assertThat(applicationService.submitApplication(submitCas2Application) is AuthorisableActionResult.Unauthorised).isTrue
    }

    @Test
    fun `returns GeneralValidationError when application schema is outdated`() {
      val application = Cas2ApplicationEntityFactory()
        .withId(applicationId)
        .withCreatedByUser(user)
        .withSubmittedAt(null)
        .produce()
        .apply {
          schemaUpToDate = false
        }

      every { mockUserService.getUserForRequest() } returns user
      every {
        mockApplicationRepository.findByIdOrNullWithWriteLock(applicationId)
      } returns application
      every { mockJsonSchemaService.checkSchemaOutdated(application) } returns application

      val result = applicationService.submitApplication(submitCas2Application)

      assertThat(result is AuthorisableActionResult.Success).isTrue
      result as AuthorisableActionResult.Success

      assertThat(result.entity is ValidatableActionResult.GeneralValidationError).isTrue
      val validatableActionResult = result.entity as ValidatableActionResult.GeneralValidationError

      assertThat(validatableActionResult.message).isEqualTo("The schema version is outdated")
    }

    @Test
    fun `returns GeneralValidationError when application has already been submitted`() {
      val newestSchema = Cas2ApplicationJsonSchemaEntityFactory().produce()

      val application = Cas2ApplicationEntityFactory()
        .withApplicationSchema(newestSchema)
        .withId(applicationId)
        .withCreatedByUser(user)
        .withSubmittedAt(OffsetDateTime.now())
        .produce()
        .apply {
          schemaUpToDate = true
        }

      every { mockUserService.getUserForRequest() } returns user
      every {
        mockApplicationRepository.findByIdOrNullWithWriteLock(applicationId)
      } returns application
      every { mockJsonSchemaService.checkSchemaOutdated(application) } returns application

      val result = applicationService.submitApplication(submitCas2Application)

      assertThat(result is AuthorisableActionResult.Success).isTrue
      result as AuthorisableActionResult.Success

      assertThat(result.entity is ValidatableActionResult.GeneralValidationError).isTrue
      val validatableActionResult = result.entity as ValidatableActionResult.GeneralValidationError

      assertThat(validatableActionResult.message).isEqualTo("This application has already been submitted")
    }

    @Test
    fun `returns Success and stores event`() {
      val newestSchema = Cas2ApplicationJsonSchemaEntityFactory().produce()

      val application = Cas2ApplicationEntityFactory()
        .withApplicationSchema(newestSchema)
        .withId(applicationId)
        .withCreatedByUser(user)
        .withSubmittedAt(null)
        .produce()
        .apply {
          schemaUpToDate = true
        }

      every { mockUserService.getUserForRequest() } returns user
      every {
        mockApplicationRepository.findByIdOrNullWithWriteLock(applicationId)
      } returns application
      every { mockJsonSchemaService.checkSchemaOutdated(application) } returns
        application
      every { mockJsonSchemaService.validate(newestSchema, application.data!!) } returns true
      every { mockApplicationRepository.save(any()) } answers {
        it.invocation.args[0]
          as Cas2ApplicationEntity
      }

      val offenderDetails = OffenderDetailsSummaryFactory()
        .withGender("male")
        .withCrn(application.crn)
        .produce()

      every { mockOffenderService.getOffenderByCrn(application.crn) } returns AuthorisableActionResult.Success(
        offenderDetails,
      )

      val _schema = application.schemaVersion as Cas2ApplicationJsonSchemaEntity

      val result = applicationService.submitApplication(submitCas2Application)

      assertThat(result is AuthorisableActionResult.Success).isTrue
      result as AuthorisableActionResult.Success

      assertThat(result.entity is ValidatableActionResult.Success).isTrue
      val validatableActionResult = result.entity as ValidatableActionResult.Success
      val persistedApplication = validatableActionResult.entity
      assertThat(persistedApplication.crn).isEqualTo(application.crn)

      verify { mockApplicationRepository.save(any()) }

      verify(exactly = 1) {
        mockDomainEventService.saveCas2ApplicationSubmittedDomainEvent(
          match {
            val data = (it.data as Cas2ApplicationSubmittedEvent).eventDetails

            it.applicationId == application.id &&
              data.personReference.noms == application.nomsNumber &&
              data.applicationUrl == "http://frontend/applications/${application.id}" &&
              data.submittedBy.staffMember.username == username
          },
        )
      }
    }
  }

  private fun userWithUsername(username: String) = NomisUserEntityFactory()
    .withNomisUsername(username)
    .produce()
}
