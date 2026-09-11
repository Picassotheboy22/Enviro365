// Typed environment variables (see https://vite.dev/guide/env-and-mode).
interface ImportMetaEnv {
  /** Absolute URL of the API, only needed when it is not reached through the Vite dev proxy. */
  readonly VITE_API_BASE_URL?: string
}
