import { View, StyleSheet } from 'react-native';

interface CalendarDotProps {
  color: string;
  size?: number;
}

export function CalendarDot({ color, size = 6 }: CalendarDotProps) {
  return (
    <View
      style={[
        styles.dot,
        {
          backgroundColor: color,
          width: size,
          height: size,
          borderRadius: size / 2,
        },
      ]}
    />
  );
}

const styles = StyleSheet.create({
  dot: {
    marginHorizontal: 1,
  },
});
