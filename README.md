# Populator

**A small but powerful Java util that works with any framework (Spring JPA, JDBC, jOOQ, MyBatis) and/or remote APIs to build complex object structures.**

When building Java APIs, we often need to fetch and map data with multiple one-to-many or many-to-one like this:

```text
Team
  ├── (one) Manager
  └── (many) Members
        ├── (many) ContactMethods
        └── (many) Assignments
```
One level is easy with JPA's `JOIN FETCH` or manual mapping. But this kind of graph is full of traps:

* N+1 queries
* Cartesian product explosion
* Monster SQL statements
* Messy, hard-to-maintain code

jOOQ handles it decently, but the query can be verbose and the result can contain a lot of duplicate data. And if you already use Spring but want to use jOOQ to handle it, you face the learning curve and the hard decision of choosing which one or both.

On top of that, **what if ContactMethods comes from a remote API, not your DB**? No ORM framework is designed to build a clean object graph from mixed data sources like DB plus remote APIs.

Worse yet, in the next sprint, the customer adds projects and tasks to the structure:

```text
Team
  ├── (one) Manager
  ├── (many) Members
  │     ├── (many) ContactMethods (from a remote API)
  │     └── (many) Assignments
  └── (many) Projects
        └── (many) Tasks
```

How do you add it to your already-complex code?

**This is where `Populator` comes into play.** It makes the complex problem simple.

* Stay with your framework — JPA, JDBC, jOOQ, MyBatis, etc.
* No N+1, no Cartesian product
* Seamlessly work with REST API, gRPC, etc.
* Code made simple, clean, reusable, and easy to expand
* Zero magic, no annotations, no dependencies, plain Java
* 340 lines, 2 files — **copy-paste straight into your project**

Performance? See a dedicated section below.

More than a problem solver, it can be an **application-wide strategy** that keeps your application code clean, reusable, and scalable.

## Table of Contents

