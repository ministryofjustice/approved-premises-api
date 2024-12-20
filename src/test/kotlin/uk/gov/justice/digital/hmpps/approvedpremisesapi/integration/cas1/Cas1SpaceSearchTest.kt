package uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.cas1

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import org.springframework.beans.factory.annotation.Autowired
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ApType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1SpaceCharacteristic
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1SpaceSearchParameters
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1SpaceSearchRequirements
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1SpaceSearchResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1SpaceSearchResults
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ServiceName
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.InitialiseDatabasePerClassTestBase
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenAProbationRegion
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenAUser
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenAnApplication
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApprovedPremisesEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApprovedPremisesGender
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.cas1.Cas1SpaceSearchResultsTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.randomInt
import java.time.LocalDate

class Cas1SpaceSearchTest : InitialiseDatabasePerClassTestBase() {
  @Autowired
  lateinit var transformer: Cas1SpaceSearchResultsTransformer

  @BeforeEach
  fun setup() {
    postCodeDistrictRepository.deleteAll()
    roomRepository.deleteAll()
    approvedPremisesRepository.deleteAll()
  }

  @Test
  fun `Search for Spaces without JWT returns 401`() {
    webTestClient.post()
      .uri("/cas1/spaces/search")
      .exchange()
      .expectStatus()
      .isUnauthorized
  }

  @Test
  fun `Search for Spaces returns OK with correct body, returning only premises supporting space bookings`() {
    postCodeDistrictFactory.produceAndPersist {
      withOutcode("SE1")
      withLatitude(-0.07)
      withLongitude(51.48)
    }

    givenAUser { user, jwt ->
      val application = givenAnApplication(createdByUser = user, isWomensApplication = false)

      val premises = approvedPremisesEntityFactory.produceAndPersistMultipleIndexed(5) {
        withYieldedProbationRegion { givenAProbationRegion() }
        withYieldedLocalAuthorityArea {
          localAuthorityEntityFactory.produceAndPersist()
        }
        withLatitude((it * -0.01) - 0.08)
        withLongitude((it * 0.01) + 51.49)
        withSupportsSpaceBookings(true)
      }

      // premise that doesn't support space bookings
      approvedPremisesEntityFactory.produceAndPersist {
        withYieldedProbationRegion { givenAProbationRegion() }
        withYieldedLocalAuthorityArea {
          localAuthorityEntityFactory.produceAndPersist()
        }
        withLatitude((-0.01) - 0.08)
        withLongitude((0.01) + 51.49)
        withSupportsSpaceBookings(false)
      }

      val searchParameters = Cas1SpaceSearchParameters(
        applicationId = application.id,
        startDate = LocalDate.now(),
        durationInDays = 14,
        targetPostcodeDistrict = "SE1",
        requirements = Cas1SpaceSearchRequirements(
          apTypes = null,
          spaceCharacteristics = null,
        ),
      )

      val response = webTestClient.post()
        .uri("/cas1/spaces/search")
        .header("Authorization", "Bearer $jwt")
        .bodyValue(searchParameters)
        .exchange()
        .expectStatus()
        .isOk
        .returnResult(Cas1SpaceSearchResults::class.java)

      val results = response.responseBody.blockFirst()!!

      assertThat(results.resultsCount).isEqualTo(5)
      assertThat(results.searchCriteria).isEqualTo(searchParameters)

      assertThatResultMatches(results.results[0], premises[0])
      assertThatResultMatches(results.results[1], premises[1])
      assertThatResultMatches(results.results[2], premises[2])
      assertThatResultMatches(results.results[3], premises[3])
      assertThatResultMatches(results.results[4], premises[4])
    }
  }

