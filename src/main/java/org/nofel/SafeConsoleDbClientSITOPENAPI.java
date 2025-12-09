//package org.nofel;
//
//import org.jline.reader.LineReader;
//import org.jline.reader.LineReaderBuilder;
//import org.jline.reader.UserInterruptException;
//import org.jline.reader.EndOfFileException;
//
//import java.sql.*;
//import java.util.ArrayList;
//import java.util.List;
//
//public class SafeConsoleDbClientSITOPENAPI {
//
//    private static final String URL = "jdbc:oracle:thin:@192.168.207.1:1524:apidb";
//    private static final String USERNAME = "api";
//    private static final String PASSWORD = "V1ct0r13F3lls#2022";
//
//    private static final int MAX_ROWS = 50;  // limit rows for SELECT queries
//    private static final int PAGE_SIZE = 20; // rows per page for pagination
//
//    public static void main(String[] args) {
//
//        System.out.println("Connecting to Oracle SIT OPENAPI DB...");
//
//        try (Connection connection = DriverManager.getConnection(URL, USERNAME, PASSWORD)) {
//
//            System.out.println("✅ Connected to SIT OPENAPI DB successfully!");
//
//            // Initialize JLine reader
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
//                    // Ctrl+C pressed
//                    System.out.println("^C Pressed. Type 'exit' to quit.");
//                } catch (EndOfFileException e) {
//                    // Ctrl+D pressed
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
//
//            boolean hasResultSet = stmt.execute(sql);
//
//            if (hasResultSet) {
//                ResultSet rs = stmt.getResultSet();
//                printResultSet(rs, reader);
//                rs.close();
//            } else {
//                int count = stmt.getUpdateCount();
//                System.out.println("Statement executed successfully. Rows affected: " + count);
//            }
//
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
//        // Print header
//        for (int i = 1; i <= columns; i++) {
//            System.out.print(padRight(meta.getColumnName(i), columnWidths.get(i - 1)) + " | ");
//        }
//        System.out.println();
//
//        // Print separator
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
//            System.out.println("...Results truncated to " + MAX_ROWS + " rows. Refine your query to see more.");
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
//        for (int i = 0; i < times; i++) {
//            sb.append(c);
//        }
//        return sb.toString();
//    }
//}
