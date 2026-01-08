//package org.nofel;
//
//import org.jline.reader.LineReader;
//import org.jline.reader.LineReaderBuilder;
//import org.jline.reader.UserInterruptException;
//import org.jline.reader.EndOfFileException;
//
//import java.io.FileInputStream;
//import java.io.IOException;
//import java.sql.*;
//import java.util.*;
//
//public class MultiEnvConsoleDbClient {
//
//    private static final int MAX_ROWS = 50;
//    private static final int PAGE_SIZE = 20;
//    private static Properties dbProps = new Properties();
//
//    public static void main(String[] args) {
//        // Load properties from external file
//        try (FileInputStream fis = new FileInputStream("dbconfig.properties")) {
//            dbProps.load(fis);
//        } catch (IOException e) {
//            System.out.println("❌ Failed to load dbconfig.properties: " + e.getMessage());
//            return;
//        }
//
//        Scanner sc = new Scanner(System.in);
//
//        // Step 1: Select environment
//        System.out.println("Select Environment:");
//        System.out.println("1. INHOUSE");
//        System.out.println("2. SIT");
//        String env = null;
//        while (env == null) {
//            System.out.print("Enter choice (1-2): ");
//            String choice = sc.nextLine().trim();
//            switch (choice) {
//                case "1":
//                    env = "INHOUSE";
//                    break;
//                case "2":
//                    env = "SIT";
//                    break;
//                default:
//                    System.out.println("Invalid choice. Try again.");
//            }
//        }
//
//        // Step 2: Select database
//        List<String> dbOptions = Arrays.asList("MIDDLEWARE", "OPENAPI", "WALLET", "SWITCH");
//        System.out.println("Select Database:");
//        for (int i = 0; i < dbOptions.size(); i++) {
//            System.out.println((i + 1) + ". " + dbOptions.get(i));
//        }
//        String db = null;
//        while (db == null) {
//            System.out.print("Enter choice (1-4): ");
//            String choice = sc.nextLine().trim();
//            try {
//                int idx = Integer.parseInt(choice) - 1;
//                if (idx >= 0 && idx < dbOptions.size()) db = dbOptions.get(idx);
//                else System.out.println("Invalid choice. Try again.");
//            } catch (NumberFormatException e) {
//                System.out.println("Invalid input. Try again.");
//            }
//        }
//
//        // Step 3: Fetch credentials
//        String keyPrefix = env + "." + db;
//        String url = dbProps.getProperty(keyPrefix + ".url");
//        String username = dbProps.getProperty(keyPrefix + ".username");
//        String password = dbProps.getProperty(keyPrefix + ".password");
//
//        if (url == null || username == null || password == null) {
//            System.out.println("❌ Credentials not found for " + keyPrefix);
//            return;
//        }
//
//        // Step 4: Connect and launch console
//        try (Connection connection = DriverManager.getConnection(url, username, password)) {
//            System.out.println("✅ Connected to " + env + " " + db + " successfully!");
//            LineReader reader = LineReaderBuilder.builder().build();
//
//            while (true) {
//                try {
//                    StringBuilder queryBuilder = new StringBuilder();
//                    String line = reader.readLine("SQL> ");
//
//                    if (line.trim().equalsIgnoreCase("exit")) break;
//
//                    // Multi-line input until ';'
//                    while (!line.trim().endsWith(";")) {
//                        queryBuilder.append(line).append(" ");
//                        line = reader.readLine("... ");
//                        if (line.trim().equalsIgnoreCase("exit")) break;
//                    }
//                    queryBuilder.append(line, 0, line.length() - 1); // remove trailing ';'
//
//                    String sql = queryBuilder.toString().trim();
//                    if (sql.isEmpty()) continue;
//
//                    executeQuery(connection, sql, reader);
//
//                } catch (UserInterruptException e) {
//                    System.out.println("^C Pressed. Type 'exit' to quit.");
//                } catch (EndOfFileException e) {
//                    System.out.println("Exiting...");
//                    break;
//                }
//            }
//
//        } catch (SQLException e) {
//            System.out.println("❌ Connection failed: " + e.getMessage());
//        }
//    }
//
//    private static void executeQuery(Connection connection, String sql, LineReader reader) {
//        try (Statement stmt = connection.createStatement()) {
//            boolean hasResultSet = stmt.execute(sql);
//            if (hasResultSet) {
//                ResultSet rs = stmt.getResultSet();
//                printResultSet(rs, reader);
//                rs.close();
//            } else {
//                int count = stmt.getUpdateCount();
//                System.out.println("Statement executed successfully. Rows affected: " + count);
//            }
//        } catch (SQLException e) {
//            System.out.println("❌ SQL Error: " + e.getMessage());
//        }
//    }
//
//    private static void printResultSet(ResultSet rs, LineReader reader) throws SQLException {
//        ResultSetMetaData meta = rs.getMetaData();
//        int columns = meta.getColumnCount();
//
//        List<Integer> columnWidths = new ArrayList<>();
//        for (int i = 1; i <= columns; i++) {
//            columnWidths.add(meta.getColumnName(i).length());
//        }
//
//        List<String[]> rows = new ArrayList<>();
//        int rowCount = 0;
//        while (rs.next() && rowCount < MAX_ROWS) {
//            String[] row = new String[columns];
//            for (int i = 1; i <= columns; i++) {
//                row[i - 1] = rs.getString(i);
//                if (row[i - 1] != null && row[i - 1].length() > columnWidths.get(i - 1)) {
//                    columnWidths.set(i - 1, row[i - 1].length());
//                }
//            }
//            rows.add(row);
//            rowCount++;
//        }
//
//        // Header
//        for (int i = 1; i <= columns; i++) {
//            System.out.print(padRight(meta.getColumnName(i), columnWidths.get(i - 1)) + " | ");
//        }
//        System.out.println();
//
//        // Separator
//        for (int width : columnWidths) {
//            System.out.print(repeatChar('-', width) + "-+-");
//        }
//        System.out.println();
//
//        // Pagination
//        for (int i = 0; i < rows.size(); i++) {
//            printRow(rows.get(i), columnWidths);
//            if ((i + 1) % PAGE_SIZE == 0 && i < rows.size() - 1) {
//                System.out.print("Press Enter to continue...");
//                reader.readLine();
//            }
//        }
//
//        if (rowCount == MAX_ROWS && !rs.isAfterLast()) {
//            System.out.println("...Results truncated to " + MAX_ROWS + " rows.");
//        }
//
//        System.out.println(rows.size() + " row(s) displayed.");
//    }
//
//    private static void printRow(String[] row, List<Integer> widths) {
//        for (int i = 0; i < row.length; i++) {
//            System.out.print(padRight(row[i], widths.get(i)) + " | ");
//        }
//        System.out.println();
//    }
//
//    private static String padRight(String text, int length) {
//        if (text == null) text = "NULL";
//        return String.format("%-" + length + "s", text);
//    }
//
//    private static String repeatChar(char c, int times) {
//        StringBuilder sb = new StringBuilder(times);
//        for (int i = 0; i < times; i++) sb.append(c);
//        return sb.toString();
//    }
//}

