import { useCallback, useEffect, useRef, useState } from 'react';
import { AppState, AppStateStatus } from 'react-native';
import * as LocalAuthentication from 'expo-local-authentication';

type BiometricLockState = {
  biometricAuthenticated: boolean;
  showFallback: boolean;
  retry: () => void;
};

export function useBiometricLock(): BiometricLockState {
  const [biometricAuthenticated, setBiometricAuthenticated] = useState(false);
  const [showFallback, setShowFallback] = useState(false);
  const isPrompting = useRef(false);

  const authenticate = useCallback(async () => {
    if (isPrompting.current) return;

    try {
      const hasHardware = await LocalAuthentication.hasHardwareAsync();
      const isEnrolled = await LocalAuthentication.isEnrolledAsync();

      if (!hasHardware || !isEnrolled) {
        // No biometric available — skip, treat as authenticated
        setBiometricAuthenticated(true);
        return;
      }

      isPrompting.current = true;
      const result = await LocalAuthentication.authenticateAsync({
        promptMessage: 'Desbloqueie com sua digital',
        fallbackLabel: 'Usar senha',
        cancelLabel: 'Cancelar',
      });
      isPrompting.current = false;

      if (result.success) {
        setBiometricAuthenticated(true);
        setShowFallback(false);
      } else {
        setBiometricAuthenticated(false);
        setShowFallback(true);
      }
    } catch {
      isPrompting.current = false;
      setBiometricAuthenticated(false);
      setShowFallback(true);
    }
  }, []);

  // Trigger on mount
  useEffect(() => {
    authenticate();
  }, [authenticate]);

  // Re-trigger on every app foreground
  useEffect(() => {
    function handleAppStateChange(nextState: AppStateStatus) {
      if (nextState === 'active') {
        setBiometricAuthenticated(false);
        setShowFallback(false);
        authenticate();
      }
    }

    const subscription = AppState.addEventListener('change', handleAppStateChange);
    return () => subscription.remove();
  }, [authenticate]);

  const retry = useCallback(() => {
    setShowFallback(false);
    authenticate();
  }, [authenticate]);

  return { biometricAuthenticated, showFallback, retry };
}