  @ParameterizedTest
  @EnumSource
  fun `Only returns APs matching associated gender in application`(gender: ApprovedPremisesGender) {
    postCodeDistrictFactory.produceAndPersist {
      withOutcode("SE1")
      withLatitude(-0.07)
      withLongitude(51.48)
    }

    givenAUser { user, jwt ->
      val application = givenAnApplication(createdByUser = user, isWomensApplication = gender == ApprovedPremisesGender.WOMAN)

      val expectedPremises = approvedPremisesEntityFactory.produceAndPersistMultipleIndexed(5) {
        withYieldedProbationRegion { givenAProbationRegion() }
        withYieldedLocalAuthorityArea {
          localAuthorityEntityFactory.produceAndPersist()
        }
        withLatitude((it * -0.01) - 0.08)
        withLongitude((it * 0.01) + 51.49)
        withSupportsSpaceBookings(true)
        withGender(gender)
      }

      val unexpectedPremises = approvedPremisesEntityFactory.produceAndPersistMultipleIndexed(5) {
        withYieldedProbationRegion { givenAProbationRegion() }
        withYieldedLocalAuthorityArea {
          localAuthorityEntityFactory.produceAndPersist()
        }
        withLatitude((it * -0.01) - 0.08)
        withLongitude((it * 0.01) + 51.49)
        withSupportsSpaceBookings(true)
        withGender(ApprovedPremisesGender.entries.first { it != gender })
      }

      val searchParameters = Cas1SpaceSearchParameters(
        applicationId = application.id,
        startDate = LocalDate.now(),
        durationInDays = 14,
        targetPostcodeDistrict = "SE1",
        requirements = Cas1SpaceSearchRequirements(
          apTypes = null,
          spaceCharacteristics = null,
        ),
      )

      val response = webTestClient.post()
        .uri("/cas1/spaces/search")
        .header("Authorization", "Bearer $jwt")
        .bodyValue(searchParameters)
        .exchange()
        .expectStatus()
        .isOk
        .returnResult(Cas1SpaceSearchResults::class.java)

      val results = response.responseBody.blockFirst()!!

      assertThat(results.resultsCount).isEqualTo(5)
      assertThat(results.searchCriteria).isEqualTo(searchParameters)

      assertThatResultMatches(results.results[0], expectedPremises[0])
      assertThatResultMatches(results.results[1], expectedPremises[1])
      assertThatResultMatches(results.results[2], expectedPremises[2])
      assertThatResultMatches(results.results[3], expectedPremises[3])
      assertThatResultMatches(results.results[4], expectedPremises[4])
    }
  }

  @ParameterizedTest
  @EnumSource
  fun `Filtering APs by AP type returns only APs of that type`(apType: ApType) {
    postCodeDistrictFactory.produceAndPersist {
      withOutcode("SE1")
      withLatitude(-0.07)
      withLongitude(51.48)
    }

    givenAUser { user, jwt ->
      val application = givenAnApplication(createdByUser = user, isWomensApplication = false)

      val expectedPremises = approvedPremisesEntityFactory.produceAndPersistMultipleIndexed(5) {
        withYieldedProbationRegion { givenAProbationRegion() }
        withYieldedLocalAuthorityArea {
          localAuthorityEntityFactory.produceAndPersist()
        }
        withLatitude((it * -0.01) - 0.08)
        withLongitude((it * 0.01) + 51.49)
        withCharacteristicsList(listOfNotNull(apType.asCharacteristicEntity()))
        withSupportsSpaceBookings(true)
      }

      val unexpectedPremises = approvedPremisesEntityFactory.produceAndPersistMultipleIndexed(5) {
        withYieldedProbationRegion { givenAProbationRegion() }
        withYieldedLocalAuthorityArea {
          localAuthorityEntityFactory.produceAndPersist()
        }
        withLatitude((it * -0.01) - 0.08)
        withLongitude((it * 0.01) + 51.49)
        withCharacteristicsList(listOfNotNull(ApType.entries.first { it != apType }.asCharacteristicEntity()))
        withSupportsSpaceBookings(true)
      }

      val searchParameters = Cas1SpaceSearchParameters(
        applicationId = application.id,
        startDate = LocalDate.now(),
        durationInDays = 14,
        targetPostcodeDistrict = "SE1",
        requirements = Cas1SpaceSearchRequirements(
          apTypes = listOf(apType),
          spaceCharacteristics = null,
        ),
      )

      val response = webTestClient.post()
        .uri("/cas1/spaces/search")
        .header("Authorization", "Bearer $jwt")
        .bodyValue(searchParameters)
        .exchange()
        .expectStatus()
        .isOk
        .returnResult(Cas1SpaceSearchResults::class.java)

      val results = response.responseBody.blockFirst()!!

      assertThat(results.resultsCount).isEqualTo(5)
      assertThat(results.searchCriteria).isEqualTo(searchParameters)

      val expectedApType = if (apType == ApType.mhapElliottHouse) {
        ApType.mhapStJosephs
      } else {
        apType
      }

      assertThatResultMatches(results.results[0], expectedPremises[0], expectedApType)
      assertThatResultMatches(results.results[1], expectedPremises[1], expectedApType)
      assertThatResultMatches(results.results[2], expectedPremises[2], expectedApType)
      assertThatResultMatches(results.results[3], expectedPremises[3], expectedApType)
      assertThatResultMatches(results.results[4], expectedPremises[4], expectedApType)
    }
  }

