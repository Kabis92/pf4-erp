import type { HTMLAttributes } from 'react';
import { cn } from '@/lib/utils';

export function Badge({
  className,
  variant = 'secondary',
  ...props
}: HTMLAttributes<HTMLDivElement> & { variant?: 'secondary' | 'outline' }) {
  return (
    <div
      className={cn(
        'inline-flex items-center rounded-md border px-2 py-0.5 text-xs font-semibold transition-colors',
        variant === 'secondary' && 'border-transparent bg-secondary text-secondary-foreground',
        variant === 'outline' && 'text-foreground',
        className,
      )}
      {...props}
    />
  );
}
