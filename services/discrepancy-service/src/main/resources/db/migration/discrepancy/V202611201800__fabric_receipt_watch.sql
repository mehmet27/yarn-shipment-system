-- Kalite itirazı yalnızca girişi yapılmış kumaş için açılır: itiraz edilebilecek kilo, sevkiyata girilen kumaş
-- toplamından açık (haksız bulunmamış) itirazların kilosu çıkarılarak bulunur. Kumaş girişi KumasGirisiYapildi
-- olayından izlenir; olay tekrar gelirse aynı kumaş kaydı ikinci kez sayılmaz.
CREATE TABLE fabric_receipt_watch (
    receipt_id  uuid          PRIMARY KEY,
    shipment_id uuid          NOT NULL,
    net_kg      numeric(14, 3) NOT NULL CHECK (net_kg > 0),
    received_at timestamptz   NOT NULL,
    created_at  timestamptz   NOT NULL,
    created_by  uuid          NOT NULL
);

COMMENT ON TABLE fabric_receipt_watch IS 'KumasGirisiYapildi olaylarından; itiraz edilebilecek kumaş kilosu için';

CREATE INDEX fabric_receipt_watch_shipment_idx ON fabric_receipt_watch (shipment_id);

GRANT SELECT, INSERT ON fabric_receipt_watch TO ${app_role};
