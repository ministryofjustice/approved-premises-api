package uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1

import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1SpaceSearchParameters
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ServiceName
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApprovedPremiseApplicationRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.CharacteristicEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.cas1.CandidatePremises
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.cas1.Cas1SpaceSearchRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.ApprovedPremisesType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.asApprovedPremisesType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.problem.NotFoundProblem
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.CharacteristicService
import java.util.UUID

@Service
class Cas1SpaceSearchService(
  private val characteristicService: CharacteristicService,
  private val spaceSearchRepository: Cas1SpaceSearchRepository,
  private val applicationRepository: ApprovedPremiseApplicationRepository,
) {
  fun findSpaces(searchParameters: Cas1SpaceSearchParameters): List<CandidatePremises> {
    val applicationId = searchParameters.applicationId
    val application = applicationRepository.findByIdOrNull(searchParameters.applicationId)
      ?: throw NotFoundProblem(applicationId, "Application")

    val requiredCharacteristics = getRequiredCharacteristics(searchParameters)

    return getCandidatePremises(
      searchParameters.targetPostcodeDistrict,
      requiredCharacteristics,
      isWomensPremises = application.isWomensApplication!!,
    )
  }

  private fun getRequiredCharacteristics(searchParameters: Cas1SpaceSearchParameters) = RequiredCharacteristics(
    searchParameters.requirements.apTypes?.map { it.asApprovedPremisesType() } ?: listOf(),
    getSpaceCharacteristics(searchParameters),
  )

  private fun getSpaceCharacteristics(searchParameters: Cas1SpaceSearchParameters): GroupedCharacteristics {
    val propertyNames = searchParameters.requirements.spaceCharacteristics?.map { it.value } ?: listOf()
    val characteristics = characteristicService.getCharacteristicsByPropertyNames(propertyNames, ServiceName.approvedPremises)

    return GroupedCharacteristics(
      characteristics.filter { it.isPremisesCharacteristic() }.map { it.id },
      characteristics.filter { it.isRoomCharacteristic() }.map { it.id },
    )
  }

  private fun getCandidatePremises(
    targetPostcodeDistrict: String,
    requiredCharacteristics: RequiredCharacteristics,
    isWomensPremises: Boolean,
  ): List<CandidatePremises> {
    return spaceSearchRepository.findAllPremisesWithCharacteristicsByDistance(
      targetPostcodeDistrict,
      requiredCharacteristics.apTypes,
      isWomensPremises,
      requiredCharacteristics.space.premisesCharacteristics,
      requiredCharacteristics.space.roomCharacteristics,
    )
  }

  private fun CharacteristicEntity.isPremisesCharacteristic(): Boolean =
    this.serviceMatches(ServiceName.approvedPremises.value) && this.modelMatches("premises")

  private fun CharacteristicEntity.isRoomCharacteristic(): Boolean =
    this.serviceMatches(ServiceName.approvedPremises.value) && this.modelMatches("room")
}

data class RequiredCharacteristics(
  val apTypes: List<ApprovedPremisesType>,
  val space: GroupedCharacteristics,
)

data class GroupedCharacteristics(
  val premisesCharacteristics: List<UUID>,
  val roomCharacteristics: List<UUID>,
)
