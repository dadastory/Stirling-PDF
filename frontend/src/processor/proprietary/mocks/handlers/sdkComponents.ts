import { http, HttpResponse, delay } from "msw";
import type { Tier } from "@processor/contexts/TierContext";
import { buildComponentsResponse } from "@processor/mocks/sdkComponents";

export const sdkComponentsHandlers = [
  http.get("/v1/components", async ({ request }) => {
    await delay(120);
    const url = new URL(request.url);
    const tier = (url.searchParams.get("tier") ?? "pro") as Tier;
    return HttpResponse.json(buildComponentsResponse(tier));
  }),
];
