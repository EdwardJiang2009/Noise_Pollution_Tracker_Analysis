-- ================================================================
-- SECTION 1: CREATE TABLES, sets up all give tables from scratch with primary key in each table

DROP TABLE IF EXISTS readings;
DROP TABLE IF EXISTS hk_locations;
DROP TABLE IF EXISTS noise_sources;
DROP TABLE IF EXISTS noise_classifications;
DROP TABLE IF EXISTS hk_districts;

-- Districts table
CREATE TABLE hk_districts (
    District_ID  INTEGER  PRIMARY KEY,
    District     TEXT     NOT NULL,
    Region       TEXT     NOT NULL
);

-- Locations table
CREATE TABLE hk_locations (
    Location_ID  INTEGER  PRIMARY KEY,
    Location     TEXT     NOT NULL,
    District     TEXT     NOT NULL,
    Longitude    REAL     NOT NULL,
    Latitude     REAL     NOT NULL
);

-- Noise sources table
CREATE TABLE noise_sources (
    Source_ID    INTEGER  PRIMARY KEY,
    Source       TEXT     NOT NULL,
    Description  TEXT
);

-- Classifications table
CREATE TABLE noise_classifications (
    classification_id  INTEGER  PRIMARY KEY,
    name               TEXT     NOT NULL,
    lower_bound        REAL     NOT NULL,
    upper_bound        REAL     NOT NULL
);

-- Readings table
CREATE TABLE readings (
    reading_id         INTEGER  PRIMARY KEY,
    decibel_level      REAL     NOT NULL,
    source_id          INTEGER  NOT NULL,
    date               TEXT     NOT NULL,
    time               TEXT     NOT NULL,
    classification_id  INTEGER  NOT NULL,
    location_id        INTEGER  NOT NULL
);


-- ================================================================
-- SECTION 2: Populates the three lookup tables that never change

-- The three classification bands (must match the Java app logic)
INSERT INTO noise_classifications (classification_id, name, lower_bound, upper_bound)
VALUES (1, 'Safe',      0.0,  70.0);

INSERT INTO noise_classifications (classification_id, name, lower_bound, upper_bound)
VALUES (2, 'Moderate',  70.1, 85.0);

INSERT INTO noise_classifications (classification_id, name, lower_bound, upper_bound)
VALUES (3, 'Dangerous', 85.1, 194.0);

-- Eight noise source categories
INSERT INTO noise_sources (Source_ID, Source, Description)
VALUES (1, 'Traffic',      'Road vehicles including cars, buses, and trucks');

INSERT INTO noise_sources (Source_ID, Source, Description)
VALUES (2, 'Construction', 'Building sites, drilling, and demolition work');

INSERT INTO noise_sources (Source_ID, Source, Description)
VALUES (3, 'Aircraft',     'Planes and helicopters flying overhead');

INSERT INTO noise_sources (Source_ID, Source, Description)
VALUES (4, 'MTR / Rail',   'Trains, the MTR network, and Light Rail');

INSERT INTO noise_sources (Source_ID, Source, Description)
VALUES (5, 'Crowd',        'Large gatherings of people in public spaces');

INSERT INTO noise_sources (Source_ID, Source, Description)
VALUES (6, 'Music',        'Bars, restaurants, concerts, and buskers');

INSERT INTO noise_sources (Source_ID, Source, Description)
VALUES (7, 'Industrial',   'Factories and heavy machinery');

INSERT INTO noise_sources (Source_ID, Source, Description)
VALUES (8, 'Marine',       'Boats, ferries, and harbour activity');


-- ================================================================
-- SECTION 3: View reference data

-- All locations, sorted by district then name.
SELECT Location_ID   AS id,
       Location      AS location_name,
       District      AS district,
       Latitude      AS lat,
       Longitude     AS lon
FROM hk_locations
ORDER BY District ASC, Location ASC;

-- All noise sources with their descriptions, sorted by ID.
SELECT Source_ID    AS id,
       Source       AS source_name,
       Description  AS details
FROM noise_sources
ORDER BY Source_ID ASC;

-- All classification bands, ordered from quietest to loudest.
SELECT classification_id  AS id,
       name                AS band,
       lower_bound         AS min_dB,
       upper_bound         AS max_dB
