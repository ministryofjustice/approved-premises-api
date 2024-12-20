package uk.gov.justice.digital.hmpps.approvedpremisesapi.integration

import org.assertj.core.api.Assertions.assertThat
import org.jetbrains.kotlinx.dataframe.DataFrame
import org.jetbrains.kotlinx.dataframe.api.ExcessiveColumns
import org.jetbrains.kotlinx.dataframe.api.convertTo
import org.jetbrains.kotlinx.dataframe.api.toList
import org.jetbrains.kotlinx.dataframe.io.readExcel
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.repository.findByIdOrNull
import org.springframework.test.web.reactive.server.returnResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ApType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.AppealDecision
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.AssessmentAcceptance
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.AssessmentRejection
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1ApplicationUserDetails
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Gender
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.NewAppeal
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.NewPlacementApplication
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.NewReallocation
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.PlacementApplication
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.PlacementApplicationDecision
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.PlacementApplicationDecisionEnvelope
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.PlacementCriteria
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.PlacementDates
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.PlacementRequirements
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.PlacementType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ReleaseTypeOption
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.SentenceTypeOption
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ServiceName
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.SubmitApprovedPremisesApplication
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.SubmitPlacementApplication
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.UpdatePlacementApplication
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.ApDeliusContextApiClient
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.ClientResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.CaseDetailFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.PersonRisksFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.StaffDetailFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.TeamFactoryDeliusContext
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.from
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.cas1.Cas1SimpleApiClient
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenAUser
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenAnApArea
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenAnOffender
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.httpmocks.apDeliusContextMockSuccessfulCaseDetailCall
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.httpmocks.govUKBankHolidaysAPIMockSuccessfullCallWithEmptyResponse
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.AppealEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.AppealRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApplicationRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApprovedPremisesApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApprovedPremisesApplicationJsonSchemaEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApprovedPremisesAssessmentEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApprovedPremisesAssessmentJsonSchemaEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApprovedPremisesPlacementApplicationJsonSchemaEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.AssessmentDecision
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.AssessmentEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.AssessmentRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserRole
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.Mappa
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.PersonInfoResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.RiskStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.RiskTier
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.RiskWithStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.community.OffenderDetailSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.deliuscontext.CaseDetail
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.deliuscontext.MappaDetail
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.deliuscontext.ProbationArea
import uk.gov.justice.digital.hmpps.approvedpremisesapi.reporting.model.ApplicationReportRow
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.OffenderService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.asCaseDetail
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.randomStringMultiCaseWithNumbers
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.Period
import java.time.ZonedDateTime
import java.util.UUID

class ApplicationReportsTest : InitialiseDatabasePerClassTestBase() {
  @Autowired
  lateinit var realApplicationRepository: ApplicationRepository

  @Autowired
  lateinit var realAssessmentRepository: AssessmentRepository

  @Autowired
  lateinit var realAppealRepository: AppealRepository

  @Autowired
  lateinit var realOffenderService: OffenderService

  @Autowired
  lateinit var apDeliusContextApiClient: ApDeliusContextApiClient

  @Autowired
  lateinit var cas1SimpleApiClient: Cas1SimpleApiClient

  lateinit var referrerDetails: Pair<UserEntity, String>
  lateinit var referrerTeam: TeamFactoryDeliusContext
  lateinit var referrerProbationArea: String

  lateinit var assessorDetails: Pair<UserEntity, String>
  lateinit var futureManagerDetails: Pair<UserEntity, String>
  lateinit var workflowManagerDetails: Pair<UserEntity, String>
  lateinit var matcherDetails: Pair<UserEntity, String>
  lateinit var appealManagerDetails: Pair<UserEntity, String>

  lateinit var applicationSchema: ApprovedPremisesApplicationJsonSchemaEntity
  lateinit var assessmentSchema: ApprovedPremisesAssessmentJsonSchemaEntity
  lateinit var placementApplicationSchema: ApprovedPremisesPlacementApplicationJsonSchemaEntity

