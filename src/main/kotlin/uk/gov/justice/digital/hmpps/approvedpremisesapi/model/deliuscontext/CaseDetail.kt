package uk.gov.justice.digital.hmpps.approvedpremisesapi.model.deliuscontext

import java.time.LocalDate
import java.time.ZonedDateTime

data class CaseDetail(
  val case: CaseSummary,
  val offences: List<Offence>,
  val registrations: List<Registration>,
  val mappaDetail: MappaDetail?,
)

data class CaseSummary(
  val crn: String,
  val nomsId: String?,
  val pnc: String?,
  val name: Name,
  val dateOfBirth: LocalDate,
  val gender: String?,
  val profile: Profile?,
  val manager: Manager,
  val currentExclusion: Boolean,
  val currentRestriction: Boolean,
)

data class Name(
  val forename: String,
  val surname: String,
  val middleNames: List<String>,
)

data class Manager(
  val team: Team,
)

data class Team(
  val code: String,
  val name: String,
  val ldu: Ldu,
  val borough: Borough?,
  val startDate: LocalDate?,
  val endDate: LocalDate?,
)

data class Borough(
  val code: String,
  val description: String,
)

data class Ldu(
  val code: String,
  val name: String,
)

data class Profile(
  val ethnicity: String?,
  val genderIdentity: String?,
  @Deprecated("This is not currently populated by ap-delius-context")
  val selfDescribedGender: String?,
  val nationality: String?,
  val religion: String?,
)

data class Offence(
  val id: String,
  val description: String,
  val mainCategoryDescription: String,
  val subCategoryDescription: String,
  val date: LocalDate?,
  val main: Boolean,
  val eventNumber: String,
  val eventId: Long,
)

data class Registration(
  val code: String,
  val description: String,
  val startDate: LocalDate,
)

data class MappaDetail(
  val level: Int?,
  val levelDescription: String?,
  val category: Int?,
  val categoryDescription: String?,
  val startDate: LocalDate,
  val lastUpdated: ZonedDateTime,
)

data class CaseSummaries(
  var cases: List<CaseSummary>,
)

data class ReferralDetail(
  /*
  Note that the time element is typically 00:00
   */
  val arrivedAt: ZonedDateTime?,
  /*
  Note that the time element is typically 00:00
   */
  val departedAt: ZonedDateTime?,
)
