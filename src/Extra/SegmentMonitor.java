package Extra;

import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

public class SegmentMonitor {
    private boolean occupied = false;
    private ReentrantLock lock = new ReentrantLock();
    private Condition occupiedCondition = lock.newCondition();

    public void enter(){
        lock.lock();
        try {
            while (occupied) {
                occupiedCondition.await();
            }

            occupied = true;
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            lock.unlock();
        }
    }

    public void leave(){
        lock.lock();

        try {
            occupied = false;
    
            occupiedCondition.signal();
        } finally {
            lock.unlock();
        }
    }

    public boolean getOccupied(){
        lock.lock();
        try {
            return occupied;
        } finally {
            lock.unlock();
        }
    }
}
