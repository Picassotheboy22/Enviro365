import { describe, expect, it } from 'vitest'
import { toQueryString } from './client'

describe('toQueryString', () => {
  it('leaves out empty filters', () => {
    expect(toQueryString({})).toBe('')
    expect(toQueryString({ investorId: 3, from: '', to: undefined })).toBe('?investorId=3')
  })

  it('repeats the parameter for each status, which Spring binds to a list', () => {
    expect(toQueryString({ status: ['PENDING', 'APPROVED'] })).toBe('?status=PENDING&status=APPROVED')
  })
})
