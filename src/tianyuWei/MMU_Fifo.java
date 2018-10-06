package tianyuWei;

public class MMU_Fifo extends MMU
{

    // This index is the 'FIFO' pointer: it states which slot in the page table
    // we will be filling next.
    private int _index;

    MMU_Fifo(Tester theTester, int addrBits, int pageSize, int numPages)
    {
        super(theTester, addrBits, pageSize, numPages);
        _index = 0;
    }

    public String algName() { return "FIFO"; }

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

    // Note: this MMU does not have R bits, so we don't need the clearRBits() function
}
