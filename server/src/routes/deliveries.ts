import { Router, Request, Response } from 'express';
import multer from 'multer';
import path from 'path';
import fs from 'fs';
import { v4 as uuidv4 } from 'uuid';
import { pool } from '../db';

const router = Router();

// Ensure local uploads directory exists (Section 7: simple local development storage strategy)
const uploadsDir = path.join(__dirname, '../../uploads');
if (!fs.existsSync(uploadsDir)) {
  fs.mkdirSync(uploadsDir, { recursive: true });
}

const storage = multer.diskStorage({
  destination: (_req, _file, cb) => {
    cb(null, uploadsDir);
  },
  filename: (_req, file, cb) => {
    const ext = path.extname(file.originalname) || '.jpg';
    cb(null, `ticket-${Date.now()}-${uuidv4().substring(0, 8)}${ext}`);
  },
});

const upload = multer({
  storage,
  limits: { fileSize: 15 * 1024 * 1024 }, // 15 MB
});

/**
 * POST /api/deliveries
 * Saves a delivery ticket with duplicate submission protection via idempotencyKey (Section 13 & 14)
 */
router.post('/', upload.single('photo'), async (req: Request, res: Response) => {
  try {
    const { supplierName, poNumber, note, idempotencyKey } = req.body;

    if (!supplierName || !supplierName.trim()) {
      return res.status(400).json({ error: 'supplierName is required' });
    }
    if (!poNumber || !poNumber.trim()) {
      return res.status(400).json({ error: 'poNumber is required' });
    }
    if (!idempotencyKey || !idempotencyKey.trim()) {
      return res.status(400).json({ error: 'idempotencyKey is required' });
    }

    const trimmedKey = idempotencyKey.trim();

    // 1. Idempotency Check: query database for existing record with this idempotency_key
    try {
      const existingCheck = await pool.query(
        'SELECT * FROM deliveries WHERE idempotency_key = $1 LIMIT 1',
        [trimmedKey]
      );

      if (existingCheck.rows.length > 0) {
        const existing = existingCheck.rows[0];
        console.log(`[Idempotency] Returning existing delivery ${existing.id} for key ${trimmedKey}`);
        return res.status(200).json({
          id: existing.id,
          supplierName: existing.supplier_name,
          poNumber: existing.po_number,
          note: existing.note,
          photoUrl: existing.photo_url,
          idempotencyKey: existing.idempotency_key,
          createdAt: existing.created_at,
          isDuplicate: true,
          message: 'Delivery already processed with this idempotency key.'
        });
      }
    } catch (dbErr) {
      console.warn('Database idempotency check skipped or unavailable:', (dbErr as Error).message);
    }

    // 2. Prepare new delivery record
    const deliveryId = `srv_${Date.now()}_${uuidv4().substring(0, 8)}`;
    const photoUrl = req.file ? `/uploads/${req.file.filename}` : null;
    const now = new Date();

    try {
      const insertResult = await pool.query(
        `INSERT INTO deliveries 
          (id, supplier_name, po_number, note, photo_url, idempotency_key, created_at, updated_at)
         VALUES ($1, $2, $3, $4, $5, $6, $7, $8)
         RETURNING *`,
        [deliveryId, supplierName.trim(), poNumber.trim(), (note || '').trim(), photoUrl, trimmedKey, now, now]
      );

      const saved = insertResult.rows[0];
      return res.status(201).json({
        id: saved.id,
        supplierName: saved.supplier_name,
        poNumber: saved.po_number,
        note: saved.note,
        photoUrl: saved.photo_url,
        idempotencyKey: saved.idempotency_key,
        createdAt: saved.created_at,
        isDuplicate: false,
        message: 'Delivery successfully recorded in PostgreSQL.'
      });
    } catch (insertErr: any) {
      // Catch unique constraint violation in case of a race condition
      if (insertErr.code === '23505') { // PostgreSQL unique violation code
        const fallback = await pool.query('SELECT * FROM deliveries WHERE idempotency_key = $1', [trimmedKey]);
        if (fallback.rows.length > 0) {
          const row = fallback.rows[0];
          return res.status(200).json({
            id: row.id,
            supplierName: row.supplier_name,
            poNumber: row.po_number,
            note: row.note,
            photoUrl: row.photo_url,
            idempotencyKey: row.idempotency_key,
            createdAt: row.created_at,
            isDuplicate: true,
            message: 'Delivery already processed (concurrent request caught by UNIQUE constraint).'
          });
        }
      }
      throw insertErr;
    }
  } catch (error) {
    console.error('Error saving delivery:', error);
    return res.status(500).json({ error: (error as Error).message || 'Internal Server Error' });
  }
});

/**
 * GET /api/deliveries/:id
 */
router.get('/:id', async (req: Request, res: Response) => {
  try {
    const { id } = req.params;
    const result = await pool.query('SELECT * FROM deliveries WHERE id = $1', [id]);
    if (result.rows.length === 0) {
      return res.status(404).json({ error: 'Delivery not found' });
    }
    const row = result.rows[0];
    return res.json({
      id: row.id,
      supplierName: row.supplier_name,
      poNumber: row.po_number,
      note: row.note,
      photoUrl: row.photo_url,
      idempotencyKey: row.idempotency_key,
      createdAt: row.created_at
    });
  } catch (error) {
    return res.status(500).json({ error: (error as Error).message });
  }
});

/**
 * GET /api/deliveries
 */
router.get('/', async (_req: Request, res: Response) => {
  try {
    const result = await pool.query('SELECT * FROM deliveries ORDER BY created_at DESC LIMIT 50');
    return res.json(result.rows);
  } catch (error) {
    return res.status(500).json({ error: (error as Error).message });
  }
});

/**
 * POST /api/deliveries/:id/retry
 */
router.post('/:id/retry', async (req: Request, res: Response) => {
  try {
    const { id } = req.params;
    const result = await pool.query('SELECT * FROM deliveries WHERE id = $1', [id]);
    if (result.rows.length === 0) {
      return res.status(404).json({ error: 'Delivery not found' });
    }
    return res.json({ success: true, delivery: result.rows[0] });
  } catch (error) {
    return res.status(500).json({ error: (error as Error).message });
  }
});

export default router;
