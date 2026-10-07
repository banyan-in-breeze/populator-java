package banyaninbreeze.demo.simple;
import java.util.Collection;
import java.util.List;
import java.util.stream.IntStream;

import banyaninbreeze.populator.BatchFinder;

/**
 * Demo for BatchFinder utility class.
 * 
 * BatchFinder is a utility class that comes with Populator.
 * 
 * When using Populator, you often need to query data using lists of IDs. Many databases 
 * have a strict limit on how many parameters you can pass in a single query. If your ID list is 
 * too long, it will exceed this limit and cause a database error. 
 * 
 * BatchFinder fixes this by splitting large ID lists into smaller batches. It queries the 
 * database batch-by-batch and combines all the results back into a single list so you never hit 
 * those database limits.
 *
 * Besides capping the batch size, BatchFinder uses a smart algorithm to save database resources. 
 * It minimizes the number of database trips while keeping the batch sizes consistent. Keeping sizes 
 * consistent helps the database reuse its cached query plans (prepared statements).
 * 
 * Use BatchFinder whenever your ID lists can grow larger than 200 items.
 */
public class Demo8BatchFinder {
    public static void main(String[] args) {
        var demo = new Demo8BatchFinder();
        demo.showcase();
    }

    /******************************************************************************************
     * Showcase: Use BatchFinder to fetch objects batch-by-batch
     * 
     * With the default global batch sizes in BatchFinder class:
     * - If the ID list size is <= 180, it runs in one batch.
     * - If the ID list size is > 180 and <= 960, it runs in one or two batches.
     * - if the ID list size is > 960, it runs in two or more batches.
     *****************************************************************************************/
    public void showcase() {
        runFinder(126);
        runFinder(195);
        runFinder(379);
        runFinder(823);
        runFinder(1450);
        runFinder(8560);
    }

    private void runFinder(int size) {
        List<Integer> ids = IntStream.range(0, size).boxed().toList();;
        System.out.println("*** Size: " + size + " ***");

        List<String> result = BatchFinder.find(ids, this::findFieldByIds);

        System.out.println("Result Size: " + result.size());
        System.out.println("");
    }

    /**
     * Simulate query:
     * SELECT a_string_field FROM my_table WHERE id IN (:ids)
     * 
     * It prints the size, the first value and the last value of the given ID list.
     */
    private List<String> findFieldByIds(Collection<Integer> ids) {
        var idList = List.copyOf(ids);
        System.out.println("In this batch:  IDListSize = " + idList.size() + "    FirstID = " + idList.get(0) + "    LastID = " + idList.get(idList.size() - 1));
        return ids.stream().map(id -> "String-" + id).toList();
    }
}
