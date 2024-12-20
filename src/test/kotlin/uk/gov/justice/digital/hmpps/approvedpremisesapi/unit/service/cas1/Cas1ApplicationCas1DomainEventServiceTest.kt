package uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.service.cas1

import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.http.HttpStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model.ApplicationSubmittedSubmittedBy
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model.Ldu
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model.PersonReference
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model.Region
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model.Team
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ApType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1ApplicationUserDetails
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ReleaseTypeOption
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.SentenceTypeOption
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.SituationOption
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.SubmitApprovedPremisesApplication
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.ApDeliusContextApiClient
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.ClientResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ApAreaEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ApprovedPremisesApplicationEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ApprovedPremisesApplicationJsonSchemaEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.CaseDetailFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.OffenderDetailsSummaryFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.PersonRisksFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ProbationRegionEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.StaffDetailFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.UserEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.events.ProbationAreaFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.events.WithdrawnByFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApprovedPremisesApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.MetaDataName
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.ApprovedPremisesType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.Mappa
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.RiskStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.RiskWithStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.asApiType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.results.AuthorisableActionResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.OffenderService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.Cas1ApplicationDomainEventService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.Cas1DomainEventService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.DomainEventTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.UrlTemplate
import java.time.Clock
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.Period
import java.util.UUID

class Cas1ApplicationCas1DomainEventServiceTest {
  private val mockOffenderService = mockk<OffenderService>()
  private val mockDomainEventService = mockk<Cas1DomainEventService>()
  private val mockApDeliusContextApiClient = mockk<ApDeliusContextApiClient>()
  private val mockDomainEventTransformer = mockk<DomainEventTransformer>()

  private val service = Cas1ApplicationDomainEventService(
    mockDomainEventService,
    mockOffenderService,
    mockApDeliusContextApiClient,
    mockDomainEventTransformer,
    UrlTemplate("http://frontend/applications/#id"),
    Clock.systemDefaultZone(),
  )

  val applicationId: UUID = UUID.fromString("fa6e97ce-7b9e-473c-883c-83b1c2af773d")
  val username = "SOMEPERSON"
  val user = UserEntityFactory()
    .withDeliusUsername(this.username)
    .withYieldedProbationRegion {
      ProbationRegionEntityFactory()
        .withYieldedApArea { ApAreaEntityFactory().produce() }
        .produce()
    }
    .produce()

  private val newestSchema = ApprovedPremisesApplicationJsonSchemaEntityFactory().produce()

