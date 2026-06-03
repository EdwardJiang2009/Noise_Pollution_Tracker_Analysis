import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.ArrayList;

// NoiseTrackerApp is the main window (JFrame) of the application.
// It contains a tab for each of the 10 design specifications.
public class NoiseTrackerApp extends JFrame {

    private DatabaseHelper db;

    // Reference data loaded once at startup and shared across all tabs
    private ArrayList<String[]> locations;  // each entry: [Location_ID, display label]
    private ArrayList<String[]> sources;    // each entry: [Source_ID, Source name]
    private ArrayList<String> districts;    // list of district name strings

    // ---- Tab 1: Log Reading ----
    private JComboBox<String> logLocationCombo;
    private JTextField logDbField, logDateField, logTimeField;
    private JComboBox<String> logSourceCombo;
    private JLabel logStatusLabel;

    // ---- Tab 2: Search Location ----
    private JTextField searchField;
    private DefaultTableModel searchModel;

    // ---- Tab 3: Location History ----
    private JComboBox<String> historyLocationCombo;
    private DefaultTableModel historyModel;

    // ---- Tab 4: Filter by Source ----
    private JComboBox<String> filterSourceCombo;
    private DefaultTableModel filterModel;

    // ---- Tab 5: Ranked by Hour ----
    private JSpinner rankedHourSpinner;
    private DefaultTableModel rankedModel;

    // ---- Tab 6: District Summary ----
    private JComboBox<String> districtCombo;
    private JSpinner districtHourSpinner;
    private DefaultTableModel districtModel;
    private JLabel districtSummaryLabel;

    // ---- Tab 7: dB Estimator ----
    private JComboBox<String> estSourceCombo, estTargetCombo;
    private JTextField estDbField;
    private JLabel estResultLabel;

    // Constructor: connect to DB, load reference data, build the GUI
    public NoiseTrackerApp() {
        try {
            db = new DatabaseHelper();
            locations = db.getLocations();
            sources = db.getSources();
            districts = db.getDistrictNames();
        } catch (Exception e) {
            JOptionPane.showMessageDialog(null, "Could not connect to database:\n" + e.getMessage());
            System.exit(1);
        }

        buildFrame();
    }

    // -----------------------------------------------------------------------
    // Frame setup
    // -----------------------------------------------------------------------
    private void buildFrame() {
        setTitle("HK Urban Noise Pollution Tracker");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(950, 650);
        setLocationRelativeTo(null); // centre on screen
        setLayout(new BorderLayout());

        // Header bar at the top
        JPanel header = new JPanel(new FlowLayout(FlowLayout.LEFT, 16, 10));
        header.setBackground(new Color(26, 58, 90));
        JLabel titleLabel = new JLabel("HK Urban Noise Pollution Tracker");
        titleLabel.setFont(new Font("SansSerif", Font.BOLD, 19));
        titleLabel.setForeground(Color.WHITE);
        header.add(titleLabel);
        add(header, BorderLayout.NORTH);

        // Tab pane in the centre
        JTabbedPane tabs = new JTabbedPane();
        tabs.setFont(new Font("SansSerif", Font.PLAIN, 13));
        tabs.addTab("Log Reading",      buildLogTab());
        tabs.addTab("Search Location",  buildSearchTab());
        tabs.addTab("Location History", buildHistoryTab());
        tabs.addTab("Filter by Source", buildFilterTab());
        tabs.addTab("Ranked by Hour",   buildRankedTab());
        tabs.addTab("District Summary", buildDistrictTab());
        tabs.addTab("dB Estimator",     buildEstimatorTab());
        add(tabs, BorderLayout.CENTER);
    }

    // -----------------------------------------------------------------------
    // Shared helper methods
    // -----------------------------------------------------------------------

    // Build a reusable blue button
    private JButton makeButton(String text) {
        JButton btn = new JButton(text);
        btn.setBackground(new Color(34, 139, 34));  // forest green
        btn.setForeground(Color.WHITE);
        btn.setOpaque(true);           // required on macOS to actually paint the background
        btn.setBorderPainted(false);   // removes the native border that overrides the colour
        btn.setFocusPainted(false);
        btn.setFont(new Font("SansSerif", Font.BOLD, 13));
        return btn;
    }

