package uk.gov.justice.digital.hmpps.approvedpremisesapi.seed.cas1

import org.jetbrains.kotlinx.dataframe.DataFrame
import org.jetbrains.kotlinx.dataframe.api.getColumn
import org.jetbrains.kotlinx.dataframe.name
import org.slf4j.LoggerFactory
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ServiceName
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApprovedPremisesRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.BedEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.BedRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.CharacteristicEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.CharacteristicRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PremisesEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.RoomEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.RoomRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.seed.ExcelSeedJob
import java.util.UUID

class SiteSurveyImportException(message: String) : Exception(message)

@Component
@Suppress("LongParameterList")
class ApprovedPremisesRoomsSeedFromXLSXJob(
  private val approvedPremisesRepository: ApprovedPremisesRepository,
  private val roomRepository: RoomRepository,
  private val bedRepository: BedRepository,
  private val siteSurvey: SiteSurvey,
) : ExcelSeedJob {
  private val log = LoggerFactory.getLogger(this::class.java)
  override fun processDataFrame(roomsWorksheet: DataFrame<*>, premisesId: UUID) {
    findExistingPremisesOrThrow(premisesId)

    var rooms = buildRooms(roomsWorksheet, premisesId)
    val characteristics = buildCharacteristics(roomsWorksheet)

    rooms.forEach {
      characteristics[it.code]?.let { roomCharacteristics -> it.characteristics.addAll(roomCharacteristics.toList()) }
    }

    rooms = createOrUpdateRooms(rooms)

    val beds = buildBeds(roomsWorksheet, rooms)
    createBedsIfNotExist(beds)
  }

  private fun buildRooms(dataFrame: DataFrame<*>, premisesId: UUID): MutableList<RoomEntity> {
    val rooms = mutableListOf<RoomEntity>()

    for (i in 1..<dataFrame.columnsCount()) {
      val roomAnswers = dataFrame.getColumn(i)
      val room = buildRoom(premisesId, roomCode = roomAnswers.name, roomName = roomAnswers[0].toString())

      rooms.add(room)
    }
    return rooms
  }

  private fun buildCharacteristics(dataFrame: DataFrame<*>): MutableMap<String, MutableList<CharacteristicEntity>> {
    var premisesCharacteristics = mutableMapOf<String, MutableList<CharacteristicEntity>>()

    siteSurvey.questionToCharacterEntityMapping.forEach { (question, characteristic) ->
      val rowId = dataFrame.getColumn(0).values().indexOf(question)

      if (rowId == -1) throw SiteSurveyImportException("Characteristic question '$question' not found on sheet Sheet3.")

      for (colId in 1..<dataFrame.columnsCount()) {
        val roomCode = dataFrame.getColumn(colId).name
        val answer = dataFrame[rowId][colId].toString().trim()

        if (answer.equals("yes", ignoreCase = true)) {
          premisesCharacteristics.computeIfAbsent(roomCode) { mutableListOf() }.add(characteristic!!)
        } else if (!answer.equals("no", ignoreCase = true) && !answer.equals("N/A", ignoreCase = true)) {
          throw SiteSurveyImportException("Expecting 'yes' or 'no' for question '$question' but is '$answer' on sheet Sheet3 (row = ${rowId + 1}, col = $colId).")
        }
      }
    }
    return premisesCharacteristics
  }

  private fun buildBeds(dataFrame: DataFrame<*>, rooms: MutableList<RoomEntity>): List<BedEntity> {
    val beds = mutableListOf<BedEntity>()
    for (i in 1..<dataFrame.columnsCount()) {
      val roomAnswers = dataFrame.getColumn(i)
      val bedNumber = roomAnswers[1].toString()
      beds.add(
        buildBed(
          bedName = "${roomAnswers.name}",
          bedCode = "${roomAnswers.name} - $bedNumber",
          room = rooms.firstOrNull { it.code == roomAnswers.name }
            ?: throw IllegalArgumentException("Room not found with id ${roomAnswers.name} and code ${roomAnswers.name}."),
        ),
      )
    }
    return beds
  }

  private fun createOrUpdateRooms(rooms: MutableList<RoomEntity>): MutableList<RoomEntity> {
    rooms.forEachIndexed { index, room ->
      var persistedRoom = roomRepository.findByCodeAndPremisesId(room.code!!, room.premises.id)

      if (persistedRoom == null) {
        roomRepository.save(room)
        log.info("Created new room ${room.id} with code ${room.code} and name ${room.name} in premise ${room.premises.name}.")
        log.info("Added characteristic(s) ${room.characteristics.joinToString()} to room code ${room.code}.")
      } else {
        rooms[index] = persistedRoom
        persistedRoom.characteristics.clear()
        persistedRoom.characteristics.addAll(room.characteristics)
        roomRepository.save(persistedRoom)
        log.info("Added characteristic(s) ${persistedRoom.characteristics.joinToString()} to room code ${persistedRoom.code}.")
      }
    }
    return rooms
  }

  private fun createBedsIfNotExist(beds: List<BedEntity>) {
    beds.forEach {
      val existingBed = bedRepository.findByCodeAndRoomId(it.code!!, it.room.id)
      if (existingBed != null) {
        log.info("Bed ${it.id} with code ${it.code} already exists in room code ${it.room.code}.")
      } else {
        bedRepository.save(it)
        log.info("Created new bed ${it.id} with code ${it.code} and name ${it.name} in room code ${it.room.code}.")
      }
    }
  }

  private fun buildRoom(premisesId: UUID, roomCode: String, roomName: String): RoomEntity = RoomEntity(
    id = UUID.randomUUID(),
    name = roomName,
    code = roomCode,
    notes = null,
    premises = approvedPremisesRepository.findByIdOrNull(premisesId)!!,
    beds = mutableListOf(),
    characteristics = mutableListOf(),
  )

  fun buildBed(bedName: String, bedCode: String, room: RoomEntity): BedEntity = BedEntity(
    id = UUID.randomUUID(),
    name = bedName,
    code = bedCode,
    room = room,
    endDate = null,
    createdAt = null,
  )

  private fun findExistingPremisesOrThrow(premisesId: UUID): PremisesEntity {
    return approvedPremisesRepository.findByIdOrNull(premisesId) ?: throw SiteSurveyImportException(
      "No premises with id '$premisesId' found.",
    )
  }
}

