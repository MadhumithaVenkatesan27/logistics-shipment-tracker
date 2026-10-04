/** Mirrors the backend rules. The backend remains the source of truth. */
const TEXT_FIELDS = [
  ['trackingNumber', 'Tracking number', 5, 30],
  ['senderName', 'Sender name', 2, 100],
  ['receiverName', 'Receiver name', 2, 100],
  ['origin', 'Origin', 2, 100],
  ['destination', 'Destination', 2, 100],
  ['carrier', 'Carrier', 2, 100],
];

/** yyyy-mm-dd for the user's local "today" */
export function todayString() {
  const d = new Date();
  const month = String(d.getMonth() + 1).padStart(2, '0');
  const day = String(d.getDate()).padStart(2, '0');
  return `${d.getFullYear()}-${month}-${day}`;
}

/**
 * @param values   form values
 * @param minDate  earliest allowed delivery date (yyyy-mm-dd)
 * @returns object of { field: message }; empty object means valid
 */
export function validateShipment(values, minDate = todayString()) {
  const errors = {};

  TEXT_FIELDS.forEach(([field, label, min, max]) => {
    const value = (values[field] ?? '').trim();
    if (!value) {
      errors[field] = `${label} is required`;
    } else if (value.length < min || value.length > max) {
      errors[field] = `${label} must be ${min}-${max} characters`;
    }
  });

  if (!values.status) {
    errors.status = 'Status is required';
  }

  if (!values.expectedDeliveryDate) {
    errors.expectedDeliveryDate = 'Expected delivery date is required';
  } else if (values.expectedDeliveryDate < minDate) {
    errors.expectedDeliveryDate = 'Expected delivery date must not be earlier than the creation date';
  }

  return errors;
}
