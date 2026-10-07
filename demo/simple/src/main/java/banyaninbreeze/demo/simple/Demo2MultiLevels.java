package banyaninbreeze.demo.simple;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import banyaninbreeze.populator.Populator;
import banyaninbreeze.populator.Populator.ChildModifier;
import banyaninbreeze.populator.Populator.PopulateParams;

/**
 * Demo for building object structure with multiple levels of objects.
 * A level can have multiple siblings.
 * 
 * Data object structure in this demo:
 * Team --- (one) Manager
 *      |-- (many) Members --- (one) Title
 *                         |-- (many) ContactMethods
 */
public class Demo2MultiLevels {
    public static void main(String[] args) {
        var demo = new Demo2MultiLevels();
        demo.showcaseAllInOnePlace();
        demo.showcaseChildModifier();
    }

    /******************************************************************************************
     * Showcase #1: All in one place
     * - A bit messy, right?
     * - While not recommended for complex structures, it shows how the whole logic works.
     ******************************************************************************************/
    public void showcaseAllInOnePlace() {
        List<Team> teams = findAllTeams();
        teams = Populator.of(teams)
            .populateOne(Team::id, Manager::teamId, this::findManagersByTeamIds, Team::withManager)
            .populateMany(
                Team::id,
                Member::teamId,
                teamIds -> {
                    // KEY STRATEGY:
                    // Fetch children, then immediately populate their sub-children inline
                    var members = this.findMembersByTeamIds(teamIds);
                    return Populator.of(members)
                        .populateOne(Member::titleId, Title::id, this::findTitlesByIds, Member::withTitle)
                        .populateMany(Member::employeeId, ContactMethod::employeeId,
                                        this::findContactMethodsByEmployeeIds, Member::withContactMethods)
                        .run();
                },
                Team::withMembers
            )
            .run();

        printResult("All in one place", teams);
    }

    /******************************************************************************************
     * Showcase #2: Use PopulateParams and ChildModifier together
     * - Create one method for each PopulateParams object - they are reusable
     * - Add a ChildModifier callback to the PopulateParams creators to populate nested
     *   data for the children
     * - It looks much cleaner, right?
     ******************************************************************************************/
    public void showcaseChildModifier() {
        List<Team> teams = findAllTeams();
        teams = Populator.of(teams)
                .populate(paramsTeamWithManager())
                .populate(paramsTeamWithMembers(
                    // The ChildModifier intercepts the list, running the sub-population
                    members -> Populator.of(members)
                        .populate(paramsMemberWithTitle())
                        .populate(paramsMemberWithContactMethods())
                        .run()
                ))
                .run();

        printResult("Use PopulateParams and ChildModifier together", teams);
    }

    // PopulateParams creator for populating team with manager
    private PopulateParams<Team, Manager, Integer> paramsTeamWithManager() {
        return PopulateParams.<Team, Manager, Integer>ofOne(
                    Team::id,
                    Manager::teamId,
                    this::findManagersByTeamIds,
                    Team::withManager
                );
    }

    // Creates PopulateParams to populate teams with members.
    // The childModifier parameter allows callers to populate nested data on the members.
    // Pass ChildModifier.none() if you don't need to populate anything.
    private PopulateParams<Team, Member, Integer> paramsTeamWithMembers(ChildModifier<Member> childModifier) {
        return PopulateParams.<Team, Member, Integer>ofMany(
                    Team::id,
                    Member::teamId,
                    teamIds -> childModifier.modify(this.findMembersByTeamIds(teamIds)),
                    Team::withMembers
                );
    }

    // PopulateParams creator for populating member with title
    private PopulateParams<Member, Title, Integer> paramsMemberWithTitle() {
        return PopulateParams.<Member, Title, Integer>ofOne(
                    Member::titleId,
                    Title::id,
                    this::findTitlesByIds,
                    Member::withTitle
                );
    }

