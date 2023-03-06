package uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import java.util.UUID
import javax.persistence.Entity
import javax.persistence.Id
import javax.persistence.Table

interface CharacteristicRepository : JpaRepository<CharacteristicEntity, UUID> {

  @Query("SELECT c FROM CharacteristicEntity c WHERE c.serviceScope = :serviceName")
  fun findAllByServiceScope(serviceName: String): List<CharacteristicEntity>

  fun findByName(name: String): CharacteristicEntity?

  @Query(
    "SELECT c FROM CharacteristicEntity c " +
      "WHERE c.serviceScope = :serviceName AND c.modelScope = :modelName " +
      "AND c.propertyName = :propertyName"
  )
  fun findByPropertyNameAndScopes(propertyName: String, serviceName: String, modelName: String): CharacteristicEntity?

  fun findAllByName(name: String): List<CharacteristicEntity>
}

@Entity
@Table(name = "characteristics")
data class CharacteristicEntity(
  @Id
  var id: UUID,
  var propertyName: String?,
  var name: String,
  var serviceScope: String,
  var modelScope: String
)
