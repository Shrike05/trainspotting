import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class Parsing {
    public static HashMap<Coordinate, Integer> parseSensorToSegMap(String filePath) {
        HashMap<Coordinate, Integer> map = new HashMap<>();

        try (BufferedReader br = new BufferedReader(new FileReader(filePath))) {
            String line;

            while ((line = br.readLine()) != null) {
                line = line.trim();

                String[] parts = line.split(":");
                if (parts.length != 2)
                    continue;

                // Left side -> "(x, y)"
                String keyPart = parts[0].trim();
                keyPart = keyPart.replace("(", "").replace(")", "");
                String[] numbers = keyPart.split(",");

                List<Integer> keys = new ArrayList<>();
                for (String num : numbers) {
                    keys.add(Integer.parseInt(num.trim()));
                }
                Coordinate key = new Coordinate(keys.get(0), keys.get(1));

                // Right side -> "x"
                int value = Integer.parseInt(parts[1].trim());

                map.put(key, value);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return map;
    }

    public static HashMap<Integer, List<Integer>> parseSegTransitions(String filePath) {
        HashMap<Integer, List<Integer>> map = new HashMap<>();

        try (BufferedReader br = new BufferedReader(new FileReader(filePath))) {
            String line;
            while ((line = br.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty())
                    continue;

                String[] parts = line.split(":");
                int key = Integer.parseInt(parts[0].trim());

                List<Integer> values = new ArrayList<>();
                for (String num : parts[1].trim().split("\\s+")) {
                    values.add(Integer.parseInt(num));
                }

                map.put(key, values);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return map;
    }

    public static HashMap<List<Integer>, Command> parseSegToCommand(String filePath) {
        HashMap<List<Integer>, Command> map = new HashMap<>();

        try (BufferedReader br = new BufferedReader(new FileReader(filePath))) {
            String line;
            while ((line = br.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty())
                    continue;

                // Split into "(a, b)" and "x y cmd"
                String[] parts = line.split(":");
                String keyPart = parts[0].trim().replace("(", "").replace(")", "");
                String[] keyNums = keyPart.split(",");

                List<Integer> key = new ArrayList<>();
                for (String num : keyNums)
                    key.add(Integer.parseInt(num.trim()));

                String[] valueParts = parts[1].trim().split("\\s+");
                Command cmd = new Command();
                cmd.x = Integer.parseInt(valueParts[0]);
                cmd.y = Integer.parseInt(valueParts[1]);

                switch (valueParts[2].trim().toLowerCase()) {
                    case "left":
                        cmd.command = CommandType.Left;
                        break;
                    case "right":
                        cmd.command = CommandType.Right;
                        break;
                    default:
                        cmd.command = CommandType.Right;
                        break;
                }

                map.put(key, cmd);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return map;
    }

    public static HashMap<Coordinate, SensorType> parseSensorTypes(String filePath) {
        HashMap<Coordinate, SensorType> map = new HashMap<>();

        try (BufferedReader br = new BufferedReader(new FileReader(filePath))) {
            String line;
            while ((line = br.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty())
                    continue;

                String[] parts = line.split(":");
                String keyPart = parts[0].trim().replace("(", "").replace(")", "");
                String[] keyNums = keyPart.split(",");

                List<Integer> keys = new ArrayList<>();
                for (String num : keyNums)
                    keys.add(Integer.parseInt(num.trim()));

                Coordinate key = new Coordinate(keys.get(0), keys.get(1));

                SensorType value = SensorType.Segment;
                switch (parts[1].trim()) {
                    case "Seg":
                        value = SensorType.Segment;
                        break;
                    case "Sta":
                        value = SensorType.Station;
                        break;
                    case "Cro":
                        value = SensorType.Crossing;
                        break;
                    default:
                        break;
                }

                map.put(key, value);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return map;
    }
}
