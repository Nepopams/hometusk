import { StatusBar } from 'expo-status-bar';
import { ActivityIndicator, Pressable, SafeAreaView, ScrollView, Text, View } from 'react-native';

import type { AuthMode } from '../../app/types';
import { LabeledInput } from '../../shared/ui/LabeledInput';
import { styles } from '../../shared/ui/styles';

type AuthScreenProps = {
  authMode: AuthMode;
  email: string;
  password: string;
  displayName: string;
  error: string | null;
  isSubmitting: boolean;
  onChangeAuthMode: (mode: AuthMode) => void;
  onChangeDisplayName: (value: string) => void;
  onChangeEmail: (value: string) => void;
  onChangePassword: (value: string) => void;
  onSubmit: () => void;
};

export function AuthScreen({
  authMode,
  email,
  password,
  displayName,
  error,
  isSubmitting,
  onChangeAuthMode,
  onChangeDisplayName,
  onChangeEmail,
  onChangePassword,
  onSubmit,
}: AuthScreenProps) {
  const isRegister = authMode === 'register';

  return (
    <SafeAreaView style={styles.safeArea}>
      <StatusBar style="auto" />
      <ScrollView contentContainerStyle={styles.authShell} keyboardShouldPersistTaps="handled">
        <View style={styles.authHeader}>
          <Text style={styles.eyebrow}>HomeTusk</Text>
          <Text style={styles.authTitle}>Home in your pocket</Text>
          <Text style={styles.subtitle}>Sign in to load households, tasks, shopping, and commands.</Text>
        </View>

        <View style={styles.modeSwitch}>
          <Pressable
            accessibilityRole="button"
            onPress={() => onChangeAuthMode('login')}
            style={[styles.modeButton, authMode === 'login' && styles.modeButtonActive]}
          >
            <Text style={[styles.modeText, authMode === 'login' && styles.modeTextActive]}>
              Login
            </Text>
          </Pressable>
          <Pressable
            accessibilityRole="button"
            onPress={() => onChangeAuthMode('register')}
            style={[styles.modeButton, authMode === 'register' && styles.modeButtonActive]}
          >
            <Text style={[styles.modeText, authMode === 'register' && styles.modeTextActive]}>
              Register
            </Text>
          </Pressable>
        </View>

        <View style={styles.form}>
          {isRegister && (
            <LabeledInput
              label="Name"
              value={displayName}
              onChangeText={onChangeDisplayName}
              placeholder="Alice Test"
              editable={!isSubmitting}
            />
          )}
          <LabeledInput
            label="Email"
            value={email}
            onChangeText={onChangeEmail}
            placeholder="alice@example.com"
            editable={!isSubmitting}
            keyboardType="email-address"
          />
          <LabeledInput
            label="Password"
            value={password}
            onChangeText={onChangePassword}
            placeholder="Password"
            editable={!isSubmitting}
            secureTextEntry
          />

          {error && (
            <View style={styles.errorBox}>
              <Text style={styles.errorText}>{error}</Text>
            </View>
          )}

          <Pressable
            accessibilityRole="button"
            disabled={isSubmitting}
            onPress={onSubmit}
            style={({ pressed }) => [
              styles.primaryButton,
              pressed && styles.buttonPressed,
              isSubmitting && styles.buttonDisabled,
            ]}
          >
            {isSubmitting ? (
              <ActivityIndicator color="#ffffff" />
            ) : (
              <Text style={styles.primaryButtonText}>{isRegister ? 'Create account' : 'Login'}</Text>
            )}
          </Pressable>
        </View>
      </ScrollView>
    </SafeAreaView>
  );
}
