
CREATE TABLE cas_2_bail_application_json_schemas
(
    json_schema_id UUID NOT NULL,
    CONSTRAINT pk_cas_2_bail_application_json_schemas PRIMARY KEY (json_schema_id)
);

CREATE TABLE cas_2_bail_application_notes
(
    id                          UUID NOT NULL,
    application_id              UUID,
    created_at                  TIMESTAMP WITHOUT TIME ZONE,
    body                        VARCHAR(255),
    assessment_id               UUID,
    created_by_nomis_user_id    UUID,
    created_by_external_user_id UUID,
    CONSTRAINT pk_cas_2_bail_application_notes PRIMARY KEY (id)
);

CREATE TABLE cas_2_bail_applications
(
    id                       UUID NOT NULL,
    crn                      VARCHAR(255),
    created_by_user_id       UUID,
    data                     VARCHAR(255),
    document                 VARCHAR(255),
    schema_version           UUID,
    created_at               TIMESTAMP WITHOUT TIME ZONE,
    submitted_at             TIMESTAMP WITHOUT TIME ZONE,
    abandoned_at             TIMESTAMP WITHOUT TIME ZONE,
    assessment_id            UUID,
    noms_number              VARCHAR(255),
    referring_prison_code    VARCHAR(255),
    preferred_areas          VARCHAR(255),
    hdc_eligibility_date     date,
    conditional_release_date date,
    telephone_number         VARCHAR(255),
    CONSTRAINT pk_cas_2_bail_applications PRIMARY KEY (id)
);

CREATE TABLE cas_2_bail_assessments
(
    id                UUID NOT NULL,
    application_id    UUID,
    created_at        TIMESTAMP WITHOUT TIME ZONE,
    nacro_referral_id VARCHAR(255),
    assessor_name     VARCHAR(255),
    CONSTRAINT pk_cas_2_bail_assessments PRIMARY KEY (id)
);

CREATE TABLE cas_2_bail_status_update_details
(
    id               UUID NOT NULL,
    status_detail_id UUID,
    label            VARCHAR(255),
    status_update_id UUID,
    created_at       TIMESTAMP WITHOUT TIME ZONE,
    CONSTRAINT pk_cas_2_bail_status_update_details PRIMARY KEY (id)
);

CREATE TABLE cas_2_bail_status_updates
(
    id             UUID NOT NULL,
    status_id      UUID,
    description    VARCHAR(255),
    label          VARCHAR(255),
    assessor_id    UUID,
    application_id UUID,
    assessment_id  UUID,
    created_at     TIMESTAMP WITHOUT TIME ZONE,
    CONSTRAINT pk_cas_2_bail_status_updates PRIMARY KEY (id)
);

ALTER TABLE cas_2_bail_applications
    ADD CONSTRAINT FK_CAS_2_BAIL_APPLICATIONS_ON_ASSESSMENT FOREIGN KEY (assessment_id) REFERENCES cas_2_bail_assessments (id);

ALTER TABLE cas_2_bail_applications
    ADD CONSTRAINT FK_CAS_2_BAIL_APPLICATIONS_ON_CREATED_BY_USER FOREIGN KEY (created_by_user_id) REFERENCES nomis_users (id);

ALTER TABLE cas_2_bail_applications
    ADD CONSTRAINT FK_CAS_2_BAIL_APPLICATIONS_ON_SCHEMA_VERSION FOREIGN KEY (schema_version) REFERENCES json_schemas (id);

ALTER TABLE cas_2_bail_application_json_schemas
    ADD CONSTRAINT FK_CAS_2_BAIL_APPLICATION_JSON_SCHEMAS_ON_JSON_SCHEMA FOREIGN KEY (json_schema_id) REFERENCES json_schemas (id);

ALTER TABLE cas_2_bail_application_notes
    ADD CONSTRAINT FK_CAS_2_BAIL_APPLICATION_NOTES_ON_APPLICATION FOREIGN KEY (application_id) REFERENCES cas_2_bail_applications (id);

