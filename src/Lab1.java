import java.util.concurrent.Semaphore;

import TSim.*;

public class Lab1 {

  public Lab1(int speed1, int speed2)  throws InterruptedException {
    TSimInterface tsi = TSimInterface.getInstance();
    Semaphore[] segments = initialize_semaphores();

    Train train1 = new Train(1, true, tsi, segments, 8, 6, speed1);
    Train train2 = new Train(2, false, tsi, segments, 2, 3, speed2);

    try {
      //tsi.setSpeed(1,speed1);
      tsi.setSpeed(1,speed1);
      tsi.setSpeed(2,speed2);

      train1.start();
      train2.start();
    }
    catch (CommandException e) {
      e.printStackTrace();    // or only e.getMessage() for the error
      System.exit(1);
    }
  }

  private static Semaphore[] initialize_semaphores(){
    Semaphore[] segments = new Semaphore[9];

    for(int i = 0; i < segments.length; i++){
      segments[i] = new Semaphore(1);
    }

    return segments;
  }
}

