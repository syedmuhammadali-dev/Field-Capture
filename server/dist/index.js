"use strict";
var __importDefault =
  (this && this.__importDefault) ||
  function (mod) {
    return mod && mod.__esModule ? mod : { default: mod };
  };
Object.defineProperty(exports, "__esModule", { value: true });
const express_1 = __importDefault(require("express"));
const cors_1 = __importDefault(require("cors"));
const dotenv_1 = __importDefault(require("dotenv"));
const deliveries_1 = __importDefault(require("./routes/deliveries"));
const db_1 = require("./db");
dotenv_1.default.config();
const app = (0, express_1.default)();
const PORT = process.env.PORT || 3000;
app.use((0, cors_1.default)());
app.use(express_1.default.json());
app.use(express_1.default.urlencoded({ extended: true }));
// Health check endpoint (Section 14)
app.get("/api/health", async (_req, res) => {
  let dbStatus = "disconnected";
  try {
    const dbTest = await db_1.pool.query("SELECT 1");
    if (dbTest) dbStatus = "connected";
  } catch {
    dbStatus = "offline";
  }
  res.json({
    status: "ok",
    service: "FieldCapture Delivery API",
    timestamp: new Date().toISOString(),
    database: dbStatus,
  });
});
// Deliveries API
app.use("/api/deliveries", deliveries_1.default);
// Start server
app.listen(PORT, async () => {
  console.log(`===============================================`);
  console.log(`FieldCapture API listening on http://localhost:${PORT}`);
  console.log(`Health Check: http://localhost:${PORT}/api/health`);
  console.log(`Deliveries:   http://localhost:${PORT}/api/deliveries`);
  console.log(`===============================================`);
  await (0, db_1.initDatabase)();
});
exports.default = app;
