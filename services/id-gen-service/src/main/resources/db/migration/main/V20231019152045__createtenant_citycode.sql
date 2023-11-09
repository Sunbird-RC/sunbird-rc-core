CREATE TABLE IF NOT EXISTS tenantid_citycode
(
    id bigserial NOT NULL,
    tenantid text NOT NULL,
    citycode text NOT NULL,
    CONSTRAINT pk_tenantid_citycode PRIMARY KEY (id),
    CONSTRAINT uk_tenantid UNIQUE (tenantid)
)