  lateinit var applicationWithoutAssessment: ApprovedPremisesApplicationEntity
  lateinit var applicationWithAssessment: ApprovedPremisesApplicationEntity
  lateinit var applicationWithPlacementApplication: ApprovedPremisesApplicationEntity
  lateinit var applicationWithReallocatedCompleteAssessments: ApprovedPremisesApplicationEntity
  lateinit var applicationShortNotice: ApprovedPremisesApplicationEntity
  lateinit var applicationWithAcceptedAppeal: ApprovedPremisesApplicationEntity
  lateinit var applicationWithRejectedAppeal: ApprovedPremisesApplicationEntity
  lateinit var applicationWithMultipleAppeals: ApprovedPremisesApplicationEntity
  lateinit var applicationWithMultipleAssessments: ApprovedPremisesApplicationEntity

  companion object Constants {
    const val AUTHORISED_DURATION_DAYS: Int = 12
  }

  @BeforeAll
  fun setup() {
    govUKBankHolidaysAPIMockSuccessfullCallWithEmptyResponse()

    referrerTeam = TeamFactoryDeliusContext
    referrerProbationArea = "Referrer probation area"

    referrerDetails = givenAUser(
      staffDetail = StaffDetailFactory.staffDetail(
        teams = listOf(referrerTeam.team()),
        probationArea = ProbationArea(code = randomStringMultiCaseWithNumbers(8), description = referrerProbationArea),
      ),
    )
    assessorDetails = givenAUser(
      roles = listOf(UserRole.CAS1_ASSESSOR),
      probationRegion = probationRegionEntityFactory.produceAndPersist {
        withYieldedApArea {
          givenAnApArea(name = "Wales")
        }
      },
    )
    futureManagerDetails = givenAUser(roles = listOf(UserRole.CAS1_FUTURE_MANAGER))
    workflowManagerDetails = givenAUser(roles = listOf(UserRole.CAS1_WORKFLOW_MANAGER))
    matcherDetails = givenAUser(roles = listOf(UserRole.CAS1_MATCHER))
    appealManagerDetails = givenAUser(roles = listOf(UserRole.CAS1_APPEALS_MANAGER))

    applicationSchema = approvedPremisesApplicationJsonSchemaEntityFactory.produceAndPersist {
      withAddedAt(OffsetDateTime.now())
      withId(UUID.randomUUID())
      withPermissiveSchema()
    }

    assessmentSchema = approvedPremisesAssessmentJsonSchemaEntityFactory.produceAndPersist {
      withAddedAt(OffsetDateTime.now())
      withId(UUID.randomUUID())
      withPermissiveSchema()
    }

    placementApplicationSchema = approvedPremisesPlacementApplicationJsonSchemaEntityFactory.produceAndPersist {
      withPermissiveSchema()
    }

    applicationWithoutAssessment = createApplication("applicationWithoutAssessment")

    applicationWithAssessment = createApplicationWithCompletedAssessment("application")

    applicationWithPlacementApplication = createApplicationWithCompletedAssessment("applicationWithPlacementApplication")
    createAndAcceptPlacementApplication(applicationWithPlacementApplication)

    applicationWithMultipleAssessments = createApplication("applicationWithMultipleAssessments")
    reallocateAssessment(applicationWithMultipleAssessments)
    acceptAssessmentForApplication(applicationWithMultipleAssessments)

    applicationWithReallocatedCompleteAssessments = createApplication("applicationWithReallocatedCompleteAssessments")
    reallocateAssessment(applicationWithReallocatedCompleteAssessments)
    acceptAssessmentForApplication(applicationWithReallocatedCompleteAssessments)

    applicationShortNotice = createApplication("applicationShortNotice", shortNotice = true)
    acceptAssessmentForApplication(applicationShortNotice, shortNotice = true)

    applicationWithAcceptedAppeal = createApplication("applicationWithAcceptedAppeal")
    val assessmentToAppealAccepted = rejectAssessmentForApplication(applicationWithAcceptedAppeal)
    acceptAppealForAssessment(assessmentToAppealAccepted)

    applicationWithRejectedAppeal = createApplication("applicationWithRejectedAppeal")
    val assessmentToAppealRejected = rejectAssessmentForApplication(applicationWithRejectedAppeal)
    rejectAppealForAssessment(assessmentToAppealRejected)

    applicationWithMultipleAppeals = createApplication("applicationWithMultipleAppeals")
    val assessmentToAppealMultiple = rejectAssessmentForApplication(applicationWithMultipleAppeals)
    rejectAppealForAssessment(assessmentToAppealMultiple)
    acceptAppealForAssessment(assessmentToAppealMultiple)
  }

