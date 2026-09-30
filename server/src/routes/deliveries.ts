import { NextFunction, Router, Request, Response } from 'express';
import multer from 'multer';
import { v4 as uuidv4 } from 'uuid';
import { del, put } from '@vercel/blob';
import { pool } from '../db';

const router = Router();

const upload = multer({
  storage: multer.memoryStorage(),
  limits: { fileSize: 3 * 1024 * 1024, fields: 4, fieldSize: 16 * 1024 },
});

const parsePhoto = (req: Request, res: Response, next: NextFunction) => {
  upload.single('photo')(req, res, (error) => {
    if (error instanceof multer.MulterError && error.code === 'LIMIT_FILE_SIZE') {
      return res.status(413).json({ error: 'Ticket photo must be 3 MB or smaller' });
    }
    if (error) {
      return res.status(400).json({ error: 'Invalid delivery photo upload' });
    }
    next();
  });
};

/**
 * POST /api/deliveries
 * Saves a delivery ticket with duplicate submission protection via idempotencyKey (Section 13 & 14)
 */
router.post('/', parsePhoto, async (req: Request, res: Response) => {
  let uploadedPhotoUrl: string | null = null;

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

    if (req.file && !['image/jpeg', 'image/png', 'image/webp'].includes(req.file.mimetype)) {
      return res.status(415).json({ error: 'Ticket photo must be JPEG, PNG, or WebP' });
    }

    const trimmedKey = idempotencyKey.trim();

    // 1. Idempotency Check: query database for existing record with this idempotency_key
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

    // 2. Store the photo outside the serverless function filesystem.
    let photoUrl: string | null = null;
    if (req.file) {
      const extension = req.file.mimetype === 'image/png'
        ? 'png'
        : req.file.mimetype === 'image/webp'
          ? 'webp'
          : 'jpg';
      const blob = await put(`delivery-tickets/${uuidv4()}.${extension}`, req.file.buffer, {
        access: 'public',
        contentType: req.file.mimetype,
        addRandomSuffix: true,
      });
      photoUrl = blob.url;
      uploadedPhotoUrl = blob.url;
    }

    // 3. Prepare new delivery record
    const deliveryId = `srv_${Date.now()}_${uuidv4().substring(0, 8)}`;
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
      uploadedPhotoUrl = null;
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
        if (uploadedPhotoUrl) {
          await del(uploadedPhotoUrl).catch(() => undefined);
          uploadedPhotoUrl = null;
        }
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
    if (uploadedPhotoUrl) {
      await del(uploadedPhotoUrl).catch(() => undefined);
    }
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
