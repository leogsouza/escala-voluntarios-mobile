require('@testing-library/jest-native/extend-expect');

global.fetch = jest.fn();

jest.mock('expo-router', () => {
  const React = require('react');
  const { View, Text } = require('react-native');

  const Stack = ({ children }) => React.createElement(View, null, children);
  Stack.Screen = ({ name, options }) => React.createElement(Text, null, options?.title ?? name);

  const Tabs = ({ children }) => React.createElement(View, null, children);
  Tabs.Screen = ({ name, options }) => React.createElement(Text, null, options?.title ?? name);

  const Redirect = ({ href }) => React.createElement(Text, null, href);

  return {
    Stack,
    Tabs,
    Redirect
  };
});
