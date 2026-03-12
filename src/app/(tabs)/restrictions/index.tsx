import { Text, View } from 'react-native';

export default function RestrictionsScreen() {
  return (
    <View style={{ flex: 1, justifyContent: 'center', alignItems: 'center', padding: 24 }}>
      <Text style={{ fontSize: 24, fontWeight: '700' }}>🚫 Restrições</Text>
      <Text style={{ marginTop: 8, fontSize: 16 }}>Lista de restrições será exibida aqui.</Text>
    </View>
  );
}
