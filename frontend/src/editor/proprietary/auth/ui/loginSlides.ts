import { defaultTranslate, type AuthTranslate } from "@editor/auth/types";
import type { ImageSlide } from "@editor/auth/ui/LoginRightCarousel";
import { loginSlideText } from "@editor/auth/ui/loginSlideText";
import firstPage from "@editor/assets/login/Firstpage.png";
import addToPdf from "@editor/assets/login/AddToPDF.png";
import securePdf from "@editor/assets/login/SecurePDF.png";

const SLIDE_TILT = { followMouseTilt: true, tiltMaxDeg: 5 } as const;

/**
 * Default login carousel slides using bundled images. The portal uses this set;
 * the editor builds its own (logo-variant hero) from loginSlideText.
 */
export function buildDefaultLoginSlides(
  translate: AuthTranslate = defaultTranslate,
): ImageSlide[] {
  const text = loginSlideText(translate);
  return [firstPage, addToPdf, securePdf].map((src, i) => ({
    src,
    ...text[i],
    ...SLIDE_TILT,
  }));
}
