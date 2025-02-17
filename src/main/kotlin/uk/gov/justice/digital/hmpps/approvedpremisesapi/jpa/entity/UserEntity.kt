package uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity

import jakarta.persistence.Convert
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.OneToMany
import jakarta.persistence.Table
import org.hibernate.annotations.UpdateTimestamp
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.JpaSpecificationExecutor
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.converter.StringListConverter
import java.time.OffsetDateTime
import java.util.Objects
import java.util.UUID

@SuppressWarnings("TooManyFunctions")
@Repository
interface UserRepository : JpaRepository<UserEntity, UUID>, JpaSpecificationExecutor<UserEntity> {
  @Query(
    """
    SELECT u.* FROM users u WHERE u.name ILIKE '%' || :str || '%' AND u.is_active IS TRUE
  """,
    nativeQuery = true,
  )
  fun findByNameContainingIgnoreCase(str: String): List<UserEntity>

  fun findByDeliusUsername(deliusUsername: String): UserEntity?

  @Query("SELECT DISTINCT u FROM UserEntity u join u.roles r where r.role = :role and u.isActive = true")
  fun findActiveUsersWithRole(role: UserRole): List<UserEntity>

  @Query("SELECT DISTINCT u FROM UserEntity u join u.roles r where r.role in (:roles) and u.isActive = true")
  fun findActiveUsersWithAtLeastOneRole(roles: List<UserRole>): List<UserEntity>

  @Query("SELECT DISTINCT u FROM UserEntity u where u.isActive = true")
  fun findActiveUsers(): List<UserEntity>

  @Query("SELECT DISTINCT u FROM UserEntity u join u.qualifications q where q.qualification = :qualification and u.isActive = true")
  fun findActiveUsersWithQualification(qualification: UserQualification): List<UserEntity>

  @Query(
    """
      SELECT
      u.id as userId, 
      a.role as roleName
      FROM 
      users u
      LEFT OUTER JOIN user_role_assignments a ON u.id = a.user_id
      WHERE u.delius_username = :deliusUsername
    """,
    nativeQuery = true,
  )
  fun findRoleAssignmentByUsername(deliusUsername: String): List<RoleAssignmentByUsername>

  interface RoleAssignmentByUsername {
    val userId: UUID
    val roleName: String?
  }

  @Query(
    """
    SELECT u.*,
     RANK() OVER (
      ORDER BY
        (
          SELECT COUNT(1)
            FROM assessments a
            WHERE a.allocated_to_user_id = u.id
            AND 
              (
                a.submitted_at IS NULL
                OR (
                  a.submitted_at IS NOT NULL
                    AND a.created_at BETWEEN 
                      (CURRENT_TIMESTAMP - interval '1 week') AND
                  		CURRENT_TIMESTAMP
                )
            )
        ) ASC
     ) as score
    FROM "users" u
    WHERE u.id IN (:userIds)
    ORDER BY score ASC
    LIMIT 1
    """,
    nativeQuery = true,
  )
  fun findUserWithLeastAssessmentsPendingOrCompletedInLastWeek(userIds: List<UUID>): UserEntity?

  @Query(
    """
    SELECT u.*,
    RANK() OVER (
      ORDER BY
        (
          SELECT COUNT(1)
            FROM placement_applications pa
            WHERE pa.allocated_to_user_id = u.id
            AND 
              (
                pa.submitted_at IS NULL
                OR (
                  pa.submitted_at IS NOT NULL
                    AND pa.created_at BETWEEN 
                      (CURRENT_TIMESTAMP - interval '1 week') AND
                  		CURRENT_TIMESTAMP
                )
            )
        ) ASC
     ) as score
    FROM "users" u
    WHERE u.id IN (:userIds)
    ORDER BY score ASC 
    LIMIT 1
    """,
    nativeQuery = true,
  )
  fun findUserWithLeastPlacementApplicationsPendingOrCompletedInLastWeek(userIds: List<UUID>): UserEntity?

  @Query(
    """
    SELECT
      CAST(u.id as TEXT) as userId,
      (
        SELECT
          count(*)
        from
          assessments a
        where
          a.allocated_to_user_id = u.id
          and a.reallocated_at is null
          and a.submitted_at is null
          and a.is_withdrawn != true
      ) as pendingAssessments,
      (
        SELECT
          count(*)
        from
          assessments a
        where
          a.allocated_to_user_id = u.id
          and a.reallocated_at is null
          and a.submitted_at > current_date - interval '7' day
      ) as completedAssessmentsInTheLastSevenDays,
      (
        SELECT
          count(*)
        from
          assessments a
        where
          a.allocated_to_user_id = u.id
          and a.reallocated_at is null
          and a.submitted_at > current_date - interval '30' day
      ) as completedAssessmentsInTheLastThirtyDays,
      (
        SELECT
          count(*)
        from
          placement_applications placement_application
        where
          placement_application.allocated_to_user_id = u.id
          and placement_application.reallocated_at is null
          and placement_application.submitted_at is null
          and placement_application.decision is null
      ) as pendingPlacementApplications,
      (
        SELECT
          count(*)
        from
          placement_applications placement_application
        where
          placement_application.allocated_to_user_id = u.id
          and placement_application.reallocated_at is null
          and placement_application.submitted_at > current_date - interval '7' day
      ) as completedPlacementApplicationsInTheLastSevenDays,
      (
        SELECT
          count(*)
        from
          placement_applications placement_application
        where
          placement_application.allocated_to_user_id = u.id
          and placement_application.reallocated_at is null
          and placement_application.submitted_at > current_date - interval '30' day
      ) as completedPlacementApplicationsInTheLastThirtyDays
    FROM
      users u
    WHERE
      u.id IN (:userIds)
    """,
    nativeQuery = true,
  )
  fun findWorkloadForUserIds(userIds: List<UUID>): List<UserWorkload>
}

