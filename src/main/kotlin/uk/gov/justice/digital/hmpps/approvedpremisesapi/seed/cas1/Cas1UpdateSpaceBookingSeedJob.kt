package uk.gov.justice.digital.hmpps.approvedpremisesapi.seed.cas1

import org.slf4j.LoggerFactory
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ServiceName
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.Cas1SpaceBookingEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.Cas1SpaceBookingRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.CharacteristicRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.seed.SeedColumns
import uk.gov.justice.digital.hmpps.approvedpremisesapi.seed.SeedJob
import java.util.UUID

@Service
class Cas1UpdateSpaceBookingSeedJob(
  val cas1SpaceBookingRepository: Cas1SpaceBookingRepository,
  val characteristicRepository: CharacteristicRepository,
) : SeedJob<Cas1UpdateSpaceBookingSeedJobCsvRow>(
  requiredHeaders = setOf(
    "space_booking_id",
    "update_event_number",
    "event_number",
    "update_criteria",
    "criteria",
  ),
) {
  private val log = LoggerFactory.getLogger(this::class.java)

  override fun deserializeRow(columns: Map<String, String>): Cas1UpdateSpaceBookingSeedJobCsvRow {
    val seedColumns = SeedColumns(columns)

    return Cas1UpdateSpaceBookingSeedJobCsvRow(
      spaceBookingId = seedColumns.getUuidOrNull("space_booking_id")!!,
      updateEventNumber = seedColumns.getYesNoBooleanOrNull("update_event_number") ?: false,
      eventNumber = seedColumns.getStringOrNull("event_number"),
      updateCriteria = seedColumns.getYesNoBooleanOrNull("update_criteria") ?: false,
      criteria = seedColumns.getCommaSeparatedValues("criteria"),
    )
  }

  override fun processRow(row: Cas1UpdateSpaceBookingSeedJobCsvRow) {
    val id = row.spaceBookingId

    val spaceBooking = cas1SpaceBookingRepository.findByIdOrNull(id) ?: error("Could not find space booking with id $id")

    if (!row.updateEventNumber && !row.updateCriteria) {
      error("Nothing to do")
    }

    if (row.updateEventNumber) {
      updateEventNumber(row, spaceBooking)
    }

    if (row.updateCriteria) {
      updateCriteria(row, spaceBooking)
    }

    cas1SpaceBookingRepository.save(spaceBooking)
  }

  private fun updateEventNumber(
    row: Cas1UpdateSpaceBookingSeedJobCsvRow,
    spaceBooking: Cas1SpaceBookingEntity,
  ) {
    requireNotNull(row.eventNumber) { "No event number specified" }
    require(spaceBooking.application == null) { "Cannot update the event number for a booking linked to an application" }

    spaceBooking.offlineApplication?.let {
      require(it.eventNumber == null) {
        "Cannot update the event number for a booking linked to an offline application with an event number"
      }
    }

    log.info("Updating event number for space booking ${spaceBooking.id} to ${row.eventNumber}")

    spaceBooking.deliusEventNumber = row.eventNumber
  }

  private fun updateCriteria(
    row: Cas1UpdateSpaceBookingSeedJobCsvRow,
    spaceBooking: Cas1SpaceBookingEntity,
  ) {
    val updatedCriteria = row.criteria
    val allowedCriteria = Cas1SpaceBookingEntity.CHARACTERISTICS_OF_INTEREST

    val unexpectedCriteria = updatedCriteria.filter { !allowedCriteria.contains(it) }
    if (unexpectedCriteria.size > 1) {
      error("The following criteria are not supported - $unexpectedCriteria")
    }

    log.info("Updating criteria for space booking ${spaceBooking.id} to $updatedCriteria")

    spaceBooking.criteria.clear()
    if (updatedCriteria.isNotEmpty()) {
      spaceBooking.criteria.addAll(
        characteristicRepository.findAllWherePropertyNameIn(updatedCriteria, ServiceName.approvedPremises.value),
      )
    }
  }
}

data class Cas1UpdateSpaceBookingSeedJobCsvRow(
  val spaceBookingId: UUID,
  val updateEventNumber: Boolean,
  val eventNumber: String? = null,
  val updateCriteria: Boolean,
  val criteria: List<String> = emptyList(),
)
