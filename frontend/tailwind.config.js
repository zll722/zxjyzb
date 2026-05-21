/** @type {import('tailwindcss').Config} */

export default {
  darkMode: 'class',
  content: ['./index.html', './src/**/*.{js,ts,vue}'],
  theme: {
    container: { center: true, padding: '1rem' },
    // ── Type scale: 12 / 14 / 16 / 20 / 24 / 32 / 40 ──
    fontSize: {
      xs:   ['12px', { lineHeight: '1.6' }],
      sm:   ['14px', { lineHeight: '1.6' }],
      base: ['16px', { lineHeight: '1.6' }],
      xl:   ['20px', { lineHeight: '1.3' }],
      '2xl':['24px', { lineHeight: '1.3' }],
      '3xl':['32px', { lineHeight: '1.3' }],
      '4xl':['40px', { lineHeight: '1.2' }],
    },
    extend: {
      colors: {
        // Primary — unified across all three portals
        primary: {
          50:  '#F0F0FE',
          100: '#E0E0FD',
          200: '#C4C4FB',
          300: '#A3A3F8',
          400: '#8181F6',
          500: '#6E6EF8',
          600: '#5B5BF5',
          700: '#4747D4',
          800: '#3535A8',
          900: '#242478',
        },
        // Neutral
        neutral: {
          0:   '#FFFFFF',
          50:  '#F8FAFC',
          100: '#F1F5F9',
          200: '#E2E8F0',
          300: '#CBD5E1',
          400: '#94A3B8',
          500: '#64748B',
          600: '#475569',
          700: '#334155',
          800: '#1E293B',
          900: '#0F172A',
        },
        // Semantic — status badges only
        success: { DEFAULT: '#10B981', bg: '#ECFDF5', text: '#065F46' },
        warning: { DEFAULT: '#F59E0B', bg: '#FFFBEB', text: '#92400E' },
        danger:  { DEFAULT: '#EF4444', bg: '#FEF2F2', text: '#991B1B' },
        info:    { DEFAULT: '#3B82F6', bg: '#EFF6FF', text: '#1D4ED8' },
        // Convenience aliases
        surface: '#FFFFFF',
        bg:      '#F8FAFC',
        muted:   '#F1F5F9',
        border:  '#E2E8F0',
        // Dark mode (LiveRoom)
        dark: {
          bg:      '#020617',
          surface: '#0F172A',
          border:  'rgba(255,255,255,0.1)',
        },
        // Legacy brand alias — maps to primary for backward compat
        brand: {
          50:  '#F0F0FE',
          100: '#E0E0FD',
          200: '#C4C4FB',
          300: '#A3A3F8',
          400: '#8181F6',
          500: '#6E6EF8',
          600: '#5B5BF5',
          700: '#4747D4',
          800: '#3535A8',
          900: '#242478',
        },
      },
      fontFamily: {
        sans: ["'PingFang SC'", "'Microsoft YaHei'", "'Noto Sans SC'", 'ui-sans-serif', 'system-ui', 'sans-serif'],
      },
      fontWeight: {
        normal: '400',
        medium: '500',
        semibold: '600',
        // 700 not used in this design system
      },
      spacing: {
        // 4px-grid steps already covered by Tailwind (1=4px, 2=8px, 3=12px, 4=16px, 6=24px, 8=32px, 12=48px, 16=64px)
      },
      borderRadius: {
        sm:  '4px',
        DEFAULT: '6px',
        md:  '8px',    // button / input
        lg:  '12px',   // card
        xl:  '16px',   // modal
        '2xl': '20px',
        full: '9999px',
      },
      boxShadow: {
        sm:    '0 1px 2px rgba(15,23,42,0.04)',
        card:  '0 1px 2px rgba(15,23,42,0.04)',           // alias
        md:    '0 4px 12px rgba(15,23,42,0.06)',
        'card-hover': '0 4px 12px rgba(15,23,42,0.06)',   // alias
        lg:    '0 12px 32px rgba(15,23,42,0.10)',
        modal: '0 12px 32px rgba(15,23,42,0.10)',          // alias
        // NO colored shadows, NO inset shadows
      },
      transitionTimingFunction: {
        standard: 'cubic-bezier(0.4, 0, 0.2, 1)',
      },
      transitionDuration: {
        DEFAULT: '200ms',
      },
      keyframes: {
        'live-pulse': {
          '0%, 100%': { opacity: '1', transform: 'scale(1)' },
          '50%':       { opacity: '0.6', transform: 'scale(1.4)' },
        },
        'float-up': {
          '0%, 100%': { transform: 'translateY(0px)' },
          '50%':       { transform: 'translateY(-4px)' },
        },
        'fade-in': {
          from: { opacity: '0', transform: 'translateY(8px)' },
          to:   { opacity: '1', transform: 'translateY(0)' },
        },
      },
      animation: {
        'live-pulse': 'live-pulse 1.4s ease-in-out infinite',
        'float-up':   'float-up 3s ease-in-out infinite',
        'fade-in':    'fade-in 0.3s cubic-bezier(0.4,0,0.2,1) both',
      },
    },
  },
  plugins: [],
}
