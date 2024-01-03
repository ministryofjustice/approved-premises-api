package uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity

import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import java.time.LocalDateTime
import java.util.UUID
import javax.persistence.Entity
import javax.persistence.Id

@Repository
interface TaskRespository : JpaRepository<Task, UUID> {
  companion object {
    private const val ALLOCATABLE_QUERY = """
      SELECT
        cast(assessment.id as TEXT) as id,
        assessment.created_at as created_at,
        'assessment' as type
      from
        assessments assessment
        left join applications application on assessment.application_id = application.id
      where
        application.service = 'approved-premises'
        and assessment.is_withdrawn is not true
        and assessment.reallocated_at is null
        and assessment.submitted_at is null
        and (
              (:isAllocated is null) OR 
              (
                (:isAllocated = true and assessment.allocated_to_user_id is not null) or
                (:isAllocated = false and assessment.allocated_to_user_id is null)
              )
        )
      UNION ALL
      SELECT
        cast(placement_application.id as TEXT) as id,
        placement_application.created_at as created_at,
        'placement_application' as type
      from
        placement_applications placement_application
      where
        placement_application.submitted_at is not null
        and placement_application.reallocated_at is null
        and placement_application.decision is null
        and (
                (:isAllocated is null) OR 
                (
                    (:isAllocated = true and placement_application.allocated_to_user_id is not null) or
                    (:isAllocated = false and placement_application.allocated_to_user_id is null)
                )
            )
      UNION ALL
      SELECT
        cast(placement_request.id as TEXT) as id,
        placement_request.created_at as created_at,
        'placement_request' as type
      from
        placement_requests placement_request
        left join booking_not_mades booking_not_made on booking_not_made.placement_request_id = placement_request.id
      where
        placement_request.booking_id IS NULL
        AND placement_request.reallocated_at IS NULL
        AND placement_request.is_withdrawn is false
        AND booking_not_made.id IS NULL
        AND (
                (:isAllocated is null) OR 
                (
                    (:isAllocated = true and placement_request.allocated_to_user_id is not null) or
                    (:isAllocated = false and placement_request.allocated_to_user_id is null)
                )
            )
    """
  }

  @Query(
    ALLOCATABLE_QUERY,
    countQuery = "SELECT COUNT(1) FROM ($ALLOCATABLE_QUERY) as count",
    nativeQuery = true,
  )
  fun getAllReallocatable(isAllocated: Boolean?, pageable: Pageable?): Page<Task>
}

@Entity
data class Task(
  @Id
  val id: UUID,
  val createdAt: LocalDateTime,
  val type: String,
)
