package uk.gov.justice.digital.hmpps.approvedpremisesapi.service

import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.ApDeliusContextApiClient
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.ApOASysContextApiClient
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.CaseNotesClient
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.ClientResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.PrisonsApiClient
import uk.gov.justice.digital.hmpps.approvedpremisesapi.config.ExcludedCategory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.config.PrisonAdjudicationsConfig
import uk.gov.justice.digital.hmpps.approvedpremisesapi.config.PrisonAdjudicationsConfigBindingModel
import uk.gov.justice.digital.hmpps.approvedpremisesapi.config.PrisonCaseNotesConfig
import uk.gov.justice.digital.hmpps.approvedpremisesapi.config.PrisonCaseNotesConfigBindingModel
import uk.gov.justice.digital.hmpps.approvedpremisesapi.datasource.OffenderDetailsDataSource
import uk.gov.justice.digital.hmpps.approvedpremisesapi.datasource.OffenderRisksDataSource
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserQualification
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.PersonInfoResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.PersonRisks
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.PersonSummaryInfoResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.community.OffenderDetailSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.deliuscontext.APDeliusDocument
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.deliuscontext.CaseAccess
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.deliuscontext.CaseDetail
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.deliuscontext.CaseSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.deliuscontext.UserOffenderAccess
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.oasyscontext.NeedsDetails
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.oasyscontext.OffenceDetails
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.oasyscontext.RiskManagementPlan
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.oasyscontext.RisksToTheIndividual
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.oasyscontext.RoshSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.prisonsapi.Adjudication
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.prisonsapi.AdjudicationsPage
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.prisonsapi.Agency
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.prisonsapi.Alert
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.prisonsapi.CaseNote
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.prisonsapi.CaseNotesPage
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.prisonsapi.InmateDetail
import uk.gov.justice.digital.hmpps.approvedpremisesapi.problem.InternalServerErrorProblem
import uk.gov.justice.digital.hmpps.approvedpremisesapi.problem.NotFoundProblem
import uk.gov.justice.digital.hmpps.approvedpremisesapi.results.AuthorisableActionResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.results.CasResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.OffenderService.LimitedAccessStrategy
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.PersonTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.asCaseSummary
import java.io.OutputStream
import java.time.LocalDate

