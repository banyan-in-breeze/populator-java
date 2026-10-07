# Populator

**Populator** is a small but powerful Java utility designed to fetch parent and child data using any data layer framework (like Spring JPA, JDBC, jOOQ, or MyBatis) or remote API calls, stitch them together, and build complex object structures of any number of levels.

It has zero third-party dependencies, lets you replace complex database table joins with fast, efficient batch lookups, and completely avoids the N+1 query issue.

Beyond a simple helper, Populator can serve as a system-wide data assembly strategy that makes your code clean, reusable, and scalable.

The utility is extremely lightweight, containing about **250 lines of executable code** in `Populator.java` and about **90 lines of executable code** in `BatchFinder.java` (excluding comments and blank lines).

## Table of Contents

* [Why Populator?](#why-populator)
  * [Problems it solves](#problems-it-solves)
  * [How Populator helps](#how-populator-helps)
* [Installation](#installation)
* [Demonstrate Populator with Examples](#demonstrate-populator-with-examples)
  * [Start with the Simplest Scenario](#start-with-the-simplest-scenario)
  * [More Complex Scenario - Multi-level, Multi-sibling Structure, Multi-Datasource](#more-complex-scenario---multi-level-multi-sibling-structure-multi-datasource)
  * [Using PopulateParams for Cleaner and Reusable Code](#using-populateparams-for-cleaner-and-reusable-code)
  * [Using ChildModifier for Ultimate Reusability](#using-childmodifier-for-ultimate-reusability)
  * [Populate Siblings in Parallel](#populate-siblings-in-parallel)
  * [Safe Query Batching with BatchFinder](#safe-query-batching-with-batchfinder)
  * [Immutable, Mutable, and Semi-mutable Model Classes](#immutable-mutable-and-semi-mutable-model-classes)
  * [Small Type Hinting, Big Help](#small-type-hinting-big-help)
* [Performance Analysis](#performance-analysis)
  * [Compare with Examples](#compare-with-examples)
  * [Understanding the Trade-offs](#understanding-the-trade-offs)
  * [Other Performance Factors](#other-performance-factors)
  * [Conclusion on Performance](#conclusion-on-performance)
* [Use Populator as a System-Wide Strategy](#use-populator-as-a-system-wide-strategy)
  * [1. Create Reusable Building Blocks in the Repository Layer](#1-create-reusable-building-blocks-in-the-repository-layer)
  * [2. Assemble Data Models in the Service Layer](#2-assemble-data-models-in-the-service-layer)
  * [3. Convert Models to DTOs in the End](#3-convert-models-to-dtos-in-the-end)
  * [Package Structure](#package-structure)
  * [Explore the Bookstore Demo](#explore-the-bookstore-demo)
* [Explore the Demos for More Details](#explore-the-demos-for-more-details)
* [Contributing, Issues & License](#contributing-issues--license)

---

## Why Populator?

Data frameworks work well for basic database operations, but they can struggle when assembling complex, multi-level object trees.

### Problems it solves:
* **The Join Explosion and Mapping Difficulty:** Joining multiple one-to-many tables in a single SQL query duplicates data across the network and slows down your database. It also makes mapping those flat query results back into nested object structures complex.
* **The N+1 Query Trap:** Fetching child records inside a loop creates too many individual database or API requests.
* **Hybrid Data Sources:** Traditional frameworks assume all your data lives in databases reachable in one query. They cannot easily connect a database entity to a child object that comes from an isolated remote REST or gRPC API.

### How Populator helps:
* **Problem Solver:** Use `Populator` alongside your existing data layer (like Spring Data JPA, JDBC, jOOQ, or MyBatis) to solve problems of complex data mapping. Instead of executing one massive SQL join, you write small batch queries (like `SELECT ... WHERE ID IN (?, ?, ?, ...)`). Your framework handles the row-to-object mapping, and `Populator` coordinates the execution to stitch the data together.
* **System-Wide Solution:** Beyond a simple helper, `Populator` can serve as a core data assembly strategy. You can reuse independent data finders and mapping parameters to satisfy various client requirements cleanly, which drastically reduces the need for massive, complex SQL queries.
* **Mix Data Sources:** You can combine database rows and remote API responses inside a single, fluent method chain. Traditional data frameworks cannot natively bridge relational databases and remote network endpoints during relationship assembly.
* **Virtually Zero Learning Curve:** It introduces no new expression languages or proprietary annotations. If you know how to use standard Java functional interfaces and method references, you already know how to use Populator.
* **Low Cognitive Load** Instead of wrestling with massive SQLs and data mappings for complex object structure, you only write simple, small, and reusable building blocks, then use Populator to stitch them together. Furthermore, the building blocks you write in earlier sprints are directly usable in later sprints, reducing your overall development work.
* **Memory Efficient:** It supports immutable Java `record` types, standard POJOs, and mutable objects. For high-throughput systems, using mutable or semi-mutable classes (immutable core fields with mutable child references) prevents heavy object copying and memory churn.
* **Performance for High Standards:** Compared to large, complex table joins, the Populator approach fetches data without any duplication, reducing network payload and memory usage, which often offsets the delay caused by making more network roundtrips.
* **No Dependencies:** It is written in pure Java. It does not require Spring, Lombok, or any other external library.

---

## Installation

Since Populator is a plain source code utility, you do not need to install or import a JAR dependency. Just copy the package `banyaninbreeze.populator` (including `Populator.java` and `BatchFinder.java`) into your project's source folder.

* **Core Utility:** The two core source files (`Populator.java` and `BatchFinder.java`) are compatible with **Java 8 or higher** and have zero dependencies on third-party libraries.
* **Demos:** The included example projects require **Java 17 or higher** to run, and also have zero dependencies on third-party libraries.

---

## Demonstrate Populator with Examples

I will use simple examples to explain how to use Populator. For more details, explore the two demos in the repository that I will explain soon.

### Start with the Simplest Scenario

Assume you have two tables of one-to-many relationship: team and member as the following:

```text
Team
  └── (many) Members
```

You want to compose the following data model for the client side (I could use records here like in the "simple" demo program, but let's use classes here):

```java
class Team {
    Integer id;
    String name;
    List<Member> members;

    // ... getters/setters/constructors
}

class Member {
    Integer id;
    String name;
    Integer teamId;

    // ... getters/setters/constructors
}
```

With other frameworks, you use `JOIN FETCH`, `JOIN` plus custom Java loop mapping, or jOOQ's MULTISET subqueries with record mappers.

With Populator approach, you do the following:

```java
// SQL: SELECT team.id, team.name FROM team WHERE ...
List<Team> findTeamsBySomeCriteria(/* ... some criteria */)  { ... }

// SQL: SELECT member.id, member.name FROM member WHERE team_id IN (:teamIds)
List<Member> findMembersByTeamIds(Collection<String> teamIds)  { ... }

List<Team> findTeamsWithMembersPopulated(/* ... some criteria */) {
    // Use Populator to fetch members and stitich teams with members
    List<Team> teams = findTeamsBySomeCriteria(/* ... some criteria */);
    teams = Populator.of(teams)
            .populateMany(Team::getId, Member::getTeamId,
                         this::findMembersByTeamIds, Team::setMembers)
            .run();
    return teams; // already populated with Member objects
}
```
*(Note: In the above example, we assume Team.setMembers() is a setter that returns "this". See the "simple" demo for a simple solution if setMembers() can only return void.)*

Alternatively, you can use the one-shot static method `populateMany()` to do the population in a single call::

```java
teams = Populator.populateMany(teams, Team::getId, Member::getTeamId,
                        this::findMembersByTeamIds, Team::setMembers);
```

#### How It Works under the Hood

Curious about what `populateMany()` does internally? It virtually does this:

```java
List<Team> teams = findTeamsBySomeCriteria(/* ... some criteria */);

Set<Integer> teamIds = teams.stream()
                    .map(Team::getId).collect(Collectors.toSet());
List<Member> members = findMembersByTeamIds(teamIds);
Map<Integer, List<Member>> memberMap = 
    members.stream().collect(Collectors.groupingBy(Member::getTeamId));
teams = teams.stream()
    .map(team -> team.withMembers(memberMap.get(team.getId())))
    .toList();
```

Populator uses generics to implement the logic and can be used for any data types and child finder methods. By using Populator, you don't need to implement the same logic each time you want to stitch the data objects together.

### More Complex Scenario - Multi-level, Multi-sibling Structure, Multi-Datasource

Assume the team-member database is expanded to include manager and title tables as well. What's more, you need to call a remote API to get member contact methods from an external directory. Here is the structure:

```text
Team
  ├── (one) Manager
  └── (many) Members
        ├── (one) Title
        └── (many) ContactMethods
```

With traditional frameworks, building structures like this requires massive SQL joins and complex manual coding to mix database rows with remote API data. If Manager and Title were "many" relationships too, the manual code would turn into a nightmare.

With Populator, everything is done smoothly in a single, unified method chain, no matter how complex the structure gets.

```java
// Add model classes Manager, Title, and ContactMethod
class Manager { ... }
class Title { ... }
class ContactMethod { ... }

// Modify Member to include fields for title and contact methods
class Member {
    Integer id;
    String name;
    Integer teamId;
    Integer titleId; // new field in member table
    String employeeId; // new field in table, reference to external directory

    Title title; // new - to be populated
    List<ContactMethod> contactMethods; // new - to be populated

    // ... getters/setters/constructors
}

// Implement database query functions for finding managers and titles
List<Manager> findManagersByTeamIds(Collection<Integer> teamIds) { ... }
List<Title> findTitlesByIds(Collection<Integer> titleIds) { ... }

// Remote API call to get contact methods from external directory
List<ContactMethod> findContactMethodsByEmployeeIds(
                        Collection<String> employeeIds) { ... }

// The key pattern for multi-level, multi-datasource stitching
List<Team> findTeamsWithEverythingPopulated(/* ... some criteria */) {
    List<Team> teams = findTeamsBySomeCriteria(/*... some criteria */);
    
    teams = Populator.of(teams)
        // Level 1 Sibling: Fetch and stitch single Managers from DB
        .populateOne(Team::getId, Manager::getTeamId, 
                    this::findManagersByTeamIds, Team::setManager)
        
        // Level 1 Sibling: Fetch and stitch Members from DB
        .populateMany(
            Team::getId,
            Member::getTeamId,
            teamIds -> {
                var members = this.findMembersByTeamIds(teamIds);
                
                // Level 2: Stitch members with sub-children before returning
                return Populator.of(members)
                    .populateOne(Member::getTitleId, Title::getId, 
                                this::findTitlesByIds, Member::setTitle)
                    .populateMany(Member::getEmployeeId,
                                ContactMethod::getEmployeeId, 
                                this::findContactMethodsByEmployeeIds,
                                Member::setContactMethods) // Remote API Call!
                    .run();
            },
            Team::setMembers
        )
        .run();

    return teams; // already populated with the multi-level objects
}
```

### Using PopulateParams for Cleaner and Reusable Code

Instead of passing four separate parameters (two ID getters, one finder, and one setter) directly to a method, you can encapsulate them inside a `PopulateParams` object and pass it to the `.populate()` method. 

`PopulateParams` instances can be defined once and shared across your project, and this keeps your code cleaner.

Here is an example of creating and using a `PopulateParams` object for a one-to-many relationship:
```java
PopulateParams<Team, Member, Integer> paramsTeamWithMembers() {
    return PopulateParams.ofMany(Team::getId, Member::getTeamId,
                                 this::findMembersByTeamIds, Team::setMembers);
}

List<Team> findTeamsWithMembersPopulated(/* ... some criteria */) {
    List<Team> teams = findTeamsBySomeCriteria(/* ... some criteria */);
    return Populator.of(teams).populate(paramsTeamWithMembers()).run();
}
```
*(Note: Use `PopulateParams.ofOne()` for one-to-one relationships).*

### Using ChildModifier for Ultimate Reusability

In the above example, assume you want to populate `Members` with their `ContactMethods`, you could do it like this:

```java
PopulateParams<Team, Member, Integer> paramsTeamWithMembers() {
    return PopulateParams.ofMany(Team::getId, Member::getTeamId,
        teamIds -> {
            var members = findMembersByTeamIds(teamIds);
            return Populator.of(members)
                .populate(paramsMemberWithContactMethods())
                .run();
        },
        Team::setMembers);
}
```
While this compiles and works, it introduces two architectural problems:
1. Developers looking at the top-level code cannot tell that calling `paramsTeamWithMembers()` also fetches contact data unless they look inside this function to find out.
2. If one place needs members *with* contact methods, but another place needs members *without* them, you cannot reuse this function for both.

To solve this, Populator includes the `ChildModifier` interface. It is a simple interface similar to Java's `Function<T, U>` but is designed specifically for Populator:

```java
public static interface ChildModifier<C> {
    // A passthrough instance that returns the list untouched
    static <C> ChildModifier<C> none() {
        return children -> children;
    }

    // Takes the child collection and modifies and/or returns it
    List<C> modify(List<C> children);
}
```

By adding a `ChildModifier` to the parameter creators, you can control the data population of all levels from the top-level code:

```java
PopulateParams<Team, Member, Integer> paramsTeamWithMembers(
            ChildModifier<Member> childModifier) {
    return PopulateParams.ofMany(Team::getId, Member::getTeamId,
        teamIds -> childModifier.modify(findMembersByTeamIds(teamIds)),
        Team::setMembers);
}

// Scenario A: Populate members WITH contact methods
List<Team> findTeamsWithMembersAndContacts(/* ... some criteria */) {
    List<Team> teams = findTeamsBySomeCriteria(/* ... some criteria */);
    return Populator.of(teams)
        .populate(paramsTeamWithMembers(
            members -> Populator.of(members)
                .populate(paramsMemberWithContactMethods())
                .run()
        ))
        .run();
}

// Scenario B: Populate members ONLY (No contacts)
List<Team> findTeamsWithMembersOnly(/* ... some criteria */) {
    List<Team> teams = findTeamsBySomeCriteria(/* ... some criteria */);
    return Populator.of(teams)
        .populate(paramsTeamWithMembers(ChildModifier.none()))
        .run();
}
```

The full function `findTeamsWithEverythingPopulated()` can be rewritten with `PopulateParams` and `ChildModifier` as following: 

```java
// Add paramsTeamWithManager(), paramsMemberWithTitle(),
//     paramsMemberWithContactMethods()

List<Team> findTeamsWithEverythingPopulated(/* ... some criteria */) {
    List<Team> teams = findTeamsBySomeCriteria(/* ... some criteria */);
    
    return Populator.of(teams)
        .populate(paramsTeamWithManager())
        .populate(paramsTeamWithMembers(
            members -> Populator.of(members)
                .populate(paramsMemberWithTitle())
                .populate(paramsMemberWithContactMethods())
                .run()
        ))
        .run();
}
```
Now, it is clean, simple, and shows the whole structure of the data objects in one place.

### Populate Siblings in Parallel

Populator features built-in parallel processing to improve data-fetching performance. Using it is simple: change your terminal method call from `.run()` to `.runParallel()`.

Here is the concurrent version of our complex multi-level function:

```java
List<Team> findTeamsWithEverythingPopulatedParallel(/* ... some criteria */) {
    List<Team> teams = findTeamsBySomeCriteria(/*... some criteria */);
    
    return Populator.of(teams)
        .populate(paramsTeamWithManager())
        .populate(paramsTeamWithMembers(
            members -> Populator.of(members)
                .populate(paramsMemberWithTitle())
                .populate(paramsMemberWithContactMethods())
                .runParallel() // <-- parallel
        ))
        .runParallel(); // <-- parallel
}
```

Alternatively, you can call `.runWithMode(boolean parallel)` to pass a boolean flag that switches between sequential and parallel processing dynamically.

#### How It Works under the Hood
When you execute `runParallel()`, Populator invokes all data finder functions at that specific level concurrently using an internal thread pool. Once all threads finish retrieving the child datasets, Populator switches back to sequential execution to stitch the child objects into their parents one parameter at a time. This structural design ensures that immutable records or copy-on-write classes can safely accumulate all child siblings without race conditions or data loss.

#### Using a Virtual Thread Pool with Java 21+

By default, `Populator` uses `Executors.newCachedThreadPool()` as its global thread pool. You can change this default thread pool using a static setter method.

If your project uses Java 21 or higher, it is highly recommended to switch to a Virtual Thread pool. Virtual threads do not block OS carrier threads, which maximizes network I/O throughput and safely prevents thread-starvation deadlocks during nested parallel runs.

Add the following code as part of your project's startup code:

```java
Populator.setGlobalExecutor(Executors.newVirtualThreadPerTaskExecutor());
```

#### Thread-Starvation Deadlock Prevention

If you change the default thread pool to a custom thread pool with a **fixed or limited number of threads**, a thread-starvation deadlock can occur if you invoke `runParallel()` at multiple nested levels simultaneously (like the example above). This happens because the outer threads will block while waiting for the inner threads to finish, exhausting the pool. If you must use a limited thread pool, use `runParallel()` at either parent or child levels, not both.

If you don't change the default thread pool, you don't need to worry about thread-starvation deadlock.

### Safe Query Batching with BatchFinder

When using `Populator`, your system will rely heavily on batch lookup methods (like `findMembersByIds(Collection<String> ids)`). However, passing large or unpredictable numbers of IDs directly to a database can cause two major problems:
1. **Parameter Limits:** Many databases fail if an `IN` clause exceeds maximum parameter limits (e.g., Oracle's 1,000-item limit).
2. **Cache Bloat:** Passing a varying number of IDs (like anywhere between 1 to 1000) forces the database to re-compile execution plans for each variation, bloating the prepared statement cache.

`BatchFinder` is a companion utility coming with Populator. It splits a large collection of IDs into predictable, standardized batch sizes, calls the provided finder function for each batch of IDs, and merges the results into one list. This solves both problems.

Here is an example:
```java
public List<Member> findMembersByIds(Collection<Integer> ids) {
    return BatchFinder.find(ids, this::localFindMembersByIds);
}

// This is the actual function that finds members from the database
private List<Member> localFindMembersByIds(Collection<Integer> ids) { ... }
```

The best practice is to encapsulate `BatchFinder` directly inside your repository. If you use Spring Data JDBC/JPA's Repository interface to implement the finders, you can add an "interface default" function for `findMembersByIds()`, while `localFindMembersByIds()` is your actual data access function annotated with `@Query`. This keeps your `Populator` chains completely clean.

For the details of how `BatchFinder` works under the hood, feel free to explore the source code.

### Immutable, Mutable, and Semi-mutable Model Classes

Populator can stitch both immutable and mutable parent and child objects together. The key part is the setter parameter passed to methods like `populateOne()`, `populateMany()`, `PopulateParams.ofOne()`, or `PopulateParams.ofMany()`. 

The setter function must do two things: assign the child object(s) to the parent, and then return that parent instance. This can be an immutable object's copy method (like a "wither") or a mutable object's setter that returns `this`. You can easily use tools like Lombok to auto-generate these setters or withers.

#### For Immutable Classes and Records

```java
record Team(
    Integer id,
    String name,
    List<Member> members // Child objects to be populated
) {
    public Team(Integer id, String name) {
        this(id, name, null);
    }

    public Team withMembers(List<Member> members) {
        return new Team(this.id, this.name, members);
    }
}

// Populate teams with members:
teams = Populator.of(teams)
        .populateMany(Team::id, Member::teamId, this::findMembersByTeamIds,
                      Team::withMembers) // <-- Pass the wither
        .run();
```

#### For Mutable Classes

```java
class Team {
    Integer id;
    String name;
    List<Member> members; // Child objects to be populated

    public Team(Integer id, String name) {
        this.id = id;
        this.name = name;
    }

    public Team setMembers(List<Member> members) {
        this.members = members;
        return this; // <-- Return "this"
    }

    // ... other getters and setters
}

// Populate teams with members:
teams = Populator.of(teams)
        .populateMany(Team::getId, Member::getTeamId,
                    this::findMembersByTeamIds,
                    Team::setMembers) // <-- Pass the setter
        .run();
```

If you cannot change your setter methods to return `this`, you can wrap the assignment in a short inline lambda expression instead:
```java
teams = Populator.of(teams)
        .populateMany(Team::getId, Member::getTeamId,
                this::findMembersByTeamIds,
                (team, members) -> { team.setMembers(members); return team; })
        .run();
```

#### Use Semi-mutable Classes if Possible
If you have control over your data models, the best approach is to make your core primitive fields `final` (like `id` and `name` in the `Team` class) and keep your child object references mutable with setters. 

For added protection from unwanted changes outside your data layer, make the setter methods package-scoped and define your `PopulateParams` creator methods within that same package.

#### Memory Usage Considerations
For high-throughput production systems, using semi-mutable or mutable data models is highly recommended. It uses much less memory and runs faster because it prevents heavy object copying and constant garbage collection cleanups during deep nesting runs.

### Small Type Hinting, Big Help

When you call `populateOne()`, `populateMany()`, or create a `PopulateParams` instance, you can explicitly add type arguments right in front of the method name like this:

```java
teams = Populator.of(teams)
        .<Team, Member, Integer>populateMany(Team::getId, Member::getTeamId,
                        this::findMembersByTeamIds, Team::setMembers)
        .run();
```

While type arguments are not required by the compiler here, adding them helps your IDE immediately identify syntax errors in your parameter list. 

Without the type arguments, if you make a typo (such as writing `Team::getid` instead of `Team::getId`), the IDE gets confused and underlines the *entire method call* in red, and you wonder where the actual error is. Once you add the type arguments explicitly, your IDE can point directly to the typo. If you want to keep your code clean, you can delete the type arguments after the block is finished.

---

## Performance Analysis

To build multi-level object structures, the Populator approach and traditional approaches (single-query solutions like Spring Data JPA's `JOIN FETCH`) handle performance trade-offs differently.

### Compare with Examples
Assume you want to fetch **10 teams** that contain a total of **100 members** (an average of 10 members per team).

* **With the Populator approach:**
  * Database Network Trips: **2 trips** (one for teams, one for members).
  * Total Objects Fetched: **110 objects** (10 teams + 100 members).
* **With the traditional approach:**
  * Database Network Trips: **1 trip**.
  * Total Objects Fetched: **200 objects** (10 teams repeated for 100 members + 100 members).

If you increase the dataset to **100 teams** and **1,000 members**, the number of trips remains the same, but the difference between the objects fetched grows significantly:

* **With the Populator approach:** Total objects fetched: **1,100 objects**.
* **With the traditional approach:** Total objects fetched: **2,000 objects**.

**Summary:**
* The **Populator approach** takes **more network trips** but fetches **less data**.
* The **Traditional approach** takes **fewer network trips** but fetches **more data**.

As the number of hierarchical levels and sibling relationships increase, the number of network trips for Populator increases at the same pace. However, the data size for single-query approaches grows drastically, which gets much worse as your database records scale up.

### Understanding the Trade-offs

* **More trips** mean more network latency. In a typical production environment where your Java application and database share a local network, a single network roundtrip takes no more than a few milliseconds, or even sub-millisecond.
* **More data** means heavier payloads and more CPU overhead inside your Java application to create and recycle duplicate objects in memory.

This means:
* For **simpler structures and fewer records**, the **traditional approach** of a single-join is **faster** because of its lower network latency, but only **by a very small margin** in a typical production environment.
* For **more complex structures and more records**, the **Populator approach** is **faster** because it transfers a smaller amount of data and creates fewer objects.

### Other Performance Factors
* **Database Workload:** Databases are very fast at running simple, index-driven primary key lookups (like `WHERE ID IN (?, ?, ...)`). Complex table joins force the database server to spend heavy CPU and memory resources sorting, merging, and filtering large datasets. Populator offloads this object stitching workload away from the database server and into the cheaper, horizontally scalable application server. **Populator wins this one.**
* **Execution Plan Cache:** Populator relies heavily on SQL statements like `WHERE ID IN (?, ?, ...)`. Different numbers of parameters require different execution plans to be cached in the database. Passing random ID counts can flood this cache. However, because `BatchFinder` groups large lists into standardized batch steps, it limits cache variations to a predictable range. While the traditional single-query approach still has a clear advantage here, `BatchFinder` prevents runaway cache bloat and eliminates intermittent slowness under high traffic. **The traditional approach wins this one, but the difference is negligible in production.**

### Conclusion on Performance

The above analysis is only a general, rough idea regarding the most important factors. The real world is more complex; SQL query shapes, database indexes, connection pools, and many other factors also matter. So the analysis does not cover every single scenario.

But in general, for more complex structures and larger datasets, the Populator approach can deliver higher performance than the single-query approach. For simpler structures and smaller datasets, the single-query approach may be faster by a very narrow margin.

Therefore, performance-wise, it is safe to choose the Populator approach as your main and consistent system-wide strategy.

---

## Use Populator as a System-Wide Strategy

You can use `Populator` in your systems in two ways.

The first is to use it as a simple utility whenever you need to handle one-to-many associations or assemble data from different data sources (such as local databases and remote APIs). 

The second way is to use it as a core architectural pattern to help create a highly scalable, maintainable, and performant system. To achieve this, you follow three clean steps:
1. Create reusable building blocks in your **Repository Layer**.
2. Use `Populator` to assemble them as needed in your **Service Layer**.
3. Convert the assembled object structure into specific DTOs in the very end.

### 1. Create Reusable Building Blocks in the Repository Layer

There are three types of reusable building blocks in this layer:
* Data models
* Database access functions and remote API clients
* `PopulateParams` creators

#### (1) Data Models

These are your standard model or entity classes that map to database tables or query result sets (such as `Team`, `Manager`, and `Member`). 

When defining your data models, use these practices:
1. **Include Remote Objects:** Treat data structures returned from remote APIs as part of your data models (such as `ContactMethod`).
2. **Add Relationship Pointers:** Add reference fields to associated models for one-to-one or one-to-many relationships (such as the `members` and `manager` fields in the `Team` class). Keep these references empty or null by default.
3. **Map to Query Results:** There are a few scenarios:
   * **One model maps to one table:** The query's `SELECT` clause pulls fields from a single table, though it can still join other tables for filtering. This maximizes the reusability of the model.
   * **One model maps to multiple joined tables:** You may want to do this for different reasons:
     * Multiple tables are repeatedly used together, so combining them in one model is convenient. This acts just like a Java-side database view.
     * Reducing the number of roundtrips to the database for performance reasons.
   * **One model maps to an aggregation query result:** This applies to queries using `SUM`, `COUNT`, `MAX`, `MIN`, and so on. Always ensure the model includes a proper unique ID so other models can link to it.

Here are some considerations regarding **reusability and performance** (Note: this applies to all frameworks, not just Populator):
* Including every single table column in a model makes it highly reusable for different DTOs, but fetching unnecessary columns degrades query speeds, hurts database index utilization, and can cause slow full-table scans.
* Restricting models to only a few fields makes them fast to fetch but reduces their reusability across different application endpoints.

**The Solution:**
* **For standard tables:** Map only the most frequently used primitive fields to the model.
* **For exceptionally wide tables:** Create two versions of the model: a **Lean Model** (containing only primary keys and core fields) for high-traffic listing endpoints, and a **Wide Model** for deep detail views.
* **Use Semi-Mutable Classes:** Keep primitive fields immutable but leave relationship reference fields mutable with package-scoped setters.

#### (2) Database Access Functions & Remote APIs
Database access functions include:
* **Parent Queries:** Functions that query "parent" data based on incoming criteria. These queries can perform SQL joins to filter or sort data, but the final database result set should be mapped into a single model class type.
* **Child Lookups:** Simple batch functions that fetch "child" data using a collection of IDs associated with the parents (e.g., `WHERE ID IN (?, ?, ...)`). Always use `BatchFinder` to wrap these functions if the number of IDs can exceed 200 items.

Remote API client calls should follow this same pattern, serving as a remote repository.

#### (3) PopulateParams Creators
These are standalone helper functions that build and return the `PopulateParams` configurations used by `Populator` (such as `PopulateParams<Team, Member, Integer> paramsTeamWithMembers()`). 

**Best Practice:** Put these creator methods in classes within the same package as your data models. This allows you to make your model's setter methods package-private. By doing this, you keep those methods hidden and safe from unwanted changes outside of that package.

### 2. Assemble Data Models in the Service Layer

Call `Populator` inside your service layer to stitch the above building blocks together. Only populate the specific data that the client needs for that request context (like the `findTeamsWithMembersPopulated()` example).

### 3. Convert Models to DTOs in the End

After your model objects are stitched together, map them into DTOs that meet the client's requirements at the very end of your pipeline.

This step may involve generating custom values from your compiled models based on specific business rules or calculations. These calculations can be implemented as functions wrapped inside a standalone helper class. You then pass this helper object directly into your DTO converter function (refer to class `BookstoreHelper` in the Bookstore demo for a practical example).

### Package Structure

While you can organize your packages only by layer or only by feature, a hybrid approach provides the best balance:
* **By Layer for Shared Data:** Define a top-level `repository` package. Because data models and query functions are shared across multiple unrelated features, keeping them centralized maximizes reuse. You can add `local` and `remote` sub-packages to separate database queries from external API clients.
* **By Feature for Business Logic:** Organize your `service` and `controller` packages strictly by business feature to keep your core use-case logic modular and clean.

### Explore the Bookstore Demo

To see how this architecture works, check the `bookstore` demo in this repository. This ready-to-run project shows how using Populator with small, reusable building blocks turns rather complex requirements into a clean system.

---

## Explore the Demos for More Details

There are two demos in the repository: simple and bookstore.

The "simple" demo demonstrates the following:
* Basic techniques of using Populator
* Building multi-level object structures
* Populator's concurrent capabilities
* Using mutable data models
* Building object structures having many-to-many relationship
* Filtering parent and child data
* Supporting legacy database tables with composite IDs
* Usage of BatchFinder

The "bookstore" demo implements a simplified, simulated book navigation system for an online bookstore. It uses a standard three-tier architecture: repository, service, and an interactive command-line UI representing the controller and client. It demonstrates the following:
* Using Populator as a system-wide strategy
* Eliminating complexity by building small, reusable building blocks
* Integrating external API calls seamlessly with database access functions

Both the "simple" and "bookstore" demos are completely independent of third-party frameworks or libraries like Spring, making them easy to compile and run. They simulate database queries and remote APIs using native Java collection classes and streams.

---

## Contributing, Issues & License

### Project Maintenance Status
`Populator` is designed to be a complete, lightweight utility with a fixed scope. Because of its tiny footprint, it is considered mature and does not require active ongoing feature maintenance. 

* **Issues:** Bugs or architectural issues are highly welcome! Please feel free to open an issue in the GitHub tracker.
* **Contributions:** To keep the codebase lightweight and dependency-free, pull requests for new feature branches are not being accepted. 
* **License:** Distributed under the MIT License. See `LICENSE` for more information.
