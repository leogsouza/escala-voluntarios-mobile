import React, { useState, useMemo } from 'react';
import { View, Text, StyleSheet, ActivityIndicator, ScrollView, Pressable } from 'react-native';
import { Calendar, DateData, LocaleConfig } from 'react-native-calendars';
import dayjs from 'dayjs';
import { useRouter } from 'expo-router';

import { useActiveSchedules } from '@/hooks/queries/useSchedules';
import { useEventsByMonth } from '@/hooks/queries/useEvents';
import { BRAND_COLORS, getServiceTypeColors } from '@/constants/colors';
import { DayEventCard } from '@/components/DayEventCard';
import { Event } from '@/types/schedule';

// Configure locale
LocaleConfig.locales['pt-br'] = {
  monthNames: ['Janeiro', 'Fevereiro', 'Março', 'Abril', 'Maio', 'Junho', 'Julho', 'Agosto', 'Setembro', 'Outubro', 'Novembro', 'Dezembro'],
  monthNamesShort: ['Jan', 'Fev', 'Mar', 'Abr', 'Mai', 'Jun', 'Jul', 'Ago', 'Set', 'Out', 'Nov', 'Dez'],
  dayNames: ['Domingo', 'Segunda', 'Terça', 'Quarta', 'Quinta', 'Sexta', 'Sábado'],
  dayNamesShort: ['Dom', 'Seg', 'Ter', 'Qua', 'Qui', 'Sex', 'Sáb'],
  today: 'Hoje'
};
LocaleConfig.defaultLocale = 'pt-br';

export default function ScheduleScreen() {
  const router = useRouter();
  const [selectedDate, setSelectedDate] = useState(dayjs().format('YYYY-MM-DD'));
  const [currentMonth, setCurrentMonth] = useState(dayjs().format('YYYY-MM-DD'));

  const { data: schedules, isLoading: isLoadingSchedules, isError: isScheduleError, refetch: refetchSchedules } = useActiveSchedules();
  const activeSchedule = schedules?.[0];

  const { data: events, isLoading: isLoadingEvents, isError: isEventError, refetch: refetchEvents } = useEventsByMonth(
    dayjs(currentMonth).year(),
    dayjs(currentMonth).month() + 1,
    activeSchedule?.id
  );

  const markedDates = useMemo(() => {
    const marks: any = {};
    
    if (events) {
      events.forEach((event: Event) => {
        const date = event.date.slice(0, 10);
        if (!marks[date]) {
          marks[date] = { dots: [] };
        }
        const serviceType = event.service?.toLowerCase() || 'default';
        const color = getServiceTypeColors(serviceType).background;
        
        // Check if dot already exists to avoid duplicates
        const dotExists = marks[date].dots.some((d: any) => d.key === event.id.toString());
        if (!dotExists) {
            marks[date].dots.push({ key: event.id.toString(), color });
        }
      });
    }

    if (selectedDate) {
      marks[selectedDate] = {
        ...(marks[selectedDate] || {}),
        selected: true,
        selectedColor: BRAND_COLORS.primary,
        disableTouchEvent: true
      };
    }

    return marks;
  }, [events, selectedDate]);

  const selectedDayEvents = useMemo(() => {
    if (!events || !selectedDate) return [];
    return events.filter((event: Event) => event.date.slice(0, 10) === selectedDate);
  }, [events, selectedDate]);

  const handleDayPress = (day: DateData) => {
    setSelectedDate(day.dateString);
  };

  const handleMonthChange = (date: DateData) => {
    setCurrentMonth(date.dateString);
  };

  const navigateToDayDetail = () => {
    if (activeSchedule) {
      router.push({
        pathname: '/(tabs)/schedule/[day]',
        params: { day: selectedDate, scheduleId: activeSchedule.id }
      });
    }
  };

  if (isLoadingSchedules) {
    return (
      <View style={styles.center}>
        <ActivityIndicator size="large" color={BRAND_COLORS.primary} />
      </View>
    );
  }

  if (isScheduleError) {
    return (
        <View style={styles.center}>
            <Text style={styles.errorText}>Erro ao carregar escala.</Text>
            <Pressable onPress={() => refetchSchedules()}>
                <Text style={styles.retryText}>Tentar novamente</Text>
            </Pressable>
        </View>
    );
  }

  if (!activeSchedule) {
    return (
      <View style={styles.center}>
        <Text style={styles.emptyText}>Nenhuma escala ativa encontrada.</Text>
      </View>
    );
  }

  return (
    <View style={styles.container}>
      <Calendar
        current={currentMonth}
        minDate={activeSchedule.start_date.slice(0, 10)}
        maxDate={activeSchedule.end_date.slice(0, 10)}
        onDayPress={handleDayPress}
        onMonthChange={handleMonthChange}
        markedDates={markedDates}
        markingType="multi-dot"
        theme={{
          selectedDayBackgroundColor: BRAND_COLORS.primary,
          todayTextColor: BRAND_COLORS.accent,
          arrowColor: BRAND_COLORS.primary,
        }}
      />
      
      <View style={styles.bottomPanel}>
        <View style={styles.panelHeader}>
          <Text style={styles.panelTitle}>
            Eventos de {dayjs(selectedDate).format('DD/MM')}
          </Text>
          <Pressable onPress={navigateToDayDetail}>
            <Text style={styles.seeAllLink}>Ver detalhes</Text>
          </Pressable>
        </View>

        {isLoadingEvents ? (
          <ActivityIndicator color={BRAND_COLORS.primary} style={{ marginTop: 20 }} />
        ) : selectedDayEvents.length === 0 ? (
          <Text style={styles.noEventsText}>Nenhum evento neste dia.</Text>
        ) : (
          <ScrollView style={styles.eventList}>
            {selectedDayEvents.map((event: Event) => (
              <DayEventCard key={event.id} event={event} />
            ))}
          </ScrollView>
        )}
      </View>
    </View>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: '#fff',
  },
  center: {
    flex: 1,
    justifyContent: 'center',
    alignItems: 'center',
  },
  bottomPanel: {
    flex: 1,
    backgroundColor: '#F9FAFB',
    borderTopWidth: 1,
    borderTopColor: '#E5E7EB',
    padding: 16,
  },
  panelHeader: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    marginBottom: 12,
  },
  panelTitle: {
    fontSize: 18,
    fontWeight: 'bold',
    color: '#111827',
  },
  seeAllLink: {
    fontSize: 14,
    color: BRAND_COLORS.primary,
    fontWeight: '600',
  },
  eventList: {
    flex: 1,
  },
  noEventsText: {
    color: '#6B7280',
    fontStyle: 'italic',
    marginTop: 8,
  },
  errorText: {
    color: '#EF4444',
    marginBottom: 8,
  },
  retryText: {
    color: BRAND_COLORS.primary,
    textDecorationLine: 'underline',
  },
  emptyText: {
    fontSize: 16,
    color: '#6B7280',
  }
});
