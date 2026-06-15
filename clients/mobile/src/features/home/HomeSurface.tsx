import { Text, View, type ColorValue } from 'react-native';

import type { HouseholdSummary, UserProfile } from '../../api/types';
import type { HouseholdReadModels } from '../../app/types';
import { formatShortDate } from '../../shared/format/dates';
import { formatNotificationType } from '../../shared/format/labels';
import { EmptyRow, InfoRow } from '../../shared/ui/InfoRow';
import { SectionList } from '../../shared/ui/SectionList';
import { StatTile } from '../../shared/ui/StatTile';
import { styles } from '../../shared/ui/styles';

export function HomeSurface({
  profile,
  selectedHousehold,
  models,
  accent,
}: {
  profile: UserProfile;
  selectedHousehold: HouseholdSummary;
  models: HouseholdReadModels;
  accent: ColorValue;
}) {
  const openTaskCount = models.tasks.filter((task) => task.status !== 'done').length;
  const unreadCount = models.notifications.filter((notification) => !notification.readAt).length;
  const unpurchasedCount = models.shoppingLists.reduce(
    (total, list) => total + list.unpurchasedCount,
    0
  );

  return (
    <View style={styles.section}>
      <View style={styles.statsGrid}>
        <StatTile label="Open tasks" value={String(openTaskCount)} />
        <StatTile label="Members" value={String(models.members.length)} />
        <StatTile label="To buy" value={String(unpurchasedCount)} />
        <StatTile label="Unread" value={String(unreadCount)} />
      </View>

      <View style={styles.profilePanel}>
        <Text style={styles.sectionTitle}>{selectedHousehold.name}</Text>
        <Text style={styles.profileName}>{profile.displayName}</Text>
        <Text style={styles.profileMeta}>{profile.email ?? profile.externalId}</Text>
      </View>

      <SectionList title="Members">
        {models.members.length === 0 ? (
          <EmptyRow accent={accent} title="No members loaded" />
        ) : (
          models.members.map((member) => (
            <InfoRow
              accent={accent}
              key={member.userId}
              meta={member.role}
              title={member.displayName}
            />
          ))
        )}
      </SectionList>

      <SectionList title="Zones">
        {models.zones.length === 0 ? (
          <EmptyRow accent={accent} title="No zones yet" />
        ) : (
          models.zones.map((zone) => (
            <InfoRow
              accent={accent}
              key={zone.id}
              meta={`Created ${formatShortDate(zone.createdAt)}`}
              title={zone.name}
            />
          ))
        )}
      </SectionList>

      <SectionList title="Notifications">
        {models.notifications.length === 0 ? (
          <EmptyRow accent={accent} title="No notifications" />
        ) : (
          models.notifications.slice(0, 5).map((notification) => (
            <InfoRow
              accent={accent}
              key={notification.id}
              meta={notification.readAt ? 'Read' : 'Unread'}
              title={notification.payload.summary ?? formatNotificationType(notification.type)}
            />
          ))
        )}
      </SectionList>
    </View>
  );
}
