-- ================================================================
--  queries.sql  --  HK Urban Noise Pollution Tracker
--  Edward Jiang
--
--  Run these queries yourself in DataGrip.
--  Each query has a plain-English description and lists which
--  SQL constructs it uses.
-- ================================================================


-- ================================================================
-- SECTION A: CREATE TABLES
-- ----------------------------------------------------------------
-- PURPOSE:  Sets up all five tables from scratch with a PRIMARY KEY
--           on every table, as required.
-- CONSTRUCTS: CREATE, PRIMARY KEY, NOT NULL
--
-- WARNING: Running the DROP lines will DELETE all existing data.
--          Only run Section A if you are starting fresh.
-- ================================================================

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

-- Locations table (District links by name to hk_districts)
CREATE TABLE hk_locations (
    Location_ID  INTEGER  PRIMARY KEY,
    Location     TEXT     NOT NULL,
    District     TEXT     NOT NULL,
    Longitude    REAL     NOT NULL,
    Latitude     REAL     NOT NULL
);

-- Noise sources table (description is the only nullable column)
CREATE TABLE noise_sources (
    Source_ID    INTEGER  PRIMARY KEY,
    Source       TEXT     NOT NULL,
    Description  TEXT
);

-- Classifications table (three bands: Safe / Moderate / Dangerous)
CREATE TABLE noise_classifications (
    classification_id  INTEGER  PRIMARY KEY,
    name               TEXT     NOT NULL,
    lower_bound        REAL     NOT NULL,
    upper_bound        REAL     NOT NULL
);

-- Readings table (links to locations, sources, and classifications)
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
-- SECTION B: INSERT reference data
-- ----------------------------------------------------------------
-- PURPOSE:  Populates the three lookup tables that never change.
--           Run these after creating the tables in Section A.
-- CONSTRUCTS: INSERT
-- ================================================================

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
-- SECTION C: View reference data
-- ----------------------------------------------------------------
-- PURPOSE:  Quick SELECTs to confirm all lookup tables are correct.
-- CONSTRUCTS: SELECT, ORDER BY, Aliases (AS)
-- ================================================================

-- All locations, sorted by district then name.
-- AS gives the column a readable label in the results.
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
-- SECTION D: Search locations  (Spec 2)
-- ----------------------------------------------------------------
-- PURPOSE:  Find locations whose name OR district contains a keyword.
-- CONSTRUCTS: SELECT, WHERE, LIKE, OR, ORDER BY
-- ================================================================

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
-- SECTION E: Exclude locations by keyword  (uses NOT LIKE)
-- ----------------------------------------------------------------
-- PURPOSE:  Show every location whose district does NOT contain
--           a chosen word — the opposite of the search above.
-- CONSTRUCTS: SELECT, WHERE, NOT LIKE, ORDER BY
-- ================================================================

-- Shows all locations that are NOT in any Kowloon district.
SELECT Location_ID  AS id,
       Location     AS location_name,
       District     AS district
FROM hk_locations
WHERE District NOT LIKE '%Kowloon%'
ORDER BY District ASC, Location ASC;


-- ================================================================
-- SECTION F: Log a new reading  (Specs 3, 6, 7, 8)
-- ----------------------------------------------------------------
-- Run these three steps in order whenever you want to add a reading.
-- ================================================================

-- F1. Check for a duplicate before inserting  (Spec 7)
-- PURPOSE:  If this returns a count of 1, a reading for this
--           location/date/time already exists — do not insert.
-- CONSTRUCTS: SELECT, COUNT, WHERE, AND
--
-- Replace the values with the ones you are about to insert.
SELECT COUNT(*) AS duplicate_count
FROM readings
WHERE location_id = 1
  AND date        = '2026-06-03'
  AND time        = '09:00';

-- F2. Validate the dB value is in the physical range  (Spec 8)
-- PURPOSE:  BETWEEN checks that dB falls within 0–194.
--           CASE WHEN turns the result into a readable message.
-- CONSTRUCTS: SELECT, CASE WHEN, BETWEEN
--
-- Replace 75.5 with the dB value you are testing.
SELECT CASE
           WHEN 75.5 BETWEEN 0 AND 194
               THEN 'Valid — dB is within 0 to 194'
           ELSE
               'Invalid — noise decibel level must be between 0 and 194dB'
       END AS validation_result;

-- F3. Find which classification band applies to the dB value  (Spec 3)
-- PURPOSE:  Looks up Safe / Moderate / Dangerous for a given dB level.
-- CONSTRUCTS: SELECT, WHERE, BETWEEN
--
-- Replace 75.5 with the actual dB value.
SELECT classification_id, name AS band
FROM noise_classifications
WHERE 75.5 BETWEEN lower_bound AND upper_bound;

