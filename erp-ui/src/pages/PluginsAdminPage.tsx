import { useCallback, useEffect, useRef, useState } from 'react';
import {
  BookOpen,
  CheckCircle2,
  Loader2,
  Pause,
  Play,
  Puzzle,
  RefreshCw,
  Trash2,
  Upload,
} from 'lucide-react';
import { getToken } from '@/lib/auth';
import { Badge } from '@/components/ui/badge';
import { Button } from '@/components/ui/button';

export type AdminPlugin = {
  id: string;
  version: string;
  displayName: string;
  description: string;
  requires: string;
  started: boolean;
  deletable: boolean;
  apiDocsEnabled: boolean;
  apiDocsGroup: string | null;
  apiDocsUrl: string | null;
  entry: string;
  uiSlots: string[];
};

async function adminFetch<T>(path: string, init?: RequestInit): Promise<T> {
  const token = getToken();
  const res = await fetch(path, {
    ...init,
    headers: {
      ...(token ? { Authorization: `Bearer ${token}` } : {}),
      ...init?.headers,
    },
  });
  if (!res.ok) {
    let message = `HTTP ${res.status}`;
    try {
      const body = (await res.json()) as { message?: string };
      if (body.message) {
        message = body.message;
      }
    } catch {
      // ignore
    }
    throw new Error(message);
  }
  if (res.status === 204) {
    return undefined as T;
  }
  return (await res.json()) as T;
}

function swaggerHref(plugin: AdminPlugin) {
  if (!plugin.apiDocsEnabled || !plugin.apiDocsUrl) {
    return null;
  }
  return plugin.apiDocsUrl;
}