//package org.nofel;
//
//import org.jline.reader.LineReader;
//import org.jline.reader.LineReaderBuilder;
//import org.jline.reader.UserInterruptException;
//import org.jline.reader.EndOfFileException;
//
//import java.io.FileInputStream;
//import java.io.IOException;
//import java.sql.*;
//import java.util.*;
//
//public class MultiEnvConsoleDbClient {
//
//    private static final int MAX_ROWS = 100;
//    private static final int PAGE_SIZE = 20;
//    private static final Properties dbProps = new Properties();
//
//    public static void main(String[] args) {
//
//        // Load DB config
//        try (FileInputStream fis = new FileInputStream("dbconfig.properties")) {
//            dbProps.load(fis);
//        } catch (IOException e) {
//            System.out.println("❌ Failed to load dbconfig.properties: " + e.getMessage());
//            return;
//        }
//
//        Scanner sc = new Scanner(System.in);
//
//        String env = choose(sc, "Select Environment", Arrays.asList("INHOUSE", "SIT"));
//        String db = choose(sc, "Select Database",
//                Arrays.asList("MIDDLEWARE", "OPENAPI", "WALLET", "SWITCH"));
//
//        String prefix = env + "." + db;
//        String url = dbProps.getProperty(prefix + ".url");
//        String user = dbProps.getProperty(prefix + ".username");
//        String pass = dbProps.getProperty(prefix + ".password");
//
//        if (url == null || user == null || pass == null) {
//            System.out.println("❌ Missing DB credentials for " + prefix);
//            return;
//        }
//
//        try (Connection conn = DriverManager.getConnection(url, user, pass)) {
//
//            System.out.println("✅ Connected to " + env + " / " + db);
//            LineReader reader = LineReaderBuilder.builder().build();
//
//            while (true) {
//                try {
//                    String sqlBlock = readSqlBlock(reader);
//                    if (sqlBlock == null) break;
//
//                    List<String> statements = splitStatements(sqlBlock);
//                    for (String stmt : statements) {
//                        execute(conn, stmt.trim(), reader);
//                    }
//
//                } catch (UserInterruptException e) {
//                    System.out.println("^C — Type 'exit' to quit");
//                } catch (EndOfFileException e) {
//                    break;
//                }
//            }
//
//        } catch (SQLException e) {
//            System.out.println("❌ Connection failed: " + e.getMessage());
//        }
//    }
//
//    // ---------- INPUT ----------
//
//    private static String readSqlBlock(LineReader reader) {
//        StringBuilder sb = new StringBuilder();
//        String line = reader.readLine("SQL> ");
//
//        if ("exit".equalsIgnoreCase(line.trim())) return null;
//
//        while (!line.trim().endsWith(";")) {
//            sb.append(line).append("\n");
//            line = reader.readLine("... ");
//            if ("exit".equalsIgnoreCase(line.trim())) return null;
//        }
//
//        sb.append(line, 0, line.length() - 1); // remove ;
//        return sb.toString().trim();
//    }
//
//    private static List<String> splitStatements(String sql) {
//        List<String> list = new ArrayList<>();
//        StringBuilder current = new StringBuilder();
//        boolean inString = false;
//
//        for (char c : sql.toCharArray()) {
//            if (c == '\'') inString = !inString;
//
//            if (c == ';' && !inString) {
//                list.add(current.toString());
//                current.setLength(0);
//            } else {
//                current.append(c);
//            }
//        }
//
//        if (current.length() > 0)
//            list.add(current.toString());
//
//        return list;
//    }
//
//    // ---------- EXECUTION ----------
//
//    private static void execute(Connection conn, String sql, LineReader reader) {
//
//        if (sql.isEmpty()) return;
//
//        if (isDescribe(sql)) {
//            describeTable(conn, extractTable(sql), reader);
//            return;
//        }
//
//        try (Statement stmt = conn.createStatement()) {
//
//            boolean hasResult = stmt.execute(sql);
//
//            if (hasResult) {
//                printResultSet(stmt.getResultSet(), reader);
//            } else {
//                System.out.println("✔ Rows affected: " + stmt.getUpdateCount());
//            }
//
//        } catch (SQLException e) {
//            System.out.println("❌ SQL Error: " + e.getMessage());
//        }
//    }
//
//    // ---------- DESCRIBE ----------
//
//    private static boolean isDescribe(String sql) {
//        String s = sql.trim().toLowerCase();
//        return s.startsWith("desc ") || s.startsWith("describe ");
//    }
//
//    private static String extractTable(String sql) {
//        return sql.trim().split("\\s+")[1];
//    }
//
//    private static void describeTable(Connection conn, String table, LineReader reader) {
//        String q =
//                "SELECT column_name, data_type, data_length, nullable " +
//                        "FROM user_tab_columns WHERE table_name = '" + table.toUpperCase() + "' " +
//                        "ORDER BY column_id";
//
//        try (Statement stmt = conn.createStatement();
//             ResultSet rs = stmt.executeQuery(q)) {
//            printResultSet(rs, reader);
//        } catch (SQLException e) {
//            System.out.println("❌ DESCRIBE failed: " + e.getMessage());
//        }
//    }
//
//    // ---------- RESULT SET ----------
//
//    private static void printResultSet(ResultSet rs, LineReader reader) throws SQLException {
//
//        ResultSetMetaData meta = rs.getMetaData();
//        int cols = meta.getColumnCount();
//
//        List<Integer> widths = new ArrayList<>();
//        for (int i = 1; i <= cols; i++) {
//            widths.add(meta.getColumnName(i).length());
//        }
//
//        List<String[]> rows = new ArrayList<>();
//        int count = 0;
//
//        while (rs.next() && count < MAX_ROWS) {
//            String[] row = new String[cols];
//            for (int i = 1; i <= cols; i++) {
//                row[i - 1] = rs.getString(i);
//                if (row[i - 1] != null && row[i - 1].length() > widths.get(i - 1)) {
//                    widths.set(i - 1, row[i - 1].length());
//                }
//            }
//            rows.add(row);
//            count++;
//        }
//
//        // Header
//        for (int i = 1; i <= cols; i++) {
//            System.out.print(pad(meta.getColumnName(i), widths.get(i - 1)) + " | ");
//        }
//        System.out.println();
//
//        for (int w : widths) {
//            System.out.print(repeat('-', w) + "-+-");
//        }
//        System.out.println();
//
//        // Rows
//        for (int i = 0; i < rows.size(); i++) {
//            printRow(rows.get(i), widths);
//            if ((i + 1) % PAGE_SIZE == 0 && i < rows.size() - 1) {
//                reader.readLine("Press Enter...");
//            }
//        }
//
//        if (count == MAX_ROWS) {
//            System.out.println("... truncated to " + MAX_ROWS + " rows");
//        }
//
//        System.out.println(rows.size() + " row(s)");
//    }
//
//    private static void printRow(String[] row, List<Integer> widths) {
//        for (int i = 0; i < row.length; i++) {
//            System.out.print(pad(row[i], widths.get(i)) + " | ");
//        }
//        System.out.println();
//    }
//
//    // ---------- UTILS ----------
//
//    private static String pad(String s, int len) {
//        if (s == null) s = "NULL";
//        return String.format("%-" + len + "s", s);
//    }
//
//    private static String repeat(char c, int n) {
//        StringBuilder sb = new StringBuilder();
//        for (int i = 0; i < n; i++) sb.append(c);
//        return sb.toString();
//    }
//
//    private static String choose(Scanner sc, String title, List<String> opts) {
//        System.out.println(title + ":");
//        for (int i = 0; i < opts.size(); i++) {
//            System.out.println((i + 1) + ". " + opts.get(i));
//        }
//
//        while (true) {
//            System.out.print("> ");
//            try {
//                int i = Integer.parseInt(sc.nextLine()) - 1;
//                if (i >= 0 && i < opts.size()) return opts.get(i);
//            } catch (Exception ignored) {}
//            System.out.println("Invalid choice.");
//        }
//    }
//}

