import { renderHook, act, waitFor } from '@testing-library/react-native';
import { AppState, AppStateStatus } from 'react-native';

jest.mock('expo-local-authentication', () => ({
  hasHardwareAsync: jest.fn(),
  isEnrolledAsync: jest.fn(),
  authenticateAsync: jest.fn(),
}));

import * as LocalAuthentication from 'expo-local-authentication';
import { useBiometricLock } from '@/hooks/useBiometricLock';

const mockedLocalAuth = LocalAuthentication as jest.Mocked<typeof LocalAuthentication>;

describe('useBiometricLock', () => {
  let appStateListener: ((state: AppStateStatus) => void) | null = null;

  beforeEach(() => {
    jest.clearAllMocks();
    appStateListener = null;

    // Capture the AppState listener
    jest.spyOn(AppState, 'addEventListener').mockImplementation(
      (_event: string, callback: (state: AppStateStatus) => void) => {
        appStateListener = callback;
        return { remove: jest.fn() } as any;
      },
    );
  });

  afterEach(() => {
    jest.restoreAllMocks();
  });

  it('sets biometricAuthenticated=true when biometric succeeds', async () => {
    mockedLocalAuth.hasHardwareAsync.mockResolvedValue(true);
    mockedLocalAuth.isEnrolledAsync.mockResolvedValue(true);
    mockedLocalAuth.authenticateAsync.mockResolvedValue({ success: true } as any);

    const { result } = renderHook(() => useBiometricLock());
    await waitFor(() => expect(result.current.biometricAuthenticated).toBe(true));

    expect(result.current.showFallback).toBe(false);
  });

  it('sets showFallback=true when biometric fails', async () => {
    mockedLocalAuth.hasHardwareAsync.mockResolvedValue(true);
    mockedLocalAuth.isEnrolledAsync.mockResolvedValue(true);
    mockedLocalAuth.authenticateAsync.mockResolvedValue({
      success: false,
      error: 'user_cancel',
    } as any);

    const { result } = renderHook(() => useBiometricLock());
    await waitFor(() => expect(result.current.showFallback).toBe(true));

    expect(result.current.biometricAuthenticated).toBe(false);
  });

  it('skips biometric when hardware not available', async () => {
    mockedLocalAuth.hasHardwareAsync.mockResolvedValue(false);
    mockedLocalAuth.isEnrolledAsync.mockResolvedValue(false);

    const { result } = renderHook(() => useBiometricLock());
    await waitFor(() => expect(result.current.biometricAuthenticated).toBe(true));

    expect(mockedLocalAuth.authenticateAsync).not.toHaveBeenCalled();
  });

  it('skips biometric when not enrolled', async () => {
    mockedLocalAuth.hasHardwareAsync.mockResolvedValue(true);
    mockedLocalAuth.isEnrolledAsync.mockResolvedValue(false);

    const { result } = renderHook(() => useBiometricLock());
    await waitFor(() => expect(result.current.biometricAuthenticated).toBe(true));

    expect(mockedLocalAuth.authenticateAsync).not.toHaveBeenCalled();
  });

  it('triggers biometric again on app foreground', async () => {
    mockedLocalAuth.hasHardwareAsync.mockResolvedValue(true);
    mockedLocalAuth.isEnrolledAsync.mockResolvedValue(true);
    mockedLocalAuth.authenticateAsync.mockResolvedValue({ success: true } as any);

    const { result } = renderHook(() => useBiometricLock());
    await waitFor(() => expect(result.current.biometricAuthenticated).toBe(true));

    const callCount = mockedLocalAuth.authenticateAsync.mock.calls.length;

    await act(async () => {
      appStateListener?.('active');
    });

    await waitFor(() =>
      expect(mockedLocalAuth.authenticateAsync.mock.calls.length).toBeGreaterThan(callCount),
    );
  });

  it('retry function re-triggers biometric prompt', async () => {
    mockedLocalAuth.hasHardwareAsync.mockResolvedValue(true);
    mockedLocalAuth.isEnrolledAsync.mockResolvedValue(true);
    mockedLocalAuth.authenticateAsync
      .mockResolvedValueOnce({ success: false, error: 'user_cancel' } as any)
      .mockResolvedValueOnce({ success: true } as any);

    const { result } = renderHook(() => useBiometricLock());
    await waitFor(() => expect(result.current.showFallback).toBe(true));

    await act(async () => {
      result.current.retry();
    });

    await waitFor(() => expect(result.current.biometricAuthenticated).toBe(true));
    expect(result.current.showFallback).toBe(false);
  });
});
