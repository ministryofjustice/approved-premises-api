-- add application origin to the cas2 application table
ALTER TABLE cas_2_applications ADD application_origin TEXT NULL;

-- include application origin in the cas 2 application view
DROP VIEW IF EXISTS cas_2_application_summary CASCADE;
CREATE OR REPLACE VIEW cas_2_application_summary AS SELECT
    a.id,
    a.crn,
    a.noms_number,
    CAST(a.created_by_user_id AS TEXT),
    nu.name,
    a.created_at,
    a.submitted_at,
    a.hdc_eligibility_date,
    asu.label,
    CAST(asu.status_id AS TEXT),
    a.referring_prison_code,
    a.conditional_release_date,
    asu.created_at AS status_created_at,
    a.abandoned_at,
    a.application_origin
FROM cas_2_applications a
LEFT JOIN (SELECT DISTINCT ON (application_id) su.application_id, su.label, su.status_id, su.created_at
    FROM cas_2_status_updates su
    ORDER BY su.application_id, su.created_at DESC) as asu
    ON a.id = asu.application_id
JOIN nomis_users nu ON nu.id = a.created_by_user_id;

-- include application origin in the cas 2 application summary view
DROP VIEW IF EXISTS cas_2_application_live_summary CASCADE;
CREATE OR REPLACE VIEW cas_2_application_live_summary AS SELECT
    a.id,
    a.crn,
    a.noms_number,
    a.created_by_user_id,
    a.name,
    a.created_at,
    a.submitted_at,
    a.hdc_eligibility_date,
    a.label,
    a.status_id,
    a.referring_prison_code,
    a.abandoned_at,
    a.application_origin
FROM cas_2_application_summary a
WHERE (a.conditional_release_date IS NULL OR a.conditional_release_date >= current_date)
AND a.abandoned_at IS NULL
AND a.status_id IS NULL
   OR (a.status_id = '004e2419-9614-4c1e-a207-a8418009f23d' AND a.status_created_at > (current_date - INTERVAL '32 DAY')) -- Referral withdrawn
   OR (a.status_id = 'f13bbdd6-44f1-4362-b9d3-e6f1298b1bf9' AND a.status_created_at > (current_date - INTERVAL '32 DAY')) -- Referral cancelled
   OR (a.status_id = '89458555-3219-44a2-9584-c4f715d6b565' AND a.status_created_at > (current_date - INTERVAL '32 DAY')) -- Awaiting arrival
   OR (a.status_id NOT IN ('004e2419-9614-4c1e-a207-a8418009f23d',
                           'f13bbdd6-44f1-4362-b9d3-e6f1298b1bf9',
                           '89458555-3219-44a2-9584-c4f715d6b565'));