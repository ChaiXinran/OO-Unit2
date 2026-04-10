package producer;

public class Person {
    private final Integer id;
    private final Integer fromFloor;
    private final Integer toFloor;
    private final Integer weight;
    private final boolean direction;
    private final Integer elevatorId;

    public Person(Integer id, String fromStr, String toStr, Integer weight,Integer elevatorId) {
        this.id = id;
        this.weight = weight;
        this.fromFloor = parseFloor(fromStr);
        this.toFloor = parseFloor(toStr);
        this.direction = (fromFloor < toFloor);
        this.elevatorId = elevatorId;
    }

    private int parseFloor(String s) {
        int num = Integer.parseInt(s.substring(1));
        if (s.charAt(0) == 'B') {
            return 5 - num;
        } else { // F
            return num + 4;
        }
    }

    public Integer getId() {
        return id;
    }

    public Integer getFromFloor() {
        return fromFloor;
    }

    public Integer getToFloor() {
        return toFloor;
    }

    public Integer getWeight() {
        return weight;
    }

    public boolean isDirection() {
        return direction;
    }

    public Integer getElevatorId() {
        return elevatorId;
    }
}