package org.nofel;

import org.jline.reader.LineReader;
import org.jline.reader.LineReaderBuilder;
import org.jline.reader.UserInterruptException;
import org.jline.reader.EndOfFileException;

import java.io.FileInputStream;
import java.io.IOException;
import java.sql.*;
import java.util.*;

public class MultiEnvConsoleDbClient {

    private static final int MAX_ROWS = 100;      // Max rows per query
    private static final int PAGE_SIZE = 20;      // Rows per page
    private static final int MAX_COL_WIDTH = 30;  // Max width per column before truncation
    private static final int HORIZONTAL_THRESHOLD = 10; // Columns after which vertical mode activates
    private static Properties dbProps = new Properties();

    public static void main(String[] args) {
        // Load DB config
        try (FileInputStream fis = new FileInputStream("dbconfig.properties")) {
            dbProps.load(fis);
        } catch (IOException e) {
            System.out.println("❌ Failed to load dbconfig.properties: " + e.getMessage());
            return;
        }

        Scanner sc = new Scanner(System.in);

        String env = selectEnvironment(sc);
        String db = selectDatabase(sc);

        String keyPrefix = env + "." + db;
        String url = dbProps.getProperty(keyPrefix + ".url");
        String username = dbProps.getProperty(keyPrefix + ".username");
        String password = dbProps.getProperty(keyPrefix + ".password");

        if (url == null || username == null || password == null) {
            System.out.println("❌ Credentials not found for " + keyPrefix);
            return;
        }

        try (Connection connection = DriverManager.getConnection(url, username, password)) {
            System.out.println("✅ Connected to " + env + " " + db + " successfully!");
            LineReader reader = LineReaderBuilder.builder().build();
            runConsole(connection, reader);
        } catch (SQLException e) {
            System.out.println("❌ Connection failed: " + e.getMessage());
        }
    }

