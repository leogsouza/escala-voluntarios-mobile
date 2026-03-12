import React from 'react';
import { render } from '@testing-library/react-native';
import { VolunteerAssignmentCard } from '@/components/VolunteerAssignmentCard';
import { Assignment } from '@/types/schedule';

// Mock netinfo as requested
jest.mock('@react-native-community/netinfo', () => ({
  addEventListener: jest.fn(() => jest.fn()),
  fetch: jest.fn().mockResolvedValue({ isConnected: true, isInternetReachable: true }),
}));

describe('VolunteerAssignmentCard', () => {
  const mockAssignment: Assignment = {
    id: 1,
    event_id: 1,
    volunteer_id: 1,
    position_id: 1,
    status: 'PUBLISHED',
    volunteer: {
      id: 1,
      name: 'Leonardo',
      full_name: 'Leonardo Souza',
      active: true,
      main_role: { id: 1, name: 'Guitarra' }
    },
    position: {
      id: 1,
      name: 'Guitarra',
      role_id: 1,
    }
  };

  it('renders correctly with PUBLISHED status', () => {
    const { getByText } = render(<VolunteerAssignmentCard assignment={mockAssignment} />);
    
    // Check for names
    expect(getByText('Leonardo Souza')).toBeTruthy();
    expect(getByText('Guitarra')).toBeTruthy();
    
    // Check for status label
    expect(getByText('Confirmado')).toBeTruthy();
  });

  it('renders correctly with PENDING status', () => {
    const pendingAssignment: Assignment = { ...mockAssignment, status: 'PENDING' };
    const { getByText } = render(<VolunteerAssignmentCard assignment={pendingAssignment} />);
    
    expect(getByText('Pendente')).toBeTruthy();
  });

  it('renders correctly with CANCELLED status', () => {
    const cancelledAssignment: Assignment = { ...mockAssignment, status: 'CANCELLED' };
    const { getByText } = render(<VolunteerAssignmentCard assignment={cancelledAssignment} />);
    
    expect(getByText('Cancelado')).toBeTruthy();
  });

  it('renders correctly with unknown status', () => {
    const unknownAssignment: Assignment = { ...mockAssignment, status: 'UNKNOWN' };
    const { getByText } = render(<VolunteerAssignmentCard assignment={unknownAssignment} />);
    
    expect(getByText('UNKNOWN')).toBeTruthy();
  });

  it('handles missing volunteer name gracefully', () => {
    const noNameAssignment: Assignment = { 
      ...mockAssignment, 
      volunteer: { 
        ...mockAssignment.volunteer!, 
        full_name: '', 
        name: '' 
      } 
    };
    
    const { getByText } = render(<VolunteerAssignmentCard assignment={noNameAssignment} />);
    expect(getByText('Voluntário desconhecido')).toBeTruthy();
  });
});
