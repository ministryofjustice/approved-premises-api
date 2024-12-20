ALTER TABLE offline_applications ADD name text NULL;
CREATE INDEX offline_applications_name_idx ON offline_applications (name);
