package uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.BedSearchResultBedSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.BedSearchResultPremisesSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.BedSearchResultRoomSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.BedSearchResults
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.CharacteristicPair
import uk.gov.justice.digital.hmpps.approvedpremisesapi.repository.Cas3BedSearchResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.TemporaryAccommodationBedSearchResult as ApiTemporaryAccommodationBedSearchResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.TemporaryAccommodationBedSearchResultOverlap as ApiTemporaryAccommodationBedSearchResultOverlap

@Component
class BedSearchResultTransformer {
  fun transformDomainToApi(results: List<Cas3BedSearchResult>) = BedSearchResults(
    resultsRoomCount = results.distinctBy { it.roomId }.size,
    resultsPremisesCount = results.distinctBy { it.premisesId }.size,
    resultsBedCount = results.size,
    results = results.map(::transformResult),
  )

  private fun transformResult(result: Cas3BedSearchResult) = ApiTemporaryAccommodationBedSearchResult(
    premises = BedSearchResultPremisesSummary(
      id = result.premisesId,
      name = result.premisesName,
      addressLine1 = result.premisesAddressLine1,
      postcode = result.premisesPostcode,
      characteristics = result.premisesCharacteristics.map {
        CharacteristicPair(
          name = it.name,
          propertyName = it.propertyName,
        )
      },
      addressLine2 = result.premisesAddressLine2,
      town = result.premisesTown,
      probationDeliveryUnitName = result.probationDeliveryUnitName,
      notes = result.premisesNotes,
      bedCount = result.premisesBedCount,
      bookedBedCount = result.bookedBedCount,
    ),
    room = BedSearchResultRoomSummary(
      id = result.roomId,
      name = result.roomName,
      characteristics = result.roomCharacteristics.map {
        CharacteristicPair(
          name = it.name,
          propertyName = it.propertyName,
        )
      },
    ),
    bed = BedSearchResultBedSummary(
      id = result.bedId,
      name = result.bedName,
    ),
    overlaps = result.overlaps.map {
      ApiTemporaryAccommodationBedSearchResultOverlap(
        name = it.name,
        crn = it.crn,
        personType = it.personType,
        days = it.days,
        bookingId = it.bookingId,
        roomId = it.roomId,
        sex = it.sex,
        assessmentId = it.assessmentId,
      )
    },
  )
}
