export declare class VerifyCredentialResponse {
    status?: string;
    checks?: [
        {
            active?: string;
            revoke?: string;
            expired?: string;
            proof?: string;
        }
    ];
    warnings?: string[];
    errors?: string[];
}
