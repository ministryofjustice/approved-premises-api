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
        withCharacteristicsList(emptyList())
      }

      val premiseWithCharacteristics = approvedPremisesEntityFactory.produceAndPersist {
        withYieldedProbationRegion { givenAProbationRegion() }
        withYieldedLocalAuthorityArea {
          localAuthorityEntityFactory.produceAndPersist()
        }
        withLatitude(1.0)
        withLongitude(2.0)
        withSupportsSpaceBookings(true)
        withCharacteristicsList(
          listOf(
            characteristicRepository.findCas1ByPropertyName("hasWideAccessToCommunalAreas")!!,
            characteristicRepository.findCas1ByPropertyName("hasWideStepFreeAccess")!!,
            characteristicRepository.findCas1ByPropertyName("hasLift")!!,
          ),
        )
      }.also {
        roomEntityFactory.produceAndPersist {
          withPremises(it)
          withCharacteristics(
            characteristicRepository.findCas1ByPropertyName("hasEnSuite")!!,
          )
        }
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

      assertThat(results.resultsCount).isEqualTo(6)
      assertThat(results.searchCriteria).isEqualTo(searchParameters)

      assertThatResultMatches(results.results[0], premises[0], expectedCharacteristics = emptyList())
      assertThatResultMatches(results.results[1], premises[1], expectedCharacteristics = emptyList())
      assertThatResultMatches(results.results[2], premises[2], expectedCharacteristics = emptyList())
      assertThatResultMatches(results.results[3], premises[3], expectedCharacteristics = emptyList())
      assertThatResultMatches(results.results[4], premises[4], expectedCharacteristics = emptyList())
      assertThatResultMatches(
        results.results[5],
        premiseWithCharacteristics,
        expectedCharacteristics = listOf(
          Cas1SpaceCharacteristic.hasWideAccessToCommunalAreas,
          Cas1SpaceCharacteristic.hasWideStepFreeAccess,
          Cas1SpaceCharacteristic.hasLift,
          Cas1SpaceCharacteristic.hasEnSuite,
        ),
      )
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
  fun `Filtering APs by AP type returns only APs of that type - using single ap type option`(apType: ApType) {
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

      val unexpectedPremises = approvedPremisesEntityFactory.produceAndPersistMultipleIndexed(4) {
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
          apTypes = emptyList(),
          apType = apType,
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

  @ParameterizedTest
  @EnumSource
  fun `Filtering APs by AP type returns only APs of that type - using multiple ap types option`(apType: ApType) {
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

      val unexpectedPremises = approvedPremisesEntityFactory.produceAndPersistMultipleIndexed(4) {
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
          apType = null,
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

  private fun assertThatResultMatches(
    actual: Cas1SpaceSearchResult,
    expected: ApprovedPremisesEntity,
    expectedApType: ApType = ApType.normal,
    expectedCharacteristics: List<Cas1SpaceCharacteristic>? = null,
  ) {
    assertThat(actual.distanceInMiles).isGreaterThan(0f.toBigDecimal())
    assertThat(actual.premises).isNotNull
    val premises = actual.premises
    assertThat(premises.id).isEqualTo(expected.id)
    assertThat(premises.apType).isEqualTo(expectedApType)
    assertThat(premises.name).isEqualTo(expected.name)
    assertThat(premises.fullAddress).isEqualTo(expected.fullAddress)
    assertThat(premises.addressLine1).isEqualTo(expected.addressLine1)
    assertThat(premises.addressLine2).isEqualTo(expected.addressLine2)
    assertThat(premises.town).isEqualTo(expected.town)
    assertThat(premises.postcode).isEqualTo(expected.postcode)
    assertThat(premises.premisesCharacteristics).isEmpty()

    if (expectedCharacteristics != null) {
      assertThat(premises.characteristics).containsExactlyInAnyOrder(*expectedCharacteristics.toTypedArray())
    }
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

  private fun ApType.asCharacteristicProperty() = when (this) {
    ApType.normal -> null
    ApType.pipe -> "isPIPE"
    ApType.esap -> "isESAP"
    ApType.rfap -> "isRecoveryFocussed"
    ApType.mhapStJosephs, ApType.mhapElliottHouse -> "isSemiSpecialistMentalHealth"
  }

  private fun ApType.asCharacteristicEntity() = this.asCharacteristicProperty()?.let {
    characteristicRepository.findByPropertyName(it, ServiceName.approvedPremises.value)
  }

  private fun Cas1SpaceCharacteristic.asCharacteristicEntity() = characteristicRepository.findByPropertyName(this.value, ServiceName.approvedPremises.value)!!
}
