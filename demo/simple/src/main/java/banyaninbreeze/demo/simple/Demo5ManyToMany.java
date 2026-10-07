package banyaninbreeze.demo.simple;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import banyaninbreeze.populator.Populator;
import banyaninbreeze.populator.Populator.ChildModifier;
import banyaninbreeze.populator.Populator.PopulateParams;

/**
 * Demo for many-to-many parent-child relationship
 * 
 * Data object structure in this demo:
 * Team --- (many) TeamMember (many) --- Members
 */
public class Demo5ManyToMany {
    public static void main(String[] args) {
        var demo = new Demo5ManyToMany();
        demo.showcaseManyToMany();
    }

    /******************************************************************************************
     * Showcase: One team has multiple members, and one member can belong to multiple teams.
     *   An association table team_member is added as usual.
     * - Define the structure in 3 levels exactly like the table structure:
     *   Team (one) -> (many) TeamMember (one) -> (one) Member
     * - This demo only demonstrates getting teams and populating them with members.
     *   Getting members and populating them with teams are similar.
     ******************************************************************************************/
    public void showcaseManyToMany() {
        List<Team> teams = findAllTeams();
        teams = Populator.of(teams)
                .populate(paramsTeamWithTeamMembers(
                    teamMembers -> Populator.of(teamMembers)
                        .populate(paramsTeamMemberWithMember())
                        .run()
                ))
                .run();
        printResult("Many to many", teams);
    }

    private PopulateParams<Team, TeamMember, Integer> paramsTeamWithTeamMembers(ChildModifier<TeamMember> childModifier) {
        return PopulateParams.<Team, TeamMember, Integer>ofMany(
                    Team::id,
                    TeamMember::teamId,
                    teamIds -> childModifier.modify(findTeamMembersByTeamIds(teamIds)),
                    Team::withTeamMembers
            );
    }

    private PopulateParams<TeamMember, Member, Integer> paramsTeamMemberWithMember() {
        return PopulateParams.<TeamMember, Member, Integer>ofOne(
                    TeamMember::memberId,
                    Member::id,
                    this::findMembersByIds,
                    TeamMember::withMember
            );
    }

    /****************
     * Print Result
     ***************/
    private void printResult(String note, List<Team> teams) {
        System.out.println("*** " + note + " ***");
        teams.forEach(team -> {
            System.out.println(team.name());
            if (team.teamMembers() == null || team.teamMembers().isEmpty())
                System.out.println("  <No member>");
            else
                System.out.println("  Members: "
                 + String.join(", ",
                     team.teamMembers().stream()
                     .map(teamMember -> teamMember.member().name())
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
     * SELECT ... FROM team_member WHERE team_id IN (:teamIds)
     */
    private List<TeamMember> findTeamMembersByTeamIds(Collection<Integer> teamIds) {
        return TEAM_MEMBER_SAMPLES.stream()
                .filter(map -> teamIds.contains((Integer) map.get("teamId")))
                .map(map -> new TeamMember((Integer) map.get("id"), (Integer) map.get("teamId"), (Integer) map.get("memberId")))
                .toList();
    }

    /**
     * Simulate query:
     * SELECT ... FROM member WHERE id IN (:ids)
     */
    private List<Member> findMembersByIds(Collection<Integer> ids) {
        return MEMBER_SAMPLES.stream()
                .filter(map -> ids.contains((Integer) map.get("id")))
                .map(map -> new Member((Integer) map.get("id"), (String) map.get("name")))
                .toList();
    }

    /****************************************************
     * Definitions of Data Object classes/records
     ***************************************************/

    private static record Team(
        Integer id,
        String name,

        // fields of child objects to be populated
        List<TeamMember> teamMembers
    ) {
        public Team(Integer id, String name) {
            this(id, name, null);
        }

        public Team withTeamMembers(List<TeamMember> teamMembers) {
            return new Team(this.id, this.name, teamMembers);
        }

    }

    private static record Member(Integer id, String name) {}

    private static record TeamMember(
        Integer id,
        Integer teamId,
        Integer memberId,

        // fields of child objects to be populated
        Member member
    ) {
        public TeamMember(Integer id, Integer teamId, Integer memberId) {
            this(id, teamId, memberId, null);
        }

        public TeamMember withMember(Member member) {
            return new TeamMember(this.id, this.teamId, this.memberId, member);
        }
    }


    /****************************************************
     * Sample data
     ***************************************************/

    private static List<Map<String, Object>> TEAM_SAMPLES = List.of(
        Map.of("id", 1, "name", "Frontend Team"),
        Map.of("id", 2, "name", "Backend Team"),
        Map.of("id", 3, "name", "QA Team")
    );

    private static List<Map<String, Object>> MEMBER_SAMPLES = List.of(
        Map.of("id", 1, "name", "Elena"),
        Map.of("id", 2, "name", "Julian"),
        Map.of("id", 3, "name", "David"),
        Map.of("id", 4, "name", "Ryan"),
        Map.of("id", 5, "name", "Sarah"),
        Map.of("id", 6, "name", "Sofia"),
        Map.of("id", 7, "name", "Ethan")
    );

    private static List<Map<String, Object>> TEAM_MEMBER_SAMPLES = List.of(
        Map.of("id", 1, "teamId", 1, "memberId", 1),
        Map.of("id", 2, "teamId", 3, "memberId", 1),
        Map.of("id", 3, "teamId", 1, "memberId", 2),
        Map.of("id", 4, "teamId", 2, "memberId", 2),
        Map.of("id", 5, "teamId", 3, "memberId", 2),
        Map.of("id", 6, "teamId", 2, "memberId", 3),
        Map.of("id", 7, "teamId", 2, "memberId", 4),
        Map.of("id", 8, "teamId", 3, "memberId", 5),
        Map.of("id", 9, "teamId", 1, "memberId", 6),
        Map.of("id", 10, "teamId", 3, "memberId", 6),
        Map.of("id", 11, "teamId", 1, "memberId", 7)
    );
}