- [Getting Started](#getting-started)
- [Introduce Populator](#introduce-populator)
- [Build More Complex Object Structures](#build-more-complex-object-structures)
    - [Two or More Siblings (Horizontal Population)](#two-or-more-siblings-horizontal-population)
    - [Two or More Nested Levels (Vertical Population)](#two-or-more-nested-levels-vertical-population)
    - [More Complex Population](#more-complex-population)
- [`PopulateParams` and `ChildModifier`: Cleaner and Reusable Code](#populateparams-and-childmodifier-cleaner-and-reusable-code)
    - [Use `PopulateParams` to Bundle Parameters](#use-populateparams-to-bundle-parameters)
    - [Use `ChildModifier` to Abstract Nested Population](#use-childmodifier-to-abstract-nested-population)
- [Build the Full Team Structure](#build-the-full-team-structure)
- [Building Blocks from Any Data Source](#building-blocks-from-any-data-source)
- [Populate in Parallel](#populate-in-parallel)
- [Use `BatchFinder` to Find Data in Batches](#use-batchfinder-to-find-data-in-batches)
- [Model Class Types: Immutable, Mutable, Semi-mutable, Entity](#model-class-types-immutable-mutable-semi-mutable-entity)
- [Performance Analysis](#performance-analysis)
- [Use Populator as an Application-Wide Strategy](#use-populator-as-an-application-wide-strategy)
- [Helpful Tips](#helpful-tips)
    - [When Child Setter Returns `void` Instead of `this`](#when-child-setter-returns-void-instead-of-this)
    - [Adapting Finder Parameter Types](#adapting-finder-parameter-types)
    - [One-shot Static Version of Populate Methods](#one-shot-static-version-of-populate-methods)
    - [Type Hinting for Better IDE Error Detection](#type-hinting-for-better-ide-error-detection)
    - [Using `Populator` with Transaction](#using-populator-with-transaction)
    - [Child Finder Arguments](#child-finder-arguments)
- [Explore the Demos for More Details](#explore-the-demos-for-more-details)
- [Contributing, Issues & License](#contributing-issues--license)

## Getting Started

Just copy 2 files into your project — no jar, no dependency, Java 8 compatible:

* `Populator.java`
* `BatchFinder.java`

Package: `banyaninbreeze.populator` (in `populator` folder)

#### Explore Demos

Check the `demo` folder:

* `simple` and `bookstore` examples
* Requires Java 17+
* Copy-paste to your machine and run
* No third-party dependencies

## Introduce Populator

Use the simplest example to begin with: `team` and `member` DB tables of one-to-many relationship:

```text
Team
  └── (many) Members
```

The domain model (Can be JPA entity, POJO, Java record, etc.):

```java
// Populator also works with immutable classes and Java records.
// This demo uses mutable classes.

class Team {
    Integer id;
    String name;
    List<Member> members; // To be populated later

    // ... getters/setters/constructors
}

class Member {
    Integer id;
    String name;
    Integer teamId;

    // ... getters/setters/constructors
}
```

Instead of building one SQL to join the tables, create 2 separate queries - one for team, one for member. Use Spring JPA/JDBC, jOOQ or any framework to implement:

```java
// SQL: SELECT id, name FROM team WHERE ...
List<Team> findTeamsBySomeCriteria(/* ... some criteria */)  { ... }

// SQL: SELECT id, name, team_id FROM member WHERE team_id IN (:teamIds)
List<Member> findMembersByTeamIds(Collection<Integer> teamIds)  { ... }
```

Use `Populator` to call them and stitch the results together:

```java
List<Team> findTeamsWithMembersPopulated(/* ... some criteria */) {
    List<Team> teams = findTeamsBySomeCriteria(/* ... some criteria */);
    teams = Populator.of(teams)
            .populateMany(Team::getId, Member::getTeamId,
                this::findMembersByTeamIds, Team::setMembers)
            .run();
    return teams; // teams now populated with members
}
```

What this does:
* Creates a Populator instance for teams
* Calls `populateMany()` to describe how to populate teams. It takes four arguments:
  * `Team::getId` and `Member::getTeamId` - the fields that link the two types
  * The finder method to load members by team IDs: `findMembersByTeamIds`
  * The setter that assigns members to a team
* Calls `run()`, which executes the whole procedure and returns teams populated with members

Use `populateMany()` for one-to-many relationships. For one-to-one or many-to-one, use `populateOne()`.

#### How It Works Under the Hood

To fetch teams and members and stitch them together, you can manually write the following code:

```java
List<Team> teams = findTeamsBySomeCriteria(/* ... some criteria */);

Set<Integer> teamIds = teams.stream()
                    .map(Team::getId).collect(Collectors.toSet());
List<Member> members = findMembersByTeamIds(teamIds);

// Group members by team ID
Map<Integer, List<Member>> memberMap = 
    members.stream().collect(Collectors.groupingBy(Member::getTeamId));

teams = teams.stream()
    .map(team -> team.setMembers(memberMap.get(team.getId())))
    .toList();
```

Since it finds all members with all team IDs at once, it avoids N+1 queries.

At its core, `Populator` does the same thing, but with generics instead of hardcoded types like `Team`, `Member`, and `Integer`.

**Note:** We assume the `setMembers()` in `Team` returns `this` instead of `void`. See "Helpful Tips" later in this document for how to handle setters that returns `void`.

## Build More Complex Object Structures

### Two or More Siblings (Horizontal Population)

This structure has 2 children under `Team`:

```text
Team
  ├── (one) Manager
  └── (many) Members
```
Chain populate calls to populate all children:

```java
teams = Populator.of(teams)
        .populateOne(Team::getId, Manager::getTeamId, 
            this::findManagersByTeamIds, Team::setManager)
        .populateMany(Team::getId, Member::getTeamId,
            this::findMembersByTeamIds, Team::setMembers)
        .run();
```

### Two or More Nested Levels (Vertical Population)

This structure has 2 levels of nesting:

```text
Team
  └── (many) Members
        └── (many) Assignments
```

Do this:

```java
teams = Populator.of(teams)
    .populateMany(Team::getId, Member::getTeamId,
        // Instead of passing this::findMembersByTeamIds,
        // pass a lambda so that we can populate members before returning
        teamIds -> {
            var members = this.findMembersByTeamIds(teamIds);
            // Populate members with assignments
            return Populator.of(members)
                .populateMany(Member::getId, Assignment::getMemberId, 
                        this::findAssignmentsByMemberIds,
                        Member::setAssignments)
                .run();
        },
        Team::setMembers
    )
    .run();
```

How it works:
1. Inside `Populator.of(teams).populateMany()`, it calls the child finder to fetch members.
2. Inside that finder, we not only fetch members via `findMembersByTeamIds()`, but also populate them with assignments before returning.
3. Back in the outer `populateMany()`, it assigns those members (already populated with assignments) to their teams.

### More Complex Population

To build this structure:

```text
Team
  ├── (one) Manager
  └── (many) Members
        └── (many) Assignments
```

Combine horizontal and vertical population:

```java
teams = Populator.of(teams)
        .populateOne(Team::getId, Manager::getTeamId, 
            this::findManagersByTeamIds, Team::setManager)
        .populateMany(Team::getId, Member::getTeamId,
            teamIds -> {
                var members = this.findMembersByTeamIds(teamIds);
                return Populator.of(members)
                    .populateMany(Member::getId, Assignment::getMemberId, 
                            this::findAssignmentsByMemberIds,
                            Member::setAssignments)
                    .run();
            },
            Team::setMembers)
        .run();
```

## `PopulateParams` and `ChildModifier`: Cleaner and Reusable Code

### Use `PopulateParams` to Bundle Parameters

Instead of passing four separate parameters (two ID getters, one finder, and one setter), you can bundle them into a `PopulateParams` object and pass it to `populate()`.

```java
PopulateParams<Team, Member, Integer> populateParams =
    PopulateParams.ofMany(
        Team::getId, Member::getTeamId,
        this::findMembersByTeamIds, Team::setMembers
    );

teams = Populator.of(teams)
    .populate(populateParams)
    .run();
```

Use `PopulateParams.ofOne()` for one-to-one or many-to-one, and `PopulateParams.ofMany()` for one-to-many.

Note that the method is `populate()`, not `populateOne()` or `populateMany()` — `PopulateParams` already knows whether it's one or many.

This also lets you define params once and reuse them across the app.

### Use `ChildModifier` to Abstract Nested Population

`ChildModifier` is a simple interface that comes with Populator. It's similar to `Function<T, U>`, but designed to abstract nested population. Here's its source code (you don't need to write it):


```java
public interface ChildModifier<C> {
    // Passthrough: returns the list untouched
    static <C> ChildModifier<C> none() {
        return children -> children;
    }

    // Modify the child collection and/or return it
    List<C> modify(List<C> children);
}
```

Use `ChildModifier` with `PopulateParams` to build this structure:

```text
Team
  └── (many) Members
        └── (many) Assignments
```
```java
PopulateParams<Team, Member, Integer> paramsTeamWithMembers(
            ChildModifier<Member> childModifier) {
    return PopulateParams.ofMany(Team::getId, Member::getTeamId,
        teamIds -> childModifier.modify(findMembersByTeamIds(teamIds)),
        Team::setMembers);
}

PopulateParams<Member, Assignment, Integer> paramsMemberWithAssignments() {
    return PopulateParams.ofMany(Member::getId, Assignment::getMemberId,
        this::findAssignmentsByMemberIds,
        Member::setAssignments);
}

// Case A: Populate members WITH assignments
List<Team> findTeamsWithMembersAndAssignments(/* ... some criteria */) {
    List<Team> teams = findTeamsBySomeCriteria(/* ... some criteria */);

    return Populator.of(teams)
        .populate(paramsTeamWithMembers(
            // Lambda of ChildModifier:
            members -> Populator.of(members)
                .populate(paramsMemberWithAssignments())
                .run()
        ))
        .run();
}

// Case B: Populate members with NO assignments
List<Team> findTeamsWithMembersOnly(/* ... some criteria */) {
    List<Team> teams = findTeamsBySomeCriteria(/* ... some criteria */);

    return Populator.of(teams)
        .populate(paramsTeamWithMembers(ChildModifier.none()))
        .run();
}
```
By using `ChildModifier` in the `PopulateParams` creator methods, the creators don't populate children themselves — they leave that decision to the caller. This makes the creators reusable and lets callers chain population to any nesting level.

## Build the Full Team Structure

```text
Team
  ├── (one) Manager
  ├── (many) Members
  │     ├── (many) ContactMethods (from a remote API)
  │     └── (many) Assignments
  └── (many) Projects
        └── (many) Tasks
```

Model classes:
```java
class Team {
    Integer id;
    String name;

    Manager manager;
    List<Member> members; // To be populated later
    List<Project> projects; // To be populated later
    // ... getters/setters/constructors
}

class Manager { ... }
class Member {
    Integer id;
    String name;
    Integer teamId;
    String employeeId;

    List<ContactMethod> contactMethods; // To be populated later
    List<Assignment> asignments; // To be populated later
    // ... getters/setters/constructors
}

class ContactMethod { ... }
class Assignment { ... }

class Project {
    Integer id;
    String name;
    Integer teamId;

    List<Task> tasks; // To be populated later
    // ... getters/setters/constructors
}
class Task { ... }
```

Finder functions:
```java
List<Team> findTeamsBySomeCriteria(...) { ... }

List<Manager> findManagersByTeamIds(Collection<Integer> teamIds) { ... }
List<Member> findMembersByTeamIds(Collection<Integer> teamIds) { ... }

List<ContactMethod> findContactMethodsByEmployeeIds( // remote API client
    Collection<String> employeeIds) { ... }
List<Assignment> findAssignmentsByMemberIds(Collection<Integer> memberIds) { ... }

List<Project> findProjectsByTeamIds(Collection<Integer> teamIds) { ... }
List<Task> findTasksByProjectIds(Collection<Integer> projectIds) { ... }
```

`PopulateParams` Creators
```java
// Each is one call to PopulateParams.ofMany() / ofOne()
PopulateParams<Team, Manager, Integer> paramsTeamWithManager() {...}
PopulateParams<Team, Member, Integer> paramsTeamWithMembers(
    ChildModifier<Member> childModifier) {...}
PopulateParams<Member, ContactMethod, String> paramsMemberWithContactMethods() {...}
PopulateParams<Member, Assignment, Integer> paramsMemberWithAssignments()  {...}
PopulateParams<Team, Project, Integer> paramsTeamWithProjects(
    ChildModifier<Project> childModifier) {...}
PopulateParams<Project, Task, Integer>  paramsProjectWithTasks() {...}
```

Populate all levels:

```java
List<Team> findTeamsWithEverythingPopulated(/* ... some criteria */) {
    List<Team> teams = findTeamsBySomeCriteria(/*...*/);
    
    teams = Populator.of(teams)
        .populate(paramsTeamWithManager())
        .populate(paramsTeamWithMembers(
            members -> Populator.of(members)
                .populate(paramsMemberWithContactMethods())
                .populate(paramsMemberWithAssignments())
                .run()
        ))
        .populate(paramsTeamWithProjects(
            projects -> Populator.of(projects)
                .populate(paramsProjectWithTasks())
                .run()
        ))
        .run();

    return teams; // already populated with the multi-level objects
}
```

The benefits are:

* Breaks complex assembly into small, simple functions.
* Avoids N+1 queries and Cartesian product issues.
* Keeps all assembly logic in one place — no need to jump between functions to see how the structure is built.
* The small functions are reusable across the whole application.
* AI-generated code using Populator is simple and easy to review

### Building Blocks from Any Data Source

Populator does not require each building block to come from a single database table. A finder can retrieve or produce data in many ways, including:

* Joining multiple tables in a database query
* Computing aggregate values using functions such as `SUM` or `MAX`
* Calling remote APIs
* Reading from in-memory collections such as `List` and `Map`

The key requirement is that the finder conforms to Populator’s API and provides data that can be associated with its parent objects through a common key, such as `Team.id` and `Member.teamId`.

**Populator is responsible for assembling the object structure; finders are responsible for retrieving or computing the data.** This separation lets you combine data from different sources without tying object assembly to a particular storage technology.

## Populate in Parallel

`Populator` can fetch data in parallel to run faster. Just change `run()` to `runParallel()`.

Here is part of the example above, rewritten for parallel execution:

```java
List<Team> findTeamsWithEverythingPopulated(/* ... some criteria */) {
    List<Team> teams = findTeamsBySomeCriteria(...);
    
    teams = Populator.of(teams)
        .populate(paramsTeamWithManager())
        .populate(paramsTeamWithMembers(
            members -> Populator.of(members)
                .populate(paramsMemberWithContactMethods())
                .populate(paramsMemberWithAssignments())
                .runParallel() // <-- parallel
        ))
        .populate(paramsTeamWithProjects(
            projects -> Populator.of(projects)
                .populate(paramsProjectWithTasks())
                .run()
        ))
        .runParallel(); // <-- parallel

    return teams;
}
```

Alternatively, call `runWithMode(boolean parallel)` to switch between sequential and parallel execution dynamically.

#### How It Works under the Hood

When you call `runParallel()`, Populator fetches all child datasets concurrently. After all fetches complete, it stitches the children into their parents one by one.This two-phase design is important for immutable parents that use `withXxx(child)` methods. It ensures no child data is lost when multiple children are set.

#### Using a Virtual Thread Pool with Java 21+

By default, `Populator` uses `Executors.newCachedThreadPool()` as its global thread pool. You can change it with a static setter.

On Java 21 or higher, it's recommended to switch to a virtual thread pool. Virtual threads don't block OS threads, so they give better throughput for network I/O and help avoid thread-starvation deadlocks in nested parallel runs.

Add this to your application startup:
```java
Populator.setGlobalExecutorService(Executors.newVirtualThreadPerTaskExecutor());
```

#### Avoiding Thread-Starvation Deadlock

`Populator`'s default thread pool will not cause thread-starvation deadlock.

But if you replace the default pool with a fixed-size pool, you can get a deadlock when you call `runParallel()` at multiple nested levels. For example, outer tasks can block waiting for inner tasks, while all threads in the pool are already in use.

If you must use a limited pool, use `runParallel()` at only one level - either the parent or the child, not both.

## Use `BatchFinder` to Find Data in Batches

When using `Populator`, your application will rely heavily on batch lookup methods (like `findMembersByIds(Collection<Integer> ids)`). However, passing large or unpredictable numbers of IDs directly to a database can cause two major problems:
1. **Parameter Limits:** Many databases fail if an `IN` clause exceeds maximum parameter limits (e.g., Oracle's 1,000-item limit).
2. **Cache Bloat:** Passing a varying number of IDs (like anywhere between 1 to 1000) forces the database to re-compile execution plans for each variation, bloating the prepared statement cache.

`BatchFinder` is a utility class coming with Populator. It splits a large collection of IDs into predictable, standardized batch sizes, calls the provided finder function for each batch of IDs, and merges the results into one list. This solves both problems.

Here is an example:
```java
public List<Member> findMembersByIds(Collection<Integer> ids) {
    return BatchFinder.find(ids, this::localFindMembersByIds);
}

// This is the actual function that finds members from the database
private List<Member> localFindMembersByIds(Collection<Integer> ids) { ... }
```

The best practice is to encapsulate `BatchFinder` directly inside your repository. If you use Spring Data JDBC/JPA's Repository interface to implement the finders, you can add an "interface default" function for `findMembersByIds()`, while `localFindMembersByIds()` is your actual data access function annotated with `@Query`. This keeps your `Populator` chains completely clean.

## Model Class Types: Immutable, Mutable, Semi-mutable, Entity

Populator can stitch together both immutable and mutable parents and children. The key is the setter parameter passed to `populateOne()`, `populateMany()`, `PopulateParams.ofOne()`, or `PopulateParams.ofMany()`.

The setter must do two things: assign the child object(s) to the parent, and return that parent instance. This can be an immutable object's copy method (a "wither" like `withMembers`) or a mutable object's setter that returns `this`. You can use Lombok to auto-generate these setters or withers.

#### For Immutable Classes and Records

```java
record Team(
    Integer id,
    String name,
    List<Member> members // Child objects populate
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

#### Use Semi-mutable Classes if Possible
If you have control over your data models, the best approach is to make your core primitive fields `final` (like `id` and `name` in the `Team` class) and keep your child object references mutable with setters. 

For added protection from unwanted changes outside your data layer, make the setter methods package-scoped and define your `PopulateParams` creator methods within that same package.

#### Memory Usage Considerations
For high-throughput production systems, using semi-mutable or mutable data models is highly recommended. It uses much less memory and runs faster because it prevents heavy object copying and constant garbage collection cleanups during deep nesting runs.

#### Entity Class

In the team-member example, if you map the SQL result to a `Team` object with Spring Data JPA / JDBC, you’ll need to leave the `members` field untouched during mapping. For example, mark it with `@Transient` if `Team` is an entity, or create a constructor that excludes members for projections.

For other data-access frameworks, there are different ways to do the same thing.

## Performance Analysis

Performance is a complicated question with many moving variables.

`Populator` uses multiple smaller queries to do what a single query with multiple `JOINs` does. So what does that mean for performance?

**One thing to clarify first**: we are not discussing the performance of `Populator` itself, since it is just a stitching tool. We are comparing the *multiple-small-query approach* versus the *single-query approach*. The *multiple-small-query approach* has long existed — `Populator` just makes it extremely easy to implement.

### 1. Comparing the Approaches with Examples

Let's look at the two biggest variables:
* **Roundtrips to the database**
* **Amount of data fetched**

**Case #1**

Assume you want to fetch **10 teams** that have a total of **100 members** (an average of 10 members per team). 

The multiple-small-query approach fetches data from the team and member tables using 2 separate queries. The single-query approach joins the 2 tables together in 1 query.

* **The Multiple-Small-Query Approach:**
  * **Roundtrips:** 2 trips (one for teams, one for members).
  * **Data Fetched:** 110 data items = 10 teams + 100 members.
* **The Single-Query Approach (JOIN):**
  * **Roundtrips:** 1 trip.
  * **Data Fetched:** 200 data items = 100 team records (duplicated inside 100 joined rows) + 100 member records.

The multiple-small-query approach needs **1 extra network trip**, but it cuts the total amount of data by **90 data items (nearly 50%)**.

**Note**: *Data item sizes differ from table to table. The comparison illustrates relative growth and duplication trends, not exact byte counts.*

**Case #2**

What happens if the dataset grows to **100 teams** and **1,000 members**?

* **The Multiple-Small-Query Approach:**
  * **Roundtrips:** 2 trips.
  * **Data Fetched:** 1,100 data items = 100 teams + 1,000 members.
* **The Single-Query Approach (JOIN):**
  * **Roundtrips:** 1 trip.
  * **Data Fetched:** 2,000 data items = 1,000 team records (duplicated inside 1,000 joined rows) + 1,000 member records.

The multiple-small-query approach still needs **1 extra network trip**, but it cuts the total amount of data by **900 data items (nearly 50%)**.

**Case #3**

Let's add a third level: each member has 1 of 10 possible professional titles.

* **The Multiple-Small-Query Approach:**
  * **Roundtrips:** 3 trips (one for teams, one for members, one for titles).
  * **Data Fetched:** 1,110 data items = 100 teams + 1,000 members + 10 titles.
* **The Single-Query Approach (JOIN):**
  * **Roundtrips:** 1 trip.
  * **Data Fetched:** 3,000 data items = 1,000 team records (duplicates) + 1,000 member records + 1,000 title records (duplicates).

Now, the multiple-small-query approach needs **2 extra trips**, but it reduces the total amount of data by **1,890 data items (nearly 63%)**.

**Case #4**

The other end of the spectrum: you only fetch **1 team**, populate it with **1 manager** and **1 latest project**.

* **The Multiple-Small-Query Approach:**
  * **Roundtrips:** 3 trips (one for team, one for manager, one for project).
  * **Data Fetched:** 3 data items = 1 team + 1 manager + 1 project
* **The Single-Query Approach (JOIN):**
  * **Roundtrips:** 1 trip.
  * **Data Fetched:** 3 data items =  = 1 team + 1 manager + 1 project

Now multiple-small-query approach needs **2 extra trips** to fetch **about the same amount of data** as single-query approach.

### 2. Trends and Trade-offs

To summarize the trade-off:
* The **multiple-small-query** approach takes more roundtrips, fetches from equal amount to significantly smaller amount of data.
* The **single-query approach** takes fewer roundtrips, fetches from equal amount to significantly larger amount of data.

With the single-query approach, data volume increases **fast** as the following 3 values increase:
* The number of nesting levels (table joins).
* The number of rows fetched in each table.
* The size/width of each data row.

With the multiple-small-query approach, the number of roundtrips increases **slowly** as the following 2 values increase:
* The number of nesting levels: add 1 roundtrip for 1 extra level.
* The number of IDs: add 1 roundtrip for 1 extra batch.

**What More Roundtrips Mean:**
* **Network Latency**: If the app and DB are far apart, this hurts. But in prod they usually sit in the same local network, so a roundtrip is only 0.3 ms to 2 ms, sometimes as low as 0.1 ms. 3 queries add 0.3 ms to 6 ms, 10 queries add 1 ms to 20 ms - minor, but noticeable at the slow end. (But a single query joining 10 tables with lots of duplicate data can easily cost you far more than 20 ms.)

**What a Heavier Data Volume Means:**
* **CPU Burden:** Both the DB server and the app server spend more CPU cycles creating duplicate data objects.
* **Garbage Collection:** Short-lived, duplicate data triggers more frequent JVM Garbage Collection cycles.
* **Network Bandwidth:** Large datasets use up more network bandwidth.

**Additional Facts:**
* Multiple small queries rely on simple lookups, typically by primary key (`WHERE ID IN (?)`), which databases execute efficiently.
* Complex JOIN execution typically requires more database work than simple primary-key or indexed lookups, especially as the number of joined relationships grows. And when a centralized DB hits its hardware limit, scaling vertically gets expensive fast.

**Two Factors We Ignore Here:**
1. **Indexing:** We assume proper indexing, so both approaches execute efficiently on key columns.
2. **Execution Plan Cache:** Varying ID list sizes can bloat the cache, but `BatchFinder` uses preferred batch sizes to limit variations in SQL parameter counts and help improve plan-cache reuse.

### 3. Conclusions on Performance

Every prod setup is unique, but here is a practical rule of thumb for picking your strategy based on network layout:

* **If your app and DB share a fast local network (almost always the case in prod):**
  * For complex structures and larger datasets, multiple small queries are often competitive with or faster than single query.
  * For simple tables and smaller datasets, a single `JOIN` can be slightly faster, but only by a small margin.
  * *The Takeaway:* On a fast local network, `Populator` is generally safe to use from a performance perspective and may provide performance benefits as graph complexity grows.
* **If your app and DB are far apart (high latency):**
  * The single-query approach is more likely to be faster, because extra roundtrips get more expensive over a slower connection.
  * *The Takeaway:* Be cautious and measure your network lag before splitting queries.

**In the past, network latency made multiple small queries expensive. Today, network latency is much lower. The tradeoff has changed.**

Every prod system has its own limits. Under extreme traffic, running your own real-world benchmarks is the only way to be sure.

## Use Populator as an Application-Wide Strategy

You can use `Populator` in your applications in two ways.

The first is to use it as a simple utility whenever you need to handle one-to-many associations or assemble data from different data sources (such as local databases and remote APIs). 

The second way is to use it as a core architectural pattern to help create a highly scalable, maintainable, and high-performance application. To achieve this, you follow three clean steps:

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

#### Explore the Bookstore Demo

To see how this architecture works, check the `bookstore` demo in this repository. This ready-to-run project shows how using Populator with small, reusable building blocks turns rather complex requirements into a clean codebase.

## Helpful Tips

### When Child Setter Returns `void` Instead of `this`

In this example:

```java
teams = Populator.of(teams)
    .populateMany(Team::getId, Member::getTeamId,
        this::findMembersByTeamIds, Team::setMembers)
    .run();
```

The last parameter — the setter that assigns children to the parent — must return the parent. It can be a mutable setter that returns `this` or an immutable "wither" like withMembers.

A traditional JavaBean setter returns `void`, so it won't work. If you can't change it to return `this`, wrap it:

```java
teams = Populator.of(teams)
    .populateMany(Team::getId, Member::getTeamId,
        this::findMembersByTeamIds,
        (team, members) -> { team.setMembers(members); return team; }
    )
    .run();
```

### Adapting Finder Parameter Types

If the finder method was written by someone else and takes `List<T>` instead of `Collection<T>`, and you can't change it, adapt the `Collection<T>` parameter to the required type:

```java
// Takes List<Integer>
List<Member> findMembersByTeamIds(List<Integer> teamIds)  { ... }

...
teams = Populator.of(teams)
    .populateMany(Team::getId, Member::getTeamId,
        // Adapt the type of teamIds
        teamIds -> findMembersByTeamIds(new ArrayList<>(teamIds)),
        Team::setMembers)
    .run();
```

### One-shot Static Version of Populate Methods

In the examples above, we use `of(parents)` to create a `Populator`, then call `populate()` (or `populateOne()`, `populateMany()`) to define the relationship, and finally `run()` to execute. If you only need to populate one child field, there are static shortcuts that do all three steps in one call:

```java
// Static method to populate one-to-many data
teams = Populator.populateMany(teams, Team::getId, Member::getTeamId,
                        this::findMembersByTeamIds, Team::setMembers);

// Static method to populate one-to-one or many-to-one data
teams = Populator.populateOne(teams, Team::getId, Manager::getTeamId,
                        this::findManagersByTeamIds, Team::setManager);

// Static method to populate data using PopulateParams object
teams = Populator.populate(teams, paramsTeamWithProjects(ChildModifier.none));
```

### Type Hinting for Better IDE Error Detection

When you call `populateOne()`, `populateMany()`, or create a `PopulateParams` instance, you can explicitly add type arguments right in front of the method name like this:

```java
teams = Populator.of(teams)
        .<Team, Member, Integer>populateMany(Team::getId, Member::getTeamId,
                        this::findMembersByTeamIds, Team::setMembers)
        .run();
```

While type arguments are not required by the compiler here, adding them helps your IDE immediately identify syntax errors in your parameter list. 

Without the type arguments, if you make a typo (such as writing `Team::getid` instead of `Team::getId`), the IDE gets confused and underlines the *entire method call* in red, and you wonder where the actual error is. Once you add the type arguments explicitly, your IDE can point directly to the typo. If you want to keep your code clean, you can delete the type arguments after the block is finished.

### Using `Populator` with Transaction

Normally, you don't need to stress about strict data consistency when making separate queries to fetch your data. It's the same story with `Populator`: each chained `populate()` call runs as an independent query, so leaving them out of a transaction is usually fine.

However, if you do need to wrap your populator chain in a transaction, keep these two rules in mind:

1. **Stick to `run()` instead of `runParallel()`.** Parallel threads won't play nice with your database transaction. Even worse, holding an open transaction connection while trying to borrow more connections in separate threads can starve your connection pool.
2. **Never mix remote API calls with a database transaction.** If a third-party API slows down, it will hang onto your database connection for way too long, tanking your app's performance.

If you must combine data from a remote API with transactional database data, you have to separate the calls. Here is how:

1. Populate top-level data with remote API data
```java
// Open the database transaction using Spring's TransactionTemplate
List<Member> members = transactionTemplate.execute(status -> {
    List<Member> localMembers = findMembersBySomeCriteria(...);
    
    return Populator.of(localMembers)
        .populate(paramsMemberWithTitle()) 
        .run();
});

// Populate with remote API data outside the transaction
members = Populator.of(members)
        .populate(paramsMemberWithContactMethods())
        .run();
```

2. Populate nested sub-level data with remote API data:
*(Note: This pattern assumes that `Member`'s `setContactMethods()` is a mutable setter that returns the same `Member` object).*

```java
AtomicReference<List<Member>> atomicMembers = new AtomicReference<>();

// Open the database transaction
List<Team> teams = transactionTemplate.execute(status -> {
    List<Team> localTeams = findTeamsBySomeCriteria(...);
    
    return Populator.of(localTeams)
        .populate(paramsTeamWithManager())
        .populate(paramsTeamWithMembers(members -> {
            List<Member> populatedMembers = Populator.of(members)
                .populate(paramsMemberWithTitle())
                .run();
                
            // Cache the reference to process later
            atomicMembers.set(populatedMembers); 
            return populatedMembers;
        }))
        .run();
});

// Populate with remote API data outside the transaction
if (atomicMembers.get() != null) {
    Populator.of(atomicMembers.get())
        .populate(paramsMemberWithContactMethods())
        .run();
}
```

### Child Finder Arguments

All examples above use find-by-IDs finders that take a single argument — the collection of IDs. But you can pass additional arguments. For example, to populate teams with members that have a specific title:

```java
String title = "SENIOR_SOFTWARE_ENGINEER";
teams = Populator.of(teams)
    .populateMany(Team::getId, Member::getTeamId,
        // Pass title here
        teamIds -> findMembersByTeamIdsAndTitle(teamIds, title),
        Team::setMembers)
    .run();
```

If using `PopulateParams`:

```java
PopulateParams<Team, Member, Integer> paramsTeamWithMembers(
            String title, ChildModifier<Member> childModifier) {
    return PopulateParams.ofMany(Team::getId, Member::getTeamId,
            teamIds -> childModifier.modify(
                findMembersByTeamIdsAndTitle(teamIds, title)
            ),
            Team::setMembers);
}
```

```java
String title = "SENIOR_SOFTWARE_ENGINEER";
teams = Populator.of(teams)
    .populate(paramsTeamWithMembers(
        title,
        members -> Populator.of(members)
            .populate(paramsMemberWithContactMethods())
            .run()
    ))
    .run();
```

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

The "bookstore" demo implements simplified, simulated book navigation functionality for an online bookstore. It uses a standard three-tier architecture: repository, service, and an interactive command-line UI representing the controller and client. It demonstrates the following:
* Using Populator as an application-wide strategy
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
