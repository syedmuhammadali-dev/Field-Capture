import { Pool } from 'pg';
import dotenv from 'dotenv';

dotenv.config();

const connectionString = process.env.DATABASE_URL || 'postgresql://postgres:postgres@localhost:5432/fieldcapture';

export const pool = new Pool({
  connectionString,
  connectionTimeoutMillis: 5000,
});

export async function initDatabase(): Promise<void> {
  try {
    const client = await pool.connect();
    try {
      await client.query(`
        CREATE TABLE IF NOT EXISTS deliveries (
          id VARCHAR(64) PRIMARY KEY,
          supplier_name VARCHAR(255) NOT NULL,
          po_number VARCHAR(100) NOT NULL,
          note TEXT,
          photo_url VARCHAR(500),
          idempotency_key VARCHAR(128) NOT NULL UNIQUE,
          created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
          updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
        );
        CREATE INDEX IF NOT EXISTS idx_deliveries_po_number ON deliveries(po_number);
      `);
      console.log('PostgreSQL deliveries table verified and ready.');
    } finally {
      client.release();
    }
  } catch (err) {
    console.warn('PostgreSQL connection notice:', (err as Error).message);
    console.warn('Ensure PostgreSQL is running if testing with a live database.');
  }
}
