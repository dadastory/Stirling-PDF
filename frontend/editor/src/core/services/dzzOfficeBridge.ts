import { getApiBaseUrl } from "@app/services/apiClientConfig";

let active = false;

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

export function resetDzzOfficeBridgeForTests(): void {
  active = false;
}
