import { Pressable, ScrollView, Text, View, type ColorValue } from 'react-native';

import type { ShoppingItem, ShoppingList } from '../../api/types';
import type { MutationControls } from '../../app/types';
import { shortId } from '../../shared/format/ids';
import { EmptyState } from '../../shared/ui/EmptyState';
import { EmptyRow } from '../../shared/ui/InfoRow';
import { LabeledInput } from '../../shared/ui/LabeledInput';
import { SectionList } from '../../shared/ui/SectionList';
import { styles } from '../../shared/ui/styles';

export function ShoppingSurface({
  lists,
  items,
  accent,
  controls,
}: {
  lists: ShoppingList[];
  items: ShoppingItem[];
  accent: ColorValue;
  controls: MutationControls;
}) {
  if (lists.length === 0) {
    return (
      <EmptyState
        body="Создай список через web или команду, затем добавляй товары здесь."
        mascotMood="idle"
        title="Списков покупок пока нет"
      />
    );
  }

  return (
    <View style={styles.section}>
      <View style={styles.formPanel}>
        <ScrollView
          contentContainerStyle={styles.shoppingListChips}
          horizontal
          showsHorizontalScrollIndicator={false}
        >
          {lists.map((list) => {
            const selected = list.id === controls.selectedShoppingListId;
            return (
              <Pressable
                accessibilityRole="button"
                disabled={Boolean(controls.savingAction)}
                key={list.id}
                onPress={() => controls.onSelectShoppingList(list.id)}
                style={[styles.listChip, selected && styles.listChipActive]}
              >
                <Text style={[styles.listChipText, selected && styles.listChipTextActive]}>
                  {list.name}
                </Text>
              </Pressable>
            );
          })}
        </ScrollView>
        <LabeledInput
          editable={!controls.savingAction}
          label="Item"
          onChangeText={controls.onChangeShoppingItemName}
          placeholder="Milk"
          value={controls.shoppingItemName}
        />
        <Pressable
          accessibilityRole="button"
          disabled={Boolean(controls.savingAction)}
          onPress={controls.onAddShoppingItem}
          style={({ pressed }) => [
            styles.primaryButton,
            pressed && styles.buttonPressed,
            controls.savingAction && styles.buttonDisabled,
          ]}
        >
          <Text style={styles.primaryButtonText}>
            {controls.savingAction === 'add-shopping-item' ? 'Adding...' : 'Add item'}
          </Text>
        </Pressable>
      </View>

      <SectionList title={`${lists.length} shopping lists`}>
        {lists.map((list) => {
          const listItems = items.filter((item) => item.listId === list.id);
          return (
            <View key={list.id} style={styles.listBlock}>
              <View style={styles.listHeader}>
                <Text style={styles.entityTitle}>{list.name}</Text>
                <Text style={styles.entityMeta}>{list.unpurchasedCount} to buy</Text>
              </View>
              {listItems.length === 0 ? (
                <EmptyRow accent={accent} title="No items in this list" />
              ) : (
                listItems.slice(0, 6).map((item) => (
                  <View key={item.id} style={styles.entityRow}>
                    <View style={[styles.checkDot, { backgroundColor: accent }]} />
                    <View style={styles.entityCopy}>
                      <Text style={styles.entityTitle}>{item.name}</Text>
                      <Text style={styles.entityMeta}>
                        {item.purchased ? 'Purchased' : item.quantity ? `Qty ${item.quantity}` : 'To buy'}
                        {item.linkedTaskId ? ` - linked ${shortId(item.linkedTaskId)}` : ''}
                      </Text>
                    </View>
                    <View style={styles.rowActions}>
                      {!item.purchased && (
                        <Pressable
                          accessibilityRole="button"
                          disabled={Boolean(controls.savingAction)}
                          onPress={() => controls.onMarkPurchased(item.id)}
                          style={({ pressed }) => [
                            styles.smallButton,
                            pressed && styles.buttonPressed,
                            controls.savingAction && styles.buttonDisabled,
                          ]}
                        >
                          <Text style={styles.smallButtonText}>
                            {controls.savingAction === `purchase-item:${item.id}` ? '...' : 'Buy'}
                          </Text>
                        </Pressable>
                      )}
                      <Pressable
                        accessibilityRole="button"
                        disabled={Boolean(controls.savingAction)}
                        onPress={() => controls.onDeleteShoppingItem(item.id)}
                        style={({ pressed }) => [
                          styles.smallDangerButton,
                          pressed && styles.buttonPressed,
                          controls.savingAction && styles.buttonDisabled,
                        ]}
                      >
                        <Text style={styles.smallDangerText}>
                          {controls.savingAction === `delete-item:${item.id}` ? '...' : 'Delete'}
                        </Text>
                      </Pressable>
                    </View>
                  </View>
                ))
              )}
            </View>
          );
        })}
      </SectionList>
    </View>
  );
}
