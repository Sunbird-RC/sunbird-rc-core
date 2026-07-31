import type { Config } from 'tailwindcss'

// Sunbird Spark design system — real tokens (project 9691223d-9eb7-449e-80d1-
// b921ec6f1970), not invented. `theme.extend` only, never top-level
// `theme.colors`/`theme.borderRadius` — that would delete Tailwind's stock
// scales (amber/emerald/sky/red etc.) before the semantic migration lands.
const config: Config = {
  content: ['./app/**/*.{ts,tsx}', './components/**/*.{ts,tsx}'],
  theme: {
    extend: {
      colors: {
        brick: 'var(--brick)',
        'brick-shade': 'var(--brick-shade)',
        ginger: 'var(--ginger)',
        ink: 'var(--ink)',
        'ink-shade': 'var(--ink-shade)',
        wave: 'var(--wave)',
        'wave-shade': 'var(--wave-shade)',
        sunflower: 'var(--sunflower)',
        'warm-yellow': 'var(--warm-yellow)',
        forest: 'var(--forest)',
        moss: 'var(--moss)',
        danger: {
          DEFAULT: 'var(--danger)',
          bg: 'var(--danger-bg)',
        },
        jamun: 'var(--jamun)',
        lavender: 'var(--lavender)',
        ivory: 'var(--ivory)',
        cream: 'var(--cream-bg)',
        charcoal: 'var(--charcoal)',
        obsidian: 'var(--obsidian)',
        tint: {
          97: 'var(--tint-97)',
          93: 'var(--tint-93)',
          89: 'var(--tint-89)',
          73: 'var(--tint-73)',
          66: 'var(--tint-66)',
          border: 'var(--tint-border)',
        },
        success: {
          DEFAULT: 'var(--forest)',
          bg: 'var(--success-bg)',
        },
        warning: {
          DEFAULT: 'var(--sunflower)',
          bg: 'var(--warning-bg)',
          text: 'var(--warning-text)',
        },
        info: 'var(--info)',
        gray: {
          50: 'var(--gray-50)',
          100: 'var(--gray-100)',
          200: 'var(--gray-200)',
          300: 'var(--gray-300)',
          400: 'var(--gray-400)',
          500: 'var(--gray-500)',
          700: 'var(--gray-700)',
          900: 'var(--gray-900)',
        },
      },
      borderRadius: {
        xxs: 'var(--r-xxs)',
        xs: 'var(--r-xs)',
        sm: 'var(--r-sm)',
        md: 'var(--r-md)',
        lg: 'var(--r-lg)',
        xl: 'var(--r-xl)',
      },
      boxShadow: {
        sm: 'var(--shadow-sm)',
        md: 'var(--shadow-md)',
        lg: 'var(--shadow-lg)',
      },
      fontFamily: {
        sans: ['var(--font-rubik)', 'system-ui', '-apple-system', 'Segoe UI', 'sans-serif'],
        serif: ['var(--font-rubik)', 'system-ui', 'sans-serif'],
      },
      transitionTimingFunction: {
        out: 'var(--ease-out)',
      },
    },
  },
  plugins: [],
}

export default config