  @Test
  fun `Get application report returns 403 Forbidden if user does not have all regions access`() {
    givenAUser { _, jwt ->
      webTestClient.get()
        .uri("/cas1/reports/applications?year=2023&month=4")
        .header("Authorization", "Bearer $jwt")
        .header("X-Service-Name", ServiceName.approvedPremises.value)
        .exchange()
        .expectStatus()
        .isForbidden
    }
  }

  @Test
  fun `Get application report returns 400 if month is provided and not within 1-12`() {
    givenAUser(roles = listOf(UserRole.CAS1_REPORT_VIEWER)) { _, jwt ->
      webTestClient.get()
        .uri("/cas1/reports/applications?year=2023&month=-1")
        .header("Authorization", "Bearer $jwt")
        .header("X-Service-Name", ServiceName.approvedPremises.value)
        .exchange()
        .expectStatus()
        .isBadRequest
        .expectBody()
        .jsonPath("$.detail").isEqualTo("month must be between 1 and 12")
    }
  }

  @Test
  fun `Get application report returns OK with correct applications`() {
    givenAUser(roles = listOf(UserRole.CAS1_REPORT_VIEWER)) { userEntity, jwt ->
      val now = LocalDate.now()
      val year = now.year.toString()
      val month = now.monthValue.toString()

      webTestClient.get()
        .uri("/cas1/reports/applications?year=$year&month=$month")
        .header("Authorization", "Bearer $jwt")
        .header("X-Service-Name", ServiceName.approvedPremises.value)
        .exchange()
        .expectStatus()
        .isOk
        .expectHeader().valuesMatch(
          "content-disposition",
          "attachment; filename=\"applications-$year-${month.padStart(2, '0')}-[0-9_]+.xlsx\"",
        )
        .expectBody()
        .consumeWith {
          val actual = DataFrame
            .readExcel(it.responseBody!!.inputStream())
            .convertTo<ApplicationReportRow>(ExcessiveColumns.Remove)
            .toList()

          assertThat(actual.size).isEqualTo(9)

          assertApplicationRowHasCorrectData(actual, applicationWithoutAssessment.id, userEntity, ApplicationFacets(isAssessed = false, isAccepted = false))
          assertApplicationRowHasCorrectData(actual, applicationWithAssessment.id, userEntity)
          assertApplicationRowHasCorrectData(actual, applicationWithPlacementApplication.id, userEntity, ApplicationFacets(hasPlacementApplication = true))
          assertApplicationRowHasCorrectData(actual, applicationWithReallocatedCompleteAssessments.id, userEntity)
          assertApplicationRowHasCorrectData(actual, applicationWithMultipleAssessments.id, userEntity)
          assertApplicationRowHasCorrectData(actual, applicationShortNotice.id, userEntity, ApplicationFacets(isShortNotice = true))
          assertApplicationRowHasCorrectData(actual, applicationWithAcceptedAppeal.id, userEntity, ApplicationFacets(hasAppeal = true, isAccepted = false))
          assertApplicationRowHasCorrectData(actual, applicationWithRejectedAppeal.id, userEntity, ApplicationFacets(hasAppeal = true, isAccepted = false))
          assertApplicationRowHasCorrectData(actual, applicationWithMultipleAppeals.id, userEntity, ApplicationFacets(hasAppeal = true, isAccepted = false))
        }
    }
  }

  enum class ReportType {
    Applications,
  }

  data class ApplicationFacets(
    val isAssessed: Boolean = true,
    val isAccepted: Boolean = true,
    val hasPlacementApplication: Boolean = false,
    val hasAppeal: Boolean = false,
    val reportType: ReportType = ReportType.Applications,
    val isShortNotice: Boolean = false,
  )

  private fun ApprovedPremisesApplicationEntity.getAppropriateAssessment(hasAppeal: Boolean): AssessmentEntity? = when (hasAppeal) {
    // By the time the assertions are made, a newer assessment will have automatically been made for accepted appeals.
    // To correctly assert on accepted appeals, the assessment that should be used is the latest one that was *rejected*,
    // not the latest one of any status.
    true -> this.assessments.filter { it.decision == AssessmentDecision.REJECTED }.maxByOrNull { it.createdAt }
    false -> this.getLatestAssessment()
  }

