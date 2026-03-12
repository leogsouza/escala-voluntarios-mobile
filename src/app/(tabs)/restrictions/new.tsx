import React from 'react';
import { View, StyleSheet } from 'react-native';
import { useRouter } from 'expo-router';
import { useQueryClient } from '@tanstack/react-query';

import RestrictionForm from '@/components/restrictions/RestrictionForm';
import { useCreateRestriction } from '@/hooks/queries/useRestrictions';
import { Restriction } from '@/types/restriction';
import { queryKeys } from '@/lib/queryKeys';

export default function NewRestrictionScreen() {
  const router = useRouter();
  const queryClient = useQueryClient();
  const createRestrictionMutation = useCreateRestriction();

  const handleSubmit = (data: Restriction) => {
    createRestrictionMutation.mutate(data, {
      onSuccess: () => {
        // Invalidate queries to refresh the list
        queryClient.invalidateQueries({ queryKey: queryKeys.restrictions.all() });
        router.back();
      },
      onError: (error) => {
        console.error('Failed to create restriction', error);
      },
    });
  };

  return (
    <View style={styles.container}>
      <RestrictionForm
        onSubmit={handleSubmit}
        isSubmitting={createRestrictionMutation.isPending}
        error={createRestrictionMutation.error}
      />
    </View>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: '#fff',
  },
});
