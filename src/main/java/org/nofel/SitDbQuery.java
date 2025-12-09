//package org.nofel;
//
//import java.sql.Connection;
//import java.sql.DriverManager;
//import java.sql.Statement;
//
//public class SitDbQuery {
//
//    public static void main(String[] args) {
//
//        String url = "jdbc:oracle:thin:@192.168.207.1:1524:apidb";
//        String username = "api";
//        String password = "V1ct0r13F3lls#2022";
//
//        try {
//            // Load Oracle Driver
//            Class.forName("oracle.jdbc.driver.OracleDriver");
//
//            // Create connection
//            Connection connection = DriverManager.getConnection(url, username, password);
//            System.out.println("Connected to SIT DB ✅");
//
//            // Create statement
//            Statement stmt = connection.createStatement();
//
//            // Run the UPDATE query
//            int rowsAffected = stmt.executeUpdate(
//                    "UPDATE u_users SET status = '1', logged = '0'"
//            );
//
//            System.out.println("Rows updated: " + rowsAffected);
//
//            // Close resources
//            stmt.close();
//            connection.close();
//
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//    }
//}
