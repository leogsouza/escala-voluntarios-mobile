import React, { useState, useEffect, useCallback, useMemo } from 'react';
import { View, StyleSheet, FlatList, RefreshControl } from 'react-native';
import { 
  Text, 
  FAB, 
  Searchbar, 
  ActivityIndicator, 
  Button, 
  Dialog, 
  Portal, 
  useTheme 
} from 'react-native-paper';
import { useRouter } from 'expo-router';
import { useRestrictionsPaginated, useDeleteRestriction } from '@/hooks/queries/useRestrictions';
import { Restriction } from '@/types/restriction';
import { RestrictionCard } from '@/components/RestrictionCard';

export default function RestrictionsScreen() {
  const router = useRouter();
  const theme = useTheme();
  
  // State
  const [searchQuery, setSearchQuery] = useState('');
  const [debouncedSearch, setDebouncedSearch] = useState('');
  const [page, setPage] = useState(1);
  const [items, setItems] = useState<Restriction[]>([]);
  const [deleteId, setDeleteId] = useState<number | null>(null);
  const [isRefreshing, setIsRefreshing] = useState(false);

  // Debounce search
  useEffect(() => {
    const handler = setTimeout(() => {
      setDebouncedSearch(searchQuery);
      setPage(1); // Reset to first page on new search
    }, 300);

    return () => clearTimeout(handler);
  }, [searchQuery]);

  // Query
  const { 
    data, 
    isLoading, 
    isError, 
    error, 
    refetch, 
    isFetching 
  } = useRestrictionsPaginated({
    page,
    page_size: 20,
    q: debouncedSearch
  });

  // Accumulate data for infinite scroll
  useEffect(() => {
    if (data?.data) {
      if (page === 1) {
        setItems(data.data);
      } else {
        setItems(prev => {
          // Avoid duplicates based on ID
          const existingIds = new Set(prev.map(item => item.id));
          const newItems = data.data.filter(item => item.id !== undefined && !existingIds.has(item.id));
          return [...prev, ...newItems];
        });
      }
    }
  }, [data, page]);

  // Delete mutation
  const deleteMutation = useDeleteRestriction();

  const handleDeleteConfirm = () => {
    if (deleteId !== null) {
      deleteMutation.mutate(deleteId, {
        onSuccess: () => {
          // Remove from local state immediately for better UX
          setItems(prev => prev.filter(item => item.id !== deleteId));
          setDeleteId(null);
          // Refetch to ensure sync (handled by hook invalidation mostly)
        }
      });
    }
  };

  const handleLoadMore = () => {
    if (!isFetching && data?.pagination && page < data.pagination.total_pages) {
      setPage(prev => prev + 1);
    }
  };

  const handleRefresh = async () => {
    setIsRefreshing(true);
    setPage(1);
    await refetch();
    setIsRefreshing(false);
  };

  const renderFooter = () => {
    if (!isFetching || items.length === 0) return null;
    return (
      <View style={styles.loaderFooter}>
        <ActivityIndicator animating size="small" />
      </View>
    );
  };

  const renderEmpty = () => {
    if (isLoading && page === 1) return null; // Initial loading handled by main loader
    if (isError) return null; // Error handled separately
    
    return (
      <View style={styles.emptyContainer}>
        <Text variant="bodyLarge" style={{ color: theme.colors.secondary }}>
          Nenhuma restrição encontrada
        </Text>
      </View>
    );
  };

  if (isLoading && page === 1 && !isRefreshing) {
    return (
      <View style={styles.centered}>
        <ActivityIndicator size="large" />
      </View>
    );
  }

  if (isError && page === 1 && items.length === 0) {
    return (
      <View style={styles.centered}>
        <Text variant="bodyLarge" style={{ marginBottom: 16, color: theme.colors.error }}>
          Erro ao carregar restrições
        </Text>
        <Button mode="contained" onPress={() => refetch()}>
          Tentar Novamente
        </Button>
      </View>
    );
  }

  return (
    <View style={styles.container}>
      <View style={styles.searchContainer}>
        <Searchbar
          placeholder="Buscar voluntário..."
          onChangeText={setSearchQuery}
          value={searchQuery}
          elevation={0}
          style={styles.searchBar}
        />
      </View>

      <FlatList
        data={items}
        keyExtractor={(item) => item.id?.toString() || Math.random().toString()}
        renderItem={({ item }) => (
          <RestrictionCard 
            restriction={item}
            onDelete={(id) => setDeleteId(id)}
            onPress={(id) => router.push(`/(tabs)/restrictions/${id}/edit`)}
          />
        )}
        onEndReached={handleLoadMore}
        onEndReachedThreshold={0.5}
        ListFooterComponent={renderFooter}
        ListEmptyComponent={renderEmpty}
        refreshControl={
          <RefreshControl refreshing={isRefreshing} onRefresh={handleRefresh} />
        }
        contentContainerStyle={items.length === 0 ? styles.listContentEmpty : styles.listContent}
      />

      <FAB
        icon="plus"
        style={[styles.fab, { backgroundColor: theme.colors.primary }]}
        color="white"
        onPress={() => router.push('/(tabs)/restrictions/new')}
      />

      <Portal>
        <Dialog visible={deleteId !== null} onDismiss={() => setDeleteId(null)}>
          <Dialog.Title>Confirmar Exclusão</Dialog.Title>
          <Dialog.Content>
            <Text variant="bodyMedium">
              Tem certeza que deseja excluir esta restrição?
            </Text>
          </Dialog.Content>
          <Dialog.Actions>
            <Button onPress={() => setDeleteId(null)}>Cancelar</Button>
            <Button onPress={handleDeleteConfirm} textColor={theme.colors.error}>
              Excluir
            </Button>
          </Dialog.Actions>
        </Dialog>
      </Portal>
    </View>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: '#f5f5f5',
  },
  centered: {
    flex: 1,
    justifyContent: 'center',
    alignItems: 'center',
    padding: 20,
  },
  searchContainer: {
    padding: 16,
    backgroundColor: 'white',
    borderBottomWidth: 1,
    borderBottomColor: '#e0e0e0',
  },
  searchBar: {
    backgroundColor: '#f5f5f5',
  },
  listContent: {
    paddingBottom: 80, // Space for FAB
  },
  listContentEmpty: {
    flexGrow: 1,
    justifyContent: 'center',
  },
  loaderFooter: {
    paddingVertical: 20,
    alignItems: 'center',
  },
  emptyContainer: {
    alignItems: 'center',
    justifyContent: 'center',
    padding: 40,
  },
  fab: {
    position: 'absolute',
    margin: 16,
    right: 0,
    bottom: 0,
  },
});
