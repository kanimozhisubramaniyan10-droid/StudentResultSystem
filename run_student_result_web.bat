@echo off
cd /d "%~dp0"
echo Starting Student Result Web Server...

mvn clean compile
mvn exec:java -Dexec.mainClass=com.studentresult.StudentResultWebServer
