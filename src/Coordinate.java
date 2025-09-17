import java.util.Objects;

public class Coordinate {
    Integer x;
    Integer y;
    public Coordinate(Integer x, Integer y){
        this.x = x;
        this.y = y;
    }

    @Override
    public int hashCode() {
        return Objects.hash(x, y);
    }

     @Override
    public String toString() {
        return "(" + x + ", " + y + ")";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true; // same object
        if (o == null || getClass() != o.getClass()) return false; // type check
        Coordinate that = (Coordinate) o;
        return Objects.equals(x, that.x) && Objects.equals(y, that.y);
    }
}
