package uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.cas1

import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.UUID

@Repository
interface Cas1DeliusBookingImportRepository : JpaRepository<Cas1DeliusBookingImportEntity, UUID> {
  fun findByBookingId(id: UUID): Cas1DeliusBookingImportEntity?
}

@Entity
@Table(name = "cas1_delius_booking_import")
data class Cas1DeliusBookingImportEntity(
  @Id
  val bookingId: UUID,
  val crn: String,
  val eventNumber: String,
  val keyWorkerStaffCode: String?,
  val keyWorkerForename: String?,
  val keyWorkerMiddleName: String?,
  val keyWorkerSurname: String?,
  val departureReasonCode: String?,
  val moveOnCategoryCode: String?,
  val moveOnCategoryDescription: String?,
  val expectedArrivalDate: LocalDate,
  val arrivalDate: LocalDate?,
  val expectedDepartureDate: LocalDate?,
  val departureDate: LocalDate?,
  val nonArrivalDate: LocalDate?,
  val nonArrivalContactDatetime: OffsetDateTime?,
  val nonArrivalReasonCode: String?,
  val nonArrivalReasonDescription: String?,
  val nonArrivalNotes: String?,
)
