import { useEffect } from "react";
import { useFileActions } from "@app/contexts/FileContext";
import { loadDzzOfficePdf } from "@app/services/dzzOfficeBridge";

/** Loads the one Dzz-authorized source PDF when Stirling is opened from DzzOffice. */
export function DzzOfficeBridgeBootstrap() {
  const { actions } = useFileActions();

  useEffect(() => {
    let cancelled = false;
    void loadDzzOfficePdf().then((file) => {
      if (!cancelled && file) {
        void actions.addFiles([file], { selectFiles: true });
      }
    });
    return () => {
      cancelled = true;
    };
  }, [actions]);

  return null;
}
