import { validate } from './schema.validator';
import { samples } from './test-samples';

describe('test schema validator', () => {
  it('schema examples', () => {
    samples.forEach((sample) => {
      expect(validate(sample.sample)).toEqual(sample.isValid);
    });
  });
});
