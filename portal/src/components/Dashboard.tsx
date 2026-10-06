import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { api, type CostReport, type Metric } from '../api';
import { useSession } from '../auth';
import { formatCount, formatDate, formatMoney, formatPrecise, metricLabel } from '../format';

/** Live cost of the open period. Refetches every 10 s, so new usage shows up without reloading. */
export function Dashboard({ accountId }: { accountId: number }) {
  const session = useSession();
  const cost = useQuery({
    queryKey: ['cost', accountId],
    queryFn: () => api.currentCost(session.token, accountId),
    refetchInterval: 10_000,
  });

  if (cost.isLoading) return <p className="notice">Loading…</p>;
  if (cost.isError) return <p className="error">Could not load cost: {cost.error.message}</p>;
  const report = cost.data!;

  return (
    <>
      <section className="tiles">
        <Tile label="Billing period" value={`${formatDate(report.startsOn)} – ${formatDate(report.endsOn)}`} />
        <Tile label="Usage so far" value={formatMoney(report.usageTotal)} />
        <Tile label="Included in plan" value={formatMoney(report.planIncluded)} />
        <Tile label="Amount due" value={formatMoney(report.amountDue)} highlight />
      </section>

      {session.roles.includes('ADMIN') && <AdminActions report={report} />}

      <section className="card">
        <h2>
          {report.accountName} <span className="muted">· breakdown</span>
        </h2>
        {report.services.length === 0 ? (
          <p className="muted">No usage recorded in this period yet.</p>
        ) : (
          <table className="breakdown">
            <thead>
              <tr>
                <th>Service / endpoint / metric</th>
                <th className="num">Quantity</th>
                <th className="num">Unit price</th>
                <th className="num">Amount</th>
              </tr>
            </thead>
            {report.services.map((service) => (
              <tbody key={service.serviceId}>
                <tr className="service-row">
                  <td colSpan={3}>{service.name}</td>
                  <td className="num">{formatMoney(service.subtotal)}</td>
                </tr>
                {service.endpoints.map((endpoint) => [
                  <tr key={`e${endpoint.endpointId}`} className="endpoint-row">
                    <td colSpan={3}>{endpoint.name}</td>
                    <td className="num">{formatMoney(endpoint.subtotal)}</td>
                  </tr>,
                  ...endpoint.lines.map((line) => (
                    <tr key={`e${endpoint.endpointId}-${line.metric}`} className="line-row">
                      <td>{metricLabel[line.metric] ?? line.metric}</td>
                      <td className="num">{formatCount(line.quantity)}</td>
                      <td className="num">{formatPrecise(line.unitPrice)}</td>
                      <td className="num">{formatPrecise(line.amount)}</td>
                    </tr>
                  )),
                ])}
              </tbody>
            ))}
          </table>
        )}
      </section>
    </>
  );
}

function Tile({ label, value, highlight }: { label: string; value: string; highlight?: boolean }) {
  return (
    <div className={`tile${highlight ? ' highlight' : ''}`}>
      <div className="tile-label">{label}</div>
      <div className="tile-value">{value}</div>
    </div>
  );
}

const sampleMetrics: Metric[] = ['API_CALLS', 'AI_TOKENS', 'EGRESS'];

/** Operator tools: simulate a worker sending usage, and close the period (issues an invoice). */
function AdminActions({ report }: { report: CostReport }) {
  const session = useSession();
  const queryClient = useQueryClient();
  const endpoints = report.services.flatMap((s) => s.endpoints);

  const refresh = () => {
    queryClient.invalidateQueries({ queryKey: ['cost', report.accountId] });
    queryClient.invalidateQueries({ queryKey: ['invoices', report.accountId] });
  };

  const sendUsage = useMutation({
    mutationFn: () => {
      const endpoint = endpoints[Math.floor(Math.random() * endpoints.length)];
      const metric = sampleMetrics[Math.floor(Math.random() * sampleMetrics.length)];
      const quantity = metric === 'AI_TOKENS' ? 50_000 : 1_000;
      return api.recordUsage(session.token, endpoint.endpointId, metric, quantity);
    },
    onSuccess: refresh,
  });

  const close = useMutation({
    mutationFn: () => api.closePeriod(session.token, report.accountId),
    onSuccess: refresh,
  });

  return (
    <section className="actions">
      <button onClick={() => sendUsage.mutate()} disabled={endpoints.length === 0 || sendUsage.isPending}>
        + Record sample usage
      </button>
      <button
        className="danger"
        disabled={close.isPending}
        onClick={() => {
          if (confirm(`Close the period for ${report.accountName} and issue the invoice? This cannot be undone.`)) {
            close.mutate();
          }
        }}
      >
        Close period &amp; invoice
      </button>
      {(sendUsage.error || close.error) && <span className="error">{(sendUsage.error ?? close.error)!.message}</span>}
      {close.isSuccess && <span className="ok">Invoice #{close.data.id} issued.</span>}
    </section>
  );
}
