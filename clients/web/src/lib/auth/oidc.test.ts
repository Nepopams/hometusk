import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';

const oidcMock = vi.hoisted(() => ({
  signinRedirect: vi.fn(),
  userManager: vi.fn(),
  webStorageStateStore: vi.fn(),
}));

vi.mock('oidc-client-ts', () => ({
  UserManager: oidcMock.userManager,
  WebStorageStateStore: oidcMock.webStorageStateStore,
}));

describe('signinWithYandex', () => {
  beforeEach(() => {
    vi.resetModules();
    vi.stubEnv('VITE_OIDC_AUTHORITY', 'http://localhost:8180/realms/hometusk');
    vi.stubEnv('VITE_OIDC_CLIENT_ID', 'hometusk-web');
    vi.stubEnv('VITE_OIDC_REDIRECT_URI', 'http://localhost:5173/callback');
    vi.stubGlobal('window', {
      location: { origin: 'http://localhost:5173' },
      sessionStorage: {},
    });

    oidcMock.signinRedirect.mockReset();
    oidcMock.userManager.mockReset();
    oidcMock.webStorageStateStore.mockReset();
    oidcMock.userManager.mockImplementation(() => ({
      signinRedirect: oidcMock.signinRedirect,
    }));
  });

  afterEach(() => {
    vi.unstubAllEnvs();
    vi.unstubAllGlobals();
  });

  it('passes Keycloak identity provider hint for Yandex broker login', async () => {
    const { signinWithYandex } = await import('./oidc');

    await signinWithYandex();

    expect(oidcMock.userManager).toHaveBeenCalledWith(
      expect.objectContaining({
        authority: 'http://localhost:8180/realms/hometusk',
        client_id: 'hometusk-web',
        redirect_uri: 'http://localhost:5173/callback',
        response_type: 'code',
        scope: 'openid profile email',
      })
    );
    expect(oidcMock.signinRedirect).toHaveBeenCalledWith({
      extraQueryParams: {
        kc_idp_hint: 'yandex',
      },
    });
  });
});
