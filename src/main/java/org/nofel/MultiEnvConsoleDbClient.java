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

    private static final int MAX_ROWS = 50;
    private static final int PAGE_SIZE = 20;
    private static Properties dbProps = new Properties();

    public static void main(String[] args) {
        // Load properties from external file
        try (FileInputStream fis = new FileInputStream("dbconfig.properties")) {
            dbProps.load(fis);
        } catch (IOException e) {
            System.out.println("❌ Failed to load dbconfig.properties: " + e.getMessage());
            return;
        }

        Scanner sc = new Scanner(System.in);

        // Step 1: Select environment
        System.out.println("Select Environment:");
        System.out.println("1. INHOUSE");
        System.out.println("2. SIT");
        String env = null;
        while (env == null) {
            System.out.print("Enter choice (1-2): ");
            String choice = sc.nextLine().trim();
            switch (choice) {
                case "1":
                    env = "INHOUSE";
                    break;
                case "2":
                    env = "SIT";
                    break;
                default:
                    System.out.println("Invalid choice. Try again.");
            }
        }

        // Step 2: Select database
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

        // Step 3: Fetch credentials
        String keyPrefix = env + "." + db;
        String url = dbProps.getProperty(keyPrefix + ".url");
        String username = dbProps.getProperty(keyPrefix + ".username");
        String password = dbProps.getProperty(keyPrefix + ".password");

        if (url == null || username == null || password == null) {
            System.out.println("❌ Credentials not found for " + keyPrefix);
            return;
        }

        // Step 4: Connect and launch console
        try (Connection connection = DriverManager.getConnection(url, username, password)) {
            System.out.println("✅ Connected to " + env + " " + db + " successfully!");
            LineReader reader = LineReaderBuilder.builder().build();

            while (true) {
                try {
                    StringBuilder queryBuilder = new StringBuilder();
                    String line = reader.readLine("SQL> ");

                    if (line.trim().equalsIgnoreCase("exit")) break;

                    // Multi-line input until ';'
                    while (!line.trim().endsWith(";")) {
                        queryBuilder.append(line).append(" ");
                        line = reader.readLine("... ");
                        if (line.trim().equalsIgnoreCase("exit")) break;
                    }
                    queryBuilder.append(line, 0, line.length() - 1); // remove trailing ';'

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

        } catch (SQLException e) {
            System.out.println("❌ Connection failed: " + e.getMessage());
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

        List<Integer> columnWidths = new ArrayList<>();
        for (int i = 1; i <= columns; i++) {
            columnWidths.add(meta.getColumnName(i).length());
        }

        List<String[]> rows = new ArrayList<>();
        int rowCount = 0;
        while (rs.next() && rowCount < MAX_ROWS) {
            String[] row = new String[columns];
            for (int i = 1; i <= columns; i++) {
                row[i - 1] = rs.getString(i);
                if (row[i - 1] != null && row[i - 1].length() > columnWidths.get(i - 1)) {
                    columnWidths.set(i - 1, row[i - 1].length());
                }
            }
            rows.add(row);
            rowCount++;
        }

        // Header
        for (int i = 1; i <= columns; i++) {
            System.out.print(padRight(meta.getColumnName(i), columnWidths.get(i - 1)) + " | ");
        }
        System.out.println();

        // Separator
        for (int width : columnWidths) {
            System.out.print(repeatChar('-', width) + "-+-");
        }
        System.out.println();

        // Pagination
        for (int i = 0; i < rows.size(); i++) {
            printRow(rows.get(i), columnWidths);
            if ((i + 1) % PAGE_SIZE == 0 && i < rows.size() - 1) {
                System.out.print("Press Enter to continue...");
                reader.readLine();
            }
        }

        if (rowCount == MAX_ROWS && !rs.isAfterLast()) {
            System.out.println("...Results truncated to " + MAX_ROWS + " rows.");
        }

        System.out.println(rows.size() + " row(s) displayed.");
    }

    private static void printRow(String[] row, List<Integer> widths) {
        for (int i = 0; i < row.length; i++) {
            System.out.print(padRight(row[i], widths.get(i)) + " | ");
        }
        System.out.println();
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
