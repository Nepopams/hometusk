import { Text, View, type StyleProp, type ViewStyle } from 'react-native';

import { Mascot } from '../../shared/ui/Mascot';
import { styles } from '../../shared/ui/styles';
import type { CommandTone } from './commandOutcomeFormatting';
import { getCommandMascotMood, getCommandTone } from './commandOutcomeFormatting';

type CommandHeroCardProps = {
  status?: string | null;
  isSaving: boolean;
};

export function CommandHeroCard({ status, isSaving }: CommandHeroCardProps) {
  const tone = getCommandTone(status, isSaving);
  const copy = getHeroCopy(status, isSaving);

  return (
    <View style={[styles.commandHero, commandHeroStyle[tone]]}>
      <View style={styles.commandHeroCopy}>
        <Text style={styles.commandHeroTitle}>{copy.title}</Text>
        <Text style={styles.commandHeroBody}>{copy.body}</Text>
      </View>
      <Mascot mood={getCommandMascotMood(tone)} />
    </View>
  );
}

function getHeroCopy(status: string | null | undefined, isSaving: boolean) {
  if (isSaving) {
    return {
      title: 'Разбираю команду',
      body: 'Проверяю домашние правила и безопасный следующий шаг.',
    };
  }
  if (status === 'executed') {
    return {
      title: 'Готово',
      body: 'Действие применено через HomeTusk для выбранного дома.',
    };
  }
  if (status === 'scheduled') {
    return {
      title: 'Запланировано',
      body: 'HomeTusk поставил действие на нужное время.',
    };
  }
  if (status === 'needs_input') {
    return {
      title: 'Нужна деталь',
      body: 'Ответь на уточнение. Это не подтверждение действия.',
    };
  }
  if (status === 'needs_confirmation') {
    return {
      title: 'Нужно подтверждение',
      body: 'Пока ничего не изменилось. Проверь действие и подтверди явно.',
    };
  }
  if (status === 'rejected') {
    return {
      title: 'Команда остановлена',
      body: 'HomeTusk не применил действие, если оно не прошло правила дома.',
    };
  }
  if (status === 'executed_degraded') {
    return {
      title: 'Сделано осторожно',
      body: 'Применена только безопасная часть действия.',
    };
  }
  return {
    title: 'Готов принять команду',
    body: 'Напиши обычными словами, что нужно сделать дома.',
  };
}

const commandHeroStyle: Record<CommandTone, StyleProp<ViewStyle>> = {
  idle: styles.commandHeroIdle,
  thinking: styles.commandHeroThinking,
  success: styles.commandHeroExecuted,
  clarify: styles.commandHeroNeedsInput,
  confirm: styles.commandHeroConfirm,
  reject: styles.commandHeroRejected,
  degraded: styles.commandHeroDegraded,
};
