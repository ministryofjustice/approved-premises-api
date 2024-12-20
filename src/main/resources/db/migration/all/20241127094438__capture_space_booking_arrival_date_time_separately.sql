ALTER TABLE cas1_space_bookings DROP COLUMN actual_arrival_date_time;
ALTER TABLE cas1_space_bookings ADD actual_arrival_date date NULL;
ALTER TABLE cas1_space_bookings ADD actual_arrival_time time NULL;
ALTER TABLE cas1_space_bookings DROP COLUMN actual_departure_date_time;
ALTER TABLE cas1_space_bookings ADD actual_departure_date date NULL;
ALTER TABLE cas1_space_bookings ADD actual_departure_time time NULL;
