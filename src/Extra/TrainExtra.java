package Extra;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import TSim.SensorEvent;
import TSim.TSimInterface;
import Util.Command;
import Util.CommandType;
import Util.Coordinate;
import Util.Parsing;
import Util.SensorType;

public class TrainExtra extends Thread {
    private int trainid;
    private boolean goingDown;
    private TSimInterface tsi;
    private SegmentMonitor[] monitors;
    private int currentSegment;
    private int targetSegment;
    private int speed;
    private boolean stopped;
    private boolean reversed;
    private boolean crossing;
    private boolean targetUnacquired;

    private HashMap<Coordinate, Integer> senToSeg;
    private HashMap<Integer, List<Integer>> segToSegs;
    private HashMap<List<Integer>, Command> segToComd;
    private HashMap<Coordinate, SensorType> sensorType;

    public TrainExtra(int trainid, boolean goingDown, TSimInterface tsi, SegmentMonitor[] monitors, int currentSegment,
            int target_segment, int speed) {
        this.speed = speed;
        this.trainid = trainid;
        this.goingDown = goingDown;
        this.tsi = tsi;
        this.monitors = monitors;
        this.currentSegment = currentSegment;
        this.targetSegment = target_segment;
        stopped = true;
        reversed = false;
        crossing = false;
        targetUnacquired = true;

        senToSeg = Parsing.parseSensorToSegMap("SensorToSegment.txt");
        segToSegs = Parsing.parseSegTransitions("SegmentTransitions.txt");
        segToComd = Parsing.parseSegToCommand("SegmentTransitionToCommand.txt");
        sensorType = Parsing.parseSensorTypes("SensorToType.txt");
        
        startTrain();

        try {
            monitors[currentSegment - 1].enter();
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
        List<Integer> segToSeg = currentSegment < targetSegment ? Arrays.asList(currentSegment, targetSegment)
                : Arrays.asList(targetSegment, currentSegment);

        Command comd = segToComd.get(segToSeg);

        boolean success = false;
        while (!success) {
            try {
                Integer switchState = comd.command.equals(CommandType.Left) ? TSimInterface.SWITCH_LEFT : TSimInterface.SWITCH_RIGHT;
                tsi.setSwitch(comd.x, comd.y, switchState);
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
                    if (!monitors[nextSegment - 1].getOccupied()) {
                        monitors[nextSegment - 1].enter();
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
            targetSegment = nextSegmentId;
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void segmentSensorProcess(Coordinate coords) {
        // Get the current segment that the train is on
        int segId = senToSeg.get(coords);
        if (segId == currentSegment && targetUnacquired) {
            targetUnacquired = false;
            // Get all the further segments
            List<Integer> segments = segToSegs.get(currentSegment);

            // Filter out segments going in the wrong direction
            segments = segments.stream().filter(nextSegment -> (nextSegment > currentSegment) ^ goingDown)
                    .toList();

            if (segments.size() > 0) {
                // We are supposed to go forward
                setNextTarget(segments);
                updateSwitches();
            }
        }

        // If the segment returned is not the target segment then we have not crossed
        // yet
        if (segId != targetSegment) {
            return;
        }

        // We have reached the target segment
        monitors[currentSegment - 1].leave();
        currentSegment = targetSegment;
        targetUnacquired = true;
        targetSegment = -1;
    }

    private void stationSensorProcess(Coordinate coords) {
        if ((goingDown && coords.y < 10) || (!goingDown && coords.y > 10)) {
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
            while (monitors[8].getOccupied()) {
                stopTrain();
            }
            monitors[8].enter();
            startTrain();
        } else {
            monitors[8].leave();
        }
    }

    public void run() {
        try {
            while (true) {
                // Wait until the next event
                SensorEvent ev = tsi.getSensor(trainid);
                // Only continue on Active sensors
                if (ev.getStatus() == SensorEvent.INACTIVE) {
                    continue;
                }

                // Get the coordinates of the sensor
                Coordinate coords = new Coordinate(ev.getXpos(), ev.getYpos());

                SensorType type = sensorType.get(coords);

                switch (type) {
                    case SensorType.Segment:
                        segmentSensorProcess(coords);
                        break;
                    case SensorType.Station:
                        stationSensorProcess(coords);
                        break;
                    case SensorType.Crossing:
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