@Entity
@Table(name = "users")
data class UserEntity(
  @Id
  val id: UUID,
  var name: String,
  val deliusUsername: String,
  var deliusStaffCode: String,
  var email: String?,
  var telephoneNumber: String?,
  var isActive: Boolean,
  @OneToMany(mappedBy = "createdByUser")
  val applications: MutableList<ApplicationEntity>,
  @OneToMany(mappedBy = "user")
  var roles: MutableList<UserRoleAssignmentEntity>,
  @OneToMany(mappedBy = "user")
  val qualifications: MutableList<UserQualificationAssignmentEntity>,
  @ManyToOne(fetch = FetchType.LAZY)
  var probationRegion: ProbationRegionEntity,
  @ManyToOne(fetch = FetchType.LAZY)
  var probationDeliveryUnit: ProbationDeliveryUnitEntity?,
  /**
   * The geographical area the user belongs to.
   */
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "ap_area_id")
  var apArea: ApAreaEntity?,
  /**
   * Used by CRU Members only to determine which tasks/applications they should work on.
   *
   * If a value is set in [cruManagementAreaOverride], the same value will also be set here
   */
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "cas1_cru_management_area_id")
  var cruManagementArea: Cas1CruManagementAreaEntity?,
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "cas1_cru_management_area_override_id")
  var cruManagementAreaOverride: Cas1CruManagementAreaEntity?,
  @Convert(converter = StringListConverter::class)
  var teamCodes: List<String>?,
  val createdAt: OffsetDateTime?,
  @UpdateTimestamp
  var updatedAt: OffsetDateTime?,
) {
  fun hasRole(userRole: UserRole) = roles.any { it.role == userRole }
  fun hasAnyRole(vararg userRoles: UserRole) = userRoles.any(::hasRole)
  fun hasQualification(userQualification: UserQualification) =
    qualifications.any { it.qualification === userQualification }

  fun hasAllQualifications(requiredQualifications: List<UserQualification>) =
    requiredQualifications.all(::hasQualification)
  fun hasPermission(permission: UserPermission) = roles.any { it.role.hasPermission(permission) }

  override fun toString() = "User $id"

  companion object {
    fun getVersionHashCode(roles: List<UserRole>): Int {
      val normalisedRoles = roles.toSet().sortedBy { it.name }
      return Objects.hash(
        normalisedRoles.map { it.name },
        normalisedRoles.map { it.permissions.map { permission -> permission.name } },
      )
    }
  }
}

@Repository
interface UserRoleAssignmentRepository : JpaRepository<UserRoleAssignmentEntity, UUID>

@Entity
@Table(name = "user_role_assignments")
data class UserRoleAssignmentEntity(
  @Id
  val id: UUID,
  @ManyToOne
  @JoinColumn(name = "user_id")
  val user: UserEntity,
  @Enumerated(value = EnumType.STRING)
  val role: UserRole,
) {
  override fun hashCode() = Objects.hash(id, role)
}

@Repository
interface UserQualificationAssignmentRepository : JpaRepository<UserQualificationAssignmentEntity, UUID>

@Entity
@Table(name = "user_qualification_assignments")
data class UserQualificationAssignmentEntity(
  @Id
  val id: UUID,
  @ManyToOne
  @JoinColumn(name = "user_id")
  val user: UserEntity,
  @Enumerated(value = EnumType.STRING)
  val qualification: UserQualification,
) {
  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (javaClass != other?.javaClass) return false

    other as UserQualificationAssignmentEntity

    if (id != other.id) return false
    if (user.id != other.user.id) return false
    if (qualification != other.qualification) return false

    return true
  }

  override fun hashCode(): Int {
    var result = id.hashCode()
    result = 31 * result + user.id.hashCode()
    result = 31 * result + qualification.hashCode()
    return result
  }
}

enum class UserQualification {
  PIPE,
  LAO,
  ESAP,
  EMERGENCY,
  RECOVERY_FOCUSED,
  MENTAL_HEALTH_SPECIALIST,
}

interface UserWorkload {
  fun getUserId(): UUID
  fun getPendingAssessments(): Int
  fun getCompletedAssessmentsInTheLastSevenDays(): Int
  fun getCompletedAssessmentsInTheLastThirtyDays(): Int
  fun getPendingPlacementApplications(): Int
  fun getCompletedPlacementApplicationsInTheLastSevenDays(): Int
  fun getCompletedPlacementApplicationsInTheLastThirtyDays(): Int
}
