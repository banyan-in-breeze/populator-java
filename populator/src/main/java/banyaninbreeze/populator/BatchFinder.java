/*
 * Copyright (c) 2026 banyan-in-breeze
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */
package banyaninbreeze.populator;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Utility class designed to process collections of identifiers by splitting them into 
 * optimal sub-batches before executing a lookup function (typically a database query).
 * 
 * <p>The splitting algorithm balances two primary performance factors:
 * <ul>
 *   <li><b>Minimizing database trips:</b> By maximizing individual batch sizes up to the limit, 
 *       the total number of batch invocations (round trips) is kept as low as possible.</li>
 *   <li><b>Optimizing plan caching:</b> By preferring standardized, predefined batch sizes, 
 *       the variety of distinct SQL statement shapes/lengths is restricted, which helps 
 *       maximize database query plan cache efficiency and prevents plan bloat.</li>
 * </ul>
 * 
 * @param <T>         the type of objects being retrieved/found
 * @param <IDType>    the type of the unique identifiers used for lookup
 */
public class BatchFinder<T, IDType> {
    /**
     * Default ascendingly sorted list of standard batch sizes optimized for SQL plan caching 
     * and safe database parameter thresholds.
     */
    private static List<Integer> defaultBatchSizes = Collections.unmodifiableList(
                Arrays.asList(180, 240, 300, 360, 420, 480, 540, 600, 660, 720, 780, 840, 900, 960)
            );

    /**
     * Retrieves the global default batch sizes.
     * 
     * @return an unmodifiable, ascendingly sorted {@link List} of default batch sizes
     */
    public static List<Integer> getDefaultBatchSizes() {
        return defaultBatchSizes;
    }

    /**
     * Sets the global default batch sizes. 
     * The provided list is automatically sorted in ascending order before storing.
     * 
     * @param defaultBatchSizes the new {@link List} of batch sizes to be applied globally
     * @throws IllegalArgumentException if the provided list is {@code null}, empty, 
     *                                  or contains any batch size less than or equal to 0
     */
    public static void setDefaultBatchSizes(List<Integer> defaultBatchSizes) {
        if (defaultBatchSizes == null || defaultBatchSizes.size() == 0)
            throw new IllegalArgumentException("defaultBatchSizes must not be null and empty");
        for (Integer size : defaultBatchSizes) {
            if (size <= 0)
                throw new IllegalArgumentException("The batch sizes must be greater than 0");
        }
        BatchFinder.defaultBatchSizes = defaultBatchSizes.stream().distinct().sorted().collect(Collectors.toList());
    }
    
    /**
     * Filters the existing default batch sizes to exclude any size greater than the specified maximum.
     * If all existing sizes exceed the threshold, the list will fall back to a single-element list 
     * containing only the specified maximum size.
     * 
     * @param maxDefaultBatchSize the upper boundary threshold for the batch sizes
     * @throws IllegalArgumentException if {@code maxDefaultBatchSize} is less than or equal to 0
     */
    public static void setMaxDefaultBatchSize(int maxDefaultBatchSize) {
        if (maxDefaultBatchSize <= 0)
            throw new IllegalArgumentException("maxDefaultBatchSize must be greater than 0");
        List<Integer> batchSizes = defaultBatchSizes.stream().filter(s -> s <= maxDefaultBatchSize).collect(Collectors.toList());
        if (batchSizes.isEmpty())
            batchSizes = Arrays.asList(maxDefaultBatchSize);
        BatchFinder.defaultBatchSizes = batchSizes;
    }

    /**
     * Finds and aggregates objects for the given collection of identifiers using the default batch sizes.
     * Duplicate identifiers are automatically filtered out.
     *
     * @param ids           the collection of identifiers to look up
     * @param objsFinder    the function that executes the lookup for a single batch of identifiers
     * @param <T>           the type of output objects
     * @param <IDType>      the type of the identifier
     * @return an unmodifiable list of all retrieved objects combined from all batches
     */
    public static <T, IDType> List<T> find(Collection<IDType> ids,
            Function<Collection<IDType>, Collection<T>> objsFinder) {
        return find(defaultBatchSizes, ids, objsFinder);
    }
    
