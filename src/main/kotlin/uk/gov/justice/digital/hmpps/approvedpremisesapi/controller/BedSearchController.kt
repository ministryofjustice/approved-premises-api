package uk.gov.justice.digital.hmpps.approvedpremisesapi.controller
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.BedsApiDelegate
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ApprovedPremisesBedSearchParameters
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.BedSearchParameters
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.BedSearchResults
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.TemporaryAccommodationBedSearchParameters
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.BedSearchService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.UserService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.BedSearchResultTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.extractEntityFromCasResult

@Service
class BedSearchController(
  private val userService: UserService,
  private val bedSearchService: BedSearchService,
  private val bedSearchResultTransformer: BedSearchResultTransformer,
) : BedsApiDelegate {
  override fun bedsSearchPost(bedSearchParameters: BedSearchParameters): ResponseEntity<BedSearchResults> {
    val user = userService.getUserForRequest()

    val searchResult = when (bedSearchParameters) {
      is ApprovedPremisesBedSearchParameters -> bedSearchService.findApprovedPremisesBeds(
        user = user,
        maxDistanceMiles = bedSearchParameters.maxDistanceMiles,
        startDate = bedSearchParameters.startDate,
        durationInDays = bedSearchParameters.durationDays,
        requiredCharacteristics = bedSearchParameters.requiredCharacteristics,
        postcodeDistrictOutcode = bedSearchParameters.postcodeDistrict,
      )
      is TemporaryAccommodationBedSearchParameters -> bedSearchService.findTemporaryAccommodationBeds(
        user = user,
        probationDeliveryUnits = bedSearchParameters.probationDeliveryUnits,
        startDate = bedSearchParameters.startDate,
        durationInDays = bedSearchParameters.durationDays,
        propertyBedAttributes = bedSearchParameters.attributes,
      )
      else -> throw RuntimeException("Unsupported BedSearchParameters type: ${bedSearchParameters::class.qualifiedName}")
    }

    return ResponseEntity.ok(
      bedSearchResultTransformer.transformDomainToApi(extractEntityFromCasResult(searchResult)),
    )
  }
}
