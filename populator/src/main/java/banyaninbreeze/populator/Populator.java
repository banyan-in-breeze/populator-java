/*
 * Copyright (c) 2026 banyan-in-breeze
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */
package banyaninbreeze.populator;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * A util class that helps stitch objects from database queries, API calls or other sources together.
 * 
 * @param <P> The Parent model type being populated.
 */
public class Populator<P> {
    private static volatile ExecutorService globalExecutor = Executors.newCachedThreadPool();
    
    private final ExecutorService executor;
    private Collection<P> parents;
    private List<PopulateParams<P, ?, ?>> paramsList;
    
    private Populator(ExecutorService executor, Collection<P> parents) {
        this.parents = parents;
        this.paramsList = new ArrayList<>();
        this.executor = executor;
    }

    /**
     * Updates the global executor service used by all Populator instances by default.
     * Use this at application startup to configure custom thread pools or upgrade to virtual threads.
     *
     * @param executor The non-null custom ExecutorService to use globally.
     * @throws IllegalArgumentException if the provided executor is null.
     */
    public static void setGlobalExecutor(ExecutorService executor) {
        if (executor == null) {
            throw new IllegalArgumentException("Global executor cannot be null");
        }
        Populator.globalExecutor = executor;
    }

    /**
     * Get the global executor
     * 
     * @return the global executor
     */
    public static ExecutorService getGlobalExecutor() {
        return Populator.globalExecutor;
    }

    /**
     * Shortcut method to immediately execute a single population rule parameter configuration 
     * on a collection of parents using a single line of code.
     * 
     * <p>This utility wraps {@code of()}, {@code populate()}, and {@code run()} into one transaction 
     * execution block on the calling thread.</p>
     *
     * @param <P>       The Parent model object type.
     * @param <C>       The Child object model type being fetched.
     * @param <ID>      The unique identifier key type connecting Parent and Child.
     * @param parents   The collection of parent objects to populate.
     * @param params A pre-configured PopulateParams instance defining the relationship mapping rules.
     * @return An unmodifiable List of successfully processed and populated Parent objects.
     */
    public static <P, C, ID> List<P> populate(Collection<P> parents, PopulateParams<P, C, ID> params) {
        return Populator.of(parents).populate(params).run();
    }
    
    /**
     * Shortcut method to immediately configure and execute a One-to-One child population 
     * relationship workflow rule on a collection of parents using a single line of code.
     *
     * @param <P>                  The Parent model object type.
     * @param <C>                  The Child object model type.
     * @param <ID>                 The unique identifier key type.
     * @param parents              The collection of parent objects to populate.
     * @param parentIdGetter       Lambda method reference to read the unique join key from the Parent.
     * @param childIdGetter        Lambda method reference to read the unique join key from the fetched Child.
     * @param childFinder          Lambda function that batch-fetches a Collection of Children using a Collection of unique keys.
     * @param parentOneChildSetter Lambda function to map and save a single Child reference back onto its matching Parent.
     * @return An unmodifiable List of successfully processed and populated Parent objects.
     */
    public static <P, C, ID> List<P> populateOne(
            Collection<P> parents,
            Function<P, ID> parentIdGetter,
            Function<C, ID> childIdGetter,
            Function<Collection<ID>, Collection<C>> childFinder,
            BiFunction<P, C, P> parentOneChildSetter
    ) {
        return Populator.of(parents)
                .populateOne(parentIdGetter, childIdGetter, childFinder, parentOneChildSetter)
                .run();
    }
    
    /**
     * Shortcut method to immediately configure and execute a One-to-Many children list population 
     * relationship workflow rule on a collection of parents using a single line of code.
     *
     * @param <P>                   The Parent model object type.
     * @param <C>                   The Child object model type.
     * @param <ID>                  The unique identifier key type.
     * @param parents               The collection of parent objects to populate.
     * @param parentIdGetter        Lambda method reference to read the unique join key from the Parent.
     * @param childIdGetter         Lambda method reference to read the unique join key from the fetched Child.
     * @param childFinder           Lambda function that batch-fetches a Collection of Children using a Collection of unique keys.
     * @param parentManyChildSetter Lambda function to map and save a List of Children back onto their matching Parent.
     * @return An unmodifiable List of successfully processed and populated Parent objects.
     */
    public static <P, C, ID> List<P> populateMany(
            Collection<P> parents,
            Function<P, ID> parentIdGetter,
            Function<C, ID> childIdGetter,
            Function<Collection<ID>, Collection<C>> childFinder,
            BiFunction<P, List<C>, P> parentManyChildSetter
    ) {
        return Populator.of(parents)
                .populateMany(parentIdGetter, childIdGetter, childFinder, parentManyChildSetter)
                .run();
    }
    
