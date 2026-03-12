import React, { useMemo } from 'react';
import { View, Text, StyleSheet, ScrollView, ActivityIndicator, Pressable } from 'react-native';
import { useLocalSearchParams, Stack, useRouter } from 'expo-router';
import dayjs from 'dayjs';
import 'dayjs/locale/pt-br';

import { useAssignmentsByEvent } from '@/hooks/queries/useAssignments';
import { VolunteerAssignmentCard } from '@/components/VolunteerAssignmentCard';
import { getServiceTypeColors, BRAND_COLORS } from '@/constants/colors';
import { Assignment, Event } from '@/types/schedule';

dayjs.locale('pt-br');

export default function EventDetailScreen() {
  const { eventId, eventJson } = useLocalSearchParams<{ eventId: string; eventJson: string }>();
  const router = useRouter();

  const eventIdNum = parseInt(eventId, 10);
  
  const { data: assignments, isLoading, isError, refetch } = useAssignmentsByEvent(eventIdNum);

  const eventFromParams = useMemo(() => {
    if (eventJson) {
      try {
        return JSON.parse(eventJson) as Event;
      } catch (e) {
        return null;
      }
    }
    return null;
  }, [eventJson]);

  const event = eventFromParams || assignments?.[0]?.event;

  const serviceColors = useMemo(() => {
    const type = event?.service?.toLowerCase() || 'default';
    return getServiceTypeColors(type);
  }, [event]);

  const groupedAssignments = useMemo(() => {
    if (!assignments) return {};

    const groups: Record<string, Assignment[]> = {};
    
    assignments.forEach((assignment) => {
      const roleName = assignment.position?.role?.name || 'Sem função';
      if (!groups[roleName]) {
        groups[roleName] = [];
      }
      groups[roleName].push(assignment);
    });

    return groups;
  }, [assignments]);

  const sortedRoleNames = useMemo(() => {
    return Object.keys(groupedAssignments).sort();
  }, [groupedAssignments]);

  const formattedDate = useMemo(() => {
    if (!event) return '';
    return dayjs(event.date).format('dddd, D [de] MMMM');
  }, [event]);

  if (!event && isLoading) {
    return (
      <View style={styles.center}>
        <ActivityIndicator size="large" color={BRAND_COLORS.primary} />
      </View>
    );
  }

  if (!event && isError) {
    return (
      <View style={styles.center}>
        <Text style={styles.errorText}>Erro ao carregar detalhes do evento</Text>
        <Pressable onPress={() => router.back()}>
          <Text style={styles.retryText}>Voltar</Text>
        </Pressable>
      </View>
    );
  }

  return (
    <View style={styles.container}>
      <Stack.Screen 
        options={{ 
          title: event?.service || 'Detalhes do Evento',
          headerBackTitle: 'Eventos',
          headerTintColor: BRAND_COLORS.primary,
        }} 
      />

      {event && (
        <View style={[styles.header, { backgroundColor: serviceColors.background }]}>
          <Text style={[styles.serviceName, { color: serviceColors.text }]}>
            {event.service}
          </Text>
          <Text style={[styles.dateText, { color: serviceColors.text }]}>
            {formattedDate} • {event.time}
          </Text>
        </View>
      )}

      <ScrollView contentContainerStyle={styles.content}>
        {isLoading ? (
          <View style={styles.loadingContainer}>
            <ActivityIndicator size="large" color={BRAND_COLORS.primary} />
          </View>
        ) : isError ? (
          <View style={styles.center}>
            <Text style={styles.errorText}>Erro ao carregar voluntários</Text>
            <Pressable onPress={() => refetch()}>
              <Text style={styles.retryText}>Tentar novamente</Text>
            </Pressable>
          </View>
        ) : !assignments || assignments.length === 0 ? (
          <View style={styles.emptyContainer}>
            <Text style={styles.emptyText}>Nenhum voluntário escalado</Text>
          </View>
        ) : (
          sortedRoleNames.map((roleName) => (
            <View key={roleName} style={styles.section}>
              <Text style={styles.sectionHeader}>{roleName}</Text>
              {groupedAssignments[roleName].map((assignment) => (
                <VolunteerAssignmentCard 
                  key={assignment.id} 
                  assignment={assignment} 
                />
              ))}
            </View>
          ))
        )}
      </ScrollView>
    </View>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: '#F3F4F6',
  },
  center: {
    flex: 1,
    justifyContent: 'center',
    alignItems: 'center',
    padding: 20,
  },
  loadingContainer: {
    padding: 20,
    alignItems: 'center',
  },
  header: {
    padding: 20,
    paddingBottom: 24,
    borderBottomLeftRadius: 16,
    borderBottomRightRadius: 16,
    shadowColor: '#000',
    shadowOffset: { width: 0, height: 2 },
    shadowOpacity: 0.1,
    shadowRadius: 4,
    elevation: 3,
    marginBottom: 16,
  },
  serviceName: {
    fontSize: 24,
    fontWeight: 'bold',
    marginBottom: 4,
  },
  dateText: {
    fontSize: 16,
    fontWeight: '500',
    opacity: 0.9,
  },
  content: {
    padding: 16,
    paddingTop: 0,
    paddingBottom: 32,
  },
  section: {
    marginBottom: 20,
  },
  sectionHeader: {
    fontSize: 18,
    fontWeight: 'bold',
    color: '#374151',
    marginBottom: 12,
    marginLeft: 4,
  },
  errorText: {
    fontSize: 16,
    color: '#EF4444',
    marginBottom: 12,
    textAlign: 'center',
  },
  retryText: {
    fontSize: 16,
    color: BRAND_COLORS.primary,
    fontWeight: 'bold',
  },
  emptyContainer: {
    padding: 32,
    alignItems: 'center',
  },
  emptyText: {
    fontSize: 16,
    color: '#6B7280',
    textAlign: 'center',
  },
});
