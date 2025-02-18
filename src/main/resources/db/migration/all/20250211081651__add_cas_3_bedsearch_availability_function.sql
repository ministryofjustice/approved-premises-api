CREATE OR REPLACE FUNCTION cas_3_bedspace_availability(
    user_probation_region_id UUID,
    probation_delivery_unit_ids UUID[],
    included_premises_characteristic_ids UUID[],
    included_room_characteristic_ids UUID[],
    excluded_premises_characteristic_ids UUID[],
    excluded_room_characteristic_ids UUID[],
    bank_holidays DATE[],
    search_start_date DATE,
    search_end_date DATE
)
    RETURNS TABLE (
                      pdu_name TEXT,
                      premises_id UUID,
                      premises_name TEXT,
                      address_line1 TEXT,
                      address_line2 TEXT,
                      town TEXT,
                      postcode TEXT,
                      premises_notes TEXT,
                      premises_characteristic_names TEXT[],
                      room_id UUID,
                      room_name TEXT,
                      room_characteristics TEXT[],
                      bed_id UUID,
                      bed_name TEXT,
                      premises_bed_count BIGINT,
                      unavailable_bed_count BIGINT,
                      available_bed_count BIGINT,
                      overlapped_booking_ids UUID[]
                  ) AS $$
BEGIN
    RETURN QUERY
        with filtered_bedspaces as (
            --CTE to filter out premises that do not match the characteristic requirements
            select premises.id                                                     as premises_id,
                   array_agg(distinct c1.name) filter ( where c1.name is not null) as premises_characteristic_names,
                   array_agg(distinct c2.name) filter ( where c2.name is not null) as room_characteristic_names,
                   premises.name                                                   as premises_name,
                   premises.address_line1,
                   premises.address_line2,
                   premises.town,
                   premises.postcode,
                   premises.notes                                                  as premises_notes,
                   pdu.name                                                        as pdu_name,
                   room.id                                                         as room_id,
                   room.name                                                       as room_name,
                   tap.turnaround_working_day_count

            from premises premises
                     join temporary_accommodation_premises tap on premises.id = tap.premises_id
                     join probation_delivery_units pdu on pdu.id = tap.probation_delivery_unit_id
                     left join premises_characteristics pc on pc.premises_id = premises.id
                     left join characteristics c1 on pc.characteristic_id = c1.id and c1.is_active = true

                     left join rooms room on premises.id = room.premises_id
                     left join room_characteristics rc on room.id = rc.room_id
                     left join characteristics c2 on rc.characteristic_id = c2.id and c2.is_active = true

            where premises.service = 'temporary-accommodation'
              and premises.status = 'active'
              and premises.probation_region_id = user_probation_region_id
              and (cardinality(probation_delivery_unit_ids) = 0 or pdu.id = any (probation_delivery_unit_ids))
            group by premises.id, pdu.name, room.id, room.name, turnaround_working_day_count

            -- will be true if the list passed in is empty, or if the premises / room has / does not have the given characteristics
            having (cardinality(included_premises_characteristic_ids) = 0 or
                    array_agg(c1.id) @> included_premises_characteristic_ids)

               and (cardinality(included_room_characteristic_ids) = 0 or
                    array_agg(c2.id) @> included_room_characteristic_ids)

               and (cardinality(excluded_premises_characteristic_ids) = 0 or
                    not (array_agg(c1.id) && excluded_premises_characteristic_ids))

               and (cardinality(excluded_room_characteristic_ids) = 0 or
                    not (array_agg(c2.id) && excluded_room_characteristic_ids))),

             bedspace_availability as (
                 -- CTE to find the availability of the filtered bedspaces
                 select distinct on (bed.id ) bed.id     as bed_id,
                                              bed.name   as bed_name,
                                              filtered_bedspaces.premises_id,
                                              filtered_bedspaces.room_id,
                                              filtered_bedspaces.room_name,
                                              filtered_bedspaces.premises_name,
                                              filtered_bedspaces.address_line1,
                                              filtered_bedspaces.address_line2,
                                              filtered_bedspaces.town,
                                              filtered_bedspaces.postcode,
                                              filtered_bedspaces.premises_notes,
                                              filtered_bedspaces.pdu_name,
                                              booking.id as booking_id,
                                              filtered_bedspaces.turnaround_working_day_count,
                                              filtered_bedspaces.premises_characteristic_names,
                                              filtered_bedspaces.room_characteristic_names,

                                              -- calculate the availability of the bedspace
                                              case
                                                  when (booking.id is not null and booking_cancellation.id is null) or
                                                       (void_bedspace.id is not null and void_bedspace_cancellation.id is null)
                                                      then false
                                                  else true
                                                  end    as available

                 from filtered_bedspaces
                          join beds bed on filtered_bedspaces.room_id = bed.room_id
                          left join bookings booking on bed.id = booking.bed_id
                     -- we need to add the working days on to both the departure date and the search range
                     and tsrange(booking.arrival_date::date, add_working_days(booking.departure_date,
                                                                              filtered_bedspaces.turnaround_working_day_count,
                                                                              bank_holidays), '[]') &&
                         tsrange(search_start_date,  add_working_days(search_end_date, filtered_bedspaces.turnaround_working_day_count,
                                                                      bank_holidays), '[]')

                          left join cancellations booking_cancellation on booking.id = booking_cancellation.booking_id

                          left join cas3_void_bedspaces void_bedspace on bed.id = void_bedspace.bed_id and
                                                                         tsrange(void_bedspace.start_date, void_bedspace.end_date, '[]') &&
                                                                         tsrange(search_start_date, search_end_date, '[]')
                          left join cas3_void_bedspace_cancellations void_bedspace_cancellation
                                    on void_bedspace.id = void_bedspace_cancellation.cas3_void_bedspace_id

                 where (bed.end_date is null or bed.end_date >= search_end_date)
                 order by bed_id, available)

        select ba.pdu_name,
               ba.premises_id,
               ba.premises_name,
               ba.address_line1,
               ba.address_line2,
               ba.town,
               ba.postcode,
               ba.premises_notes,
               ba.premises_characteristic_names,
               ba.room_id,
               ba.room_name,
               ba.room_characteristic_names,
               ba.bed_id,
               ba.bed_name,

               (select count(distinct ba2.bed_id)
                from bedspace_availability ba2
                where ba.premises_id = ba2.premises_id) as premises_bed_count,

               (select count(distinct ba2.bed_id)
                from bedspace_availability ba2
                where ba.premises_id = ba2.premises_id
                  and ba2.available = false)            as unavailable_bed_count,

               (select count(distinct ba2.bed_id)
                from bedspace_availability ba2
                where ba.premises_id = ba2.premises_id
                  and ba2.booking_id is not null
                  and ba.available = true)              as available_bed_count,

               (array(select ba2.booking_id
                      from bedspace_availability ba2
                      where ba.premises_id = ba2.premises_id
                        and ba2.available = false))     as overlapped_booking_ids

        from bedspace_availability ba
        where ba.available = true

        order by ba.pdu_name, ba.premises_name, ba.room_name;
END;
$$ LANGUAGE plpgsql;