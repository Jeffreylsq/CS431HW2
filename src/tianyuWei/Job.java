package tianyuWei;

//A 'Job' provides a stream of memory access requests.  This class is
//subclassed.  One of the subclasses creates random test cases, while
//other subclasses give specific test cases along with expected
//faults, reads, and writes.
public class Job
{
 public Tester tester;

 public Job(Tester theTester)
 {
     tester = theTester;
 }

 public void run()
 {
     // This function is overwritten by the subclass.
 }
}
