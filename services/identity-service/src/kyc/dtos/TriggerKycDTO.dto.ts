import { ApiProperty } from "@nestjs/swagger";

export class TriggerKycDTO {
    @ApiProperty({description: 'The aadhaar of the user.'})
    aadhaar: string;
}