FROM noise_classifications
ORDER BY lower_bound ASC;

-- All districts, sorted alphabetically.
SELECT District_ID  AS id,
       District     AS district_name,
       Region       AS region
FROM hk_districts
ORDER BY District ASC;


-- ================================================================
-- SECTION 4: Search for locations in the database using a keyword

-- Replace 'central' with any word you want to search for.
SELECT Location_ID  AS id,
       Location     AS location_name,
       District     AS district,
       Latitude     AS lat,
       Longitude    AS lon
FROM hk_locations
WHERE Location LIKE '%central%'
   OR District LIKE '%central%'
ORDER BY Location ASC;


-- ================================================================
-- SECTION 5: Show every location whose district does NOT contain a chosen word

SELECT Location_ID  AS id,
       Location     AS location_name,
       District     AS district
FROM hk_locations
WHERE District NOT LIKE '%Kowloon%'
ORDER BY District ASC, Location ASC;


-- ================================================================
-- SECTION 6: Log a new reading, run these steps in order

-- Check if a reading already exists for this location, date and time.
-- If this returns 1, don't insert — it's a duplicate.
-- Replace the values with the ones you are about to insert.
SELECT COUNT(*) AS duplicate_count
FROM readings
WHERE location_id = 1
  AND date        = '2026-06-03'
  AND time        = '09:00';

-- Check that the dB value is physically possible (0 to 194).
-- Replace 75.5 with the dB value you are testing.
SELECT CASE
           WHEN 75.5 BETWEEN 0 AND 194
               THEN 'Valid — dB is within 0 to 194'
           ELSE
               'Invalid — noise decibel level must be between 0 and 194dB'
       END AS validation_result;

-- Find which classification band (Safe / Moderate / Dangerous) the dB value falls into.
-- Replace 75.5 with the actual dB value.
SELECT classification_id, name AS band
FROM noise_classifications
WHERE 75.5 BETWEEN lower_bound AND upper_bound;

-- Insert the new reading. Replace the five values with your actual data:
-- decibel level, source_id, date, time, location_id.
INSERT INTO readings (reading_id, decibel_level, source_id, date, time, classification_id, location_id)
VALUES (
    (SELECT COALESCE(MAX(reading_id), 0) + 1 FROM readings),
    75.5,
    1,
    '2026-06-03',
    '09:00',
    (SELECT classification_id FROM noise_classifications WHERE 75.5 BETWEEN lower_bound AND upper_bound),
    1
);


-- ================================================================
-- SECTION 7: Update an existing reading

-- Change the decibel level of a reading and re-classify it automatically.
-- Replace 80.0 with the new dB value and 1 with the reading_id.
UPDATE readings
SET decibel_level     = 80.0,
    classification_id = (SELECT classification_id
                         FROM noise_classifications
                         WHERE 80.0 BETWEEN lower_bound AND upper_bound)
WHERE reading_id = 1;


-- ================================================================
-- SECTION 8: Delete a reading

-- Permanently removes one reading. Replace 1 with the reading_id you want to delete.
-- Warning: this cannot be undone.
DELETE FROM readings
WHERE reading_id = 1;


-- ================================================================
-- SECTION 9: View all readings with full details

-- Shows every reading with its location, source and classification joined in.
SELECT r.reading_id                AS id,
       l.Location                  AS location_name,
       l.District                  AS district,
       r.date,
       r.time,
       r.decibel_level             AS dB,
       ns.Source                   AS noise_source,
       nc.name                     AS classification
FROM readings r
INNER JOIN hk_locations l           ON r.location_id       = l.Location_ID
INNER JOIN noise_sources ns         ON r.source_id         = ns.Source_ID
INNER JOIN noise_classifications nc ON r.classification_id = nc.classification_id
ORDER BY r.date DESC, r.time DESC;


-- ================================================================
-- SECTION 10: All locations with how many readings each one has

-- Locations with zero readings still show up with a count of 0.
SELECT l.Location_ID              AS id,
       l.Location                 AS location_name,
       l.District                 AS district,
       COUNT(r.reading_id)        AS total_readings,
       ROUND(AVG(r.decibel_level), 1) AS avg_dB
