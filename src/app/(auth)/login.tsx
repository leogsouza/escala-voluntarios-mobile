'use client';

import React, { useState } from 'react';
import { StyleSheet, View } from 'react-native';
import { Button, Text, TextInput } from 'react-native-paper';

import { useAuth } from '@/lib/auth-context';
import { APIError } from '@/services/api';

export default function LoginScreen() {
  const { login } = useAuth();
  const [username, setUsername] = useState('');
  const [password, setPassword] = useState('');
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [passwordVisible, setPasswordVisible] = useState(false);

  async function handleLogin() {
    if (!username.trim() || !password.trim()) return;

    setLoading(true);
    setError(null);

    try {
      await login(username.trim(), password);
    } catch (err: unknown) {
      if (err instanceof APIError && err.status === 401) {
        setError('Usuário ou senha inválidos');
      } else if (err instanceof APIError) {
        setError('Usuário ou senha inválidos');
      } else {
        setError('Erro ao conectar. Tente novamente.');
      }
    } finally {
      setLoading(false);
    }
  }

  return (
    <View style={styles.container}>
      <Text variant="headlineMedium" style={styles.title}>
        Escala de Voluntários
      </Text>
      <Text variant="bodyMedium" style={styles.subtitle}>
        Faça login para continuar
      </Text>

      <View style={styles.form}>
        <TextInput
          label="Usuário"
          value={username}
          onChangeText={(text) => {
            setUsername(text);
            setError(null);
          }}
          autoCapitalize="none"
          autoCorrect={false}
          disabled={loading}
          testID="username-input"
          style={styles.input}
        />

        <TextInput
          label="Senha"
          value={password}
          onChangeText={(text) => {
            setPassword(text);
            setError(null);
          }}
          secureTextEntry={!passwordVisible}
          autoCapitalize="none"
          disabled={loading}
          testID="password-input"
          style={styles.input}
          right={
            <TextInput.Icon
              icon={passwordVisible ? 'eye-off' : 'eye'}
              onPress={() => setPasswordVisible((v) => !v)}
            />
          }
        />

        {error ? (
          <Text style={styles.errorText} testID="error-text">
            {error}
          </Text>
        ) : null}

        <Button
          mode="contained"
          onPress={handleLogin}
          loading={loading}
          disabled={loading || !username.trim() || !password.trim()}
          testID="login-button"
          style={styles.button}
          contentStyle={styles.buttonContent}
        >
          Entrar
        </Button>
      </View>
    </View>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    justifyContent: 'center',
    alignItems: 'stretch',
    padding: 24,
    backgroundColor: '#f8fafc',
  },
  title: {
    textAlign: 'center',
    fontWeight: '700',
    marginBottom: 8,
    color: '#1e3a5f',
  },
  subtitle: {
    textAlign: 'center',
    color: '#64748b',
    marginBottom: 32,
  },
  form: {
    gap: 16,
  },
  input: {
    backgroundColor: '#fff',
  },
  errorText: {
    color: '#dc2626',
    fontSize: 14,
    textAlign: 'center',
  },
  button: {
    marginTop: 8,
  },
  buttonContent: {
    paddingVertical: 6,
  },
});
