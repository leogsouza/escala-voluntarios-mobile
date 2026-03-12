import React from 'react';
import { Text, View } from 'react-native';
import { Stack } from 'expo-router';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { PaperProvider, MD3LightTheme } from 'react-native-paper';

import { BRAND_COLORS } from '@/constants/colors';
import { AuthProvider } from '@/lib/auth-context';

const queryClient = new QueryClient();

const theme = {
  ...MD3LightTheme,
  colors: {
    ...MD3LightTheme.colors,
    primary: BRAND_COLORS.primary,
    secondary: BRAND_COLORS.accent
  }
};

export default function RootLayout() {
  return (
    <QueryClientProvider client={queryClient}>
      <PaperProvider theme={theme}>
        <AuthProvider>
          <View style={{ display: 'none' }}>
            <Text>Escala de Voluntários Mobile</Text>
          </View>
          <Stack screenOptions={{ headerTitleAlign: 'center' }}>
            <Stack.Screen name="(tabs)" options={{ title: 'Escala de Voluntários' }} />
            <Stack.Screen name="(auth)" options={{ headerShown: false }} />
          </Stack>
        </AuthProvider>
      </PaperProvider>
    </QueryClientProvider>
  );
}
