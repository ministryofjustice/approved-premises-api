package uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity

import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import org.hibernate.annotations.CacheConcurrencyStrategy
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import java.util.UUID

interface CharacteristicRepository : JpaRepository<CharacteristicEntity, UUID> {

  companion object Constants {
    const val CAS1_PROPERTY_NAME_ARSON_SUITABLE = "isArsonSuitable"
    const val CAS1_PROPERTY_NAME_ENSUITE = "hasEnSuite"
    const val CAS1_PROPERTY_NAME_SINGLE_ROOM = "isSingle"
    const val CAS1_PROPERTY_NAME_STEP_FREE_DESIGNATED = "isStepFreeDesignated"
    const val CAS1_PROPERTY_NAME_SUITED_FOR_SEX_OFFENDERS = "isSuitedForSexOffenders"
    const val CAS1_PROPERTY_NAME_WHEELCHAIR_DESIGNATED = "isWheelchairDesignated"
  }

  @Query("SELECT c FROM CharacteristicEntity c WHERE c.serviceScope = :serviceName")
  fun findAllByServiceScope(serviceName: String): List<CharacteristicEntity>

  @Query("SELECT c FROM CharacteristicEntity c WHERE c.serviceScope = :serviceName AND c.isActive = true")
  fun findActiveByServiceScope(serviceName: String): List<CharacteristicEntity>

  @Query("SELECT c FROM CharacteristicEntity c WHERE c.isActive = true")
  fun findActive(): List<CharacteristicEntity>

  fun findByName(name: String): CharacteristicEntity?

  @Query(
    "SELECT c FROM CharacteristicEntity c " +
      "WHERE c.propertyName = :propertyName AND c.serviceScope = :serviceName",
  )
  fun findByPropertyName(propertyName: String, serviceName: String): CharacteristicEntity?

  @Query(
    "SELECT c FROM CharacteristicEntity c " +
      "WHERE c.propertyName = :propertyName AND c.serviceScope = 'approved-premises'",
  )
  fun findCas1ByPropertyName(propertyName: String): CharacteristicEntity?

  @Query(
    "SELECT c FROM CharacteristicEntity c " +
      "WHERE c.serviceScope = :serviceName AND c.modelScope = :modelName " +
      "AND c.propertyName = :propertyName",
  )
  fun findByPropertyNameAndScopes(propertyName: String, serviceName: String, modelName: String): CharacteristicEntity?

  fun findAllByName(name: String): List<CharacteristicEntity>

  @Query(
    "SELECT c FROM CharacteristicEntity c " +
      "WHERE c.propertyName IN :names AND c.serviceScope = :serviceName",
  )
  fun findAllWherePropertyNameIn(names: List<String>, serviceName: String): List<CharacteristicEntity>

  @Query(
    """
      SELECT c.*
      FROM characteristics c
      LEFT JOIN room_characteristics rc ON rc.characteristic_id = c.id
      WHERE rc.room_id = :roomId
    """,
    nativeQuery = true,
  )
  fun findAllForRoomId(roomId: UUID): List<CharacteristicEntity>
}

@Entity
@Table(name = "characteristics")
@org.hibernate.annotations.Cache(usage = CacheConcurrencyStrategy.READ_ONLY)
data class CharacteristicEntity(
  @Id
  var id: UUID,
  var propertyName: String?,
  var name: String,
  var serviceScope: String,
  var modelScope: String,
  var isActive: Boolean,
) {
  fun isModelScopeRoom() = modelMatches("room")
  fun matches(entityServiceScope: String, entityModelScope: String) = serviceMatches(entityServiceScope) && modelMatches(entityModelScope)
  fun serviceMatches(entityServiceScope: String) = serviceScope == "*" || entityServiceScope == serviceScope
  fun modelMatches(entityModelScope: String) = modelScope == "*" || entityModelScope == modelScope
}
