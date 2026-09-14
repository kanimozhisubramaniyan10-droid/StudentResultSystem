package com.studentresult;

import java.awt.Desktop;
import java.io.File;
import java.io.FileOutputStream;
import java.sql.*;
import java.util.Scanner;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

public final class StudentResultSystem {

    private static final String DB_URL = "jdbc:sqlite:student_results.db";
    private static final String EXCEL_FILE = "StudentResults.xlsx";
    private static final Scanner INPUT = new Scanner(System.in);

    public static void main(String[] args) {

        createTable();

        System.out.println("\n==============================================");
        System.out.println("       STUDENT RESULT MANAGEMENT SYSTEM");
        System.out.println("              JDBC + SQLite");
        System.out.println("==============================================");

        while (true) {

            System.out.println("\n1. Add student result");
            System.out.println("2. View all results");
            System.out.println("3. Search by roll number");
            System.out.println("4. Exit");

            int choice = readInt("Enter your choice: ");

            switch (choice) {

                case 1 -> addResult();

                case 2 -> viewAllResults();

                case 3 -> searchByRollNumber();

                case 4 -> {
                    System.out.println("\nThank you for using Student Result System.");
                    INPUT.close();
                    return;
                }

                default ->
                    System.out.println(
                        "\nPlease enter a number from 1 to 4."
                    );
            }
        }
    }


    // ============================================================
    // DATABASE CONNECTION
    // ============================================================

    private static Connection connect() throws SQLException {

        return DriverManager.getConnection(DB_URL);
    }


    // ============================================================
    // CREATE TABLE
    // ============================================================

    private static void createTable() {

        String sql = """
                CREATE TABLE IF NOT EXISTS student_results (
                    roll_no INTEGER PRIMARY KEY,
                    name TEXT NOT NULL,
                    class_name TEXT NOT NULL,
                    tamil INTEGER NOT NULL CHECK(tamil BETWEEN 0 AND 100),
                    english INTEGER NOT NULL CHECK(english BETWEEN 0 AND 100),
                    maths INTEGER NOT NULL CHECK(maths BETWEEN 0 AND 100),
                    science INTEGER NOT NULL CHECK(science BETWEEN 0 AND 100),
                    social_science INTEGER NOT NULL CHECK(social_science BETWEEN 0 AND 100)
                )
                """;

        try (
            Connection con = connect();
            Statement statement = con.createStatement()
        ) {

            statement.execute(sql);

        } catch (SQLException e) {

            throw new IllegalStateException(
                "Unable to create database table: " + e.getMessage(),
                e
            );
        }
    }


    // ============================================================
    // ADD STUDENT RESULT
    // ============================================================

    private static void addResult() {

        System.out.println("\n========== ADD STUDENT RESULT ==========");

        String sql = """
                INSERT INTO student_results
                VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                """;

        int rollNo = readInt("Roll number: ");

        String name = readText("Student name: ");

        String className = readText("Class: ");

        int tamil = readMark("Tamil");

        int english = readMark("English");

        int maths = readMark("Maths");

        int science = readMark("Science");

        int socialScience = readMark("Social Science");


        try (
            Connection con = connect();
            PreparedStatement ps = con.prepareStatement(sql)
        ) {

            ps.setInt(1, rollNo);
            ps.setString(2, name);
            ps.setString(3, className);
            ps.setInt(4, tamil);
            ps.setInt(5, english);
            ps.setInt(6, maths);
            ps.setInt(7, science);
            ps.setInt(8, socialScience);

            ps.executeUpdate();

            System.out.println(
                "\nStudent result saved successfully in database."
            );

            // Export database data to Excel
            exportToExcel();

            // Open Excel automatically
            openExcelFile();

        } catch (SQLException e) {

            if (
                e.getMessage() != null &&
                e.getMessage().toUpperCase().contains("PRIMARY KEY")
            ) {

                System.out.println(
                    "\nThis roll number already exists."
                );

            } else {

                System.out.println(
                    "\nCould not save result: " + e.getMessage()
                );
            }
        }
    }


    // ============================================================
    // EXPORT DATABASE TO EXCEL
    // ============================================================