@Service
class OffenderService(
  private val prisonsApiClient: PrisonsApiClient,
  private val caseNotesClient: CaseNotesClient,
  private val apOASysContextApiClient: ApOASysContextApiClient,
  private val apDeliusContextApiClient: ApDeliusContextApiClient,
  private val offenderDetailsDataSource: OffenderDetailsDataSource,
  private val offenderRisksDataSource: OffenderRisksDataSource,
  private val personTransformer: PersonTransformer,
  prisonCaseNotesConfigBindingModel: PrisonCaseNotesConfigBindingModel,
  adjudicationsConfigBindingModel: PrisonAdjudicationsConfigBindingModel,
) {
  private val log = LoggerFactory.getLogger(this::class.java)

  private val prisonCaseNotesConfig: PrisonCaseNotesConfig
  private val adjudicationsConfig: PrisonAdjudicationsConfig

  init {
    val excludedCategories = prisonCaseNotesConfigBindingModel.excludedCategories
      ?: throw RuntimeException("No prison-case-notes.excluded-categories provided")

    prisonCaseNotesConfig = PrisonCaseNotesConfig(
      lookbackDays = prisonCaseNotesConfigBindingModel.lookbackDays ?: throw RuntimeException("No prison-case-notes.lookback-days configuration provided"),
      prisonApiPageSize = prisonCaseNotesConfigBindingModel.prisonApiPageSize ?: throw RuntimeException("No prison-api-page-size configuration provided"),
      excludedCategories = excludedCategories.mapIndexed { index, categoryConfig ->
        ExcludedCategory(
          category = categoryConfig.category ?: throw RuntimeException("No category provided for prison-case-notes.excluded-categories at index $index"),
          subcategory = categoryConfig.subcategory,
        )
      },
    )

    adjudicationsConfig = PrisonAdjudicationsConfig(
      prisonApiPageSize = adjudicationsConfigBindingModel.prisonApiPageSize ?: throw RuntimeException("No prison-adjudications.prison-api-page-size configuration provided"),
    )
  }

  sealed interface LimitedAccessStrategy {
    /**
     * Even if the calling user has exclusions or restrictions for a given offender,
     * return [PersonSummaryInfoResult.Success.Full] regardless.
     *
     * This strategy should be used with care, typically when the calling user
     * has a specific qualification that indicates they can always view limited
     * access offender information
     */
    data object IgnoreLimitedAccess : LimitedAccessStrategy

    /**
     * If the offender has restrictions or exclusions (i.e. limited access), retrieve
     * the access information for the calling user. If the calling user has limited
     * access to this offender (either restricted or excluded), return
     * [PersonSummaryInfoResult.Success.Restricted]
     */
    data class ReturnRestrictedIfLimitedAccess(val deliusUsername: String) : LimitedAccessStrategy
  }

  fun getPersonSummaryInfoResults(
    crns: Set<String>,
    limitedAccessStrategy: LimitedAccessStrategy,
  ): List<PersonSummaryInfoResult> {
    if (crns.isEmpty()) {
      return emptyList()
    }

    val crnsList = crns.toList()

    val caseSummariesByCrn = when (val result = apDeliusContextApiClient.getSummariesForCrns(crnsList)) {
      is ClientResult.Success -> result.body
      is ClientResult.Failure -> result.throwException()
    }.cases.associateBy(
      keySelector = { it.crn },
      valueTransform = { it },
    )

   /*
    * this could be more efficient by only retrieving access information for CRNs where
    * the corresponding [CaseSummary.hasLimitedAccess()] is true. A similar short-circuit
    * was implemented in the new deprecated [getOffender()] function
    */
    val caseAccessByCrn = when (limitedAccessStrategy) {
      is LimitedAccessStrategy.IgnoreLimitedAccess -> emptyMap()
      is LimitedAccessStrategy.ReturnRestrictedIfLimitedAccess ->
        when (val result = apDeliusContextApiClient.getUserAccessForCrns(limitedAccessStrategy.deliusUsername, crnsList)) {
          is ClientResult.Success -> result.body
          is ClientResult.Failure -> result.throwException()
        }.access.associateBy(
          keySelector = { it.crn },
          valueTransform = { it },
        )
    }

    return crns.map { crn ->
      toPersonSummaryInfo(
        crn,
        caseSummariesByCrn[crn],
        caseAccessByCrn[crn],
        limitedAccessStrategy,
      )
    }
  }

  fun toPersonSummaryInfo(
    crn: String,
    caseSummary: CaseSummary?,
    caseAccess: CaseAccess?,
    limitedAccessStrategy: LimitedAccessStrategy,
  ): PersonSummaryInfoResult {
    if (caseSummary == null) {
      log.debug("Could not find case summary for '$crn'. Returning not found")
      return PersonSummaryInfoResult.NotFound(crn)
    }

    if (!caseSummary.hasLimitedAccess() || limitedAccessStrategy is LimitedAccessStrategy.IgnoreLimitedAccess) {
      log.debug("No restrictions apply, or the caller has indicated to ignore restrictions for '$crn'. Returning full details")
      return PersonSummaryInfoResult.Success.Full(crn, caseSummary)
    }

    return if (caseAccess == null) {
      // This shouldn't happen
      log.warn("Could not find case access details for LAO '$crn'. Returning 'Not Found'")
      PersonSummaryInfoResult.NotFound(crn)
    } else {
      if (caseAccess.hasLimitedAccess()) {
        log.debug("Caller cannot access LAO '$crn'. Returning restricted")
        PersonSummaryInfoResult.Success.Restricted(crn, caseSummary.nomsId)
      } else {
        log.debug("Caller can access LAO '$crn'. Returning full details")
        PersonSummaryInfoResult.Success.Full(crn, caseSummary)
      }
    }
  }

  private fun CaseSummary.hasLimitedAccess() = this.currentExclusion == true || this.currentRestriction == true
  private fun CaseAccess.hasLimitedAccess() = this.userExcluded || this.userRestricted
  private fun CaseAccess.hasNotLimitedAccess() = !this.userExcluded && !this.userRestricted

  @Deprecated(
    "This function uses the now deprecated [OffenderDetailsDataSource]",
    ReplaceWith("getPersonSummaryInfoResults(crns, limitedAccessStrategy)"),
  )
  fun getOffenderSummariesByCrns(
    crns: Set<String>,
    deliusUsername: String?,
    ignoreLaoRestrictions: Boolean = false,
  ): List<PersonSummaryInfoResult> {
    check(ignoreLaoRestrictions || deliusUsername != null) { "If ignoreLao is false, delius username must be provided " }

    if (crns.isEmpty()) return emptyList()

    val offenderDetailsList = offenderDetailsDataSource.getOffenderDetailSummaries(crns.toList())
    val userAccessList = deliusUsername?.let { offenderDetailsDataSource.getUserAccessForOffenderCrns(it, crns.toList()) }

    return crns.map { crn ->
      val offenderResponse = offenderDetailsList[crn]
      val accessResponse = userAccessList?.get(crn)

      val offender = getOffender(
        ignoreLaoRestrictions,
        { offenderResponse },
        { accessResponse },
      )

      when (offender) {
        is AuthorisableActionResult.Success -> {
          PersonSummaryInfoResult.Success.Full(crn, offender.entity.asCaseSummary())
        }
        is AuthorisableActionResult.NotFound -> PersonSummaryInfoResult.NotFound(crn)
        is AuthorisableActionResult.Unauthorised -> {
          val nomsNumber = (offenderResponse as ClientResult.Success).body.otherIds.nomsNumber
          PersonSummaryInfoResult.Success.Restricted(crn, nomsNumber)
        }
      }
    }
  }

  @Deprecated(
    """
      This function uses the now deprecated [OffenderDetailsDataSource]. 
      It also throws an exception if an offender isn't found, instead of returning PersonSummaryInfoResult.NotFound
    """,
    ReplaceWith("getPersonSummaryInfoResults(crns, limitedAccessStrategy)"),
  )
  @SuppressWarnings("CyclomaticComplexMethod", "MagicNumber")
  fun getOffenderSummariesByCrns(crns: List<String>, userDistinguishedName: String, ignoreLaoRestrictions: Boolean = false): List<PersonSummaryInfoResult> {
    if (crns.isEmpty()) {
      return emptyList()
    }

    if (crns.size > 500) {
      throw InternalServerErrorProblem("Cannot bulk request more than 500 CRNs. ${crns.size} have been provided.")
    }

    val offenders = when (val response = apDeliusContextApiClient.getSummariesForCrns(crns)) {
      is ClientResult.Success -> response.body
      is ClientResult.Failure.StatusCode -> response.throwException()
      is ClientResult.Failure -> response.throwException()
    }

    val laoResponse = if (!ignoreLaoRestrictions) {
      when (val response = apDeliusContextApiClient.getUserAccessForCrns(userDistinguishedName, crns)) {
        is ClientResult.Success -> response.body
        is ClientResult.Failure.StatusCode -> response.throwException()
        is ClientResult.Failure -> response.throwException()
      }
    } else {
      null
    }

    return crns.map { crn ->
      val caseSummary = offenders.cases.find { it.crn == crn } ?: return@map PersonSummaryInfoResult.NotFound(crn)

      val isLao = laoResponse?.let {
        laoResponse.access.find { caseAccess ->
          caseAccess.crn == crn &&
            (caseAccess.userExcluded || caseAccess.userRestricted)
        } != null
      } ?: false

      if (isLao) {
        PersonSummaryInfoResult.Success.Restricted(crn, caseSummary.nomsId)
      } else {
        PersonSummaryInfoResult.Success.Full(crn, caseSummary)
      }
    }
  }

  @Deprecated(
    " This function returns the now deprecated [OffenderDetailSummary], which is the community-api data model",
    ReplaceWith("getPersonSummaryInfoResults(crns, limitedAccessStrategy)"),
  )
  fun getOffenderByCrn(crn: String, userDistinguishedName: String, ignoreLaoRestrictions: Boolean = false): AuthorisableActionResult<OffenderDetailSummary> {
    return getOffender(
      ignoreLaoRestrictions,
      { offenderDetailsDataSource.getOffenderDetailSummary(crn) },
      { offenderDetailsDataSource.getUserAccessForOffenderCrn(userDistinguishedName, crn) },
    )
  }

  @Deprecated(
    """
      This function returns the now deprecated [OffenderDetailSummary], which is the community-api data model
    """,
    ReplaceWith("getPersonSummaryInfoResults(crns, limitedAccessStrategy)"),
  )
  @Suppress("detekt:CyclomaticComplexMethod", "detekt:NestedBlockDepth", "detekt:ReturnCount")
  private fun getOffender(
    ignoreLaoRestrictions: Boolean,
    offenderProducer: () -> ClientResult<OffenderDetailSummary>?,
    userAccessProducer: () -> ClientResult<UserOffenderAccess>?,
  ): AuthorisableActionResult<OffenderDetailSummary> {
    val offender = when (val offenderResponse = offenderProducer()) {
      is ClientResult.Success -> offenderResponse.body
      is ClientResult.Failure.StatusCode -> when (offenderResponse.status) {
        HttpStatus.NOT_FOUND -> return AuthorisableActionResult.NotFound()
        else -> offenderResponse.throwException()
      }
      is ClientResult.Failure -> offenderResponse.throwException()
      null -> return AuthorisableActionResult.NotFound()
    }

    if (!ignoreLaoRestrictions) {
      if (offender.currentExclusion || offender.currentRestriction) {
        val access = when (val accessResponse = userAccessProducer()) {
          is ClientResult.Success -> accessResponse.body
          is ClientResult.Failure.StatusCode -> {
            if (accessResponse.status == HttpStatus.FORBIDDEN) {
              // This is legacy behaviour to differentiate between the community api get access for
              // a single CRN endpoint returning 403 (meaning the client can't access the endpoint),
              // and the endpoint returning a response indicating the user can't access the offender
              // This isn't required if directly calling apDeliusContextApiClient.getUserAccessForCrns
              try {
                accessResponse.deserializeTo<UserOffenderAccess>()
                return AuthorisableActionResult.Unauthorised()
              } catch (exception: Exception) {
                accessResponse.throwException()
              }
            }

            accessResponse.throwException()
          }
          is ClientResult.Failure -> accessResponse.throwException()
          null -> return AuthorisableActionResult.NotFound()
        }

        if (access.userExcluded || access.userRestricted) {
          return AuthorisableActionResult.Unauthorised()
        }
      }
    }

    return AuthorisableActionResult.Success(offender)
  }

  /*
   * This should call apDeliusContextApiClient.getSummariesForCrns(crns) directly
   */
  fun isLao(crn: String): Boolean {
    val offenderResponse = offenderDetailsDataSource.getOffenderDetailSummary(crn)

    val offender = when (offenderResponse) {
      is ClientResult.Success -> offenderResponse.body
      is ClientResult.Failure -> offenderResponse.throwException()
    }

    return offender.currentExclusion || offender.currentRestriction
  }

  fun canAccessOffender(
    crn: String,
    limitedAccessStrategy: LimitedAccessStrategy,
  ) = when (limitedAccessStrategy) {
    is LimitedAccessStrategy.IgnoreLimitedAccess -> true
    is LimitedAccessStrategy.ReturnRestrictedIfLimitedAccess -> canAccessOffender(
      username = limitedAccessStrategy.deliusUsername,
      crn = crn,
    )
  }

  fun canAccessOffender(username: String, crn: String) = canAccessOffenders(username, listOf(crn))[crn] == true

  @SuppressWarnings("MagicNumber")
  fun canAccessOffenders(username: String, crns: List<String>): Map<String, Boolean> {
    if (crns.isEmpty()) return emptyMap()

    if (crns.size > 500) {
      throw InternalServerErrorProblem("Cannot bulk request access details for more than 500 CRNs. ${crns.size} have been provided.")
    }

    return when (val clientResult = apDeliusContextApiClient.getUserAccessForCrns(username, crns)) {
      is ClientResult.Success -> {
        val crnToAccessResult = clientResult.body.access.associateBy(
          keySelector = { it.crn },
          valueTransform = { it },
        )

        crns.associateBy(
          keySelector = { it },
          valueTransform = { crn ->
            val access = crnToAccessResult[crn]

            access?.hasNotLimitedAccess() ?: false
          },
        )
      }
      is ClientResult.Failure -> clientResult.throwException()
    }
  }

  fun getInmateDetailByNomsNumber(crn: String, nomsNumber: String): AuthorisableActionResult<InmateDetail?> {
    var inmateDetailResponse = prisonsApiClient.getInmateDetailsWithWait(nomsNumber)

    val hasCacheTimedOut = inmateDetailResponse is ClientResult.Failure.PreemptiveCacheTimeout
    if (hasCacheTimedOut) {
      inmateDetailResponse = prisonsApiClient.getInmateDetailsWithCall(nomsNumber)
    }

    fun logFailedResponse(inmateDetailResponse: ClientResult.Failure<InmateDetail>) = when (hasCacheTimedOut) {
      true -> log.warn("Could not get inmate details for $crn after cache timed out", inmateDetailResponse.toException())
      false -> log.warn("Could not get inmate details for $crn as an unsuccessful response was cached", inmateDetailResponse.toException())
    }

    val inmateDetail = when (inmateDetailResponse) {
      is ClientResult.Success -> inmateDetailResponse.body
      is ClientResult.Failure.StatusCode -> when (inmateDetailResponse.status) {
        HttpStatus.NOT_FOUND -> return AuthorisableActionResult.NotFound()
        HttpStatus.FORBIDDEN -> return AuthorisableActionResult.Unauthorised()
        else -> {
          logFailedResponse(inmateDetailResponse)
          null
        }
      }
      is ClientResult.Failure -> {
        logFailedResponse(inmateDetailResponse)
        null
      }
    }

    return AuthorisableActionResult.Success(inmateDetail)
  }

  fun getRiskByCrn(crn: String, deliusUsername: String): AuthorisableActionResult<PersonRisks> {
    return when (getOffenderByCrn(crn, deliusUsername)) {
      is AuthorisableActionResult.NotFound -> AuthorisableActionResult.NotFound()
      is AuthorisableActionResult.Unauthorised -> AuthorisableActionResult.Unauthorised()
      is AuthorisableActionResult.Success -> AuthorisableActionResult.Success(
        offenderRisksDataSource.getPersonRisks(crn),
      )
    }
  }

  fun getFilteredPrisonCaseNotesByNomsNumber(nomsNumber: String, getCas1SpecificNoteTypes: Boolean): CasResult<List<CaseNote>> {
    val cas1PrisonNoteTypesToInclude = listOf(
      "Alert", "Conduct & Behaviour", "Custodial Violence Management", "Negative Behaviours", "Enforcement", "Interventions / Keywork",
      "Mental Health", "Drug Rehabilitation", "Social Care", "Positive Behaviour / Achievements", "Alcohol Treatment",
    )
    val allCaseNotes = mutableListOf<CaseNote>()

    val fromDate = LocalDate.now().minusDays(prisonCaseNotesConfig.lookbackDays.toLong())

    var currentPage: CaseNotesPage?
    var currentPageIndex: Int? = null
    do {
      if (currentPageIndex == null) {
        currentPageIndex = 0
      } else {
        currentPageIndex += 1
      }

      val caseNotesPageResponse = caseNotesClient.getCaseNotesPage(nomsNumber, fromDate, currentPageIndex, prisonCaseNotesConfig.prisonApiPageSize)
      currentPage = when (caseNotesPageResponse) {
        is ClientResult.Success -> caseNotesPageResponse.body
        is ClientResult.Failure.StatusCode -> when (caseNotesPageResponse.status) {
          HttpStatus.NOT_FOUND -> return CasResult.NotFound()
          HttpStatus.FORBIDDEN -> return CasResult.Unauthorised()
          else -> caseNotesPageResponse.throwException()
        }
        is ClientResult.Failure -> caseNotesPageResponse.throwException()
      }

      allCaseNotes.addAll(
        if (getCas1SpecificNoteTypes) {
          currentPage.content.filter { caseNote ->
            cas1PrisonNoteTypesToInclude.any { it == (caseNote.typeDescription ?: caseNote.type) }
          }
        } else {
          currentPage.content.filter { caseNote ->
            prisonCaseNotesConfig.excludedCategories.none { it.excluded(caseNote.type, caseNote.subType) }
          }
        },
      )
    } while (currentPage != null && currentPage.totalPages > currentPageIndex!! + 1)

    return CasResult.Success(allCaseNotes)
  }

  fun getAdjudicationsByNomsNumber(nomsNumber: String): AuthorisableActionResult<AdjudicationsPage> {
    val allAdjudications = mutableListOf<Adjudication>()
    val allAgencies = mutableListOf<Agency>()

    var currentPage: AdjudicationsPage? = null
    var currentPageIndex = 0
    do {
      if (currentPage != null) {
        currentPageIndex += 1
      }

      val offset = currentPageIndex * adjudicationsConfig.prisonApiPageSize

      val adjudicationsPageResponse = prisonsApiClient.getAdjudicationsPage(nomsNumber, offset, adjudicationsConfig.prisonApiPageSize)
      currentPage = when (adjudicationsPageResponse) {
        is ClientResult.Success -> adjudicationsPageResponse.body
        is ClientResult.Failure.StatusCode -> when (adjudicationsPageResponse.status) {
          HttpStatus.NOT_FOUND -> return AuthorisableActionResult.NotFound()
          HttpStatus.FORBIDDEN -> return AuthorisableActionResult.Unauthorised()
          else -> adjudicationsPageResponse.throwException()
        }
        is ClientResult.Failure -> adjudicationsPageResponse.throwException()
      }

      allAdjudications.addAll(currentPage.results)
      allAgencies.addAll(currentPage.agencies)
    } while (currentPage != null && currentPage.results.size == adjudicationsConfig.prisonApiPageSize)

    return AuthorisableActionResult.Success(
      AdjudicationsPage(
        results = allAdjudications,
        agencies = allAgencies,
      ),
    )
  }

  fun getAcctAlertsByNomsNumber(nomsNumber: String): AuthorisableActionResult<List<Alert>> {
    val alertsResult = prisonsApiClient.getAlerts(nomsNumber, "HA")

    val alerts = when (alertsResult) {
      is ClientResult.Success -> alertsResult.body
      is ClientResult.Failure.StatusCode -> when (alertsResult.status) {
        HttpStatus.NOT_FOUND -> return AuthorisableActionResult.NotFound()
        HttpStatus.FORBIDDEN -> return AuthorisableActionResult.Unauthorised()
        else -> alertsResult.throwException()
      }
      is ClientResult.Failure -> alertsResult.throwException()
    }

    return AuthorisableActionResult.Success(alerts)
  }

  fun getOASysNeeds(crn: String): AuthorisableActionResult<NeedsDetails> {
    val needsResult = apOASysContextApiClient.getNeedsDetails(crn)

    val needs = when (needsResult) {
      is ClientResult.Success -> needsResult.body
      is ClientResult.Failure.StatusCode -> when (needsResult.status) {
        HttpStatus.NOT_FOUND -> return AuthorisableActionResult.NotFound()
        HttpStatus.FORBIDDEN -> return AuthorisableActionResult.Unauthorised()
        else -> {
          log.warn("Failed to fetch OASys needs details - got response status ${needsResult.status}, returning 404")
          throw NotFoundProblem(crn, "OASys")
        }
      }
      is ClientResult.Failure.Other -> {
        log.warn("Failed to fetch OASys needs details, returning 404", needsResult.exception)
        throw NotFoundProblem(crn, "OASys")
      }
      is ClientResult.Failure -> needsResult.throwException()
    }

    return AuthorisableActionResult.Success(needs)
  }

  fun getOASysOffenceDetails(crn: String): AuthorisableActionResult<OffenceDetails> {
    val offenceDetailsResult = apOASysContextApiClient.getOffenceDetails(crn)

    val offenceDetails = when (offenceDetailsResult) {
      is ClientResult.Success -> offenceDetailsResult.body
      is ClientResult.Failure.StatusCode -> when (offenceDetailsResult.status) {
        HttpStatus.NOT_FOUND -> return AuthorisableActionResult.NotFound()
        HttpStatus.FORBIDDEN -> return AuthorisableActionResult.Unauthorised()
        else -> offenceDetailsResult.throwException()
      }
      is ClientResult.Failure -> offenceDetailsResult.throwException()
    }

    return AuthorisableActionResult.Success(offenceDetails)
  }

  fun getOASysRiskManagementPlan(crn: String): AuthorisableActionResult<RiskManagementPlan> {
    val riskManagementPlanResult = apOASysContextApiClient.getRiskManagementPlan(crn)

    val riskManagement = when (riskManagementPlanResult) {
      is ClientResult.Success -> riskManagementPlanResult.body
      is ClientResult.Failure.StatusCode -> when (riskManagementPlanResult.status) {
        HttpStatus.NOT_FOUND -> return AuthorisableActionResult.NotFound()
        HttpStatus.FORBIDDEN -> return AuthorisableActionResult.Unauthorised()
        else -> riskManagementPlanResult.throwException()
      }
      is ClientResult.Failure -> riskManagementPlanResult.throwException()
    }

    return AuthorisableActionResult.Success(riskManagement)
  }

  fun getOASysRoshSummary(crn: String): AuthorisableActionResult<RoshSummary> {
    val roshSummaryResult = apOASysContextApiClient.getRoshSummary(crn)

    val roshSummary = when (roshSummaryResult) {
      is ClientResult.Success -> roshSummaryResult.body
      is ClientResult.Failure.StatusCode -> when (roshSummaryResult.status) {
        HttpStatus.NOT_FOUND -> return AuthorisableActionResult.NotFound()
        HttpStatus.FORBIDDEN -> return AuthorisableActionResult.Unauthorised()
        else -> roshSummaryResult.throwException()
      }
      is ClientResult.Failure -> roshSummaryResult.throwException()
    }

    return AuthorisableActionResult.Success(roshSummary)
  }

  fun getOASysRiskToTheIndividual(crn: String): AuthorisableActionResult<RisksToTheIndividual> {
    val risksToTheIndividualResult = apOASysContextApiClient.getRiskToTheIndividual(crn)

    val riskToTheIndividual = when (risksToTheIndividualResult) {
      is ClientResult.Success -> risksToTheIndividualResult.body
      is ClientResult.Failure.StatusCode -> when (risksToTheIndividualResult.status) {
        HttpStatus.NOT_FOUND -> return AuthorisableActionResult.NotFound()
        HttpStatus.FORBIDDEN -> return AuthorisableActionResult.Unauthorised()
        else -> risksToTheIndividualResult.throwException()
      }
      is ClientResult.Failure -> risksToTheIndividualResult.throwException()
    }

    return AuthorisableActionResult.Success(riskToTheIndividual)
  }

  fun getCaseDetail(crn: String): CasResult<CaseDetail> {
    val caseDetail = when (val caseDetailResult = apDeliusContextApiClient.getCaseDetail(crn)) {
      is ClientResult.Success -> caseDetailResult.body
      is ClientResult.Failure.StatusCode -> when (caseDetailResult.status) {
        HttpStatus.NOT_FOUND -> return CasResult.NotFound()
        HttpStatus.FORBIDDEN -> return CasResult.Unauthorised()
        else -> caseDetailResult.throwException()
      }

      is ClientResult.Failure -> caseDetailResult.throwException()
    }
    return CasResult.Success(caseDetail)
  }

  fun getDocumentsFromApDeliusApi(crn: String): AuthorisableActionResult<List<APDeliusDocument>> {
    val documentsResult = apDeliusContextApiClient.getDocuments(crn)

    val documents = when (documentsResult) {
      is ClientResult.Success -> documentsResult.body
      is ClientResult.Failure.StatusCode -> when (documentsResult.status) {
        HttpStatus.NOT_FOUND -> return AuthorisableActionResult.NotFound()
        HttpStatus.FORBIDDEN -> return AuthorisableActionResult.Unauthorised()
        else -> documentsResult.throwException()
      }

      is ClientResult.Failure -> documentsResult.throwException()
    }
    return AuthorisableActionResult.Success(documents)
  }

  fun getDocumentFromDelius(
    crn: String,
    documentId: String,
    outputStream: OutputStream,
  ) = apDeliusContextApiClient.getDocument(crn, documentId, outputStream)

  @SuppressWarnings("CyclomaticComplexMethod", "NestedBlockDepth", "ReturnCount")
  fun getPersonInfoResult(
    crn: String,
    deliusUsername: String?,
    ignoreLaoRestrictions: Boolean,
  ): PersonInfoResult {
    check(ignoreLaoRestrictions || deliusUsername != null) { "If ignoreLao is false, delius username must be provided " }
    return getPersonInfoResults(setOf(crn), deliusUsername, ignoreLaoRestrictions).first()
  }

  fun getPersonInfoResults(
    crns: Set<String>,
    deliusUsername: String?,
    ignoreLaoRestrictions: Boolean,
  ): List<PersonInfoResult> {
    check(ignoreLaoRestrictions || deliusUsername != null) { "If ignoreLao is false, delius username must be provided" }

    if (crns.isEmpty()) return emptyList()

    val offendersDetails = getOffenderSummariesByCrns(crns, deliusUsername, ignoreLaoRestrictions)

    return offendersDetails.map {
      when (it) {
        is PersonSummaryInfoResult.Success.Full -> {
          val inmateDetails = it.summary.nomsId?.let { nomsNumber ->
            when (val inmateDetailsResult = getInmateDetailByNomsNumber(it.crn, nomsNumber)) {
              is AuthorisableActionResult.Success -> inmateDetailsResult.entity
              else -> null
            }
          }
          personTransformer.transformPersonSummaryInfoToPersonInfo(it, inmateDetails)
        }

        is PersonSummaryInfoResult.Success.Restricted,
        is PersonSummaryInfoResult.NotFound,
        is PersonSummaryInfoResult.Unknown,
        ->
          personTransformer.transformPersonSummaryInfoToPersonInfo(it, null)
      }
    }
  }
}

fun UserEntity.cas1LimitedAccessStrategy() = if (this.hasQualification(UserQualification.LAO)) {
  LimitedAccessStrategy.IgnoreLimitedAccess
} else {
  LimitedAccessStrategy.ReturnRestrictedIfLimitedAccess(this.deliusUsername)
}

fun UserEntity.cas3LimitedAccessStrategy() = LimitedAccessStrategy.ReturnRestrictedIfLimitedAccess(this.deliusUsername)
