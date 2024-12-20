package uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.transformer.cas1

import org.junit.jupiter.params.provider.Arguments
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1SpaceBookingSummaryStatus
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.stream.Stream

data class TestCaseForSpaceBookingSummaryStatus(
  val expectedArrivalDate: LocalDate,
  val actualArrivalDate: LocalDate?,
  val expectedDepartureDate: LocalDate,
  val actualDepartureDate: LocalDate?,
  val nonArrivalConfirmedAtDateTime: LocalDateTime?,
)

class Cas1SpaceBookingSummaryStatusTestHelper {
  private val nowDate: LocalDate = LocalDate.now()
  private val dateWayInTheFuture: LocalDate = nowDate.plusDays(800)

  fun spaceBookingSummaryStatusCases(): Stream<Arguments> {
    return (
      getArrivedCase() +
        getArrivalTodayCase() +
        getOverdueArrivalCases() +
        getArrivalWithin2WeeksCases() +
        getArrivalWithin6WeeksCases() +
        getDepartingTodayCase() +
        getDepartedCase() +
        getOverdueDepartureCases() +
        getDepartingWithin2WeeksCases() +
        getNotArrivedCase()
      ).stream()
  }

  private fun getArrivedCase() = listOf(
    Arguments.of(
      TestCaseForSpaceBookingSummaryStatus(
        expectedArrivalDate = nowDate,
        actualArrivalDate = nowDate,
        expectedDepartureDate = dateWayInTheFuture,
        actualDepartureDate = null,
        nonArrivalConfirmedAtDateTime = null,
      ),
      Cas1SpaceBookingSummaryStatus.arrived,
    ),
  )

  private fun getNotArrivedCase() = listOf(
    Arguments.of(
      TestCaseForSpaceBookingSummaryStatus(
        expectedArrivalDate = nowDate,
        actualArrivalDate = null,
        expectedDepartureDate = dateWayInTheFuture,
        actualDepartureDate = null,
        nonArrivalConfirmedAtDateTime = LocalDateTime.now(),
      ),
      Cas1SpaceBookingSummaryStatus.notArrived,
    ),
  )

  private fun getArrivalTodayCase() = listOf(
    Arguments.of(
      TestCaseForSpaceBookingSummaryStatus(
        expectedArrivalDate = nowDate,
        actualArrivalDate = null,
        expectedDepartureDate = dateWayInTheFuture,
        actualDepartureDate = null,
        nonArrivalConfirmedAtDateTime = null,
      ),
      Cas1SpaceBookingSummaryStatus.arrivingToday,
    ),
  )

  private fun getOverdueArrivalCases() = (1L..14L).toList()
    .map {
      Arguments.of(
        TestCaseForSpaceBookingSummaryStatus(
          expectedArrivalDate = nowDate.minusDays(it),
          actualArrivalDate = null,
          expectedDepartureDate = dateWayInTheFuture,
          actualDepartureDate = null,
          nonArrivalConfirmedAtDateTime = null,
        ),
        Cas1SpaceBookingSummaryStatus.overdueArrival,
      )
    }

  private fun getArrivalWithin2WeeksCases() = (1L..14L).toList()
    .map {
      Arguments.of(
        TestCaseForSpaceBookingSummaryStatus(
          expectedArrivalDate = nowDate.plusDays(it),
          actualArrivalDate = null,
          expectedDepartureDate = dateWayInTheFuture,
          actualDepartureDate = null,
          nonArrivalConfirmedAtDateTime = null,
        ),
        Cas1SpaceBookingSummaryStatus.arrivingWithin2Weeks,
      )
    }

  private fun getArrivalWithin6WeeksCases() = (15L..42L).toList()
    .map {
      Arguments.of(
        TestCaseForSpaceBookingSummaryStatus(
          expectedArrivalDate = nowDate.plusDays(it),
          actualArrivalDate = null,
          expectedDepartureDate = dateWayInTheFuture,
          actualDepartureDate = null,
          nonArrivalConfirmedAtDateTime = null,
        ),
        Cas1SpaceBookingSummaryStatus.arrivingWithin6Weeks,
      )
    }

  private fun getDepartingTodayCase(): List<Arguments> {
    val date1dayAgo = nowDate.minusDays(1)
    return listOf(
      Arguments.of(
        TestCaseForSpaceBookingSummaryStatus(
          expectedArrivalDate = date1dayAgo,
          actualArrivalDate = date1dayAgo,
          expectedDepartureDate = nowDate,
          actualDepartureDate = null,
          nonArrivalConfirmedAtDateTime = null,
        ),
        Cas1SpaceBookingSummaryStatus.departingToday,
      ),
    )
  }

  private fun getDepartedCase(): List<Arguments> {
    val date1dayAgo = nowDate.minusDays(1)
    return listOf(
      Arguments.of(
        TestCaseForSpaceBookingSummaryStatus(
          expectedArrivalDate = date1dayAgo,
          actualArrivalDate = date1dayAgo,
          expectedDepartureDate = nowDate,
          actualDepartureDate = nowDate,
          nonArrivalConfirmedAtDateTime = null,
        ),
        Cas1SpaceBookingSummaryStatus.departed,
      ),
    )
  }

  private fun getOverdueDepartureCases() = (1L..14L).toList()
    .map {
      Arguments.of(
        TestCaseForSpaceBookingSummaryStatus(
          expectedArrivalDate = nowDate.minusDays(100),
          actualArrivalDate = nowDate.minusDays(100),
          expectedDepartureDate = nowDate.minusDays(it),
          actualDepartureDate = null,
          nonArrivalConfirmedAtDateTime = null,
        ),
        Cas1SpaceBookingSummaryStatus.overdueDeparture,
      )
    }

  private fun getDepartingWithin2WeeksCases() = (1L..14L).toList()
    .map {
      Arguments.of(
        TestCaseForSpaceBookingSummaryStatus(
          expectedArrivalDate = nowDate.minusDays(100),
          actualArrivalDate = nowDate.minusDays(100),
          expectedDepartureDate = nowDate.plusDays(it),
          actualDepartureDate = null,
          nonArrivalConfirmedAtDateTime = null,
        ),
        Cas1SpaceBookingSummaryStatus.departingWithin2Weeks,
      )
    }
}
