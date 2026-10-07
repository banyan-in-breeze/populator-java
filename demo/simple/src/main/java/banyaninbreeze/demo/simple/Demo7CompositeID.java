package banyaninbreeze.demo.simple;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import banyaninbreeze.populator.Populator;

/**
 * Demo for matching parent and child objects using composite IDs
 * 
 * Data object structure in this demo:
 * Team --- (many) Members
 */
public class Demo7CompositeID {
    public static void main(String[] args) {
        var demo = new Demo7CompositeID();
        demo.showcaseCompositeID();
    }

    /******************************************************************************************
     * Showcase: Parent and child objects associated with composite IDs
     *  - Define a composite ID class or record. Ensure its equals() method works properly.
     *  - If possible, add this composite ID object as a field in both your parent and child classes.
     *  - If you do this, you can pass the composite ID getters to the Populator exactly 
     *    like regular IDs.
     *  - If you cannot modify the classes, follow the pattern inside this method: instantiate 
     *    the composite ID objects directly inside the getters. 
     *  - Note: Creating IDs on the fly generates more objects during population, which increases 
     *    memory usage for large datasets.
     ******************************************************************************************/
    public void showcaseCompositeID() {

        List<Team> teams = findAllTeams();
        teams = Populator.<Team, Member, CompositeID>populateMany(
                        teams,
                        team -> new CompositeID(team.departmentId(), team.sequence()),
                        member -> new CompositeID(member.departmentId(), member.sequence()),
                        this::findMembersByCompositeIds,
                        Team::withMembers
                    );

        printResult("Static populating methods", teams);
    }

    /****************
     * Print Result
     ***************/
    private void printResult(String note, List<Team> teams) {
        System.out.println("*** " + note + " ***");
        teams.forEach(team -> {
            System.out.println(team.name());
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
                .map(map -> new Team((Integer) map.get("departmentId"),
                                     (Integer) map.get("sequence"), (String) map.get("name")))
                .toList();
    }

    private List<Member> findMembersByCompositeIds(Collection<CompositeID> compositeIds) {
        return MEMBER_SAMPLES.stream()
                .filter(map -> compositeIds.contains(
                        new CompositeID((Integer) map.get("departmentId"), (Integer) map.get("sequence"))))
                .map(map -> new Member((Integer) map.get("id"), (String) map.get("name"),
                                     (String) map.get("employeeId"), (Integer) map.get("departmentId"),
                                     (Integer) map.get("sequence")))
                .toList();
    }

    /****************************************************
     * Definitions of Data Object classes/records
     ***************************************************/

    private static record Team(
        Integer departmentId,
        Integer sequence,
        String name,

        // fields of child objects to be populated
        List<Member> members
    ) {
        public Team(Integer departmentId, Integer sequence, String name) {
            this(departmentId, sequence, name, null);
        }

        public Team withMembers(List<Member> members) {
            return new Team(this.departmentId, this.sequence, this.name, members);
        }

    }

    private static record Member(Integer id, String name, String employeeId, Integer departmentId, Integer sequence) {}

    private static record CompositeID(Integer departmentId, Integer sequence) {}

    /****************************************************
     * Sample data
     ***************************************************/

    private static List<Map<String, Object>> TEAM_SAMPLES = List.of(
        Map.of("departmentId", 10, "sequence", 1, "name", "IT - Database Team"),
        Map.of("departmentId", 10, "sequence", 2, "name", "IT - App Team"),
        Map.of("departmentId", 11, "sequence", 1, "name", "Product - Customer Support"),
        Map.of("departmentId", 11, "sequence", 2, "name", "Product - Sales")
    );

    private static List<Map<String, Object>> MEMBER_SAMPLES = List.of(
        Map.of("id", 1, "name", "Elena", "employeeId", "101", "departmentId", 10, "sequence", 1),
        Map.of("id", 2, "name", "Julian", "employeeId", "102", "departmentId", 10, "sequence", 1),
        Map.of("id", 3, "name", "David", "employeeId", "103", "departmentId", 10, "sequence", 2),
        Map.of("id", 4, "name", "Ryan", "employeeId", "104", "departmentId", 11, "sequence", 1),
        Map.of("id", 5, "name", "Sarah", "employeeId", "105", "departmentId", 11, "sequence", 2),
        Map.of("id", 6, "name", "Sofia", "employeeId", "106", "departmentId", 11, "sequence", 2),
        Map.of("id", 7, "name", "Ethan", "employeeId", "107", "departmentId", 11, "sequence", 1)
    );
}
