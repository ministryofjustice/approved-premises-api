package uk.gov.justice.digital.hmpps.approvedpremisesapi.service

import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ServiceName
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApprovedPremisesEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.CharacteristicEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.CharacteristicRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PremisesEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.RoomEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.TemporaryAccommodationPremisesEntity
import java.util.UUID

@Service
class CharacteristicService(
  val characteristicRepository: CharacteristicRepository,
) {
  fun getCharacteristic(characteristicId: UUID): CharacteristicEntity? =
    characteristicRepository.findByIdOrNull(characteristicId)

  fun getCharacteristicByPropertyName(propertyName: String, serviceName: ServiceName): CharacteristicEntity? =
    characteristicRepository.findByPropertyName(propertyName, serviceName.value)

  fun getCharacteristics(characteristicName: String): List<CharacteristicEntity> =
    characteristicRepository.findAllByName(characteristicName)

  fun getCharacteristicsByPropertyNames(requiredCharacteristics: List<String>, serviceName: ServiceName) =
    characteristicRepository.findAllWherePropertyNameIn(requiredCharacteristics, serviceName.value)

  fun serviceScopeMatches(characteristic: CharacteristicEntity, target: Any): Boolean {
    val targetService = getServiceForTarget(target) ?: return false

    return when (characteristic.serviceScope) {
      "*" -> true
      targetService -> true
      else -> return false
    }
  }

  fun modelScopeMatches(characteristic: CharacteristicEntity, target: Any): Boolean {
    return when (characteristic.modelScope) {
      "*" -> true
      "room" -> target is RoomEntity
      "premises" -> target is PremisesEntity
      else -> false
    }
  }

  private fun getServiceForTarget(target: Any): String? {
    return when (target) {
      is RoomEntity -> getServiceForTarget(target.premises)
      is ApprovedPremisesEntity -> ServiceName.approvedPremises.value
      is TemporaryAccommodationPremisesEntity -> ServiceName.temporaryAccommodation.value
      else -> null
    }
  }
}
