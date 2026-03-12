

import React, { useEffect, useRef, useState } from 'react';
import { Animated, StyleSheet, Text, View } from 'react-native';
import NetInfo from '@react-native-community/netinfo';

const BANNER_HEIGHT = 44;

export default function NetworkBanner() {
  const [offline, setOffline] = useState(false);
  const height = useRef(new Animated.Value(0)).current;

  useEffect(() => {
    const sub = NetInfo.addEventListener((state) => {
      const isOffline = !(state.isConnected && state.isInternetReachable !== false);
      setOffline(isOffline);
    });

    return () => sub();
  }, []);

  useEffect(() => {
    Animated.timing(height, {
      toValue: offline ? BANNER_HEIGHT : 0,
      duration: 220,
      useNativeDriver: false,
    }).start();
  }, [offline, height]);


  // Keep mounted always so animation can run smoothly
  return (
    <Animated.View style={[styles.container, { height }]}>
      <View style={styles.inner}>
        <Text style={styles.text}>Sem conexão — exibindo dados em cache</Text>
      </View>
    </Animated.View>
  );
}

const styles = StyleSheet.create({
  container: {
    overflow: 'hidden',
    backgroundColor: '#F59E0B', // amber
  },
  inner: {
    height: BANNER_HEIGHT,
    alignItems: 'center',
    justifyContent: 'center',
    paddingHorizontal: 12,
  },
  text: {
    color: '#1F2937',
    fontWeight: '600',
    fontSize: 13,
  },
});
