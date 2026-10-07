package banyaninbreeze.demo.simple;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import banyaninbreeze.populator.Populator;

/**
 * Demo for using Populator with mutable data object classes.
 * 
 * Data object structure in this demo:
 * Team --- (one) Manager
 *      |-- (many) Members
 */
public class Demo4Mutable {

    public static void main(String[] args) {
        var demo = new Demo4Mutable();
        demo.showcaseMutable();
    }

    /******************************************************************************************
     * Showcase: Use mutable class for models
     *  - Modifies the parent objects directly using setters during the population process.
     *  - Avoids the object-copying overhead of immutable withers.
     *  - Reduces memory usage, which is ideal for high-throughput systems.
     ******************************************************************************************/
    public void showcaseMutable() {

        List<Team> teams = findAllTeams();
        teams = Populator.<Team, Manager, Integer>populateOne(
                        teams,
                        Team::getId,
                        Manager::teamId,
                        this::findManagersByTeamIds,
                        Team::setManager // <- use setter which returns "this"
                    );
        teams = Populator.<Team, Member, Integer>populateMany(
                        teams,
                        Team::getId,
                        Member::teamId,
                        this::findMembersByTeamIds,
                        // In case you cannot make the setter return "this", do this:
                        (team, members) -> { team.setMembers(members); return team; }
                    );

        printResult("Use mutable model classes", teams);
    }

    /****************
     * Print Result
     ***************/
    private void printResult(String note, List<Team> teams) {
        System.out.println("*** " + note + " ***");
        teams.forEach(team -> {
            System.out.println(team.getName());
            if (team.getManager() == null)
                System.out.println("  <No manager>");
            else
                System.out.println("  Manager: " + team.getManager().name());
            if (team.getMembers() == null || team.getMembers().isEmpty())
                System.out.println("  <No member>");
            else
                System.out.println("  Members: " + String.join(", ", team.getMembers().stream().map(Member::name).toList()));
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

    private List<Manager> findManagersByTeamIds(Collection<Integer> teamIds) {
        return MANAGER_SAMPLES.stream()
                .filter(map -> teamIds.contains((Integer) map.get("teamId")))
                .map(map -> new Manager((Integer) map.get("id"), (String) map.get("name"), (Integer) map.get("teamId")))
                .toList();
    }

    private List<Member> findMembersByTeamIds(Collection<Integer> teamIds) {
        return MEMBER_SAMPLES.stream()
                .filter(map -> teamIds.contains((Integer) map.get("teamId")))
                .map(map -> new Member((Integer) map.get("id"), (String) map.get("name"),
                                     (String) map.get("employeeId"), (Integer) map.get("teamId")))
                .toList();
    }

    /****************************************************
     * Definitions of Data Object classes/records
     ***************************************************/

    private static class Team {
        Integer id;
        String name;

        // fields of child objects to be populated
        Manager manager;
        List<Member> members;

        public Team(Integer id, String name) {
            this.id = id;
            this.name = name;

            this.manager = null;
            this.members = null;
        }

        public Integer getId() { return id; }
        public String getName() { return name; }
        public Manager getManager() { return manager; }
        public List<Member> getMembers() { return members; }

        public Team setManager(Manager manager) {
            this.manager = manager;
            return this;
        }

        public void setMembers(List<Member> members) {
            this.members = members;
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
