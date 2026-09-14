# Student Result System — JDBC

This Java console project stores and reads student marks from a SQLite database through JDBC. The database file `student_results.db` is created automatically when the program starts.

## Requirements

- Java 21+
- Maven 3.9+ (recommended)

## Run with Maven

```powershell
cd jdbc-student-result-system
mvn compile
mvn exec:java -Dexec.mainClass="com.studentresult.StudentResultSystem"
```

The menu lets you add a result, view all database records, or find a record using its roll number.

## Run without Maven

The required JDBC driver files are available in the local `lib` folder. Run:

```powershell
javac -cp "lib/*" -d out src/main/java/com/studentresult/StudentResultSystem.java
java -cp "out;lib/*" com.studentresult.StudentResultSystem
```

## Database view

Open `student_results.db` in DB Browser for SQLite, then run:

```sql
SELECT roll_no, name, class_name, tamil, english, maths, science, social_science,
       tamil + english + maths + science + social_science AS total
FROM student_results
ORDER BY roll_no;
```

## MySQL alternative

For MySQL, replace the SQLite JDBC dependency with `com.mysql:mysql-connector-j`, change `DB_URL` to e.g. `jdbc:mysql://localhost:3306/student_result_db`, and run the equivalent `CREATE TABLE` statement in MySQL.
