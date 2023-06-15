ALTER TABLE premises ADD COLUMN created_at TIMESTAMP WITH TIME ZONE;
ALTER TABLE rooms ADD COLUMN created_at TIMESTAMP WITH TIME ZONE;
ALTER TABLE beds ADD COLUMN created_at TIMESTAMP WITH TIME ZONE;

ALTER TABLE premises ALTER COLUMN created_at SET DEFAULT now();
ALTER TABLE rooms ALTER COLUMN created_at SET DEFAULT now();
ALTER TABLE beds ALTER COLUMN created_at SET DEFAULT now();
