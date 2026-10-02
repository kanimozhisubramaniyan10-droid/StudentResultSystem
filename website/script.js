// Student data
let students = [];

async function loadStudents() {
    try {
        const response = await fetch("/api/students");
        if (!response.ok) {
            throw new Error("Unable to load students");
        }

        students = await response.json();
        displayStudents();
    } catch (error) {
        alert("Could not load student records from the database.");
        console.error(error);
    }
}

// Change section
function showSection(sectionId) {
    const sections = document.querySelectorAll(".section");

    sections.forEach(section => {
        section.classList.remove("active");
    });

    document.getElementById(sectionId).classList.add("active");

    if (sectionId === "view") {
        loadStudents();
    }
}

// Calculate grade
function calculateGrade(percentage) {

    if (percentage >= 90) {
        return "A+";
    } else if (percentage >= 80) {
        return "A";
    } else if (percentage >= 70) {
        return "B";
    } else if (percentage >= 60) {
        return "C";
    } else if (percentage >= 50) {
        return "D";
    } else {
        return "F";
    }
}

// Add student
document.getElementById("studentForm").addEventListener("submit", async function(event) {

    event.preventDefault();

    const rollNo = document.getElementById("rollNo").value.trim();
    const name = document.getElementById("studentName").value.trim();
    const className = document.getElementById("className").value.trim();

    const tamil = Number(document.getElementById("tamil").value);
    const english = Number(document.getElementById("english").value);
    const maths = Number(document.getElementById("maths").value);
    const science = Number(document.getElementById("science").value);
    const social = Number(document.getElementById("social").value);

    const student = {
        rollNo: rollNo,
        name: name,
        className: className,
        tamil: tamil,
        english: english,
        maths: maths,
        science: science,
        social: social
    };

    try {
        const response = await fetch("/api/students", {
            method: "POST",
            headers: {
                "Content-Type": "application/json"
            },
            body: JSON.stringify(student)
        });

        const result = await response.json();

        if (!response.ok) {
            throw new Error(result.error || "Failed to save student result");
        }

        alert("Student result saved successfully!");
        document.getElementById("studentForm").reset();
        await loadStudents();
        showSection("view");
    } catch (error) {
        alert(error.message);
        console.error(error);
    }
});

// Display all students
function displayStudents() {

    const table = document.getElementById("resultTable");

    table.innerHTML = "";

    if (students.length === 0) {

        table.innerHTML = `
            <tr>
                <td colspan="7">No student results available</td>
            </tr>
        `;

        return;
    }

    students.forEach(student => {

        const row = document.createElement("tr");

        row.innerHTML = `
            <td>${student.rollNo}</td>
            <td>${student.name}</td>
            <td>${student.className}</td>
            <td>${student.total}</td>
            <td>${student.percentage}%</td>
            <td>${student.grade}</td>
            <td class="${student.status === "Pass" ? "success" : "fail"}">
                ${student.status}
            </td>
        `;

        table.appendChild(row);
    });
}

// Search student
async function searchStudent() {

    const rollNo = document.getElementById("searchRoll").value.trim();
    const resultDiv = document.getElementById("searchResult");

    if (!rollNo) {
        resultDiv.innerHTML = `
            <div class="search-card">
                <p class="fail">Please enter a roll number.</p>
            </div>
        `;
        return;
    }

    try {
        const response = await fetch(`/api/students?rollNo=${encodeURIComponent(rollNo)}`);

        if (!response.ok) {
            throw new Error("Student not found");
        }

        const student = (await response.json())[0];

        if (!student) {
            resultDiv.innerHTML = `
                <div class="search-card">
                    <p class="fail">Student not found!</p>
                </div>
            `;
            return;
        }

        resultDiv.innerHTML = `
            <div class="search-card">

                <h3>Student Details</h3>

                <br>

                <p><strong>Roll Number:</strong> ${student.rollNo}</p>

                <p><strong>Name:</strong> ${student.name}</p>

                <p><strong>Class:</strong> ${student.className}</p>

                <p><strong>Total Marks:</strong> ${student.total}</p>

                <p><strong>Percentage:</strong> ${student.percentage}%</p>

                <p><strong>Grade:</strong> ${student.grade}</p>

                <p>
                    <strong>Status:</strong>
                    <span class="${student.status === "Pass" ? "success" : "fail"}">
                        ${student.status}
                    </span>
                </p>

            </div>
        `;
    } catch (error) {
        resultDiv.innerHTML = `
            <div class="search-card">
                <p class="fail">Student not found!</p>
            </div>
        `;
    }
}

// Show home when page loads
showSection("home");