    /**
     * Finds and aggregates objects for the given collection of identifiers, constrained by a 
     * custom maximum batch size limit. Filters the default batch size tier to only include 
     * sizes less than or equal to {@code maxBatchSize}.
     *
     * @param maxBatchSize  the upper bound limit for any single batch size
     * @param ids           the collection of identifiers to look up
     * @param objsFinder    the function that executes the lookup for a single batch of identifiers
     * @param <T>           the type of output objects
     * @param <IDType>      the type of the identifier
     * @return an unmodifiable list of all retrieved objects combined from all batches
     */
    public static <T, IDType> List<T> find(int maxBatchSize, Collection<IDType> ids,
            Function<Collection<IDType>, Collection<T>> objsFinder) {
        List<Integer> batchSizes = defaultBatchSizes.stream().filter(s -> s <= maxBatchSize).collect(Collectors.toList());
        if (batchSizes.isEmpty())
            batchSizes = Arrays.asList(maxBatchSize);
        return find(batchSizes, ids, objsFinder);
    }
    
    /**
     * Finds and aggregates objects for a collection of identifiers using an explicitly provided list 
     * of permissible batch sizes. Distinct identifiers are partitioned and processed sequentially through 
     * the provided finder function.
     *
     * @param batchSizes    the allowed batch sizes (must be sorted in ascending order)
     * @param ids           the collection of identifiers to look up
     * @param objsFinder    the function that executes the lookup for a single batch of identifiers
     * @param <T>           the type of output objects
     * @param <IDType>      the type of the identifier
     * @return an unmodifiable list of all retrieved objects combined from all batches
     */
    public static <T, IDType> List<T> find(List<Integer> batchSizes, Collection<IDType> ids,
            Function<Collection<IDType>, Collection<T>> objsFinder) {
        if (ids == null || ids.isEmpty())
            return Collections.emptyList();
        
        List<T> result = new ArrayList<T>();
        List<List<IDType>> idBatches = divideIdsIntoBatches(
                    ids.stream().distinct().collect(Collectors.toList()), batchSizes);
        
        for (List<IDType> idBatch : idBatches) {
            Collection<T> oneBatchResult = objsFinder.apply(idBatch);
            if (oneBatchResult != null)
                result.addAll(oneBatchResult);
        }
        return Collections.unmodifiableList(result);
    }
    
    /**
     * Divides a list of IDs into smaller batches based on a preferred hierarchy of 
     * batch sizes. 
     * <p>
     * The method iterates through the remaining IDs and attempts to match the largest 
     * available batch size from the provided {@code batchSizes} list (assumed to be 
     * sorted or evaluated from largest to smallest/preferred order). If the remaining 
     * number of IDs is smaller than all preferred batch sizes, it falls back to adding 
     * the remainder as the final batch.
     * </p>
     * 
     * @param <IDType>   the type of elements contained in the ID list
     * @param ids        the master list of IDs to be split into batches
     * @param batchSizes a list of acceptable or target batch sizes (e.g., {@code [10, 50, 100]})
     * @return a list of lists, where each inner list represents an individual ID batch
     * 
     * @example
     * <pre>
     * List&lt;Integer&gt; ids = Arrays.asList(1, 2, 3, 4, 5, 6, 7);
     * List&lt;Integer&gt; batchSizes = Arrays.asList(2, 5); 
     * // Iterating or checking from largest to smallest element in batchSizes:
     * // - 7 IDs left: fits preferred size 5 -> batch of 5 [1, 2, 3, 4, 5]
     * // - 2 IDs left: fits preferred size 2 -> batch of 2 [6, 7]
     * List&lt;List&lt;Integer&gt;&gt; batches = divideIdsIntoBatches(ids, batchSizes);
     * // Result: [[1, 2, 3, 4, 5], [6, 7]]
     * </pre>
     */
    public static <IDType> List<List<IDType>> divideIdsIntoBatches(List<IDType> ids, List<Integer> batchSizes) {
        if (ids == null || ids.isEmpty())
            return Arrays.asList();
        if (batchSizes == null || batchSizes.isEmpty())
            return Arrays.asList(ids);
        for (Integer size : batchSizes)
            if (size <= 0)
                throw new IllegalArgumentException("Batch size cannot be 0 or negative.");
        List<List<IDType>> result = new ArrayList<>();
        int startIndex = 0;
        while (startIndex < ids.size()) {
            int numLeft = ids.size() - startIndex;
            int sizeToAdd = 0;
            for (int i = batchSizes.size() - 1; i >= 0; i--) {
                if (numLeft >= batchSizes.get(i)) {
                    sizeToAdd = batchSizes.get(i);
                } else if (i == 0) {
                    sizeToAdd = numLeft;
                }
                if (sizeToAdd > 0) {
                    result.add(new ArrayList<>(ids.subList(startIndex, sizeToAdd + startIndex)));
                    startIndex += sizeToAdd;
                    break;
                }
            }
        }
        return result;
    }
}
