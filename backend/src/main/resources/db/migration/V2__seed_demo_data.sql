-- Optional enhancement: demo data so the UI is not empty on first start.
INSERT INTO shipments (tracking_number, sender_name, receiver_name, origin, destination, carrier, status, expected_delivery_date, created_at, updated_at) VALUES
('SHP-1001', 'North Warehouse', 'City Pharmacy',   'Leeds',      'London',     'QuickMove Logistics', 'IN_TRANSIT',       CURRENT_DATE + 3, NOW(), NOW()),
('SHP-1002', 'Acme Electronics', 'Tech Hub Ltd',   'Manchester', 'Bristol',    'SwiftFreight',        'CREATED',          CURRENT_DATE + 6, NOW(), NOW()),
('SHP-1003', 'Green Farms',      'Fresh Mart',     'York',       'Birmingham', 'QuickMove Logistics', 'OUT_FOR_DELIVERY', CURRENT_DATE + 1, NOW(), NOW()),
('SHP-1004', 'Book Depot',       'Reader''s Corner','Glasgow',   'Edinburgh',  'RoyalPost Express',   'DELIVERED',        CURRENT_DATE,     NOW(), NOW()),
('SHP-1005', 'Fashion Outlet',   'Style Boutique', 'Liverpool',  'Cardiff',    'SwiftFreight',        'CANCELLED',        CURRENT_DATE + 4, NOW(), NOW());
