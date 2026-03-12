import React, { useEffect, useState } from 'react';
import {
  View,
  ScrollView,
  StyleSheet,
  KeyboardAvoidingView,
  Platform,
  TouchableOpacity,
} from 'react-native';
import {
  TextInput,
  Button,
  Text,
  useTheme,
  HelperText,
  Chip,
  Menu,
  Divider,
  ActivityIndicator,
  SegmentedButtons,
  IconButton,
} from 'react-native-paper';
import { useRouter } from 'expo-router';
import dayjs from 'dayjs';

import { useActiveSchedules } from '@/hooks/queries/useSchedules';
import { useSearchVolunteers } from '@/hooks/queries/useVolunteers';
import { useRestrictionTypes } from '@/hooks/queries/useRestrictionTypes';
import { Restriction, RestrictionRules, SpecificDate, DateRangeEntry } from '@/types/restriction';

interface RestrictionFormProps {
  initialData?: Restriction;
  onSubmit: (data: Restriction) => void;
  isSubmitting: boolean;
  error?: Error | null;
}

const WEEKDAYS = [
  { label: 'Dom', value: 0 },
  { label: 'Seg', value: 1 },
  { label: 'Ter', value: 2 },
  { label: 'Qua', value: 3 },
  { label: 'Qui', value: 4 },
  { label: 'Sex', value: 5 },
  { label: 'Sáb', value: 6 },
];

const PERIODS = [
  { label: 'Manhã', value: 'MORNING' },
  { label: 'Tarde', value: 'AFTERNOON' },
  { label: 'Noite', value: 'EVENING' },
  { label: 'Madrugada', value: 'NIGHT' },
];

