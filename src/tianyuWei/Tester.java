package tianyuWei;

public class Tester
{
    MMU mmu;

    // We can be running in 'evaluate' mode, where we are checking the student's
    // routine to the canned data coming from a test case, or we can be running
    // in 'generate' mode, where we are building the test cases.
    private boolean _evaluateMode;

    // When we are generating the examples, we may optionally write the
    // instructions indicating page faults, read and write accesses.
    private boolean _listActions;

    // If we are in silent mode, we don't print the list of basic instructions.
    // We do this when we are running custom jobs through the MMUs.
    private boolean _silentMode;

    // When running a test, the tester may set that it expects to find a pageFault
    // (for example).  If running the test did in fact have a pageFault, all is well.
    // However, if we were expecting a pageFault, but didn't get one, or if we were
    // not expecting a pageFault but we got one, then we print an error message.
    // Before running a performAccess, if we are expecting any of these actions, the
    // test should call expectPageFault (or one of the other 'expect' functions.  These
    // will set the following values.
    private boolean _expectPageFault, _gotPageFault;
    private boolean _expectFillPage, _gotFillPage;
    private boolean _expectFlushPage, _gotFlushPage;
    private int _expectFillDiskAddr, _gotFillDiskAddr;
    private int _expectFillPageFrame, _gotFillPageFrame;
    private int _expectFlushDiskAddr, _gotFlushDiskAddr;
    private int _expectFlushPageFrame, _gotFlushPageFrame;

    // The total counts for the various actions
    private int _numFaults, _numFills, _numFlushes;

    public Tester(boolean evaluateMode, boolean listActions, boolean silentMode)
    {
        mmu = null;
        _evaluateMode = evaluateMode;
        _listActions = listActions;
        _silentMode = silentMode;
        clearExpectations();
        _numFaults = 0;
        _numFills = 0;
        _numFlushes = 0;
    }

    public void setMMU(MMU theMMU)
    {
        mmu = theMMU;
    }

    // The tester calls this to clear all of the expectations,
    // preparing for the next test.
    private void clearExpectations()
    {
        _expectPageFault = _gotPageFault = false;
        _expectFillPage = _gotFillPage = false;
        _expectFlushPage = _gotFlushPage = false;
        _expectFillDiskAddr = _gotFillDiskAddr = 0;
        _expectFillPageFrame = _gotFillPageFrame = 0;
        _expectFlushDiskAddr = _gotFlushDiskAddr = 0;
        _expectFlushPageFrame = _gotFlushPageFrame = 0;
    }

    // The MMU should call this if there is a page fault
    public void pageFault()
    {
        _gotPageFault = true;
        _numFaults++;
        if (_listActions)
        {
            System.out.println("\ttester.expectPageFault();");
        }
    }

    // The MMU should call this to read a page from the disk
    // into a given page frame.
    public void fillPage(int diskAddr, int pageFrame)
    {
        _gotFillPage = true;
        _numFills++;
        _gotFillDiskAddr = diskAddr;
        _gotFillPageFrame = pageFrame;
        if (_listActions)
        {
            System.out.println("\ttester.expectFillPage(" + diskAddr + ", " + pageFrame + ");");
        }
    }

    // The MMU should call this to write a page from a page
    // frame to the disk.
    public void flushPage(int diskAddr, int pageFrame)
    {
        _gotFlushPage = true;
        _numFlushes++;
        _gotFlushDiskAddr = diskAddr;
        _gotFlushPageFrame = pageFrame;
        if (_listActions)
        {
            System.out.println("\ttester.expectFlushPage(" + diskAddr + ", " + pageFrame + ");");
        }
    }

    // This routine is called by the Job to indicate that
    // we expect a page fault on this next access
    public void expectPageFault()
    {
        _expectPageFault = true;
    }

    // This routine is called by the Job to indicate that
    // we expect a fillPage on this next access
    public void expectFillPage(int diskAddr, int pageFrame)
    {
        _expectFillPage = true;
        _expectFillDiskAddr = diskAddr;
        _expectFillPageFrame = pageFrame;
    }

    // This routine is called by the Job to indicate that
    // we expect a flushPage on this next access
    public void expectFlushPage(int diskAddr, int pageFrame)
    {
        _expectFlushPage = true;
        _expectFlushDiskAddr = diskAddr;
        _expectFlushPageFrame = pageFrame;
    }

