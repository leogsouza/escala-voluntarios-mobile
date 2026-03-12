import { QueryClient } from '@tanstack/react-query';

import { createMMKVPersister } from '@/lib/mmkv-persister';

export const queryClient = new QueryClient({
  defaultOptions: {
    queries: {
      staleTime: 5 * 60 * 1000, // 5 minutes
      gcTime: 30 * 60 * 1000, // 30 minutes
      retry: 2,
      refetchOnWindowFocus: true,
    },
  },
});

// Export persister instance (used by PersistQueryClientProvider when available)
export const mmkvPersister = createMMKVPersister();