  @Test
  fun `Filtering APs by multiple AP types returns APs of any specified type`() {
    postCodeDistrictFactory.produceAndPersist {
      withOutcode("SE1")
      withLatitude(-0.07)
      withLongitude(51.48)
    }

    givenAUser { user, jwt ->
      val application = givenAnApplication(createdByUser = user, isWomensApplication = false)
      val expectedPremises = approvedPremisesEntityFactory.produceAndPersistMultipleIndexed(4) {
        withYieldedProbationRegion { givenAProbationRegion() }
        withYieldedLocalAuthorityArea {
          localAuthorityEntityFactory.produceAndPersist()
        }
        withLatitude((it * -0.01) - 0.08)
        withLongitude((it * 0.01) + 51.49)
        withCharacteristicsList(listOfNotNull(ApType.entries[it - 1].asCharacteristicEntity()))
        withSupportsSpaceBookings(true)
      }

      val unexpectedPremises = approvedPremisesEntityFactory.produceAndPersistMultipleIndexed(5) {
        withYieldedProbationRegion { givenAProbationRegion() }
        withYieldedLocalAuthorityArea {
          localAuthorityEntityFactory.produceAndPersist()
        }
        withLatitude((it * -0.01) - 0.08)
        withLongitude((it * 0.01) + 51.49)
        withCharacteristicsList(listOfNotNull(ApType.entries[5].asCharacteristicEntity()))
        withSupportsSpaceBookings(true)
      }

      val searchParameters = Cas1SpaceSearchParameters(
        applicationId = application.id,
        startDate = LocalDate.now(),
        durationInDays = 14,
        targetPostcodeDistrict = "SE1",
        requirements = Cas1SpaceSearchRequirements(
          apTypes = ApType.entries.slice(0..3),
          spaceCharacteristics = null,
        ),
      )

      val response = webTestClient.post()
        .uri("/cas1/spaces/search")
        .header("Authorization", "Bearer $jwt")
        .bodyValue(searchParameters)
        .exchange()
        .expectStatus()
        .isOk
        .returnResult(Cas1SpaceSearchResults::class.java)

      val results = response.responseBody.blockFirst()!!

      assertThat(results.resultsCount).isEqualTo(4)
      assertThat(results.searchCriteria).isEqualTo(searchParameters)

      assertThatResultMatches(results.results[0], expectedPremises[0], ApType.normal)
      assertThatResultMatches(results.results[1], expectedPremises[1], ApType.pipe)
      assertThatResultMatches(results.results[2], expectedPremises[2], ApType.esap)
      assertThatResultMatches(results.results[3], expectedPremises[3], ApType.rfap)
    }
  }

