package uk.gov.justice.digital.hmpps.approvedpremisesapi.repository

import org.springframework.jdbc.core.ResultSetExtractor
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.PersonType
import java.sql.ResultSet
import java.time.LocalDate
import java.util.UUID

@Repository
class BedSearchRepository(private val namedParameterJdbcTemplate: NamedParameterJdbcTemplate) {

  private val temporaryAccommodationSearchQuery =
    """
    WITH BookedBedspaces AS
        (SELECT distinct b.bed_id, b.premises_id 
             FROM bookings b
             INNER JOIN premises p ON b.premises_id = p.id
             LEFT JOIN cancellations bc ON bc.booking_id = b.id
         WHERE
             b.service = 'temporary-accommodation'
             AND p.probation_region_id = :probation_region_id
             AND (b.arrival_date, b.departure_date) OVERLAPS (:start_date, :end_date)
             AND bc.id IS NULL)
                             
    SELECT p.id as premises_id,
           p.name as premises_name,
           p.address_line1 as premises_address_line1,
           p.address_line2 as premises_address_line2,
           p.town as premises_town,
           p.postcode as premises_postcode,
           p.notes as premises_notes,
           c.property_name as premises_characteristic_property_name,
           c.name as premises_characteristic_name,
           r.id as room_id,
           r.name as room_name,
           c2.property_name as room_characteristic_property_name,
           (SELECT count(1) FROM beds b2 WHERE b2.room_id IN (SELECT id FROM rooms r2 WHERE r2.premises_id = p.id) AND ( b2.end_date IS NULL OR b2.end_date > :end_date ) ) as premises_bed_count,
           (SELECT count(bed_id) FROM BookedBedspaces bb WHERE bb.premises_id = p.id) as booked_bed_count,
           c2.name as room_characteristic_name,
           b.id as bed_id,
           b.name as bed_name,
           pdu.name as probation_delivery_unit_name
    FROM premises p  
    JOIN temporary_accommodation_premises tap ON tap.premises_id = p.id
    LEFT JOIN premises_characteristics pc ON p.id = pc.premises_id
    LEFT JOIN characteristics c ON pc.characteristic_id = c.id
    LEFT JOIN rooms r ON r.premises_id = p.id
    LEFT JOIN room_characteristics rc on rc.room_id = r.id
    LEFT JOIN characteristics c2 ON rc.characteristic_id = c2.id
    LEFT JOIN beds b ON b.room_id = r.id
    LEFT JOIN probation_delivery_units pdu on pdu.id = tap.probation_delivery_unit_id
    WHERE
        NOT EXISTS (SELECT bb.bed_id FROM BookedBedspaces bb
         WHERE bb.bed_id = b.id
        ) AND
        NOT EXISTS (SELECT void_bedspace.bed_id FROM cas3_void_bedspaces void_bedspace
             LEFT JOIN cas3_void_bedspace_cancellations void_bedspace_cancellation ON void_bedspace_cancellation.cas3_void_bedspace_id = void_bedspace.id
         WHERE
             void_bedspace.bed_id = b.id AND
             (void_bedspace.start_date, void_bedspace.end_date) OVERLAPS (:start_date, :end_date) AND
             void_bedspace_cancellation.id IS NULL
        ) AND 
        #OPTIONAL_FILTERS
        pdu.id IN (:probation_delivery_unit_ids) AND
        p.probation_region_id = :probation_region_id AND 
        p.status = 'active' AND 
        p.service = 'temporary-accommodation' AND
        (b.end_date IS NULL OR b.end_date > :end_date)
        ORDER BY pdu.name, p.name, r.name;
"""
  private val temporaryAccommodationPremisesCharacteristicFilter = """
    p.id in (SELECT pc2.premises_id
    FROM premises_characteristics pc2
    INNER JOIN premises p2 ON pc2.premises_id = p2.id
    WHERE service = 'temporary-accommodation'
    And pc2.characteristic_id in (:premises_characteristic_ids)
    Group By pc2.premises_id
    Having count(pc2.premises_id) = :premises_characteristic_ids_count)
"""

  private val temporaryAccommodationRoomCharacteristicFilter = """
    r.id in (SELECT rc2.room_id
    FROM room_characteristics rc2
             INNER JOIN rooms r2 ON rc2.room_id = r2.id
    WHERE rc2.characteristic_id in (:room_characteristic_ids)
    Group By rc2.room_id
    Having count(rc2.room_id) = :room_characteristic_ids_count)
"""

