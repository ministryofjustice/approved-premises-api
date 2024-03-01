package uk.gov.justice.digital.hmpps.approvedpremisesapi.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.BookingEntity
import java.sql.Timestamp
import java.time.LocalDate
import java.util.UUID

interface TransitionalAccommodationReferralReportRepository : JpaRepository<BookingEntity, UUID> {
  @Query(
    """
    SELECT cast(a.id as text) AS assessmentId,
      CAST(ap.id as text) AS referralId,
      ap.created_at AS referralCreatedDate,
      ap.crn AS crn,
      ap.submitted_at AS referralSubmittedDate,
      taa.risk_ratings->'roshRisks'->'value'->>'overallRisk' AS riskOfSeriousHarm,
      taa.is_registered_sex_offender AS sexOffender,
      taa.needs_accessible_property AS needForAccessibleProperty,
      taa.has_history_of_arson AS historyOfArsonOffence,
      taa.is_duty_to_refer_submitted AS dutyToReferMade,
      taa.duty_to_refer_submission_date AS dateDutyToReferMade,
      taa.duty_to_refer_local_authority_area_name AS dutyToReferLocalAuthorityAreaName,
      probation_region.name AS probationRegionName,
      a.decision AS assessmentDecision,
      a.rejection_rationale AS assessmentRejectionReason,
      a.submitted_at AS assessmentSubmittedDate,
      CAST(b.id AS text) AS bookingId,
      taa.is_eligible AS isReferralEligibleForCas3,
      taa.eligibility_reason AS referralEligibilityReason,
      ap.noms_number as nomsNumber,
      taa.arrival_date as accommodationRequiredDate,
      taa.prison_name_on_creation as prisonNameOnCreation,
      taa.person_release_date as personReleaseDate,
      premises.town as town,
      premises.postcode as postCode
    FROM temporary_accommodation_assessments aa
    JOIN assessments a on aa.assessment_id = a.id AND a.service='temporary-accommodation' AND a.reallocated_at IS NULL
    JOIN applications ap on a.application_id = ap.id AND ap.service='temporary-accommodation'
    LEFT OUTER JOIN temporary_accommodation_applications taa on ap.id = taa.id
    LEFT JOIN probation_regions probation_region ON probation_region.id = taa.probation_region_id
    LEFT JOIN bookings b on b.application_id = ap.id AND b.service='temporary-accommodation'
    LEFT JOIN premises premises ON premises.id = b.premises_id and premises.service='temporary-accommodation'
    WHERE
      a.service = 'temporary-accommodation'
      AND ap.submitted_at BETWEEN :startDate AND :endDate
      AND (CAST(:probationRegionId AS UUID) IS NULL OR probation_region.id = :probationRegionId)
    ORDER BY ap.submitted_at
    """,
    nativeQuery = true,
  )
  fun findAllReferrals(
    startDate: LocalDate,
    endDate: LocalDate,
    probationRegionId: UUID?,
  ): List<TransitionalAccommodationReferralReportData>
}

interface TransitionalAccommodationReferralReportData {
  val assessmentId: String
  val referralId: String
  val bookingId: String?
  val referralCreatedDate: LocalDate
  val crn: String
  val referralSubmittedDate: LocalDate?
  val riskOfSeriousHarm: String?
  val sexOffender: Boolean?
  val needForAccessibleProperty: Boolean?
  val historyOfArsonOffence: Boolean?
  val dutyToReferMade: Boolean?
  val dateDutyToReferMade: LocalDate?
  val probationRegionName: String
  val dutyToReferLocalAuthorityAreaName: String?
  val assessmentDecision: String?
  val assessmentRejectionReason: String?
  val assessmentSubmittedDate: LocalDate?
  val referralEligibleForCas3: Boolean?
  val referralEligibilityReason: String?
  val accommodationRequiredDate: Timestamp?
  val prisonNameOnCreation: String?
  val personReleaseDate: LocalDate?
  val town: String?
  val postCode: String?
}