    /**
     * A standalone utility helper to collect unique keys from a list of objects 
     * and invoke a targeted query function to retrieve associated child matches.
     *
     * @param <T>                The Source data model object type.
     * @param <U>                The Associated target object type being looked up.
     * @param <ID>               The unique connecting key field type.
     * @param objects            The root source data collection to scan.
     * @param idGetter           Lambda rule to extract the connecting identifier key from a source object.
     * @param assocObjectsfinder Lambda repository rule that accepts a Set of keys and returns matching target models.
     * @return A clean List of successfully found associated target items.
     */
    public static <T, U, ID> List<U> findAssociatedObjects(
            Collection<T> objects,
            Function<T, ID> idGetter,
            Function<Collection<ID>, Collection<U>> assocObjectsfinder
    ) {
        if (objects == null || objects.isEmpty())
            return Collections.emptyList();
        Set<ID> ids = objects.stream()
                .map(o -> idGetter.apply(o))
                .filter(id -> id != null)
                .collect(Collectors.toSet());
        return ids.isEmpty() ? Collections.emptyList() : Collections.unmodifiableList(new ArrayList<>(assocObjectsfinder.apply(ids)));
    }
    
    /**
     * Converts a collection to a one-to-one map using a key extractor function. If there are duplicate keys 
     * extracted from the collection, the first element encountered is retained.
     * <p>
     * Null keys extracted by the {@code idGetter} function are filtered out. If the input 
     * collection is null, an empty map is returned.
     *
     * @param <T>        the type of elements in the collection
     * @param <ID>       the type of keys maintained by the resulting map
     * @param collection the collection to be converted, may be null
     * @param idGetter   the function used to extract the key from each element
     * @return a map mapping the extracted keys to their corresponding elements, or an empty map if the collection is null
     */
    public static <T, ID> Map<ID, T> collectionToMapOne(Collection<T> collection, Function<T, ID> idGetter) {
        Map<ID, T> map;
        if (collection == null)
            map = Collections.emptyMap();
        else 
            map = collection.stream()
                    .filter(e -> idGetter.apply(e) != null)
                    .collect(Collectors.toMap(idGetter::apply, e -> e, (e1, e2) -> e1));
        return map;
    }

    /**
     * Groups the elements of a collection into a one-to-many map according to a key extractor function.
     * <p>
     * Null keys extracted by the {@code idGetter} function are filtered out. If the input 
     * collection is null, an empty map is returned.
     *
     * @param <T>        the type of elements in the collection
     * @param <ID>       the type of keys maintained by the resulting map
     * @param collection the collection to be grouped, may be null
     * @param idGetter   the function used to extract the key from each element
     * @return a map where each extracted key maps to a list of elements sharing that key, or an empty map if the collection is null
     */
    public static <T, ID> Map<ID, List<T>> collectionToMapMany(Collection<T> collection, Function<T, ID> idGetter) {
        Map<ID, List<T>> map;
        if (collection == null)
            map = Collections.emptyMap();
        else 
            map = collection.stream()
                    .filter(e -> idGetter.apply(e) != null)
                    .collect(Collectors.groupingBy(idGetter::apply));
        return map;
    }

    /**
     * Creates a new Populator instance using the shared global executor service.
     *
     * @param <P>     The Parent model type.
     * @param parents The collection of parent objects to populate.
     * @return A new, chainable Populator instance.
     */
    public static <P> Populator<P> of(Collection<P> parents) {
        return new Populator<P>(globalExecutor, parents);
    }

    /**
     * Creates a new Populator instance using a locally injected custom executor service.
     * Perfect for isolating specific heavy queries or for running single-threaded unit tests.
     *
     * @param <P>      The Parent model type.
     * @param executor The specific custom ExecutorService to use for this pipeline execution.
     * @param parents  The collection of parent objects to populate.
     * @return A new, chainable Populator instance.
     */
    public static <P> Populator<P> of(ExecutorService executor, Collection<P> parents) {
        return new Populator<P>(executor, parents);
    }