    private static String selectEnvironment(Scanner sc) {
        System.out.println("Select Environment:");
        System.out.println("1. INHOUSE");
        System.out.println("2. SIT");
        String env = null;
        while (env == null) {
            System.out.print("Enter choice (1-2): ");
            String choice = sc.nextLine().trim();
            switch (choice) {
                case "1": env = "INHOUSE"; break;
                case "2": env = "SIT"; break;
                default: System.out.println("Invalid choice. Try again.");
            }
        }
        return env;
    }

    private static String selectDatabase(Scanner sc) {
        List<String> dbOptions = Arrays.asList("MIDDLEWARE", "OPENAPI", "WALLET", "SWITCH");
        System.out.println("Select Database:");
        for (int i = 0; i < dbOptions.size(); i++) {
            System.out.println((i + 1) + ". " + dbOptions.get(i));
        }
        String db = null;
        while (db == null) {
            System.out.print("Enter choice (1-4): ");
            String choice = sc.nextLine().trim();
            try {
                int idx = Integer.parseInt(choice) - 1;
                if (idx >= 0 && idx < dbOptions.size()) db = dbOptions.get(idx);
                else System.out.println("Invalid choice. Try again.");
            } catch (NumberFormatException e) {
                System.out.println("Invalid input. Try again.");
            }
        }
        return db;
    }

