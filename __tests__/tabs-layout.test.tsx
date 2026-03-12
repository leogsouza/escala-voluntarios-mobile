import React from 'react';
import { render } from '@testing-library/react-native';

import TabsLayout from '@/app/(tabs)/_layout';

describe('TabsLayout', () => {
  it('shows exactly 2 tab labels', () => {
    const { getByText } = render(<TabsLayout />);

    expect(getByText('📅 Escala')).toBeTruthy();
    expect(getByText('🚫 Restrições')).toBeTruthy();
  });
});
