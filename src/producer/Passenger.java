package producer;

public class Passenger implements Person {
    private final Integer id;
    private final Integer fromFloor;
    private final Integer toFloor;
    private final Integer weight;
    private final boolean direction;

    public Passenger(Integer id, String fromStr, String toStr, Integer weight) {
        this.id = id;
        this.weight = weight;
        this.fromFloor = parseFloor(fromStr);
        this.toFloor = parseFloor(toStr);
        this.direction = (fromFloor < toFloor);
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
    public Integer getFromFloor() {
        return fromFloor;
    }

    @Override
    public Integer getToFloor() {
        return toFloor;
    }

    @Override
    public Integer getWeight() {
        return weight;
    }

    @Override
    public boolean isDirection() {
        return direction;
    }
}
