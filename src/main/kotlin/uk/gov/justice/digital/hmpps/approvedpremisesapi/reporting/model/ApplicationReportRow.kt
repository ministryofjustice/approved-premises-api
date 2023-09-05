package uk.gov.justice.digital.hmpps.approvedpremisesapi.reporting.model

import java.time.LocalDate

data class ApplicationReportRow(
  val id: String,
  val crn: String,
  val applicationAssessedDate: LocalDate?,
  val assessorCru: String?,
  val assessmentDecision: String?,
  val assessmentDecisionRationale: String?,
  val ageInYears: Int?,
  val gender: String?,
  val mappa: String,
  val offenceId: String,
  val noms: String?,
  val premisesType: String?,
  val releaseType: String?,
  val sentenceLengthInMonths: Int?,
  val applicationSubmissionDate: LocalDate?,
  val referrerLdu: String?,
  val referrerRegion: String?,
  val referrerTeam: String?,
  val targetLocation: String?,
  val applicationWithdrawalReason: String?,
  val applicationWithdrawalDate: LocalDate?,
  val bookingID: String?,
  val bookingCancellationReason: String?,
  val bookingCancellationDate: LocalDate?,
  val expectedArrivalDate: LocalDate?,
  val matcherCru: String?,
  val expectedDepartureDate: LocalDate?,
  val premisesName: String?,
  val actualArrivalDate: LocalDate?,
  val actualDepartureDate: LocalDate?,
  val departureMoveOnCategory: String?,
  val nonArrivalDate: LocalDate?,
)
