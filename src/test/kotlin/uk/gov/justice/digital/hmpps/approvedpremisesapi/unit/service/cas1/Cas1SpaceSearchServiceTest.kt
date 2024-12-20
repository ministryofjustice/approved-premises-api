package uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.service.cas1

import io.mockk.clearAllMocks
import io.mockk.confirmVerified
import io.mockk.every
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.CsvSource
import org.junit.jupiter.params.provider.MethodSource
import org.springframework.data.repository.findByIdOrNull
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ApType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1SpaceCharacteristic
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1SpaceSearchParameters
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1SpaceSearchRequirements
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ServiceName
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ApprovedPremisesApplicationEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.CharacteristicEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApprovedPremiseApplicationRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.cas1.CandidatePremises
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.cas1.Cas1SpaceSearchRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.ApprovedPremisesType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.asApprovedPremisesType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.CharacteristicService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.Cas1SpaceSearchService
import java.time.LocalDate
import java.util.UUID
import java.util.stream.Stream
import kotlin.enums.enumEntries

@ExtendWith(MockKExtension::class)
class Cas1SpaceSearchServiceTest {
  @MockK
  private lateinit var characteristicService: CharacteristicService

  @MockK
  private lateinit var spaceSearchRepository: Cas1SpaceSearchRepository

  @MockK
  private lateinit var applicationRepository: ApprovedPremiseApplicationRepository

  @InjectMockKs
  private lateinit var service: Cas1SpaceSearchService

  @Test
  fun `findSpaces throws error if the associated application cannot be found`() {
    val applicationId = UUID.randomUUID()

    every { applicationRepository.findByIdOrNull(any()) } returns null

    assertThatThrownBy {
      service.findSpaces(
        Cas1SpaceSearchParameters(
          applicationId,
          startDate = LocalDate.parse("2024-08-01"),
          durationInDays = 14,
          targetPostcodeDistrict = "TB1",
          requirements = Cas1SpaceSearchRequirements(
            apTypes = ApType.entries,
            spaceCharacteristics = Cas1SpaceCharacteristic.entries,
          ),
        ),
      )
    }.hasMessage(
      "Not Found: No Application with an ID of $applicationId could be found",
    )
  }

  @Test
  fun `findSpaces returns premises ordered by distance with the correct space availability`() {
    val application = ApprovedPremisesApplicationEntityFactory()
      .withDefaults()
      .withIsWomensApplication(false)
      .produce()

    val candidatePremises1 = CandidatePremises(
      UUID.randomUUID(),
      1.0f,
      ApprovedPremisesType.NORMAL,
      "Some AP",
      "1 The Street",
      null,
      "Townsbury",
      "TB1 2AB",
      UUID.randomUUID(),
      "Some AP Area",
    )

    val candidatePremises2 = CandidatePremises(
      UUID.randomUUID(),
      2.0f,
      ApprovedPremisesType.ESAP,
      "Some Other AP",
      "2 The Street",
      null,
      "Townsbury",
      "TB1 2AB",
      UUID.randomUUID(),
      "Some AP Area",
    )

    val candidatePremises3 = CandidatePremises(
      UUID.randomUUID(),
      3.0f,
      ApprovedPremisesType.PIPE,
      "Some AP",
      "3 The Street",
      null,
      "Townsbury",
      "TB1 2AB",
      UUID.randomUUID(),
      "Some AP Area",
    )

    val spaceCharacteristics = Cas1SpaceCharacteristic.entries.map { characteristicWithRandomModelScopeCalled(it.value) }

    every {
      characteristicService.getCharacteristicsByPropertyNames(any(), ServiceName.approvedPremises)
    } returnsMany listOf(
      spaceCharacteristics,
    )

    every {
      spaceSearchRepository.findAllPremisesWithCharacteristicsByDistance(
        any(),
        any(),
        any(),
        any(),
        any(),
      )
    } returns listOf(candidatePremises1, candidatePremises2, candidatePremises3)

    every { applicationRepository.findByIdOrNull(application.id) } returns application

    val result = service.findSpaces(
      Cas1SpaceSearchParameters(
        applicationId = application.id,
        startDate = LocalDate.parse("2024-08-01"),
        durationInDays = 14,
        targetPostcodeDistrict = "TB1",
        requirements = Cas1SpaceSearchRequirements(
          apTypes = ApType.entries,
          spaceCharacteristics = Cas1SpaceCharacteristic.entries,
        ),
      ),
    )

    assertThat(result).hasSize(3)
    assertThat(result).containsExactly(
      candidatePremises1,
      candidatePremises2,
      candidatePremises3,
    )

    verify(exactly = 1) {
      applicationRepository.findByIdOrNull(application.id)

      characteristicService.getCharacteristicsByPropertyNames(
        Cas1SpaceCharacteristic.entries.map { it.value },
        ServiceName.approvedPremises,
      )

      spaceSearchRepository.findAllPremisesWithCharacteristicsByDistance(
        "TB1",
        ApType.entries.map { it.asApprovedPremisesType() },
        isWomensPremises = false,
        spaceCharacteristics.filter { it.modelMatches("premises") }.map { it.id },
        spaceCharacteristics.filter { it.modelMatches("room") }.map { it.id },
      )
    }

    confirmVerified()
  }

