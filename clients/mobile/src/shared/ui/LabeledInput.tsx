import { Text, TextInput, View } from 'react-native';

import { styles } from './styles';

type LabeledInputProps = {
  label: string;
  value: string;
  onChangeText: (value: string) => void;
  placeholder: string;
  editable: boolean;
  keyboardType?: 'default' | 'email-address';
  multiline?: boolean;
  numberOfLines?: number;
  secureTextEntry?: boolean;
};

export function LabeledInput({
  label,
  value,
  onChangeText,
  placeholder,
  editable,
  keyboardType,
  multiline,
  numberOfLines,
  secureTextEntry,
}: LabeledInputProps) {
  return (
    <View style={styles.inputGroup}>
      <Text style={styles.inputLabel}>{label}</Text>
      <TextInput
        autoCapitalize="none"
        editable={editable}
        keyboardType={keyboardType ?? 'default'}
        multiline={multiline}
        numberOfLines={numberOfLines}
        onChangeText={onChangeText}
        placeholder={placeholder}
        placeholderTextColor="#8a908d"
        secureTextEntry={secureTextEntry}
        style={[styles.input, multiline && styles.inputMultiline]}
        textAlignVertical={multiline ? 'top' : 'center'}
        value={value}
      />
    </View>
  );
}
