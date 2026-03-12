import React from 'react';
import { View, Text, StyleSheet } from 'react-native';
import { Assignment } from '@/types/schedule';
import { BRAND_COLORS } from '@/constants/colors';

interface VolunteerAssignmentCardProps {
  assignment: Assignment;
}

export function VolunteerAssignmentCard({ assignment }: VolunteerAssignmentCardProps) {
  const volunteerName = assignment.volunteer?.full_name || assignment.volunteer?.name || 'Voluntário desconhecido';
  const positionName = assignment.position?.name || 'Posição desconhecida';
  const status = assignment.status;

  const getStatusBadge = (status: string) => {
    switch (status) {
      case 'PUBLISHED':
        return { 
          bg: '#DCFCE7', // green-100
          text: '#166534', // green-800
          label: 'Confirmado' 
        };
      case 'PENDING':
        return { 
          bg: '#FEF3C7', // amber-100
          text: '#92400E', // amber-800
          label: 'Pendente' 
        };
      case 'CANCELLED':
        return { 
          bg: '#FEE2E2', // red-100
          text: '#991B1B', // red-800
          label: 'Cancelado' 
        };
      default:
        return { 
          bg: '#F3F4F6', // gray-100
          text: '#374151', // gray-700
          label: status 
        };
    }
  };

  const badge = getStatusBadge(status);

  return (
    <View style={styles.card}>
      <View style={styles.info}>
        <Text style={styles.name}>{volunteerName}</Text>
        <Text style={styles.position}>{positionName}</Text>
      </View>
      <View style={[styles.badge, { backgroundColor: badge.bg }]}>
        <Text style={[styles.badgeText, { color: badge.text }]}>{badge.label}</Text>
      </View>
    </View>
  );
}

const styles = StyleSheet.create({
  card: {
    backgroundColor: 'white',
    borderRadius: 8,
    padding: 12,
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    marginBottom: 8,
    shadowColor: '#000',
    shadowOffset: { width: 0, height: 1 },
    shadowOpacity: 0.05,
    shadowRadius: 2,
    elevation: 2,
  },
  info: {
    flex: 1,
    marginRight: 8,
  },
  name: {
    fontSize: 16,
    fontWeight: 'bold',
    color: '#1F2937', // gray-800
    marginBottom: 4,
  },
  position: {
    fontSize: 14,
    color: '#6B7280', // gray-500
  },
  badge: {
    paddingHorizontal: 8,
    paddingVertical: 4,
    borderRadius: 12,
  },
  badgeText: {
    fontSize: 12,
    fontWeight: '600',
  },
});
