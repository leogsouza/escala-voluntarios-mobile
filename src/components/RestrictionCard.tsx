import React, { useMemo } from 'react';
import { View, StyleSheet } from 'react-native';
import { Card, Text, IconButton, Chip, useTheme } from 'react-native-paper';
import { Restriction, RestrictionRules } from '@/types/restriction';
import { parseRulesSummary } from '@/lib/restriction-rules';

interface RestrictionCardProps {
  restriction: Restriction;
  onDelete: (id: number) => void;
  onPress: (id: number) => void;
}

export function RestrictionCard({ restriction, onDelete, onPress }: RestrictionCardProps) {
  const theme = useTheme();
  
  const summary = useMemo(() => {
    return parseRulesSummary(restriction.rules_json);
  }, [restriction.rules_json]);
  
  const mode = useMemo(() => {
    let m: 'exclude' | 'include' = 'exclude';
    try {
      if (restriction.rules_json) {
        const rules = JSON.parse(restriction.rules_json) as RestrictionRules;
        if (rules.mode) m = rules.mode;
      }
    } catch (e) {
      // default to exclude
    }
    return m;
  }, [restriction.rules_json]);

  const isExclude = mode === 'exclude';
  const badgeBackgroundColor = isExclude ? theme.colors.errorContainer : '#E8F5E9';
  const badgeTextColor = isExclude ? theme.colors.error : '#2E7D32';

  return (
    <Card style={styles.card} onPress={() => restriction.id && onPress(restriction.id)}>
      <Card.Content>
        <View style={styles.header}>
          <Text variant="titleMedium" style={styles.volunteerName} numberOfLines={1}>
            {restriction.volunteer?.name || 'Sem voluntário'}
          </Text>
          <Chip 
            compact 
            textStyle={{ color: badgeTextColor, fontWeight: 'bold', fontSize: 10, lineHeight: 16 }}
            style={{ backgroundColor: badgeBackgroundColor, height: 24, borderRadius: 12 }}
          >
            {isExclude ? 'Bloquear' : 'Preferir'}
          </Chip>
        </View>

        <View style={styles.contentRow}>
          <View style={styles.textContainer}>
            <Text variant="bodyMedium" style={styles.summaryText} numberOfLines={2}>
              {summary ? summary.summary : 'Sem regras definidas'}
            </Text>
            {summary && summary.details.length > 0 && (
              <Text variant="bodySmall" style={styles.detailsText} numberOfLines={1}>
                {summary.details[0]}
              </Text>
            )}
          </View>
          
          <IconButton
            icon="trash-can-outline"
            iconColor={theme.colors.error}
            size={20}
            onPress={() => restriction.id && onDelete(restriction.id)}
            style={styles.deleteButton}
          />
        </View>
      </Card.Content>
    </Card>
  );
}

const styles = StyleSheet.create({
  card: {
    marginHorizontal: 16,
    marginVertical: 8,
    elevation: 2,
    backgroundColor: 'white',
  },
  header: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    marginBottom: 8,
  },
  volunteerName: {
    fontWeight: 'bold',
    flex: 1,
    marginRight: 8,
  },
  contentRow: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
  },
  textContainer: {
    flex: 1,
    marginRight: 8,
  },
  summaryText: {
    color: '#333',
  },
  detailsText: {
    color: '#666',
    marginTop: 2,
  },
  deleteButton: {
    margin: 0,
  },
});