@Component
class SiteSurvey(characteristicRepository: CharacteristicRepository) {
  private val questionToPropertyNameMapping = mapOf(
    "Is this bed in a single room?" to "isSingle",
    "Is this room located on the ground floor?" to "isGroundFloor",
    "Is the room using only furnishings and bedding supplied by FM?" to "isFullyFm",
    "Does this room have Crib7 rated bedding?" to "hasCrib7Bedding",
    "Is there a smoke/heat detector in the room?" to "hasSmokeDetector",
    "Is this room on the top floor with at least one external wall and not located directly next to a fire exit or a protected stairway?" to "isTopFloorVulnerable",
    "Is the room close to the admin/staff office on the ground floor with at least one external wall and not located directly next to a fire exit or a protected stairway?"
      to "isGroundFloorNrOffice",
    "is there a water mist extinguisher in close proximity to this room?" to "hasNearbySprinkler",
    "Is this room suitable for people who pose an arson risk? (Must answer yes to Q; 6 & 7, and 9 or  10)" to "isArsonSuitable",
    "Is this room currently a designated arson room?" to "isArsonDesignated",
    "If IAP - Is there any insurance conditions that prevent a person with arson convictions being placed?" to "hasArsonInsuranceConditions",
    "Is this room suitable for people convicted of sexual offences?" to "isSuitedForSexOffenders",
    "Does this room have en-suite bathroom facilities?" to "hasEnSuite",
    "Are corridors leading to this room of sufficient width to accommodate a wheelchair? (at least 1.2m wide)" to "isWheelchairAccessible",
    "Is the door to this room at least 900mm wide?" to "hasWideDoor",
    "Is there step free access to this room and in corridors leading to this room?" to "hasStepFreeAccess",
    "Are there fixed mobility aids in this room?" to "hasFixedMobilityAids",
    "Does this room have at least a 1500mmx1500mm turning space?" to "hasTurningSpace",
    "Is there provision for people to call for assistance from this room?" to "hasCallForAssistance",
    "Can this room be designated as suitable for wheelchair users?   Must answer yes to Q23-26 on previous sheet and Q17-19 & 21 on this sheet)" to "isWheelchairDesignated",
    "Can this room be designated as suitable for people requiring step free access? (Must answer yes to Q23 and 25 on previous sheet and Q19 on this sheet)" to "isStepFreeDesignated",
  )

  val questionToCharacterEntityMapping = questionToPropertyNameMapping.map { (key, value) ->
    Pair(key, characteristicRepository.findByPropertyName(value, ServiceName.approvedPremises.value))
  }.toMap()
}
