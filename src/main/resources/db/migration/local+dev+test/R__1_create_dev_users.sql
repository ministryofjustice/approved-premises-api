-- ${flyway:timestamp}

--These are randomly generated names

INSERT INTO "users" (id, name, delius_username, delius_staff_identifier, probation_region_id, delius_staff_code) VALUES
    ('aa30f20a-84e3-4baa-bef0-3c9bd51879ad', 'Default User', 'JIMSNOWLDAP', 2500041001, '43606be0-9836-441d-9bc1-5586de9ac931', 'STAFFCODE'), -- South West
    ('f29b6402-b5f0-4f39-a66f-a2bb13eb5d6d', 'A. E2etester', 'APPROVEDPREMISESTESTUSER', 2500041002, 'ca979718-b15d-4318-9944-69aaff281cad', 'STAFFCODE'), -- East of England
    ('9807de9d-05b3-49e2-84b8-529790a299cc', 'C. Load-Tester', 'CAS-LOAD-TESTER', 2500041004, 'db82d408-d440-4eb5-960b-119cb33427cd', 'STAFFCODE'), -- Kent, Surrey & Sussex
    ('0621c5b0-0028-40b7-87fb-53ec65704314', 'T. Assessor', 'TEMPORARY-ACCOMMODATION-E2E-TESTER', 2500041003, 'db82d408-d440-4eb5-960b-119cb33427cd', 'STAFFCODE'), -- Kent, Surrey & Sussex
    ('7e36a89e-c69e-48b1-a5cf-a7c8949f432a', 'T. Referrer', 'TEMPORARY-ACCOMMODATION-E2E-REFERRER', 2500510725, 'db82d408-d440-4eb5-960b-119cb33427cd', 'STAFFCODE'), -- Kent, Surrey & Sussex
    ('695ba399-c407-4b66-aafc-e8835d72b8a7', 'Bernard Beaks', 'BERNARD.BEAKS', 2500057096, 'ca979718-b15d-4318-9944-69aaff281cad', 'STAFFCODE') -- East of England
ON CONFLICT (id) DO NOTHING;

UPDATE
  users
SET
  "delius_username" = 'JIMSNOWLDAP'
WHERE
  id = 'aa30f20a-84e3-4baa-bef0-3c9bd51879ad';

INSERT INTO
  "user_role_assignments" ("id", "role", "user_id")
VALUES
  (
    '4d6500ff-0670-4a8e-8581-dff45292b2e4',
    'CAS1_ASSESSOR',
    'aa30f20a-84e3-4baa-bef0-3c9bd51879ad'
  ),
  (
    'f78a5a6d-7d15-415c-b10f-91c0dff16753',
    'CAS1_MATCHER',
    'aa30f20a-84e3-4baa-bef0-3c9bd51879ad'
  ),
  (
    'b7f476af-0e7e-44ba-9c66-abe27d8c70ba',
    'CAS1_MANAGER',
    'aa30f20a-84e3-4baa-bef0-3c9bd51879ad'
  ),
  (
    '0354ff61-295a-4d06-b9e7-ae9fd53eba12',
    'CAS1_WORKFLOW_MANAGER',
    'aa30f20a-84e3-4baa-bef0-3c9bd51879ad'
  ),
  (
    'dd949e2b-5f9b-4560-b3bc-c3ad6793fb28',
    'CAS1_APPLICANT',
    'aa30f20a-84e3-4baa-bef0-3c9bd51879ad'
  ),
  (
    '81e7c996-4e20-4c4f-94c4-1acd4e5bce33',
    'CAS1_ADMIN',
    'aa30f20a-84e3-4baa-bef0-3c9bd51879ad'
  ),
  (
    'f1118a4e-bb6f-4b16-83ad-08ef92314610',
    'CAS3_ASSESSOR',
    'aa30f20a-84e3-4baa-bef0-3c9bd51879ad'
  ),
  (
    '77c6cc54-5d01-41d2-9987-5b293d6d2e07',
    'CAS3_REFERRER',
    'aa30f20a-84e3-4baa-bef0-3c9bd51879ad'
  ),
  (
    '482a4ab7-a6e4-4bf8-a0fc-8516eca9efa2',
    'CAS3_REPORTER',
    'aa30f20a-84e3-4baa-bef0-3c9bd51879ad'
  )
   ON CONFLICT (id)
DO
  NOTHING;

-- Copy all roles from JIMSNOWLDAP to APPROVEDPREMISESTESTUSER
INSERT INTO
  "user_role_assignments" ("id", "role", "user_id")
SELECT
  gen_random_uuid() AS id,
  role AS role,
  (SELECT id FROM users where delius_username='APPROVEDPREMISESTESTUSER') AS user_id
FROM
  "user_role_assignments"
WHERE
  "user_id" = (SELECT id FROM users where delius_username='JIMSNOWLDAP')
ON CONFLICT (id)
DO
  NOTHING;

-- Copy all roles from JIMSNOWLDAP to TEMPORARY-ACCOMMODATION-E2E-TESTER
INSERT INTO
  "user_role_assignments" ("id", "role", "user_id")
SELECT
  gen_random_uuid() AS id,
  role AS role,
  (SELECT id FROM users where delius_username='TEMPORARY-ACCOMMODATION-E2E-TESTER') AS user_id
FROM
  "user_role_assignments"
WHERE
  "user_id" = (SELECT id FROM users where delius_username='JIMSNOWLDAP')
  AND ("role" = 'CAS3_ASSESSOR' OR "role" = 'CAS3_REPORTER')
ON CONFLICT (id)
DO
  NOTHING;


-- Copy all roles from JIMSNOWLDAP to CAS-LOAD-TESTER
INSERT INTO
  "user_role_assignments" ("id", "role", "user_id")
SELECT
  gen_random_uuid() AS id,
  role AS role,
  (SELECT id FROM users where delius_username='CAS-LOAD-TESTER') AS user_id
FROM
  "user_role_assignments"
WHERE
  "user_id" = (SELECT id FROM users where delius_username='JIMSNOWLDAP')
ON CONFLICT (id)
DO
  NOTHING;