  fun findTemporaryAccommodationBeds(
    probationDeliveryUnits: List<UUID>,
    startDate: LocalDate,
    endDate: LocalDate,
    probationRegionId: UUID,
    premisesCharacteristicsIds: List<UUID>,
    roomCharacteristicsIds: List<UUID>,
  ): List<Cas3BedSearchResult> {
    val params = MapSqlParameterSource().apply {
      addValue("probation_region_id", probationRegionId)
      addValue("probation_delivery_unit_ids", probationDeliveryUnits)
      addValue("start_date", startDate)
      addValue("end_date", endDate)
      addValue("premises_characteristic_ids", premisesCharacteristicsIds)
      addValue("premises_characteristic_ids_count", premisesCharacteristicsIds.size)
      addValue("room_characteristic_ids", roomCharacteristicsIds)
      addValue("room_characteristic_ids_count", roomCharacteristicsIds.size)
    }

    var optionalFilters = if (premisesCharacteristicsIds.any()) {
      "$temporaryAccommodationPremisesCharacteristicFilter AND\n"
    } else {
      ""
    }

    if (roomCharacteristicsIds.any()) {
      optionalFilters += "$temporaryAccommodationRoomCharacteristicFilter AND\n"
    }

    val query = temporaryAccommodationSearchQuery.replace("#OPTIONAL_FILTERS", optionalFilters)

    val result = namedParameterJdbcTemplate.query(
      query,
      params,
      ResultSetExtractor { resultSet ->
        val beds = mutableMapOf<UUID, Cas3BedSearchResult>()

        while (resultSet.next()) {
          val premisesId = UUID.fromString(resultSet.getString("premises_id"))
          val premisesName = resultSet.getString("premises_name")
          val premisesAddressLine1 = resultSet.getString("premises_address_line1")
          val premisesAddressLine2 = resultSet.getString("premises_address_line2")
          val premisesTown = resultSet.getString("premises_town")
          val premisesPostcode = resultSet.getString("premises_postcode")
          val premisesNotes = resultSet.getString("premises_notes")
          val premisesCharacteristicName = resultSet.getString("premises_characteristic_name")
          val premisesCharacteristicPropertyName = resultSet.getString("premises_characteristic_property_name")
          val premisesBedCount = resultSet.getInt("premises_bed_count")
          val bookedBedCount = resultSet.getInt("booked_bed_count")
          val roomId = resultSet.getNullableUUID("room_id")
          val roomName = resultSet.getString("room_name")
          val roomCharacteristicName = resultSet.getString("room_characteristic_name")
          val roomCharacteristicPropertyName = resultSet.getString("room_characteristic_property_name")
          val bedId = resultSet.getNullableUUID("bed_id")
          val bedName = resultSet.getString("bed_name")
          val probationDeliveryUnitName = resultSet.getString("probation_delivery_unit_name")

          if (bedId == null) continue

          if (!beds.containsKey(bedId)) {
            beds[bedId] = Cas3BedSearchResult(
              premisesId = premisesId,
              premisesName = premisesName,
              premisesAddressLine1 = premisesAddressLine1,
              premisesAddressLine2 = premisesAddressLine2,
              premisesTown = premisesTown,
              premisesPostcode = premisesPostcode,
              premisesNotes = premisesNotes,
              probationDeliveryUnitName = probationDeliveryUnitName,
              premisesCharacteristics = mutableListOf(),
              premisesBedCount = premisesBedCount,
              bookedBedCount = bookedBedCount,
              bedId = bedId,
              bedName = bedName,
              roomId = roomId!!,
              roomName = roomName,
              roomCharacteristics = mutableListOf(),
              overlaps = mutableListOf(),
            )
          }

          beds[bedId]!!.apply {
            if (premisesCharacteristicName != null) {
              premisesCharacteristics.addIfNoneMatch(CharacteristicNames(premisesCharacteristicPropertyName, premisesCharacteristicName)) {
                it.name == premisesCharacteristicName
              }
            }

            if (roomCharacteristicName != null) {
              roomCharacteristics.addIfNoneMatch(CharacteristicNames(roomCharacteristicPropertyName, roomCharacteristicName)) {
                it.name == roomCharacteristicName
              }
            }
          }
        }

        beds.values.toList()
      },
    )

    return result ?: emptyList()
  }
}

private fun ResultSet.getNullableUUID(columnName: String): UUID? {
  val stringValue = this.getString(columnName) ?: return null

  return UUID.fromString(stringValue)
}

private inline fun <reified T> MutableList<T>.addIfNoneMatch(entry: T, matcher: (T) -> Boolean) {
  if (this.any { matcher(it) }) return

  this.add(entry)
}

@SuppressWarnings("LongParameterList")
class Cas3BedSearchResult(
  val premisesId: UUID,
  val premisesName: String,
  val premisesAddressLine1: String,
  val premisesAddressLine2: String?,
  val premisesTown: String?,
  val premisesPostcode: String,
  val premisesCharacteristics: MutableList<CharacteristicNames>,
  val premisesBedCount: Int,
  val roomId: UUID,
  val roomName: String,
  val bedId: UUID,
  val bedName: String,
  val roomCharacteristics: MutableList<CharacteristicNames>,
  val probationDeliveryUnitName: String,
  val premisesNotes: String?,
  val bookedBedCount: Int,
  val overlaps: MutableList<TemporaryAccommodationBedSearchResultOverlap>,
)

data class CharacteristicNames(
  val propertyName: String?,
  val name: String,
)

data class TemporaryAccommodationBedSearchResultOverlap(
  val name: String,
  val crn: String,
  val personType: PersonType,
  val sex: String?,
  val days: Int,
  val premisesId: UUID,
  val roomId: UUID,
  val bookingId: UUID,
  val assessmentId: UUID?,
)
