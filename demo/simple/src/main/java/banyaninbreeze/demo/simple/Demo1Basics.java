package banyaninbreeze.demo.simple;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import banyaninbreeze.populator.Populator;
import banyaninbreeze.populator.Populator.PopulateParams;

/**
 * Demo for Populator basics using 2 levels of objects.
 * 
 * Data object structure in this demo:
 * Team --- (one) Manager
 *      |-- (many) Members
 *
 * All the model classes are immutable (record) in this demo.
 * Another demo showcases how to work with mutable objects.
 */
public class Demo1Basics {
    public static void main(String[] args) {
        var demo = new Demo1Basics();
        demo.showcaseTypical();
        demo.showcaseStatic();
        demo.showcasePopulateParams();
        demo.showcaseTypeArgs();
        demo.showcaseAdaptChildFinderParamType();
        demo.showcaseUnderTheHood();
    }

    /******************************************************************************************
     * Showcase #1: Typical usage of populateOne() and populateMany()
     *  - Instantiate a Populator with the parent objects.
     *  - Chain the populate method calls.
     *  - Call run(), runParallel(), or runWithMode() to execute all population steps.
     ******************************************************************************************/
    public void showcaseTypical() {
        List<Team> teams = findAllTeams();
        teams = Populator.of(teams) // instantiate Populator with the parent list to be populated
                .populateOne(  // for one-to-ONE association
                    Team::id,  // getter for the parent field used to match the child
                    Manager::teamId,  // getter for the child field used to match the parent
                    this::findManagersByTeamIds,  // function that fetches child objects
                    Team::withManager  // wither/setter that assigns the child and returns the updated parent object
                )
                .populateMany(
                    Team::id,  // getter for the parent field used to match the child
                    Member::teamId,  // getter for the child field used to match the parent
                    this::findMembersByTeamIds,  // function that fetches child objects
                    Team::withMembers  // wither/setter that assigns the children and returns the updated parent object
                )
                .run();

        printResult("Typical usage of population methods", teams);
    }

    /******************************************************************************************
     * Showcase #2: static Populator.populateOne() and Populator.populateMany()
     *  - A one-shot quick call to populate child objects
     *  - Call once for each population step.
     *  - Returns a new parent list with the children populated.
     *  - Pass the resulting parent list to subsequent calls to populatie other children.
     ******************************************************************************************/
    public void showcaseStatic() {

        List<Team> teams = findAllTeams();
        teams = Populator.populateOne(  // for one-to-ONE association
                        teams,  // parent list to be populated
                        Team::id,  // getter for the parent field used to match the child
                        Manager::teamId,  // getter for the child field used to match the parent
                        this::findManagersByTeamIds,  // function that fetches child objects
                        Team::withManager  // wither/setter that assigns the child and returns the updated parent object
                    );
        teams = Populator.populateMany(  // for one-to-MANY association
                        teams,  // parent list to be populated
                        Team::id,  // getter for the parent field used to match the child
                        Member::teamId,  // getter for the child field used to match the parent
                        this::findMembersByTeamIds,  // function that fetches child objects
                        Team::withMembers  // wither/setter that assigns the children and returns the updated parent object
                    );

        printResult("Static population methods", teams);
    }

    /******************************************************************************************
     * Showcase #3: Use the PopulateParams class
     *  - Instantiate PopulateParams objects with ofOne() or ofMany().
     *  - Pass them to either static or non-static populating methods.
     *  - Use the populate() method instead of populateOne() and populateMany() because the
     *    PopulateParams object already knows whether it's a one-to-one or one-to-many relationship.
     *  - Creating PopulateParams is separate from calling Populator methods. This means you can
     *    reuse them, and defined in a separate function or a separate class.
     ******************************************************************************************/
    public void showcasePopulateParams() {
        PopulateParams<Team, Manager, Integer> paramsTeamWithManager = 
            PopulateParams.ofOne(  // for one-to-ONE association
                Team::id,  // getter for the parent field used to match the child
                Manager::teamId,  // getter for the child field used to match the parent
                this::findManagersByTeamIds,  // function that fetches child objects
                Team::withManager  // wither/setter that assigns the child and returns the updated parent object
            );

        List<Team> teams = findAllTeams();
        teams = Populator.of(teams)
                .populate(paramsTeamWithManager) // non-static
                .run();
        teams = Populator.populate(teams, paramsTeamWithMembers()); // static; calls a method to get params

        printResult("Use class PopulateParams", teams);
    }

