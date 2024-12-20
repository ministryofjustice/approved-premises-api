do
$$
    declare
        wheelchairAccessibleCharacteristicId constant uuid := (select id
                                                               from characteristics
                                                               where name = 'Wheelchair accessible'
                                                                 and service_scope = 'temporary-accommodation');
        roomId1                                       uuid;
        roomId2                                       uuid;

    begin
        delete
        from premises_characteristics
        where characteristic_id = wheelchairAccessibleCharacteristicId
          and premises_id = (select id from premises where name = 'MAN29');

        select id into roomId1 from rooms where name = 'MAN29-1';
        if roomId1 is not null then
            insert into room_characteristics (room_id, characteristic_id)
            values (roomId1, wheelchairAccessibleCharacteristicId)
            on conflict do nothing;
        end if;

        select id into roomId2 from rooms where name = 'MAN29-2';
        if roomId2 is not null then
            insert into room_characteristics (room_id, characteristic_id)
            values (roomId2, wheelchairAccessibleCharacteristicId)
            on conflict do nothing;
        end if;
    end
$$