package uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.cas2bail

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.UUID

@Repository
interface Cas2BailApplicationSummaryRepository : JpaRepository<Cas2BailApplicationSummaryEntity, String> {
  fun findByUserId(userId: String, pageable: Pageable?): Page<Cas2BailApplicationSummaryEntity>

  fun findByUserIdAndSubmittedAtIsNotNull(userId: String, pageable: Pageable?): Page<Cas2BailApplicationSummaryEntity>

  fun findByUserIdAndSubmittedAtIsNull(userId: String, pageable: Pageable?): Page<Cas2BailApplicationSummaryEntity>

  fun findByPrisonCode(prisonCode: String, pageable: Pageable?): Page<Cas2BailApplicationSummaryEntity>

  fun findByPrisonCodeAndSubmittedAtIsNotNull(prisonCode: String, pageable: Pageable?): Page<Cas2BailApplicationSummaryEntity>

  fun findByPrisonCodeAndSubmittedAtIsNull(prisonCode: String, pageable: Pageable?): Page<Cas2BailApplicationSummaryEntity>

  fun findBySubmittedAtIsNotNull(pageable: Pageable?): Page<Cas2BailApplicationSummaryEntity>
}

@Entity
@Table(name = "cas_2_bail_application_live_summary")
data class Cas2BailApplicationSummaryEntity(
  @Id
  val id: UUID,
  val crn: String,
  @Column(name = "noms_number")
  var nomsNumber: String,
  @Column(name = "created_by_user_id")
  val userId: String,
  @Column(name = "name")
  val userName: String,
  @Column(name = "created_at")
  val createdAt: OffsetDateTime,
  @Column(name = "submitted_at")
  var submittedAt: OffsetDateTime?,
  @Column(name = "abandoned_at")
  var abandonedAt: OffsetDateTime? = null,
  @Column(name = "hdc_eligibility_date")
  var hdcEligibilityDate: LocalDate? = null,
  @Column(name = "label")
  var latestStatusUpdateLabel: String? = null,
  @Column(name = "status_id")
  var latestStatusUpdateStatusId: String? = null,
  @Column(name = "referring_prison_code")
  val prisonCode: String,
)
