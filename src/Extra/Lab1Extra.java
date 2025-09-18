package Extra;

import TSim.*;

public class Lab1Extra {

  public Lab1Extra(int speed1, int speed2)  throws InterruptedException {
    TSimInterface tsi = TSimInterface.getInstance();
    SegmentMonitor[] segments = initializeMonitors(9);

    TrainExtra train1 = new TrainExtra(1, true, tsi, segments, 8, 6, speed1);
    TrainExtra train2 = new TrainExtra(2, false, tsi, segments, 2, 3, speed2);

    train1.start();
    train2.start();
  }

  private static SegmentMonitor[] initializeMonitors(int size){
    SegmentMonitor[] segments = new SegmentMonitor[size];

    for(int i = 0; i < segments.length; i++){
      segments[i] = new SegmentMonitor();
    }

    return segments;
  }
}

