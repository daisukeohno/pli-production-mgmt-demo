import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
import { Pm01Screen } from './screens/Pm01Screen'
import './styles.css'

createRoot(document.getElementById('root')!).render(
  <StrictMode>
    <Pm01Screen />
  </StrictMode>,
)
