import { useEffect, useState } from 'react';
import { getMarketplaceTemplates, type MarketplaceTemplate } from '../lib/api';

export function useMarketplaceTemplates() {
  const [templates, setTemplates] = useState<MarketplaceTemplate[]>([]);
  const [isLoading, setIsLoading] = useState(true);

  useEffect(() => {
    let mounted = true;

    getMarketplaceTemplates()
      .then((data) => {
        if (mounted) {
          setTemplates(data);
        }
      })
      .catch(() => {
        // Silently fail - marketplace buttons just won't show
      })
      .finally(() => {
        if (mounted) {
          setIsLoading(false);
        }
      });

    return () => {
      mounted = false;
    };
  }, []);

  return { templates, isLoading };
}
