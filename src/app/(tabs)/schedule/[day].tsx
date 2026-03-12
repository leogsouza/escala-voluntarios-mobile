import React, { useMemo } from 'react';
import { View, Text, StyleSheet, FlatList, ActivityIndicator, Pressable } from 'react-native';
import { useLocalSearchParams, Stack } from 'expo-router';
import dayjs from 'dayjs';
import 'dayjs/locale/pt-br';

import { useEventsByMonth } from '@/hooks/queries/useEvents';
import { DayEventCard } from '@/components/DayEventCard';
import { BRAND_COLORS } from '@/constants/colors';

dayjs.locale('pt-br');

export default function DayDetailScreen() {
  const { day, scheduleId } = useLocalSearchParams<{ day: string; scheduleId: string }>();
  
  const date = useMemo(() => dayjs(day), [day]);
  const year = date.year();
  const month = date.month() + 1; // dayjs month is 0-indexed, API expects 1-12
  
  const { data: allEvents, isLoading, isError, refetch } = useEventsByMonth(
    year, 
    month, 
    scheduleId ? parseInt(scheduleId, 10) : undefined
  );

  const dayEvents = useMemo(() => {
    if (!allEvents) return [];
    const targetDate = day; 
    return allEvents.filter(event => {
      const eventDate = event.date.slice(0, 10);
      return eventDate === targetDate;
    });
  }, [allEvents, day]);

  return (
    <View style={styles.container}>
      <Stack.Screen 
        options={{ 
          title: date.format('dddd, D [de] MMMM'),
          headerBackTitle: 'Calendário',
          headerTintColor: BRAND_COLORS.primary,
        }} 
      />
      
      {isLoading ? (
        <View style={styles.center}>
          <ActivityIndicator size="large" color={BRAND_COLORS.primary} />
        </View>
      ) : isError ? (
        <View style={styles.center}>
          <Text style={styles.errorText}>Erro ao carregar eventos</Text>
          <Pressable onPress={() => refetch()}>
            <Text style={styles.retryText}>Tentar novamente</Text>
          </Pressable>
        </View>
      ) : dayEvents.length === 0 ? (
        <View style={styles.center}>
          <Text style={styles.emptyText}>Nenhum evento neste dia</Text>
        </View>
      ) : (
        <FlatList
          data={dayEvents}
          keyExtractor={(item) => item.id.toString()}
          renderItem={({ item }) => <DayEventCard event={item} />}
          contentContainerStyle={styles.listContent}
        />
      )}
    </View>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: '#F3F4F6',
  },
  listContent: {
    padding: 16,
  },
  center: {
    flex: 1,
    justifyContent: 'center',
    alignItems: 'center',
    padding: 20,
  },
  errorText: {
    fontSize: 16,
    color: '#EF4444',
    marginBottom: 12,
  },
  retryText: {
    fontSize: 16,
    color: BRAND_COLORS.primary,
    fontWeight: 'bold',
  },
  emptyText: {
    fontSize: 16,
    color: '#6B7280',
  },
});
