import { Text, View } from 'react-native';

import { styles } from '../../shared/ui/styles';

type HomeSummaryCardProps = {
  openTaskCount: number;
  zoneCount: number;
  unpurchasedCount: number;
};

export function HomeSummaryCard({
  openTaskCount,
  zoneCount,
  unpurchasedCount,
}: HomeSummaryCardProps) {
  return (
    <View style={styles.homeSummaryRow}>
      <View style={styles.statTile}>
        <Text style={styles.statValue}>{openTaskCount}</Text>
        <Text style={styles.statLabel}>задач сегодня</Text>
      </View>
      <View style={styles.statTile}>
        <Text style={styles.statValue}>{zoneCount}</Text>
        <Text style={styles.statLabel}>активных зон</Text>
      </View>
      <View style={styles.statTile}>
        <Text style={styles.statValue}>{unpurchasedCount}</Text>
        <Text style={styles.statLabel}>покупок</Text>
      </View>
    </View>
  );
}
