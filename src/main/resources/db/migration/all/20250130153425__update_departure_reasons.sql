-- changes to departure reason parent options
UPDATE departure_reasons SET name = 'Absconded' WHERE id = 'ea6f21d9-e658-487f-9765-d8b09187df93'::uuid; -- updates "Absconded, still at large" -> "Absconded"
UPDATE departure_reasons set is_active = false WHERE id = '1ecc1a09-725f-44d8-b03b-314d1ff4da62'::uuid; -- soft deletes "Other"

-- changes to departure reason child options (for "Breach/Recall" parent)
UPDATE departure_reasons set is_active = false WHERE id = '012f21fa-1c46-4dca-ab8d-2e73cbe5ec84'::uuid; -- soft deletes "Breach/Recall > Other"
UPDATE departure_reasons set is_active = false WHERE id = '6761f2a5-4a83-4c2b-a9c8-d6906a7f6e8d'::uuid; -- soft deletes "Breach/Recall > House rules"
INSERT INTO departure_reasons (id, name, is_active, service_scope, legacy_delius_reason_code, parent_reason_id)
VALUES ('bb460d27-db25-4647-be6c-8517e70b0b9c', 'Positive alcohol test / use', true, 'approved-premises','TBD','d3e43ec3-02f4-4b96-a464-69dc74099259');
INSERT INTO departure_reasons (id, name, is_active, service_scope, legacy_delius_reason_code, parent_reason_id)
VALUES ('4b5a22c1-d194-4759-8e13-9ffae2f4bfc8', 'Behaviour / increasing risk in AP', true, 'approved-premises','TBD','d3e43ec3-02f4-4b96-a464-69dc74099259');

UPDATE departure_reasons SET name = 'Order, licence or Bail Condition' WHERE id = '1d0cbc0a-9625-438a-a598-c8c8ac284c0d'::uuid; -- updates "Licence or bail condition" -> "Order, licence or Bail Condition"
UPDATE departure_reasons SET name = 'Positive drug test / use' WHERE id = '0de457c6-8938-47b6-9de8-723fbc4dd408'::uuid; -- updates "Positive drugs test" -> "Positive drug test / use"
UPDATE departure_reasons SET name = 'Absconded' WHERE id = 'f20dd19b-d134-4abd-87a7-b4e5722c0729'::uuid; -- updates "Abscond" -> "Absconded"