  @ParameterizedTest
  @MethodSource("apTypeArgs")
  fun `findSpaces uses the correct list of AP types`(apTypes: List<ApType>) {
    val application = ApprovedPremisesApplicationEntityFactory()
      .withDefaults()
      .withIsWomensApplication(false)
      .produce()

    val candidatePremises1 = CandidatePremises(
      UUID.randomUUID(),
      1.0f,
      ApprovedPremisesType.NORMAL,
      "Some AP",
      "1 The Street",
      null,
      "Townsbury",
      "TB1 2AB",
      UUID.randomUUID(),
      "Some AP Area",
    )

    val candidatePremises2 = CandidatePremises(
      UUID.randomUUID(),
      2.0f,
      ApprovedPremisesType.ESAP,
      "Some Other AP",
      "2 The Street",
      null,
      "Townsbury",
      "TB1 2AB",
      UUID.randomUUID(),
      "Some AP Area",
    )

    val candidatePremises3 = CandidatePremises(
      UUID.randomUUID(),
      3.0f,
      ApprovedPremisesType.PIPE,
      "Some AP",
      "3 The Street",
      null,
      "Townsbury",
      "TB1 2AB",
      UUID.randomUUID(),
      "Some AP Area",
    )

    val spaceCharacteristics = Cas1SpaceCharacteristic.entries.map { characteristicWithRandomModelScopeCalled(it.value) }

    every { applicationRepository.findByIdOrNull(application.id) } returns application

    every {
      characteristicService.getCharacteristicsByPropertyNames(any(), ServiceName.approvedPremises)
    } returnsMany listOf(
      spaceCharacteristics,
    )

    every {
      spaceSearchRepository.findAllPremisesWithCharacteristicsByDistance(
        any(),
        any(),
        any(),
        any(),
        any(),
      )
    } returns listOf(candidatePremises1, candidatePremises2, candidatePremises3)

    service.findSpaces(
      Cas1SpaceSearchParameters(
        applicationId = application.id,
        startDate = LocalDate.parse("2024-08-01"),
        durationInDays = 14,
        targetPostcodeDistrict = "TB1",
        requirements = Cas1SpaceSearchRequirements(
          apTypes = apTypes,
          spaceCharacteristics = Cas1SpaceCharacteristic.entries,
        ),
      ),
    )

    verify(exactly = 1) {
      applicationRepository.findByIdOrNull(application.id)

      spaceSearchRepository.findAllPremisesWithCharacteristicsByDistance(
        "TB1",
        apTypes.map { it.asApprovedPremisesType() },
        any(),
        any(),
        any(),
      )
    }

    verify {
      characteristicService.getCharacteristicsByPropertyNames(any(), ServiceName.approvedPremises)
    }

    confirmVerified()
  }

  @ParameterizedTest
  @CsvSource("true", "false")
  fun `findSpaces uses the associated gender from the application`(isWomensApplication: Boolean) {
    val application = ApprovedPremisesApplicationEntityFactory()
      .withDefaults()
      .withIsWomensApplication(isWomensApplication)
      .produce()

    val candidatePremises1 = CandidatePremises(
      UUID.randomUUID(),
      1.0f,
      ApprovedPremisesType.NORMAL,
      "Some AP",
      "1 The Street",
      null,
      "Townsbury",
      "TB1 2AB",
      UUID.randomUUID(),
      "Some AP Area",
    )

    val candidatePremises2 = CandidatePremises(
      UUID.randomUUID(),
      2.0f,
      ApprovedPremisesType.ESAP,
      "Some Other AP",
      "2 The Street",
      null,
      "Townsbury",
      "TB1 2AB",
      UUID.randomUUID(),
      "Some AP Area",
    )

    val candidatePremises3 = CandidatePremises(
      UUID.randomUUID(),
      3.0f,
      ApprovedPremisesType.PIPE,
      "Some AP",
      "3 The Street",
      null,
      "Townsbury",
      "TB1 2AB",
      UUID.randomUUID(),
      "Some AP Area",
    )

    val spaceCharacteristics = Cas1SpaceCharacteristic.entries.map { characteristicWithRandomModelScopeCalled(it.value) }

    every { applicationRepository.findByIdOrNull(application.id) } returns application

    every {
      characteristicService.getCharacteristicsByPropertyNames(any(), ServiceName.approvedPremises)
    } returnsMany listOf(
      spaceCharacteristics,
    )

    every {
      spaceSearchRepository.findAllPremisesWithCharacteristicsByDistance(
        any(),
        any(),
        any(),
        any(),
        any(),
      )
    } returns listOf(candidatePremises1, candidatePremises2, candidatePremises3)

    service.findSpaces(
      Cas1SpaceSearchParameters(
        applicationId = application.id,
        startDate = LocalDate.parse("2024-08-01"),
        durationInDays = 14,
        targetPostcodeDistrict = "TB1",
        requirements = Cas1SpaceSearchRequirements(
          apTypes = ApType.entries,
          spaceCharacteristics = Cas1SpaceCharacteristic.entries,
        ),
      ),
    )

    verify(exactly = 1) {
      applicationRepository.findByIdOrNull(application.id)

      spaceSearchRepository.findAllPremisesWithCharacteristicsByDistance(
        any(),
        any(),
        isWomensPremises = isWomensApplication,
        any(),
        any(),
      )
    }

    verify {
      characteristicService.getCharacteristicsByPropertyNames(any(), ServiceName.approvedPremises)
    }
    confirmVerified()
  }

