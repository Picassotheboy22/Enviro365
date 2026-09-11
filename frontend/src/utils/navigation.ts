/**
 * Where to send the user after they sign in. Only paths inside this app are accepted (e.g. "/history?investor=3"),
 * never another site ("https://evil.example", or "//evil.example" which browsers also treat as another site). This
 * stops the return address from being abused as an "open redirect".
 */
export function safeReturnPath(value: unknown): string {
  if (
    typeof value === 'string' &&
    value.startsWith('/') &&
    !value.startsWith('//') &&
    !value.startsWith('/\\')
  ) {
    return value
  }
  return '/'
}
