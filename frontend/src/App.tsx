import { Navigate, Route, Routes } from 'react-router'
import { AppLayout } from '@/components/layout/app-layout'
import { ComponentsShowcasePage } from '@/pages/components-showcase'
import { NotFoundPage } from '@/pages/not-found'
import { Pm01Page, Pm02Page, Pm03Page } from '@/pages/screens'

export function App() {
  return (
    <Routes>
      <Route element={<AppLayout />}>
        <Route index element={<Navigate to="/items" replace />} />
        <Route path="items" element={<Pm01Page />} />
        <Route path="stock" element={<Pm02Page />} />
        <Route path="work-orders" element={<Pm03Page />} />
        <Route path="dev/components" element={<ComponentsShowcasePage />} />
        <Route path="*" element={<NotFoundPage />} />
      </Route>
    </Routes>
  )
}
