package uk.gov.justice.digital.hmpps.approvedpremisesapi.util

import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.PersonSummaryInfoResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.community.OffenderDetailSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.community.OffenderIds
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.community.OffenderLanguages
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.community.OffenderProfile
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.deliuscontext.CaseAccess
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.deliuscontext.CaseSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.deliuscontext.Ldu
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.deliuscontext.Manager
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.deliuscontext.Name
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.deliuscontext.Profile
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.deliuscontext.Team
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.deliuscontext.UserOffenderAccess

fun getNameFromPersonSummaryInfoResult(result: PersonSummaryInfoResult): String = when (result) {
  is PersonSummaryInfoResult.Success.Full -> {
    listOf(
      listOf(result.summary.name.forename),
      listOf(result.summary.name.surname),
    ).flatten().joinToString(" ")
  }
  is PersonSummaryInfoResult.Success.Restricted -> {
    "Limited Access Offender"
  }
  is PersonSummaryInfoResult.NotFound -> {
    "Unknown"
  }
  is PersonSummaryInfoResult.Unknown -> {
    "Unknown"
  }
}

fun <V> PersonSummaryInfoResult.tryGetDetails(value: (CaseSummary) -> V): V? {
  return when (this) {
    is PersonSummaryInfoResult.Success.Full -> value(this.summary)
    else -> null
  }
}

fun OffenderDetailSummary.asCaseSummary() = CaseSummary(
  crn = this.otherIds.crn,
  nomsId = this.otherIds.nomsNumber,
  pnc = this.otherIds.pncNumber,
  name = Name(
    forename = this.firstName,
    surname = this.surname,
    middleNames = this.middleNames ?: emptyList(),
  ),
  dateOfBirth = this.dateOfBirth,
  gender = this.gender,
  profile = Profile(
    ethnicity = this.offenderProfile.ethnicity,
    genderIdentity = this.offenderProfile.genderIdentity,
    selfDescribedGender = this.offenderProfile.selfDescribedGender,
    nationality = this.offenderProfile.nationality,
    religion = this.offenderProfile.religion,
  ),
  manager = Manager(
    team = Team(
      code = "",
      name = "",
      ldu = Ldu(
        code = "",
        name = "",
      ),
      borough = null,
      startDate = null,
      endDate = null,
    ),
  ),
  currentExclusion = this.currentExclusion,
  currentRestriction = this.currentRestriction,
)

fun CaseSummary.asOffenderDetailSummary() = OffenderDetailSummary(
  offenderId = null,
  title = null,
  firstName = this.name.forename,
  middleNames = this.name.middleNames,
  surname = this.name.surname,
  previousSurname = null,
  preferredName = null,
  dateOfBirth = this.dateOfBirth,
  gender = this.gender ?: "unknown",
  otherIds = OffenderIds(
    crn = this.crn,
    croNumber = null,
    immigrationNumber = null,
    mostRecentPrisonNumber = null,
    niNumber = null,
    nomsNumber = this.nomsId,
    pncNumber = this.pnc,
  ),
  offenderProfile = OffenderProfile(
    ethnicity = this.profile?.ethnicity,
    nationality = this.profile?.nationality,
    secondaryNationality = null,
    notes = null,
    immigrationStatus = null,
    offenderLanguages = OffenderLanguages(
      primaryLanguage = null,
      otherLanguages = null,
      languageConcerns = null,
      requiresInterpreter = null,
    ),
    religion = this.profile?.religion,
    sexualOrientation = null,
    offenderDetails = null,
    remandStatus = null,
    riskColour = null,
    disabilities = null,
    genderIdentity = this.profile?.genderIdentity,
    selfDescribedGender = null,
  ),
  softDeleted = false,
  currentDisposal = null,
  partitionArea = null,
  currentRestriction = this.currentRestriction ?: false,
  currentExclusion = this.currentExclusion ?: false,
  isActiveProbationManagedSentence = null,
)

fun CaseAccess.asUserOffenderAccess() = UserOffenderAccess(
  userRestricted = this.userRestricted,
  userExcluded = this.userExcluded,
  restrictionMessage = this.restrictionMessage,
)

fun UserOffenderAccess.asCaseAccess(crn: String) = CaseAccess(
  crn = crn,
  userExcluded = this.userExcluded,
  userRestricted = this.userRestricted,
  exclusionMessage = null,
  restrictionMessage = this.restrictionMessage,
)
