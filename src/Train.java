import java.util.*;
import java.util.concurrent.Semaphore;

import TSim.SensorEvent;
import TSim.TSimInterface;

public class Train extends Thread {
    private int trainid;
    private boolean goingDown;
    private TSimInterface tsi;
    private Semaphore[] semaphores;
    private int current_segment;
    private int target_segment;
    private int speed;
    private boolean stopped;
    private boolean reversed;
    private boolean crossing;
    private boolean targetUnacquired;

    public Train(int trainid, boolean goingDown, TSimInterface tsi, Semaphore[] semaphores, int current_segment,
            int target_segment, int speed) {
        this.speed = speed;
        this.trainid = trainid;
        this.goingDown = goingDown;
        this.tsi = tsi;
        this.semaphores = semaphores;
        this.current_segment = current_segment;
        this.target_segment = target_segment;
        stopped = false;
        reversed = false;
        crossing = false;
        targetUnacquired = true;

        try {
            semaphores[current_segment - 1].acquire();

            tsi.setSpeed(trainid, speed);
            updateSwitches();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void stopTrain() {
        if (stopped) {
            return;
        }

        stopped = true;

        try {
            tsi.setSpeed(trainid, 0);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void startTrain() {
        if (!stopped) {
            return;
        }

        stopped = false;

        try {
            tsi.setSpeed(trainid, speed * (reversed ? -1 : 1));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void updateSwitches() {
        HashMap<List<Integer>, Command> segToComd = Parsing.parseSegToCommand("SegmentTransitionToCommand.txt");

        List<Integer> segToSeg = current_segment < target_segment ? Arrays.asList(current_segment, target_segment)
                : Arrays.asList(target_segment, current_segment);

        Command comd = segToComd.get(segToSeg);

        boolean success = false;
        while (!success) {
            try {
                tsi.setSwitch(comd.x, comd.y,
                        comd.command.equals("left") ? TSimInterface.SWITCH_LEFT : TSimInterface.SWITCH_RIGHT);
                success = true;
            } catch (Exception e) {
                success = false;
                e.printStackTrace();
                System.err.println("Switch failed, trying again...");
            }
        }
    }

    private void setNextTarget(List<Integer> candidateSegments) {
        try {
            boolean foundFreeSegment = false;
            Integer nextSegmentId = -1;
            // As long as we haven't found a free segment, keep checking
            while (!foundFreeSegment) {
                for (Integer nextSegment : candidateSegments) {
                    if (semaphores[nextSegment - 1].tryAcquire()) {
                        foundFreeSegment = true;
                        nextSegmentId = nextSegment;
                        break;
                    }
                }
                if (!foundFreeSegment && !stopped) {
                    stopped = true;
                    tsi.setSpeed(trainid, 0);
                } else if (foundFreeSegment) {
                    stopped = false;
                    tsi.setSpeed(trainid, speed * (reversed ? -1 : 1));
                }
            }
            target_segment = nextSegmentId;
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void segmentSensorProcess(List<Integer> coords) {
        HashMap<List<Integer>, Integer> senToSeg = Parsing.parseSensorToSegMap("SensorToSegment.txt");
        HashMap<Integer, List<Integer>> segToSegs = Parsing.parseSegTransitions("SegmentTransitions.txt");

        // Get the current segment that the train is on
        int segId = senToSeg.get(coords);
        if (segId == current_segment && targetUnacquired) {
            targetUnacquired = false;
            // Get all the further segments
            List<Integer> segments = segToSegs.get(current_segment);

            // Filter out segments going in the wrong direction
            segments = segments.stream().filter(nextSegment -> (nextSegment > current_segment) ^ goingDown)
                    .toList();

            if (segments.size() > 0) {
                // We are supposed to go forward
                setNextTarget(segments);
                updateSwitches();
            }
        }

        // If the segment returned is not the target segment then we have not crossed
        // yet
        if (segId != target_segment) {
            return;
        }

        // We have reached the target segment
        semaphores[current_segment - 1].release();
        current_segment = target_segment;
        targetUnacquired = true;
        target_segment = -1;
    }

    private void stationSensorProcess(List<Integer> coords) {
        if ((goingDown && coords.get(1) < 10) || (!goingDown && coords.get(1) > 10)) {
            return;
        }

        try {
            stopTrain();
            Thread.sleep(1000 + (20 * Math.abs(speed)));
            goingDown = !goingDown;
            reversed = !reversed;
            startTrain();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void crossingSensorProcess() {
        crossing = !crossing;
        if (crossing) {
            while (!semaphores[8].tryAcquire()) {
                stopTrain();
            }
            startTrain();
        } else {
            semaphores[8].release();
        }
    }

    public void run() {

        HashMap<List<Integer>, String> sensorType = Parsing.parseSensorTypes("SensorToType.txt");

        try {
            while (true) {
                // Wait until the next event
                SensorEvent ev = tsi.getSensor(trainid);
                // Only continue on Active sensors
                if (ev.getStatus() == SensorEvent.INACTIVE) {
                    continue;
                }

                // Get the coordinates of the sensor
                List<Integer> coords = Arrays.asList(ev.getXpos(), ev.getYpos());

                String type = sensorType.get(coords);

                switch (type) {
                    case "Seg":
                        segmentSensorProcess(coords);
                        break;
                    case "Sta":
                        stationSensorProcess(coords);
                        break;
                    case "Cro":
                        crossingSensorProcess();
                        break;
                    default:
                        break;
                }

            }
        } catch (Exception e) {
            e.printStackTrace(); // or only e.getMessage() for the error
            System.exit(1);
        }
    }
}