    // This is used to construct part of error messages
    private String whereErr(int virtualAddr, AccessType mode)
    {
        String msg = "Error when ";
        switch (mode)
        {
        case READ:  msg += "READING";  break;
        case WRITE: msg += "WRITING";  break;
        case EXECUTE:   msg += "EXECUTING";    break;
        }
        msg += " virtual address " + virtualAddr + ": ";
        return msg;
    }

    // Converts the access mode into a string:
    private String access(AccessType mode)
    {
        switch (mode)
        {
        default:
        case READ:  return("AccessType.READ");
        case WRITE: return("AccessType.WRITE");
        case EXECUTE: return("AccessType.EXECUTE");
        }
    }

    // This routine is called by the Job: it causes the MMU to
    // clear its R bits.
    public void clearRBits()
    {
        mmu.clearRBits();
        if (_evaluateMode == false)
        {
            if (_silentMode == false)
            {
                System.out.println("\ttester.clearRBits();");
            }
            return;
        }
    }

    // This routine is called by the Job: perform a disk
    // access.
    public void performAccess(int virtualAddr, AccessType mode, int physicalAddr)
    {
        int result = mmu.accessMemory(virtualAddr, mode);
        if (_evaluateMode == false)
        {
            if (_silentMode == false)
            {
                System.out.println("\ttester.performAccess(" + virtualAddr + ", " + access(mode) + ", " + result + ");");
            }
            return;
        }
        if (result != physicalAddr)
        {
            System.out.println(whereErr(virtualAddr, mode) + " Expected physical address "
                    + physicalAddr + " but got " + result);
        }
        if (_expectPageFault != _gotPageFault)
        {
            if (_expectPageFault)
            {
                System.out.println(whereErr(virtualAddr, mode) + " Expected pageFault, but didn't get one");
            }
            else
            {
                System.out.println(whereErr(virtualAddr, mode) + " Did not expect pageFault, but got one");
            }
        }
        if (_expectFlushPage != _gotFlushPage)
        {
            if (_expectFlushPage)
            {
                System.out.println(whereErr(virtualAddr, mode) + " Expected flushPage, but didn't get one");
            }
            else
            {
                System.out.println(whereErr(virtualAddr, mode) + " Did not expect flushPage, but got one");
            }
        }
        else if (_expectFlushPage)
        {
            if (_expectFlushDiskAddr != _gotFlushDiskAddr)
            {
                System.out.println(whereErr(virtualAddr, mode) + " Expected flush disk addr "
                        + _expectFlushDiskAddr + " but got " + _gotFlushDiskAddr);
            }
            if (_expectFlushPageFrame != _gotFlushPageFrame)
            {
                System.out.println(whereErr(virtualAddr, mode) + " Expected flush page frame "
                        + _expectFlushPageFrame + " but got " + _gotFlushPageFrame);
            }
        }
        if (_expectFillPage != _gotFillPage)
        {
            if (_expectFillPage)
            {
                System.out.println(whereErr(virtualAddr, mode) + " Expected fillPage, but didn't get one");
            }
            else
            {
                System.out.println(whereErr(virtualAddr, mode) + " Did not expect fillPage, but got one");
            }
        }
        else if (_expectFillPage)
        {
            if (_expectFillDiskAddr != _gotFillDiskAddr)
            {
                System.out.println(whereErr(virtualAddr, mode) + " Expected fill disk addr "
                        + _expectFillDiskAddr + " but got " + _gotFillDiskAddr);
            }
            if (_expectFillPageFrame != _gotFillPageFrame)
            {
                System.out.println(whereErr(virtualAddr, mode) + " Expected fill page frame "
                        + _expectFillPageFrame + " but got " + _gotFillPageFrame);
            }
        }

        clearExpectations();
    }

    // Write the status report at the end of the run
    public void printReport()
    {
        System.out.println("Final report:");
        System.out.println("Total number of page faults: " + _numFaults);
        System.out.println("Total number of page reads: " + _numFills);
        System.out.println("Total number of page writes: " + _numFlushes);
    }

    public int numFaults() { return _numFaults; }
}
