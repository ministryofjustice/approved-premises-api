package uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity

import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import java.time.OffsetDateTime
import java.util.UUID

@Repository
interface PrisonerLocationRepository : JpaRepository<PrisonerLocationEntity, UUID> {
  @Query("SELECT p FROM PrisonerLocationEntity p WHERE p.nomsNumber = :nomsNumber ORDER BY p.startDate DESC LIMIT 1")
  fun findLatestByNomsNumber(nomsNumber: String): List<PrisonerLocationEntity>

  @Modifying
  @Query(
    """
    UPDATE PrisonerLocationEntity p set 
    p.endDate = :endDate
    where p.id = :id
    """,
  )
  fun updateEndDate(id: UUID, endDate: OffsetDateTime)
}

@Entity
@Table(name = "prisoner_locations")
data class PrisonerLocationEntity(
  @Id
  val id: UUID,
  val nomsNumber: String,
  val prisonCode: String,
  val pomId: String?,
  val startDate: OffsetDateTime,
  val endDate: OffsetDateTime?,
)
