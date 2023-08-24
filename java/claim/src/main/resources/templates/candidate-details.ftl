<html>
<head>
<style>
    table {
      border-collapse: collapse;
    }

    th, td {
      text-align: left;
      padding: 8px;
    }

    tr:nth-child(odd) {
      background-color: #D6EEEE;
    }

    #cand {
      font-family: Arial, Helvetica, sans-serif;
      border-collapse: collapse;
    }

</style>
</head>

<body>
<table>
    <caption><h2>Candidate Details</h2></caption>
    <tr>
    	<td>Title</td>
    	<td>${candidate.title}</td>
    </tr>
    <tr>
    	<td>Student Name</td>
    	<td>${candidate.name}</td>
    </tr>
    <tr>
    	<td>Registration Type</td>
    	<td>${candidate.registrationType}</td>
    </tr>
    <tr>
    	<td>Registration Number</td>
    	<td>${candidate.registrationNumber}</td>
    </tr>
    <tr>
    	<td>Reference No</td>
    	<td>${candidate.refNo}</td>
    </tr>
    <tr>
    	<td>Phone Number</td>
    	<td>${candidate.phoneNumber}</td>
    </tr>
    <tr>
    	<td>Passing Year</td>
    	<td>${candidate.passingYear}</td>
    </tr>
    <tr>
    	<td>Passing Month</td>
    	<td>${candidate.passingMonth}</td>
    </tr>
    <tr>
    	<td>Nursing Collage</td>
    	<td>${candidate.nursingCollage}</td>
    </tr>
    <tr>
    	<td>Mothers Name</td>
    	<td>${candidate.mothersName}</td>
    </tr>
    <tr>
    	<td>Joining Year</td>
    	<td>${candidate.joiningYear}</td>
    </tr>
    <tr>
    	<td>Joining Month</td>
    	<td>${candidate.joiningMonth}</td>
    </tr>
    <tr>
    	<td>Gender</td>
    	<td>${candidate.gender}</td>
    </tr>
    <tr>
    	<td>Final Year Roll No</td>
    	<td>${candidate.finalYearRollNo}</td>
    </tr>
    <tr>
    	<td>Fathers Name</td>
    	<td>${candidate.fathersName}</td>
    </tr>
    <tr>
    	<td>Exam Body</td>
    	<td>${candidate.examBody}</td>
    </tr>
    <tr>
    	<td>Email</td>
    	<td>${candidate.email}</td>
    </tr>
    <tr>
    	<td>Date Of Birth</td>
    	<td>${candidate.dateOfBirth}</td>
    </tr>
    <tr>
    	<td>Date</td>
    	<td>${candidate.date}</td>
    </tr>
    <tr>
    	<td>Course Name</td>
    	<td>${candidate.courseName}</td>
    </tr>
    <tr>
    	<td>Council</td>
    	<td>${candidate.council}</td>
    </tr>
    <tr>
    	<td>CandidatePic</td>
    	<td>${candidate.candidatePic}</td>
    </tr>
    <tr>
    	<td>Bar Code</td>
    	<td>${candidate.barCode}</td>
    </tr>
    <tr>
    	<td>Aadhaar No</td>
        <td>${candidate.aadhaarNo}</td>
    </tr>
</table>
<div style="width:300px;background:#eee;height:30px;">
    <table id="cand" class="tb" style="width:300px;">
        <tr>
        <td>
            <input type=button style="height: 40px; width: 70px;"
            onClick="location.href='http://localhost:8082/api/v1/outside/foreignStudent/verify/${entityId}/Approved'"
             value='Approve'>
        </td>
        <td>
            <input type=button style="height: 40px; width: 70px;"
            onClick="location.href='http://localhost:8082/api/v1/outside/foreignStudent/verify/${entityId}/Rejected'"
             value='Reject'>
        </td>
        </tr>
    </table>
</div>
</body>
</html>
