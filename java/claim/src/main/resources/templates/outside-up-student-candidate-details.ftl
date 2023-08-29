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

            <#if candidate.title?has_content>
              <td>${candidate.title}</td>
            <#else>
              <td> </td>
            </#if>

        </tr>
        <tr>
            <td>Name</td>

            <#if candidate.name?has_content>
              <td>${candidate.name}</td>
            <#else>
              <td> </td>
            </#if>
        </tr>
        <tr>
            <td>Aadhaar No</td>

            <#if candidate.aadhaarNo?has_content>
              <td>${candidate.aadhaarNo}</td>
            <#else>
              <td> </td>
            </#if>
        </tr>
        <tr>
            <td>Bar Code</td>

            <#if candidate.barCode?has_content>
              <td>${candidate.barCode}</td>
            <#else>
              <td> </td>
            </#if>
        </tr>
        <tr>
            <td>Council</td>

            <#if candidate.council?has_content>
              <td>${candidate.council}</td>
            <#else>
              <td> </td>
            </#if>
        </tr>
        <tr>
            <td>Course Name</td>

            <#if candidate.courseName?has_content>
              <td>${candidate.courseName}</td>
            <#else>
              <td> </td>
            </#if>
        </tr>
        <tr>
            <td>Course State</td>

            <#if candidate.courseState?has_content>
              <td>${candidate.courseState}</td>
            <#else>
              <td> </td>
            </#if>
        </tr>
        <tr>
            <td>Date</td>

            <#if candidate.date?has_content>
              <td>${candidate.date}</td>
            <#else>
              <td> </td>
            </#if>
        </tr>
        <tr>
            <td>Date of birth</td>

            <#if candidate.dateOfBirth?has_content>
              <td>${candidate.dateOfBirth}</td>
            <#else>
              <td> </td>
            </#if>
        </tr>
        <tr>
            <td>Email</td>

            <#if candidate.email?has_content>
              <td>${candidate.email}</td>
            <#else>
              <td> </td>
            </#if>
        </tr>
        <tr>
            <td>Exam Body</td>

            <#if candidate.examBody?has_content>
              <td>${candidate.examBody}</td>
            <#else>
              <td> </td>
            </#if>
        </tr>
        <tr>
            <td>Fathers Name</td>

            <#if candidate.fathersName?has_content>
              <td>${candidate.fathersName}</td>
            <#else>
              <td> </td>
            </#if>
        </tr>
        <tr>
            <td>Ree Receipt No</td>

            <#if candidate.feeReciptNo?has_content>
              <td>${candidate.feeReciptNo}</td>
            <#else>
              <td> </td>
            </#if>
        </tr>
        <tr>
            <td>Final Year RollNo</td>

            <#if candidate.finalYearRollNo?has_content>
              <td>${candidate.finalYearRollNo}</td>
            <#else>
              <td> </td>
            </#if>
        </tr>
        <tr>
            <td>gender</td>

            <#if candidate.gender?has_content>
              <td>${candidate.gender}</td>
            <#else>
              <td> </td>
            </#if>
        </tr>
        <tr>
            <td>Joining Month</td>

            <#if candidate.joiningMonth?has_content>
              <td>${candidate.joiningMonth}</td>
            <#else>
              <td> </td>
            </#if>
        </tr>
        <tr>
            <td>Joining Year</td>

            <#if candidate.joiningYear?has_content>
              <td>${candidate.joiningYear}</td>
            <#else>
              <td> </td>
            </#if>
        </tr>
        <tr>
            <td>Mothers Name</td>

            <#if candidate.mothersName?has_content>
              <td>${candidate.mothersName}</td>
            <#else>
              <td> </td>
            </#if>
        </tr>
        <tr>
            <td>Nurse Registration No</td>

            <#if candidate.nurseRegNo?has_content>
              <td>${candidate.nurseRegNo}</td>
            <#else>
              <td> </td>
            </#if>
        </tr>
        <tr>
            <td>Nursing Collage</td>

            <#if candidate.nursingCollage?has_content>
              <td>${candidate.nursingCollage}</td>
            <#else>
              <td> </td>
            </#if>
        </tr>
        <tr>
            <td>Passing Month</td>

            <#if candidate.passingMonth?has_content>
              <td>${candidate.passingMonth}</td>
            <#else>
              <td> </td>
            </#if>
        </tr>
        <tr>
            <td>Passing Year</td>

            <#if candidate.passingYear?has_content>
              <td>${candidate.passingYear}</td>
            <#else>
              <td> </td>
            </#if>
        </tr>
        <tr>
            <td>Phone Number</td>

            <#if candidate.phoneNumber?has_content>
              <td>${candidate.phoneNumber}</td>
            <#else>
              <td> </td>
            </#if>
        </tr>
        <tr>
            <td>Registration No</td>

            <#if candidate.registrationNo?has_content>
              <td>${candidate.registrationNo}</td>
            <#else>
              <td> </td>
            </#if>
        </tr>
        <tr>
            <td>Registration Type</td>

            <#if candidate.registrationType?has_content>
              <td>${candidate.registrationType}</td>
            <#else>
              <td> </td>
            </#if>
        </tr>
</table>
<div style="width:300px;background:#eee;height:30px;">
    <table id="cand" class="tb" style="width:300px;">
        <tr>
        <td>
            <input type=button style="height: 40px; width: 90px;"
            onClick="location.href='http://localhost:8082/api/v1/outsideStudent/verify/${entityId}/Completed'"
             value='Completed'>
        </td>
        </tr>
    </table>
</div>
</body>
</html>