    private PopulateParams<Team, Member, Integer> paramsTeamWithMembers() {
        return PopulateParams.ofMany(  // for one-to-MANY association
                    Team::id,  // getter for the parent field used to match the child
                    Member::teamId,  // getter for the child field used to match the parent
                    this::findMembersByTeamIds,  // function that fetches child objects
                    Team::withMembers  // wither/setter that assigns the children and returns the updated parent object
            );
    }
    
    /******************************************************************************************
     * Showcase #4: Good practice: specify type arguments
     *  - Populator.<Parent, Child, IDType>populateOne() instead of Populator.populateOne()
     *  - Same for Populator.populateMany()
     *  - Populator.of(parents).<Child, IDType>populateOne() instead of Populator.of(parents).populateOne()
     *  - Same for Populator.of(parents).populateMany()
     *  - PopulateParams.<Parent, Child, IDType>ofOne() instead of PopulateParams.ofOne()
     *  - Same for PopulateParams.ofMany()
     *  - The extra type arguments are not required, but they allow IDE to detect specific errors
     ******************************************************************************************/
    public void showcaseTypeArgs() {
        List<Team> teams = findAllTeams();

        var paramsTeamWithManager = 
            PopulateParams.<Team, Manager, Integer>ofOne( // <-- specify type arguments
                Team::id,
                Manager::teamId,
                this::findManagersByTeamIds,
                Team::withManager
            );

        teams = Populator.of(teams)
            .populate(paramsTeamWithManager)
            .run();
        teams = Populator.<Team, Member, Integer>populateMany( // <-- specify type arguments
                teams,
                Team::id,
                Member::teamId,
                this::findMembersByTeamIds,
                Team::withMembers
            );

        printResult("Good practice: specify type arguments", teams);
    }

    /******************************************************************************************
     * Showcase #5: Adapt the parameter type of the childFinder function
     *  - Populator methods and PopulateParams expect childFinder to accept a Collection<ID> parameter.
     *  - If your existing finder function requires a List or another specific type you cannot change 
     *    (e.g., findByIds(List<String> ids)), passing the method reference directly will not compile.
     *  - Solution: Use a lambda to adapt the type, such as List.copyOf(ids) or new ArrayList<>(ids).
     ******************************************************************************************/
    public void showcaseAdaptChildFinderParamType() {
        List<Team> teams = findAllTeams();
        teams = Populator.<Team, Member, Integer>populateMany(
                teams,
                Team::id,
                Member::teamId,
                // this::findMembersByTeamIdList,  // <-- Doesn't work. findMembersByTeamIdList() doesn't accept Collection<>
                teamIds -> this.findMembersByTeamIdList(List.copyOf(teamIds)), // Convert the Collection to List
                Team::withMembers
            );

        printResult("Adapt the parameter type of the childFinder function", teams);
    }

    /*******************************************************************************************
     * Showcase #6: Under the hood
     * - Explain the core logic of Populator.populateMany() under the hood
     ******************************************************************************************/
    public void showcaseUnderTheHood() {
        List<Team> teams = findAllTeams();
        Set<Integer> teamIds = teams.stream().map(Team::id).collect(Collectors.toSet());
        List<Member> members = findMembersByTeamIds(teamIds);
        Map<Integer, List<Member>> memberMap =
                members.stream().collect(Collectors.groupingBy(Member::teamId));
        teams = teams.stream().map(team -> team.withMembers(memberMap.get(team.id()))).toList();
        printResult("Under the hood", teams);
    }

