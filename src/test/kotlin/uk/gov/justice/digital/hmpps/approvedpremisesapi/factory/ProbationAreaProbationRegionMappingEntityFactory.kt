package uk.gov.justice.digital.hmpps.approvedpremisesapi.factory

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ProbationAreaProbationRegionMappingEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ProbationAreaProbationRegionMappingRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ProbationRegionEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.randomStringUpperCase
import java.util.UUID

@Component
class ProbationAreaProbationRegionMappingPersistor(private val probationAreaProbationRegionMappingRepository: ProbationAreaProbationRegionMappingRepository) {

  fun persist(probationAreaProbationRegionMappingEntity: ProbationAreaProbationRegionMappingEntity): ProbationAreaProbationRegionMappingEntity =
    probationAreaProbationRegionMappingRepository.saveAndFlush(probationAreaProbationRegionMappingEntity)

  fun produceAndPersist(
    probationRegion: ProbationRegionEntity,
    probationAreaDeliusCode: String? = null,
  ): ProbationAreaProbationRegionMappingEntity =
    persist(ProbationAreaProbationRegionMapping(probationRegion).toEntity())
}

data class ProbationAreaProbationRegionMapping(
  val probationRegion: ProbationRegionEntity = ProbationRegionEntityFactory().produce(),
  val id: UUID = UUID.randomUUID(),
  val probationAreaDeliusCode: String = randomStringUpperCase(6),
) {
  fun toEntity() = ProbationAreaProbationRegionMappingEntity(id, probationAreaDeliusCode, probationRegion)
}
