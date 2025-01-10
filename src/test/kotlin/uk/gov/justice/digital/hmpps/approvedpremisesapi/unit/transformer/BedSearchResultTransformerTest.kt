package uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.transformer

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.TemporaryAccommodationBedSearchResultFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.repository.CharacteristicNames
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.BedSearchResultTransformer
import java.util.UUID

class BedSearchResultTransformerTest {
  private val bedSearchTransformer = BedSearchResultTransformer()

  @Test
  fun `transformDomainToApi transforms correctly for Temporary Accommodation results`() {
    val premisesOne = UUID.fromString("e17d7c19-dabb-4c6d-83b9-ada34d290b56")
    val premisesTwo = UUID.fromString("d9a81794-7000-4438-95e8-9f4aa7de5028")

    val roomOne = UUID.fromString("3d76ea31-e04d-4152-9c56-4e26b15306e6")
    val roomTwo = UUID.fromString("d856ecdd-89b3-4ddb-83a6-34914d39d3f6")
    val roomThree = UUID.fromString("3b805d1f-5632-4e77-87e3-34938cf9ade2")

    val domainResults = listOf(
      TemporaryAccommodationBedSearchResultFactory()
        .withPremisesId(premisesOne)
        .withRoomId(roomOne)
        .withPremisesBedCount(6)
        .produce(),
      TemporaryAccommodationBedSearchResultFactory()
        .withPremisesId(premisesOne)
        .withRoomId(roomTwo)
        .withPremisesBedCount(6)
        .produce(),
      TemporaryAccommodationBedSearchResultFactory()
        .withPremisesId(premisesTwo)
        .withRoomId(roomThree)
        .withPremisesBedCount(6)
        .produce(),
      TemporaryAccommodationBedSearchResultFactory()
        .withPremisesId(premisesTwo)
        .withRoomId(roomThree)
        .withPremisesBedCount(6)
        .produce(),
    )

    val searchResults = bedSearchTransformer.transformDomainToApi(domainResults)

    assertThat(searchResults.resultsBedCount).isEqualTo(4)
    assertThat(searchResults.resultsPremisesCount).isEqualTo(2)
    assertThat(searchResults.resultsRoomCount).isEqualTo(3)

    searchResults.results.forEachIndexed { index, result ->
      val domainResult = domainResults[index]

      assertThat(domainResult.premisesId).isEqualTo(result.premises.id)
      assertThat(domainResult.premisesName).isEqualTo(result.premises.name)
      assertThat(domainResult.premisesAddressLine1).isEqualTo(result.premises.addressLine1)
      assertThat(domainResult.premisesAddressLine2).isEqualTo(result.premises.addressLine2)
      assertThat(domainResult.premisesTown).isEqualTo(result.premises.town)
      assertThat(domainResult.premisesPostcode).isEqualTo(result.premises.postcode)
      assertThat(domainResult.premisesCharacteristics).isEqualTo(result.premises.characteristics.map { c -> CharacteristicNames(c.propertyName, c.name) })
      assertThat(domainResult.premisesBedCount).isEqualTo(result.premises.bedCount)
      assertThat(domainResult.roomId).isEqualTo(result.room.id)
      assertThat(domainResult.roomName).isEqualTo(result.room.name)
      assertThat(domainResult.bedId).isEqualTo(result.bed.id)
      assertThat(domainResult.bedName).isEqualTo(result.bed.name)
      assertThat(domainResult.roomCharacteristics).isEqualTo(result.room.characteristics.map { c -> CharacteristicNames(c.propertyName, c.name) })
      assertThat(domainResult.overlaps).isEqualTo(result.overlaps)
    }
  }
}