    /**
     * Adds a custom batch-population rule parameter configuration to the execution pipeline chain.
     *
     * @param <C>    The Child object model type being fetched.
     * @param <ID>   The unique identifier key type connecting Parent and Child.
     * @param params A pre-configured PopulateParams instance defining the relationship mapping rules. If null, it will simply ignore it.
     * @return This Populator builder instance to allow method chaining.
     */
    public <C, ID> Populator<P> populate(PopulateParams<P, C, ID> params) {
        if (params != null)
            this.paramsList.add(params);
        return this;
    }
    
    /**
     * Configures and chains a One-to-One child population relationship workflow rule.
     *
     * @param <C>                   The Child object model type.
     * @param <ID>                  The unique identifier key type.
     * @param parentIdGetter        Lambda method reference to read the unique join key from the Parent.
     * @param childIdGetter         Lambda method reference to read the unique join key from the fetched Child.
     * @param childFinder           Lambda function that batch-fetches a Collection of Children using a Collection of unique keys.
     * @param parentOneChildSetter  Lambda function to map and save a single Child reference back onto its matching Parent.
     * @return This Populator builder instance to allow method chaining.
     */
    public <C, ID> Populator<P> populateOne(
            Function<P, ID> parentIdGetter,
            Function<C, ID> childIdGetter,
            Function<Collection<ID>, Collection<C>> childFinder,
            BiFunction<P, C, P> parentOneChildSetter
    ) {
        return this.populate(PopulateParams.ofOne(parentIdGetter, childIdGetter, childFinder, parentOneChildSetter));
    }
    
    /**
     * Configures and chains a One-to-Many children list population relationship workflow rule.
     *
     * @param <C>                    The Child object model type.
     * @param <ID>                   The unique identifier key type.
     * @param parentIdGetter         Lambda method reference to read the unique join key from the Parent.
     * @param childIdGetter          Lambda method reference to read the unique join key from the fetched Child.
     * @param childFinder            Lambda function that batch-fetches a Collection of Children using a Collection of unique keys.
     * @param parentManyChildSetter  Lambda function to map and save a List of Children back onto their matching Parent.
     * @return This Populator builder instance to allow method chaining.
     */
    public <C, ID> Populator<P> populateMany(
            Function<P, ID> parentIdGetter,
            Function<C, ID> childIdGetter,
            Function<Collection<ID>, Collection<C>> childFinder,
            BiFunction<P, List<C>, P> parentManyChildSetter
    ) {
        return this.populate(PopulateParams.ofMany(parentIdGetter, childIdGetter, childFinder, parentManyChildSetter));
    }
    
    /**
     * Runs all registered population rules sequentially on the main executing calling thread.
     * 
     * <p>This method acts as a convenience shortcut that routes execution directly to 
     * {@link #runWithMode(boolean)} with the parallel flag set to {@code false}.</p>
     *
     * @return An unmodifiable {@link List} containing the processed and fully populated 
     *         Parent objects, or null if the initial input parents collection was null.
     */
    public List<P> run() {
        return this.runWithMode(false);
    }
    
    /**
     * Runs all registered population rules asynchronously using separate threads.
     * Greatly reduces total latency when gathering independent deep relationship trees concurrently.
     * 
     * <p>This method acts as a convenience shortcut that routes execution directly to 
     * {@link #runWithMode(boolean)} with the parallel flag set to {@code true}. It leverages 
     * the global or locally injected {@link ExecutorService} service to manage the asynchronous worker threads.</p>
     *
     * @return An unmodifiable {@link List} containing the processed and fully populated 
     *         Parent objects, or null if the initial input parents collection was null.
     */
    public List<P> runParallel() {
        return this.runWithMode(true);
    }
    
    /**
     * Executes all registered configuration mapping rules on the parent collection 
     * using the specified concurrency model workflow execution engine.
     * 
     * <p>This method automatically applies optimizations: if only a single population rule 
     * is registered, it bypasses parallel thread allocation entirely to minimize system overhead. 
     * It also safely guards against null or empty input collections by immediately returning 
     * an unmodifiable list fallback state or null.</p>
     *
     * @param parallel True to execute independent mapping chains concurrently using the global 
     *                 or locally injected ExecutorService; false to process all rules sequentially.
     * @return An unmodifiable {@link List} containing the processed and fully populated 
     *         Parent objects, or null if the initial input parents collection was null.
     */
    public List<P> runWithMode(boolean parallel) {
        if (this.parents == null || this.parents.isEmpty() || paramsList.isEmpty())
            return this.parents == null ? null : Collections.unmodifiableList(new ArrayList<>(this.parents));
        
        this.findAllChildren(this.parents, paramsList, parallel);
        return populateParentsWithAllChildren(this.parents, paramsList);
    }
    
