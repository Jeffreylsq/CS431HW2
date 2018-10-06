package tianyuWei;

public class MMU_LeastRecentlyUsed extends MMU
{
    // This is a parallel array to the inverse page table: we only need as
    // many entries as physical pages.  This stores the 'count' at the last
    // time this page was referenced.
    public int[] lastRefCount;
    private int counter;
    private int _index;

    // This is the 'timer', it gets incremented on each reference.
    

    MMU_LeastRecentlyUsed(Tester theTester, int addrBits, int pageSize, int numPages)
    {
        super(theTester, addrBits, pageSize, numPages);
        counter = 1;
        lastRefCount = new int[numPhysicalPages];
    }

    public String algName() { return "Least Recently Used"; }

    public int accessMemory(int virtualAddress, AccessType type)
    {
    	// Get the index of the page, which are the upper bits of the virtual address
        int virtualIndex = virtualAddress >> pageBits;

        // The page index (which is the part that bypasses the mmu) are the
        // lower bits:
        int pageIndex = virtualAddress - (virtualIndex << pageBits);

        // If this page is NOT swapped in, we will need to swap it in
        if (presentBits[virtualIndex] == false) {
            tester.pageFault();

            // We will need to load the data into physical memory.  
            // Before we load data into this location, check to make sure
            // that any old data that is dirty is written out.
            if (inversePresent[_index]) {
                int oldVirtual = inverseTable[_index];
                if (dirtyBits[oldVirtual]) {
                    // The old page was dirty, so we have to write it out.
                    tester.flushPage(oldVirtual, _index);
                    dirtyBits[oldVirtual] = false;
                }
                pageTable[oldVirtual] = 0;
                presentBits[oldVirtual] = false;
            }

            int min_value = lastRefCount[0];

            for (int i = 0; i < numPhysicalPages; i++) {
                if (min_value > lastRefCount[i]) {
                    min_value = lastRefCount[i];
                    _index = i;
                }
            }

            // Read the new data in
            tester.fillPage(virtualIndex, _index);

            // Link the tables
            pageTable[virtualIndex] = _index;
            presentBits[virtualIndex] = true;
            dirtyBits[virtualIndex] = false;
            inverseTable[_index] = virtualIndex;
            inversePresent[_index] = true;

            // Now move the lru ahead
            lastRefCount[_index] = counter;
            counter++;

        }

        // At this point, the page should be swapped in.  We now return the
        // physical address.  However, if we are doing a WRITE operation,
        // we must first set the dirty bit.
        if (type == AccessType.WRITE) {
            dirtyBits[virtualIndex] = true;
        }
        return (pageTable[virtualIndex] << pageBits) + pageIndex;
    }
}
