import { afterEach, describe, expect, test, vi } from "vitest";
import {
  isDzzOfficeBridgeActive,
  loadDzzOfficePdf,
  resetDzzOfficeBridgeForTests,
  saveDzzOfficePdf,
} from "@app/services/dzzOfficeBridge";

afterEach(() => {
  resetDzzOfficeBridgeForTests();
  vi.unstubAllGlobals();
});

describe("DzzOffice bridge", () => {
  test("imports the authorized PDF and enables version saving", async () => {
    vi.stubGlobal(
      "fetch",
      vi.fn().mockResolvedValue(
        new Response("%PDF-1.7", {
          status: 200,
          headers: {
            "Content-Type": "application/pdf",
            "Content-Disposition": 'inline; filename="source.pdf"',
          },
        }),
      ),
    );

    const file = await loadDzzOfficePdf();

    expect(file?.name).toBe("source.pdf");
    expect(isDzzOfficeBridgeActive()).toBe(true);
  });

  test("sends PDF exports back to Dzz as a new version", async () => {
    vi.stubGlobal(
      "fetch",
      vi
        .fn()
        .mockResolvedValueOnce(
          new Response("%PDF-1.7", {
            status: 200,
            headers: { "Content-Disposition": 'inline; filename="source.pdf"' },
          }),
        )
        .mockResolvedValueOnce(new Response(null, { status: 201 })),
    );
    await loadDzzOfficePdf();

    await expect(
      saveDzzOfficePdf(new Blob(["%PDF-1.7"]), "edited.pdf"),
    ).resolves.toBe(true);
    expect(fetch).toHaveBeenCalledTimes(2);
  });
});
