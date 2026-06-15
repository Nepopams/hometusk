import { Text, View } from 'react-native';

import type { StatusBannerMessage } from '../../app/types';
import { styles } from './styles';

export function StatusBanner({ status }: { status: StatusBannerMessage | null }) {
  if (!status) {
    return null;
  }

  return (
    <View
      style={[
        styles.statusBanner,
        status.tone === 'success' && styles.statusBannerSuccess,
        status.tone === 'error' && styles.statusBannerError,
      ]}
    >
      <Text
        style={[
          styles.statusBannerText,
          status.tone === 'error' && styles.statusBannerTextError,
        ]}
      >
        {status.text}
      </Text>
    </View>
  );
}
