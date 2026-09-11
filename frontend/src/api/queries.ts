import { keepPreviousData, skipToken, useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { api } from '@/api/client'
import type { WithdrawalFilter, WithdrawalRequest } from '@/types'

/*
 * Server data lives in TanStack Query. It caches responses, tracks loading and error states, and refetches after
 * changes, so components don't need hand-written useEffect + useState fetching. A query key identifies a piece of
 * cached data; invalidating a key makes every screen that shows that data fetch it again.
 */
export const queryKeys = {
  dashboard: ['dashboard'] as const,
  investors: ['investors'] as const,
  portfolios: ['portfolio'] as const,
  portfolio: (investorId: number | null) => ['portfolio', investorId] as const,
  withdrawals: ['withdrawals'] as const,
  withdrawalList: (filter: WithdrawalFilter | null) => ['withdrawals', filter] as const,
}

/** Staff only: dashboard statistics, calculated by the server. */
export function useDashboard() {
  return useQuery({ queryKey: queryKeys.dashboard, queryFn: () => api.getDashboard() })
}

/** Staff only: every client with their totals (Clients page, top clients, the client filter). */
export function useInvestors(enabled: boolean) {
  return useQuery({ queryKey: queryKeys.investors, queryFn: () => api.listInvestors(), enabled })
}

/** skipToken = "don't fetch yet" (e.g. while no single client is chosen), in a type-safe way. */
export function usePortfolio(investorId: number | null) {
  return useQuery({
    queryKey: queryKeys.portfolio(investorId),
    queryFn: investorId === null ? skipToken : () => api.getPortfolio(investorId),
  })
}

export function useWithdrawals(filter: WithdrawalFilter | null) {
  return useQuery({
    queryKey: queryKeys.withdrawalList(filter),
    queryFn: filter === null ? skipToken : () => api.listWithdrawals(filter),
    // While new filter results load, keep showing the previous rows instead of flashing an empty table.
    placeholderData: keepPreviousData,
  })
}

export function useCreateWithdrawal() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (request: WithdrawalRequest) => api.createWithdrawal(request),
    onSuccess: async () => {
      // Balances, history and totals changed on the server: refetch everything that shows them.
      await Promise.all([
        queryClient.invalidateQueries({ queryKey: queryKeys.portfolios }),
        queryClient.invalidateQueries({ queryKey: queryKeys.withdrawals }),
        queryClient.invalidateQueries({ queryKey: queryKeys.investors }),
        queryClient.invalidateQueries({ queryKey: queryKeys.dashboard }),
      ])
    },
  })
}
