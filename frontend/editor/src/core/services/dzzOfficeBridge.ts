import { getApiBaseUrl } from "@app/services/apiClientConfig";

let active = false;

export interface DzzOfficeWorkspaceResult {
  id: string;
  name: string;
}

function bridgeUrl(path: string): string {
  const base = getApiBaseUrl() || "/";
  const withTrailingSlash = base.endsWith("/") ? base : `${base}/`;
  return new URL(path, new URL(withTrailingSlash, window.location.origin)).toString();
}

function readFilename(contentDisposition: string | null): string {
  const match = contentDisposition?.match(/filename="?([^";\r\n]+)/i);
  const name = match?.[1];
  return name && name.toLowerCase().endsWith(".pdf") ? name : "document.pdf";
}

export function isDzzOfficeBridgeActive(): boolean {
  return active;
}

export async function loadDzzOfficePdf(): Promise<File | null> {
  const response = await fetch(bridgeUrl("api/v1/dzz/input"), {
    credentials: "same-origin",
  });
  if (!response.ok) {
    return null;
  }
  const fileName = readFilename(response.headers.get("Content-Disposition"));
  const pdf = await response.blob();
  active = true;
  return new File([pdf], fileName, { type: "application/pdf" });
}

export async function saveDzzOfficePdf(
  pdf: Blob | File,
  filename: string,
): Promise<boolean> {
  if (!active || !filename.toLowerCase().endsWith(".pdf")) {
    return false;
  }
  const data = new FormData();
  data.append("file", pdf, filename);
  const response = await fetch(bridgeUrl("api/v1/dzz/save"), {
    method: "POST",
    body: data,
    credentials: "same-origin",
  });
  if (response.status !== 201) {
    throw new Error("DzzOffice could not save the PDF as a new version");
  }
  return true;
}

function newWorkspaceResultId(): string {
  if (typeof crypto !== "undefined" && typeof crypto.randomUUID === "function") {
    return crypto.randomUUID();
  }
  return `dzz-${Date.now()}-${Math.random().toString(36).slice(2, 12)}`;
}

function notifyWorkspaceChanged(): void {
  window.dispatchEvent(new Event("dzz-office-workspace-changed"));
}

export async function storeDzzOfficeTemporaryPdf(
  pdf: Blob | File,
  filename: string,
  resultId = newWorkspaceResultId(),
): Promise<DzzOfficeWorkspaceResult | null> {
  if (!active || !filename.toLowerCase().endsWith(".pdf")) return null;
  const data = new FormData();
  data.append("file", pdf, filename);
  const response = await fetch(
    `${bridgeUrl("api/v1/dzz/workspace")}?id=${encodeURIComponent(resultId)}`,
    { method: "POST", body: data, credentials: "same-origin" },
  );
  if (!response.ok) throw new Error("DzzOffice temporary workspace is unavailable");
  const result = (await response.json()) as DzzOfficeWorkspaceResult;
  notifyWorkspaceChanged();
  return result;
}

export async function listDzzOfficeTemporaryPdfs(): Promise<
  DzzOfficeWorkspaceResult[]
> {
  if (!active) return [];
  const response = await fetch(bridgeUrl("api/v1/dzz/workspace/results"), {
    credentials: "same-origin",
  });
  if (!response.ok) {
    throw new Error("DzzOffice temporary workspace is unavailable");
  }
  const results = (await response.json()) as unknown;
  if (!Array.isArray(results)) return [];
  return results.filter(
    (result): result is DzzOfficeWorkspaceResult =>
      typeof result?.id === "string" &&
      typeof result?.name === "string" &&
      result.name.toLowerCase().endsWith(".pdf"),
  );
}

export async function readDzzOfficeTemporaryPdf(
  result: DzzOfficeWorkspaceResult,
): Promise<File> {
  const response = await fetch(
    bridgeUrl(`api/v1/dzz/workspace/${encodeURIComponent(result.id)}`),
    { credentials: "same-origin" },
  );
  if (!response.ok) {
    throw new Error("DzzOffice temporary result is unavailable");
  }
  return new File([await response.blob()], result.name, {
    type: "application/pdf",
  });
}

export async function deleteDzzOfficeWorkspace(): Promise<void> {
  if (!active) return;
  await fetch(bridgeUrl("api/v1/dzz/workspace"), {
    method: "DELETE",
    credentials: "same-origin",
    keepalive: true,
  });
  notifyWorkspaceChanged();
}

export function resetDzzOfficeBridgeForTests(): void {
  active = false;
}
