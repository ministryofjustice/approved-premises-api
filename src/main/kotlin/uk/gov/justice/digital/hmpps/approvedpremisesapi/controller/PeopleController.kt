package uk.gov.justice.digital.hmpps.approvedpremisesapi.controller

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.PeopleApiDelegate
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ActiveOffence
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Adjudication
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.OASysRiskOfSeriousHarm
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.OASysRiskToSelf
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.OASysSection
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.OASysSections
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Person
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.PersonAcctAlert
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.PersonRisks
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.PersonalTimeline
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.PrisonCaseNote
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ServiceName
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApprovedPremisesApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.OfflineApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserQualification
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.PersonInfoResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.community.OffenderDetailSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.problem.ForbiddenProblem
import uk.gov.justice.digital.hmpps.approvedpremisesapi.problem.NotFoundProblem
import uk.gov.justice.digital.hmpps.approvedpremisesapi.results.AuthorisableActionResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.ApplicationService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.FeatureFlagService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.HttpAuthService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.OffenderService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.UserService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.AdjudicationTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.AlertTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.ApplicationTimelineModel
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.BoxedApplication
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.NeedsDetailsTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.OASysSectionsTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.OffenceTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.PersonTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.PersonalTimelineTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.PrisonCaseNoteTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.RisksTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.extractEntityFromCasResult

