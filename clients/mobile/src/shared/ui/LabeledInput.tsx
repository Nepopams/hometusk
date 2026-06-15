import { Text, TextInput, View } from 'react-native';

import { styles } from './styles';

type LabeledInputProps = {
  label: string;
  value: string;
  onChangeText: (value: string) => void;
  placeholder: string;
  editable: boolean;
  keyboardType?: 'default' | 'email-address';
  secureTextEntry?: boolean;
};

export function LabeledInput({
  label,
  value,
  onChangeText,
  placeholder,
  editable,
  keyboardType,
  secureTextEntry,
}: LabeledInputProps) {
  return (
    <View style={styles.inputGroup}>
      <Text style={styles.inputLabel}>{label}</Text>
      <TextInput
        autoCapitalize="none"
        editable={editable}
        keyboardType={keyboardType ?? 'default'}
        onChangeText={onChangeText}
        placeholder={placeholder}
        placeholderTextColor="#8a908d"
        secureTextEntry={secureTextEntry}
        style={styles.input}
        value={value}
      />
    </View>
  );
}
