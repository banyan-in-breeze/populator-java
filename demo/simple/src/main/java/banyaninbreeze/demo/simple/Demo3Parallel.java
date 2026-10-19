package banyaninbreeze.demo.simple;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import banyaninbreeze.populator.Populator;
import banyaninbreeze.populator.Populator.ChildModifier;
import banyaninbreeze.populator.Populator.PopulateParams;

/**
 * Demo for populating sibling data in parallel.
 * 
 * Data object structure in this demo:
 * Team --- (one) Manager
 *      |-- (many) Members --- (one) Title
 *                         |-- (many) ContactMethods
 * 
 */
public class Demo3Parallel {
    public static void main(String[] args) {
        // =========================================================================
        // JAVA VERSION COMPATIBILITY NOTE:
        // If you are running Java 21+ and want to utilize virtual threads:
        // 1. In pom.xml, change <maven.compiler.release>17</maven.compiler.release>
        //    to 21 or higher
        // 2. Uncomment the line of code below.
        // =========================================================================
        // Populator.setGlobalExecutorService(Executors.newVirtualThreadPerTaskExecutor());

        var demo = new Demo3Parallel();
        demo.showcaseCompare();
        demo.showcaseCustomExecutorAndMode();

        // Shutdown executor service to prevent the threads from holding up the app
        Populator.getGlobalExecutorService().shutdown();

        demo.showcaseForceSequential();
    }

    /******************************************************************************************
     * Showcase: Compare performance of sequential versus parallel data population
     * - 30~50 ms delay is added to each "find" method to simulate real database access.
     * - Builds the same object structure twice: once sequential and once in parallel.
     * - Prints the elapsed time to compare performance.
     ******************************************************************************************/
    public void showcaseCompare() {
        long startTime;
        List<Team> teams;

        // Call this function once to "warm-up" JVM with classloading and so on
        testSequential();

        // Sequential
        startTime = System.currentTimeMillis();
        teams = testSequential();
        System.out.println("Sequential - time elapsed: " + (System.currentTimeMillis() - startTime) + " ms");
        printResult(teams);
        
        // Parallel
        startTime = System.currentTimeMillis();
        teams = testParallel();
        System.out.println("Parallel - time elapsed: " + (System.currentTimeMillis() - startTime) + " ms");
        printResult(teams);
    }

    /******************************************************************************************
     * Showcase: Use specific executor service on individual populators, run with dynamic mode
     *****************************************************************************************/
    public void showcaseCustomExecutorAndMode() {
        long startTime;
        List<Team> teams;

        ExecutorService executorService = Executors.newFixedThreadPool(4);

        // Sequential mode (will ignore the provided executor service)
        startTime = System.currentTimeMillis();
        teams = testCustom(executorService, false);
        System.out.println("Sequential mode - time elapsed: " + (System.currentTimeMillis() - startTime) + " ms");
        printResult(teams);

        // Parallel mode with a specific executor service
        startTime = System.currentTimeMillis();
        teams = testCustom(executorService, true);
        System.out.println("Parallel mode - time elapsed: " + (System.currentTimeMillis() - startTime) + " ms");
        printResult(teams);

        executorService.shutdown();
    }

    /******************************************************************************************
     * Showcase: Force all population to be sequential even for runParallel().
     * In case of testing, debugging or limited resources (like CPU), we may want to force all
     * runParallel() to be sequential in an app. Change Populator's global executor service to
     * SameThreadExecutorService, then all runParallel() or runWithMode(true) will sequentially
     * run population on the current thread just like run().
     * This way you don't need to change runParallel() to run() in your code.
     *****************************************************************************************/
    public void showcaseForceSequential() {
        long startTime;
        List<Team> teams;

        // Replace the global executor service so that all runParallel() become sequential
        Populator.setGlobalExecutorService(new SameThreadExecutorService());

        // Parallel with "same thread executor service"
        startTime = System.currentTimeMillis();
        teams = testParallel();
        System.out.println("Parallel with same thread executor service - time elapsed: "
                         + (System.currentTimeMillis() - startTime) + " ms");
        printResult(teams);
    }

    private List<Team> testSequential() {
        List<Team> teams = findAllTeams();
        return Populator.of(teams)
                .populate(paramsTeamWithManager())
                .populate(paramsTeamWithMemebers(
                    members -> Populator.of(members)
                        .populate(paramsMemberWithTitle())
                        .populate(paramsMemberWithContactMethods())
                        .run() // <-- run sequentially, alternatively call runWithMode(false)
                ))
                .run(); // <-- run sequentially, alternatively call runWithMode(false)
    }

