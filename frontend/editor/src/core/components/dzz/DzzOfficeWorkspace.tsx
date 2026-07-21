import { Group } from "@mantine/core";
import Workbench from "@app/components/layout/Workbench";
import RightSidebar from "@app/components/tools/RightSidebar";

export default function DzzOfficeWorkspace() {
  return <Group align="flex-start" gap={0} h="100%" className="flex-nowrap flex"><Workbench /><RightSidebar /></Group>;
}