@Service
class PeopleController(
  private val httpAuthService: HttpAuthService,
  private val offenderService: OffenderService,
  private val personTransformer: PersonTransformer,
  private val risksTransformer: RisksTransformer,
  private val prisonCaseNoteTransformer: PrisonCaseNoteTransformer,
  private val adjudicationTransformer: AdjudicationTransformer,
  private val alertTransformer: AlertTransformer,
  private val needsDetailsTransformer: NeedsDetailsTransformer,
  private val oaSysSectionsTransformer: OASysSectionsTransformer,
  private val offenceTransformer: OffenceTransformer,
  private val userService: UserService,
  private val applicationService: ApplicationService,
  private val personalTimelineTransformer: PersonalTimelineTransformer,
  private val featureFlagService: FeatureFlagService,
) : PeopleApiDelegate {

  override fun peopleSearchGet(crn: String): ResponseEntity<Person> {
    val user = userService.getUserForRequest()

    val personInfo = offenderService.getPersonInfoResult(crn, user.deliusUsername, user.hasQualification(UserQualification.LAO))

    when (personInfo) {
      is PersonInfoResult.NotFound -> throw NotFoundProblem(crn, "Offender")
      is PersonInfoResult.Unknown -> throw personInfo.throwable ?: RuntimeException("Could not retrieve person info for CRN: $crn")
      is PersonInfoResult.Success -> return ResponseEntity.ok(
        personTransformer.transformModelToPersonApi(personInfo),
      )
    }
  }

  override fun peopleCrnRisksGet(crn: String): ResponseEntity<PersonRisks> {
    val principal = httpAuthService.getDeliusPrincipalOrThrow()

    val risks = when (val risksResult = offenderService.getRiskByCrn(crn, principal.name)) {
      is AuthorisableActionResult.Unauthorised -> throw ForbiddenProblem()
      is AuthorisableActionResult.NotFound -> throw NotFoundProblem(crn, "Person")
      is AuthorisableActionResult.Success -> risksResult.entity
    }

    return ResponseEntity.ok(risksTransformer.transformDomainToApi(risks, crn))
  }

  override fun peopleCrnPrisonCaseNotesGet(
    crn: String,
    xServiceName: ServiceName,
  ): ResponseEntity<List<PrisonCaseNote>> {
    val offenderDetails = getOffenderDetails(crn)

    if (offenderDetails.otherIds.nomsNumber == null) {
      throw NotFoundProblem(crn, "Case Notes")
    }

    val getCas1SpecificNoteTypes = (
      xServiceName == ServiceName.approvedPremises &&
        featureFlagService.getBooleanFlag("cas1-only-list-specific-prison-note-types")
      )
    val nomsNumber = offenderDetails.otherIds.nomsNumber

    val prisonCaseNotesResult = offenderService.getFilteredPrisonCaseNotesByNomsNumber(nomsNumber, getCas1SpecificNoteTypes)

    return ResponseEntity.ok(extractEntityFromCasResult(prisonCaseNotesResult).map(prisonCaseNoteTransformer::transformModelToApi))
  }

  override fun peopleCrnAdjudicationsGet(crn: String, xServiceName: ServiceName): ResponseEntity<List<Adjudication>> {
    val offenderDetails = getOffenderDetails(crn)

    if (offenderDetails.otherIds.nomsNumber == null) {
      throw NotFoundProblem(crn, "Adjudications")
    }

    val nomsNumber = offenderDetails.otherIds.nomsNumber

    val adjudicationsResult = offenderService.getAdjudicationsByNomsNumber(nomsNumber)
    val adjudications = when (adjudicationsResult) {
      is AuthorisableActionResult.NotFound -> throw NotFoundProblem(crn, "Inmate")
      is AuthorisableActionResult.Unauthorised -> throw ForbiddenProblem()
      is AuthorisableActionResult.Success -> adjudicationsResult.entity
    }

    val getLast12MonthsOnly = (
      xServiceName == ServiceName.approvedPremises &&
        featureFlagService.getBooleanFlag("cas1-only-list-adjudications-up-to-12-months")
      )

    return ResponseEntity.ok(adjudicationTransformer.transformToApi(adjudications, getLast12MonthsOnly))
  }

  override fun peopleCrnAcctAlertsGet(crn: String): ResponseEntity<List<PersonAcctAlert>> {
    val offenderDetails = getOffenderDetails(crn)

    if (offenderDetails.otherIds.nomsNumber == null) {
      throw NotFoundProblem(crn, "ACCT Alerts")
    }

    val nomsNumber = offenderDetails.otherIds.nomsNumber

    val acctAlertsResult = offenderService.getAcctAlertsByNomsNumber(nomsNumber)
    val acctAlerts = when (acctAlertsResult) {
      is AuthorisableActionResult.NotFound -> throw NotFoundProblem(crn, "Inmate")
      is AuthorisableActionResult.Unauthorised -> throw ForbiddenProblem()
      is AuthorisableActionResult.Success -> acctAlertsResult.entity
    }

    return ResponseEntity.ok(acctAlerts.map(alertTransformer::transformToApi))
  }

  override fun peopleCrnOasysSelectionGet(crn: String): ResponseEntity<List<OASysSection>> {
    ensureUserCanAccessOffenderInfo(crn)

    val needsResult = offenderService.getOASysNeeds(crn)

    val needs = when (needsResult) {
      is AuthorisableActionResult.NotFound -> throw NotFoundProblem(crn, "Person")
      is AuthorisableActionResult.Unauthorised -> throw ForbiddenProblem()
      is AuthorisableActionResult.Success -> needsResult.entity
    }

    return ResponseEntity.ok(needsDetailsTransformer.transformToApi(needs))
  }

  override fun peopleCrnOasysSectionsGet(crn: String, selectedSections: List<Int>?): ResponseEntity<OASysSections> {
    ensureUserCanAccessOffenderInfo(crn)

    val needs = getSuccessEntityOrThrow(crn, offenderService.getOASysNeeds(crn))

    return runBlocking(context = Dispatchers.IO) {
      val offenceDetailsResult = async {
        offenderService.getOASysOffenceDetails(crn)
      }
      val roshSummaryResult = async {
        offenderService.getOASysRoshSummary(crn)
      }
      val riskToTheIndividualResult = async {
        offenderService.getOASysRiskToTheIndividual(crn)
      }
      val riskManagementPlanResult = async {
        offenderService.getOASysRiskManagementPlan(crn)
      }

      val offenceDetails = getSuccessEntityOrThrow(crn, offenceDetailsResult.await())
      val roshSummary = getSuccessEntityOrThrow(crn, roshSummaryResult.await())
      val riskToTheIndividual = getSuccessEntityOrThrow(crn, riskToTheIndividualResult.await())
      val riskManagementPlan = getSuccessEntityOrThrow(crn, riskManagementPlanResult.await())

      ResponseEntity.ok(
        oaSysSectionsTransformer.transformToApi(
          offenceDetails,
          roshSummary,
          riskToTheIndividual,
          riskManagementPlan,
          needs,
          selectedSections ?: emptyList(),
        ),
      )
    }
  }

  override fun peopleCrnOasysRiskToSelfGet(crn: String): ResponseEntity<OASysRiskToSelf> {
    ensureUserCanAccessOffenderInfo(crn)

    return runBlocking(context = Dispatchers.IO) {
      val offenceDetailsResult = async {
        offenderService.getOASysOffenceDetails(crn)
      }

      val riskToTheIndividualResult = async {
        offenderService.getOASysRiskToTheIndividual(crn)
      }

      val offenceDetails = getSuccessEntityOrThrow(crn, offenceDetailsResult.await())
      val riskToTheIndividual = getSuccessEntityOrThrow(crn, riskToTheIndividualResult.await())

      ResponseEntity.ok(
        oaSysSectionsTransformer.transformRiskToIndividual(offenceDetails, riskToTheIndividual),
      )
    }
  }

  override fun peopleCrnOasysRoshGet(crn: String): ResponseEntity<OASysRiskOfSeriousHarm> {
    ensureUserCanAccessOffenderInfo(crn)

    return runBlocking(context = Dispatchers.IO) {
      val offenceDetailsResult = async {
        offenderService.getOASysOffenceDetails(crn)
      }

      val roshResult = async {
        offenderService.getOASysRoshSummary(crn)
      }

      val offenceDetails = getSuccessEntityOrThrow(crn, offenceDetailsResult.await())
      val rosh = getSuccessEntityOrThrow(crn, roshResult.await())

      ResponseEntity.ok(
        oaSysSectionsTransformer.transformRiskOfSeriousHarm(offenceDetails, rosh),
      )
    }
  }

  override fun peopleCrnOffencesGet(crn: String): ResponseEntity<List<ActiveOffence>> {
    ensureUserCanAccessOffenderInfo(crn)

    val isOffencesFromAPDelius = featureFlagService.getBooleanFlag("get-offences-from-ap-delius")
    return if (isOffencesFromAPDelius) {
      val caseDetail = offenderService.getCaseDetail(crn)
      ResponseEntity.ok(
        offenceTransformer.transformToApi(extractEntityFromCasResult(caseDetail)),
      )
    } else {
      val convictionsResult = offenderService.getConvictions(crn)
      val activeConvictions = getSuccessEntityOrThrow(crn, convictionsResult)
        .filter { it.active }
      ResponseEntity.ok(
        activeConvictions.flatMap(offenceTransformer::transformToApi),
      )
    }
  }

  override fun peopleCrnTimelineGet(crn: String): ResponseEntity<PersonalTimeline> {
    val user = userService.getUserForRequest()

    val personInfo = offenderService.getPersonInfoResult(crn, user.deliusUsername, user.hasQualification(UserQualification.LAO))

    val personalTimeline = when (personInfo) {
      is PersonInfoResult.NotFound -> throw NotFoundProblem(crn, "Offender")
      is PersonInfoResult.Unknown -> throw personInfo.throwable ?: RuntimeException("Could not retrieve person info for CRN: $crn")
      is PersonInfoResult.Success -> {
        val regularApplications = applicationService
          .getApplicationsForCrn(crn, ServiceName.approvedPremises)
          .map { BoxedApplication.of(it as ApprovedPremisesApplicationEntity) }

        val offlineApplications = applicationService
          .getOfflineApplicationsForCrn(crn, ServiceName.approvedPremises)
          .map { BoxedApplication.of(it) }

        val allApplications = regularApplications + offlineApplications

        val applicationTimelineModels = allApplications
          .map { application ->
            val applicationId = application.map(
              ApprovedPremisesApplicationEntity::id,
              OfflineApplicationEntity::id,
            )

            val timelineEvents = applicationService.getApplicationTimeline(applicationId)

            ApplicationTimelineModel(application, timelineEvents)
          }

        personalTimelineTransformer.transformApplicationTimelineModels(personInfo, applicationTimelineModels)
      }
    }

    return ResponseEntity.ok(personalTimeline)
  }

  private fun <T> getSuccessEntityOrThrow(crn: String, authorisableActionResult: AuthorisableActionResult<T>): T = when (authorisableActionResult) {
    is AuthorisableActionResult.NotFound -> throw NotFoundProblem(crn, "Person")
    is AuthorisableActionResult.Unauthorised -> throw ForbiddenProblem()
    is AuthorisableActionResult.Success -> authorisableActionResult.entity
  }

  private fun ensureUserCanAccessOffenderInfo(crn: String) {
    getOffenderDetails(crn)
  }

  private fun getOffenderDetails(crn: String): OffenderDetailSummary {
    val user = userService.getUserForRequest()

    val offenderDetails = when (
      val offenderDetailsResult = offenderService.getOffenderByCrn(
        crn = crn,
        userDistinguishedName = user.deliusUsername,
        ignoreLaoRestrictions = false,
      )
    ) {
      is AuthorisableActionResult.NotFound -> throw NotFoundProblem(crn, "Person")
      is AuthorisableActionResult.Unauthorised -> throw ForbiddenProblem()
      is AuthorisableActionResult.Success -> offenderDetailsResult.entity
    }

    return offenderDetails
  }
}
