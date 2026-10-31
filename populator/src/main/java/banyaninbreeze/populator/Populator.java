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
import java.util.HashMap;
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
    private static volatile ExecutorService globalExecutorService = Executors.newCachedThreadPool();
    
    private final ExecutorService executorService;
    private final List<P> parents;
    private final List<ChildHandler<P>> childHandlers;
    
    private Populator(ExecutorService executorService, Collection<P> parents) {
        this.parents = parents == null ? null : Collections.unmodifiableList(new ArrayList<>(parents));
        this.childHandlers = new ArrayList<>();
        this.executorService = executorService;
    }

    /**
     * Updates the global executor service used by all Populator instances by default.
     * Use this at application startup to configure custom thread pools or upgrade to virtual threads.
     *
     * @param executorService The custom ExecutorService to use globally.
     *        If null, the population will be always sequential even if calling runParallel() or runWithMode(true).
     */
    public static void setGlobalExecutorService(ExecutorService executorService) {
        Populator.globalExecutorService = executorService;
    }

    /**
     * Get the global executor service
     * 
     * @return the global executor service
     */
    public static ExecutorService getGlobalExecutorService() {
        return Populator.globalExecutorService;
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
     * @param assocObjectsfinder Lambda repository rule that accepts a Collection of keys and returns matching target models.
     * @return A List of successfully found associated target items.
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
        if (ids.isEmpty())
            return Collections.emptyList();
        else {
            Collection<U> objs = assocObjectsfinder.apply(ids);
            return objs == null ? Collections.emptyList() : Collections.unmodifiableList(new ArrayList<>(objs));
        }
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
        Map<ID, T> map = new HashMap<>();

        if (collection != null) {
            for (T item : collection) {
                ID id = idGetter.apply(item);
                if (id != null) {
                    map.putIfAbsent(id, item);
                }
            }
        }
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
        Map<ID, List<T>> map = new HashMap<>();
        if (collection != null) {
            for (T item : collection) {
                ID id = idGetter.apply(item);
                if (id != null) {
                    map.computeIfAbsent(id, k -> new ArrayList<>()).add(item);
                }
            }
        }
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
        return new Populator<P>(globalExecutorService, parents);
    }

    /**
     * Creates a new Populator instance using a locally injected custom executor service.
     * Perfect for isolating specific heavy queries or for running single-threaded unit tests.
     *
     * @param <P>      The Parent model type.
     * @param executorService The specific custom ExecutorService to use for this pipeline execution.
     * @param parents  The collection of parent objects to populate.
     * @return A new, chainable Populator instance.
     */
    public static <P> Populator<P> of(ExecutorService executorService, Collection<P> parents) {
        return new Populator<P>(executorService, parents);
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
            this.childHandlers.add(new ChildHandlerImpl<P, C, ID>(params));
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
     * Executes all registered population rules sequentially on the calling thread.
     *
     * <p>For each rule, the associated children are fetched and then assigned
     * to their corresponding parents. Population rules are processed in the
     * order in which they were registered.</p>
     *
     * @return An unmodifiable list of populated parent objects, or {@code null}
     *         if the original parent collection was {@code null}.
     */
    public List<P> run() {
        return this.runWithMode(false);
    }
    
    /**
     * Executes independent child-fetching operations concurrently using the
     * configured {@link ExecutorService}. Once all fetch operations complete,
     * the results are assigned to their corresponding parents sequentially.
     *
     * <p>If no executor is configured, or only one population rule is registered,
     * this method executes sequentially.</p>
     *
     * <p>This method blocks until the entire population process is complete.</p>
     *
     * @return An unmodifiable list of populated parent objects, or {@code null}
     *         if the original parent collection was {@code null}.
     */
    public List<P> runParallel() {
        return this.runWithMode(true);
    }

    /**
     * Executes all registered population rules using the specified execution mode.
     *
     * <p>When {@code parallel} is {@code false}, child-fetching operations are
     * executed sequentially on the calling thread. When {@code true}, independent
     * child-fetching operations are executed concurrently using the configured
     * {@link ExecutorService}, if available.</p>
     *
     * <p>After all child-fetching operations have completed, the retrieved
     * children are assigned to their corresponding parents sequentially,
     * preserving the registration order of the population rules.</p>
     *
     * <p>If no {@link ExecutorService} is configured, execution is sequential
     * regardless of the value of {@code parallel}. A single population rule
     * is also executed sequentially to avoid unnecessary thread overhead.</p>
     *
     * @param parallel {@code true} to execute independent child-fetching
     *                 operations concurrently when an executor is available;
     *                 {@code false} to execute them sequentially.
     * @return An unmodifiable list of populated parent objects, or {@code null}
     *         if the original parent collection was {@code null}.
     */
    public List<P> runWithMode(boolean parallel) {
        if (this.parents == null || this.parents.isEmpty() || this.childHandlers.isEmpty())
            return this.parents == null ? null : this.parents;
        
        this.findAllChildren(this.parents, this.childHandlers, parallel);
        List<P> results = populateParentsWithAllChildren(this.parents, this.childHandlers);
        return Collections.unmodifiableList(results);
    }
    
    private void findAllChildren(List<P> parents, List<ChildHandler<P>> childHandlers, boolean parallel) {
        if (!parallel || executorService == null || childHandlers.size() == 1) {
            for (ChildHandler<P> childHandler : childHandlers)
                childHandler.findChildren(parents);
        } else {
            List<CompletableFuture<Void>> futures = childHandlers.stream()
                    .map(childHandler -> CompletableFuture.runAsync(
                            () -> childHandler.findChildren(parents),
                            executorService
                        )
                    )
                    .collect(Collectors.toList());
            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
        }
        
    }

    private List<P> populateParentsWithAllChildren(List<P> parents, List<ChildHandler<P>> childHandlers) {
        List<P> result = parents;
        for (ChildHandler<P> childHandler : childHandlers) {
            result = childHandler.assignToParents(result);
        }
        return result;
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
        private final Function<P, ID> parentIdGetter;
        private final Function<C, ID> childIdGetter;
        private final Function<Collection<ID>, Collection<C>> childFinder;
        private final BiFunction<P, C, P> parentOneChildSetter;
        private final BiFunction<P, List<C>, P> parentManyChildSetter;
        
        private PopulateParams(
                Function<P, ID> parentIdGetter,
                Function<C, ID> childIdGetter,
                Function<Collection<ID>, Collection<C>> childFinder,
                BiFunction<P, C, P> parentOneChildSetter,
                BiFunction<P, List<C>, P> parentManyChildSetter
        ) {
            this.parentIdGetter = parentIdGetter;
            this.childIdGetter = childIdGetter;
            this.childFinder = childFinder;
            this.parentOneChildSetter = parentOneChildSetter;
            this.parentManyChildSetter = parentManyChildSetter;
        }
        
        /**
         * Factory builder helper to configure a One-to-One single child model relationship mapping config rule.
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
         * Factory builder helper to configure a One-to-Many lists of children relationship mapping config rule.
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
    public interface ChildModifier<C> {
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

    private interface ChildHandler<P> {
        void findChildren(List<P> parents);
        List<P> assignToParents(List<P> parents);
    }

    private static class ChildHandlerImpl<P, C, ID> implements ChildHandler<P> {
        private final PopulateParams<P, C, ID> params;
        private Collection<C> children;

        public ChildHandlerImpl(PopulateParams<P, C, ID> params) {
            this.params = params;
        }

        public void findChildren(List<P> parents) {
            this.children = findAssociatedObjects(parents, params.getParentIdGetter(), params.getChildFinder());
        }
        
        public List<P> assignToParents(List<P> parents) {
            if (params.getParentOneChildSetter() != null) {
                Map<ID, C> childMap = collectionToMapOne(children, params.getChildIdGetter());
                ArrayList<P> result = new ArrayList<P>();
                parents.forEach(p -> {
                    ID parentId = params.getParentIdGetter().apply(p);
                    if (parentId != null) {
                        result.add(params.parentOneChildSetter.apply(p, childMap.get(parentId)));
                    } else {
                        result.add(p);
                    }
                });
                return result;
            } else {
                Map<ID, List<C>> childMap = collectionToMapMany(children, params.getChildIdGetter());
                ArrayList<P> result = new ArrayList<P>();
                parents.forEach(p -> {
                    ID parentId = params.getParentIdGetter().apply(p);
                    if (parentId != null) {
                        result.add(params.parentManyChildSetter.apply(p, childMap.getOrDefault(parentId, Collections.emptyList())));
                    } else {
                        result.add(p);
                    }
                });
                return result;
            }
        }
    }
}