  private fun assertApplicationRowHasCorrectData(
    report: List<ApplicationReportRow>,
    applicationId: UUID,
    userEntity: UserEntity,
    applicationFacets: ApplicationFacets = ApplicationFacets(),
  ) {
    val reportRow = report.find { it.id == applicationId.toString() }!!

    val application = realApplicationRepository.findByIdOrNull(applicationId) as ApprovedPremisesApplicationEntity
    val assessment = application.getAppropriateAssessment(applicationFacets.hasAppeal)!!
    val offenderDetailSummary = getOffenderDetailForApplication(application, userEntity.deliusUsername)
    val caseDetail = getCaseDetailForApplication(application)

    val (referrerEntity, _) = referrerDetails

    assertThat(reportRow.crn).isEqualTo(application.crn)
    assertThat(reportRow.tier).isEqualTo("B")

    assertThat(reportRow.lastAllocatedToAssessorDate).isEqualTo(assessment.allocatedAt!!.toLocalDate())
    if (applicationFacets.isAssessed) {
      assertThat(assessment).isNotNull
      assertThat(assessment.submittedAt).isNotNull
      assertThat(reportRow.applicationAssessedDate).isEqualTo(assessment.submittedAt!!.toLocalDate())
      assertThat(reportRow.assessorCru).isEqualTo("Wales")
      if (!applicationFacets.hasAppeal) {
        assertThat(reportRow.assessmentDecision).isEqualTo(assessment.decision.toString())
      }
      assertThat(reportRow.assessmentDecisionRationale).isEqualTo(assessment.rejectionRationale)

      if (applicationFacets.isShortNotice) {
        assertThat(reportRow.applicantReasonForLateApplication).isEqualTo("theReasonForShortNoticeReason")
        assertThat(reportRow.applicantReasonForLateApplicationDetail).isEqualTo("theReasonForShortNoticeOther")
        assertThat(reportRow.assessorAgreeWithShortNoticeReason).isEqualTo("yes")
        assertThat(reportRow.assessorReasonForLateApplication).isEqualTo("thisIsAgreeWithShortNoticeReasonComments")
        assertThat(reportRow.assessorReasonForLateApplicationDetail).isEqualTo("thisIsTheReasonForLateApplication")
      } else {
        assertThat(reportRow.applicantReasonForLateApplication).isNull()
        assertThat(reportRow.applicantReasonForLateApplicationDetail).isNull()
        assertThat(reportRow.assessorAgreeWithShortNoticeReason).isNull()
        assertThat(reportRow.assessorReasonForLateApplication).isNull()
        assertThat(reportRow.assessorReasonForLateApplicationDetail).isNull()
      }
    }

    assertThat(reportRow.ageInYears).isEqualTo(Period.between(offenderDetailSummary.dateOfBirth, LocalDate.now()).years)
    assertThat(reportRow.gender).isEqualTo(offenderDetailSummary.gender)
    assertThat(reportRow.mappa).isEqualTo(application.riskRatings!!.mappa.value!!.level)
    assertThat(reportRow.offenceId).isEqualTo(application.offenceId)
    assertThat(reportRow.noms).isEqualTo(application.nomsNumber)

    assertThat(reportRow.releaseType).isEqualTo(application.releaseType)
    assertThat(reportRow.applicationSubmissionDate).isEqualTo(application.submittedAt!!.toLocalDate())
    assertThat(reportRow.targetLocation).isEqualTo(application.targetLocation)
    assertThat(reportRow.applicationWithdrawalReason).isEqualTo(application.withdrawalReason)

    assertThat(reportRow.referrerUsername).isEqualTo(referrerEntity.deliusUsername)
    assertThat(reportRow.referralLdu).isEqualTo(caseDetail.case.manager.team.ldu.name)
    assertThat(reportRow.referralRegion).isEqualTo(referrerProbationArea)
    assertThat(reportRow.referralTeam).isEqualTo(caseDetail.case.manager.team.name)

    val arrivalDate = application.arrivalDate?.toLocalDate()

    assertThat(reportRow.expectedArrivalDate).isEqualTo(arrivalDate)
    if (applicationFacets.isAssessed && applicationFacets.isAccepted && arrivalDate != null) {
      assertThat(reportRow.expectedDepartureDate).isEqualTo(arrivalDate?.plusDays(AUTHORISED_DURATION_DAYS.toLong()))
    } else {
      assertThat(reportRow.expectedDepartureDate).isNull()
    }

    if (applicationFacets.hasAppeal) {
      val appeals = realAppealRepository.findAllByAssessmentId(assessment.id)
      val latestAppeal = appeals.maxByOrNull { it.createdAt }!!
      assertThat(reportRow.assessmentAppealCount).isEqualTo(appeals.size)
      assertThat(reportRow.lastAssessmentAppealedDecision).isEqualTo(latestAppeal.decision)
      assertThat(reportRow.lastAssessmentAppealedDate).isEqualTo(latestAppeal.appealDate)
      assertThat(reportRow.assessmentAppealedFromStatus).isEqualTo(assessment.decision.toString())
      assertThat(reportRow.assessmentDecision).isEqualTo(latestAppeal.decision)
    }
  }

