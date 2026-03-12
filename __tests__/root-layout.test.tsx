import React from 'react';
import { render } from '@testing-library/react-native';

import RootLayout from '@/app/_layout';

describe('RootLayout', () => {
  it('renders main stack title', () => {
    const { getByText } = render(<RootLayout />);

    expect(getByText('Escala de Voluntários')).toBeTruthy();
  });
});
