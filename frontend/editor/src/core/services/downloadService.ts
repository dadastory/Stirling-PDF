export interface DownloadRequest {
  data: Blob | File;
  filename: string;
  localPath?: string;
  /** Workspace fileId of the file being exported, when known. Lets export-time
   *  policy enforcement version the in-editor file (not just the download). */
  fileId?: string;
}

export interface DownloadResult {
  savedPath?: string;
  cancelled?: boolean;
}

export async function downloadFile(
  request: DownloadRequest,
): Promise<DownloadResult> {
  if (isDzzOfficeBridgeActive()) {
    if (!request.filename.toLowerCase().endsWith(".pdf")) {
      return { cancelled: true };
    }
    await storeDzzOfficeTemporaryPdf(request.data, request.filename);
    return {};
  }
  const url = URL.createObjectURL(request.data);

  const link = document.createElement("a");
  link.href = url;
  link.download = request.filename;
  document.body.appendChild(link);
  link.click();
  document.body.removeChild(link);

  URL.revokeObjectURL(url);

  return { savedPath: request.localPath };
}

export async function downloadFromUrl(
  url: string,
  filename: string,
  localPath?: string,
): Promise<DownloadResult> {
  if (
    isDzzOfficeBridgeActive() &&
    filename.toLowerCase().endsWith(".pdf")
  ) {
    const response = await fetch(url, { credentials: "same-origin" });
    if (!response.ok) {
      throw new Error("Unable to download the PDF result for DzzOffice");
    }
    await storeDzzOfficeTemporaryPdf(await response.blob(), filename);
    return {};
  }
  if (isDzzOfficeBridgeActive()) return { cancelled: true };
  const link = document.createElement("a");
  link.href = url;
  link.download = filename;
  document.body.appendChild(link);
  link.click();
  document.body.removeChild(link);

  return { savedPath: localPath };
}
import {
  isDzzOfficeBridgeActive,
  storeDzzOfficeTemporaryPdf,
} from "@app/services/dzzOfficeBridge";
