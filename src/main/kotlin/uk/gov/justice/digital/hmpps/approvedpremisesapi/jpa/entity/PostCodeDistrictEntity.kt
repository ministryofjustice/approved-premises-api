package uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.math.BigDecimal
import java.util.UUID
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table

@Repository
interface PostcodeDistrictRepository : JpaRepository<PostCodeDistrictEntity, UUID> {

  fun findByOutcode(outcode: String): PostCodeDistrictEntity?
}

@Entity
@Table(name = "postcode_districts")
data class PostCodeDistrictEntity(
  @Id
  val id: UUID,
  val outcode: String,
  val latitude: BigDecimal,
  val longitude: BigDecimal
)
