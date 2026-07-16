import { BASE_PATH } from "@editor/constants/app";
import { getLogoFolder } from "@editor/constants/logo";
import type { LogoVariant } from "@editor/services/preferencesService";
import type { TFunction } from "i18next";
import { loginSlideText } from "@editor/auth/ui/loginSlideText";
import type { ImageSlide } from "@editor/auth/ui/LoginRightCarousel";
import addToPdf from "@editor/assets/login/AddToPDF.png";
import securePdf from "@editor/assets/login/SecurePDF.png";

const SLIDE_TILT = { followMouseTilt: true, tiltMaxDeg: 5 } as const;

/**
 * Editor login carousel slides. Copy comes from the shared set (so the editor
 * and portal carousels stay in sync) and the edit/secure images are the shared
 * bundled assets; only the hero is the logo-variant image the editor serves
 * from /public.
 */
export const buildLoginSlides = (
  variant: LogoVariant | null | undefined,
  t: TFunction,
): ImageSlide[] => {
  const folder = getLogoFolder(variant);
  const text = loginSlideText((key, fallback) => t(key, fallback));
  const srcs = [`${BASE_PATH}/${folder}/Firstpage.png`, addToPdf, securePdf];
  return srcs.map((src, i) => ({ src, ...text[i], ...SLIDE_TILT }));
};

export default buildLoginSlides;
