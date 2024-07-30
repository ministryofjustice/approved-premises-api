package uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity

import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.UUID
import javax.persistence.Entity
import javax.persistence.Id
import javax.persistence.JoinColumn
import javax.persistence.ManyToOne
import javax.persistence.OneToMany
import javax.persistence.OneToOne
import javax.persistence.Table

@Repository
interface Cas1OutOfServiceBedRepository : JpaRepository<Cas1OutOfServiceBedEntity, UUID> {
  @Query(
    """
    SELECT
      CAST(oosb.id AS TEXT)
    FROM cas1_out_of_service_beds oosb
    INNER JOIN (
      SELECT
        out_of_service_bed_id,
        MAX(created_at) AS max_created_at
      FROM cas1_out_of_service_bed_revisions
      WHERE
        (FALSE = :excludePast OR end_date >= CURRENT_DATE) AND
        (FALSE = :excludeCurrent OR CURRENT_DATE NOT BETWEEN start_date AND end_date) AND
        (FALSE = :excludeFuture OR start_date <= CURRENT_DATE)
      GROUP BY out_of_service_bed_id
    ) dd
    ON oosb.id = dd.out_of_service_bed_id
    LEFT JOIN cas1_out_of_service_bed_revisions d
    ON dd.out_of_service_bed_id = d.out_of_service_bed_id
    AND dd.max_created_at = d.created_at
    LEFT JOIN premises p
    ON oosb.premises_id = p.id
    LEFT JOIN probation_regions pr
    ON p.probation_region_id = pr.id
    LEFT JOIN ap_areas apa
    ON pr.ap_area_id = apa.id
    LEFT JOIN beds b
    ON oosb.bed_id = b.id
    LEFT JOIN rooms r
    ON b.room_id = r.id
    LEFT JOIN cas1_out_of_service_bed_reasons oosr
    ON d.out_of_service_bed_reason_id = oosr.id
    WHERE
      (CAST(:premisesId AS UUID) IS NULL OR oosb.premises_id = :premisesId) AND
      (CAST(:apAreaId AS UUID) IS NULL OR apa.id = :apAreaId)
    """,
    nativeQuery = true,
  )
  fun findOutOfServiceBeds(
    premisesId: UUID?,
    apAreaId: UUID?,
    excludePast: Boolean,
    excludeCurrent: Boolean,
    excludeFuture: Boolean,
    pageable: Pageable?,
  ): Page<String>

  @Query("SELECT oosb FROM Cas1OutOfServiceBedEntity oosb LEFT JOIN oosb.cancellation c WHERE oosb.premises.id = :premisesId AND c is NULL")
  fun findAllActiveForPremisesId(premisesId: UUID): List<Cas1OutOfServiceBedEntity>

  @Query(
    """
    SELECT
      CAST(oosb.id AS TEXT)
    FROM cas1_out_of_service_beds oosb
    INNER JOIN (
      SELECT
        out_of_service_bed_id,
        MAX(created_at) AS max_created_at
      FROM cas1_out_of_service_bed_revisions
      WHERE
        start_date <= :endDate AND
        end_date >= :startDate
      GROUP BY out_of_service_bed_id
    ) d
    ON oosb.id = d.out_of_service_bed_id
    LEFT JOIN cas1_out_of_service_bed_cancellations c
    ON oosb.id = c.out_of_service_bed_id
    LEFT JOIN beds b
    ON oosb.bed_id = b.id
    WHERE
      b.id = :bedId AND
      (CAST(:thisEntityId AS UUID) IS NULL OR oosb.id != :thisEntityId) AND
      c IS NULL
    """,
    nativeQuery = true,
  )
  fun findByBedIdAndOverlappingDate(bedId: UUID, startDate: LocalDate, endDate: LocalDate, thisEntityId: UUID?): List<String>
}

@Entity
@Table(name = "cas1_out_of_service_beds")
data class Cas1OutOfServiceBedEntity(
  @Id
  val id: UUID,
  @ManyToOne
  @JoinColumn(name = "premises_id")
  val premises: ApprovedPremisesEntity,
  @ManyToOne
  @JoinColumn(name = "bed_id")
  val bed: BedEntity,
  val createdAt: OffsetDateTime,
  @OneToOne(mappedBy = "outOfServiceBed")
  var cancellation: Cas1OutOfServiceBedCancellationEntity?,
  @OneToMany(mappedBy = "outOfServiceBed")
  var revisionHistory: MutableList<Cas1OutOfServiceBedRevisionEntity>,
) {
  val latestRevision: Cas1OutOfServiceBedRevisionEntity
    get() = revisionHistory.maxBy { it.createdAt }

  val reason: Cas1OutOfServiceBedReasonEntity
    get() = latestRevision.reason

  val startDate
    get() = latestRevision.startDate

  val endDate
    get() = latestRevision.endDate

  val referenceNumber
    get() = latestRevision.referenceNumber

  val notes
    get() = latestRevision.notes
}
