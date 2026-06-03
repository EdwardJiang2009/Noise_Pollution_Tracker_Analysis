import java.sql.*;
import java.util.ArrayList;

// DatabaseHelper handles all communication between the Java app and the SQLite database.
// Every method here runs one SQL query and returns the results to the GUI.
public class DatabaseHelper {

    private Connection conn;

    // Constructor: opens a connection to noise_pollution.db
    public DatabaseHelper() throws SQLException {
        conn = DriverManager.getConnection("jdbc:sqlite:noise_pollution.db");
    }

    // Close the connection when the app exits
    public void close() {
        try {
            if (conn != null) conn.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // -----------------------------------------------------------------------
    // Reference data loaders (used to populate combo boxes in the GUI)
    // -----------------------------------------------------------------------

    // Returns every location as [Location_ID, "Location (District)"]
    public ArrayList<String[]> getLocations() throws SQLException {
        ArrayList<String[]> list = new ArrayList<>();
        String sql = "SELECT Location_ID, Location, District FROM hk_locations ORDER BY Location";
        Statement st = conn.createStatement();
        ResultSet rs = st.executeQuery(sql);
        while (rs.next()) {
            String label = rs.getString("Location") + " (" + rs.getString("District") + ")";
            list.add(new String[]{ rs.getString("Location_ID"), label });
        }
        return list;
    }

    // Returns every noise source as [Source_ID, Source name]
    public ArrayList<String[]> getSources() throws SQLException {
        ArrayList<String[]> list = new ArrayList<>();
        String sql = "SELECT Source_ID, Source FROM noise_sources ORDER BY Source";
        Statement st = conn.createStatement();
        ResultSet rs = st.executeQuery(sql);
        while (rs.next()) {
            list.add(new String[]{ rs.getString("Source_ID"), rs.getString("Source") });
        }
        return list;
    }

    // Returns a list of distinct district names (for the district summary combo box)
    public ArrayList<String> getDistrictNames() throws SQLException {
        ArrayList<String> list = new ArrayList<>();
        String sql = "SELECT DISTINCT District FROM hk_locations ORDER BY District";
        Statement st = conn.createStatement();
        ResultSet rs = st.executeQuery(sql);
        while (rs.next()) list.add(rs.getString("District"));
        return list;
    }

    // -----------------------------------------------------------------------
    // Feature 6 + 7 + 8: Log a new reading (with validation)
    // Returns "" on success, or an error message string on failure.
    // -----------------------------------------------------------------------
    public String logReading(int locationId, double db, int sourceId, String date, String time) throws SQLException {

        // Spec 8: dB must be between 0 and 194
        if (db < 0 || db > 194) {
            return "Noise decibel level does not exist. Noise decibel level must be between 0 and 194dB.";
        }

        // Validate date and time formats using if statements
        if (!date.matches("\\d{4}-\\d{2}-\\d{2}")) {
            return "Ensure all entered fields are valid. Date must be YYYY-MM-DD.";
        }
        if (!time.matches("\\d{2}:\\d{2}")) {
            return "Ensure all entered fields are valid. Time must be HH:MM.";
        }

        // Spec 7: block duplicate readings (same location + date + time)
        String dupSql = "SELECT COUNT(*) FROM readings WHERE location_id = ? AND date = ? AND time = ?";
        PreparedStatement dupPs = conn.prepareStatement(dupSql);
        dupPs.setInt(1, locationId);
        dupPs.setString(2, date);
        dupPs.setString(3, time);
        ResultSet dupRs = dupPs.executeQuery();
        if (dupRs.next() && dupRs.getInt(1) > 0) {
            return "Cannot have duplicate readings.";
        }

        // Spec 3: look up the classification for this dB level
        int classId = getClassificationId(db);
        if (classId == -1) {
            return "Could not find a classification for this dB level.";
        }

        // Auto-generate the next reading_id
        int nextId = getNextReadingId();

        // Insert the new reading
        String insertSql = "INSERT INTO readings (reading_id, decibel_level, source_id, date, time, classification_id, location_id) " +
                           "VALUES (?, ?, ?, ?, ?, ?, ?)";
        PreparedStatement insert = conn.prepareStatement(insertSql);
        insert.setInt(1, nextId);
        insert.setDouble(2, db);
        insert.setInt(3, sourceId);
        insert.setString(4, date);
        insert.setString(5, time);
        insert.setInt(6, classId);
        insert.setInt(7, locationId);
        insert.executeUpdate();

        return ""; // empty string means success
    }

    // -----------------------------------------------------------------------
    // Feature 2: Search locations by name or district keyword
    // -----------------------------------------------------------------------
    public ArrayList<String[]> searchLocations(String query) throws SQLException {
        ArrayList<String[]> list = new ArrayList<>();
        String sql = "SELECT Location_ID, Location, District, Latitude, Longitude " +
                     "FROM hk_locations " +
                     "WHERE Location LIKE ? OR District LIKE ? " +
                     "ORDER BY Location";
        PreparedStatement ps = conn.prepareStatement(sql);
        String pattern = "%" + query + "%";
        ps.setString(1, pattern);
        ps.setString(2, pattern);
        ResultSet rs = ps.executeQuery();
        while (rs.next()) {
            list.add(new String[]{
                rs.getString("Location_ID"),
                rs.getString("Location"),
                rs.getString("District"),
                rs.getString("Latitude"),
                rs.getString("Longitude")
            });
        }
        return list;
    }

    // -----------------------------------------------------------------------
    // Feature 9: View reading history for a location (newest first)
    // -----------------------------------------------------------------------
    public ArrayList<String[]> getLocationHistory(int locationId) throws SQLException {
        ArrayList<String[]> list = new ArrayList<>();
        String sql = "SELECT r.reading_id, r.date, r.time, r.decibel_level, ns.Source, nc.name " +
                     "FROM readings r " +
                     "INNER JOIN noise_sources ns ON r.source_id = ns.Source_ID " +
                     "INNER JOIN noise_classifications nc ON r.classification_id = nc.classification_id " +
                     "WHERE r.location_id = ? " +
                     "ORDER BY r.date DESC, r.time DESC";
        PreparedStatement ps = conn.prepareStatement(sql);
        ps.setInt(1, locationId);
        ResultSet rs = ps.executeQuery();
        while (rs.next()) {
            list.add(new String[]{
                rs.getString(1), rs.getString(2), rs.getString(3),
                rs.getString(4), rs.getString(5), rs.getString(6)
            });
        }
        return list;
    }

    // -----------------------------------------------------------------------
    // Feature 4: Filter all readings by a chosen noise source
    // -----------------------------------------------------------------------
    public ArrayList<String[]> filterBySource(int sourceId) throws SQLException {
        ArrayList<String[]> list = new ArrayList<>();
        String sql = "SELECT r.reading_id, l.Location, l.District, r.date, r.time, r.decibel_level, nc.name " +
                     "FROM readings r " +
                     "INNER JOIN hk_locations l ON r.location_id = l.Location_ID " +
                     "INNER JOIN noise_classifications nc ON r.classification_id = nc.classification_id " +
                     "WHERE r.source_id = ? " +
                     "ORDER BY r.date DESC, r.time DESC";
        PreparedStatement ps = conn.prepareStatement(sql);
        ps.setInt(1, sourceId);
        ResultSet rs = ps.executeQuery();
        while (rs.next()) {
            list.add(new String[]{
                rs.getString(1), rs.getString(2), rs.getString(3),
                rs.getString(4), rs.getString(5), rs.getString(6), rs.getString(7)
            });
        }
        return list;
    }

    // -----------------------------------------------------------------------
    // Feature 5: Rank locations by average dB at a given hour (0-23)
    // -----------------------------------------------------------------------
    public ArrayList<String[]> getRankedByHour(int hour) throws SQLException {
        ArrayList<String[]> list = new ArrayList<>();
        String sql = "SELECT l.Location_ID, l.Location, l.District, " +
                     "ROUND(AVG(r.decibel_level), 1) AS avg_dB, COUNT(*) AS num_readings " +
                     "FROM readings r " +
                     "INNER JOIN hk_locations l ON r.location_id = l.Location_ID " +
                     "WHERE CAST(SUBSTR(r.time, 1, 2) AS INTEGER) = ? " +
                     "GROUP BY l.Location_ID, l.Location, l.District " +
                     "ORDER BY avg_dB DESC, l.Location_ID ASC";
        PreparedStatement ps = conn.prepareStatement(sql);
        ps.setInt(1, hour);
        ResultSet rs = ps.executeQuery();
        while (rs.next()) {
            list.add(new String[]{
                rs.getString("Location_ID"), rs.getString("Location"),
                rs.getString("District"), rs.getString("avg_dB"), rs.getString("num_readings")
            });
        }
        return list;
    }

    // -----------------------------------------------------------------------
    // Feature 1: District summary — per-location averages at a given hour
    // -----------------------------------------------------------------------
    public ArrayList<String[]> getDistrictPerLocation(String district, int hour) throws SQLException {
        ArrayList<String[]> list = new ArrayList<>();
        String sql = "SELECT l.Location, ROUND(AVG(r.decibel_level), 1) AS avg_dB, " +
                     "COUNT(*) AS num_readings, " +
                     "CASE WHEN AVG(r.decibel_level) >= 85.1 THEN 'YES' ELSE 'no' END AS dangerous " +
                     "FROM readings r " +
                     "INNER JOIN hk_locations l ON r.location_id = l.Location_ID " +
                     "WHERE l.District = ? AND CAST(SUBSTR(r.time, 1, 2) AS INTEGER) = ? " +
                     "GROUP BY l.Location_ID, l.Location " +
                     "ORDER BY avg_dB DESC";
        PreparedStatement ps = conn.prepareStatement(sql);
        ps.setString(1, district);
        ps.setInt(2, hour);
        ResultSet rs = ps.executeQuery();
        while (rs.next()) {
            list.add(new String[]{
                rs.getString("Location"), rs.getString("avg_dB"),
                rs.getString("num_readings"), rs.getString("dangerous")
            });
        }
        return list;
    }

    // -----------------------------------------------------------------------
    // Feature 10: Get GPS coordinates for a location (for the dB estimator)
    // Returns [latitude, longitude], or null if not found.
    // -----------------------------------------------------------------------
    public double[] getLocationCoords(int locationId) throws SQLException {
        String sql = "SELECT Latitude, Longitude FROM hk_locations WHERE Location_ID = ?";
        PreparedStatement ps = conn.prepareStatement(sql);
        ps.setInt(1, locationId);
        ResultSet rs = ps.executeQuery();
        if (rs.next()) {
            return new double[]{ rs.getDouble("Latitude"), rs.getDouble("Longitude") };
        }
        return null;
    }

    // -----------------------------------------------------------------------
    // Private helpers
    // -----------------------------------------------------------------------

    // Find the classification_id for a given dB level
    private int getClassificationId(double db) throws SQLException {
        String sql = "SELECT classification_id FROM noise_classifications " +
                     "WHERE ? >= lower_bound AND ? <= upper_bound";
        PreparedStatement ps = conn.prepareStatement(sql);
        ps.setDouble(1, db);
        ps.setDouble(2, db);
        ResultSet rs = ps.executeQuery();
        if (rs.next()) return rs.getInt(1);
        return -1;
    }

    // Generate the next reading_id (max existing + 1)
    private int getNextReadingId() throws SQLException {
        String sql = "SELECT COALESCE(MAX(reading_id), 0) + 1 FROM readings";
        Statement st = conn.createStatement();
        ResultSet rs = st.executeQuery(sql);
        if (rs.next()) return rs.getInt(1);
        return 1;
    }
}
