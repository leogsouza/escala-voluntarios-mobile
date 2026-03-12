'use client';

import React from 'react';
import { StyleSheet, View } from 'react-native';
import { Button, Icon, Text } from 'react-native-paper';

type Props = {
  message: string;
  onRetry?: () => void;
};

export default function ErrorScreen({ message, onRetry }: Props) {
  return (
    <View style={styles.container}>
      <Icon source="alert-circle-outline" size={56} color="#DC2626" />
      <Text variant="titleMedium" style={styles.title}>
        Algo deu errado
      </Text>
      <Text variant="bodyMedium" style={styles.message}>
        {message}
      </Text>

      {onRetry ? (
        <Button mode="contained" onPress={onRetry} style={styles.button}>
          Tentar novamente
        </Button>
      ) : null}
    </View>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    alignItems: 'center',
    justifyContent: 'center',
    padding: 24,
    backgroundColor: '#f8fafc',
    gap: 12,
  },
  title: {
    fontWeight: '700',
    color: '#111827',
  },
  message: {
    color: '#374151',
    textAlign: 'center',
  },
  button: {
    marginTop: 8,
  },
});
