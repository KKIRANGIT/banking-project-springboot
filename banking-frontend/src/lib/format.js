export function formatCurrency(value) {
  if (value === null || value === undefined || value === "") {
    return "USD 0.00";
  }

  return new Intl.NumberFormat("en-US", {
    style: "currency",
    currency: "USD",
    minimumFractionDigits: 2,
  }).format(Number(value));
}

export function formatDateTime(value) {
  if (!value) {
    return "Not available";
  }

  return new Intl.DateTimeFormat("en-US", {
    dateStyle: "medium",
    timeStyle: "short",
  }).format(new Date(value));
}

export function accountMask(accountNumber) {
  if (!accountNumber) {
    return "No account selected";
  }

  const lastFour = accountNumber.slice(-4);
  return `•••• ${lastFour}`;
}
