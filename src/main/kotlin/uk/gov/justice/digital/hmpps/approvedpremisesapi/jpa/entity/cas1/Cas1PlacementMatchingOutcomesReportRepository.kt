package uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.cas1

import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.reporting.util.JdbcResultSetConsumer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.reporting.util.ReportJdbcTemplate

@Repository
class Cas1PlacementMatchingOutcomesReportRepository(
  val reportJdbcTemplate: ReportJdbcTemplate,
) {

  companion object {
    /**
     * We don't have a dedicated entity representing the dates requested
     * on the initial application (if any were requested at all).
     *
     * To find these dates we look for placement_requests that aren't linked to a
     * placement_application as this also includes withdrawal information
     *
     * For more information see [PlacementRequestEntity.isForApplicationsArrivalDate]
     */
    private const val INITIAL_REQUEST_FOR_PLACEMENT_QUERY = """
      SELECT 
        a.crn as crn,
        apa.risk_ratings -> 'tier' -> 'value' ->> 'level' as tier,
        CAST(a.id as TEXT) as application_id,
        CONCAT('placement_request:',pr.id) AS request_for_placement_id,
        CAST(pr.id as TEXT) AS match_request_id,
        'STANDARD' AS request_for_placement_type, 
        TO_CHAR(pr.expected_arrival,'dd/mm/yyyy') as requested_arrival_date,
        pr.duration as requested_duration_days,
        TO_CHAR(a.submitted_at,'dd/mm/yyyy') as request_for_placement_submitted_at,
        pr.withdrawal_reason as request_for_placement_withdrawal_reason,
        TO_CHAR(assess.submitted_at,'dd/mm/yyyy') as request_for_placement_assessed_date,
        CAST(b.id as TEXT) as placement_id,
        cr.name as placement_cancellation_reason
        FROM placement_requests pr
        INNER JOIN applications a ON pr.application_id = a.id
        INNER JOIN approved_premises_applications apa ON a.id = apa.id
        INNER JOIN assessments as assess ON assess.id = pr.assessment_id
        LEFT OUTER JOIN bookings as b ON b.id = pr.booking_id
        LEFT OUTER JOIN cancellations as c ON c.booking_id = b.id
        LEFT OUTER JOIN cancellation_reasons as cr ON c.cancellation_reason_id = cr.id
        WHERE
            pr.reallocated_at IS NULL AND
            pr.placement_application_id IS NULL AND
            date_part('month', pr.expected_arrival) = :month AND
            date_part('year', pr.expected_arrival) = :year AND
            a.service = 'approved-premises'
    """

    /**
     * Prior to March 2024, placement requests could have more than one associated
     * placement_application_dates entries. Unfortunately, we didn't maintain a
     * FK between these entries and their corresponding placement_requests.
     *
     * The inline query used in the FROM statement below links each placement_application_dates
     * to their corresponding placement_request by matching on placement_applications.id, start date
     * & duration.
     */
    private const val OTHER_REQUEST_FOR_PLACEMENT_QUERY = """
      SELECT 
      a.crn as crn,
      apa.risk_ratings -> 'tier' -> 'value' ->> 'level' as tier,
      CAST(a.id as TEXT) as application_id,
      CONCAT('placement_application:',pa.id) AS request_for_placement_id,
      CAST(pr.id as TEXT) AS match_request_id,
      CASE
        WHEN pa.placement_type = '0' THEN 'ROTL'
        WHEN pa.placement_type = '1' THEN 'RELEASE_FOLLOWING_DECISION'
        WHEN pa.placement_type = '2' THEN 'ADDITIONAL_PLACEMENT'
        ELSE ''
      END AS request_for_placement_type,
      TO_CHAR(pr.expected_arrival,'dd/mm/yyyy') as requested_arrival_date,
      pr.duration as requested_duration_days,
      TO_CHAR(pa.submitted_at,'dd/mm/yyyy') as request_for_placement_submitted_at,
      pa.withdrawal_reason as request_for_placement_withdrawal_reason,
      TO_CHAR(pa.decision_made_at,'dd/mm/yyyy') as request_for_placement_assessed_date,
      CAST(b.id as TEXT) as placement_id,
      cr.name as placement_cancellation_reason
      from
      (
	      SELECT
           pr.id as placement_request_id,
           pa.id as placement_application_id,
           pa_dates.id as placement_application_dates_id
			  FROM placement_requests pr
          INNER JOIN placement_applications pa ON pa.id = pr.placement_application_id
          INNER JOIN placement_application_dates pa_dates ON 
                    pa_dates.placement_application_id = pa.id AND 
                    pa_dates.expected_arrival = pr.expected_arrival AND
                    pa_dates.duration = pr.duration
			  WHERE pr.reallocated_at IS NULL
      ) as pr_to_dates
      inner join placement_requests pr on pr.id = pr_to_dates.placement_request_id 
      INNER JOIN placement_applications pa ON pa.id = pr_to_dates.placement_application_id
      INNER JOIN placement_application_dates pa_dates ON pa_dates.id = pr_to_dates.placement_application_dates_id
      INNER JOIN applications a ON pr.application_id = a.id
      INNER JOIN approved_premises_applications apa ON a.id = apa.id
      LEFT OUTER JOIN bookings as b ON b.id = pr.booking_id
      LEFT OUTER JOIN cancellations as c ON c.booking_id = b.id
      LEFT OUTER JOIN cancellation_reasons as cr ON c.cancellation_reason_id = cr.id
      WHERE
          pa.decision = 'ACCEPTED' AND
          date_part('month', pr.expected_arrival) = :month AND
          date_part('year', pr.expected_arrival) = :year AND
          a.service = 'approved-premises'
    """

    private const val QUERY = """ 
      $INITIAL_REQUEST_FOR_PLACEMENT_QUERY
      UNION ALL
      $OTHER_REQUEST_FOR_PLACEMENT_QUERY
      ORDER BY requested_arrival_date ASC
    """
  }

  fun generateReportRowsForExpectedArrivalMonth(
    month: Int,
    year: Int,
    jbdcResultSetConsumer: JdbcResultSetConsumer,
  ) =
    reportJdbcTemplate.query(
      QUERY,
      mapOf<String, Any>("month" to month, "year" to year),
      jbdcResultSetConsumer,
    )
}
