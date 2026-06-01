import { useEffect, useRef, useState } from "react";
import { useNavigate } from "react-router-dom";
import { Search, X, Clock, TrendingUp, Sparkles } from "lucide-react";
import { Input } from "@/components/ui/input";
import { productApi } from "@/api/productApi";
import { useStore } from "@/store/useStore";

const TRENDING = ["iPhone 15", "Nike shoes", "MacBook", "Headphones", "PlayStation 5"];
const DEBOUNCE_MS = 250;

function highlight(text: string, query: string) {
  if (!query) return text;
  const idx = text.toLowerCase().indexOf(query.toLowerCase());
  if (idx < 0) return text;
  return (
    <>
      {text.slice(0, idx)}
      <mark className="bg-primary/20 text-foreground rounded px-0.5">{text.slice(idx, idx + query.length)}</mark>
      {text.slice(idx + query.length)}
    </>
  );
}

export function ProductSearchBar({ initialValue = "" }: { initialValue?: string }) {
  const navigate = useNavigate();
  const ref = useRef<HTMLDivElement>(null);
  const { searchHistory, addToSearchHistory, clearSearchHistory } = useStore();
  const [value, setValue] = useState(initialValue);
  const [suggestions, setSuggestions] = useState<string[]>([]);
  const [open, setOpen] = useState(false);

  // Debounced suggestions fetch.
  useEffect(() => {
    if (value.length < 2) { setSuggestions([]); return; }
    const t = setTimeout(async () => {
      const res = await productApi.searchSuggestions(value);
      setSuggestions(res.data);
    }, DEBOUNCE_MS);
    return () => clearTimeout(t);
  }, [value]);

  useEffect(() => {
    const handler = (e: MouseEvent) => {
      if (ref.current && !ref.current.contains(e.target as Node)) setOpen(false);
    };
    document.addEventListener("mousedown", handler);
    return () => document.removeEventListener("mousedown", handler);
  }, []);

  const submit = (q: string) => {
    const term = q.trim();
    if (!term) return;
    addToSearchHistory(term);
    setOpen(false);
    navigate(`/products?q=${encodeURIComponent(term)}`);
  };

  return (
    <div ref={ref} className="relative w-full max-w-xl">
      <form onSubmit={(e) => { e.preventDefault(); submit(value); }}>
        <Search className="absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-muted-foreground" />
        <Input
          value={value}
          onChange={(e) => { setValue(e.target.value); setOpen(true); }}
          onFocus={() => setOpen(true)}
          placeholder="Search products, brands and more..."
          className="pl-10 pr-9 bg-card border-border"
        />
        {value && (
          <button
            type="button"
            onClick={() => { setValue(""); setSuggestions([]); }}
            className="absolute right-3 top-1/2 -translate-y-1/2"
            aria-label="Clear search"
          >
            <X className="h-4 w-4 text-muted-foreground hover:text-foreground" />
          </button>
        )}
      </form>

      {open && (
        <div className="absolute top-full left-0 right-0 z-50 mt-1 rounded-lg border bg-card shadow-elevated overflow-hidden">
          {value.length >= 2 ? (
            suggestions.length ? (
              <ul className="py-1">
                {suggestions.map((s) => (
                  <li key={s}>
                    <button onClick={() => submit(s)} className="flex w-full items-center gap-2 px-3 py-2 text-sm hover:bg-muted text-left">
                      <Sparkles className="h-3.5 w-3.5 text-primary shrink-0" />
                      <span>{highlight(s, value)}</span>
                    </button>
                  </li>
                ))}
              </ul>
            ) : (
              <p className="px-4 py-6 text-sm text-muted-foreground text-center">No matches for "{value}"</p>
            )
          ) : (
            <>
              {searchHistory.length > 0 && (
                <div className="p-3 border-b">
                  <div className="flex items-center justify-between mb-2">
                    <span className="text-xs font-medium text-muted-foreground flex items-center gap-1">
                      <Clock className="h-3 w-3" /> Recent
                    </span>
                    <button onClick={clearSearchHistory} className="text-[10px] text-muted-foreground hover:text-destructive">Clear</button>
                  </div>
                  <div className="flex flex-wrap gap-1.5">
                    {searchHistory.slice(0, 6).map((q) => (
                      <button key={q} onClick={() => submit(q)} className="text-xs px-2.5 py-1 rounded-full bg-muted hover:bg-muted/80">
                        {q}
                      </button>
                    ))}
                  </div>
                </div>
              )}
              <div className="p-3">
                <span className="text-xs font-medium text-muted-foreground flex items-center gap-1 mb-2">
                  <TrendingUp className="h-3 w-3" /> Trending
                </span>
                <div className="space-y-0.5">
                  {TRENDING.map((q) => (
                    <button key={q} onClick={() => submit(q)} className="flex w-full items-center gap-2 px-2 py-1.5 text-left text-sm hover:bg-muted rounded">
                      <TrendingUp className="h-3 w-3 text-secondary shrink-0" />
                      <span>{q}</span>
                    </button>
                  ))}
                </div>
              </div>
            </>
          )}
        </div>
      )}
    </div>
  );
}