    // Build a table model whose cells cannot be edited by the user
    private DefaultTableModel makeModel(String[] columns) {
        return new DefaultTableModel(columns, 0) {
            public boolean isCellEditable(int row, int col) { return false; }
        };
    }

    // Build a single label + component row for forms
    private JPanel formRow(String labelText, JComponent field) {
        JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 4));
        JLabel lbl = new JLabel(labelText);
        lbl.setPreferredSize(new Dimension(140, 24));
        row.add(lbl);
        row.add(field);
        return row;
    }

    // Return the Location_ID integer at a given combo box index
    private int locationIdAt(int index) {
        return Integer.parseInt(locations.get(index)[0]);
    }

    // Return the Source_ID integer at a given combo box index
    private int sourceIdAt(int index) {
        return Integer.parseInt(sources.get(index)[0]);
    }

    // Build a String[] of display labels for the location combo boxes
    private String[] locationLabels() {
        String[] labels = new String[locations.size()];
        for (int i = 0; i < locations.size(); i++) labels[i] = locations.get(i)[1];
        return labels;
    }

    // Build a String[] of display labels for the source combo boxes
    private String[] sourceLabels() {
        String[] labels = new String[sources.size()];
        for (int i = 0; i < sources.size(); i++) labels[i] = sources.get(i)[1];
        return labels;
    }

    // -----------------------------------------------------------------------
    // Tab 1 — Log Reading (Specs 3, 6, 7, 8)
    // -----------------------------------------------------------------------
    private JPanel buildLogTab() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        JLabel heading = new JLabel("Log a New Noise Reading");
        heading.setFont(new Font("SansSerif", Font.BOLD, 15));
        panel.add(heading);
        panel.add(Box.createVerticalStrut(12));

        logLocationCombo = new JComboBox<>(locationLabels());
        logDbField       = new JTextField(10);
        logSourceCombo   = new JComboBox<>(sourceLabels());
        logDateField     = new JTextField("YYYY-MM-DD", 12);
        logTimeField     = new JTextField("HH:MM", 8);

        panel.add(formRow("Location:",      logLocationCombo));
        panel.add(formRow("Decibel Level:", logDbField));
        panel.add(formRow("Noise Source:",  logSourceCombo));
        panel.add(formRow("Date:",          logDateField));
        panel.add(formRow("Time:",          logTimeField));
        panel.add(Box.createVerticalStrut(14));

        JButton logBtn = makeButton("Log Reading");
        logBtn.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel.add(logBtn);
        panel.add(Box.createVerticalStrut(12));

        logStatusLabel = new JLabel(" ");
        logStatusLabel.setFont(new Font("SansSerif", Font.BOLD, 13));
        panel.add(logStatusLabel);

        logBtn.addActionListener(e -> handleLogReading());
        return panel;
    }

    // Called when the user clicks "Log Reading"
    private void handleLogReading() {
        try {
            int locId = locationIdAt(logLocationCombo.getSelectedIndex());
            int srcId = sourceIdAt(logSourceCombo.getSelectedIndex());
            String date = logDateField.getText().trim();
            String time = logTimeField.getText().trim();

            // Parse the dB value — show error if it is not a number
            double dbLevel;
            try {
                dbLevel = Double.parseDouble(logDbField.getText().trim());
            } catch (NumberFormatException ex) {
                setStatus(logStatusLabel, "Ensure all entered fields are valid.", false);
                return;
            }

            String result = db.logReading(locId, dbLevel, srcId, date, time);
            if (result.isEmpty()) {
                setStatus(logStatusLabel, "Reading logged successfully!", true);
            } else {
                setStatus(logStatusLabel, result, false);
            }

        } catch (Exception ex) {
            setStatus(logStatusLabel, "Error: " + ex.getMessage(), false);
        }
    }

    // Colour a status label green (success) or red (error)
    private void setStatus(JLabel label, String text, boolean success) {
        label.setText(text);
        label.setForeground(success ? new Color(0, 140, 0) : Color.RED);
    }

    // -----------------------------------------------------------------------
    // Tab 2 — Search Location (Spec 2)
    // -----------------------------------------------------------------------
    private JPanel buildSearchTab() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        // Top: search bar
        JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT));
        searchField = new JTextField(22);
        JButton searchBtn = makeButton("Search");
        top.add(new JLabel("Name or district keyword:  "));
        top.add(searchField);
        top.add(searchBtn);

        // Results table
        searchModel = makeModel(new String[]{"ID", "Location", "District", "Latitude", "Longitude"});
        JTable table = new JTable(searchModel);

        panel.add(top, BorderLayout.NORTH);
        panel.add(new JScrollPane(table), BorderLayout.CENTER);

        searchBtn.addActionListener(e -> {
            try {
                ArrayList<String[]> rows = db.searchLocations(searchField.getText().trim());
                searchModel.setRowCount(0);
                if (rows.isEmpty()) {
                    JOptionPane.showMessageDialog(this, "No matching results found.");
                } else {
                    for (String[] row : rows) searchModel.addRow(row);
                }
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Error: " + ex.getMessage());
            }
        });

        return panel;
    }

    // -----------------------------------------------------------------------
    // Tab 3 — Location History (Spec 9)
    // -----------------------------------------------------------------------
    private JPanel buildHistoryTab() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT));
        historyLocationCombo = new JComboBox<>(locationLabels());
        JButton histBtn = makeButton("View History");
        top.add(new JLabel("Location:  "));
        top.add(historyLocationCombo);
        top.add(histBtn);

        historyModel = makeModel(new String[]{"ID", "Date", "Time", "dB Level", "Source", "Classification"});
        JTable table = new JTable(historyModel);

        panel.add(top, BorderLayout.NORTH);
        panel.add(new JScrollPane(table), BorderLayout.CENTER);

        histBtn.addActionListener(e -> {
            try {
                int locId = locationIdAt(historyLocationCombo.getSelectedIndex());
                ArrayList<String[]> rows = db.getLocationHistory(locId);
                historyModel.setRowCount(0);
                if (rows.isEmpty()) {
                    JOptionPane.showMessageDialog(this, "No past records.");
                } else {
                    for (String[] row : rows) historyModel.addRow(row);
                }
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Error: " + ex.getMessage());
            }
        });

        return panel;
    }

    // -----------------------------------------------------------------------
    // Tab 4 — Filter by Source (Spec 4)
    // -----------------------------------------------------------------------
    private JPanel buildFilterTab() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT));
        filterSourceCombo = new JComboBox<>(sourceLabels());
        JButton filterBtn = makeButton("Filter");
        top.add(new JLabel("Noise Source:  "));
        top.add(filterSourceCombo);
        top.add(filterBtn);

        filterModel = makeModel(new String[]{"ID", "Location", "District", "Date", "Time", "dB Level", "Classification"});
        JTable table = new JTable(filterModel);

        panel.add(top, BorderLayout.NORTH);
        panel.add(new JScrollPane(table), BorderLayout.CENTER);

        filterBtn.addActionListener(e -> {
            try {
                int srcId = sourceIdAt(filterSourceCombo.getSelectedIndex());
                ArrayList<String[]> rows = db.filterBySource(srcId);
                filterModel.setRowCount(0);
                if (rows.isEmpty()) {
                    JOptionPane.showMessageDialog(this, "No readings found for this noise source.");
                } else {
                    for (String[] row : rows) filterModel.addRow(row);
                }
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Error: " + ex.getMessage());
            }
        });

        return panel;
    }

    // -----------------------------------------------------------------------
    // Tab 5 — Ranked by Hour (Spec 5)
    // -----------------------------------------------------------------------
    private JPanel buildRankedTab() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT));
        rankedHourSpinner = new JSpinner(new SpinnerNumberModel(8, 0, 23, 1));
        JButton rankBtn = makeButton("Show Ranking");
        top.add(new JLabel("Hour of day (0–23):  "));
        top.add(rankedHourSpinner);
        top.add(rankBtn);

        rankedModel = makeModel(new String[]{"Rank", "Location ID", "Location", "District", "Avg dB", "Readings"});
        JTable table = new JTable(rankedModel);

        panel.add(top, BorderLayout.NORTH);
        panel.add(new JScrollPane(table), BorderLayout.CENTER);

        rankBtn.addActionListener(e -> {
            try {
                int hour = (int) rankedHourSpinner.getValue();
                ArrayList<String[]> rows = db.getRankedByHour(hour);
                rankedModel.setRowCount(0);
                if (rows.isEmpty()) {
                    JOptionPane.showMessageDialog(this, "No readings found for hour " + hour + ".");
                } else {
                    // Add rank number as first column using iteration
                    for (int rank = 1; rank <= rows.size(); rank++) {
                        String[] row = rows.get(rank - 1);
                        rankedModel.addRow(new String[]{ String.valueOf(rank), row[0], row[1], row[2], row[3], row[4] });
                    }
                }
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Error: " + ex.getMessage());
            }
        });

        return panel;
    }

    // -----------------------------------------------------------------------
    // Tab 6 — District Summary (Spec 1)
    // -----------------------------------------------------------------------
    private JPanel buildDistrictTab() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT));
        districtCombo = new JComboBox<>(districts.toArray(new String[0]));
        districtHourSpinner = new JSpinner(new SpinnerNumberModel(8, 0, 23, 1));
        JButton summaryBtn = makeButton("Generate Summary");
        top.add(new JLabel("District:  "));
        top.add(districtCombo);
        top.add(new JLabel("   Hour:  "));
        top.add(districtHourSpinner);
        top.add(summaryBtn);

        districtModel = makeModel(new String[]{"Location", "Avg dB", "Readings", "Dangerous?"});
        JTable table = new JTable(districtModel);

        districtSummaryLabel = new JLabel(" ");
        districtSummaryLabel.setFont(new Font("SansSerif", Font.BOLD, 13));
        districtSummaryLabel.setBorder(BorderFactory.createEmptyBorder(6, 4, 6, 4));

        panel.add(top, BorderLayout.NORTH);
        panel.add(new JScrollPane(table), BorderLayout.CENTER);
        panel.add(districtSummaryLabel, BorderLayout.SOUTH);

        summaryBtn.addActionListener(e -> {
            try {
                String district = (String) districtCombo.getSelectedItem();
                int hour = (int) districtHourSpinner.getValue();
                ArrayList<String[]> rows = db.getDistrictPerLocation(district, hour);

                districtModel.setRowCount(0);
                if (rows.isEmpty()) {
                    JOptionPane.showMessageDialog(this, "No readings for this district at hour " + hour + ".");
                    districtSummaryLabel.setText(" ");
                    return;
                }

                // Count dangerous locations and compute overall average using arithmetic operators
                int dangerousCount = 0;
                double totalAvg = 0;
                for (String[] row : rows) {
                    districtModel.addRow(row);
                    if ("YES".equals(row[3])) dangerousCount++;
                    if (row[1] != null) totalAvg += Double.parseDouble(row[1]);
                }

                double distAvg = Math.round((totalAvg / rows.size()) * 10.0) / 10.0;
                double pct     = Math.round((100.0 * dangerousCount / rows.size()) * 10.0) / 10.0;

                districtSummaryLabel.setText(String.format(
                    "  District avg: %.1f dB  |  Locations: %d  |  Dangerous: %d (%.1f%%)",
                    distAvg, rows.size(), dangerousCount, pct
                ));

            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Error: " + ex.getMessage());
            }
        });

        return panel;
    }

    // -----------------------------------------------------------------------
    // Tab 7 — dB Estimator using Inverse Square Law (Spec 10)
    // -----------------------------------------------------------------------
    private JPanel buildEstimatorTab() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        JLabel heading = new JLabel("Estimate dB at a Target Location  (Inverse Square Law)");
        heading.setFont(new Font("SansSerif", Font.BOLD, 15));
        panel.add(heading);
        panel.add(Box.createVerticalStrut(14));

        estSourceCombo = new JComboBox<>(locationLabels());
        estTargetCombo = new JComboBox<>(locationLabels());
        estDbField     = new JTextField(10);

        panel.add(formRow("Source Location:", estSourceCombo));
        panel.add(formRow("Target Location:", estTargetCombo));
        panel.add(formRow("dB at Source:",    estDbField));
        panel.add(Box.createVerticalStrut(14));

        JButton estBtn = makeButton("Estimate");
        estBtn.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel.add(estBtn);
        panel.add(Box.createVerticalStrut(14));

        estResultLabel = new JLabel(" ");
        estResultLabel.setFont(new Font("SansSerif", Font.BOLD, 14));
        panel.add(estResultLabel);

        estBtn.addActionListener(e -> handleEstimate());
        return panel;
    }

    // Called when the user clicks "Estimate"
    private void handleEstimate() {
        try {
            int srcId = locationIdAt(estSourceCombo.getSelectedIndex());
            int tgtId = locationIdAt(estTargetCombo.getSelectedIndex());

            if (srcId == tgtId) {
                estResultLabel.setText("Source and target must be different locations.");
                return;
            }

            double srcDb;
            try {
                srcDb = Double.parseDouble(estDbField.getText().trim());
            } catch (NumberFormatException ex) {
                estResultLabel.setText("Please enter a valid dB level.");
                return;
            }

            if (srcDb < 0 || srcDb > 194) {
                estResultLabel.setText("Noise decibel level does not exist. Must be 0–194 dB.");
                return;
            }

            double[] srcCoords = db.getLocationCoords(srcId);
            double[] tgtCoords = db.getLocationCoords(tgtId);

            if (srcCoords == null || tgtCoords == null) {
                estResultLabel.setText("Could not find GPS coordinates for the selected locations.");
                return;
            }

            // Calculate the real-world distance using the Haversine formula
            double distanceM = haversineDistance(srcCoords[0], srcCoords[1], tgtCoords[0], tgtCoords[1]);

            // Inverse square law: dB drops by 20*log10(distance) as distance increases
            double estimatedDb = srcDb - 20.0 * Math.log10(Math.max(distanceM, 1.0));
            estimatedDb = Math.round(estimatedDb * 10.0) / 10.0;

            // Classify the estimated dB level
            String classification;
            if (estimatedDb >= 85.1) {
                classification = "Dangerous";
            } else if (estimatedDb >= 70.1) {
                classification = "Moderate";
            } else {
                classification = "Safe";
            }

            estResultLabel.setText(String.format(
                "<html>Distance: <b>%.1f m</b> &nbsp;&nbsp; Estimated dB at target: <b>%.1f dB</b> &nbsp;&nbsp; Classification: <b>%s</b></html>",
                distanceM, estimatedDb, classification
            ));

        } catch (Exception ex) {
            estResultLabel.setText("Error: " + ex.getMessage());
        }
    }

    // Haversine formula: returns the straight-line distance in metres
    // between two GPS coordinates (latitude/longitude in decimal degrees).
    private double haversineDistance(double lat1, double lon1, double lat2, double lon2) {
        final double R = 6371000; // Earth's mean radius in metres

        double lat1Rad   = Math.toRadians(lat1);
        double lat2Rad   = Math.toRadians(lat2);
        double deltaLat  = Math.toRadians(lat2 - lat1);
        double deltaLon  = Math.toRadians(lon2 - lon1);

        double a = Math.sin(deltaLat / 2) * Math.sin(deltaLat / 2)
                 + Math.cos(lat1Rad) * Math.cos(lat2Rad)
                 * Math.sin(deltaLon / 2) * Math.sin(deltaLon / 2);

        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

        return R * c;
    }
}
