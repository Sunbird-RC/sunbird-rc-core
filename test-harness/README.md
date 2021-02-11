## Philosophy of naming
Every action inside a thread group conforms to this naming convention
    `Action_[out_variable]`
For example, 
    `AddTeacher_[osid]`
would indicate that whenever AddTeacher action is finished, in success cases, the variable osid must be populated.


## Test script details

### TeacherRecordTests.jmx

#### Steps
1. Add - Teacher
2. If response is failure due to "Authentication token is invalid", then
2.1 Authenticate and fetch the token
2.2 Re-attempt - Add Teacher (step 1)
3. Read the just added Teacher record
4. Update
5. Read after update and ensure the right value is reflected.
