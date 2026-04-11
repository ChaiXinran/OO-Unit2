package producer;

public class Worker implements Person {
    private final Integer id;
    private final Integer elevatorId;
    private final Integer toFloor;
    private final boolean direction;

    public Worker(Integer id, String toStr, Integer elevatorId) {
        this.id = id;
        this.elevatorId = elevatorId;
        this.toFloor = parseFloor(toStr);
        this.direction = (toFloor > 5);
    }

    private int parseFloor(String s) {
        int num = Integer.parseInt(s.substring(1));
        if (s.charAt(0) == 'B') {
            return 5 - num;
        } else { // F
            return num + 4;
        }
    }

    @Override
    public Integer getId() {
        return id;
    }

    @Override
    public Integer getToFloor() {
        return toFloor;
    }

    @Override
    public boolean isDirection() {
        return direction;
    }

    public Integer getElevatorId() {
        return elevatorId;
    }

    @Override
    public Integer getFromFloor() {
        return 5;
    }

    @Override
    public Integer getWeight() {
        return null;
    }
}
