'use client';

import React, { useEffect, useRef } from 'react';
import { router, Stack } from 'expo-router';
import { QueryClientProvider } from '@tanstack/react-query';
import { MD3LightTheme, PaperProvider } from 'react-native-paper';

import NetworkBanner from '@/components/NetworkBanner';
import { BRAND_COLORS } from '@/constants/colors';
import { AuthProvider, useAuth } from '@/lib/auth-context';
import { queryClient } from '@/lib/queryClient';

const theme = {
  ...MD3LightTheme,
  colors: {
    ...MD3LightTheme.colors,
    primary: BRAND_COLORS.primary,
    secondary: BRAND_COLORS.accent,
  },
};

function NavigationGuard({ children }: { children: React.ReactNode }) {
  const { isAuthenticated, loading } = useAuth();
  const hasNavigated = useRef(false);

  useEffect(() => {
    if (loading) return;

    if (!isAuthenticated && !hasNavigated.current) {
      hasNavigated.current = true;
      router.replace('/(auth)/login');
    } else if (isAuthenticated) {
      hasNavigated.current = false;
    }
  }, [isAuthenticated, loading]);

  if (loading) return null;

  return <>{children}</>;
}

export default function RootLayout() {
  return (
    <QueryClientProvider client={queryClient}>
      <PaperProvider theme={theme}>
        <AuthProvider>
          <NavigationGuard>
            <NetworkBanner />
            <Stack screenOptions={{ headerTitleAlign: 'center' }}>
              <Stack.Screen name="(tabs)" options={{ title: 'Escala de Voluntários' }} />
              <Stack.Screen name="(auth)" options={{ headerShown: false }} />
            </Stack>
          </NavigationGuard>
        </AuthProvider>
      </PaperProvider>
    </QueryClientProvider>
  );
}
