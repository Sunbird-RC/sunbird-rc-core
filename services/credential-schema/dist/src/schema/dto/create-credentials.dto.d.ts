import { Status } from '@prisma/client';
import { VCModelSchema } from '../entities/VCModelSchema.entity';
export declare class CreateCredentialDTO {
    schema: VCModelSchema["schema"];
    tags: string[];
    status: Status;
}
