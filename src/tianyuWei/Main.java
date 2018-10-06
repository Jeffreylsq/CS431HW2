package tianyuWei;

import java.util.Scanner;

public class Main
{
    static public String studentName() { return "Tianyu Wei"; }

    public static void main(String[] args)
    {
        runTestCases();

        /*                Fifo NRU 2nd LRU ARU
            Small:
                faults:     22  17  19  21  23
                reads:      22  17  19  21  23
                writes:      3   0   3   3   1
            Large:
                faults:     187 136 131 119 165
                reads:      187 136 131 119 165
                writes:      45  11  20  15  16
         */
    }

    // This function was used to build the initial test case files
    private static void buildInitialTestCases()
    {
        // For this, it doesn't matter which MMU we use.
        // If we are generating a small test case, set the numberOfTests in the
        // RandomJob call to 200.  For a large test case, use 2000.
        Tester t = new Tester(false, false, false);
        MMU x = new MMU_Fifo(t, 15, 1024, 8);
        Job j = new RandomJob(t, 2000, 32);
        j.run();
        t.printReport();
    }

    // This function was used to build the test case files for each of the
    // MMUs.
    private static void buildMMUTestCases()
    {
        // Set which MMU to use, and set which sample set to use
        // (either Sample1Job or Sample2Job)
        Tester t = new Tester(false, true, false);
        MMU x = new MMU_ApproximateLRU(t, 15, 1024, 8);
        Job j = new Sample2Job(t);
        j.run();
        t.printReport();
    }

    // This is the main routine for the students to use.  It asks which MMU to use,
    // and it asks which sample size to use.
    private static void runTestCases()
    {
        Scanner reader = new Scanner(System.in);

        System.out.println("MMU Simulator!");
        while (true)
        {
            System.out.println("0: quit");
            System.out.println("1: run a test case");
            System.out.println("2: final report");
            System.out.print("Yes? ");
            int op = reader.nextInt();
            if (op == 0)
            {
                break;
            }
            if (op < 0 || op > 2)
            {
                continue;
            }
            if (op == 2)
            {
                finalReport();
                continue;
            }

            // We are running a test case!
            System.out.println("0: Fifo");
            System.out.println("1: Second Chance");
            System.out.println("2: Not Recently Used");
            System.out.println("3: Least Recently Used");
            System.out.println("4: Approximate LRU");
            System.out.print("Algorithm? ");
            int alg = reader.nextInt();

            System.out.println("0: Small");
            System.out.println("1: Large");
            System.out.println("2: Custom");
            System.out.print("Test case? ");
            int job = reader.nextInt();

            if (job == 2)
            {
                System.out.print("Number of accesses? ");
                job = reader.nextInt();
            }

            Tester t = new Tester(job < 2, false, job >= 2);
            MMU x;
            Job j = null;
            if (job >= 2)
            {
                j = new RandomJob(t, job, 32);
            }
            switch (alg)
            {
            case 0:
                x = new MMU_Fifo(t, 15, 1024, 8);
                if (job < 2)
                {
                    j = (job == 1) ? new LargeFifoJob(t) : new SmallFifoJob(t);
                }
                break;
            case 1:
                x = new MMU_SecondChance(t, 15, 1024, 8);
                if (job < 2)
                {
                    j = (job == 1) ? new LargeSecondChanceJob(t) : new SmallSecondChanceJob(t);
                }
                break;
            case 2:
                x = new MMU_NotRecentlyUsed(t, 15, 1024, 8);
                if (job < 2)
                {
                    j = (job == 1) ? new LargeNotRecentlyUsedJob(t) : new SmallNotRecentlyUsedJob(t);
                }
                break;
            case 3:
                x = new MMU_LeastRecentlyUsed(t, 15, 1024, 8);
                if (job < 2)
                {
                    j = (job == 1) ? new LargeLeastRecentlyUsedJob(t) : new SmallLeastRecentlyUsedJob(t);
                }
                break;
            case 4:
                x = new MMU_ApproximateLRU(t, 15, 1024, 8);
                if (job < 2)
                {
                    j = (job == 1) ? new LargeApproximateLRUJob(t) : new SmallApproximateLRUJob(t);
                }
                break;
            default:
                continue;
            }
            j.run();
            System.out.println("Results for " + studentName() + " with algorithm " + x.algName());
            t.printReport();
        }
    }

    // This function is used to compute the final report
    private static void finalReport()
    {
        System.out.println("Results for " + studentName());
        for (int alg = 0 ; alg < 5; alg++)
        {
            int num = 0;
            int sum = 0;
            int squares = 0;
            int min = 99999;
            int max = -99999;
            for (int i = 0; i < 100; i++)
            {
                Tester t = new Tester(false, false, true);
                MMU x;
                Job j = new RandomJob(t, 1000, 32);
                switch (alg)
                {
                case 0:
                    x = new MMU_Fifo(t, 15, 1024, 8);
                    break;
                case 1:
                    x = new MMU_SecondChance(t, 15, 1024, 8);
                    break;
                case 2:
                    x = new MMU_NotRecentlyUsed(t, 15, 1024, 8);
                    break;
                case 3:
                    x = new MMU_LeastRecentlyUsed(t, 15, 1024, 8);
                    break;
                case 4:
                    x = new MMU_ApproximateLRU(t, 15, 1024, 8);
                    break;
                default:
                    continue;
                }
                j.run();
                int result = t.numFaults();
                num++;
                sum += result;
                squares += result * result;
                min = (min < result) ? min : result;
                max = (max > result) ? max : result;
            }
            double avg = (double)sum / (double)num;
            double stddev = Math.sqrt((double)(num * squares - sum * sum)) / (double)num;
            switch (alg)
            {
            case 0: System.out.print("Fifo:");  break;
            case 1: System.out.print("2nd: ");  break;
            case 2: System.out.print("NRU: ");  break;
            case 3: System.out.print("LRU: ");  break;
            case 4: System.out.print("ALRU:");  break;
            }
            System.out.print("  min = " + min);
            System.out.print("  max = " + max);
            System.out.print("  avg = " + avg);
            System.out.println("  sd = " + stddev);
        }
    }
}
