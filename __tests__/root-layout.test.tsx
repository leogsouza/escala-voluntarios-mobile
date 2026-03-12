import React from 'react';
import { render } from '@testing-library/react-native';

// Mock native modules before any imports that depend on them
jest.mock('expo-secure-store', () => ({
  getItemAsync: jest.fn().mockResolvedValue(null),
  setItemAsync: jest.fn().mockResolvedValue(undefined),
  deleteItemAsync: jest.fn().mockResolvedValue(undefined),
}));

jest.mock('expo-local-authentication', () => ({
  hasHardwareAsync: jest.fn().mockResolvedValue(false),
  isEnrolledAsync: jest.fn().mockResolvedValue(false),
  authenticateAsync: jest.fn().mockResolvedValue({ success: false }),
}));

jest.mock('@react-native-community/netinfo', () => ({
  addEventListener: jest.fn(() => jest.fn()),
  fetch: jest.fn().mockResolvedValue({ isConnected: true, isInternetReachable: true }),
}));

jest.mock('expo-router', () => ({
  router: { replace: jest.fn(), push: jest.fn() },
  Stack: Object.assign(
    ({ children }: { children: React.ReactNode }) => <>{children}</>,
    {
      Screen: () => null,
    }
  ),
  usePathname: jest.fn().mockReturnValue('/'),
  useSegments: jest.fn().mockReturnValue([]),
}));

import RootLayout from '@/app/_layout';

describe('RootLayout', () => {
  it('renders without crashing', () => {
    const { toJSON } = render(<RootLayout />);
    expect(toJSON()).toBeTruthy();
  });

  it('renders providers hierarchy', () => {
    // Verifies QueryClientProvider, PaperProvider, AuthProvider all mount
    expect(() => render(<RootLayout />)).not.toThrow();
  });
});
