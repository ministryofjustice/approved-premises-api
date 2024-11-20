package uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.planning

import java.time.LocalDate
import java.util.UUID

data class Characteristic(
  val label: String,
  val propertyName: String,
  val weighting: Int,
  val singleRoom: Boolean,
)

data class Bed(
  val id: UUID,
  val label: String,
  val room: Room,
)

data class BedDayState(
  val bed: Bed,
  val day: LocalDate,
  val inactiveReason: BedInactiveReason?,
) {
  fun isActive() = inactiveReason == null
}

sealed interface BedInactiveReason
data class BedEnded(val ended: LocalDate) : BedInactiveReason
data class BedOutOfService(val reason: String) : BedInactiveReason

data class Room(
  val id: UUID,
  val label: String,
  val characteristics: Set<Characteristic>,
) {
  fun characteristicsExcludingSingle() = characteristics.filter { !it.singleRoom }
}

data class SpaceBooking(
  val id: UUID,
  val label: String,
  val requiredRoomCharacteristics: Set<Characteristic>,
) {
  fun requiresSingleRoom() = requiredRoomCharacteristics.any { it.singleRoom }
  fun requiredRoomCharacteristicsExcludingSingle() = requiredRoomCharacteristics.filter { !it.singleRoom }
}
