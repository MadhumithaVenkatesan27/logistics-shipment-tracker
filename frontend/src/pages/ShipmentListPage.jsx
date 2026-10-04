import { useCallback, useEffect, useState } from 'react';
import { Link, useLocation, useNavigate } from 'react-router-dom';
import { deleteShipment, getErrorMessage, getShipments } from '../services/shipmentService.js';
import { STATUSES, STATUS_LABELS } from '../utils/constants.js';
import StatusBadge from '../components/StatusBadge.jsx';
import ConfirmDialog from '../components/ConfirmDialog.jsx';

export default function ShipmentListPage() {
  const location = useLocation();
  const navigate = useNavigate();

  const [shipments, setShipments] = useState([]);
  const [statusFilter, setStatusFilter] = useState('');
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [toDelete, setToDelete] = useState(null);
  const [deleting, setDeleting] = useState(false);
  const [notice, setNotice] = useState(location.state?.message || '');

  const load = useCallback(async () => {
    setLoading(true);
    setError('');
    try {
      setShipments(await getShipments(statusFilter));
    } catch (err) {
      setShipments([]);
      setError(getErrorMessage(err));
    } finally {
      setLoading(false);
    }
  }, [statusFilter]);

  useEffect(() => {
    load();
  }, [load]);

  // Clear the one-time success message from router state so a refresh does not show it again
  useEffect(() => {
    if (location.state?.message) navigate(location.pathname, { replace: true, state: null });
  }, [location, navigate]);

  async function confirmDelete() {
    setDeleting(true);
    try {
      await deleteShipment(toDelete.id);
      setNotice(`Shipment ${toDelete.trackingNumber} was deleted.`);
      setToDelete(null);
      await load();
    } catch (err) {
      setToDelete(null);
      setError(getErrorMessage(err));
    } finally {
      setDeleting(false);
    }
  }

  return (
    <section>
      <div className="page-head">
        <h1>Shipments</h1>
        <Link to="/shipments/new" className="btn btn-primary">+ Add Shipment</Link>
      </div>

      <div className="toolbar">
        <label htmlFor="status-filter">Filter by status</label>
        <select id="status-filter" value={statusFilter} onChange={(e) => setStatusFilter(e.target.value)}>
          <option value="">All</option>
          {STATUSES.map((s) => (
            <option key={s} value={s}>{STATUS_LABELS[s]}</option>
          ))}
        </select>
      </div>

      {notice && <div className="alert alert-success" role="status">{notice}</div>}
      {error && <div className="alert alert-error" role="alert">{error}</div>}

      {loading ? (
        <p className="state-box" role="status">Loading shipments…</p>
      ) : !error && shipments.length === 0 ? (
        <p className="state-box">
          {statusFilter
            ? `No shipments match the status "${STATUS_LABELS[statusFilter]}".`
            : 'No shipments yet. Click "Add Shipment" to create the first one.'}
        </p>
      ) : (
        shipments.length > 0 && (
          <div className="table-wrap">
            <table>
              <thead>
                <tr>
                  <th>Tracking #</th>
                  <th>Origin</th>
                  <th>Destination</th>
                  <th>Carrier</th>
                  <th>Expected delivery</th>
                  <th>Status</th>
                  <th>Actions</th>
                </tr>
              </thead>
              <tbody>
                {shipments.map((s) => (
                  <tr key={s.id}>
                    <td data-label="Tracking #"><strong>{s.trackingNumber}</strong></td>
                    <td data-label="Origin">{s.origin}</td>
                    <td data-label="Destination">{s.destination}</td>
                    <td data-label="Carrier">{s.carrier}</td>
                    <td data-label="Expected delivery">{s.expectedDeliveryDate}</td>
                    <td data-label="Status"><StatusBadge status={s.status} /></td>
                    <td data-label="Actions" className="row-actions">
                      <Link to={`/shipments/${s.id}`} className="btn btn-sm">View</Link>
                      <Link to={`/shipments/${s.id}/edit`} className="btn btn-sm">Edit</Link>
                      <button type="button" className="btn btn-sm btn-danger-outline" onClick={() => setToDelete(s)}>
                        Delete
                      </button>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )
      )}

      {toDelete && (
        <ConfirmDialog
          title="Delete shipment?"
          message={`Shipment ${toDelete.trackingNumber} will be permanently deleted. This cannot be undone.`}
          busy={deleting}
          onConfirm={confirmDelete}
          onCancel={() => setToDelete(null)}
        />
      )}
    </section>
  );
}