  private fun getOffenderDetailForApplication(application: ApplicationEntity, deliusUsername: String): OffenderDetailSummary {
    return when (val personInfo = realOffenderService.getPersonInfoResult(application.crn, deliusUsername, true)) {
      is PersonInfoResult.Success.Full -> personInfo.offenderDetailSummary
      else -> throw Exception("No offender found for CRN ${application.crn}")
    }
  }

  private fun getCaseDetailForApplication(application: ApplicationEntity): CaseDetail {
    return when (val caseDetailResult = apDeliusContextApiClient.getCaseDetail(application.crn)) {
      is ClientResult.Success -> caseDetailResult.body
      is ClientResult.Failure -> caseDetailResult.throwException()
    }
  }

  private fun createApplication(crn: String, withArrivalDate: Boolean = true, shortNotice: Boolean = false): ApprovedPremisesApplicationEntity {
    return createAndSubmitApplication(ApType.normal, crn, withArrivalDate, shortNotice)
  }

  private fun createApplicationWithCompletedAssessment(crn: String, withArrivalDate: Boolean = true): ApprovedPremisesApplicationEntity {
    val application = createAndSubmitApplication(ApType.normal, crn, withArrivalDate)
    acceptAssessmentForApplication(application)
    return application
  }

  private fun createAndSubmitApplication(apType: ApType, crn: String, withArrivalDate: Boolean = true, shortNotice: Boolean = false): ApprovedPremisesApplicationEntity {
    val (referrer, jwt) = referrerDetails
    val (offenderDetails, _) = givenAnOffender(
      offenderDetailsConfigBlock = { withCrn(crn) },
    )

    apDeliusContextMockSuccessfulCaseDetailCall(
      offenderDetails.otherIds.crn,
      CaseDetailFactory()
        .from(offenderDetails.asCaseDetail())
        .withMappaDetail(
          MappaDetail(
            2,
            "M2",
            2,
            "M2",
            LocalDate.parse("2022-09-06"),
            ZonedDateTime.parse("2022-09-06T00:00:00Z"),
          ),
        )
        .produce(),
    )

    val basicInformationJson =
      mapOf(
        "basic-information" to
          listOfNotNull(
            "sentence-type" to mapOf("sentenceType" to "Some Sentence Type"),
            if (shortNotice) {
              "reason-for-short-notice" to mapOf(
                "reason" to "theReasonForShortNoticeReason",
                "other" to "theReasonForShortNoticeOther",
              )
            } else {
              null
            },
          ).toMap(),
      )

    val application = approvedPremisesApplicationEntityFactory.produceAndPersist {
      withCreatedByUser(referrer)
      withCrn(offenderDetails.otherIds.crn)
      withNomsNumber(offenderDetails.otherIds.nomsNumber!!)
      withApplicationSchema(applicationSchema)
      withData(objectMapper.writeValueAsString(basicInformationJson))
      withRiskRatings(
        PersonRisksFactory()
          .withTier(
            RiskWithStatus(
              status = RiskStatus.Retrieved,
              value = RiskTier(
                level = "B",
                lastUpdated = LocalDate.now(),
              ),
            ),
          )
          .withMappa(
            RiskWithStatus(
              status = RiskStatus.Retrieved,
              value = Mappa(
                level = "CAT M2/LEVEL M2",
                lastUpdated = LocalDate.now(),
              ),
            ),
          ).produce(),
      )
    }

    val arrivalDate = if (withArrivalDate) {
      LocalDate.now().plusMonths(8)
    } else {
      null
    }

    webTestClient.post()
      .uri("/applications/${application.id}/submission")
      .header("Authorization", "Bearer $jwt")
      .bodyValue(
        SubmitApprovedPremisesApplication(
          translatedDocument = {},
          isPipeApplication = apType == ApType.pipe,
          isWomensApplication = false,
          isEmergencyApplication = false,
          isEsapApplication = apType == ApType.esap,
          targetLocation = "SW1A 1AA",
          releaseType = ReleaseTypeOption.licence,
          type = "CAS1",
          sentenceType = SentenceTypeOption.nonStatutory,
          arrivalDate = arrivalDate,
          applicantUserDetails = Cas1ApplicationUserDetails("applicantName", "applicantEmail", "applicationPhone"),
          caseManagerIsNotApplicant = false,
        ),
      )
      .exchange()
      .expectStatus()
      .isOk

    return realApplicationRepository.findByIdOrNull(application.id) as ApprovedPremisesApplicationEntity
  }

