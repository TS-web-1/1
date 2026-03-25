import js from '@eslint/js'
import pluginVue from 'eslint-plugin-vue'
import prettierConfig from 'eslint-config-prettier'
import prettierPlugin from 'eslint-plugin-prettier'

export default [
  // JavaScript 推荐规则
  js.configs.recommended,
  
  // Vue 3 推荐规则
  ...pluginVue.configs['flat/recommended'],
  
  // Prettier 配置
  prettierConfig,
  {
    plugins: {
      prettier: prettierPlugin
    },
    rules: {
      'prettier/prettier': 'error'
    }
  },
  
  // 自定义规则
  {
    files: ['**/*.{js,vue}'],
    languageOptions: {
      ecmaVersion: 'latest',
      sourceType: 'module',
      globals: {
        // 浏览器环境
        window: 'readonly',
        document: 'readonly',
        console: 'readonly',
        localStorage: 'readonly',
        sessionStorage: 'readonly',
        fetch: 'readonly',
        alert: 'readonly',
        confirm: 'readonly',
        prompt: 'readonly',
        setTimeout: 'readonly',
        clearTimeout: 'readonly',
        setInterval: 'readonly',
        clearInterval: 'readonly',
        URLSearchParams: 'readonly',
        FileReader: 'readonly',
        // Node.js 环境
        process: 'readonly',
        __dirname: 'readonly',
        __filename: 'readonly'
      }
    },
    rules: {
      // Vue 规则
      'vue/multi-word-component-names': 'off',
      'vue/no-unused-vars': 'error',
      'vue/require-default-prop': 'off',
      'vue/require-prop-types': 'error',
      
      // JavaScript 规则
      'no-console': 'off',
      'no-debugger': 'error',
      'no-unused-vars': ['error', { 
        argsIgnorePattern: '^_',
        varsIgnorePattern: '^_',
        caughtErrorsIgnorePattern: '^_'
      }],
      'no-empty': ['error', { allowEmptyCatch: true }],
      'prefer-const': 'error',
      'no-var': 'error',
      'object-shorthand': 'error',
      'prefer-template': 'error',
      'arrow-spacing': 'error',
      'prefer-arrow-callback': 'error',
      'no-duplicate-imports': 'error'
    }
  },
  
  // 忽略文件
  {
    ignores: [
      'dist/**',
      'node_modules/**',
      '*.config.js',
      '.env*'
    ]
  }
]