    private static void exportToExcel() {

        String sql = """
                SELECT
                    roll_no,
                    name,
                    class_name,
                    tamil,
                    english,
                    maths,
                    science,
                    social_science,
                    tamil + english + maths + science + social_science AS total
                FROM student_results
                ORDER BY roll_no
                """;

        try (
            Connection con = connect();
            Statement statement = con.createStatement();
            ResultSet rs = statement.executeQuery(sql);
            Workbook workbook = new XSSFWorkbook()
        ) {

            Sheet sheet = workbook.createSheet("Student Results");


            // ====================================================
            // HEADER STYLE
            // ====================================================

            CellStyle headerStyle = workbook.createCellStyle();

            Font headerFont = workbook.createFont();

            headerFont.setBold(true);

            headerStyle.setFont(headerFont);


            // ====================================================
            // EXCEL HEADERS
            // ====================================================

            String[] columns = {
                "Roll Number",
                "Name",
                "Class",
                "Tamil",
                "English",
                "Maths",
                "Science",
                "Social Science",
                "Total",
                "Percentage",
                "Grade",
                "Result"
            };


            Row header = sheet.createRow(0);

            for (int i = 0; i < columns.length; i++) {

                Cell cell = header.createCell(i);

                cell.setCellValue(columns[i]);

                cell.setCellStyle(headerStyle);
            }


            // ====================================================
            // STUDENT DATA
            // ====================================================

            int rowNumber = 1;

            while (rs.next()) {

                Row row = sheet.createRow(rowNumber++);

                int tamil = rs.getInt("tamil");
                int english = rs.getInt("english");
                int maths = rs.getInt("maths");
                int science = rs.getInt("science");
                int socialScience = rs.getInt("social_science");

                int total =
                    tamil +
                    english +
                    maths +
                    science +
                    socialScience;

                double percentage = total / 5.0;


                String result =
                    tamil >= 35 &&
                    english >= 35 &&
                    maths >= 35 &&
                    science >= 35 &&
                    socialScience >= 35
                    ? "PASS"
                    : "FAIL";


                // ------------------------------------------------
                // Column 0 - Roll Number
                // ------------------------------------------------

                row.createCell(0).setCellValue(
                    rs.getInt("roll_no")
                );


                // ------------------------------------------------
                // Column 1 - Name
                // ------------------------------------------------

                row.createCell(1).setCellValue(
                    rs.getString("name")
                );


                // ------------------------------------------------
                // Column 2 - Class
                // ------------------------------------------------

                row.createCell(2).setCellValue(
                    rs.getString("class_name")
                );


                // ------------------------------------------------
                // Column 3 - Tamil
                // ------------------------------------------------

                row.createCell(3).setCellValue(tamil);


                // ------------------------------------------------
                // Column 4 - English
                // ------------------------------------------------

                row.createCell(4).setCellValue(english);


                // ------------------------------------------------
                // Column 5 - Maths
                // ------------------------------------------------

                row.createCell(5).setCellValue(maths);


                // ------------------------------------------------
                // Column 6 - Science
                // ------------------------------------------------

                row.createCell(6).setCellValue(science);


                // ------------------------------------------------
                // Column 7 - Social Science
                // ------------------------------------------------

                row.createCell(7).setCellValue(socialScience);


                // ------------------------------------------------
                // Column 8 - Total
                // ------------------------------------------------

                row.createCell(8).setCellValue(total);


                // ------------------------------------------------
                // Column 9 - Percentage
                // ------------------------------------------------

                row.createCell(9).setCellValue(percentage);


                // ------------------------------------------------
                // Column 10 - Grade
                // ------------------------------------------------

                row.createCell(10).setCellValue(
                    grade(percentage)
                );


                // ------------------------------------------------
                // Column 11 - Result
                // ------------------------------------------------

                row.createCell(11).setCellValue(result);
            }


            // ====================================================
            // FORMAT PERCENTAGE COLUMN
            // ====================================================

            CellStyle percentageStyle =
                workbook.createCellStyle();

            percentageStyle.setDataFormat(
                workbook.createDataFormat()
                    .getFormat("0.00")
            );

            for (int i = 1; i < rowNumber; i++) {

                sheet.getRow(i)
                    .getCell(9)
                    .setCellStyle(percentageStyle);
            }


            // ====================================================
            // AUTO SIZE COLUMNS
            // ====================================================

            for (int i = 0; i < columns.length; i++) {

                sheet.autoSizeColumn(i);
            }


            // ====================================================
            // SAVE EXCEL FILE
            // ====================================================

            try (
                FileOutputStream output =
                    new FileOutputStream(EXCEL_FILE)
            ) {

                workbook.write(output);
            }


            System.out.println(
                "\nStudentResults.xlsx updated successfully."
            );

        } catch (Exception e) {

            System.out.println(
                "\nCould not update Excel file: "
                + e.getMessage()
            );
        }
    }


    // ============================================================
    // OPEN EXCEL FILE
    // ============================================================

    private static void openExcelFile() {

        try {

            File file = new File(EXCEL_FILE);

            if (!file.exists()) {

                System.out.println(
                    "Excel file not found."
                );

                return;
            }


            if (isDesktopSupported()) {

                Desktop.getDesktop().open(file);

            } else {

                System.out.println(
                    "Automatic Excel opening is not supported on this system."
                );
            }

        } catch (Exception e) {

            System.out.println(
                "Could not open Excel file: "
                + e.getMessage()
            );
        }
    }


    // ============================================================
    // DESKTOP SUPPORT
    // ============================================================

    private static boolean isDesktopSupported() {

        return Desktop.isDesktopSupported()
            && Desktop.getDesktop()
                .isSupported(Desktop.Action.OPEN);
    }


