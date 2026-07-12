-- UK tax years reference data
CREATE TABLE tax_years (
    id SERIAL PRIMARY KEY,
    label VARCHAR(10) NOT NULL UNIQUE,
    start_date DATE NOT NULL,
    end_date DATE NOT NULL,
    final_declaration_deadline DATE NOT NULL
);

-- Pre-populate tax years
INSERT INTO tax_years (label, start_date, end_date, final_declaration_deadline) VALUES
('2025-26', '2025-04-06', '2026-04-05', '2027-01-31'),
('2026-27', '2026-04-06', '2027-04-05', '2028-01-31'),
('2027-28', '2027-04-06', '2028-04-05', '2029-01-31'),
('2028-29', '2028-04-06', '2029-04-05', '2030-01-31'),
('2029-30', '2029-04-06', '2030-04-05', '2031-01-31');
