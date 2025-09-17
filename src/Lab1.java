import java.util.concurrent.Semaphore;

import TSim.*;

public class Lab1 {

  public Lab1(int speed1, int speed2)  throws InterruptedException {
    TSimInterface tsi = TSimInterface.getInstance();
    Semaphore[] segments = initializeBinarySemaphores(9);

    Train train1 = new Train(1, true, tsi, segments, 8, 6, speed1);
    Train train2 = new Train(2, false, tsi, segments, 2, 3, speed2);

    train1.start();
    train2.start();
  }

  private static Semaphore[] initializeBinarySemaphores(int size){
    Semaphore[] segments = new Semaphore[size];

    for(int i = 0; i < segments.length; i++){
      segments[i] = new Semaphore(1);
    }

    return segments;
  }
}

