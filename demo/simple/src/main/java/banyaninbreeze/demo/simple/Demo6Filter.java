package banyaninbreeze.demo.simple;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import banyaninbreeze.populator.Populator;
import banyaninbreeze.populator.Populator.PopulateParams;

/**
 * Demo for filtering child objects and parent objects using certain conditions
 * 
 * Data object structure in this demo:
 * Team --- (many) Members
 */
public class Demo6Filter {
    public static void main(String[] args) {
        var demo = new Demo6Filter();
        demo.showcaseFilterChildObjects();
        demo.showcaseFilterParentAndChildObjects();
    }

    /******************************************************************************************
     * Showcase #1: Filter the child objects with additional parameters
     ******************************************************************************************/
    public void showcaseFilterChildObjects() {
        // Find teams and populate all members
        List<Team> teams = findAllTeams();
        var availableFlags = List.of(Boolean.TRUE, Boolean.FALSE);
        teams = Populator.of(teams)
                .populate(paramsTeamWithMembers(availableFlags))
                .run();
        printResult("Teams and all members", teams);

        // Find teams and populate them with only available members
        teams = findAllTeams();
        availableFlags = List.of(Boolean.TRUE);
        teams = Populator.of(teams)
                .populate(paramsTeamWithMembers(availableFlags))
                .run();
        printResult("Teams and available members only", teams);
    }

    /******************************************************************************************
     * Showcase #2: Filter parent objects by properties of there associated child objects
     ******************************************************************************************/
    public void showcaseFilterParentAndChildObjects() {
        // Find teams that only have available members and populate them with only available members
        var availableFlags = List.of(Boolean.TRUE);
        List<Team> teams = findTeamsByAvailableMembers(availableFlags);
        teams = Populator.of(teams)
                .populate(paramsTeamWithMembers(availableFlags))
                .run();
        printResult("Teams that have available members", teams);
    }

    private PopulateParams<Team, Member, Integer> paramsTeamWithMembers(List<Boolean> availableFlags) {
        return PopulateParams.<Team, Member, Integer>ofMany(
                    Team::id,
                    Member::teamId,
                    teamIds -> findMembersByTeamIds(teamIds, availableFlags),
                    Team::withMembers
            );
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
                System.out.println("  Members: "
                 + String.join(", ",
                     team.members().stream()
                     .map(member -> member.name() + (member.available() ? " (available)" : " (unavailable)"))
                     .toList()));
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
     * SELECT team. ...
     * FROM team INNER JOIN member ON team.id = member.teamId
     * WHERE member.available = true
     */
    private List<Team> findTeamsByAvailableMembers(List<Boolean> availableFlags) {
        List<Integer> teamIds = MEMBER_SAMPLES.stream()
                .filter(map -> availableFlags.contains((Boolean) map.get("available")))
                .map(map -> (Integer) map.get("teamId"))
                .toList();
        return TEAM_SAMPLES.stream()
                .filter(map -> teamIds.contains((Integer) map.get("id")))
                .map(map -> new Team((Integer) map.get("id"), (String) map.get("name")))
                .toList();
    }

    /**
     * Simulate query:
     * SELECT ... FROM member
     * WHERE teamId IN (:ids) AND available IN (:availableFlags)
     */
    private List<Member> findMembersByTeamIds(Collection<Integer> teamIds, List<Boolean> availableFlags) {
        return MEMBER_SAMPLES.stream()
                .filter(map -> teamIds.contains((Integer) map.get("teamId"))
                                 && availableFlags.contains((Boolean) map.get("available")))
                .map(map -> new Member((Integer) map.get("id"), (String) map.get("name"),
                                     (String) map.get("employeeId"), (Integer) map.get("teamId"),
                                      (Boolean) map.get("available")))
                .toList();
    }

    /****************************************************
     * Definitions of Data Object classes/records
     ***************************************************/

    private static record Team(
        Integer id,
        String name,

        // fields of child objects to be populated
        List<Member> members
    ) {
        public Team(Integer id, String name) {
            this(id, name, null);
        }

        public Team withMembers(List<Member> members) {
            return new Team(this.id, this.name, members);
        }

    }

    private static record Member(Integer id, String name, String employeeId, Integer teamId, Boolean available) {}

    /****************************************************
     * Sample data
     ***************************************************/

    private static List<Map<String, Object>> TEAM_SAMPLES = List.of(
        Map.of("id", 1, "name", "Frontend Team"),
        Map.of("id", 2, "name", "Backend Team"),
        Map.of("id", 3, "name", "QA Team")
    );

    private static List<Map<String, Object>> MEMBER_SAMPLES = List.of(
        Map.of("id", 1, "name", "Elena", "employeeId", "101", "teamId", 1, "available", true),
        Map.of("id", 2, "name", "Julian", "employeeId", "102", "teamId", 1, "available", true),
        Map.of("id", 3, "name", "David", "employeeId", "103", "teamId", 2, "available", false),
        Map.of("id", 4, "name", "Ryan", "employeeId", "104", "teamId", 2, "available", true),
        Map.of("id", 5, "name", "Sarah", "employeeId", "105", "teamId", 2, "available", true),
        Map.of("id", 6, "name", "Sofia", "employeeId", "106", "teamId", 3, "available", false),
        Map.of("id", 7, "name", "Ethan", "employeeId", "107", "teamId", 3, "available", false)
    );
}