export default function RestrictionForm({
  initialData,
  onSubmit,
  isSubmitting,
  error,
}: RestrictionFormProps) {
  const theme = useTheme();
  const router = useRouter();

  // Form State
  const [volunteerQuery, setVolunteerQuery] = useState('');
  const [selectedVolunteer, setSelectedVolunteer] = useState<{ id: number; name: string } | null>(
    initialData?.volunteer
      ? { id: initialData.volunteer.id, name: initialData.volunteer.full_name }
      : null
  );
  const [scheduleId, setScheduleId] = useState<number | undefined>(initialData?.schedule_id);
  const [typeId, setTypeId] = useState<number | undefined>(initialData?.restriction_type_id);
  const [description, setDescription] = useState(initialData?.description || '');
  
  // Rules State
  const initialRules: RestrictionRules = initialData?.rules_json
    ? JSON.parse(initialData.rules_json)
    : { mode: 'exclude' };

  const [mode, setMode] = useState<'exclude' | 'include'>(initialRules.mode || 'exclude');
  const [weekdays, setWeekdays] = useState<number[]>(initialRules.weekdays || []);
  const [periods, setPeriods] = useState<string[]>(initialRules.periods || []);
  const [specificDates, setSpecificDates] = useState<SpecificDate[]>(
    initialRules.specificDates || []
  );
  const [dateRanges, setDateRanges] = useState<DateRangeEntry[]>(initialRules.dateRanges || []);

  // UI State
  const [showScheduleMenu, setShowScheduleMenu] = useState(false);
  const [showTypeMenu, setShowTypeMenu] = useState(false);
  const [showVolunteerList, setShowVolunteerList] = useState(false);
  const [formError, setFormError] = useState<string | null>(null);

  // Queries
  const { data: schedules } = useActiveSchedules();
  const { data: restrictionTypes } = useRestrictionTypes();
  const { data: volunteers, isFetching: isSearchingVolunteers } = useSearchVolunteers(volunteerQuery);

  useEffect(() => {
    if (volunteerQuery.length >= 2) {
      setShowVolunteerList(true);
    } else {
      setShowVolunteerList(false);
    }
  }, [volunteerQuery]);

  const handleVolunteerSelect = (volunteer: { id: number; full_name: string }) => {
    setSelectedVolunteer({ id: volunteer.id, name: volunteer.full_name });
    setVolunteerQuery('');
    setShowVolunteerList(false);
  };

  const toggleWeekday = (day: number) => {
    setWeekdays((prev) =>
      prev.includes(day) ? prev.filter((d) => d !== day) : [...prev, day].sort()
    );
  };

  const togglePeriod = (period: string) => {
    setPeriods((prev) =>
      prev.includes(period) ? prev.filter((p) => p !== period) : [...prev, period]
    );
  };

  const addSpecificDate = () => {
    setSpecificDates([...specificDates, { date: '', notes: '' }]);
  };

  const updateSpecificDate = (index: number, field: keyof SpecificDate, value: string) => {
    const newDates = [...specificDates];
    newDates[index] = { ...newDates[index], [field]: value };
    setSpecificDates(newDates);
  };

  const removeSpecificDate = (index: number) => {
    setSpecificDates(specificDates.filter((_, i) => i !== index));
  };

  const addDateRange = () => {
    setDateRanges([...dateRanges, { start: '', end: '' }]);
  };

  const updateDateRange = (index: number, field: keyof DateRangeEntry, value: string) => {
    const newRanges = [...dateRanges];
    newRanges[index] = { ...newRanges[index], [field]: value };
    setDateRanges(newRanges);
  };

  const removeDateRange = (index: number) => {
    setDateRanges(dateRanges.filter((_, i) => i !== index));
  };

  const validateDate = (date: string) => {
    return /^\d{4}-\d{2}-\d{2}$/.test(date) && dayjs(date).isValid();
  };

  const handleSubmit = () => {
    setFormError(null);

    if (!selectedVolunteer) {
      setFormError('Selecione um voluntário');
      return;
    }
    if (!scheduleId) {
      setFormError('Selecione uma escala');
      return;
    }
    if (!typeId) {
      setFormError('Selecione um tipo de restrição');
      return;
    }

    const hasRules =
      weekdays.length > 0 ||
      periods.length > 0 ||
      specificDates.length > 0 ||
      dateRanges.length > 0;

    if (!hasRules) {
      setFormError('Adicione pelo menos uma regra (dia da semana, período ou data)');
      return;
    }

    // Validate dates
    for (const d of specificDates) {
      if (!validateDate(d.date)) {
        setFormError(`Data inválida: ${d.date}. Use o formato AAAA-MM-DD`);
        return;
      }
    }
    for (const r of dateRanges) {
      if (!validateDate(r.start) || !validateDate(r.end)) {
        setFormError(`Intervalo inválido: ${r.start} - ${r.end}. Use o formato AAAA-MM-DD`);
        return;
      }
    }

    const rules: RestrictionRules = {
      mode,
      weekdays: weekdays.length > 0 ? weekdays : undefined,
      periods: periods.length > 0 ? periods : undefined,
      specificDates: specificDates.length > 0 ? specificDates : undefined,
      dateRanges: dateRanges.length > 0 ? dateRanges : undefined,
    };

    const data: Restriction = {
      ...initialData,
      volunteer_id: selectedVolunteer.id,
      schedule_id: scheduleId,
      restriction_type_id: typeId,
      description,
      rules_json: JSON.stringify(rules),
    };

    onSubmit(data);
  };

  return (
    <KeyboardAvoidingView
      behavior={Platform.OS === 'ios' ? 'padding' : 'height'}
      style={styles.container}
    >
      <ScrollView contentContainerStyle={styles.content}>
        {/* Volunteer Selection */}
        <Text variant="titleMedium" style={styles.sectionTitle}>
          Voluntário
        </Text>
        {selectedVolunteer ? (
          <View style={styles.selectedVolunteer}>
            <Text variant="bodyLarge">{selectedVolunteer.name}</Text>
            <IconButton icon="close" onPress={() => setSelectedVolunteer(null)} />
          </View>
        ) : (
          <>
            <TextInput
              label="Buscar voluntário"
              value={volunteerQuery}
              onChangeText={setVolunteerQuery}
              mode="outlined"
              right={isSearchingVolunteers ? <TextInput.Icon icon="loading" /> : null}
            />
            {showVolunteerList && volunteers && (
              <View style={styles.volunteerList}>
                {volunteers.map((v) => (
                  <TouchableOpacity
                    key={v.id}
                    style={styles.volunteerItem}
                    onPress={() => handleVolunteerSelect(v)}
                  >
                    <Text>{v.full_name}</Text>
                  </TouchableOpacity>
                ))}
              </View>
            )}
          </>
        )}

        {/* Schedule Selection */}
        <Text variant="titleMedium" style={styles.sectionTitle}>
          Escala
        </Text>
        <Menu
          visible={showScheduleMenu}
          onDismiss={() => setShowScheduleMenu(false)}
          anchor={
            <TouchableOpacity onPress={() => setShowScheduleMenu(true)}>
              <TextInput
                label="Selecione a escala"
                value={schedules?.find((s) => s.id === scheduleId)?.name || ''}
                mode="outlined"
                editable={false}
                right={<TextInput.Icon icon="menu-down" />}
              />
            </TouchableOpacity>
          }
        >
          {schedules?.map((schedule) => (
            <Menu.Item
              key={schedule.id}
              onPress={() => {
                setScheduleId(schedule.id);
                setShowScheduleMenu(false);
              }}
              title={schedule.name}
            />
          ))}
        </Menu>

        {/* Type Selection */}
        <Text variant="titleMedium" style={styles.sectionTitle}>
          Tipo de Restrição
        </Text>
        <Menu
          visible={showTypeMenu}
          onDismiss={() => setShowTypeMenu(false)}
          anchor={
            <TouchableOpacity onPress={() => setShowTypeMenu(true)}>
              <TextInput
                label="Selecione o tipo"
                value={restrictionTypes?.find((t) => t.id === typeId)?.name || ''}
                mode="outlined"
                editable={false}
                right={<TextInput.Icon icon="menu-down" />}
              />
            </TouchableOpacity>
          }
        >
          {restrictionTypes?.map((type) => (
            <Menu.Item
              key={type.id}
              onPress={() => {
                setTypeId(type.id);
                setShowTypeMenu(false);
              }}
              title={type.name}
            />
          ))}
        </Menu>

        <TextInput
          label="Descrição"
          value={description}
          onChangeText={setDescription}
          mode="outlined"
          multiline
          numberOfLines={3}
          style={styles.input}
        />

        <Divider style={styles.divider} />

        <Text variant="titleLarge" style={styles.sectionTitle}>
          Regras
        </Text>

        <SegmentedButtons
          value={mode}
          onValueChange={(val) => setMode(val as 'exclude' | 'include')}
          buttons={[
            { value: 'exclude', label: 'Excluir', icon: 'block-helper' },
            { value: 'include', label: 'Incluir', icon: 'check-circle-outline' },
          ]}
          style={styles.input}
        />

        {/* Weekdays */}
        <Text variant="titleMedium" style={styles.sectionTitle}>
          Dias da Semana
        </Text>
        <View style={styles.chipContainer}>
          {WEEKDAYS.map((day) => (
            <Chip
              key={day.value}
              selected={weekdays.includes(day.value)}
              onPress={() => toggleWeekday(day.value)}
              style={styles.chip}
              showSelectedOverlay
            >
              {day.label}
            </Chip>
          ))}
        </View>

        {/* Periods */}
        <Text variant="titleMedium" style={styles.sectionTitle}>
          Períodos
        </Text>
        <View style={styles.chipContainer}>
          {PERIODS.map((period) => (
            <Chip
              key={period.value}
              selected={periods.includes(period.value)}
              onPress={() => togglePeriod(period.value)}
              style={styles.chip}
              showSelectedOverlay
            >
              {period.label}
            </Chip>
          ))}
        </View>

        {/* Specific Dates */}
        <View style={styles.rowBetween}>
          <Text variant="titleMedium" style={styles.sectionTitle}>
            Datas Específicas
          </Text>
          <Button onPress={addSpecificDate} mode="text" icon="plus">
            Adicionar
          </Button>
        </View>
        {specificDates.map((item, index) => (
          <View key={index} style={styles.dateRow}>
            <TextInput
              label="Data (AAAA-MM-DD)"
              value={item.date}
              onChangeText={(text) => updateSpecificDate(index, 'date', text)}
              mode="outlined"
              style={styles.dateInput}
              keyboardType="numeric"
              maxLength={10}
            />
            <TextInput
              label="Notas"
              value={item.notes || ''}
              onChangeText={(text) => updateSpecificDate(index, 'notes', text)}
              mode="outlined"
              style={styles.notesInput}
            />
            <IconButton icon="delete" onPress={() => removeSpecificDate(index)} />
          </View>
        ))}

        {/* Date Ranges */}
        <View style={styles.rowBetween}>
          <Text variant="titleMedium" style={styles.sectionTitle}>
            Intervalos de Datas
          </Text>
          <Button onPress={addDateRange} mode="text" icon="plus">
            Adicionar
          </Button>
        </View>
        {dateRanges.map((item, index) => (
          <View key={index} style={styles.dateRow}>
            <TextInput
              label="Início (AAAA-MM-DD)"
              value={item.start}
              onChangeText={(text) => updateDateRange(index, 'start', text)}
              mode="outlined"
              style={styles.rangeInput}
              keyboardType="numeric"
              maxLength={10}
            />
            <TextInput
              label="Fim (AAAA-MM-DD)"
              value={item.end}
              onChangeText={(text) => updateDateRange(index, 'end', text)}
              mode="outlined"
              style={styles.rangeInput}
              keyboardType="numeric"
              maxLength={10}
            />
            <IconButton icon="delete" onPress={() => removeDateRange(index)} />
          </View>
        ))}

        {formError && (
          <HelperText type="error" visible={!!formError}>
            {formError}
          </HelperText>
        )}

        {error && (
          <HelperText type="error" visible={!!error}>
            {error.message}
          </HelperText>
        )}

        <Button
          mode="contained"
          onPress={handleSubmit}
          loading={isSubmitting}
          disabled={isSubmitting}
          style={styles.submitButton}
        >
          Salvar Restrição
        </Button>
      </ScrollView>
    </KeyboardAvoidingView>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: '#fff',
  },
  content: {
    padding: 16,
    paddingBottom: 100,
  },
  sectionTitle: {
    marginTop: 16,
    marginBottom: 8,
    fontWeight: 'bold',
  },
  input: {
    marginBottom: 12,
  },
  divider: {
    marginVertical: 16,
  },
  chipContainer: {
    flexDirection: 'row',
    flexWrap: 'wrap',
    gap: 8,
  },
  chip: {
    marginBottom: 4,
  },
  selectedVolunteer: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
    padding: 8,
    borderWidth: 1,
    borderColor: '#ccc',
    borderRadius: 4,
    marginBottom: 12,
  },
  volunteerList: {
    maxHeight: 150,
    borderWidth: 1,
    borderColor: '#eee',
    borderRadius: 4,
    marginBottom: 12,
  },
  volunteerItem: {
    padding: 12,
    borderBottomWidth: 1,
    borderBottomColor: '#eee',
  },
  rowBetween: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    marginTop: 16,
  },
  dateRow: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 8,
    marginBottom: 8,
  },
  dateInput: {
    flex: 1,
  },
  notesInput: {
    flex: 1.5,
  },
  rangeInput: {
    flex: 1,
  },
  submitButton: {
    marginTop: 24,
  },
});
