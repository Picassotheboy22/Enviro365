import { describe, expect, it } from 'vitest'
import { safeReturnPath } from './navigation'

describe('safeReturnPath', () => {
  it('keeps in-app paths, including their query string', () => {
    expect(safeReturnPath('/history')).toBe('/history')
    expect(safeReturnPath('/history?investor=3')).toBe('/history?investor=3')
  })

  it.each(['https://evil.example', '//evil.example', '/\\evil.example', 'history', '', null, undefined, 42])(
    'falls back to the overview for %j',
    (value) => {
      expect(safeReturnPath(value)).toBe('/')
    },
  )
})
