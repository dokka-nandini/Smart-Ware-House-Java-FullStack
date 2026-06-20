import React, { useState, useEffect, useCallback } from "react";
import "./App.css";

const API_BASE = "http://localhost:8080/api";

function StatusBadge({ status }) {
  const cls =
    status === "BACKORDERED"
      ? "status-badge status-backordered"
      : status === "FULFILLED"
      ? "status-badge status-fulfilled"
      : "status-badge status-pending";
  return <span className={cls}>{status}</span>;
}

export default function App() {
  const [inventory, setInventory] = useState([]);
  const [orders, setOrders] = useState([]);
  const [form, setForm] = useState({ itemId: "", quantity: 1, customerType: "REGULAR" });
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState("");
  const [lastResult, setLastResult] = useState(null);

  const loadInventory = useCallback(async () => {
    try {
      const res = await fetch(`${API_BASE}/inventory`);
      const data = await res.json();
      setInventory(data);
      if (data.length > 0 && !form.itemId) {
        setForm((f) => ({ ...f, itemId: data[0].id }));
      }
    } catch (e) {
      setError("Could not reach backend (inventory). Is it running on port 8080?");
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  const loadOrders = useCallback(async () => {
    try {
      const res = await fetch(`${API_BASE}/orders`);
      const data = await res.json();
      setOrders(data);
    } catch (e) {
      setError("Could not reach backend (orders). Is it running on port 8080?");
    }
  }, []);

  useEffect(() => {
    loadInventory();
    loadOrders();
    // Poll periodically so the dashboard reflects stock changes from other clients too.
    const interval = setInterval(() => {
      loadInventory();
      loadOrders();
    }, 4000);
    return () => clearInterval(interval);
  }, [loadInventory, loadOrders]);

  const handleChange = (e) => {
    const { name, value } = e.target;
    setForm((f) => ({ ...f, [name]: name === "quantity" ? Number(value) : value }));
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError("");
    setSubmitting(true);
    setLastResult(null);
    try {
      const res = await fetch(`${API_BASE}/orders`, {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify(form),
      });

      if (!res.ok && res.status !== 200) {
        const errBody = await res.json().catch(() => ({}));
        setError(
          typeof errBody === "object"
            ? Object.values(errBody).join(", ") || `Request failed (${res.status})`
            : `Request failed (${res.status})`
        );
        setSubmitting(false);
        return;
      }

      const data = await res.json();
      setLastResult(data);
      await loadInventory();
      await loadOrders();
    } catch (e) {
      setError("Could not submit order. Is the backend running on port 8080?");
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <div className="app">
      <header className="app-header">
        <h1>Smart Warehouse Dispatcher</h1>
        <p className="subtitle">Inventory &amp; Order Dashboard</p>
      </header>

      {error && <div className="banner banner-error">{error}</div>}

      <section className="panel">
        <h2>Inventory</h2>
        <table className="data-table">
          <thead>
            <tr>
              <th>Item ID</th>
              <th>Name</th>
              <th>Stock</th>
              <th>Restock Threshold</th>
              <th>Status</th>
            </tr>
          </thead>
          <tbody>
            {inventory.map((item) => (
              <tr key={item.id} className={item.belowThreshold ? "row-low-stock" : ""}>
                <td>{item.id}</td>
                <td>{item.name}</td>
                <td>{item.stockQuantity}</td>
                <td>{item.restockThreshold}</td>
                <td>
                  {item.belowThreshold ? (
                    <span className="status-badge status-backordered">LOW STOCK</span>
                  ) : (
                    <span className="status-badge status-fulfilled">OK</span>
                  )}
                </td>
              </tr>
            ))}
            {inventory.length === 0 && (
              <tr>
                <td colSpan="5" className="empty-row">
                  No inventory loaded yet.
                </td>
              </tr>
            )}
          </tbody>
        </table>
      </section>

      <section className="panel">
        <h2>Place a New Order</h2>
        <form className="order-form" onSubmit={handleSubmit}>
          <div className="form-field">
            <label htmlFor="itemId">Item</label>
            <select id="itemId" name="itemId" value={form.itemId} onChange={handleChange} required>
              {inventory.map((item) => (
                <option key={item.id} value={item.id}>
                  {item.name} ({item.id})
                </option>
              ))}
            </select>
          </div>

          <div className="form-field">
            <label htmlFor="quantity">Quantity</label>
            <input
              id="quantity"
              name="quantity"
              type="number"
              min="1"
              value={form.quantity}
              onChange={handleChange}
              required
            />
          </div>

          <div className="form-field">
            <label htmlFor="customerType">Customer Type</label>
            <select id="customerType" name="customerType" value={form.customerType} onChange={handleChange}>
              <option value="REGULAR">REGULAR</option>
              <option value="PREMIUM">PREMIUM</option>
            </select>
          </div>

          <button type="submit" disabled={submitting || !form.itemId}>
            {submitting ? "Submitting..." : "Submit Order"}
          </button>
        </form>

        {lastResult && (
          <div
            className={
              "banner " + (lastResult.status === "BACKORDERED" ? "banner-warning" : "banner-success")
            }
          >
            Order {lastResult.id}: <strong>{lastResult.status}</strong> — {lastResult.message}
          </div>
        )}
      </section>

      <section className="panel">
        <h2>Recent Orders</h2>
        <table className="data-table">
          <thead>
            <tr>
              <th>Order ID</th>
              <th>Item</th>
              <th>Qty</th>
              <th>Customer</th>
              <th>Status</th>
              <th>Placed At</th>
            </tr>
          </thead>
          <tbody>
            {orders.map((order) => (
              <tr key={order.id} className={order.status === "BACKORDERED" ? "row-backordered" : ""}>
                <td>{order.id}</td>
                <td>{order.itemName}</td>
                <td>{order.quantity}</td>
                <td>{order.customerType}</td>
                <td>
                  <StatusBadge status={order.status} />
                </td>
                <td>{order.createdAt ? new Date(order.createdAt).toLocaleTimeString() : "-"}</td>
              </tr>
            ))}
            {orders.length === 0 && (
              <tr>
                <td colSpan="6" className="empty-row">
                  No orders placed yet.
                </td>
              </tr>
            )}
          </tbody>
        </table>
      </section>
    </div>
  );
}
