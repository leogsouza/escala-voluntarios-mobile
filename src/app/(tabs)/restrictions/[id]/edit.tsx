import React, { useEffect } from 'react';
import { View, StyleSheet, ActivityIndicator } from 'react-native';
import { useLocalSearchParams, useRouter, Stack } from 'expo-router';
import { useQueryClient } from '@tanstack/react-query';
import { Text } from 'react-native-paper';

import RestrictionForm from '@/components/restrictions/RestrictionForm';
import { useRestriction, useUpdateRestriction } from '@/hooks/queries/useRestrictions';
import { Restriction } from '@/types/restriction';
import { queryKeys } from '@/lib/queryKeys';

export default function EditRestrictionScreen() {
  const router = useRouter();
  const queryClient = useQueryClient();
  const { id } = useLocalSearchParams<{ id: string }>();
  const restrictionId = id ? parseInt(id, 10) : 0;

  const {
    data: restriction,
    isLoading,
    error: fetchError,
  } = useRestriction(restrictionId);

  const updateRestrictionMutation = useUpdateRestriction();

  const handleSubmit = (data: Restriction) => {
    if (!restrictionId) return;

    updateRestrictionMutation.mutate(
      { id: restrictionId, data },
      {
        onSuccess: () => {
          // Invalidate queries to refresh the list and details
          queryClient.invalidateQueries({ queryKey: queryKeys.restrictions.all() });
          queryClient.invalidateQueries({ queryKey: queryKeys.restrictions.detail(restrictionId) });
          router.back();
        },
        onError: (error) => {
          console.error('Failed to update restriction', error);
        },
      }
    );
  };

  if (isLoading) {
    return (
      <View style={styles.centerContainer}>
        <ActivityIndicator size="large" />
      </View>
    );
  }

  if (fetchError || !restriction) {
    return (
      <View style={styles.centerContainer}>
        <Text>Erro ao carregar restrição.</Text>
      </View>
    );
  }

  return (
    <View style={styles.container}>
      <Stack.Screen options={{ title: 'Editar Restrição' }} />
      <RestrictionForm
        initialData={restriction}
        onSubmit={handleSubmit}
        isSubmitting={updateRestrictionMutation.isPending}
        error={updateRestrictionMutation.error}
      />
    </View>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: '#fff',
  },
  centerContainer: {
    flex: 1,
    justifyContent: 'center',
    alignItems: 'center',
    backgroundColor: '#fff',
  },
});
