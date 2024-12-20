package uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.transformer.cas1

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1SpaceBookingSummaryStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.SpaceBookingDates
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.cas1.Cas1SpaceBookingStatusTransformer
import java.util.stream.Stream

class Cas1SpaceBookingStatusTransformerTest {

  private val transformer = Cas1SpaceBookingStatusTransformer()

  @ParameterizedTest
  @MethodSource("spaceBookingSummaryStatusCases")
  fun `Space booking status is transformed correctly`(
    testCaseForSpaceBookingSummaryStatus: TestCaseForSpaceBookingSummaryStatus,
    expectedStatus: Cas1SpaceBookingSummaryStatus,
  ) {
    val result = transformer.transformToSpaceBookingSummaryStatus(
      SpaceBookingDates(
        testCaseForSpaceBookingSummaryStatus.expectedArrivalDate,
        testCaseForSpaceBookingSummaryStatus.expectedDepartureDate,
        testCaseForSpaceBookingSummaryStatus.actualArrivalDate,
        testCaseForSpaceBookingSummaryStatus.actualDepartureDate,
        testCaseForSpaceBookingSummaryStatus.nonArrivalConfirmedAtDateTime,
      ),
    )
    assertThat(result).isEqualTo(expectedStatus)
  }

  companion object {
    @JvmStatic
    fun spaceBookingSummaryStatusCases(): Stream<Arguments> {
      return Cas1SpaceBookingSummaryStatusTestHelper().spaceBookingSummaryStatusCases()
    }
  }
}
