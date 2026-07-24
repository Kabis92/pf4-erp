import { cn } from '@/lib/utils';

export function Avatar({
  fallback,
  className,
}: {
  fallback: string;
  className?: string;
}) {
  return (
    <div
      className={cn(
        'flex size-8 shrink-0 items-center justify-center rounded-full bg-muted text-xs font-medium',
        className,
      )}
    >
      {fallback}
    </div>
  );
}
