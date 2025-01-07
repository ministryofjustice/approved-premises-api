package uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.cas2v2

import io.hypersistence.utils.hibernate.type.json.JsonType
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.LockModeType
import jakarta.persistence.ManyToOne
import jakarta.persistence.OneToMany
import jakarta.persistence.OneToOne
import jakarta.persistence.Table
import org.hibernate.annotations.Immutable
import org.hibernate.annotations.OrderBy
import org.hibernate.annotations.Type
import org.springframework.data.domain.Slice
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Lock
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.JsonSchemaEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.NomisUserEntity
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.UUID

@Suppress("TooManyFunctions")
@Repository
interface Cas2v2ApplicationRepository : JpaRepository<Cas2v2ApplicationEntity, UUID> {
  @Query(
    "SELECT a FROM Cas2v2ApplicationEntity a WHERE a.id = :id AND " +
      "a.submittedAt IS NOT NULL",
  )
  fun findSubmittedApplicationById(id: UUID): Cas2v2ApplicationEntity?

  @Query("SELECT a FROM Cas2v2ApplicationEntity a WHERE a.createdByUser.id = :id")
  fun findAllByCreatedByUserId(id: UUID): List<Cas2v2ApplicationEntity>

  @Query(
    "SELECT a FROM Cas2v2ApplicationEntity a WHERE a.submittedAt IS NOT NULL " +
      "AND a NOT IN (SELECT application FROM Cas2v2AssessmentEntity)",
  )
  fun findAllSubmittedApplicationsWithoutAssessments(): Slice<Cas2v2ApplicationEntity>
}

@Repository
interface Cas2v2LockableApplicationRepository : JpaRepository<Cas2v2LockableApplicationEntity, UUID> {
  @Query("SELECT a FROM Cas2v2LockableApplicationEntity a WHERE a.id = :id")
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  fun acquirePessimisticLock(id: UUID): Cas2v2LockableApplicationEntity?
}

@Entity
@Table(name = "cas_2_v2_applications")
data class Cas2v2ApplicationEntity(
  @Id
  val id: UUID,

  val crn: String,

  @ManyToOne
  @JoinColumn(name = "created_by_user_id")
  val createdByUser: NomisUserEntity,

  @Type(JsonType::class)
  var data: String?,

  @Type(JsonType::class)
  var document: String?,

  @ManyToOne
  @JoinColumn(name = "schema_version")
  var schemaVersion: JsonSchemaEntity,
  val createdAt: OffsetDateTime,
  var submittedAt: OffsetDateTime?,
  var abandonedAt: OffsetDateTime? = null,

  @OneToMany(mappedBy = "application")
  @OrderBy("createdAt DESC")
  var statusUpdates: MutableList<Cas2v2StatusUpdateEntity>? = null,

  @OneToMany(mappedBy = "application")
  @OrderBy("createdAt DESC")
  var notes: MutableList<Cas2v2ApplicationNoteEntity>? = null,

  @OneToOne(mappedBy = "application")
  var assessment: Cas2v2AssessmentEntity? = null,

  @Transient
  var schemaUpToDate: Boolean,

  var nomsNumber: String?,

  var referringPrisonCode: String? = null,
  var preferredAreas: String? = null,
  var hdcEligibilityDate: LocalDate? = null,
  var conditionalReleaseDate: LocalDate? = null,
  var telephoneNumber: String? = null,
) {
  override fun toString() = "Cas2v2ApplicationEntity: $id"
}

/**
 * Provides a version of the Cas2v2ApplicationEntity with no relationships, allowing
 * us to lock the applications table only without JPA/Hibernate attempting to
 * lock all eagerly loaded relationships
 */
@Entity
@Table(name = "cas_2_v2_applications")
@Immutable
class Cas2v2LockableApplicationEntity(
  @Id
  val id: UUID,
)