  private fun assertThatResultMatches(
    actual: Cas1SpaceSearchResult,
    expected: ApprovedPremisesEntity,
    expectedApType: ApType = ApType.normal,
  ) {
    assertThat(actual.spacesAvailable).isEmpty()
    assertThat(actual.distanceInMiles).isGreaterThan(0f.toBigDecimal())
    assertThat(actual.premises).isNotNull
    assertThat(actual.premises!!.id).isEqualTo(expected.id)
    assertThat(actual.premises!!.apType).isEqualTo(expectedApType)
    assertThat(actual.premises!!.name).isEqualTo(expected.name)
    assertThat(actual.premises!!.addressLine1).isEqualTo(expected.addressLine1)
    assertThat(actual.premises!!.addressLine2).isEqualTo(expected.addressLine2)
    assertThat(actual.premises!!.town).isEqualTo(expected.town)
    assertThat(actual.premises!!.postcode).isEqualTo(expected.postcode)
    assertThat(actual.premises!!.premisesCharacteristics).isEmpty()
  }

  @ParameterizedTest
  @EnumSource
  fun `Filtering APs by characteristic only returns APs with that characteristic`(characteristic: Cas1SpaceCharacteristic) {
    postCodeDistrictFactory.produceAndPersist {
      withOutcode("SE1")
      withLatitude(-0.07)
      withLongitude(51.48)
    }

    givenAUser { user, jwt ->
      val application = givenAnApplication(createdByUser = user, isWomensApplication = false)

      val expectedPremises = approvedPremisesEntityFactory.produceAndPersistMultipleIndexed(5) {
        withYieldedProbationRegion { givenAProbationRegion() }
        withYieldedLocalAuthorityArea {
          localAuthorityEntityFactory.produceAndPersist()
        }
        withLatitude((it * -0.01) - 0.08)
        withLongitude((it * 0.01) + 51.49)
        withCharacteristicsList(listOf(characteristic.asCharacteristicEntity()))
        withSupportsSpaceBookings(true)
      }

      expectedPremises.forEach {
        roomEntityFactory.produceAndPersistMultiple(amount = 5) {
          withPremises(it)
          withCharacteristicsList(listOf(characteristic.asCharacteristicEntity()))
        }
      }

      val unexpectedPremises = approvedPremisesEntityFactory.produceAndPersistMultipleIndexed(5) {
        withYieldedProbationRegion { givenAProbationRegion() }
        withYieldedLocalAuthorityArea {
          localAuthorityEntityFactory.produceAndPersist()
        }
        withLatitude((it * -0.01) - 0.08)
        withLongitude((it * 0.01) + 51.49)
        withCharacteristicsList(
          listOf(
            Cas1SpaceCharacteristic.entries.first { it != characteristic }.asCharacteristicEntity(),
          ),
        )
        withSupportsSpaceBookings(true)
      }

      unexpectedPremises.forEach {
        roomEntityFactory.produceAndPersist {
          withPremises(it)
          withCharacteristicsList(
            listOf(
              Cas1SpaceCharacteristic.entries.first { it != characteristic }.asCharacteristicEntity(),
            ),
          )
        }
      }

      val searchParameters = Cas1SpaceSearchParameters(
        applicationId = application.id,
        startDate = LocalDate.now(),
        durationInDays = 14,
        targetPostcodeDistrict = "SE1",
        requirements = Cas1SpaceSearchRequirements(
          apTypes = null,
          spaceCharacteristics = listOf(characteristic),
        ),
      )

      val response = webTestClient.post()
        .uri("/cas1/spaces/search")
        .header("Authorization", "Bearer $jwt")
        .bodyValue(searchParameters)
        .exchange()
        .expectStatus()
        .isOk
        .returnResult(Cas1SpaceSearchResults::class.java)

      val results = response.responseBody.blockFirst()!!

      assertThat(results.resultsCount).isEqualTo(5)
      assertThat(results.searchCriteria).isEqualTo(searchParameters)

      assertThatResultMatches(results.results[0], expectedPremises[0])
      assertThatResultMatches(results.results[1], expectedPremises[1])
      assertThatResultMatches(results.results[2], expectedPremises[2])
      assertThatResultMatches(results.results[3], expectedPremises[3])
      assertThatResultMatches(results.results[4], expectedPremises[4])
    }
  }

