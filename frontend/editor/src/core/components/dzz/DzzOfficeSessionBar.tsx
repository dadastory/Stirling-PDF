import { useCallback, useContext, useEffect, useRef, useState } from "react";
import { Radio } from "@mantine/core";
import { Button } from "@app/ui/Button";
import { useFileState } from "@app/contexts/FileContext";
import { useViewer, ViewerContext } from "@app/contexts/ViewerContext";
import { isStirlingFile } from "@app/types/fileContext";
import {
  deleteDzzOfficeWorkspace,
  listDzzOfficeTemporaryPdfs,
  readDzzOfficeTemporaryPdf,
  saveDzzOfficePdf,
  storeDzzOfficeTemporaryPdf,
  type DzzOfficeWorkspaceResult,
} from "@app/services/dzzOfficeBridge";

/**
 * The only Dzz-specific save control. Tool exports are cached in the Dzz
 * workspace; this component is the sole path that creates a Dzz file version.
 */
export default function DzzOfficeSessionBar() {
  const { selectors } = useFileState();
  const { activeFileId } = useViewer();
  const viewer = useContext(ViewerContext);
  const files = selectors
    .getFiles()
    .filter((file) => file.type === "application/pdf" && isStirlingFile(file));
  const fileSignature = files
    .map((file) => `${file.fileId}:${file.size}:${file.lastModified}`)
    .join("|");
  const filesRef = useRef(files);
  filesRef.current = files;
  const storedFileIds = useRef(new Set<string>());
  const [results, setResults] = useState<DzzOfficeWorkspaceResult[]>([]);
  const [selected, setSelected] = useState("");
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState("");
  const inFlight = useRef(false);

  const refreshResults = useCallback(async () => {
    try {
      setResults(await listDzzOfficeTemporaryPdfs());
    } catch (cause) {
      setError(cause instanceof Error ? cause.message : "无法读取临时编辑结果");
    }
  }, []);

  useEffect(() => {
    void refreshResults();
    window.addEventListener("dzz-office-workspace-changed", refreshResults);
    return () => window.removeEventListener("dzz-office-workspace-changed", refreshResults);
  }, [refreshResults]);

  useEffect(() => {
    let cancelled = false;
    void (async () => {
      for (const file of filesRef.current) {
        if (storedFileIds.current.has(file.fileId)) continue;
        try {
          await storeDzzOfficeTemporaryPdf(file, file.name, file.fileId);
          if (!cancelled) storedFileIds.current.add(file.fileId);
        } catch (cause) {
          if (!cancelled) {
            setError(cause instanceof Error ? cause.message : "无法暂存 PDF 结果");
          }
        }
      }
      if (!cancelled) await refreshResults();
    })();
    return () => {
      cancelled = true;
    };
  }, [fileSignature, refreshResults]);

  useEffect(() => {
    if (!results.length) {
      if (selected) setSelected("");
      return;
    }
    if (results.some((result) => result.id === selected)) return;
    setSelected(
      results.some((result) => result.id === activeFileId)
        ? activeFileId!
        : results[results.length - 1]!.id,
    );
  }, [activeFileId, results, selected]);

  useEffect(() => {
    const cleanup = () => {
      void deleteDzzOfficeWorkspace();
    };
    window.addEventListener("pagehide", cleanup);
    return () => window.removeEventListener("pagehide", cleanup);
  }, []);

  const save = async () => {
    if (inFlight.current || !selected) return;
    const result = results.find((value) => value.id === selected);
    if (!result) return;

    inFlight.current = true;
    setSaving(true);
    setError("");
    try {
      const activeFile = files.find((file) => file.fileId === selected);
      let content: Blob | File;
      if (activeFile && activeFile.fileId === activeFileId && viewer?.exportActions?.saveAsCopy) {
        const exported = await viewer.exportActions.saveAsCopy();
        content = exported
          ? new Blob([exported], { type: "application/pdf" })
          : activeFile;
      } else {
        content = await readDzzOfficeTemporaryPdf(result);
      }
      await saveDzzOfficePdf(content, result.name);
      await deleteDzzOfficeWorkspace();
      window.location.assign("/");
    } catch (cause) {
      setError(cause instanceof Error ? cause.message : "保存 DzzOffice 新版本失败");
    } finally {
      inFlight.current = false;
      setSaving(false);
    }
  };

  return (
    <div className="dzz-office-session-bar">
      <strong>DzzOffice PDF</strong>
      <span>选择一个最终结果保存为 DzzOffice 新版本：</span>
      <Radio.Group
        value={selected}
        onChange={setSelected}
        aria-label="DzzOffice 临时 PDF 结果"
      >
        {results.map((result) => (
          <Radio
            key={result.id}
            value={result.id}
            label={result.name}
            disabled={saving}
          />
        ))}
      </Radio.Group>
      {error && <span role="alert">{error}</span>}
      <Button onClick={save} disabled={!selected || saving}>
        {saving ? "正在保存…" : "保存并返回 DzzOffice"}
      </Button>
    </div>
  );
}
