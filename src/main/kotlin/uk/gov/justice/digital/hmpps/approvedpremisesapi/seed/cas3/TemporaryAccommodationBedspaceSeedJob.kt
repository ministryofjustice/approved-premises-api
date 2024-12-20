package uk.gov.justice.digital.hmpps.approvedpremisesapi.seed.cas3

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.CharacteristicEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PremisesRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.RoomEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.TemporaryAccommodationPremisesEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.seed.SeedJob
import uk.gov.justice.digital.hmpps.approvedpremisesapi.seed.appendCharacteristicIfSet
import uk.gov.justice.digital.hmpps.approvedpremisesapi.seed.trimToNull
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.CharacteristicService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.RoomService
import java.lang.RuntimeException

@Component
class TemporaryAccommodationBedspaceSeedJob(
  private val premisesRepository: PremisesRepository,
  private val characteristicService: CharacteristicService,
  private val roomService: RoomService,
) : SeedJob<TemporaryAccommodationBedspaceSeedCsvRow>() {
  private val log = LoggerFactory.getLogger(this::class.java)

  override fun deserializeRow(columns: Map<String, String>) = TemporaryAccommodationBedspaceSeedCsvRow(
    premisesName = columns["Property reference"]!!.trim(),
    bedspaceName = columns["Bedspace reference"]!!.trim(),
    characteristics = getCharacteristics(columns),
    notes = columns["Optional notes about the bedspace"].trimToNull(),
  )

  override fun processRow(row: TemporaryAccommodationBedspaceSeedCsvRow) {
    val premises = premisesRepository
      .findByName(row.premisesName, TemporaryAccommodationPremisesEntity::class.java) as TemporaryAccommodationPremisesEntity?
      ?: throw RuntimeException("Premises with reference '${row.premisesName}' does not exist")

    val existingRoom = premises.rooms.firstOrNull { it.name == row.bedspaceName }

    val characteristics = row.characteristics.map {
      characteristicService.getCharacteristics(it)
        .firstOrNull { it.serviceScope == "temporary-accommodation" }
        ?: throw RuntimeException("Characteristic $it does not exist")
    }

    if (existingRoom != null) {
      updateExistingRoom(row, existingRoom, premises, characteristics)
    } else {
      createNewRoom(row, premises, characteristics)
    }
  }

  private fun createNewRoom(
    row: TemporaryAccommodationBedspaceSeedCsvRow,
    premises: TemporaryAccommodationPremisesEntity,
    characteristics: List<CharacteristicEntity>,
  ) {
    log.info("Creating new Temporary Accommodation bedspace '${row.bedspaceName}' on premises '${row.premisesName}'")

    roomService.createRoom(premises, row.bedspaceName, row.notes, characteristics.map { it.id }, null)
  }

  private fun updateExistingRoom(
    row: TemporaryAccommodationBedspaceSeedCsvRow,
    existingRoom: RoomEntity,
    premises: TemporaryAccommodationPremisesEntity,
    characteristics: List<CharacteristicEntity>,
  ) {
    log.info("Updating existing Temporary Accommodation bedspace '${row.bedspaceName}' on premises '${row.premisesName}'")

    roomService.updateRoom(premises, existingRoom.id, row.notes, characteristics.map { it.id })
  }

  private fun getCharacteristics(columns: Map<String, String>): List<String> {
    val characteristics = mutableListOf<String>()

    appendCharacteristicIfSet(columns, characteristics, "Shared kitchen")
    appendCharacteristicIfSet(columns, characteristics, "Shared bathroom")
    appendCharacteristicIfSet(columns, characteristics, "Ground floor level access", "Floor level access?")
    appendCharacteristicIfSet(columns, characteristics, "Wheelchair accessible")
    appendCharacteristicIfSet(columns, characteristics, "Other - please state in notes")

    return characteristics.toList()
  }
}

data class TemporaryAccommodationBedspaceSeedCsvRow(
  val premisesName: String,
  val bedspaceName: String,
  val characteristics: List<String>,
  val notes: String?,
)
