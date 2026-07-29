import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
// sunbird.org's own pairing — Rubik for headings, Open Sans for body — bundled
// rather than pulled from Google Fonts so the app works offline.
import '@fontsource/rubik/400.css'
import '@fontsource/rubik/500.css'
import '@fontsource/rubik/600.css'
import '@fontsource/rubik/700.css'
import '@fontsource/open-sans/400.css'
import '@fontsource/open-sans/500.css'
import '@fontsource/open-sans/600.css'
import App from './App'
import './styles.css'

createRoot(document.getElementById('root')!).render(
  <StrictMode>
    <App />
  </StrictMode>,
)