    private void findAllChildren(Collection<P> parents, List<PopulateParams<P, ?, ?>> paramsList, boolean parallel) {
        if (!parallel || paramsList.size() == 1) {
            for (PopulateParams<P, ?, ?> params : paramsList)
                this.findChildren(parents, params);
        } else {
            List<CompletableFuture<Void>> futures = paramsList.stream()
                    .map(params -> CompletableFuture.runAsync(
                            () -> this.findChildren(parents, params),
                            executor
                        )
                    )
                    .collect(Collectors.toList());
            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
        }
        
    }

    private <C, ID> void findChildren(Collection<P> parents, PopulateParams<P, C, ID> params) {
        params.setChildren(findAssociatedObjects(parents, params.getParentIdGetter(), params.getChildFinder()));
    }

    private List<P> populateParentsWithAllChildren(Collection<P> parents, List<PopulateParams<P, ?, ?>> paramsList) {
        List<P> result = Collections.unmodifiableList(new ArrayList<>(parents));
        for (PopulateParams<P, ?, ?> params : paramsList) {
            result = populateParentsWithChildren(result, params);
        }
        return result;
    }

    private <C, ID> List<P> populateParentsWithChildren(List<P> parents, PopulateParams<P, C, ID> params) {
        if (params.getParentOneChildSetter() != null) {
            Map<ID, C> childMap = collectionToMapOne(params.getChildren(), params.getChildIdGetter());
            ArrayList<P> result = new ArrayList<P>();
            parents.forEach(p -> {
                ID parentId = params.getParentIdGetter().apply(p);
                if (parentId != null) {
                    result.add(params.parentOneChildSetter.apply(p, childMap.get(parentId)));
                } else {
                    result.add(p);
                }
            });
            return Collections.unmodifiableList(result);
        } else {
            Map<ID, List<C>> childMap = collectionToMapMany(params.getChildren(), params.getChildIdGetter());
            ArrayList<P> result = new ArrayList<P>();
            parents.forEach(p -> {
                ID parentId = params.getParentIdGetter().apply(p);
                if (parentId != null) {
                    result.add(params.parentManyChildSetter.apply(p, childMap.getOrDefault(parentId, Collections.emptyList())));
                } else {
                    result.add(p);
                }
            });
            return Collections.unmodifiableList(result);
        }
    }

    /**
     * A configuration parameter builder that defines the relationship data-binding mapping rules 
     * between a Parent model object and its associated Child data models.
     * 
     * <p>This class holds the target database query lambda lookups, entity keys, 
     * and data setter execution methods required by the core orchestration engine to stitch 
     * object graphs together in a single in-memory batch mapping cycle.</p>
     *
     * @param <P>  The Parent data model class type.
     * @param <C>  The Child data model class type being fetched.
     * @param <ID> The unique identifier key field type used to join Parent and Child records.
     */
    public static class PopulateParams<P, C, ID> {
        private Function<P, ID> parentIdGetter;
        private Function<C, ID> childIdGetter;
        private Function<Collection<ID>, Collection<C>> childFinder;
        private BiFunction<P, C, P> parentOneChildSetter;
        private BiFunction<P, List<C>, P> parentManyChildSetter;

        private Collection<C> children;
        
        private PopulateParams(
                Function<P, ID> parentIdGetter,
                Function<C, ID> childIdGetter,
                Function<Collection<ID>, Collection<C>> childFinder,
                BiFunction<P, C, P> parentOneChildSetter,
                BiFunction<P, List<C>, P> parentManyChildSetter
        ) {
            super();
            this.parentIdGetter = parentIdGetter;
            this.childIdGetter = childIdGetter;
            this.childFinder = childFinder;
            this.parentOneChildSetter = parentOneChildSetter;
            this.parentManyChildSetter = parentManyChildSetter;
            this.children = null;
        }
        
