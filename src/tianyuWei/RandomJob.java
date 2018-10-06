package tianyuWei;

import java.util.Random;

public class RandomJob extends Job
{
    private int count;
    private int numVirtualPages;
    private int[] activePages;

    public RandomJob(Tester theTester, int numberOfTests, int numberOfVirtualPages)
    {
        super(theTester);
        count = numberOfTests;
        activePages = new int[6];
        numVirtualPages = numberOfVirtualPages;
    }

    public void run()
    {
        Random rand = new Random();

        // We will randomly generate accesses in 6 different pages.  The first three
        // will be 'popular' pages, so they will 'always' be in the system.  They are
        // the first three virtual pages.  The last three are random pages in the
        // rest of the memory, and will change every once in a while.  Also, we will
        // sometimes do writes to pages 2 and 3 in our list of 6 pages (one of the
        // popular pages and one of the random pages).
        for (int i = 0; i < 6; i++)
        {
            activePages[i] = i;
        }

        // We increment phase for each access.  When phase gets to 20, we switch to
        // new active pages.  When phase gets to 40, we clear the R bits, and reset phase.
        int phase = 0;

        // Generate the number of accesses
        for (int i = 0 ; i < count; i++)
        {
            // Handle the phase
            phase++;
            if (phase == 20)
            {
                activePages[3] = rand.nextInt(2) + 3;
                activePages[4] = rand.nextInt(numVirtualPages - 5) + 5;
                while (true)
                {
                    activePages[5] = rand.nextInt(numVirtualPages - 5) + 5;
                    if (activePages[4] != activePages[5])
                    {
                        break;
                    }
                }
            }
            if (phase == 40)
            {
                phase = 0;
                tester.clearRBits();
            }

            // Output one access
            if (rand.nextInt(100) > 10)
            {
                int page = activePages[rand.nextInt(6)];
                tester.performAccess(page * 1024 + rand.nextInt(1024), AccessType.READ, 0);
            }
            else
            {
                int page = activePages[2 + rand.nextInt(2)];
                tester.performAccess(page * 1024 + rand.nextInt(1024), AccessType.WRITE, 0);
            }
        }
    }
}
