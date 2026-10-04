import { useEffect, useState } from 'react';
import { Link, useLocation, useNavigate, useParams } from 'react-router-dom';
import { deleteShipment, getErrorMessage, getShipment } from '../services/shipmentService.js';
import StatusBadge from '../components/StatusBadge.jsx';
import ConfirmDialog from '../components/ConfirmDialog.jsx';

function formatDateTime(value) {
  return value ? new Date(value).toLocaleString() : '—';
}

export default function ShipmentDetailPage() {
  const { id } = useParams();
  const location = useLocation();
  const navigate = useNavigate();

  const [shipment, setShipment] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [confirming, setConfirming] = useState(false);
  const [deleting, setDeleting] = useState(false);
  const [notice] = useState(location.state?.message || '');

  useEffect(() => {
    let cancelled = false;
    (async () => {
      try {
        const data = await getShipment(id);
        if (!cancelled) setShipment(data);
      } catch (err) {
        if (!cancelled) setError(getErrorMessage(err));
      } finally {
        if (!cancelled) setLoading(false);
      }
    })();
    return () => { cancelled = true; };
  }, [id]);

  async function confirmDelete() {
    setDeleting(true);
    try {
      await deleteShipment(id);
      navigate('/shipments', { state: { message: `Shipment ${shipment.trackingNumber} was deleted.` } });
    } catch (err) {
      setConfirming(false);
      setError(getErrorMessage(err));
      setDeleting(false);
    }
  }

  if (loading) return <p className="state-box" role="status">Loading shipment…</p>;

  if (!shipment) {
    return (
      <div>
        <div className="alert alert-error" role="alert">{error || 'Shipment not found.'}</div>
        <Link to="/shipments" className="btn">Back to shipments</Link>
      </div>
    );
  }

  const rows = [
    ['Tracking number', shipment.trackingNumber],
    ['Status', <StatusBadge key="s" status={shipment.status} />],
    ['Sender', shipment.senderName],
    ['Receiver', shipment.receiverName],
    ['Origin', shipment.origin],
    ['Destination', shipment.destination],
    ['Carrier', shipment.carrier],
    ['Expected delivery date', shipment.expectedDeliveryDate],
    ['Created at', formatDateTime(shipment.createdAt)],
    ['Last updated', formatDateTime(shipment.updatedAt)],
  ];

  return (
    <section>
      <div className="page-head">
        <h1>Shipment {shipment.trackingNumber}</h1>
        <div className="row-actions">
          <Link to={`/shipments/${shipment.id}/edit`} className="btn">Edit</Link>
          <button type="button" className="btn btn-danger-outline" onClick={() => setConfirming(true)}>Delete</button>
        </div>
      </div>

      {notice && <div className="alert alert-success" role="status">{notice}</div>}
      {error && <div className="alert alert-error" role="alert">{error}</div>}

      <dl className="details">
        {rows.map(([label, value]) => (
          <div className="detail-row" key={label}>
            <dt>{label}</dt>
            <dd>{value}</dd>
          </div>
        ))}
      </dl>

      <Link to="/shipments" className="btn">← Back to list</Link>

      {confirming && (
        <ConfirmDialog
          title="Delete shipment?"
          message={`Shipment ${shipment.trackingNumber} will be permanently deleted. This cannot be undone.`}
          busy={deleting}
          onConfirm={confirmDelete}
          onCancel={() => setConfirming(false)}
        />
      )}
    </section>
  );
}
