import { useState } from 'react';
import { useQuery } from '@tanstack/react-query';
import { api } from '../api';
import { useSession } from '../auth';
import { formatCount, formatDate, formatMoney, formatPrecise, metricLabel } from '../format';

export function Invoices({ accountId }: { accountId: number }) {
  const session = useSession();
  const [selected, setSelected] = useState<number | null>(null);
  const invoices = useQuery({
    queryKey: ['invoices', accountId],
    queryFn: () => api.invoices(session.token, accountId),
  });

  if (invoices.isLoading) return <p className="notice">Loading…</p>;
  if (invoices.isError) return <p className="error">Could not load invoices: {invoices.error.message}</p>;
  if (invoices.data!.length === 0) return <p className="notice">No invoices yet. They are issued when a period closes.</p>;

  return (
    <div className="invoices">
      <section className="card">
        <h2>Invoices</h2>
        <table>
          <thead>
            <tr>
              <th>#</th>
              <th>Period</th>
              <th>Issued</th>
              <th className="num">Amount due</th>
            </tr>
          </thead>
          <tbody>
            {invoices.data!.map((inv) => (
              <tr
                key={inv.id}
                className={`clickable${selected === inv.id ? ' selected' : ''}`}
                onClick={() => setSelected(inv.id)}
              >
                <td>{inv.id}</td>
                <td>
                  {formatDate(inv.startsOn)} – {formatDate(inv.endsOn)}
                </td>
                <td>{formatDate(inv.issuedAt)}</td>
                <td className="num">{formatMoney(inv.amountDue)}</td>
              </tr>
            ))}
          </tbody>
        </table>
      </section>
      {selected != null && <InvoiceDetail invoiceId={selected} />}
    </div>
  );
}

function InvoiceDetail({ invoiceId }: { invoiceId: number }) {
  const session = useSession();
  // An issued invoice never changes, so its cache never goes stale.
  const invoice = useQuery({
    queryKey: ['invoice', invoiceId],
    queryFn: () => api.invoice(session.token, invoiceId),
    staleTime: Infinity,
  });

  if (invoice.isLoading) return <p className="notice">Loading invoice…</p>;
  if (invoice.isError) return <p className="error">{invoice.error.message}</p>;
  const inv = invoice.data!;

  return (
    <section className="card">
      <h2>
        Invoice #{inv.id} <span className="badge">final</span>
      </h2>
      <p className="muted">
        {formatDate(inv.startsOn)} – {formatDate(inv.endsOn)} · issued {formatDate(inv.issuedAt)}
      </p>
      <table>
        <thead>
          <tr>
            <th>Service</th>
            <th>Endpoint</th>
            <th>Metric</th>
            <th className="num">Quantity</th>
            <th className="num">Unit price</th>
            <th className="num">Amount</th>
          </tr>
        </thead>
        <tbody>
          {inv.lines.map((l, i) => (
            <tr key={i}>
              <td>{l.serviceName}</td>
              <td>{l.endpointName}</td>
              <td>{metricLabel[l.metric] ?? l.metric}</td>
              <td className="num">{formatCount(l.quantity)}</td>
              <td className="num">{formatPrecise(l.unitPrice)}</td>
              <td className="num">{formatPrecise(l.amount)}</td>
            </tr>
          ))}
        </tbody>
        <tfoot>
          <tr>
            <td colSpan={5}>Usage total</td>
            <td className="num">{formatMoney(inv.usageTotal)}</td>
          </tr>
          <tr>
            <td colSpan={5}>Included in plan</td>
            <td className="num">− {formatMoney(inv.planIncluded)}</td>
          </tr>
          <tr className="total">
            <td colSpan={5}>Amount due</td>
            <td className="num">{formatMoney(inv.amountDue)}</td>
          </tr>
        </tfoot>
      </table>
    </section>
  );
}