ALTER TABLE cas_2_bail_application_notes
    ADD CONSTRAINT FK_CAS_2_BAIL_APPLICATION_NOTES_ON_ASSESSMENT FOREIGN KEY (assessment_id) REFERENCES cas_2_bail_assessments (id);

ALTER TABLE cas_2_bail_application_notes
    ADD CONSTRAINT FK_CAS_2_BAIL_APPLICATION_NOTES_ON_CREATED_BY_EXTERNAL_USER FOREIGN KEY (created_by_external_user_id) REFERENCES external_users (id);

ALTER TABLE cas_2_bail_application_notes
    ADD CONSTRAINT FK_CAS_2_BAIL_APPLICATION_NOTES_ON_CREATED_BY_NOMIS_USER FOREIGN KEY (created_by_nomis_user_id) REFERENCES nomis_users (id);

ALTER TABLE cas_2_bail_assessments
    ADD CONSTRAINT FK_CAS_2_BAIL_ASSESSMENTS_ON_APPLICATION FOREIGN KEY (application_id) REFERENCES cas_2_bail_applications (id);

ALTER TABLE cas_2_bail_status_updates
    ADD CONSTRAINT FK_CAS_2_BAIL_STATUS_UPDATES_ON_APPLICATION FOREIGN KEY (application_id) REFERENCES cas_2_bail_applications (id);

ALTER TABLE cas_2_bail_status_updates
    ADD CONSTRAINT FK_CAS_2_BAIL_STATUS_UPDATES_ON_ASSESSMENT FOREIGN KEY (assessment_id) REFERENCES cas_2_bail_assessments (id);

ALTER TABLE cas_2_bail_status_updates
    ADD CONSTRAINT FK_CAS_2_BAIL_STATUS_UPDATES_ON_ASSESSOR FOREIGN KEY (assessor_id) REFERENCES external_users (id);

ALTER TABLE cas_2_bail_status_update_details
    ADD CONSTRAINT FK_CAS_2_BAIL_STATUS_UPDATE_DETAILS_ON_STATUS_UPDATE FOREIGN KEY (status_update_id) REFERENCES cas_2_bail_status_updates (id);

CREATE OR REPLACE VIEW cas_2_bail_application_summary AS SELECT
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
    a.abandoned_at
FROM cas_2_bail_applications a
LEFT JOIN (SELECT DISTINCT ON (application_id) su.application_id, su.label, su.status_id, su.created_at
    FROM cas_2_bail_status_updates su
    ORDER BY su.application_id, su.created_at DESC) as asu
    ON a.id = asu.application_id
JOIN nomis_users nu ON nu.id = a.created_by_user_id;

CREATE OR REPLACE VIEW cas_2_bail_application_live_summary AS SELECT
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
    a.abandoned_at
FROM cas_2_bail_application_summary a
WHERE (a.conditional_release_date IS NULL OR a.conditional_release_date >= current_date)
AND a.abandoned_at IS NULL
AND a.status_id IS NULL
   OR (a.status_id = '004e2419-9614-4c1e-a207-a8418009f23d' AND a.status_created_at > (current_date - INTERVAL '32 DAY')) -- Referral withdrawn
   OR (a.status_id = 'f13bbdd6-44f1-4362-b9d3-e6f1298b1bf9' AND a.status_created_at > (current_date - INTERVAL '32 DAY')) -- Referral cancelled
   OR (a.status_id = '89458555-3219-44a2-9584-c4f715d6b565' AND a.status_created_at > (current_date - INTERVAL '32 DAY')) -- Awaiting arrival
   OR (a.status_id NOT IN ('004e2419-9614-4c1e-a207-a8418009f23d',
                           'f13bbdd6-44f1-4362-b9d3-e6f1298b1bf9',
                           '89458555-3219-44a2-9584-c4f715d6b565'));