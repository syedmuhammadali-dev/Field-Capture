"use strict";
var __importDefault = (this && this.__importDefault) || function (mod) {
    return (mod && mod.__esModule) ? mod : { "default": mod };
};
Object.defineProperty(exports, "__esModule", { value: true });
exports.pool = void 0;
exports.initDatabase = initDatabase;
const pg_1 = require("pg");
const dotenv_1 = __importDefault(require("dotenv"));
dotenv_1.default.config();
const connectionString = process.env.DATABASE_URL || 'postgresql://postgres:postgres@localhost:5432/fieldcapture';
exports.pool = new pg_1.Pool({
    connectionString,
    connectionTimeoutMillis: 5000,
});
async function initDatabase() {
    try {
        const client = await exports.pool.connect();
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
        }
        finally {
            client.release();
        }
    }
    catch (err) {
        console.warn('PostgreSQL connection notice:', err.message);
        console.warn('Ensure PostgreSQL is running if testing with a live database.');
    }
}