    /****************
     * Print Result
     ***************/
    private void printResult(String note, List<Team> teams) {
        System.out.println("*** " + note + " ***");
        teams.forEach(team -> {
            System.out.println(team.name());
            if (team.manager() == null)
                System.out.println("  <No manager>");
            else
                System.out.println("  Manager: " + team.manager().name());
            if (team.members() == null || team.members().isEmpty())
                System.out.println("  <No member>");
            else
                System.out.println("  Members: " + String.join(", ", team.members().stream().map(Member::name).toList()));
        });
        System.out.println("");
    }

    /****************************************************
     * Simulate "Repository" methods that fetch data
     ***************************************************/

    private List<Team> findAllTeams() {
        return TEAM_SAMPLES.stream()
                .map(map -> new Team((Integer) map.get("id"), (String) map.get("name")))
                .toList();
    }

    /**
     * Simulate query:
     * SELECT ... FROM manager WHERE team_id IN (:teamIds)
     */
    private List<Manager> findManagersByTeamIds(Collection<Integer> teamIds) {
        return MANAGER_SAMPLES.stream()
                .filter(map -> teamIds.contains((Integer) map.get("teamId")))
                .map(map -> new Manager((Integer) map.get("id"), (String) map.get("name"), (Integer) map.get("teamId")))
                .toList();
    }

    /**
     * Simulate query:
     * SELECT ... FROM member WHERE team_id IN (:teamIds)
     */
    private List<Member> findMembersByTeamIds(Collection<Integer> teamIds) {
        return MEMBER_SAMPLES.stream()
                .filter(map -> teamIds.contains((Integer) map.get("teamId")))
                .map(map -> new Member((Integer) map.get("id"), (String) map.get("name"),
                                     (String) map.get("employeeId"), (Integer) map.get("teamId")))
                .toList();
    }

    /**
     * This method takes List instead of Collection of team IDs.
     * Simulate query:
     * SELECT ... FROM member WHERE team_id IN (:teamIds)
     */
    private List<Member> findMembersByTeamIdList(List<Integer> teamIds) {
        return MEMBER_SAMPLES.stream()
                .filter(map -> teamIds.contains((Integer) map.get("teamId")))
                .map(map -> new Member((Integer) map.get("id"), (String) map.get("name"),
                                     (String) map.get("employeeId"), (Integer) map.get("teamId")))
                .toList();
    }

    /****************************************************
     * Definitions of Data Object classes/records
     ***************************************************/

    private static record Team(
        Integer id,
        String name,

        // fields of child objects to be populated
        Manager manager,
        List<Member> members
    ) {
        public Team(Integer id, String name) {
            this(id, name, null, null);
        }

        public Team withManager(Manager manager) {
            return new Team(this.id, this.name, manager, this.members);
        }

        public Team withMembers(List<Member> members) {
            return new Team(this.id, this.name, this.manager, members);
        }

    }

    private static record Manager(Integer id, String name, Integer teamId) {}

    private static record Member(Integer id, String name, String employeeId, Integer teamId) {}

    /****************************************************
     * Sample data
     ***************************************************/

    private static List<Map<String, Object>> TEAM_SAMPLES = List.of(
        Map.of("id", 1, "name", "Frontend Team"),
        Map.of("id", 2, "name", "Backend Team"),
        Map.of("id", 3, "name", "QA Team")
    );

    private static List<Map<String, Object>> MANAGER_SAMPLES = List.of(
        Map.of("id", 1, "name", "Paul", "teamId", 1),
        Map.of("id", 2, "name", "John", "teamId", 2),
        Map.of("id", 3, "name", "Rose", "teamId", 3)
    );

    private static List<Map<String, Object>> MEMBER_SAMPLES = List.of(
        Map.of("id", 1, "name", "Elena", "employeeId", "101", "teamId", 1),
        Map.of("id", 2, "name", "Julian", "employeeId", "102", "teamId", 1),
        Map.of("id", 3, "name", "David", "employeeId", "103", "teamId", 2),
        Map.of("id", 4, "name", "Ryan", "employeeId", "104", "teamId", 2),
        Map.of("id", 5, "name", "Sarah", "employeeId", "105", "teamId", 2),
        Map.of("id", 6, "name", "Sofia", "employeeId", "106", "teamId", 3),
        Map.of("id", 7, "name", "Ethan", "employeeId", "107", "teamId", 3)
    );
}
