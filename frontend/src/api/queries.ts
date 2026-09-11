import {
  keepPreviousData,
  skipToken,
  useMutation,
  useQuery,
  useQueryClient,
  type QueryClient,
} from '@tanstack/react-query'
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

/**
 * Any change to a notice can move money (a hold placed or released, a balance deducted) and changes the history, the
 * client totals and the dashboard, so every screen that shows them fetches again.
 */
function refreshNoticeData(queryClient: QueryClient) {
  return Promise.all([
    queryClient.invalidateQueries({ queryKey: queryKeys.portfolios }),
    queryClient.invalidateQueries({ queryKey: queryKeys.withdrawals }),
    queryClient.invalidateQueries({ queryKey: queryKeys.investors }),
    queryClient.invalidateQueries({ queryKey: queryKeys.dashboard }),
  ])
}

export function useCreateWithdrawal() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (request: WithdrawalRequest) => api.createWithdrawal(request),
    onSuccess: () => refreshNoticeData(queryClient),
  })
}

/** One step of the notice workflow. Only a rejection carries extra data: the reason the investor will see. */
export type NoticeChange =
  { action: 'approve' | 'pay' | 'cancel'; id: number } | { action: 'reject'; id: number; reason: string }

export function useNoticeAction() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (change: NoticeChange) => {
      switch (change.action) {
        case 'approve':
          return api.approveNotice(change.id)
        case 'reject':
          return api.rejectNotice(change.id, change.reason)
        case 'pay':
          return api.payNotice(change.id)
        case 'cancel':
          return api.cancelNotice(change.id)
      }
    },
    // Refresh after errors as well: a 409 means someone else changed the notice first, so show its current state.
    onSettled: () => refreshNoticeData(queryClient),
  })
}