    private List<Team> testParallel() {
        List<Team> teams = findAllTeams();
        return Populator.of(teams)
                .populate(paramsTeamWithManager())
                .populate(paramsTeamWithMemebers(
                    members -> Populator.of(members)
                        .populate(paramsMemberWithTitle())
                        .populate(paramsMemberWithContactMethods())
                        .runParallel() // <-- run in parallel, alternatively call runWithMode(true)
                ))
                .runParallel(); // <-- run in parallel, alternatively call runWithMode(true)
    }

    private List<Team> testCustom(ExecutorService customExecutorService, boolean parallelMode) {
        List<Team> teams = findAllTeams();
        return Populator.of(customExecutorService, teams) // Use custom executor service
                .populate(paramsTeamWithManager())
                .populate(paramsTeamWithMemebers(
                    members -> Populator.of(customExecutorService, members) // Use custom executor service
                        .populate(paramsMemberWithTitle())
                        .populate(paramsMemberWithContactMethods())
                        .runWithMode(parallelMode) // <-- run with required mode, either sequential or parallel
                ))
                .runWithMode(parallelMode); // <-- run with required mode, either sequential or parallel
    }

    /****************
     * Print Result
     ***************/
    private void printResult(List<Team> teams) {
        System.out.println(teams.size() + " teams loaded and populated.");
        System.out.println();
    }

    /***********************************
     * PopulateParams creator functions
     **********************************/

    // Creates PopulateParams to populate teams with manager
    private PopulateParams<Team, Manager, Integer> paramsTeamWithManager() {
        return PopulateParams.<Team, Manager, Integer>ofOne(
                    Team::id,
                    Manager::teamId,
                    this::findManagersByTeamIds,
                    Team::withManager
                );
    }

    // Creates PopulateParams to populate teams with members
    private PopulateParams<Team, Member, Integer> paramsTeamWithMemebers(ChildModifier<Member> childModifier) {
        return PopulateParams.<Team, Member, Integer>ofMany(
                    Team::id,
                    Member::teamId,
                    teamIds -> childModifier.modify(this.findMembersByTeamIds(teamIds)),
                    Team::withMembers
                );
    }

    // Creates PopulateParams to populate members with title
    private PopulateParams<Member, Title, Integer> paramsMemberWithTitle() {
        return PopulateParams.<Member, Title, Integer>ofOne(
                    Member::titleId,
                    Title::id,
                    this::findTitlesByIds,
                    Member::withTitle
                );
    }

    // Creates PopulateParams to populate member with contact methods
    private PopulateParams<Member, ContactMethod, String> paramsMemberWithContactMethods() {
        return PopulateParams.<Member, ContactMethod, String>ofMany(
                    Member::employeeId,
                    ContactMethod::employeeId,
                    this::findContactMethodsByEmployeeIds,
                    Member::withContactMethods
                );
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
        delay(30);
        return MANAGER_SAMPLES.stream()
                .filter(map -> teamIds.contains((Integer) map.get("teamId")))
                .map(map -> new Manager((Integer) map.get("id"), (String) map.get("name"), (Integer) map.get("teamId")))
                .toList();
    }

    private List<Member> findMembersByTeamIds(Collection<Integer> teamIds) {
        delay(40);
        return MEMBER_SAMPLES.stream()
                .filter(map -> teamIds.contains((Integer) map.get("teamId")))
                .map(map -> new Member((Integer) map.get("id"), (String) map.get("name"),
                                     (String) map.get("employeeId"), (Integer) map.get("titleId"), (Integer) map.get("teamId")))
                .toList();
    }

    private List<ContactMethod> findContactMethodsByEmployeeIds(Collection<String> employeeIds) {
        delay(50);
        return CONTACT_METHOD_SAMPLES.stream()
                .filter(map -> employeeIds.contains((String) map.get("employeeId")))
                .map(map -> new ContactMethod((Integer) map.get("id"), (String) map.get("employeeId"),
                                     (String) map.get("type"), (String) map.get("value")))
                .toList();
    }

    private List<Title> findTitlesByIds(Collection<Integer> ids) {
        delay(30);
        return TITLE_SAMPLES.stream()
                .filter(map -> ids.contains((Integer) map.get("id")))
                .map(map -> new Title((Integer) map.get("id"), (String) map.get("title")))
                .toList();
    }

    private void delay(long milliseconds) {
        try {
            Thread.sleep(milliseconds);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
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
