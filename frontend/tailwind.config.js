/** @type {import('tailwindcss').Config} */
export default {
  content: ['./index.html', './src/**/*.{ts,tsx}'],
  theme: {
    extend: {
      fontFamily: {
        sans: ['"Noto Sans JP"', '"Noto Sans CJK JP"', 'system-ui', 'sans-serif'],
      },
    },
  },
  plugins: [],
};
