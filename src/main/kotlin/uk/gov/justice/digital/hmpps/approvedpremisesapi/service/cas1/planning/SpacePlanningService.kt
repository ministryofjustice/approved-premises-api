package uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.planning

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApprovedPremisesEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.Cas1SpaceBookingEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.Cas1SpaceBookingRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PremisesEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.Cas1OutOfServiceBedService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.DateRange
import java.time.LocalDate
import java.util.UUID

@Service
class SpacePlanningService(
  private val spacePlanningModelsFactory: SpacePlanningModelsFactory,
  private val spaceBookingDayPlanner: SpaceBookingDayPlanner,
  private val outOfServiceBedService: Cas1OutOfServiceBedService,
  private val spaceBookingRepository: Cas1SpaceBookingRepository,
) {

  var log: Logger = LoggerFactory.getLogger(this::class.java)

  data class PlanCriteria(
    val premises: ApprovedPremisesEntity,
    val range: DateRange,
  )

  fun plan(criteria: PlanCriteria): SpacePlan {
    val premises = criteria.premises
    val range = criteria.range

    val bedStatesForEachDay = bedStatesForEachDay(premises, range)
    val bookingsForEachDay = spaceBookingsForEachDay(premises, range)

    val dayPlans = range.orderedDatesInRange().map { day ->
      val bedStates = bedStatesForEachDay[day]!!
      val availableBeds = bedStates.filter { it.isActive() }.map { it.bed }.toSet()
      val bookings = bookingsForEachDay[day]!!.toSet()

      SpaceDayPlan(
        day = day,
        bedStates = bedStates,
        planningResult = spaceBookingDayPlanner.plan(
          availableBeds,
          bookings,
        ),
      )
    }.toList()

    val plan = SpacePlan(
      beds = spacePlanningModelsFactory.allBeds(premises),
      dayPlans = dayPlans,
    )

    log.debug(SpacePlanRenderer.render(criteria, plan))

    return plan
  }

  fun capacity(
    premises: ApprovedPremisesEntity,
    range: DateRange,
    excludeSpaceBookingId: UUID?,
  ): PremiseCapacitySummary {
    val bedStatesForEachDay = bedStatesForEachDay(premises, range)
    val bookingsForEachDay = spaceBookingsForEachDay(premises, range, excludeSpaceBookingId)

    val capacityForEachDay = range.orderedDatesInRange().map { day ->
      val bedStates = bedStatesForEachDay[day]!!
      val availableBeds = bedStates.filter { it.isActive() }
      val bookings = bookingsForEachDay[day]!!
      PremiseCapacityForDay(
        day = day,
        totalBedCount = bedStates.count { it.isActive() || it.isTemporarilyInactive() },
        availableBedCount = availableBeds.size,
        bookingCount = bookings.size,
        characteristicAvailability = Cas1SpaceBookingEntity.Constants.CRITERIA_CHARACTERISTIC_PROPERTY_NAMES_OF_INTEREST.map {
          determineCharacteristicAvailability(
            characteristicPropertyName = it,
            availableBeds = availableBeds,
            bookings = bookings,
          )
        },
      )
    }.toList()

    return PremiseCapacitySummary(
      premise = premises,
      range = range,
      byDay = capacityForEachDay,
    )
  }

  private fun determineCharacteristicAvailability(
    characteristicPropertyName: String,
    availableBeds: List<BedDayState>,
    bookings: List<SpaceBooking>,
  ): PremiseCharacteristicAvailability {
    return PremiseCharacteristicAvailability(
      characteristicPropertyName = characteristicPropertyName,
      availableBedCount = availableBeds.count { it.bed.hasCharacteristic(characteristicPropertyName) },
      bookingCount = bookings.count { it.hasCharacteristic(characteristicPropertyName) },
    )
  }

  private fun bedStatesForEachDay(
    premises: ApprovedPremisesEntity,
    range: DateRange,
  ): Map<LocalDate, List<BedDayState>> {
    val outOfServiceBedRecordsToConsider = outOfServiceBedService.getActiveOutOfServiceBedsForPremisesId(premises.id)

    return range.orderedDatesInRange()
      .toList()
      .associateBy(
        keySelector = { it },
        valueTransform = { day ->
          spacePlanningModelsFactory.allBedsDayState(
            day = day,
            premises = premises,
            outOfServiceBedRecordsToConsider = outOfServiceBedRecordsToConsider,
          )
        },
      )
  }

  private fun spaceBookingsForEachDay(
    premises: ApprovedPremisesEntity,
    range: DateRange,
    excludeSpaceBookingId: UUID? = null,
  ): Map<LocalDate, List<SpaceBooking>> {
    val spaceBookingsToConsider = spaceBookingRepository.findAllBookingsActiveWithinAGivenRangeWithCriteria(
      premisesId = premises.id,
      rangeStartInclusive = range.fromInclusive,
      rangeEndInclusive = range.toInclusive,
    ).filter { it.id != excludeSpaceBookingId }

    return range.orderedDatesInRange()
      .toList()
      .associateBy(
        keySelector = { it },
        valueTransform = { day ->
          spacePlanningModelsFactory.spaceBookingsForDay(
            day = day,
            spaceBookingsToConsider = spaceBookingsToConsider,
          )
        },
      )
  }

  data class SpacePlan(
    val beds: List<Bed>,
    val dayPlans: List<SpaceDayPlan>,
  )

  data class SpaceDayPlan(
    val day: LocalDate,
    val bedStates: List<BedDayState>,
    val planningResult: DayPlannerResult,
  )

  data class PremiseCapacitySummary(
    val premise: PremisesEntity,
    val range: DateRange,
    val byDay: List<PremiseCapacityForDay>,
  )
  data class PremiseCapacityForDay(
    val day: LocalDate,
    val totalBedCount: Int,
    val availableBedCount: Int,
    val bookingCount: Int,
    val characteristicAvailability: List<PremiseCharacteristicAvailability>,
  ) {
    fun isPremiseOverbooked(): Boolean {
      return isPremisesCapacityOverbooked() || characteristicAvailability.any { it.isCharacteristicOverbooked() }
    }

    private fun isPremisesCapacityOverbooked(): Boolean {
      return bookingCount > availableBedCount
    }
  }

  data class PremiseCharacteristicAvailability(
    val characteristicPropertyName: String,
    val availableBedCount: Int,
    val bookingCount: Int,
  ) {

    fun isCharacteristicOverbooked(): Boolean {
      return bookingCount > availableBedCount
    }
  }
}