  @Test
  fun `Filtering APs by multiple characteristics only returns APs with all characteristics`() {
    postCodeDistrictFactory.produceAndPersist {
      withOutcode("SE1")
      withLatitude(-0.07)
      withLongitude(51.48)
    }

    givenAUser { user, jwt ->
      val application = givenAnApplication(createdByUser = user, isWomensApplication = false)

      val expectedPremises = approvedPremisesEntityFactory.produceAndPersistMultipleIndexed(5) {
        withYieldedProbationRegion { givenAProbationRegion() }
        withYieldedLocalAuthorityArea {
          localAuthorityEntityFactory.produceAndPersist()
        }
        withLatitude((it * -0.01) - 0.08)
        withLongitude((it * 0.01) + 51.49)
        withCharacteristicsList(Cas1SpaceCharacteristic.entries.slice(1..3).map { it.asCharacteristicEntity() })
        withSupportsSpaceBookings(true)
      }

      expectedPremises.forEach {
        roomEntityFactory.produceAndPersistMultiple(randomInt(1, 10)) {
          withPremises(it)
          withCharacteristicsList(Cas1SpaceCharacteristic.entries.slice(1..3).map { it.asCharacteristicEntity() })
        }
      }

      val unexpectedPremises = approvedPremisesEntityFactory.produceAndPersistMultipleIndexed(4) {
        withYieldedProbationRegion { givenAProbationRegion() }
        withYieldedLocalAuthorityArea {
          localAuthorityEntityFactory.produceAndPersist()
        }
        withLatitude((it * -0.01) - 0.08)
        withLongitude((it * 0.01) + 51.49)
        withCharacteristicsList(
          listOf(
            Cas1SpaceCharacteristic.entries[it].asCharacteristicEntity(),
          ),
        )
        withSupportsSpaceBookings(true)
      }

      unexpectedPremises.forEach {
        roomEntityFactory.produceAndPersist {
          withPremises(it)
          withCharacteristicsList(
            it.characteristics,
          )
        }
      }

      val searchParameters = Cas1SpaceSearchParameters(
        applicationId = application.id,
        startDate = LocalDate.now(),
        durationInDays = 14,
        targetPostcodeDistrict = "SE1",
        requirements = Cas1SpaceSearchRequirements(
          apTypes = null,
          spaceCharacteristics = Cas1SpaceCharacteristic.entries.slice(1..3),
        ),
      )

      val response = webTestClient.post()
        .uri("/cas1/spaces/search")
        .header("Authorization", "Bearer $jwt")
        .bodyValue(searchParameters)
        .exchange()
        .expectStatus()
        .isOk
        .returnResult(Cas1SpaceSearchResults::class.java)

      val results = response.responseBody.blockFirst()!!

      assertThat(results.resultsCount).isEqualTo(5)
      assertThat(results.searchCriteria).isEqualTo(searchParameters)

      assertThatResultMatches(results.results[0], expectedPremises[0])
      assertThatResultMatches(results.results[1], expectedPremises[1])
      assertThatResultMatches(results.results[2], expectedPremises[2])
      assertThatResultMatches(results.results[3], expectedPremises[3])
      assertThatResultMatches(results.results[4], expectedPremises[4])
    }
  }

  private fun ApType.asCharacteristicEntity() = when (this) {
    ApType.normal -> null
    ApType.pipe -> "isPIPE"
    ApType.esap -> "isESAP"
    ApType.rfap -> "isRecoveryFocussed"
    ApType.mhapStJosephs, ApType.mhapElliottHouse -> "isSemiSpecialistMentalHealth"
  }?.let {
    characteristicRepository.findByPropertyName(it, ServiceName.approvedPremises.value)
  }

  private fun Cas1SpaceCharacteristic.asCharacteristicEntity() = characteristicRepository.findByPropertyName(this.value, ServiceName.approvedPremises.value)
    ?: characteristicEntityFactory.produceAndPersist {
      withName(this@asCharacteristicEntity.value)
      withPropertyName(this@asCharacteristicEntity.value)
      withServiceScope(ServiceName.approvedPremises.value)
      withModelScope("*")
    }
}
