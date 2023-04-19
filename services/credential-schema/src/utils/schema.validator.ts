import Ajv2019, { DefinedError } from 'ajv/dist/2019';
const ajv = new Ajv2019();
// link to schema:  https://w3c-ccg.github.io/vc-json-schemas/schema/2.0/schema.json
import * as schema from '../../schema.json';

export const validate = ajv.compile(schema);
