import { useSyncExternalStore } from 'react';
import { pluginRegistry } from './registry';

export function ExtensionSlot({ name }: { name: string }) {
  const contributions = useSyncExternalStore(
    (cb) => pluginRegistry.subscribe(cb),
    () => pluginRegistry.getExtensions(name),
  );

  if (contributions.length === 0) {
    return null;
  }

  return (
    <>
      {contributions.map((c) => {
        const Comp = c.component;
        return <Comp key={c.id} {...(c.props ?? {})} />;
      })}
    </>
  );
}
