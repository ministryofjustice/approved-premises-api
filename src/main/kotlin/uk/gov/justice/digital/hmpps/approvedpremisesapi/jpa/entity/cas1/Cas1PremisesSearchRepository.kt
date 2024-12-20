package uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.cas1

import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.ApprovedPremisesType
import java.sql.ResultSet
import java.util.UUID

private const val AP_TYPE_FILTER = """
  AND result.ap_type IN (:apTypes)
"""

private const val PREMISES_CHARACTERISTICS_FILTER = """
  AND (
    SELECT COUNT(distinct pc.characteristic_id)
    FROM premises_characteristics pc
    WHERE
      pc.premises_id = result.premises_id
      AND pc.characteristic_id IN (:premisesCharacteristics)
  ) = :premisesCharacteristicCount
"""

private const val ROOM_CHARACTERISTICS_FILTER = """
  AND (
    SELECT COUNT(distinct rc.characteristic_id)
    FROM room_characteristics rc
    JOIN rooms r ON rc.room_id = r.id
    WHERE
      r.premises_id = result.premises_id
      AND rc.characteristic_id IN (:roomCharacteristics)
  ) = :roomCharacteristicCount
"""

private const val CANDIDATE_PREMISES_QUERY_TEMPLATE = """
SELECT
  result.*
FROM
(
  SELECT
    p.id AS premises_id,
    ST_Distance(
      (SELECT point FROM postcode_districts pd WHERE pd.outcode = :outcode)::geography,
      ap.point::geography
    ) * 0.000621371 AS distance_in_miles,
    CASE
      WHEN EXISTS (
        SELECT 1
        FROM premises_characteristics pc
        JOIN characteristics c
        ON pc.characteristic_id = c.id
        WHERE
          c.property_name = 'isPIPE'
          AND pc.premises_id = p.id
      ) THEN 'PIPE'
      WHEN EXISTS (
        SELECT 1
        FROM premises_characteristics pc
        JOIN characteristics c
        ON pc.characteristic_id = c.id
        WHERE
          c.property_name = 'isESAP'
          AND pc.premises_id = p.id
      ) THEN 'ESAP'
      WHEN EXISTS (
        SELECT 1
        FROM premises_characteristics pc
        JOIN characteristics c
        ON pc.characteristic_id = c.id
        WHERE
          c.property_name = 'isRecoveryFocussed'
          AND pc.premises_id = p.id
      ) THEN 'RFAP'
      WHEN EXISTS (
        SELECT 1
        FROM premises_characteristics pc
        JOIN characteristics c
        ON pc.characteristic_id = c.id
        WHERE
          c.property_name = 'isSemiSpecialistMentalHealth'
          AND pc.premises_id = p.id
      ) THEN 'MHAP'
      ELSE 'NORMAL'
    END AS ap_type,
    p.name AS name,
    p.address_line1 AS address_line1,
    p.address_line2 AS address_line2,
    p.town AS town,
    p.postcode AS postcode,
    aa.id AS ap_area_id,
    aa.name AS ap_area_name
  FROM approved_premises ap
  INNER JOIN premises p ON ap.premises_id = p.id
  INNER JOIN probation_regions pr ON p.probation_region_id = pr.id
  INNER JOIN ap_areas aa ON pr.ap_area_id = aa.id
  WHERE 
    ap.supports_space_bookings = true AND
    ap.gender = #SPECIFIED_GENDER#
) AS result
WHERE
  1 = 1
#AP_TYPE_FILTER#
#PREMISES_CHARACTERISTICS_FILTER#
#ROOM_CHARACTERISTICS_FILTER#
ORDER BY result.distance_in_miles
"""

