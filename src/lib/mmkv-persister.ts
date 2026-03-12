import { MMKV } from 'react-native-mmkv';

// Minimal type surface compatible with @tanstack/react-query-persist-client.
// We intentionally avoid importing from that package because it is not
// currently installed in this repo (per package.json).
export type PersistedClient = unknown;
export type Persister = {
  persistClient: (client: PersistedClient) => Promise<void>;
  restoreClient: () => Promise<PersistedClient | undefined>;
  removeClient: () => Promise<void>;
};

const CACHE_KEY = 'react-query-cache';

export function createMMKVPersister(): Persister {
  // MUST: id 'react-query-cache'
  const storage = new MMKV({ id: CACHE_KEY });

  return {
    async persistClient(client) {
      try {
        storage.set(CACHE_KEY, JSON.stringify(client));
      } catch {
        // Ignore persistence errors
      }
    },

    async restoreClient() {
      try {
        const cached = storage.getString(CACHE_KEY);
        if (!cached) return undefined;
        return JSON.parse(cached) as PersistedClient;
      } catch {
        return undefined;
      }
    },

    async removeClient() {
      try {
        storage.delete(CACHE_KEY);
      } catch {
        // ignore
      }
    },
  };
}
