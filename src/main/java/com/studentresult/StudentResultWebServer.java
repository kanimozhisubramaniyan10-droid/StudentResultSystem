package com.studentresult;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class StudentResultWebServer {

    private static final String DB_URL = "jdbc:sqlite:student_results.db";
    private static final int PORT = 8080;
    private static final Path WEBSITE_DIR = Paths.get("website").toAbsolutePath().normalize();

    private StudentResultWebServer() {
    }

    public static void main(String[] args) throws Exception {
        createTable();

        HttpServer server = HttpServer.create(new InetSocketAddress(PORT), 0);
        server.createContext("/api/students", StudentResultWebServer::handleStudentApi);
        server.createContext("/", StudentResultWebServer::handleStaticFiles);
        server.setExecutor(null);
        server.start();

        System.out.println("Student Result Website is running at http://localhost:8080");
        System.out.println("SQLite database: " + DB_URL);
    }

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

        try (Connection con = connect(); Statement stmt = con.createStatement()) {
            stmt.execute(sql);
        } catch (SQLException e) {
            throw new IllegalStateException("Unable to create database table: " + e.getMessage(), e);
        }
    }

    private static Connection connect() throws SQLException {
        return DriverManager.getConnection(DB_URL);
    }

    private static void handleStaticFiles(HttpExchange exchange) throws IOException {
        String path = exchange.getRequestURI().getPath();
        if (path == null || path.isBlank() || "/".equals(path)) {
            path = "/index.html";
        }

        if (path.startsWith("/api/")) {
            sendJson(exchange, 404, "{\"error\":\"Not found\"}");
            return;
        }

        Path resolved = WEBSITE_DIR.resolve(path.substring(1)).normalize();
        if (!resolved.startsWith(WEBSITE_DIR)) {
            sendText(exchange, 403, "Forbidden");
            return;
        }

        if (!Files.exists(resolved) || Files.isDirectory(resolved)) {
            sendText(exchange, 404, "Not found");
            return;
        }

        String contentType = Files.probeContentType(resolved);
        if (contentType == null) {
            contentType = "application/octet-stream";
        }

        byte[] fileBytes = Files.readAllBytes(resolved);
        exchange.getResponseHeaders().set("Content-Type", contentType);
        exchange.sendResponseHeaders(200, fileBytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(fileBytes);
        }
    }

    private static void handleStudentApi(HttpExchange exchange) throws IOException {
        String method = exchange.getRequestMethod();
        String path = exchange.getRequestURI().getPath();

        if ("GET".equalsIgnoreCase(method)) {
            handleGetStudents(exchange, exchange.getRequestURI().getRawQuery());
            return;
        }

        if ("POST".equalsIgnoreCase(method) && "/api/students".equals(path)) {
            handleCreateStudent(exchange);
            return;
        }

        sendJson(exchange, 405, "{\"error\":\"Method not allowed\"}");
    }

    private static void handleGetStudents(HttpExchange exchange, String query) throws IOException {
        String rollNoParam = null;
        if (query != null && !query.isBlank()) {
            for (String part : query.split("&")) {
                String[] pair = part.split("=", 2);
                if (pair.length == 2 && "rollNo".equals(pair[0])) {
                    rollNoParam = pair[1];
                }
            }
        }

        try {
            List<Map<String, Object>> students = fetchStudents(rollNoParam);
            sendJson(exchange, 200, toJson(students));
        } catch (SQLException e) {
            sendJson(exchange, 500, "{\"error\":\"Could not read students\"}");
        }
    }

    private static void handleCreateStudent(HttpExchange exchange) throws IOException {
        String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
        if (body == null || body.isBlank()) {
            sendJson(exchange, 400, "{\"error\":\"Request body is empty\"}");
            return;
        }

        try {
            StudentRecord student = parseStudent(body);
            insertStudent(student);
            sendJson(exchange, 201, toJson(student));
        } catch (IllegalArgumentException e) {
            sendJson(exchange, 400, "{\"error\":\"" + escapeJson(e.getMessage()) + "\"}");
        } catch (SQLException e) {
            if (e.getMessage() != null && e.getMessage().toUpperCase(Locale.ROOT).contains("PRIMARY KEY")) {
                sendJson(exchange, 409, "{\"error\":\"Roll number already exists\"}");
            } else {
                sendJson(exchange, 500, "{\"error\":\"Could not save student\"}");
            }
        }
    }

    private static List<Map<String, Object>> fetchStudents(String rollNoParam) throws SQLException {
        String sql = """
                SELECT roll_no, name, class_name, tamil, english, maths, science, social_science,
                       tamil + english + maths + science + social_science AS total
                FROM student_results
                WHERE (? IS NULL OR roll_no = ?)
                ORDER BY roll_no
                """;

        List<Map<String, Object>> list = new ArrayList<>();

        try (Connection con = connect(); PreparedStatement ps = con.prepareStatement(sql)) {
            if (rollNoParam == null || rollNoParam.isBlank()) {
                ps.setNull(1, java.sql.Types.INTEGER);
                ps.setNull(2, java.sql.Types.INTEGER);
            } else {
                int rollNo = Integer.parseInt(rollNoParam);
                ps.setInt(1, rollNo);
                ps.setInt(2, rollNo);
            }

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Map<String, Object> student = new HashMap<>();
                    int tamil = rs.getInt("tamil");
                    int english = rs.getInt("english");
                    int maths = rs.getInt("maths");
                    int science = rs.getInt("science");
                    int social = rs.getInt("social_science");
                    int total = tamil + english + maths + science + social;
                    double percentage = total / 5.0;

                    student.put("rollNo", String.valueOf(rs.getInt("roll_no")));
                    student.put("name", rs.getString("name"));
                    student.put("className", rs.getString("class_name"));
                    student.put("tamil", tamil);
                    student.put("english", english);
                    student.put("maths", maths);
                    student.put("science", science);
                    student.put("social", social);
                    student.put("total", total);
                    student.put("percentage", String.format(Locale.US, "%.2f", percentage));
                    student.put("grade", grade(percentage));
                    student.put("status", total >= 0 ? (tamil >= 35 && english >= 35 && maths >= 35 && science >= 35 && social >= 35 ? "Pass" : "Fail") : "Fail");
                    list.add(student);
                }
            }
        }

        return list;
    }

    private static void insertStudent(StudentRecord student) throws SQLException {
        String sql = """
                INSERT INTO student_results
                VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                """;

        try (Connection con = connect(); PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, student.rollNo);
            ps.setString(2, student.name);
            ps.setString(3, student.className);
            ps.setInt(4, student.tamil);
            ps.setInt(5, student.english);
            ps.setInt(6, student.maths);
            ps.setInt(7, student.science);
            ps.setInt(8, student.social);
            ps.executeUpdate();
        }
    }

    private static StudentRecord parseStudent(String json) {
        String rollNoText = extractJsonString(json, "rollNo");
        String name = extractJsonString(json, "name");
        String className = extractJsonString(json, "className");

        if (rollNoText == null || name == null || className == null) {
            throw new IllegalArgumentException("Missing required fields");
        }

        int rollNo = Integer.parseInt(rollNoText);
        int tamil = Integer.parseInt(extractJsonNumber(json, "tamil"));
        int english = Integer.parseInt(extractJsonNumber(json, "english"));
        int maths = Integer.parseInt(extractJsonNumber(json, "maths"));
        int science = Integer.parseInt(extractJsonNumber(json, "science"));
        int social = Integer.parseInt(extractJsonNumber(json, "social"));

        if (tamil < 0 || tamil > 100 || english < 0 || english > 100 || maths < 0 || maths > 100 || science < 0 || science > 100 || social < 0 || social > 100) {
            throw new IllegalArgumentException("Marks must be between 0 and 100");
        }

        return new StudentRecord(rollNo, name, className, tamil, english, maths, science, social);
    }

    private static String extractJsonString(String json, String key) {
        Pattern pattern = Pattern.compile("\\\"" + key + "\\\"\\s*:\\s*\\\"([^\\\"]*)\\\"");
        Matcher matcher = pattern.matcher(json);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return null;
    }

    private static String extractJsonNumber(String json, String key) {
        Pattern pattern = Pattern.compile("\\\"" + key + "\\\"\\s*:\\s*(-?\\d+)");
        Matcher matcher = pattern.matcher(json);
        if (matcher.find()) {
            return matcher.group(1);
        }
        throw new IllegalArgumentException("Missing numeric field: " + key);
    }

    private static String toJson(Map<String, Object> student) {
        StringBuilder sb = new StringBuilder();
        sb.append("{");
        sb.append("\"rollNo\":\"").append(escapeJson(String.valueOf(student.get("rollNo")))).append("\",");
        sb.append("\"name\":\"").append(escapeJson(String.valueOf(student.get("name")))).append("\",");
        sb.append("\"className\":\"").append(escapeJson(String.valueOf(student.get("className")))).append("\",");
        sb.append("\"tamil\":").append(student.get("tamil")).append(",");
        sb.append("\"english\":").append(student.get("english")).append(",");
        sb.append("\"maths\":").append(student.get("maths")).append(",");
        sb.append("\"science\":").append(student.get("science")).append(",");
        sb.append("\"social\":").append(student.get("social")).append(",");
        sb.append("\"total\":").append(student.get("total")).append(",");
        sb.append("\"percentage\":\"").append(student.get("percentage")).append("\",");
        sb.append("\"grade\":\"").append(student.get("grade")).append("\",");
        sb.append("\"status\":\"").append(student.get("status")).append("\"");
        sb.append("}");
        return sb.toString();
    }

    private static String toJson(List<Map<String, Object>> students) {
        StringBuilder sb = new StringBuilder();
        sb.append("[");
        for (int i = 0; i < students.size(); i++) {
            if (i > 0) {
                sb.append(",");
            }
            sb.append(toJson(students.get(i)));
        }
        sb.append("]");
        return sb.toString();
    }

    private static String toJson(StudentRecord student) {
        Map<String, Object> map = new HashMap<>();
        map.put("rollNo", String.valueOf(student.rollNo));
        map.put("name", student.name);
        map.put("className", student.className);
        map.put("tamil", student.tamil);
        map.put("english", student.english);
        map.put("maths", student.maths);
        map.put("science", student.science);
        map.put("social", student.social);
        map.put("total", student.tamil + student.english + student.maths + student.science + student.social);
        double percentage = (student.tamil + student.english + student.maths + student.science + student.social) / 5.0;
        map.put("percentage", String.format(Locale.US, "%.2f", percentage));
        map.put("grade", grade(percentage));
        map.put("status", student.tamil >= 35 && student.english >= 35 && student.maths >= 35 && student.science >= 35 && student.social >= 35 ? "Pass" : "Fail");
        return toJson(map);
    }

    private static String grade(double percentage) {
        if (percentage >= 90) return "A+";
        if (percentage >= 80) return "A";
        if (percentage >= 70) return "B";
        if (percentage >= 60) return "C";
        if (percentage >= 50) return "D";
        return "F";
    }

    private static void sendText(HttpExchange exchange, int statusCode, String text) throws IOException {
        byte[] bytes = text.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "text/plain; charset=UTF-8");
        exchange.sendResponseHeaders(statusCode, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }

    private static void sendJson(HttpExchange exchange, int statusCode, String json) throws IOException {
        byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
        exchange.sendResponseHeaders(statusCode, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }

    private static String escapeJson(String input) {
        if (input == null) {
            return "";
        }
        return input.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }

    private static final class StudentRecord {
        private final int rollNo;
        private final String name;
        private final String className;
        private final int tamil;
        private final int english;
        private final int maths;
        private final int science;
        private final int social;

        private StudentRecord(int rollNo, String name, String className, int tamil, int english, int maths, int science, int social) {
            this.rollNo = rollNo;
            this.name = name;
            this.className = className;
            this.tamil = tamil;
            this.english = english;
            this.maths = maths;
            this.science = science;
            this.social = social;
        }
    }
}