@Repository
class Cas1SpaceSearchRepository(
  private val jdbcTemplate: NamedParameterJdbcTemplate,
) {
  fun findAllPremisesWithCharacteristicsByDistance(
    targetPostcodeDistrict: String,
    apTypes: List<ApprovedPremisesType>,
    isWomensPremises: Boolean,
    premisesCharacteristics: List<UUID>,
    roomCharacteristics: List<UUID>,
  ): List<CandidatePremises> {
    val (query, parameters) = resolveCandidatePremisesQueryTemplate(
      targetPostcodeDistrict,
      apTypes,
      isWomensPremises,
      premisesCharacteristics,
      roomCharacteristics,
    )

    return jdbcTemplate.query(query, parameters) { rs, _ ->
      val apType = when (val apType = rs.getString("ap_type")) {
        "MHAP" -> ApprovedPremisesType.MHAP_ST_JOSEPHS
        else -> ApprovedPremisesType.valueOf(apType)
      }

      CandidatePremises(
        rs.getUUID("premises_id"),
        rs.getFloat("distance_in_miles"),
        apType,
        rs.getString("name"),
        rs.getString("address_line1"),
        rs.getString("address_line2"),
        rs.getString("town"),
        rs.getString("postcode"),
        rs.getUUID("ap_area_id"),
        rs.getString("ap_area_name"),
      )
    }
  }

  private fun resolveCandidatePremisesQueryTemplate(
    targetPostcodeDistrict: String,
    apTypes: List<ApprovedPremisesType>,
    isWomensPremises: Boolean,
    premisesCharacteristics: List<UUID>,
    roomCharacteristics: List<UUID>,
  ): Pair<String, Map<String, Any>> {
    var query = CANDIDATE_PREMISES_QUERY_TEMPLATE
    val params = mutableMapOf<String, Any>(
      "outcode" to targetPostcodeDistrict,
    )

    val transformedApTypes = apTypes.transformForQuery()

    when {
      transformedApTypes.isEmpty() -> {
        query = query.replace("#AP_TYPE_FILTER#", "")
      }
      else -> {
        query = query.replace("#AP_TYPE_FILTER#", AP_TYPE_FILTER)
        params["apTypes"] = transformedApTypes
      }
    }

    when {
      isWomensPremises -> {
        query = query.replace("#SPECIFIED_GENDER#", "'WOMAN'")
      }
      else -> {
        query = query.replace("#SPECIFIED_GENDER#", "'MAN'")
      }
    }

    when {
      premisesCharacteristics.isEmpty() -> {
        query = query.replace("#PREMISES_CHARACTERISTICS_FILTER#", "")
      }
      else -> {
        query = query.replace("#PREMISES_CHARACTERISTICS_FILTER#", PREMISES_CHARACTERISTICS_FILTER)
        params["premisesCharacteristics"] = premisesCharacteristics
        params["premisesCharacteristicCount"] = premisesCharacteristics.count()
      }
    }

    when {
      roomCharacteristics.isEmpty() -> {
        query = query.replace("#ROOM_CHARACTERISTICS_FILTER#", "")
      }
      else -> {
        query = query.replace("#ROOM_CHARACTERISTICS_FILTER#", ROOM_CHARACTERISTICS_FILTER)
        params["roomCharacteristics"] = roomCharacteristics
        params["roomCharacteristicCount"] = roomCharacteristics.count()
      }
    }

    return query to params
  }

  private fun ResultSet.getUUID(columnLabel: String) = UUID.fromString(this.getString(columnLabel))

  private fun List<ApprovedPremisesType>.transformForQuery() = this.map {
    when (it) {
      ApprovedPremisesType.MHAP_ST_JOSEPHS, ApprovedPremisesType.MHAP_ELLIOTT_HOUSE -> "MHAP"
      else -> it.name
    }
  }
}

data class CandidatePremises(
  val premisesId: UUID,
  val distanceInMiles: Float,
  val apType: ApprovedPremisesType,
  val name: String,
  val addressLine1: String,
  val addressLine2: String?,
  val town: String?,
  val postcode: String,
  val apAreaId: UUID,
  val apAreaName: String,
)
