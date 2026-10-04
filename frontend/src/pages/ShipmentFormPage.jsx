import { useEffect, useState } from 'react';
import { Link, useNavigate, useParams } from 'react-router-dom';
import {
  createShipment, getErrorMessage, getFieldErrors, getShipment, updateShipment,
} from '../services/shipmentService.js';
import { STATUSES, STATUS_LABELS } from '../utils/constants.js';
import { todayString, validateShipment } from '../utils/validation.js';

const EMPTY = {
  trackingNumber: '',
  senderName: '',
  receiverName: '',
  origin: '',
  destination: '',
  carrier: '',
  status: 'CREATED',
  expectedDeliveryDate: '',
};

const TEXT_INPUTS = [
  ['trackingNumber', 'Tracking number'],
  ['senderName', 'Sender name'],
  ['receiverName', 'Receiver name'],
  ['origin', 'Origin'],
  ['destination', 'Destination'],
  ['carrier', 'Carrier'],
];

export default function ShipmentFormPage() {
  const { id } = useParams();
  const isEdit = Boolean(id);
  const navigate = useNavigate();

  const [values, setValues] = useState(EMPTY);
  const [createdDate, setCreatedDate] = useState(null);
  const [errors, setErrors] = useState({});
  const [loading, setLoading] = useState(isEdit);
  const [loadFailed, setLoadFailed] = useState(false);
  const [saving, setSaving] = useState(false);
  const [formError, setFormError] = useState('');

  useEffect(() => {
    if (!isEdit) return;
    let cancelled = false;
    (async () => {
      try {
        const s = await getShipment(id);
        if (cancelled) return;
        setValues({
          trackingNumber: s.trackingNumber,
          senderName: s.senderName,
          receiverName: s.receiverName,
          origin: s.origin,
          destination: s.destination,
          carrier: s.carrier,
          status: s.status,
          expectedDeliveryDate: s.expectedDeliveryDate,
        });
        setCreatedDate(s.createdAt.slice(0, 10));
      } catch (err) {
        if (!cancelled) {
          setLoadFailed(true);
          setFormError(getErrorMessage(err));
        }
      } finally {
        if (!cancelled) setLoading(false);
      }
    })();
    return () => { cancelled = true; };
  }, [id, isEdit]);

  const minDate = isEdit && createdDate ? createdDate : todayString();

  function handleChange(e) {
    const { name, value } = e.target;
    setValues((v) => ({ ...v, [name]: value }));
    if (errors[name]) setErrors((prev) => ({ ...prev, [name]: undefined }));
  }

  async function handleSubmit(e) {
    e.preventDefault();
    setFormError('');
    const found = validateShipment(values, minDate);
    setErrors(found);
    if (Object.keys(found).length > 0) return;

    const payload = {
      ...values,
      trackingNumber: values.trackingNumber.trim(),
      senderName: values.senderName.trim(),
      receiverName: values.receiverName.trim(),
      origin: values.origin.trim(),
      destination: values.destination.trim(),
      carrier: values.carrier.trim(),
    };

    setSaving(true);
    try {
      const saved = isEdit ? await updateShipment(id, payload) : await createShipment(payload);
      navigate(`/shipments/${saved.id}`, {
        state: { message: isEdit ? 'Shipment updated successfully.' : 'Shipment created successfully.' },
      });
    } catch (err) {
      const fieldErrors = getFieldErrors(err);
      if (Object.keys(fieldErrors).length > 0) setErrors(fieldErrors);
      setFormError(getErrorMessage(err));
      setSaving(false);
    }
  }

  if (loading) return <p className="state-box" role="status">Loading shipment…</p>;
  if (loadFailed) {
    return (
      <div>
        <div className="alert alert-error" role="alert">{formError}</div>
        <Link to="/shipments" className="btn">Back to shipments</Link>
      </div>
    );
  }

  return (
    <section>
      <h1>{isEdit ? 'Edit Shipment' : 'Add Shipment'}</h1>
      {formError && <div className="alert alert-error" role="alert">{formError}</div>}

      <form className="form" onSubmit={handleSubmit} noValidate>
        {TEXT_INPUTS.map(([name, label]) => (
          <div className="field" key={name}>
            <label htmlFor={name}>{label}</label>
            <input
              id={name}
              name={name}
              type="text"
              value={values[name]}
              onChange={handleChange}
              aria-invalid={Boolean(errors[name])}
              aria-describedby={errors[name] ? `${name}-error` : undefined}
            />
            {errors[name] && <span id={`${name}-error`} className="field-error">{errors[name]}</span>}
          </div>
        ))}

        <div className="field">
          <label htmlFor="status">Status</label>
          <select id="status" name="status" value={values.status} onChange={handleChange}>
            {STATUSES.map((s) => (
              <option key={s} value={s}>{STATUS_LABELS[s]}</option>
            ))}
          </select>
          {errors.status && <span className="field-error">{errors.status}</span>}
        </div>

        <div className="field">
          <label htmlFor="expectedDeliveryDate">Expected delivery date</label>
          <input
            id="expectedDeliveryDate"
            name="expectedDeliveryDate"
            type="date"
            min={minDate}
            value={values.expectedDeliveryDate}
            onChange={handleChange}
            aria-invalid={Boolean(errors.expectedDeliveryDate)}
            aria-describedby={errors.expectedDeliveryDate ? 'expectedDeliveryDate-error' : undefined}
          />
          {errors.expectedDeliveryDate && (
            <span id="expectedDeliveryDate-error" className="field-error">{errors.expectedDeliveryDate}</span>
          )}
        </div>

        <div className="actions">
          <Link to={isEdit ? `/shipments/${id}` : '/shipments'} className="btn">Cancel</Link>
          <button type="submit" className="btn btn-primary" disabled={saving}>
            {saving ? 'Saving…' : isEdit ? 'Save changes' : 'Create shipment'}
          </button>
        </div>
      </form>
    </section>
  );
}
