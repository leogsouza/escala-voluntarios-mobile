declare module 'react-native-vector-icons/MaterialIcons' {
  import { TextProps } from 'react-native';
  import React from 'react';
  
  interface IconProps extends TextProps {
    name: string;
    size?: number;
    color?: string;
  }
  
  class Icon extends React.Component<IconProps> {}
  export default Icon;
}