  @Nested
  inner class ApplicationSubmitted {

    private lateinit var application: ApprovedPremisesApplicationEntity
    private val caseDetails = CaseDetailFactory().produce()
    private val domainEventProbationArea = ProbationAreaFactory().produce()

    private val staffUserDetails = StaffDetailFactory.staffDetail()

    @BeforeEach
    fun setup() {
      application = ApprovedPremisesApplicationEntityFactory()
        .withApplicationSchema(newestSchema)
        .withId(applicationId)
        .withCrn("THECRN")
        .withCreatedByUser(user)
        .withSubmittedAt(null)
        .produce()
        .apply {
          schemaUpToDate = true
        }

      every {
        mockOffenderService.getOffenderByCrn(
          application.crn,
          user.deliusUsername,
          true,
        )
      } returns AuthorisableActionResult.Success(
        OffenderDetailsSummaryFactory()
          .withGender("male")
          .withCrn("THECRN")
          .withNomsNumber("THENOMS")
          .withDateOfBirth(LocalDate.of(1982, 3, 11))
          .produce(),
      )

      val risks = PersonRisksFactory()
        .withMappa(
          RiskWithStatus(
            status = RiskStatus.Retrieved,
            value = Mappa(
              level = "CAT C1/LEVEL L1",
              lastUpdated = LocalDate.now(),
            ),
          ),
        )
        .produce()

      every {
        mockOffenderService.getRiskByCrn(
          application.crn,
          user.deliusUsername,
        )
      } returns AuthorisableActionResult.Success(
        risks,
      )

      every { mockApDeliusContextApiClient.getCaseDetail(application.crn) } returns ClientResult.Success(
        status = HttpStatus.OK,
        body = caseDetails,
      )

      every { mockDomainEventTransformer.toProbationArea(staffUserDetails) } returns domainEventProbationArea

      every { mockDomainEventService.saveApplicationSubmittedDomainEvent(any()) } just Runs

      every { mockApDeliusContextApiClient.getStaffDetail(user.deliusUsername) } returns ClientResult.Success(
        HttpStatus.OK,
        staffUserDetails,
      )
    }

    @SuppressWarnings("CyclomaticComplexMethod")
    @Test
    fun `applicationSubmitted success`() {
      val submitApprovedPremisesApplication = SubmitApprovedPremisesApplication(
        translatedDocument = {},
        isPipeApplication = true,
        isWomensApplication = false,
        isEmergencyApplication = false,
        isEsapApplication = false,
        targetLocation = "SW1A 1AA",
        releaseType = ReleaseTypeOption.licence,
        type = "CAS1",
        sentenceType = SentenceTypeOption.nonStatutory,
        situation = SituationOption.bailSentence,
        applicantUserDetails = Cas1ApplicationUserDetails("applicantName", "applicantEmail", "applicationPhone"),
        caseManagerIsNotApplicant = false,
        reasonForShortNotice = "reason for short notice",
        reasonForShortNoticeOther = "reason for short notice other",
      )

      service.applicationSubmitted(
        application,
        submitApprovedPremisesApplication,
        username,
      )

      verify(exactly = 1) {
        mockDomainEventService.saveApplicationSubmittedDomainEvent(
          match {
            val data = it.data.eventDetails

            it.applicationId == application.id &&
              it.crn == application.crn &&
              it.nomsNumber == "THENOMS" &&
              data.applicationId == application.id &&
              data.applicationUrl == "http://frontend/applications/${application.id}" &&
              data.personReference == PersonReference(
                crn = "THECRN",
                noms = "THENOMS",
              ) &&
              data.deliusEventNumber == application.eventNumber &&
              data.releaseType == submitApprovedPremisesApplication.releaseType.toString() &&
              data.age == Period.between(LocalDate.of(1982, 3, 11), LocalDate.now()).years &&
              data.gender == uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model.ApplicationSubmitted.Gender.male &&
              data.submittedBy == ApplicationSubmittedSubmittedBy(
                staffMember = staffUserDetails.toStaffMember(),
                probationArea = domainEventProbationArea,
                team = Team(
                  code = caseDetails.case.manager.team.code,
                  name = caseDetails.case.manager.team.name,
                ),
                ldu = Ldu(
                  code = caseDetails.case.manager.team.ldu.code,
                  name = caseDetails.case.manager.team.ldu.name,
                ),
                region = Region(
                  code = staffUserDetails.probationArea.code,
                  name = staffUserDetails.probationArea.description,
                ),
              ) &&
              data.mappa == "CAT C1/LEVEL L1" &&
              data.sentenceLengthInMonths == null &&
              data.offenceId == application.offenceId &&
              it.metadata[MetaDataName.CAS1_APP_REASON_FOR_SHORT_NOTICE].equals("reason for short notice") &&
              it.metadata[MetaDataName.CAS1_APP_REASON_FOR_SHORT_NOTICE_OTHER].equals("reason for short notice other") &&
              enumValueOf<ApprovedPremisesType>(it.metadata[MetaDataName.CAS1_REQUESTED_AP_TYPE].toString()).asApiType()
                .toString() == ApType.normal.value
          },
        )
      }
    }

    @Test
    fun `applicationSubmitted doesn't save null metadata values`() {
      val submitApprovedPremisesApplication = SubmitApprovedPremisesApplication(
        translatedDocument = {},
        isPipeApplication = true,
        isWomensApplication = false,
        isEmergencyApplication = false,
        isEsapApplication = false,
        targetLocation = "SW1A 1AA",
        releaseType = ReleaseTypeOption.licence,
        type = "CAS1",
        sentenceType = SentenceTypeOption.nonStatutory,
        situation = SituationOption.bailSentence,
        applicantUserDetails = Cas1ApplicationUserDetails("applicantName", "applicantEmail", "applicationPhone"),
        caseManagerIsNotApplicant = false,
        reasonForShortNotice = null,
        reasonForShortNoticeOther = null,
      )

      service.applicationSubmitted(
        application,
        submitApprovedPremisesApplication,
        username,
      )

      verify(exactly = 1) {
        mockDomainEventService.saveApplicationSubmittedDomainEvent(
          this.withArg {
            assertThat(it.metadata).containsOnlyKeys(MetaDataName.CAS1_REQUESTED_AP_TYPE)
          },
        )
      }
    }
  }

  @Nested
  inner class ApplicationWithdrawn {

    val user = UserEntityFactory()
      .withUnitTestControlProbationRegion()
      .produce()

    @Test
    fun `applicationWithdrawn dont emit domain event if application no submitted`() {
      val application = ApprovedPremisesApplicationEntityFactory()
        .withCreatedByUser(user)
        .withSubmittedAt(null)
        .withWithdrawalReason("alternative_identified_placement_no_longer_required")
        .withOtherWithdrawalReason("the other reason")
        .produce()

      assertDomainEventCreated(application, emitted = false)
    }

    @Test
    fun `applicationWithdrawn success`() {
      val application = ApprovedPremisesApplicationEntityFactory()
        .withCreatedByUser(user)
        .withSubmittedAt(OffsetDateTime.now())
        .withWithdrawalReason("alternative_identified_placement_no_longer_required")
        .withOtherWithdrawalReason("the other reason")
        .produce()

      assertDomainEventCreated(application, emitted = true)
    }

    private fun assertDomainEventCreated(
      application: ApprovedPremisesApplicationEntity,
      emitted: Boolean,
    ) {
      val domainEventWithdrawnBy = WithdrawnByFactory().produce()
      every { mockDomainEventTransformer.toWithdrawnBy(user) } returns domainEventWithdrawnBy
      every { mockDomainEventService.saveApplicationWithdrawnEvent(any(), any()) } just Runs

      service.applicationWithdrawn(application, user)

      verify(exactly = 1) {
        mockDomainEventService.saveApplicationWithdrawnEvent(
          match {
            val data = it.data.eventDetails

            it.applicationId == application.id &&
              it.crn == application.crn &&
              it.nomsNumber == application.nomsNumber &&
              data.applicationId == application.id &&
              data.applicationUrl == "http://frontend/applications/${application.id}" &&
              data.personReference == PersonReference(
                crn = application.crn,
                noms = application.nomsNumber!!,
              ) &&
              data.deliusEventNumber == application.eventNumber &&
              data.withdrawalReason == "alternative_identified_placement_no_longer_required" &&
              data.otherWithdrawalReason == "the other reason"
          },
          emit = emitted,
        )
      }
    }
  }
}
