import { Bell } from 'lucide-react';
import { Breadcrumb, type Crumb } from '@/components/ui/breadcrumb';
import { Button } from '@/components/ui/button';

export function Header({ crumbs }: { crumbs: Crumb[] }) {
  return (
    <header className="flex shrink-0 items-center justify-between border-b border-border bg-background px-6 py-4">
      <div className="min-w-0 flex-1">
        <Breadcrumb items={crumbs} />
      </div>
      <div className="flex shrink-0 items-center gap-2">
        <Button variant="ghost" size="icon" className="size-9" aria-label="Notifications">
          <Bell className="size-4" />
        </Button>
      </div>
    </header>
  );
}
