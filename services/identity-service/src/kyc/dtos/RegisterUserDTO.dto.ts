import { ApiProperty } from "@nestjs/swagger";

export class RegisterUserDTO {
    @ApiProperty({ description: 'The aadhaar of the user.' })
    aadhaar: string;

    @ApiProperty({ description: 'Verification OTP shared with user.' })
    otp: number;

    @ApiProperty({ description: 'Username to register with.' })
    username: string;

    @ApiProperty({ description: 'New password to register with.' })
    password: string;
}