    private static void runConsole(Connection connection, LineReader reader) {
        while (true) {
            try {
                StringBuilder queryBuilder = new StringBuilder();
                String line = reader.readLine("SQL> ");
                if (line.trim().equalsIgnoreCase("exit")) break;

                while (!line.trim().endsWith(";")) {
                    queryBuilder.append(line).append(" ");
                    line = reader.readLine("... ");
                    if (line.trim().equalsIgnoreCase("exit")) break;
                }
                queryBuilder.append(line, 0, line.length() - 1);

                String sql = queryBuilder.toString().trim();
                if (sql.isEmpty()) continue;

                executeQuery(connection, sql, reader);

            } catch (UserInterruptException e) {
                System.out.println("^C Pressed. Type 'exit' to quit.");
            } catch (EndOfFileException e) {
                System.out.println("Exiting...");
                break;
            }
        }
    }

    private static void executeQuery(Connection connection, String sql, LineReader reader) {
        try (Statement stmt = connection.createStatement()) {
            boolean hasResultSet = stmt.execute(sql);
            if (hasResultSet) {
                ResultSet rs = stmt.getResultSet();
                printResultSet(rs, reader);
                rs.close();
            } else {
                int count = stmt.getUpdateCount();
                System.out.println("Statement executed successfully. Rows affected: " + count);
            }
        } catch (SQLException e) {
            System.out.println("❌ SQL Error: " + e.getMessage());
        }
    }

