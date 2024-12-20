UPDATE characteristics SET property_name = 'isGroundFloor' WHERE id = '83377a71-6cda-4f83-90ca-32513f401500';

-- Fixing name in pre-prod and prod
UPDATE characteristics SET name = 'Is this bed in a single room?' WHERE id = 'a49feeb9-dd25-421b-8e79-d5f4586c1bca';

-- Add isSingle to local/dev/test
INSERT INTO characteristics (id, property_name, name, service_scope, model_scope, is_active) VALUES ('a49feeb9-dd25-421b-8e79-d5f4586c1bca', 'isSingle', 'Is this bed in a single room?', 'approved-premises', 'room', true) ON CONFLICT (property_name, service_scope, model_scope) DO NOTHING;