  @ParameterizedTest
  @MethodSource("spaceCharacteristicArgs")
  fun `findSpaces uses the correct space characteristics`(spaceCharacteristics: List<Cas1SpaceCharacteristic>) {
    val application = ApprovedPremisesApplicationEntityFactory()
      .withDefaults()
      .withIsWomensApplication(false)
      .produce()

    val candidatePremises1 = CandidatePremises(
      UUID.randomUUID(),
      1.0f,
      ApprovedPremisesType.NORMAL,
      "Some AP",
      "1 The Street",
      null,
      "Townsbury",
      "TB1 2AB",
      UUID.randomUUID(),
      "Some AP Area",
    )

    val candidatePremises2 = CandidatePremises(
      UUID.randomUUID(),
      2.0f,
      ApprovedPremisesType.ESAP,
      "Some Other AP",
      "2 The Street",
      null,
      "Townsbury",
      "TB1 2AB",
      UUID.randomUUID(),
      "Some AP Area",
    )

    val candidatePremises3 = CandidatePremises(
      UUID.randomUUID(),
      3.0f,
      ApprovedPremisesType.PIPE,
      "Some AP",
      "3 The Street",
      null,
      "Townsbury",
      "TB1 2AB",
      UUID.randomUUID(),
      "Some AP Area",
    )

    val spaceCharacteristicEntities = spaceCharacteristics.map { characteristicWithRandomModelScopeCalled(it.value) }

    every { applicationRepository.findByIdOrNull(application.id) } returns application

    every {
      characteristicService.getCharacteristicsByPropertyNames(any(), ServiceName.approvedPremises)
    } returnsMany listOf(
      spaceCharacteristicEntities,
    )

    every {
      spaceSearchRepository.findAllPremisesWithCharacteristicsByDistance(
        any(),
        any(),
        any(),
        any(),
        any(),
      )
    } returns listOf(candidatePremises1, candidatePremises2, candidatePremises3)

    service.findSpaces(
      Cas1SpaceSearchParameters(
        applicationId = application.id,
        startDate = LocalDate.parse("2024-08-01"),
        durationInDays = 14,
        targetPostcodeDistrict = "TB1",
        requirements = Cas1SpaceSearchRequirements(
          apTypes = ApType.entries,
          spaceCharacteristics = spaceCharacteristics,
        ),
      ),
    )

    verify(exactly = 1) {
      applicationRepository.findByIdOrNull(application.id)

      characteristicService.getCharacteristicsByPropertyNames(
        spaceCharacteristics.map { it.value },
        ServiceName.approvedPremises,
      )

      spaceSearchRepository.findAllPremisesWithCharacteristicsByDistance(
        any(),
        any(),
        any(),
        spaceCharacteristicEntities.filter { it.modelMatches("premises") }.map { it.id },
        spaceCharacteristicEntities.filter { it.modelMatches("room") }.map { it.id },
      )
    }

    verify {
      characteristicService.getCharacteristicsByPropertyNames(any(), ServiceName.approvedPremises)
    }

    confirmVerified()
  }

  private fun characteristicWithRandomModelScopeCalled(name: String) = CharacteristicEntityFactory()
    .withName(name)
    .withPropertyName(name)
    .withServiceScope(ServiceName.approvedPremises.value)
    .produce()

  companion object {
    @JvmStatic
    @BeforeAll
    fun setup() {
      clearAllMocks()
    }

    @JvmStatic
    fun apTypeArgs(): Stream<Arguments> = generateListEnumArgs<ApType>()

    @JvmStatic
    fun spaceCharacteristicArgs(): Stream<Arguments> = generateListEnumArgs<Cas1SpaceCharacteristic>()

    @OptIn(ExperimentalStdlibApi::class)
    private inline fun <reified T : Enum<T>> generateListEnumArgs(): Stream<Arguments> = Stream.concat(
      enumEntries<T>().map { Arguments.of(listOf(it)) }.stream(),
      Stream.of(Arguments.of(enumEntries<T>())),
    )
  }
}
