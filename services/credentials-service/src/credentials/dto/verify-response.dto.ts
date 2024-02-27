export class VerifyCredentialResponse {
  status?: string; // Confirm is this is going to be an enum
  checks?: [
    { active?: string; revoke?: string; expired?: string; proof?: string },
  ];
  warnings?: string[];
  errors?: string[];
}
