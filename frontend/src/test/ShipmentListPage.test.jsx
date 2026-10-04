import { render, screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter } from 'react-router-dom';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import ShipmentListPage from '../pages/ShipmentListPage.jsx';
import * as api from '../services/shipmentService.js';

vi.mock('../services/shipmentService.js', async (importOriginal) => {
  const actual = await importOriginal();
  return { ...actual, getShipments: vi.fn(), deleteShipment: vi.fn() };
});

const sample = [
  {
    id: 1, trackingNumber: 'SHP-1001', senderName: 'North Warehouse', receiverName: 'City Pharmacy',
    origin: 'Leeds', destination: 'London', carrier: 'QuickMove Logistics', status: 'IN_TRANSIT',
    expectedDeliveryDate: '2026-09-25', createdAt: '2026-09-19T10:30:00Z', updatedAt: '2026-09-19T10:30:00Z',
  },
  {
    id: 2, trackingNumber: 'SHP-1002', senderName: 'Acme', receiverName: 'Tech Hub',
    origin: 'Manchester', destination: 'Bristol', carrier: 'SwiftFreight', status: 'DELIVERED',
    expectedDeliveryDate: '2026-09-22', createdAt: '2026-09-19T10:30:00Z', updatedAt: '2026-09-19T10:30:00Z',
  },
];

function renderPage() {
  return render(
    <MemoryRouter initialEntries={['/shipments']}>
      <ShipmentListPage />
    </MemoryRouter>
  );
}

describe('ShipmentListPage', () => {
  beforeEach(() => vi.clearAllMocks());

  it('renders shipments returned by the API', async () => {
    api.getShipments.mockResolvedValue(sample);
    renderPage();

    expect(screen.getByText(/loading shipments/i)).toBeInTheDocument();
    expect(await screen.findByText('SHP-1001')).toBeInTheDocument();
    expect(screen.getByText('SHP-1002')).toBeInTheDocument();
    expect(screen.getByText('Leeds')).toBeInTheDocument();
    expect(screen.getAllByRole('link', { name: 'View' })).toHaveLength(2);
  });

  it('shows a friendly error when the API request fails', async () => {
    api.getShipments.mockRejectedValue(new Error('Network Error'));
    renderPage();

    expect(await screen.findByRole('alert')).toHaveTextContent(/unable to reach the server/i);
  });

  it('shows an empty-state message when no shipments match the filter', async () => {
    api.getShipments.mockResolvedValueOnce(sample).mockResolvedValue([]);
    renderPage();
    await screen.findByText('SHP-1001');

    await userEvent.selectOptions(screen.getByLabelText(/filter by status/i), 'CANCELLED');

    expect(await screen.findByText(/no shipments match the status "cancelled"/i)).toBeInTheDocument();
    expect(api.getShipments).toHaveBeenLastCalledWith('CANCELLED');
  });

  it('asks for confirmation before deleting a shipment', async () => {
    api.getShipments.mockResolvedValue(sample);
    api.deleteShipment.mockResolvedValue();
    renderPage();
    await screen.findByText('SHP-1001');

    await userEvent.click(screen.getAllByRole('button', { name: 'Delete' })[0]);
    expect(screen.getByRole('dialog')).toHaveTextContent(/SHP-1001/);
    expect(api.deleteShipment).not.toHaveBeenCalled();

    await userEvent.click(within(screen.getByRole('dialog')).getByRole('button', { name: 'Delete' }));
    await waitFor(() => expect(api.deleteShipment).toHaveBeenCalledWith(1));
  });
});