        /**
         * Factory builder helper to configure a strict One-to-One single child model relationship mapping config rule.
         *
         * @param <P>                  The Parent model object type.
         * @param <C>                  The Child object model type.
         * @param <ID>                 The unique identifier key type.
         * @param parentIdGetter       Lambda method reference to read the unique join key from the Parent.
         * @param childIdGetter        Lambda method reference to read the unique join key from the fetched Child.
         * @param childFinder          Lambda function that batch-fetches a Collection of Children using a Collection of unique keys.
         * @param parentOneChildSetter Lambda function to map and save a single Child reference back onto its matching Parent.
         * @return An initialized, immutable PopulateParams configuration rule rule wrapper.
         */
        public static <P, C, ID> PopulateParams<P, C, ID> ofOne(
                Function<P, ID> parentIdGetter,
                Function<C, ID> childIdGetter,
                Function<Collection<ID>, Collection<C>> childFinder,
                BiFunction<P, C, P> parentOneChildSetter
        ) {
            return new PopulateParams<P, C, ID>(parentIdGetter, childIdGetter, childFinder, parentOneChildSetter, null);
        }
        
        /**
         * Factory builder helper to configure a strict One-to-Many lists of children relationship mapping config rule.
         *
         * @param <P>                   The Parent model object type.
         * @param <C>                   The Child object model type.
         * @param <ID>                  The unique identifier key type.
         * @param parentIdGetter        Lambda method reference to read the unique join key from the Parent.
         * @param childIdGetter         Lambda method reference to read the unique join key from the fetched Child.
         * @param childFinder           Lambda function that batch-fetches a Collection of Children using a Collection of unique keys.
         * @param parentManyChildSetter Lambda function to map and save a List of Children back onto their matching Parent.
         * @return An initialized, immutable PopulateParams configuration rule rule wrapper.
         */
        public static <P, C, ID> PopulateParams<P, C, ID> ofMany(
                Function<P, ID> parentIdGetter,
                Function<C, ID> childIdGetter,
                Function<Collection<ID>, Collection<C>> childFinder,
                BiFunction<P, List<C>, P> parentManyChildSetter
        ) {
            return new PopulateParams<P, C, ID>(parentIdGetter, childIdGetter, childFinder, null, parentManyChildSetter);
        }

        public Function<P, ID> getParentIdGetter() {
            return parentIdGetter;
        }

        public Function<C, ID> getChildIdGetter() {
            return childIdGetter;
        }

        public Function<Collection<ID>, Collection<C>> getChildFinder() {
            return childFinder;
        }

        public BiFunction<P, C, P> getParentOneChildSetter() {
            return parentOneChildSetter;
        }

        public BiFunction<P, List<C>, P> getParentManyChildSetter() {
            return parentManyChildSetter;
        }

        private Collection<C> getChildren() {
            return children;
        }

        private void setChildren(Collection<C> children) {
            this.children = children;
        }
    }
    
    /**
     * A custom callback functional interface that allows developers to manipulate 
     * raw list of children before they are mapped and stitched onto parent target entities.
     * 
     * <p>While this interceptor can be used for general sorting or filtering transformations, 
     * its primary design purpose is to serve as a deep-stitching lifecycle hook. It allows 
     * developers to launch a completely separate {@code Populator} layer on the retrieved 
     * children before they are permanently mapped and stitched back onto the root parent elements.</p>
     *
     * @param <C> The Child collection list data element class type being transformed.
     */
    public static interface ChildModifier<C> {
        /**
         * Returns a modifier that performs no operation on the incoming children list.
         * 
         * <p>This serves as a standard, neutral fallback helper that returns the retrieved 
         * child records exactly as they were fetched from the data source, completely unmodified.</p>
         *
         * @param <C> The Child collection data element class type.
         * @return A transparent, pass-through ChildModifier function block.
         */
        static <C> ChildModifier<C> none() {
            return children -> children;
        }
        
        /**
         * Processes, or modifies a collection of child objects.
         * This is where developers can invoke additional {@code Populator.populate()} commands to build 
         * deeper levels of the object hierarchy tree (e.g., Grandchildren).
         *
         * @param children The raw incoming list of fetched children entities available for deeper population.
         * @return The finalized, fully populated List of data elements to be mapped onto the parent data graph.
         */
        List<C> modify(List<C> children);
    }
}
