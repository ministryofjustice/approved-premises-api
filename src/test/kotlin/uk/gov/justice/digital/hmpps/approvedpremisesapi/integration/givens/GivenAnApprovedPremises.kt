package uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens

import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApprovedPremisesEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.randomStringMultiCaseWithNumbers

fun IntegrationTestBase.givenAnApprovedPremises(
  name: String = randomStringMultiCaseWithNumbers(8),
  supportsSpaceBookings: Boolean = false,
): ApprovedPremisesEntity {
  return approvedPremisesEntityFactory
    .produceAndPersist {
      withName(name)
      withProbationRegion(probationRegionEntityFactory.produceAndPersist())
      withLocalAuthorityArea(localAuthorityEntityFactory.produceAndPersist())
      withSupportsSpaceBookings(supportsSpaceBookings)
    }
}
