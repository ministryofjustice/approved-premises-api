package uk.gov.justice.digital.hmpps.approvedpremisesapi.seed.cas1

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.BedEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.BedRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.CharacteristicEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.CharacteristicRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PremisesEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PremisesRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.RoomEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.RoomRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.seed.SeedJob
import java.time.OffsetDateTime
import java.util.UUID

@Component
class ApprovedPremisesRoomsSeedJob(
  private val premisesRepository: PremisesRepository,
  private val roomRepository: RoomRepository,
  private val bedRepository: BedRepository,
  private val characteristicRepository: CharacteristicRepository,
) : SeedJob<ApprovedPremisesRoomsSeedCsvRow>(
  requiredHeaders = setOf(
    "apCode",
    "bedCode",
    "roomNumber",
    "bedCount",
    "isSingle",
    "isGroundFloor",
    "isFullyFm",
    "hasCrib7Bedding",
    "hasSmokeDetector",
    "isTopFloorVulnerable",
    "isGroundFloorNrOffice",
    "hasNearbySprinkler",
    "isArsonSuitable",
    "isArsonDesignated",
    "hasArsonInsuranceConditions",
    "isSuitedForSexOffenders",
    "hasEnSuite",
    "isWheelchairAccessible",
    "hasWideDoor",
    "hasStepFreeAccess",
    "hasFixedMobilityAids",
    "hasTurningSpace",
    "hasCallForAssistance",
    "isWheelchairDesignated",
    "isStepFreeDesignated",
    "notes",
  ),
) {
  private val log = LoggerFactory.getLogger(this::class.java)

  override fun deserializeRow(columns: Map<String, String>) = ApprovedPremisesRoomsSeedCsvRow(
    apCode = columns["apCode"]!!,
    bedCode = columns["bedCode"]!!,
    roomNumber = columns["roomNumber"]!!,
    bedCount = columns["bedCount"]!!,
    isSingle = parseBooleanStringOrThrow(columns["isSingle"]!!, "isSingle"),
    isGroundFloor = parseBooleanStringOrThrow(columns["isGroundFloor"]!!, "isGroundFloor"),
    isFullyFm = parseBooleanStringOrThrow(columns["isFullyFm"]!!, "isFullyFm"),
    hasCrib7Bedding = parseBooleanStringOrThrow(columns["hasCrib7Bedding"]!!, "hasCrib7Bedding"),
    hasSmokeDetector = parseBooleanStringOrThrow(columns["hasSmokeDetector"]!!, "hasSmokeDetector"),
    isTopFloorVulnerable = parseBooleanStringOrThrow(columns["isTopFloorVulnerable"]!!, "isTopFloorVulnerable"),
    isGroundFloorNrOffice = parseBooleanStringOrThrow(columns["isGroundFloorNrOffice"]!!, "isGroundFloorNrOffice"),
    hasNearbySprinkler = parseBooleanStringOrThrow(columns["hasNearbySprinkler"]!!, "hasNearbySprinkler"),
    isArsonSuitable = parseBooleanStringOrThrow(columns["isArsonSuitable"]!!, "isArsonSuitable"),
    isArsonDesignated = parseBooleanStringOrThrow(columns["isArsonDesignated"]!!, "isArsonDesignated"),
    hasArsonInsuranceConditions = parseBooleanStringOrThrow(columns["hasArsonInsuranceConditions"]!!, "hasArsonInsuranceConditions"),
    isSuitedForSexOffenders = parseBooleanStringOrThrow(columns["isSuitedForSexOffenders"]!!, "isSuitedForSexOffenders"),
    hasEnSuite = parseBooleanStringOrThrow(columns["hasEnSuite"]!!, "hasEnSuite"),
    isWheelchairAccessible = parseBooleanStringOrThrow(columns["isWheelchairAccessible"]!!, "isWheelchairAccessible"),
    hasWideDoor = parseBooleanStringOrThrow(columns["hasWideDoor"]!!, "hasWideDoor"),
    hasStepFreeAccess = parseBooleanStringOrThrow(columns["hasStepFreeAccess"]!!, "hasStepFreeAccess"),
    hasFixedMobilityAids = parseBooleanStringOrThrow(columns["hasFixedMobilityAids"]!!, "hasFixedMobilityAids"),
    hasTurningSpace = parseBooleanStringOrThrow(columns["hasTurningSpace"]!!, "hasTurningSpace"),
    hasCallForAssistance = parseBooleanStringOrThrow(columns["hasCallForAssistance"]!!, "hasCallForAssistance"),
    isWheelchairDesignated = parseBooleanStringOrThrow(columns["isWheelchairDesignated"]!!, "isWheelchairDesignated"),
    isStepFreeDesignated = parseBooleanStringOrThrow(columns["isStepFreeDesignated"]!!, "isStepFreeDesignated"),
    notes = columns["notes"]!!,
  )

  override fun processRow(row: ApprovedPremisesRoomsSeedCsvRow) {
    val premises = findExistingPremisesOrThrow(row)

    val room = createOrUpdateRoom(
      row = row,
      characteristics = characteristicsFromRow(row),
      premises = premises,
    )

    createOrUpdateBed(room, row)
  }

  private fun createOrUpdateRoom(row: ApprovedPremisesRoomsSeedCsvRow, characteristics: List<CharacteristicEntity>, premises: PremisesEntity): RoomEntity {
    val roomCode = "${row.apCode}-${row.roomNumber}"

    val room = when (val existingRoom = roomRepository.findByCode(roomCode)) {
      null -> createRoom(row = row, roomCode = roomCode, premises = premises)
      else -> updateExistingRoom(room = existingRoom, row = row)
    }

    room!!.characteristics.clear()
    room!!.characteristics.addAll(characteristics)
    log.info("Adding characteristics to room: ${room.name}: ${characteristics.map { it.propertyName }}")
    roomRepository.save(room)

    return room
  }

  private fun updateExistingRoom(room: RoomEntity, row: ApprovedPremisesRoomsSeedCsvRow): RoomEntity {
    return room.apply { this!!.notes = row.notes }
  }

  private fun createRoom(row: ApprovedPremisesRoomsSeedCsvRow, premises: PremisesEntity, roomCode: String): RoomEntity? {
    val room = roomRepository.save(
      RoomEntity(
        id = UUID.randomUUID(),
        name = row.roomNumber,
        code = roomCode,
        beds = mutableListOf(),
        premises = premises,
        characteristics = mutableListOf(),
        notes = row.notes,
      ),
    )
    log.info("New room created: ${room.code} (AP: ${row.apCode}) ")

    return room
  }

  private fun createOrUpdateBed(room: RoomEntity, row: ApprovedPremisesRoomsSeedCsvRow): BedEntity {
    val bed = when (val existingBed = bedRepository.findByCode(row.bedCode)) {
      null -> createBed(room, row)
      else -> updateExistingBed(existingBed, row)
    }

    return bed
  }

  private fun createBed(room: RoomEntity, row: ApprovedPremisesRoomsSeedCsvRow): BedEntity {
    val bed = bedRepository.save(
      BedEntity(
        id = UUID.randomUUID(),
        name = "${row.roomNumber} - ${row.bedCount}",
        code = row.bedCode,
        room = room,
        null,
        createdAt = OffsetDateTime.now(),
      ),
    )
    log.info("New bed created: ${row.bedCode} (AP: ${row.apCode} | Room: ${room.code})")

    return bed
  }

  private fun updateExistingBed(bed: BedEntity, row: ApprovedPremisesRoomsSeedCsvRow): BedEntity {
    bed.apply {
      name = "${row.roomNumber} - ${row.bedCount}"
    }

    return bedRepository.save(bed)
  }

  private fun findExistingPremisesOrThrow(row: ApprovedPremisesRoomsSeedCsvRow): PremisesEntity {
    return premisesRepository.findByApCode(row.apCode)
      ?: throw RuntimeException(
        "Error: no premises with apCode '${row.apCode}' found. " +
          "Please seed premises before rooms/beds.",
      )
  }

  private fun characteristicsFromRow(row: ApprovedPremisesRoomsSeedCsvRow): List<CharacteristicEntity> {
    return listOf(
      CharacteristicValue("isSingle", castBooleanString(row.isSingle)),
      CharacteristicValue("isGroundFloor", castBooleanString(row.isGroundFloor)),
      CharacteristicValue("isFullyFm", castBooleanString(row.isFullyFm)),
      CharacteristicValue("hasCrib7Bedding", castBooleanString(row.hasCrib7Bedding)),
      CharacteristicValue("hasSmokeDetector", castBooleanString(row.hasSmokeDetector)),
      CharacteristicValue("isTopFloorVulnerable", castBooleanString(row.isTopFloorVulnerable)),
      CharacteristicValue("isGroundFloorNrOffice", castBooleanString(row.isGroundFloorNrOffice)),
      CharacteristicValue("hasNearbySprinkler", castBooleanString(row.hasNearbySprinkler)),
      CharacteristicValue("isArsonSuitable", castBooleanString(row.isArsonSuitable)),
      CharacteristicValue("isArsonDesignated", castBooleanString(row.isArsonDesignated)),
      CharacteristicValue("hasArsonInsuranceConditions", castBooleanString(row.hasArsonInsuranceConditions)),
      CharacteristicValue("isSuitedForSexOffenders", castBooleanString(row.isSuitedForSexOffenders)),
      CharacteristicValue("hasEnSuite", castBooleanString(row.hasEnSuite)),
      CharacteristicValue("isWheelchairAccessible", castBooleanString(row.isWheelchairAccessible)),
      CharacteristicValue("hasWideDoor", castBooleanString(row.hasWideDoor)),
      CharacteristicValue("hasStepFreeAccess", castBooleanString(row.hasStepFreeAccess)),
      CharacteristicValue("hasFixedMobilityAids", castBooleanString(row.hasFixedMobilityAids)),
      CharacteristicValue("hasTurningSpace", castBooleanString(row.hasTurningSpace)),
      CharacteristicValue("hasCallForAssistance", castBooleanString(row.hasCallForAssistance)),
      CharacteristicValue("isWheelchairDesignated", castBooleanString(row.isWheelchairDesignated)),
      CharacteristicValue("isStepFreeDesignated", castBooleanString(row.isStepFreeDesignated)),
    ).filter { it.value }
      .map {
        characteristicRepository.findByPropertyNameAndScopes(propertyName = it.propertyName, serviceName = "approved-premises", modelName = "room")
          ?: throw RuntimeException("Characteristic '${it.propertyName}' does not exist for AP room")
      }
  }

  private fun parseBooleanStringOrThrow(value: String, fieldName: String): String {
    val booleanString = listOf("YES", "NO").find { it == value.trim().uppercase() }
      ?: throw RuntimeException("'$value' is not a recognised boolean for '$fieldName' (use yes | no)")

    return if (booleanString == "YES") "YES" else "NO"
  }

  private fun castBooleanString(booleanString: String): Boolean {
    return booleanString == "YES"
  }
}

data class ApprovedPremisesRoomsSeedCsvRow(
  val apCode: String,
  val bedCode: String,
  val roomNumber: String,
  val bedCount: String,
  val isSingle: String,
  val isGroundFloor: String,
  val isFullyFm: String,
  val hasCrib7Bedding: String,
  val hasSmokeDetector: String,
  val isTopFloorVulnerable: String,
  val isGroundFloorNrOffice: String,
  val hasNearbySprinkler: String,
  val isArsonSuitable: String,
  val isArsonDesignated: String,
  val hasArsonInsuranceConditions: String,
  val isSuitedForSexOffenders: String,
  val hasEnSuite: String,
  val isWheelchairAccessible: String,
  val hasWideDoor: String,
  val hasStepFreeAccess: String,
  val hasFixedMobilityAids: String,
  val hasTurningSpace: String,
  val hasCallForAssistance: String,
  val isWheelchairDesignated: String,
  val isStepFreeDesignated: String,
  val notes: String?,
)
