package tianyuWei;

public class MMU
{
    public Tester tester;

    // The number of bits in a virtual address
    public int virtualBits;
    
    // The number of bits that 'bypass' the MMU.  These are the bits that index into
    // a page
    public int pageBits;

    // The number of bits that are used to index into the page table
    public int mmuInBits;

    // The number of virtual pages, the length of the page table
    public int numVirtualPages;

    // The number of bits used to give the physical page frame index
    public int mmuOutBits;

    // The number of physical pages
    public int numPhysicalPages;

    // This is the page table.  It is indexed by the upper bits of the virtual address,
    // and contains the page frame number in physical memory for that page (if the
    // page is swapped in).
    public int[] pageTable;

    // This is a parallel array to the page table.  An entry is 'true' if that virtual
    // page is actually loaded into physical memory (indicating that the pageTable value
    // is correct)
    public boolean[] presentBits;

    // This is another parallel array to the page table.  An entry is 'true' if we
    // have done a write to the page.  If we are ever going to swap out the page,
    // if this dirty bit is set, we have to write the data out before reusing that
    // page frame.
    public boolean[] dirtyBits;

    // This is the inverted page table.  The size of this array is the number of
    // page frames in physical memory.  The value in this table gives which virtual
    // page is loaded into the frame.  So if _pageTable[A] = B, then _inverseTable[B] = A.
    // This way, when we go to swap out a dirty page, we know where to write it on
    // the disk.
    public int[] inverseTable;

    // This is a parallel array to the inverseTable.  An entry is 'true' if we have
    // something loaded in this page frame.  Initially all of these bits are 'false',
    // but really quickly they will get filled, then stay filled.
    public boolean[] inversePresent;

    public MMU(Tester theTester, int addrBits, int pageSize, int numPages)
    {
        tester = theTester;
        tester.setMMU(this);

        virtualBits = addrBits;
        pageBits = bitsToEncode(pageSize);
        mmuInBits = virtualBits - pageBits;
        mmuOutBits = bitsToEncode(numPages);
        numVirtualPages = 1 << mmuInBits;
        numPhysicalPages = numPages;

        // Allocate the arrays
        pageTable = new int[numVirtualPages];
        presentBits = new boolean[numVirtualPages];
        dirtyBits = new boolean[numVirtualPages];
        inverseTable = new int[numPhysicalPages];
        inversePresent = new boolean[numPhysicalPages];
    }

    // How many bits does it take to encode (index) the given size.  For
    // example, if size is 4096, it would take 12 bits.
    private int bitsToEncode(int size)
    {
        int numBits = 16;
        for (int nextStep = 8; nextStep > 0; nextStep >>= 1)
        {
            int guess = 1 << numBits;
            if (size == guess)
            {
                return(numBits);
            }
            if (size > guess)
            {
                numBits += nextStep;
            }
            else
            {
                numBits -= nextStep;
            }
        }
        return(numBits);
    }

    public String algName() { return "Name of the MMU Algorithm"; }

    // Function to look up an address.  We pass in the virtual address,
    // whether we are doing a Read, Write, or Execute.  We get back the
    // physical address.  This may cause calls to the page fault routine,
    // and to routines to read or write pages of memory.
    public int accessMemory(int virtualAddress, AccessType type)
    {
        // This is overwritten by the subclass
        return 0;
    }

    // This routine is called to clear the 'R' bits (for MMUs that use it)
    public void clearRBits()
    {
        // This is overwritten by the subclass
    }
}

/*
Fifo: pages are placed in queue when loaded, first in queue is flushed on fault.

Not Recently Used: uses the R and M bits (we need to clear 'R' every once in a while).
Like 4 FIFOs, one for -R-M, -R+M, +R-M, +R+M, in that order

Second chance: Like FIFO, but if the first entry has R set, clear its R and move it to the
end of the queue.  Clock is just like this.

Least recently used: have a clock.  Whenever a page is referenced, increment the clock, then
save this value with the page.  Pick the one to swap out by finding the smallest value of
last reference.

Approximated LRU: have 6-bit R field.  Each clock tick we shift them right by 1.
 */