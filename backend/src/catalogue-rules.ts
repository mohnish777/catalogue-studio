import type { Product } from "./catalogue.js";

const HOME_FEATURED_IDS = [1, 2, 5, 7, 9];

export interface CatalogueFilters {
  category?: string;
  query?: string;
}

export interface Catalogue {
  categories: string[];
  featured: Product[];
  remaining: Product[];
}

export function buildCatalogue(
  products: readonly Product[],
  { category, query = "" }: CatalogueFilters = {},
): Catalogue {
  const categories = [...new Set(products.map((product) => product.category))].sort();
  const search = query.trim().toLowerCase();
  const matching = products.filter((product) => {
    const matchesCategory = !category || product.category === category;
    const matchesSearch =
      product.title.toLowerCase().includes(search) ||
      product.description.toLowerCase().includes(search);
    return matchesCategory && matchesSearch;
  });

  // Search results use one list, without a separate featured section.
  if (search) {
    return { categories, featured: [], remaining: matching };
  }

  let featured: Product[];
  if (category) {
    // Reserve one item for the remaining list, even for small categories.
    const featuredCount = Math.min(2, Math.max(0, matching.length - 1));
    featured = [...matching]
      .sort((a, b) => b.rating - a.rating || a.id - b.id)
      .slice(0, featuredCount);
  } else {
    featured = HOME_FEATURED_IDS.flatMap((id) => {
      const product = matching.find((item) => item.id === id);
      return product ? [product] : [];
    });
  }

  const featuredIds = new Set(featured.map((product) => product.id));
  return {
    categories,
    featured,
    remaining: matching.filter((product) => !featuredIds.has(product.id)),
  };
}
