package uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.cas1

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1PremiseCapacity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1PremiseCapacityForDay
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1PremiseCharacteristicAvailability
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1SpaceBookingCharacteristic
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.Cas1PremisesService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.planning.SpacePlanningService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.planning.SpacePlanningService.PremiseCapacitySummary

@Component
class Cas1PremiseCapacitySummaryTransformer(
  val cas1PremisesTransformer: Cas1PremisesTransformer,
) {

  fun toCas1PremiseCapacitySummary(
    premiseSummaryInfo: Cas1PremisesService.Cas1PremisesSummaryInfo,
    premiseCapacity: PremiseCapacitySummary,
  ) = Cas1PremiseCapacity(
    premise = cas1PremisesTransformer.toPremiseSummary(premiseSummaryInfo),
    startDate = premiseCapacity.range.fromInclusive,
    endDate = premiseCapacity.range.toInclusive,
    capacity = premiseCapacity.byDay.map { it.toApiType() },
  )

  private fun SpacePlanningService.PremiseCapacityForDay.toApiType() = Cas1PremiseCapacityForDay(
    date = day,
    totalBedCount = totalBedCount,
    availableBedCount = availableBedCount,
    bookingCount = bookingCount,
    characteristicAvailability = characteristicAvailability.map { it.toApiType() },
  )

  private fun SpacePlanningService.PremiseCharacteristicAvailability.toApiType() =
    Cas1PremiseCharacteristicAvailability(
      characteristic = Cas1SpaceBookingCharacteristic.entries.first { it.value == this.characteristicPropertyName },
      availableBedsCount = availableBedCount,
      bookingsCount = bookingCount,
    )
}
