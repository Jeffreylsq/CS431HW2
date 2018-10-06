package tianyuWei;

public class MMU_SecondChance extends MMU
{
    // This is a parallel array to the page table.  An entry is 'true' if we
    // have referenced the page.  These bits will be cleared when the clearRBits
    // function is called.
    public boolean[] recentBits;
    boolean result;

    // This index is the 'FIFO' pointer: it states which slot in the page table
    // we will be filling next.
    private int _index;

    MMU_SecondChance(Tester theTester, int addrBits, int pageSize, int numPages)
    {
        super(theTester, addrBits, pageSize, numPages);
        _index = 0;
        recentBits = new boolean[numVirtualPages];
    }

    public String algName() { return "Second Chance"; }

    public int accessMemory(int virtualAddress, AccessType type)
    {
		
    	// Get the index of the page, which are the upper bits of the virtual address
        int virtualIndex = virtualAddress >> pageBits;

        // The page index (which is the part that bypasses the mmu) are the
        // lower bits:
        int pageIndex = virtualAddress - (virtualIndex << pageBits);
        
        int flag = 0;
        for(int i =0; i< numPhysicalPages; i++)
        {
            if (inverseTable[i] == virtualIndex) {
            recentBits[i] = true;
            flag = 1;
            break;
            }
        }

        
        // If this page is NOT swapped in, we will need to swap it in
        if (presentBits[virtualIndex] == false) {
            tester.pageFault();

            // We will need to load the data into physical memory.  Since
            // this is the FIFO algorithm, we will simply overwrite the
            // page at the current index.
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

            // Read the new data in
            tester.fillPage(virtualIndex, _index);

            // Link the tables
            pageTable[virtualIndex] = _index;
            presentBits[virtualIndex] = true;
            dirtyBits[virtualIndex] = false;
            inverseTable[_index] = virtualIndex;
            inversePresent[_index] = true;

            // Now move index
            _index++;
            if (_index >= numPhysicalPages) {
                _index = 0;
            }

//            while (recentBits[_index]) {
//                recentBits[_index] = false;
//                _index++;
//            }

        }

        // At this point, the page should be swapped in.  We now return the
        // physical address.  However, if we are doing a WRITE operation,
        // we must first set the dirty bit.
        if (type == AccessType.WRITE) {
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