FROM hk_locations l
LEFT JOIN readings r ON l.Location_ID = r.location_id
GROUP BY l.Location_ID, l.Location, l.District
ORDER BY total_readings DESC, l.Location ASC;


-- ================================================================
-- SECTION 11: Filter readings by noise source

-- Returns only readings that match a chosen source.
-- Replace source_id = 1 with the Source_ID you want to filter by.
SELECT r.reading_id                AS id,
       l.Location                  AS location_name,
       l.District                  AS district,
       r.date,
       r.time,
       r.decibel_level             AS dB,
       nc.name                     AS classification
FROM readings r
INNER JOIN hk_locations l           ON r.location_id       = l.Location_ID
INNER JOIN noise_classifications nc ON r.classification_id = nc.classification_id
WHERE r.source_id = 1
ORDER BY r.date DESC, r.time DESC;


-- ================================================================
-- SECTION 12: View reading history for a location, newest first

-- Replace location_id = 1 with the Location_ID you want.
SELECT r.reading_id                AS id,
       r.date,
       r.time,
       r.decibel_level             AS dB,
       ns.Source                   AS noise_source,
       nc.name                     AS classification
FROM readings r
INNER JOIN noise_sources ns         ON r.source_id         = ns.Source_ID
INNER JOIN noise_classifications nc ON r.classification_id = nc.classification_id
WHERE r.location_id = 1
ORDER BY r.date DESC, r.time DESC;


-- ================================================================
-- SECTION 13: Rank locations by average dB at a chosen hour

-- Groups readings by location for the given hour and sorts noisiest first.
-- Replace the hour value (8) with any hour from 0 to 23.
SELECT l.Location_ID                    AS id,
       l.Location                       AS location_name,
       l.District                       AS district,
       ROUND(AVG(r.decibel_level), 1)   AS avg_dB,
       COUNT(r.reading_id)              AS num_readings
FROM readings r
INNER JOIN hk_locations l ON r.location_id = l.Location_ID
WHERE CAST(SUBSTR(r.time, 1, 2) AS INTEGER) = 8
GROUP BY l.Location_ID, l.Location, l.District
ORDER BY avg_dB DESC, l.Location_ID ASC;


-- ================================================================
-- SECTION 14: District summary — two queries, run both

-- Per-location averages inside a district at a given hour.
-- Replace 'Central and Western' and the hour value (8) as needed.
SELECT l.Location                       AS location_name,
       ROUND(AVG(r.decibel_level), 1)   AS avg_dB,
       COUNT(r.reading_id)              AS num_readings,
       CASE
           WHEN AVG(r.decibel_level) >= 85.1 THEN 'Dangerous'
           WHEN AVG(r.decibel_level) >= 70.1 THEN 'Moderate'
           ELSE                                    'Safe'
       END                             AS avg_classification
FROM readings r
INNER JOIN hk_locations l ON r.location_id = l.Location_ID
WHERE l.District = 'Central and Western'
  AND CAST(SUBSTR(r.time, 1, 2) AS INTEGER) = 8
GROUP BY l.Location_ID, l.Location
ORDER BY avg_dB DESC;

-- District-wide totals — one summary row for the whole district.
SELECT l.District                                              AS district,
       COUNT(DISTINCT l.Location_ID)                          AS total_locations,
       ROUND(AVG(r.decibel_level), 1)                         AS district_avg_dB,
       COUNT(r.reading_id)                                    AS total_readings,
       SUM(CASE WHEN r.decibel_level >= 85.1 THEN 1 ELSE 0 END) AS dangerous_readings
FROM readings r
INNER JOIN hk_locations l ON r.location_id = l.Location_ID
WHERE l.District = 'Central and Western'
  AND CAST(SUBSTR(r.time, 1, 2) AS INTEGER) = 8
GROUP BY l.District;


-- ================================================================
-- SECTION 15: GPS coordinates for the dB estimator

-- Fetches the latitude and longitude of two locations.
-- The distance calculation and inverse square law are handled in Java.
-- Replace the Location_ID values with the two you want to compare.
SELECT Location_ID  AS id,
       Location     AS location_name,
       Latitude     AS lat,
       Longitude    AS lon
FROM hk_locations
WHERE Location_ID = 1
   OR Location_ID = 5;
