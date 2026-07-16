import React, { useMemo } from "react";
import { useTranslation } from "react-i18next";
import { AuthShell } from "@editor/auth/ui/AuthShell";
import LoginRightCarousel from "@editor/auth/ui/LoginRightCarousel";
import buildLoginSlides from "@editor/components/shared/loginSlides";
import { useLogoVariant } from "@editor/hooks/useLogoVariant";
import Footer from "@editor/components/shared/Footer";

interface AuthLayoutProps {
  children: React.ReactNode;
}

/**
 * Editor login layout. The card shell + carousel now live in shared so the
 * portal renders the identical screen; this wires the editor's logo-variant
 * slides and legal/cookie footer into that shared shell.
 */
export default function AuthLayout({ children }: AuthLayoutProps) {
  const { t } = useTranslation();
  const logoVariant = useLogoVariant();
  const imageSlides = useMemo(
    () => buildLoginSlides(logoVariant, t),
    [logoVariant, t],
  );

  return (
    <AuthShell
      rightPanel={
        <LoginRightCarousel
          imageSlides={imageSlides}
          initialSeconds={5}
          slideSeconds={8}
        />
      }
      footer={<Footer />}
    >
      {children}
    </AuthShell>
  );
}