  private fun acceptAssessmentForApplication(application: ApprovedPremisesApplicationEntity, shortNotice: Boolean = false): ApprovedPremisesAssessmentEntity {
    val (assessorEntity, jwt) = assessorDetails
    val assessment = realAssessmentRepository.findByApplicationIdAndReallocatedAtNull(application.id)!!
    val postcodeDistrict = postCodeDistrictFactory.produceAndPersist()

    assessment.data = if (shortNotice) {
      """{
         "suitability-assessment": {
            "application-timeliness": {
               "agreeWithShortNoticeReason": "yes",
               "agreeWithShortNoticeReasonComments": "thisIsAgreeWithShortNoticeReasonComments",
               "reasonForLateApplication": "thisIsTheReasonForLateApplication"
            }
         }
      }"""
    } else {
      "{ }"
    }

    assessment.allocatedToUser = assessorEntity
    realAssessmentRepository.save(assessment)

    val essentialCriteria = listOf(PlacementCriteria.isArsonSuitable, PlacementCriteria.isESAP)
    val desirableCriteria = listOf(PlacementCriteria.isRecoveryFocussed, PlacementCriteria.acceptsSexOffenders)

    val placementDates = if (application.arrivalDate != null) {
      PlacementDates(
        expectedArrival = application.arrivalDate!!.toLocalDate(),
        duration = AUTHORISED_DURATION_DAYS,
      )
    } else {
      null
    }

    val placementRequirements = PlacementRequirements(
      gender = Gender.male,
      type = ApType.normal,
      location = postcodeDistrict.outcode,
      radius = 50,
      essentialCriteria = essentialCriteria,
      desirableCriteria = desirableCriteria,
    )

    webTestClient.post()
      .uri("/assessments/${assessment.id}/acceptance")
      .header("Authorization", "Bearer $jwt")
      .bodyValue(AssessmentAcceptance(document = mapOf("document" to "value"), requirements = placementRequirements, placementDates = placementDates))
      .exchange()
      .expectStatus()
      .isOk

    return realAssessmentRepository.findByIdOrNull(assessment.id) as ApprovedPremisesAssessmentEntity
  }

  private fun rejectAssessmentForApplication(application: ApprovedPremisesApplicationEntity): ApprovedPremisesAssessmentEntity {
    val (assessorEntity, jwt) = assessorDetails
    val assessment = realAssessmentRepository.findByApplicationIdAndReallocatedAtNull(application.id)!!

    assessment.data = "{}"
    assessment.allocatedToUser = assessorEntity
    realAssessmentRepository.save(assessment)

    webTestClient.post()
      .uri("/assessments/${assessment.id}/rejection")
      .header("Authorization", "Bearer $jwt")
      .bodyValue(
        AssessmentRejection(
          document = mapOf("document" to "value"),
          rejectionRationale = "Some reason",
        ),
      )
      .exchange()
      .expectStatus()
      .isOk

    return realAssessmentRepository.findByIdOrNull(assessment.id) as ApprovedPremisesAssessmentEntity
  }

