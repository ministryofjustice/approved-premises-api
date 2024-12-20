package uk.gov.justice.digital.hmpps.approvedpremisesapi.util

import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1OverbookingRange
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.planning.SpacePlanningService

class Cas1PremiseOverbookingCalculator {

  fun calculate(overbookedDays: List<SpacePlanningService.PremiseCapacityForDay>): List<Cas1OverbookingRange> {
    if (overbookedDays.isEmpty()) return emptyList()

    val sortedOverBookedDays = overbookedDays.distinctBy { it.day }.sortedBy { it.day }

    val overbookingRanges = mutableListOf<Cas1OverbookingRange>()
    var rangeStart = sortedOverBookedDays.first().day
    var previousDay = rangeStart

    // Calculate consecutive overbooking ranges
    for (current in sortedOverBookedDays.drop(1)) {
      if (current.day != previousDay.plusDays(1)) {
        overbookingRanges.add(
          Cas1OverbookingRange(startInclusive = rangeStart, endInclusive = previousDay),
        )
        rangeStart = current.day
      }
      previousDay = current.day
    }
    // Add the final range
    overbookingRanges.add(
      Cas1OverbookingRange(startInclusive = rangeStart, endInclusive = previousDay),
    )
    return overbookingRanges
  }
}
