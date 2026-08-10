import { SetMetadata } from '@nestjs/common';

// Marks a route as public so the global AuthGuard skips JWT validation.
// The guard already reads the 'isPublic' metadata key (see auth.guard.ts).
export const Public = () => SetMetadata('isPublic', true);
