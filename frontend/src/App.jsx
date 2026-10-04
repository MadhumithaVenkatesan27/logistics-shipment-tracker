import { Link, Navigate, Route, Routes } from 'react-router-dom';
import ShipmentListPage from './pages/ShipmentListPage.jsx';
import ShipmentFormPage from './pages/ShipmentFormPage.jsx';
import ShipmentDetailPage from './pages/ShipmentDetailPage.jsx';

export default function App() {
  return (
    <>
      <header className="app-header">
        <Link to="/shipments" className="brand">
          <span className="brand-mark" aria-hidden="true">▤</span> Shipment Tracker
        </Link>
      </header>
      <main className="container">
        <Routes>
          <Route path="/" element={<Navigate to="/shipments" replace />} />
          <Route path="/shipments" element={<ShipmentListPage />} />
          <Route path="/shipments/new" element={<ShipmentFormPage />} />
          <Route path="/shipments/:id" element={<ShipmentDetailPage />} />
          <Route path="/shipments/:id/edit" element={<ShipmentFormPage />} />
          <Route path="*" element={<p className="state-box">Page not found. <Link to="/shipments">Back to shipments</Link></p>} />
        </Routes>
      </main>
    </>
  );
}
