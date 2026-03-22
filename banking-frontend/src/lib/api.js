const JSON_HEADERS = {
  "Content-Type": "application/json",
};

async function parseResponse(response) {
  const text = await response.text();
  let payload = null;

  if (text) {
    try {
      payload = JSON.parse(text);
    } catch {
      payload = { error: text };
    }
  }

  if (!response.ok) {
    const message =
      payload?.message ||
      payload?.error ||
      payload?.details ||
      `Request failed with status ${response.status}`;
    throw new Error(message);
  }

  return payload;
}

async function request(path, { method = "GET", token, body, headers } = {}) {
  const response = await fetch(path, {
    method,
    headers: {
      ...JSON_HEADERS,
      ...(token ? { Authorization: `Bearer ${token}` } : {}),
      ...(headers || {}),
    },
    body: body ? JSON.stringify(body) : undefined,
  });

  return parseResponse(response);
}

export const api = {
  registerUser(payload) {
    return request("/api/auth/register", { method: "POST", body: payload });
  },
  login(payload) {
    return request("/api/auth/login", { method: "POST", body: payload });
  },
  getGatewayHealth() {
    return request("/actuator/health");
  },
  getAccount(accountNumber, token) {
    return request(`/api/accounts/${encodeURIComponent(accountNumber)}`, { token });
  },
  createAccount(payload, token) {
    return request("/api/accounts", { method: "POST", token, body: payload });
  },
  createTransaction(payload, token) {
    return request("/api/payments/transactions", {
      method: "POST",
      token,
      body: payload,
      headers: {
        "Idempotency-Key": crypto.randomUUID(),
      },
    });
  },
  getTransactionsByAccount(accountNumber, token) {
    return request(`/api/payments/transactions/account/${encodeURIComponent(accountNumber)}`, { token });
  },
};
