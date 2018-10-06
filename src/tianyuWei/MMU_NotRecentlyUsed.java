package tianyuWei;

public class MMU_NotRecentlyUsed extends MMU
{
    // This is a parallel array to the page table.  An entry is 'true' if we
    // have referenced the page.  These bits will be cleared when the clearRBits
    // function is called.
    public boolean[] recentBits;

    // This index is the 'FIFO' pointer: it states which slot in the page table
    // we will be filling next.
    private int _index;

    MMU_NotRecentlyUsed(Tester theTester, int addrBits, int pageSize, int numPages)
    {
        super(theTester, addrBits, pageSize, numPages);
        _index = 0;
        recentBits = new boolean[numVirtualPages];
    }

    public String algName() { return "Not Recently Used"; }

    // Look through all the physical pages, looking for an entry that does not have
    // either the R or M bits set -- this is the best choice to replace.  We start
    // at the given index, so this works like a fifo.  Returns -1 if none are found.
    private int best_NR_NM(int start)
    {
        for (int i = 1; i <= numPhysicalPages; i++)
        {
            int idx = (start + i) % numPhysicalPages;

            int virtualIndex = inverseTable[idx];
            if (recentBits[virtualIndex] == false && dirtyBits[virtualIndex] == false)
            {
                return(idx);
            }
        }
        return -1;
    }

    // Look for a slot with no R but M:
    private int best_NR_PM(int start)
    {
        for (int i = 1; i <= numPhysicalPages; i++)
        {
            int idx = (start + i) % numPhysicalPages;

            int virtualIndex = inverseTable[idx];
            if (recentBits[virtualIndex] == false && dirtyBits[virtualIndex] == true)
            {
                return(idx);
            }
        }
        return -1;
    }

    // Look for a slot with R but no M:
    private int best_PR_NM(int start)
    {
        for (int i = 1; i <= numPhysicalPages; i++)
        {
            int idx = (start + i) % numPhysicalPages;

            int virtualIndex = inverseTable[idx];
            if (recentBits[virtualIndex] == true && dirtyBits[virtualIndex] == false)
            {
                return(idx);
            }
        }
        return -1;
    }

    // Look for a slot with R and M:
    private int best_PR_PM(int start)
    {
        for (int i = 1; i <= numPhysicalPages; i++)
        {
            int idx = (start + i) % numPhysicalPages;

            int virtualIndex = inverseTable[idx];
            if (recentBits[virtualIndex] == true && dirtyBits[virtualIndex] == true)
            {
                return(idx);
            }
        }
        return -1;
    }

    public int accessMemory(int virtualAddress, AccessType type)
    {
    	// Get the index of the page, which are the upper bits of the virtual address
        int virtualIndex = virtualAddress >> pageBits;
        int perfect_index = best_NR_NM(virtualIndex);
        
        if(perfect_index == -1)
        {
            perfect_index = best_NR_PM(virtualIndex);
        }
        
        if(perfect_index == -1)
        {
            perfect_index = best_PR_NM(virtualIndex);
        }
        
        if(perfect_index == -1)
        {
            perfect_index = best_PR_PM(virtualIndex);
        }
        
        if(perfect_index == -1)
        {
            perfect_index = (virtualIndex);
        }
        
        
        

        // The page index (which is the part that bypasses the mmu) are the
        // lower bits:
        int pageIndex = virtualAddress - (virtualIndex << pageBits);

        // If this page is NOT swapped in, we will need to swap it in
        if (presentBits[virtualIndex] == false)
        {
            tester.pageFault();

            // We will need to load the data into physical memory.  Since
            // this is the FIFO algorithm, we will simply overwrite the
            // page at the current index.

            // Before we load data into this location, check to make sure
            // that any old data that is dirty is written out.
            if (inversePresent[_index])
            {
                int oldVirtual = inverseTable[_index];
                if (dirtyBits[oldVirtual])
                {
                    // The old page was dirty, so we have to write it out.
                    tester.flushPage(oldVirtual, _index);
                    dirtyBits[oldVirtual] = false;
                }
                pageTable[oldVirtual] = 0;
                presentBits[oldVirtual] = false;
            }

            // Read the new data in
            tester.fillPage(virtualIndex, _index);

            // Link the tables
            pageTable[virtualIndex] = _index;
            presentBits[virtualIndex] = true;
            dirtyBits[virtualIndex] = false;
            inverseTable[_index] = virtualIndex;
            inversePresent[_index] = true;

            // Now move the fifo ahead
            _index++;
            if (_index >= numPhysicalPages)
            {
                _index = 0;
            }
        }
     // At this point, the page should be swapped in.  We now return the
        // physical address.  However, if we are doing a WRITE operation,
        // we must first set the dirty bit.
        if (type == AccessType.WRITE)
        {
            dirtyBits[virtualIndex] = true;
        }
        return (pageTable[virtualIndex] << pageBits) + pageIndex;
    }

    // This routine is called to clear the 'R' bits
    public void clearRBits()
    {
        for (int i = 0; i < numVirtualPages; i++)
        {
            recentBits[i] = false;
        }
    }
}
