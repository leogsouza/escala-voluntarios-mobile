import { Tabs } from 'expo-router';

export default function TabsLayout() {
  return (
    <Tabs
      screenOptions={{
        headerTitleAlign: 'center'
      }}
    >
      <Tabs.Screen
        name="schedule"
        options={{
          title: '📅 Escala'
        }}
      />
      <Tabs.Screen
        name="restrictions"
        options={{
          title: '🚫 Restrições'
        }}
      />
    </Tabs>
  );
}