export function PluginsAdminPage() {
  const fileRef = useRef<HTMLInputElement>(null);
  const [plugins, setPlugins] = useState<AdminPlugin[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [success, setSuccess] = useState<string | null>(null);
  const [busyId, setBusyId] = useState<string | null>(null);
  const [importing, setImporting] = useState(false);
  const [dragOver, setDragOver] = useState(false);
  const [lastImportedId, setLastImportedId] = useState<string | null>(null);

  const load = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      setPlugins(await adminFetch<AdminPlugin[]>('/api/admin/plugins'));
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Chargement impossible');
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    void load();
  }, [load]);

  async function runAction(pluginId: string, action: () => Promise<void>, successMessage: string) {
    setBusyId(pluginId);
    setError(null);
    setSuccess(null);
    try {
      await action();
      setSuccess(successMessage);
      await load();
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Action impossible');
    } finally {
      setBusyId(null);
    }
  }

  async function importJar(file: File) {
    if (!file.name.toLowerCase().endsWith('.jar')) {
      setError('Sélectionnez un fichier .jar');
      return;
    }
    setImporting(true);
    setError(null);
    setSuccess(null);
    setLastImportedId(null);
    try {
      const form = new FormData();
      form.append('file', file);
      const token = getToken();
      const res = await fetch('/api/admin/plugins/import', {
        method: 'POST',
        headers: token ? { Authorization: `Bearer ${token}` } : {},
        body: form,
      });
      if (!res.ok) {
        let message = `HTTP ${res.status}`;
        try {
          const body = (await res.json()) as { message?: string };
          if (body.message) {
            message = body.message;
          }
        } catch {
          // ignore
        }
        throw new Error(message);
      }
      const imported = (await res.json()) as AdminPlugin;
      setLastImportedId(imported.id);
      setSuccess(
        `Import réussi : « ${imported.displayName} » (v${imported.version}) est chargé${
          imported.started ? ' et actif' : ''
        }. Vous pouvez maintenant le gérer.`,
      );
      await load();
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Import impossible');
    } finally {
      setImporting(false);
      if (fileRef.current) {
        fileRef.current.value = '';
      }
    }
  }

  function onFileChange(files: FileList | null) {
    const file = files?.[0];
    if (file) {
      void importJar(file);
    }
  }

  return (
    <div className="relative flex h-full flex-col overflow-hidden p-6">
      {importing && (
        <div className="absolute inset-0 z-20 flex flex-col items-center justify-center gap-3 bg-background/80 backdrop-blur-sm">
          <Loader2 className="size-10 animate-spin text-foreground" />
          <div className="text-center">
            <p className="text-sm font-medium">Import du plugin en cours…</p>
            <p className="mt-1 text-xs text-muted-foreground">
              Upload, chargement PF4J et démarrage du module
            </p>
          </div>
        </div>
      )}

      <div className="mb-5 flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
        <div>
          <h1 className="text-xl font-semibold">Plugins</h1>
          <p className="text-sm text-muted-foreground">
            Importer, activer, désactiver ou supprimer les modules. Réservé aux administrateurs.
          </p>
        </div>
        <div className="flex flex-wrap items-center gap-2">
          <Button variant="outline" size="sm" onClick={() => void load()} disabled={loading || importing}>
            <RefreshCw className={`size-4 ${loading ? 'animate-spin' : ''}`} />
            Actualiser
          </Button>
          <Button
            size="sm"
            disabled={importing}
            onClick={() => fileRef.current?.click()}
          >
            <Upload className="size-4" />
            Importer un JAR
          </Button>
          <input
            ref={fileRef}
            type="file"
            accept=".jar,application/java-archive"
            className="hidden"
            onChange={(e) => onFileChange(e.target.files)}
          />
        </div>
      </div>

      <div
        className={`mb-4 rounded-xl border border-dashed px-5 py-6 transition-colors ${
          dragOver ? 'border-foreground bg-muted/50' : 'border-border bg-card'
        }`}
        onDragOver={(e) => {
          e.preventDefault();
          setDragOver(true);
        }}
        onDragLeave={() => setDragOver(false)}
        onDrop={(e) => {
          e.preventDefault();
          setDragOver(false);
          onFileChange(e.dataTransfer.files);
        }}
      >
        <div className="flex flex-col items-center gap-2 text-center sm:flex-row sm:text-left">
          <div className="flex size-10 items-center justify-center rounded-lg bg-muted">
            <Upload className="size-5" />
          </div>
          <div className="min-w-0 flex-1">
            <p className="text-sm font-medium">Glissez-déposez un plugin (.jar)</p>
            <p className="text-xs text-muted-foreground">
              Le fichier est copié dans <code className="text-xs">./plugins</code>, chargé puis démarré
              automatiquement.
            </p>
          </div>
          <Button
            variant="outline"
            size="sm"
            disabled={importing}
            onClick={() => fileRef.current?.click()}
          >
            Choisir un fichier
          </Button>
        </div>
      </div>

      {success && (
        <div className="mb-4 flex items-start gap-2 rounded-md border border-border bg-muted/40 px-3 py-2 text-sm">
          <CheckCircle2 className="mt-0.5 size-4 shrink-0 text-foreground" />
          <div className="min-w-0 flex-1">
            <p>{success}</p>
            <button
              type="button"
              className="mt-1 text-xs text-muted-foreground underline hover:text-foreground"
              onClick={() => window.location.reload()}
            >
              Recharger le shell pour synchroniser menus / routes
            </button>
          </div>
        </div>
      )}

      {error && (
        <div className="mb-4 rounded-md border border-destructive/30 bg-destructive/10 px-3 py-2 text-sm text-destructive">
          {error}
        </div>
      )}

      <div className="min-h-0 flex-1 overflow-y-auto rounded-xl border border-border bg-card shadow-sm">
        {loading && !importing && (
          <div className="flex items-center justify-center gap-2 px-6 py-16 text-sm text-muted-foreground">
            <Loader2 className="size-4 animate-spin" />
            Chargement…
          </div>
        )}
        {!loading && plugins.length === 0 && (
          <div className="px-6 py-16 text-center text-sm text-muted-foreground">
            Aucun plugin chargé — importez un JAR pour commencer.
          </div>
        )}
        {!loading &&
          plugins.map((plugin) => {
            const docs = swaggerHref(plugin);
            const busy = busyId === plugin.id || importing;
            const highlight = lastImportedId === plugin.id;
            return (
              <div
                key={plugin.id}
                className={`flex flex-col gap-4 border-b border-border px-5 py-4 last:border-b-0 sm:flex-row sm:items-center sm:justify-between ${
                  highlight ? 'bg-muted/30' : ''
                }`}
              >
                <div className="flex min-w-0 items-start gap-3">
                  <div className="mt-0.5 flex size-9 shrink-0 items-center justify-center rounded-lg bg-muted">
                    <Puzzle className="size-4" />
                  </div>
                  <div className="min-w-0">
                    <div className="flex flex-wrap items-center gap-2">
                      <p className="truncate text-sm font-semibold">{plugin.displayName}</p>
                      <Badge variant={plugin.started ? 'secondary' : 'outline'}>
                        {plugin.started ? 'Actif' : 'Inactif'}
                      </Badge>
                      {highlight && <Badge variant="outline">Importé</Badge>}
                      <span className="text-xs text-muted-foreground">v{plugin.version}</span>
                    </div>
                    <p className="mt-0.5 text-sm text-muted-foreground">{plugin.description || plugin.id}</p>
                    <p className="mt-1 text-xs text-muted-foreground">
                      id: {plugin.id}
                      {plugin.uiSlots?.length ? ` · slots: ${plugin.uiSlots.join(', ')}` : ''}
                      {!plugin.deletable ? ' · non supprimable (dev)' : ''}
                    </p>
                  </div>
                </div>

                <div className="flex shrink-0 flex-wrap items-center gap-2">
                  {docs && (
                    <Button variant="outline" size="sm" asChild>
                      <a href={docs} target="_blank" rel="noreferrer" title="Documentation API Swagger">
                        <BookOpen className="size-4" />
                        API Docs
                      </a>
                    </Button>
                  )}
                  {plugin.started ? (
                    <Button
                      variant="outline"
                      size="sm"
                      disabled={busy}
                      onClick={() =>
                        void runAction(
                          plugin.id,
                          async () => {
                            await adminFetch(`/api/admin/plugins/${plugin.id}/stop`, { method: 'POST' });
                          },
                          `Plugin « ${plugin.displayName} » désactivé.`,
                        )
                      }
                    >
                      {busyId === plugin.id ? <Loader2 className="size-4 animate-spin" /> : <Pause className="size-4" />}
                      Désactiver
                    </Button>
                  ) : (
                    <Button
                      size="sm"
                      disabled={busy}
                      onClick={() =>
                        void runAction(
                          plugin.id,
                          async () => {
                            await adminFetch(`/api/admin/plugins/${plugin.id}/start`, { method: 'POST' });
                          },
                          `Plugin « ${plugin.displayName} » activé.`,
                        )
                      }
                    >
                      {busyId === plugin.id ? <Loader2 className="size-4 animate-spin" /> : <Play className="size-4" />}
                      Activer
                    </Button>
                  )}
                  <Button
                    variant="outline"
                    size="sm"
                    disabled={busy || !plugin.deletable}
                    className="text-destructive hover:text-destructive"
                    onClick={() => {
                      if (!window.confirm(`Supprimer le plugin « ${plugin.displayName} » ?`)) {
                        return;
                      }
                      void runAction(
                        plugin.id,
                        async () => {
                          await adminFetch(`/api/admin/plugins/${plugin.id}`, { method: 'DELETE' });
                        },
                        `Plugin « ${plugin.displayName} » supprimé.`,
                      );
                    }}
                  >
                    {busyId === plugin.id ? <Loader2 className="size-4 animate-spin" /> : <Trash2 className="size-4" />}
                    Supprimer
                  </Button>
                </div>
              </div>
            );
          })}
      </div>
    </div>
  );
}
