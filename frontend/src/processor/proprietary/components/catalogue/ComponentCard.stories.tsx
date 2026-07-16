import type { Meta, StoryObj } from "@storybook/react-vite";
import { componentsFor } from "@processor/mocks/sdkComponents";
import { ComponentCard } from "@processor/components/catalogue/ComponentCard";

const PRO = componentsFor("pro");
const GA = PRO.find((c) => c.maturity === "ga")!;
const BETA = PRO.find((c) => c.maturity === "beta")!;

const meta: Meta<typeof ComponentCard> = {
  title: "Portal/Components/ComponentCard",
  component: ComponentCard,
  parameters: { layout: "padded" },
  args: { component: GA, unlocked: true, onOpen: () => {} },
};
export default meta;
type Story = StoryObj<typeof ComponentCard>;

export const GeneralAvailability: Story = {};

export const Beta: Story = {
  args: { component: BETA },
};

/** Sits above the tier — dimmed with a lock affordance. */
export const Locked: Story = {
  args: { component: BETA, unlocked: false },
};
