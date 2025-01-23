package uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.PlacementCriteria
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.PlacementRequirements
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ServiceName
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApprovedPremisesApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApprovedPremisesAssessmentEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.CharacteristicRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PlacementRequirementsEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PlacementRequirementsRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PostcodeDistrictRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.ValidationErrors
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.validatedCasResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.results.CasResult
import java.time.OffsetDateTime
import java.util.UUID

@Service
class Cas1PlacementRequirementsService(
  private val postcodeDistrictRepository: PostcodeDistrictRepository,
  private val characteristicRepository: CharacteristicRepository,
  private val placementRequirementsRepository: PlacementRequirementsRepository,
) {
  @SuppressWarnings("TooGenericExceptionThrown")
  fun createPlacementRequirements(
    assessment: ApprovedPremisesAssessmentEntity,
    requirements: PlacementRequirements,
  ): CasResult<PlacementRequirementsEntity> = validatedCasResult {
    val postcodeDistrict = postcodeDistrictRepository.findByOutcode(requirements.location)
      ?: return CasResult.FieldValidationError(
        ValidationErrors().apply {
          this["$.postcodeDistrict"] = "doesNotExist"
        },
      )

    val desirableCriteria = toCharacteristics(
      addIsArsonSuitableIfIsArsonDesignated(
        requirements.desirableCriteria,
      ),
    )

    val essentialCriteria = toCharacteristics(
      addIsArsonSuitableIfIsArsonDesignated(
        requirements.essentialCriteria,
      ),
    )

    val placementRequirementsEntity = placementRequirementsRepository.save(
      PlacementRequirementsEntity(
        id = UUID.randomUUID(),
        apType = requirements.type,
        gender = requirements.gender,
        postcodeDistrict = postcodeDistrict,
        radius = requirements.radius,
        desirableCriteria = desirableCriteria,
        essentialCriteria = essentialCriteria,
        createdAt = OffsetDateTime.now(),
        application = assessment.application as ApprovedPremisesApplicationEntity,
        assessment = assessment,
      ),
    )

    return success(placementRequirementsEntity)
  }

  private fun addIsArsonSuitableIfIsArsonDesignated(criteria: List<PlacementCriteria>): List<PlacementCriteria> {
    return if (criteria.contains(PlacementCriteria.isArsonDesignated) && !criteria.contains(PlacementCriteria.isArsonSuitable)) {
      criteria + listOf(PlacementCriteria.isArsonSuitable)
    } else {
      criteria
    }
  }

  private fun toCharacteristics(criteria: List<PlacementCriteria>) = characteristicRepository.findAllWherePropertyNameIn(
    names = criteria.map { it.toString() },
    serviceName = ServiceName.approvedPremises.value,
  )
}
