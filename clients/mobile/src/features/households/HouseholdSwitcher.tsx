import { Pressable, ScrollView, Text, View } from 'react-native';

import type { HouseholdSummary } from '../../api/types';
import { styles } from '../../shared/ui/styles';

export function HouseholdSwitcher({
  households,
  selectedHouseholdId,
  onSelect,
}: {
  households: HouseholdSummary[];
  selectedHouseholdId: string | null;
  onSelect: (householdId: string) => void;
}) {
  if (households.length === 0) {
    return (
      <View style={styles.householdStrip}>
        <Text style={styles.householdStripText}>No households</Text>
      </View>
    );
  }

  return (
    <ScrollView
      contentContainerStyle={styles.householdChips}
      horizontal
      showsHorizontalScrollIndicator={false}
    >
      {households.map((household) => {
        const selected = household.id === selectedHouseholdId;
        return (
          <Pressable
            accessibilityRole="button"
            key={household.id}
            onPress={() => onSelect(household.id)}
            style={[styles.householdChip, selected && styles.householdChipActive]}
          >
            <Text style={[styles.householdChipText, selected && styles.householdChipTextActive]}>
              {household.name}
            </Text>
          </Pressable>
        );
      })}
    </ScrollView>
  );
}
