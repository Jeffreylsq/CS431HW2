package tianyuWei;

public class MMU_ApproximateLRU extends MMU
{
    // This is a parallel array to the inverse page table: we only need as
    // many entries as physical pages.  This stores the 'R' bits, a 6-bit field.
    public int[] RBits;

    
    private int _index;


    MMU_ApproximateLRU(Tester theTester, int addrBits, int pageSize, int numPages)
    {
        super(theTester, addrBits, pageSize, numPages);
        RBits = new int[numPhysicalPages];
    }

    public String algName() { return "Approximate LRU"; }

    public int accessMemory(int virtualAddress, AccessType type)
    {
    	// Get the index of the page, which are the upper bits of the virtual address
        int virtualIndex = virtualAddress >> pageBits;

        // The page index (which is the part that bypasses the mmu) are the
        // lower bits:
        int pageIndex = virtualAddress - (virtualIndex << pageBits);

        // If this page is NOT swapped in, we will need to swap it in
        if (presentBits[virtualIndex] == false)
        {
            tester.pageFault();

            // We will need to load the data into physical memory.
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

            // Now move the index ahead
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

    // This routine is called to clear the 'R' bits.  This actually shifts all the
    // RBits right by 1
    public void clearRBits()
    {
        for (int i = 0; i < numPhysicalPages; i++)
        {
            RBits[i] >>= 1;
        }
    }
}
