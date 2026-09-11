import { describe, expect, it } from 'vitest'
import { IDLE_AFTER_MS, isActive } from './use-live-refresh'

describe('isActive', () => {
  it('counts the user as active until five minutes pass without any input', () => {
    const lastInput = 1_000_000

    expect(isActive(lastInput, lastInput)).toBe(true)
    expect(isActive(lastInput, lastInput + IDLE_AFTER_MS - 1)).toBe(true)
    expect(isActive(lastInput, lastInput + IDLE_AFTER_MS)).toBe(false)
  })
})
