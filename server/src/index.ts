import express from 'express';
import cors from 'cors';
import path from 'path';
import dotenv from 'dotenv';
import deliveriesRouter from './routes/deliveries';
import { initDatabase, pool } from './db';

dotenv.config();

const app = express();
const PORT = process.env.PORT || 3000;

app.use(cors());
app.use(express.json());
app.use(express.urlencoded({ extended: true }));

// Serve uploaded photos
app.use('/uploads', express.static(path.join(__dirname, '../uploads')));

// Health check endpoint (Section 14)
app.get('/api/health', async (_req, res) => {
  let dbStatus = 'disconnected';
  try {
    const dbTest = await pool.query('SELECT 1');
    if (dbTest) dbStatus = 'connected';
  } catch {
    dbStatus = 'offline';
  }

  res.json({
    status: 'ok',
    service: 'FieldCapture Delivery API',
    timestamp: new Date().toISOString(),
    database: dbStatus,
  });
});

// Deliveries API
app.use('/api/deliveries', deliveriesRouter);

// Start server
app.listen(PORT, async () => {
  console.log(`===============================================`);
  console.log(`FieldCapture API listening on http://localhost:${PORT}`);
  console.log(`Health Check: http://localhost:${PORT}/api/health`);
  console.log(`Deliveries:   http://localhost:${PORT}/api/deliveries`);
  console.log(`===============================================`);
  await initDatabase();
});

export default app;
