import { ExtensionSlot } from '@/plugins/ExtensionSlot';

export function DashboardPage() {
  return (
    <div className="h-full overflow-y-auto p-6">
      <div className="mb-6">
        <h1 className="text-xl font-semibold">Dashboard</h1>
        <p className="text-sm text-muted-foreground">Vue d’ensemble et widgets des plugins</p>
      </div>
      <div className="grid gap-4 sm:grid-cols-2 xl:grid-cols-3">
        <ExtensionSlot name="dashboard.widgets" />
      </div>
    </div>
  );
}
