import {
  tableFeatures,
  columnFilteringFeature,
  rowSortingFeature,
  rowPaginationFeature,
  createCoreRowModel,
  createFilteredRowModel,
  createSortedRowModel,
  createPaginatedRowModel,
} from "@tanstack/react-table";

export const features = tableFeatures({
  columnFilteringFeature,
  rowSortingFeature,
  rowPaginationFeature,
  coreRowModel: createCoreRowModel(),
  filteredRowModel: createFilteredRowModel(),
  sortedRowModel: createSortedRowModel(),
  paginatedRowModel: createPaginatedRowModel(),
});

export type AppFeatures = typeof features;
