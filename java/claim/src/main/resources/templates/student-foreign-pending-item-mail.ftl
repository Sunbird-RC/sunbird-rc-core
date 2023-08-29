<html>
<head>
<style>
    table, th, td {
      border: 1px solid black;
      border-collapse: collapse;
    }

    th, td {
      padding: 10px;
    }
</style>
</head>

<body>
    <p><H3>Hi ${regulatorName}</H3></p>

    <p>Pending action item for student foreign verification. Following candidate has applied for certificate.</p>

    <p> <div> Candidate list: </div> </p>
    <p>
        <table>
          <tr>
            <th>Name</th>
            <th>Cred Type</th>
            <th>Reference Number</th>
            <th>Registration Number</th>
            <th>Email</th>
            <th>Verify Link</th>
          </tr>
          <#list candidates as candidate >
              <tr>
                <td>${candidate.name} </td>
                <td>${candidate.credType}</td>
                <td>${candidate.refNo}</td>
                <td>${candidate.registrationNumber}</td>
                <td>${candidate.emailAddress}</td>
                <td><a href = "${candidate.verifyLink}">Verify Candidate</a></td>
              </tr>
          </#list>
        </table>
    </p>

    <div>Please follow up to respected entity</div>

    <p>Thank you,</p>
    <p>&#60; Registration Credential Issuing Authority &#62;</p>
</body>
</html>
