import React from 'react';
import { View, Text, StyleSheet, Pressable, ActivityIndicator } from 'react-native';
import { useRouter } from 'expo-router';
import MaterialIcons from 'react-native-vector-icons/MaterialIcons';

import { Event } from '@/types/schedule';
import { getServiceTypeColors } from '@/constants/colors';
import { useAssignmentsByEvent } from '@/hooks/queries/useAssignments';

interface DayEventCardProps {
  event: Event;
}

export function DayEventCard({ event }: DayEventCardProps) {
  const router = useRouter();
  const { data: assignments, isLoading } = useAssignmentsByEvent(event.id);
  
  const serviceType = event.service?.toLowerCase() || 'default';
  const colors = getServiceTypeColors(serviceType);

  const handlePress = () => {
    router.push({
      pathname: `/(tabs)/schedule/event/${event.id}`,
      params: { eventJson: JSON.stringify(event) }
    });
  };

  return (
    <Pressable
      style={({ pressed }) => [
        styles.container,
        { borderLeftColor: colors.background },
        pressed && styles.pressed
      ]}
      onPress={handlePress}
    >
      <View style={styles.content}>
        <View style={styles.header}>
          <Text style={styles.serviceName}>{event.service}</Text>
          <Text style={styles.time}>{event.time}</Text>
        </View>
        
        {isLoading ? (
          <ActivityIndicator size="small" color={colors.text} />
        ) : (
          <View style={styles.footer}>
             <MaterialIcons name="people" size={16} color="#6B7280" />
             <Text style={styles.volunteerCount}>
               {assignments ? assignments.length : 0} voluntários
             </Text>
             {event.notes ? (
               <Text style={styles.notes} numberOfLines={1}>• {event.notes}</Text>
             ) : null}
          </View>
        )}
      </View>
    </Pressable>
  );
}

const styles = StyleSheet.create({
  container: {
    backgroundColor: 'white',
    borderRadius: 8,
    borderLeftWidth: 4,
    padding: 16,
    marginBottom: 12,
    elevation: 2,
    shadowColor: '#000',
    shadowOffset: { width: 0, height: 1 },
    shadowOpacity: 0.1,
    shadowRadius: 2,
  },
  pressed: {
    opacity: 0.7
  },
  content: {
    gap: 8
  },
  header: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center'
  },
  serviceName: {
    fontSize: 16,
    fontWeight: 'bold',
    color: '#1F2937'
  },
  time: {
    fontSize: 14,
    fontWeight: '500',
    color: '#4B5563'
  },
  footer: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 6,
    marginTop: 4
  },
  volunteerCount: {
    fontSize: 14,
    color: '#6B7280'
  },
  notes: {
    fontSize: 12,
    color: '#9CA3AF',
    fontStyle: 'italic',
    flex: 1
  }
});