-- F4. Insert the new reading
-- PURPOSE:  Adds one row to the readings table.
--           reading_id is auto-calculated as (current max + 1).
--           classification_id is looked up automatically from the dB level.
-- CONSTRUCTS: INSERT, SELECT, BETWEEN
--
-- Replace the five values (75.5, 1, '2026-06-03', '09:00', 1)
-- with the actual decibel level, source_id, date, time, location_id.
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
-- SECTION G: Update a reading
-- ----------------------------------------------------------------
-- PURPOSE:  Change the decibel level of an existing reading and
--           automatically re-classify it.
-- CONSTRUCTS: UPDATE, SET, WHERE, BETWEEN (in subquery)
--
-- Replace 80.0 with the new dB value, and 1 with the reading_id.
-- ================================================================

UPDATE readings
SET decibel_level     = 80.0,
    classification_id = (SELECT classification_id
                         FROM noise_classifications
                         WHERE 80.0 BETWEEN lower_bound AND upper_bound)
WHERE reading_id = 1;


-- ================================================================
-- SECTION H: Delete a reading
-- ----------------------------------------------------------------
-- PURPOSE:  Permanently removes one reading from the database.
-- CONSTRUCTS: DELETE, WHERE
--
-- Replace 1 with the reading_id you want to delete.
-- WARNING: This cannot be undone.
-- ================================================================

DELETE FROM readings
WHERE reading_id = 1;


-- ================================================================
-- SECTION I: View all readings with full details  (INNER JOIN)
-- ----------------------------------------------------------------
-- PURPOSE:  Shows every reading joined with its location, source,
--           and classification.  INNER JOIN means a reading only
--           appears if it has a valid match in every other table.
-- CONSTRUCTS: SELECT, INNER JOIN, ORDER BY, Aliases (AS)
-- ================================================================

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
-- SECTION J: All locations with their reading counts  (LEFT JOIN)
-- ----------------------------------------------------------------
-- PURPOSE:  Shows every location and how many readings it has.
--           LEFT JOIN means locations with zero readings still appear
--           (with a count of 0), unlike INNER JOIN which would hide them.
-- CONSTRUCTS: SELECT, LEFT JOIN, COUNT, GROUP BY, ORDER BY, Aliases (AS)
-- ================================================================

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
-- SECTION K: Filter readings by noise source  (Spec 4)
-- ----------------------------------------------------------------
-- PURPOSE:  Returns only the readings that match a chosen source.
--           INNER JOIN links readings to locations and classifications.
-- CONSTRUCTS: SELECT, INNER JOIN, WHERE, ORDER BY, Aliases (AS)
--
-- Replace source_id = 1 with the Source_ID you want to filter by.
-- ================================================================

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
-- SECTION L: Location history, newest first  (Spec 9)
-- ----------------------------------------------------------------
-- PURPOSE:  Shows all readings for one location in reverse
--           chronological order (newest date/time at the top).
-- CONSTRUCTS: SELECT, INNER JOIN, WHERE, ORDER BY, Aliases (AS)
--
-- Replace location_id = 1 with the Location_ID you want.
-- ================================================================

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
-- SECTION M: Ranked locations by average dB at a given hour  (Spec 5)
-- ----------------------------------------------------------------
-- PURPOSE:  Groups all readings for a chosen hour, calculates the
--           average dB per location, and sorts noisiest first.
--           Ties are broken by Location_ID ascending.
-- CONSTRUCTS: SELECT, INNER JOIN, WHERE, GROUP BY, ORDER BY,
--             AVG, COUNT, ROUND, Aliases (AS)
--
-- Replace the hour value (8) with 0–23.
-- ================================================================

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
-- SECTION N: District summary  (Spec 1)
-- ----------------------------------------------------------------
-- Two queries: one per location, one district-wide total.
-- ================================================================

-- N1. Per-location averages inside a district at a given hour
-- PURPOSE:  Shows each location's average dB for the chosen
--           district and hour.  CASE WHEN labels the band.
-- CONSTRUCTS: SELECT, INNER JOIN, WHERE, AND, GROUP BY, ORDER BY,
--             AVG, COUNT, ROUND, CASE WHEN, Aliases (AS)
--
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

-- N2. District-wide rollup — one summary row for the whole district
-- PURPOSE:  Totals across the whole district at the chosen hour.
--           SUM with CASE WHEN counts how many readings are Dangerous.
-- CONSTRUCTS: SELECT, INNER JOIN, WHERE, AND, GROUP BY,
--             COUNT, SUM, ROUND, CASE WHEN, Aliases (AS)
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
-- SECTION O: GPS coordinates for the dB estimator  (Spec 10)
-- ----------------------------------------------------------------
-- PURPOSE:  Fetches the latitude and longitude of two locations
--           so the Java app can calculate the Haversine distance
--           and apply the inverse square law.
--           The actual calculation is done in Java, not SQL.
-- CONSTRUCTS: SELECT, WHERE, AND, OR
--
-- Replace the Location_ID values with the two you want to compare.
-- ================================================================

SELECT Location_ID  AS id,
       Location     AS location_name,
       Latitude     AS lat,
       Longitude    AS lon
FROM hk_locations
WHERE Location_ID = 1
   OR Location_ID = 5;
