// @ts-check
import js from '@eslint/js'
import { defineConfig, globalIgnores } from 'eslint/config'
import prettier from 'eslint-config-prettier'
import reactHooks from 'eslint-plugin-react-hooks'
import { reactRefresh } from 'eslint-plugin-react-refresh'
import globals from 'globals'
import tseslint from 'typescript-eslint'

export default defineConfig([
  globalIgnores(['dist', 'coverage']),
  {
    files: ['**/*.{ts,tsx}'],
    extends: [
      js.configs.recommended,
      // "TypeChecked" rules use the TypeScript compiler, so they catch things like un-awaited promises.
      tseslint.configs.recommendedTypeChecked,
      reactHooks.configs.flat.recommended,
      reactRefresh.configs.vite(),
    ],
    languageOptions: {
      ecmaVersion: 2023,
      globals: globals.browser,
      parserOptions: { projectService: true, tsconfigRootDir: import.meta.dirname },
    },
  },
  {
    // shadcn/ui components are generated code: the shadcn CLI copies them in and replaces them on update. They get
    // the standard rules only, while our own code keeps the stricter type-aware rules. (Recharts' loose types would
    // otherwise fail chart.tsx, and hand edits to generated files would be lost on the next "shadcn add".)
    files: ['src/components/ui/**'],
    extends: [tseslint.configs.disableTypeChecked],
    // They also export helpers (e.g. buttonVariants) next to components.
    rules: { 'react-refresh/only-export-components': 'off' },
  },
  // Last: switches off formatting rules that would fight with Prettier (Prettier owns formatting).
  prettier,
])
