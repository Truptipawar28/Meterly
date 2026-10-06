// Typed client for the Meterly REST API. Types mirror the Java DTO records.

export type Role = 'ADMIN' | 'WRITER' | 'READER';
export type Metric = 'API_CALLS' | 'AI_TOKENS' | 'STORAGE' | 'EGRESS';

export interface Session {
  token: string;
  username: string;
  roles: Role[];
  accountId?: number;
}

export interface TokenResponse {
  accessToken: string;
  tokenType: string;
  expiresIn: number;
  roles: Role[];
  accountId?: number;
}

export interface Account {
  id: number;
  name: string;
  planIncluded: number;
  createdAt: string;
}

export interface LineCost {
  metric: Metric;
  quantity: number;
  unitPrice: number;
  amount: number;
}

export interface EndpointCost {
  endpointId: number;
  name: string;
  lines: LineCost[];
  subtotal: number;
}

export interface ServiceCost {
  serviceId: number;
  name: string;
  endpoints: EndpointCost[];
  subtotal: number;
}

export interface CostReport {
  accountId: number;
  accountName: string;
  periodId: number;
  startsOn: string;
  endsOn: string;
  status: 'OPEN' | 'CLOSED';
  services: ServiceCost[];
  usageTotal: number;
  planIncluded: number;
  amountDue: number;
}

export interface InvoiceSummary {
  id: number;
  accountId: number;
  periodId: number;
  startsOn: string;
  endsOn: string;
  amountDue: number;
  issuedAt: string;
}

export interface InvoiceLine {
  serviceName: string;
  endpointName: string;
  metric: Metric;
  quantity: number;
  unitPrice: number;
  amount: number;
}

export interface Invoice extends InvoiceSummary {
  lines: InvoiceLine[];
  usageTotal: number;
  planIncluded: number;
}

export class ApiError extends Error {
  constructor(public status: number, message: string) {
    super(message);
  }
}

/** Called on any 401 so the app can drop the expired session. Set by AuthProvider. */
let unauthorizedHandler: () => void = () => {};
export function onUnauthorized(handler: () => void) {
  unauthorizedHandler = handler;
}

async function request<T>(path: string, token: string | null, init: RequestInit = {}): Promise<T> {
  const headers = new Headers(init.headers);
  headers.set('Content-Type', 'application/json');
  if (token) headers.set('Authorization', `Bearer ${token}`);

  const res = await fetch(path, { ...init, headers });
  if (!res.ok) {
    if (res.status === 401 && token) unauthorizedHandler();
    let message = res.statusText;
    try {
      const problem = await res.json(); // backend sends RFC 9457 problem details
      message = problem.detail ?? problem.title ?? message;
    } catch {
      /* body was not JSON */
    }
    throw new ApiError(res.status, message);
  }
  return (await res.json()) as T;
}

export const api = {
  login: (username: string, password: string) =>
    request<TokenResponse>('/api/auth/token', null, {
      method: 'POST',
      body: JSON.stringify({ username, password }),
    }),

  accounts: (token: string) => request<Account[]>('/api/accounts', token),

  currentCost: (token: string, accountId: number) =>
    request<CostReport>(`/api/accounts/${accountId}/cost`, token),

  invoices: (token: string, accountId: number) =>
    request<InvoiceSummary[]>(`/api/accounts/${accountId}/invoices`, token),

  invoice: (token: string, invoiceId: number) => request<Invoice>(`/api/invoices/${invoiceId}`, token),

  closePeriod: (token: string, accountId: number) =>
    request<Invoice>(`/api/accounts/${accountId}/close-period`, token, { method: 'POST' }),

  recordUsage: (token: string, endpointId: number, metric: Metric, quantity: number) =>
    request<unknown>('/api/usage', token, {
      method: 'POST',
      headers: { 'Idempotency-Key': crypto.randomUUID() },
      body: JSON.stringify({ endpointId, metric, quantity }),
    }),
};
