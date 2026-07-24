export function Footer() {
  return (
    <footer className="flex shrink-0 items-center justify-between border-t border-border bg-background px-6 py-3 text-xs text-muted-foreground">
      <span>&copy; {new Date().getFullYear()} ERP Suite. Tous droits réservés.</span>
      <span>v0.1.0</span>
    </footer>
  );
}
