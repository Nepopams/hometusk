import { Pressable, Text, View, type ColorValue } from 'react-native';

import type { Task } from '../../api/types';
import type { MutationControls } from '../../app/types';
import { formatShortDate } from '../../shared/format/dates';
import { formatTaskStatus } from '../../shared/format/labels';
import { EmptyState } from '../../shared/ui/EmptyState';
import { LabeledInput } from '../../shared/ui/LabeledInput';
import { SectionList } from '../../shared/ui/SectionList';
import { styles } from '../../shared/ui/styles';

export function TasksSurface({
  tasks,
  accent,
  controls,
  highlightedTaskId,
}: {
  tasks: Task[];
  accent: ColorValue;
  controls: MutationControls;
  highlightedTaskId: string | null;
}) {
  return (
    <View style={styles.section}>
      <View style={styles.formPanel}>
        <LabeledInput
          editable={!controls.savingAction}
          label="Task title"
          onChangeText={controls.onChangeTaskTitle}
          placeholder="Take out recycling"
          value={controls.taskTitle}
        />
        <Pressable
          accessibilityRole="button"
          disabled={Boolean(controls.savingAction)}
          onPress={controls.onCreateTask}
          style={({ pressed }) => [
            styles.primaryButton,
            pressed && styles.buttonPressed,
            controls.savingAction && styles.buttonDisabled,
          ]}
        >
          <Text style={styles.primaryButtonText}>
            {controls.savingAction === 'create-task' ? 'Creating...' : 'Create task'}
          </Text>
        </Pressable>
      </View>

      {tasks.length === 0 ? (
        <EmptyState
          body="Создай первую задачу здесь или отправь домашнюю команду."
          mascotMood="idle"
          title="Задач пока нет"
        />
      ) : (
        <SectionList title={`${tasks.length} tasks`}>
          {tasks.map((task) => {
            const isHighlighted = task.id === highlightedTaskId;
            return (
              <View
                key={task.id}
                style={[styles.entityRow, isHighlighted && styles.entityRowHighlighted]}
              >
                <View
                  style={[
                    styles.checkDot,
                    { backgroundColor: isHighlighted ? '#c78b00' : accent },
                  ]}
                />
                <View style={styles.entityCopy}>
                  <Text style={styles.entityTitle}>{task.title}</Text>
                  <Text style={styles.entityMeta}>
                    {formatTaskStatus(task.status)}
                    {task.assignee ? ` - ${task.assignee.displayName}` : ''}
                    {task.zone ? ` - ${task.zone.name}` : ''}
                  </Text>
                  {task.deadline && (
                    <Text style={styles.entityMeta}>Due {formatShortDate(task.deadline)}</Text>
                  )}
                </View>
                {task.status !== 'done' && (
                  <Pressable
                    accessibilityRole="button"
                    disabled={Boolean(controls.savingAction)}
                    onPress={() => controls.onCompleteTask(task.id)}
                    style={({ pressed }) => [
                      styles.smallButton,
                      pressed && styles.buttonPressed,
                      controls.savingAction && styles.buttonDisabled,
                    ]}
                  >
                    <Text style={styles.smallButtonText}>
                      {controls.savingAction === `complete-task:${task.id}` ? '...' : 'Done'}
                    </Text>
                  </Pressable>
                )}
              </View>
            );
          })}
        </SectionList>
      )}
    </View>
  );
}
