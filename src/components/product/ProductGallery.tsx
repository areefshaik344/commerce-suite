import { useState } from "react";
import { cn } from "@/lib/utils";

interface Props {
  images: string[];
  alt: string;
}

export function ProductGallery({ images, alt }: Props) {
  const [active, setActive] = useState(0);
  const [zoom, setZoom] = useState<{ x: number; y: number } | null>(null);

  if (!images.length) {
    return <div className="aspect-square bg-muted rounded-lg" />;
  }

  const handleMove = (e: React.MouseEvent<HTMLDivElement>) => {
    const rect = e.currentTarget.getBoundingClientRect();
    setZoom({
      x: ((e.clientX - rect.left) / rect.width) * 100,
      y: ((e.clientY - rect.top) / rect.height) * 100,
    });
  };

  return (
    <div className="flex gap-3 flex-col-reverse sm:flex-row">
      {images.length > 1 && (
        <div className="flex sm:flex-col gap-2 overflow-x-auto sm:overflow-x-visible">
          {images.map((src, i) => (
            <button
              key={i}
              onClick={() => setActive(i)}
              onMouseEnter={() => setActive(i)}
              className={cn(
                "h-14 w-14 sm:h-16 sm:w-16 rounded-md overflow-hidden border-2 shrink-0 transition-colors",
                i === active ? "border-primary" : "border-transparent hover:border-muted-foreground/30"
              )}
              aria-label={`View image ${i + 1}`}
            >
              <img src={src} alt={`${alt} thumbnail ${i + 1}`} className="h-full w-full object-cover" loading="lazy" />
            </button>
          ))}
        </div>
      )}
      <div
        className="relative flex-1 aspect-square rounded-lg overflow-hidden bg-muted/30 group cursor-zoom-in"
        onMouseMove={handleMove}
        onMouseLeave={() => setZoom(null)}
      >
        <img
          src={images[active]}
          alt={alt}
          className="h-full w-full object-cover transition-transform duration-300"
          style={zoom ? { transformOrigin: `${zoom.x}% ${zoom.y}%`, transform: "scale(1.8)" } : undefined}
          loading="eager"
          decoding="async"
        />
      </div>
    </div>
  );
}