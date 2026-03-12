import { Text, View } from 'react-native';

export default function ScheduleScreen() {
  return (
    <View style={{ flex: 1, justifyContent: 'center', alignItems: 'center', padding: 24 }}>
      <Text style={{ fontSize: 24, fontWeight: '700' }}>📅 Escala</Text>
      <Text style={{ marginTop: 8, fontSize: 16 }}>Calendário de serviços será exibido aqui.</Text>
    </View>
  );
}