    // ============================================================
    // VIEW ALL RESULTS
    // ============================================================

    private static void viewAllResults() {

        System.out.println("\n========== ALL STUDENT RESULTS ==========");

        String sql = """
                SELECT
                    *,
                    tamil + english + maths + science + social_science AS total
                FROM student_results
                ORDER BY roll_no
                """;


        try (
            Connection con = connect();
            Statement st = con.createStatement();
            ResultSet rs = st.executeQuery(sql)
        ) {

            boolean found = false;

            while (rs.next()) {

                printResult(rs);

                found = true;
            }


            if (!found) {

                System.out.println(
                    "\nNo student results found."
                );
            }

        } catch (SQLException e) {

            System.out.println(
                "\nCould not read results: "
                + e.getMessage()
            );
        }
    }


    // ============================================================
    // SEARCH BY ROLL NUMBER
    // ============================================================

    private static void searchByRollNumber() {

        System.out.println("\n========== SEARCH STUDENT ==========");

        String sql = """
                SELECT
                    *,
                    tamil + english + maths + science + social_science AS total
                FROM student_results
                WHERE roll_no = ?
                """;


        int rollNo = readInt(
            "Roll number to search: "
        );


        try (
            Connection con = connect();
            PreparedStatement ps =
                con.prepareStatement(sql)
        ) {

            ps.setInt(1, rollNo);


            try (ResultSet rs = ps.executeQuery()) {

                if (rs.next()) {

                    printResult(rs);

                } else {

                    System.out.println(
                        "\nStudent not found."
                    );
                }
            }

        } catch (SQLException e) {

            System.out.println(
                "\nCould not search result: "
                + e.getMessage()
            );
        }
    }


    // ============================================================
    // PRINT RESULT
    // ============================================================

    private static void printResult(ResultSet rs)
        throws SQLException {

        int tamil = rs.getInt("tamil");
        int english = rs.getInt("english");
        int maths = rs.getInt("maths");
        int science = rs.getInt("science");
        int socialScience = rs.getInt("social_science");


        int total =
            tamil +
            english +
            maths +
            science +
            socialScience;


        double percentage = total / 5.0;


        String result =
            tamil >= 35 &&
            english >= 35 &&
            maths >= 35 &&
            science >= 35 &&
            socialScience >= 35
            ? "PASS"
            : "FAIL";


        System.out.println(
            "\n----------------------------------------"
        );

        System.out.println(
            "           STUDENT RESULT"
        );

        System.out.println(
            "----------------------------------------"
        );

        System.out.println(
            "Roll No: " + rs.getInt("roll_no")
        );

        System.out.println(
            "Name: " + rs.getString("name")
        );

        System.out.println(
            "Class: " + rs.getString("class_name")
        );

        System.out.println();

        System.out.println(
            "Tamil: " + tamil
        );

        System.out.println(
            "English: " + english
        );

        System.out.println(
            "Maths: " + maths
        );

        System.out.println(
            "Science: " + science
        );

        System.out.println(
            "Social Science: " + socialScience
        );

        System.out.println();

        System.out.println(
            "Total: " + total + " / 500"
        );

        System.out.printf(
            "Percentage: %.2f%%%n",
            percentage
        );

        System.out.println(
            "Grade: " + grade(percentage)
        );

        System.out.println(
            "Result: " + result
        );

        System.out.println(
            "----------------------------------------"
        );
    }


    // ============================================================
    // GRADE CALCULATION
    // ============================================================

    private static String grade(double percentage) {

        if (percentage >= 90) {
            return "A";
        }

        if (percentage >= 80) {
            return "B";
        }

        if (percentage >= 70) {
            return "C";
        }

        if (percentage >= 60) {
            return "D";
        }

        if (percentage >= 35) {
            return "E";
        }

        return "F";
    }


    // ============================================================
    // MARK VALIDATION
    // ============================================================

    private static int readMark(String subject) {

        while (true) {

            int mark = readInt(
                subject + " mark (0-100): "
            );


            if (mark >= 0 && mark <= 100) {

                return mark;
            }


            System.out.println(
                "Mark must be between 0 and 100."
            );
        }
    }


    // ============================================================
    // INTEGER INPUT
    // ============================================================

    private static int readInt(String prompt) {

        while (true) {

            try {

                System.out.print(prompt);

                return Integer.parseInt(
                    INPUT.nextLine().trim()
                );

            } catch (NumberFormatException e) {

                System.out.println(
                    "Please enter a valid number."
                );
            }
        }
    }


    // ============================================================
    // TEXT INPUT
    // ============================================================

    private static String readText(String prompt) {

        while (true) {

            System.out.print(prompt);

            String value =
                INPUT.nextLine().trim();


            if (!value.isEmpty()) {

                return value;
            }


            System.out.println(
                "This field cannot be empty."
            );
        }
    }
}