    // PopulateParams creator for populating member with contact methods
    private PopulateParams<Member, ContactMethod, String> paramsMemberWithContactMethods() {
        return PopulateParams.<Member, ContactMethod, String>ofMany(
                    Member::employeeId,
                    ContactMethod::employeeId,
                    this::findContactMethodsByEmployeeIds,
                    Member::withContactMethods
                );
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
            else {
                System.out.println("  Members:");
                team.members().forEach(member -> {
                    System.out.println("    " + member.name());
                    System.out.println("      Title: " + (member.title() == null ? "<No title>" : member.title().title()));
                    System.out.println("      Contacts: "
                         + (member.contactMethods() == null || member.contactMethods().isEmpty()? "<No contact method>" :
                            String.join(", ", member.contactMethods().stream().map(ContactMethod::value).toList())));
                });
            }
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
                                     (String) map.get("employeeId"), (Integer) map.get("titleId"), (Integer) map.get("teamId")))
                .toList();
    }

    private List<ContactMethod> findContactMethodsByEmployeeIds(Collection<String> employeeIds) {
        return CONTACT_METHOD_SAMPLES.stream()
                .filter(map -> employeeIds.contains((String) map.get("employeeId")))
                .map(map -> new ContactMethod((Integer) map.get("id"), (String) map.get("employeeId"),
                                     (String) map.get("type"), (String) map.get("value")))
                .toList();
    }

    private List<Title> findTitlesByIds(Collection<Integer> ids) {
        return TITLE_SAMPLES.stream()
                .filter(map -> ids.contains((Integer) map.get("id")))
                .map(map -> new Title((Integer) map.get("id"), (String) map.get("title")))
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

    private static record Member(
        Integer id,
        String name,
        String employeeId,
        Integer titleId,
        Integer teamId,

        // fields of child objects to be populated
        Title title,
        List<ContactMethod> contactMethods
    ) {
        public Member(Integer id, String name, String employeeId, Integer titleId, Integer teamId) {
            this(id, name, employeeId, titleId, teamId, null, null);
        }

        public Member withTitle(Title title) {
            return new Member(this.id, this.name, this.employeeId, this.titleId, this.teamId,
                             title, this.contactMethods);
        }

        public Member withContactMethods(List<ContactMethod> contactMethods) {
            return new Member(this.id, this.name, this.employeeId, this.titleId, this.teamId,
                             this.title, contactMethods);
        }
    }

    private static record ContactMethod(Integer id, String employeeId, String type, String value) {}

    private static record Title(Integer id, String title) {}

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
        Map.of("id", 1, "name", "Elena", "employeeId", "101", "titleId", 1, "teamId", 1),
        Map.of("id", 2, "name", "Julian", "employeeId", "102", "titleId", 2, "teamId", 1),
        Map.of("id", 3, "name", "David", "employeeId", "103", "titleId", 1, "teamId", 2),
        Map.of("id", 4, "name", "Ryan", "employeeId", "104", "titleId", 1, "teamId", 2),
        Map.of("id", 5, "name", "Sarah", "employeeId", "105", "titleId", 1, "teamId", 2),
        Map.of("id", 6, "name", "Sofia", "employeeId", "106", "titleId", 3, "teamId", 3),
        Map.of("id", 7, "name", "Ethan", "employeeId", "107", "titleId", 4, "teamId", 3)
    );

    private static List<Map<String, Object>> CONTACT_METHOD_SAMPLES = List.of(
        Map.of("id", 1, "employeeId", "101", "type", "Phone", "value", "555-555-0111"),
        Map.of("id", 2, "employeeId", "101", "type", "Email", "value", "elena@example.com"),
        Map.of("id", 3, "employeeId", "102", "type", "Phone", "value", "555-555-0121"),
        Map.of("id", 4, "employeeId", "102", "type", "Phone", "value", "555-555-0131"),
        Map.of("id", 5, "employeeId", "102", "type", "Email", "value", "julian@example.com"),
        Map.of("id", 6, "employeeId", "103", "type", "Phone", "value", "555-555-0154"),
        Map.of("id", 7, "employeeId", "104", "type", "Phone", "value", "555-555-0176"),
        Map.of("id", 8, "employeeId", "104", "type", "Email", "value", "ryan@example.com"),
        Map.of("id", 9, "employeeId", "106", "type", "Phone", "value", "555-555-0188"),
        Map.of("id", 10, "employeeId", "107", "type", "Email", "value", "ethan@example.com")
    );

    private static List<Map<String, Object>> TITLE_SAMPLES = List.of(
        Map.of("id", 1, "title", "Programmer"),
        Map.of("id", 2, "title", "UI Designer"),
        Map.of("id", 3, "title", "QA Analyst"),
        Map.of("id", 4, "title", "QA Engineer")
    );
}
