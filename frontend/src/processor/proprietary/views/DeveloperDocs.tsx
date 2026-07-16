import { useState } from "react";
import { useTranslation } from "react-i18next";
import { EmptyState } from "@editor/ui";
import { useTier } from "@processor/contexts/TierContext";
import { useAsync, useSectionFlags } from "@processor/hooks/useAsync";
import {
  fetchDocsContent,
  fetchDocsNav,
  type DocsContent,
  type DocsNavSection,
} from "@processor/api/docs";
import { DocsNav, DocsNavSkeleton } from "@processor/components/docs/DocsNav";
import { GettingStartedSection } from "@processor/components/docs/GettingStartedSection";
import { AuthenticationSection } from "@processor/components/docs/AuthenticationSection";
import { RateLimitsSection } from "@processor/components/docs/RateLimitsSection";
import { EndpointReferenceSection } from "@processor/components/docs/EndpointReferenceSection";
import { ErrorsSection } from "@processor/components/docs/ErrorsSection";
import { WebhooksSection } from "@processor/components/docs/WebhooksSection";
import { SdksSection } from "@processor/components/docs/SdksSection";
import { ComponentsSection } from "@processor/components/docs/ComponentsSection";
import { PlaybooksSection } from "@processor/components/docs/PlaybooksSection";
import { SkillsSection } from "@processor/components/docs/SkillsSection";
import "@processor/views/DeveloperDocs.css";

/** Renders the content pane for the active nav leaf against fetched content. */
function DocsContentPane({
  active,
  content,
}: {
  active: string;
  content: DocsContent;
}) {
  switch (active) {
    case "authentication":
      return <AuthenticationSection />;
    case "rate-limits":
      return <RateLimitsSection rateLimit={content.rateLimit} />;
    case "endpoints":
      return <EndpointReferenceSection />;
    case "errors":
      return <ErrorsSection errors={content.errors} />;
    case "webhooks":
      return <WebhooksSection />;
    case "sdk-overview":
      return <SdksSection sdks={content.sdks} />;
    case "component-library":
      return <ComponentsSection components={content.components} />;
    case "recipes":
      return <PlaybooksSection playbooks={content.playbooks} />;
    case "skill-catalog":
      return <SkillsSection skills={content.skills} />;
    default:
      return (
        <GettingStartedSection
          samples={content.quickstartSamples}
          response={content.quickstartResponse}
        />
      );
  }
}

export function DeveloperDocs() {
  const { t } = useTranslation();
  const { tier } = useTier();
  const [active, setActive] = useState("quickstart");

  const navState = useAsync<DocsNavSection[]>(() => fetchDocsNav(), []);
  const { data: nav } = navState;
  const { isLoading, isEmpty } = useSectionFlags(navState);

  const { data: content } = useAsync<DocsContent>(
    () => fetchDocsContent(tier),
    [tier],
  );

  return (
    <div className="portal-docs">
      <aside className="portal-docs__sidebar">
        {isLoading && <DocsNavSkeleton />}
        {isEmpty && (
          <EmptyState
            size="compact"
            title={t("portal.docs.nav.empty.title")}
            description={t("portal.docs.nav.empty.description")}
          />
        )}
        {nav && nav.length > 0 && (
          <DocsNav sections={nav} active={active} onSelect={setActive} />
        )}
      </aside>

      <main className="portal-docs__content">
        {content && <DocsContentPane active={active} content={content} />}
      </main>
    </div>
  );
}
