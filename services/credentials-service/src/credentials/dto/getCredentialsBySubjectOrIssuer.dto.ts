export class GetCredentialsBySubjectOrIssuer {
  subject?: {
    id: string;
  }; //JSON
  issuer?: {
    id: string;
  }; //DID
  // subjectId?: string; //DID of student
  type?: string;
}
