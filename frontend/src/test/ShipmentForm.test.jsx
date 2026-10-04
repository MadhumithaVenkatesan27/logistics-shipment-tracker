import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter, Route, Routes } from 'react-router-dom';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import ShipmentFormPage from '../pages/ShipmentFormPage.jsx';
import * as api from '../services/shipmentService.js';
import { validateShipment } from '../utils/validation.js';

vi.mock('../services/shipmentService.js', async (importOriginal) => {
  const actual = await importOriginal();
  return { ...actual, createShipment: vi.fn(), updateShipment: vi.fn(), getShipment: vi.fn() };
});

function renderForm() {
  return render(
    <MemoryRouter initialEntries={['/shipments/new']}>
      <Routes>
        <Route path="/shipments/new" element={<ShipmentFormPage />} />
        <Route path="/shipments/:id" element={<p>Detail page</p>} />
      </Routes>
    </MemoryRouter>
  );
}

describe('ShipmentFormPage', () => {
  beforeEach(() => vi.clearAllMocks());

  it('shows validation feedback and does not submit when required fields are missing', async () => {
    renderForm();

    await userEvent.click(screen.getByRole('button', { name: /create shipment/i }));

    expect(screen.getByText('Tracking number is required')).toBeInTheDocument();
    expect(screen.getByText('Sender name is required')).toBeInTheDocument();
    expect(screen.getByText('Expected delivery date is required')).toBeInTheDocument();
    expect(api.createShipment).not.toHaveBeenCalled();
  });

  it('shows a length error for a too-short tracking number', async () => {
    renderForm();

    await userEvent.type(screen.getByLabelText('Tracking number'), 'AB');
    await userEvent.click(screen.getByRole('button', { name: /create shipment/i }));

    expect(screen.getByText('Tracking number must be 5-30 characters')).toBeInTheDocument();
  });

  it('submits valid data, disables the button while saving, then navigates to the detail page', async () => {
    let resolveSave;
    api.createShipment.mockReturnValue(new Promise((resolve) => { resolveSave = resolve; }));
    renderForm();

    await userEvent.type(screen.getByLabelText('Tracking number'), 'SHP-2001');
    await userEvent.type(screen.getByLabelText('Sender name'), 'North Warehouse');
    await userEvent.type(screen.getByLabelText('Receiver name'), 'City Pharmacy');
    await userEvent.type(screen.getByLabelText('Origin'), 'Leeds');
    await userEvent.type(screen.getByLabelText('Destination'), 'London');
    await userEvent.type(screen.getByLabelText('Carrier'), 'QuickMove');
    await userEvent.type(screen.getByLabelText('Expected delivery date'), '2099-01-01');
    await userEvent.click(screen.getByRole('button', { name: /create shipment/i }));

    expect(screen.getByRole('button', { name: /saving/i })).toBeDisabled();
    expect(api.createShipment).toHaveBeenCalledWith(
      expect.objectContaining({ trackingNumber: 'SHP-2001', status: 'CREATED', expectedDeliveryDate: '2099-01-01' })
    );

    resolveSave({ id: 7 });
    expect(await screen.findByText('Detail page')).toBeInTheDocument();
  });

  it('shows the duplicate tracking number error returned by the backend', async () => {
    api.createShipment.mockRejectedValue({
      response: {
        status: 409,
        data: {
          message: "A shipment with tracking number 'SHP-2001' already exists",
          fieldErrors: { trackingNumber: 'Tracking number already exists' },
        },
      },
    });
    renderForm();

    await userEvent.type(screen.getByLabelText('Tracking number'), 'SHP-2001');
    await userEvent.type(screen.getByLabelText('Sender name'), 'North Warehouse');
    await userEvent.type(screen.getByLabelText('Receiver name'), 'City Pharmacy');
    await userEvent.type(screen.getByLabelText('Origin'), 'Leeds');
    await userEvent.type(screen.getByLabelText('Destination'), 'London');
    await userEvent.type(screen.getByLabelText('Carrier'), 'QuickMove');
    await userEvent.type(screen.getByLabelText('Expected delivery date'), '2099-01-01');
    await userEvent.click(screen.getByRole('button', { name: /create shipment/i }));

    expect(await screen.findByText('Tracking number already exists')).toBeInTheDocument();
    expect(screen.getByRole('button', { name: /create shipment/i })).toBeEnabled();
  });
});

describe('validateShipment', () => {
  const valid = {
    trackingNumber: 'SHP-1001', senderName: 'AB', receiverName: 'CD', origin: 'EF', destination: 'GH',
    carrier: 'IJ', status: 'CREATED', expectedDeliveryDate: '2026-09-25',
  };

  it('accepts valid values', () => {
    expect(validateShipment(valid, '2026-09-19')).toEqual({});
  });

  it('rejects a delivery date earlier than the minimum date', () => {
    expect(validateShipment({ ...valid, expectedDeliveryDate: '2026-09-01' }, '2026-09-19'))
      .toHaveProperty('expectedDeliveryDate');
  });
});