    private static void printResultSet(ResultSet rs, LineReader reader) throws SQLException {
        ResultSetMetaData meta = rs.getMetaData();
        int columns = meta.getColumnCount();

        List<Integer> colWidths = new ArrayList<>();
        for (int i = 1; i <= columns; i++) {
            colWidths.add(Math.min(meta.getColumnName(i).length(), MAX_COL_WIDTH));
        }

        List<String[]> rows = new ArrayList<>();
        int rowCount = 0;
        while (rs.next() && rowCount < MAX_ROWS) {
            String[] row = new String[columns];
            for (int i = 1; i <= columns; i++) {
                String val = rs.getString(i);
                if (val == null) val = "NULL";
                if (val.length() > MAX_COL_WIDTH) val = val.substring(0, MAX_COL_WIDTH - 3) + "...";
                row[i - 1] = val;
                if (val.length() > colWidths.get(i - 1)) colWidths.set(i - 1, val.length());
            }
            rows.add(row);
            rowCount++;
        }

        // Choose display mode
        if (columns > HORIZONTAL_THRESHOLD) {
            printVerticalMode(meta, rows, reader);
        } else {
            printHorizontalMode(meta, rows, colWidths, reader);
        }

        if (rowCount == MAX_ROWS && !rs.isAfterLast()) {
            System.out.println("...Results truncated to " + MAX_ROWS + " rows.");
        }

        System.out.println(rows.size() + " row(s) displayed.");
    }

    // Horizontal mode for small-medium tables
    private static void printHorizontalMode(ResultSetMetaData meta, List<String[]> rows, List<Integer> widths,
                                            LineReader reader) throws SQLException {
        int columns = widths.size();
        int startCol = 0;
        while (startCol < columns) {
            int displayedCols = 0;
            int totalWidth = 0;
            while (startCol + displayedCols < columns && totalWidth + widths.get(startCol + displayedCols) + 3 <= 120) {
                totalWidth += widths.get(startCol + displayedCols) + 3;
                displayedCols++;
            }

            // Header
            for (int i = startCol; i < startCol + displayedCols; i++) {
                System.out.print(padRight(meta.getColumnName(i + 1), widths.get(i)) + " | ");
            }
            System.out.println();

            // Separator
            for (int i = startCol; i < startCol + displayedCols; i++) {
                System.out.print(repeatChar('-', widths.get(i)) + "-+-");
            }
            System.out.println();

            // Rows
            for (int r = 0; r < rows.size(); r++) {
                for (int c = startCol; c < startCol + displayedCols; c++) {
                    System.out.print(padRight(rows.get(r)[c], widths.get(c)) + " | ");
                }
                System.out.println();
                if ((r + 1) % PAGE_SIZE == 0 && r < rows.size() - 1) {
                    System.out.print("Press Enter to continue...");
                    reader.readLine();
                }
            }

            startCol += displayedCols;
            if (startCol < columns) {
                System.out.print("Press Enter to view next columns...");
                reader.readLine();
            }
        }
    }

    // Vertical mode for extremely wide tables
    private static void printVerticalMode(ResultSetMetaData meta, List<String[]> rows, LineReader reader) throws SQLException {
        for (int r = 0; r < rows.size(); r++) {
            System.out.println("----- Row " + (r + 1) + " -----");
            for (int c = 0; c < rows.get(r).length; c++) {
                System.out.println(meta.getColumnName(c + 1) + ": " + rows.get(r)[c]);
            }
            if ((r + 1) % PAGE_SIZE == 0 && r < rows.size() - 1) {
                System.out.print("Press Enter to continue...");
                reader.readLine();
            } else if (r < rows.size() - 1) {
                System.out.println();
            }
        }
    }

    private static String padRight(String text, int length) {
        if (text == null) text = "NULL";
        return String.format("%-" + length + "s", text);
    }

    private static String repeatChar(char c, int times) {
        StringBuilder sb = new StringBuilder(times);
        for (int i = 0; i < times; i++) sb.append(c);
        return sb.toString();
    }
}

