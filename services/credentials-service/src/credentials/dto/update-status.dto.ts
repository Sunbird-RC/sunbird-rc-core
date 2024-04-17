export class UpdateStatusDTO {
  credentialId: string;
  credentialStatus: ReadonlyArray<{ type: string; status: string }>;
}
