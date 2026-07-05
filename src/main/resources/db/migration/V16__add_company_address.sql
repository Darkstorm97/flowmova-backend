ALTER TABLE companies
    ADD COLUMN address_line_1 VARCHAR(255),
    ADD COLUMN address_line_2 VARCHAR(255),
    ADD COLUMN city VARCHAR(120),
    ADD COLUMN region VARCHAR(120),
    ADD COLUMN postal_code VARCHAR(40),
    ADD COLUMN country VARCHAR(2),
    ADD COLUMN latitude NUMERIC(9, 6),
    ADD COLUMN longitude NUMERIC(9, 6),
    ADD CONSTRAINT ck_companies_country_length CHECK (country IS NULL OR length(trim(country)) = 2),
    ADD CONSTRAINT ck_companies_latitude_range CHECK (latitude IS NULL OR (latitude >= -90 AND latitude <= 90)),
    ADD CONSTRAINT ck_companies_longitude_range CHECK (longitude IS NULL OR (longitude >= -180 AND longitude <= 180));

CREATE INDEX idx_companies_city ON companies (city);
CREATE INDEX idx_companies_region ON companies (region);
CREATE INDEX idx_companies_country ON companies (country);
CREATE INDEX idx_companies_status_business_type ON companies (status, business_type);
CREATE INDEX idx_companies_status_city ON companies (status, city);
CREATE INDEX idx_companies_status_region ON companies (status, region);
CREATE INDEX idx_companies_status_country ON companies (status, country);