  private fun reallocateAssessment(application: ApprovedPremisesApplicationEntity) {
    val (_, jwt) = workflowManagerDetails
    val (assigneeUser, _) = givenAUser(roles = listOf(UserRole.CAS1_ASSESSOR))

    val existingAssessment = application.getLatestAssessment()!!

    webTestClient.post()
      .uri("/tasks/assessment/${existingAssessment.id}/allocations")
      .header("Authorization", "Bearer $jwt")
      .header("X-Service-Name", ServiceName.approvedPremises.value)
      .bodyValue(
        NewReallocation(
          userId = assigneeUser.id,
        ),
      )
      .exchange()
      .expectStatus()
      .isCreated
  }

  private fun createAndAcceptPlacementApplication(application: ApprovedPremisesApplicationEntity) {
    val (_, jwt) = assessorDetails

    val rawResult = webTestClient.post()
      .uri("/placement-applications")
      .header("Authorization", "Bearer $jwt")
      .bodyValue(
        NewPlacementApplication(
          applicationId = application.id,
        ),
      )
      .exchange()
      .expectStatus()
      .isOk
      .returnResult<String>()
      .responseBody
      .blockFirst()

    val placementApplication = objectMapper.readValue(rawResult, PlacementApplication::class.java)

    webTestClient.put()
      .uri("/placement-applications/${placementApplication.id}")
      .header("Authorization", "Bearer $jwt")
      .bodyValue(
        UpdatePlacementApplication(
          data = mapOf("request-a-placement" to mapOf("decision-to-release" to mapOf("decisionToReleaseDate" to "2023-11-11"))),
        ),
      )
      .exchange()
      .expectStatus()
      .isOk

    val placementDates = listOf(
      PlacementDates(
        expectedArrival = LocalDate.now(),
        duration = AUTHORISED_DURATION_DAYS,
      ),
    )
    webTestClient.post()
      .uri("/placement-applications/${placementApplication.id}/submission")
      .header("Authorization", "Bearer $jwt")
      .bodyValue(
        SubmitPlacementApplication(
          translatedDocument = mapOf("thingId" to 123),
          placementType = PlacementType.additionalPlacement,
          placementDates = placementDates,
        ),
      )
      .exchange()
      .expectStatus()
      .isOk

    val (matcher, matcherJwt) = matcherDetails

    cas1SimpleApiClient.placementApplicationReallocate(
      integrationTestBase = this,
      placementApplicationId = placementApplication.id,
      NewReallocation(
        userId = matcher.id,
      ),
    )

    val reallocatedPlacementApp =
      placementApplicationRepository.findByApplication(application).first { it.reallocatedAt == null }

    webTestClient.post()
      .uri("/placement-applications/${reallocatedPlacementApp.id}/decision")
      .header("Authorization", "Bearer $matcherJwt")
      .bodyValue(
        PlacementApplicationDecisionEnvelope(
          decision = PlacementApplicationDecision.accepted,
          summaryOfChanges = "ChangeSummary",
          decisionSummary = "DecisionSummary",
        ),
      )
      .exchange()
      .expectStatus()
      .isOk
  }

  private fun acceptAppealForAssessment(assessment: ApprovedPremisesAssessmentEntity): AppealEntity {
    val (_, jwt) = appealManagerDetails

    webTestClient.post()
      .uri("/applications/${assessment.application.id}/appeals")
      .header("Authorization", "Bearer $jwt")
      .bodyValue(
        NewAppeal(
          appealDate = LocalDate.now(),
          appealDetail = "Some details about the appeal",
          decision = AppealDecision.accepted,
          decisionDetail = "Some details about why the appeal was accepted",
        ),
      )
      .exchange()
      .expectStatus()
      .isCreated

    return realAppealRepository.findAllByAssessmentId(assessment.id).maxByOrNull { it.createdAt }!!
  }

  private fun rejectAppealForAssessment(assessment: ApprovedPremisesAssessmentEntity): AppealEntity {
    val (_, jwt) = appealManagerDetails

    webTestClient.post()
      .uri("/applications/${assessment.application.id}/appeals")
      .header("Authorization", "Bearer $jwt")
      .bodyValue(
        NewAppeal(
          appealDate = LocalDate.now(),
          appealDetail = "Some details about the appeal",
          decision = AppealDecision.rejected,
          decisionDetail = "Some details about why the appeal was rejected",
        ),
      )
      .exchange()
      .expectStatus()
      .isCreated

    return realAppealRepository.findAllByAssessmentId(assessment.id).maxByOrNull { it.createdAt }!!
  }
}
