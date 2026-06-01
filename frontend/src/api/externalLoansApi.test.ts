import { beforeEach, describe, expect, it, vi, type Mock } from 'vitest';
import { http } from './http';
import {
  applyExternalLoanBackfill,
  dryRunExternalLoans,
  getExternalLoanIdempotencyDiagnostics,
} from './externalLoansApi';

vi.mock('./http', () => ({
  http: {
    get: vi.fn(),
    post: vi.fn(),
    put: vi.fn(),
  },
}));

const mockedHttp = http as unknown as {
  get: Mock;
  post: Mock;
};

describe('externalLoansApi', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    mockedHttp.get.mockResolvedValue({ data: {} });
    mockedHttp.post.mockResolvedValue({ data: {} });
  });

  it('calls the sync dry-run endpoint', async () => {
    await dryRunExternalLoans('profile-1');

    expect(mockedHttp.post).toHaveBeenCalledWith(
      '/profiles/profile-1/external-loans/sync/dry-run',
    );
  });

  it('calls the diagnostics endpoint', async () => {
    await getExternalLoanIdempotencyDiagnostics('profile-1');

    expect(mockedHttp.get).toHaveBeenCalledWith(
      '/profiles/profile-1/external-loans/idempotency/diagnostics',
    );
  });

  it('sends includeLowConfidence=false for safe backfill apply', async () => {
    await applyExternalLoanBackfill('profile-1', { includeLowConfidence: false });

    expect(mockedHttp.post).toHaveBeenCalledWith(
      '/profiles/profile-1/external-loans/backfill/apply',
      { includeLowConfidence: false },
    );
  });
});
