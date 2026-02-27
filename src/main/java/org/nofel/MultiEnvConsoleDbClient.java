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

    private static final int MAX_ROWS = 500;          // Max rows per query
    private static final int PAGE_SIZE = 20;          // Rows per page
    private static final int MAX_COL_WIDTH = 500;      // Max width per column before truncation
    private static final int TERMINAL_WIDTH = 180;    // Terminal width for layout
    private static final int HORIZONTAL_THRESHOLD = 8; // Columns after which vertical mode activates
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
            System.out.println("💡 Commands: Type multiple queries separated by ';'. Exit with 'exit' or 'quit'.\n");
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

                if (line.trim().equalsIgnoreCase("exit") || line.trim().equalsIgnoreCase("quit")) {
                    break;
                }

                // Multi-line query support
                while (!line.trim().endsWith(";")) {
                    queryBuilder.append(line).append(" ");
                    line = reader.readLine("... ");
                    if (line.trim().equalsIgnoreCase("exit") || line.trim().equalsIgnoreCase("quit")) {
                        return;
                    }
                }
                queryBuilder.append(line);

                String input = queryBuilder.toString().trim();
                if (input.isEmpty()) continue;

                // Split and execute multiple queries
                executeMultipleQueries(connection, input, reader);

            } catch (UserInterruptException e) {
                System.out.println("^C Pressed. Type 'exit' to quit.");
            } catch (EndOfFileException e) {
                System.out.println("\nExiting...");
                break;
            }
        }
    }

    private static void executeMultipleQueries(Connection connection, String input, LineReader reader) {
        // Split by semicolon but handle edge cases
        String[] queries = input.split("(?<=;)\\s*");

        for (String sql : queries) {
            sql = sql.trim();
            if (sql.isEmpty()) continue;

            // Remove trailing semicolon if present
            if (sql.endsWith(";")) {
                sql = sql.substring(0, sql.length() - 1).trim();
            }

            executeQuery(connection, sql, reader);
        }
    }

    private static void executeQuery(Connection connection, String sql, LineReader reader) {
        try (Statement stmt = connection.createStatement()) {
            boolean hasResultSet = stmt.execute(sql);

            if (hasResultSet) {
                // SELECT or other queries that return result sets
                ResultSet rs = stmt.getResultSet();
                printResultSet(rs, reader);
                rs.close();
            } else {
                // INSERT, UPDATE, DELETE, CREATE, ALTER, DROP, etc.
                int count = stmt.getUpdateCount();
                String sqlUpper = sql.toUpperCase().trim();

                if (sqlUpper.startsWith("SELECT")) {
                    System.out.println("✅ Query executed. Rows returned: " + count);
                } else if (sqlUpper.startsWith("INSERT") || sqlUpper.startsWith("UPDATE") ||
                        sqlUpper.startsWith("DELETE")) {
                    System.out.println("✅ Statement executed successfully. Rows affected: " + count);
                } else if (sqlUpper.startsWith("CREATE") || sqlUpper.startsWith("ALTER") ||
                        sqlUpper.startsWith("DROP")) {
                    System.out.println("✅ DDL command executed successfully.");
                } else {
                    System.out.println("✅ Statement executed successfully.");
                }
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
                if (val == null) {
                    val = "NULL";
                } else if (val.length() > MAX_COL_WIDTH) {
                    val = val.substring(0, MAX_COL_WIDTH - 3) + "...";
                }
                row[i - 1] = val;
                colWidths.set(i - 1, Math.max(colWidths.get(i - 1), val.length()));
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
            System.out.println("\n⚠️  Results truncated to " + MAX_ROWS + " rows. Refine your query for faster results.");
        }

        System.out.println("\n📊 " + rows.size() + " row(s) displayed.\n");
    }

    // Horizontal mode for small-medium tables with full-width column display
    private static void printHorizontalMode(ResultSetMetaData meta, List<String[]> rows,
                                            List<Integer> widths, LineReader reader) throws SQLException {
        int columns = widths.size();
        int startCol = 0;
        boolean firstColumnSet = true;

        while (startCol < columns) {
            int displayedCols = 0;
            int totalWidth = 0;

            // Calculate how many columns fit in one screen
            while (startCol + displayedCols < columns) {
                int colWidth = widths.get(startCol + displayedCols) + 3; // +3 for " | "
                if (totalWidth + colWidth > TERMINAL_WIDTH && displayedCols > 0) {
                    break;
                }
                totalWidth += colWidth;
                displayedCols++;
            }

            if (displayedCols == 0) displayedCols = 1; // At least one column

            if (!firstColumnSet) {
                System.out.println("\n" + repeatChar('-', 60));
            }
            firstColumnSet = false;

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

            // Rows with pagination
            for (int r = 0; r < rows.size(); r++) {
                for (int c = startCol; c < startCol + displayedCols; c++) {
                    System.out.print(padRight(rows.get(r)[c], widths.get(c)) + " | ");
                }
                System.out.println();

                if ((r + 1) % PAGE_SIZE == 0 && r < rows.size() - 1) {
                    System.out.print("📄 Press Enter to continue...");
                    reader.readLine();
                }
            }

            startCol += displayedCols;
            if (startCol < columns) {
                System.out.print("➡️  Press Enter to view next columns...");
                reader.readLine();
            }
        }
    }

    // Vertical mode for extremely wide tables - shows one row at a time
    private static void printVerticalMode(ResultSetMetaData meta, List<String[]> rows, LineReader reader) throws SQLException {
        int columns = meta.getColumnCount();

        for (int r = 0; r < rows.size(); r++) {
            System.out.println("\n" + repeatChar('=', 80));
            System.out.println("📋 Row " + (r + 1) + " of " + rows.size());
            System.out.println(repeatChar('=', 80));

            for (int c = 0; c < columns; c++) {
                String colName = meta.getColumnName(c + 1);
                String colValue = rows.get(r)[c];

                // For longer values, display on next line
                if (colValue.length() > 60 || colValue.contains("\n")) {
                    System.out.println("\n" + colName + ":");
                    System.out.println("  " + colValue);
                } else {
                    System.out.println(colName + ": " + colValue);
                }
            }

            if ((r + 1) % PAGE_SIZE == 0 && r < rows.size() - 1) {
                System.out.print("\n📄 Press Enter to continue...");
                reader.readLine();
            }
        }
    }

    private static String padRight(String text, int length) {
        if (text == null) text = "NULL";
        if (text.length() > length) {
            text = text.substring(0, length - 3) + "...";
        }
        return String.format("%-" + length + "s", text);
    }

    private static String repeatChar(char c, int times) {
        StringBuilder sb = new StringBuilder(times);
        for (int i = 0; i < times; i++) sb.append(c);
        return sb.toString();
    }
}