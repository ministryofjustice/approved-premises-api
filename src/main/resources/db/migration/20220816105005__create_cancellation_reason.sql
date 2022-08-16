CREATE TABLE cancellation_reasons (
    id UUID NOT NULL,
    name TEXT NOT NULL,
    is_active BOOL NOT NULL,
    PRIMARY KEY (id),
    UNIQUE (name)
);