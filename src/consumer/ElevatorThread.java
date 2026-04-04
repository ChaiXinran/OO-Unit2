package consumer;

import buffer.AdviceType;
import buffer.RequestTable;
import buffer.Strategy;

import com.oocourse.elevator1.PersonRequest;
import com.oocourse.elevator1.TimableOutput;

import java.util.HashMap;
import java.util.HashSet;

public class ElevatorThread extends Thread {
    private int id;
    private int curWeight = 0;
    private int curFloor = 5;
    private boolean direction = true;
    private HashMap<Integer, HashSet<PersonRequest>> destMap = new HashMap<>();
    private RequestTable requestTable;
    private Strategy strategy;

    public ElevatorThread(int id,RequestTable requestTable, Strategy strategy) {
        this.id = id;
        this.requestTable = requestTable;
        this.strategy = strategy;
    }

    @Override
    public void run() {
        while (true) {
            AdviceType advice =
                    strategy.getAdvice(curFloor,curWeight,direction,destMap);
            if (advice == AdviceType.MOVE) {
                try {
                    move();
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
            else if (advice == AdviceType.OPEN) {
                try {
                    openAndClose();
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
            else if (advice == AdviceType.OVER) {
                break;
            }
            else if (advice == AdviceType.WAIT) {
                try {
                    requestTable.awaitNewRequest();
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
            else if (advice == AdviceType.REVERSE) {
                direction = !direction;
            }
        }
    }

    private void move() throws InterruptedException {
        if (direction) {
            if (curFloor < 11) {
                curFloor++;
                Thread.sleep(400);
                TimableOutput.println("ARRIVE-" + curFloor + "-" + id);
            }
        }
        else {
            if (curFloor > 1) {
                curFloor--;
                Thread.sleep(400);
                TimableOutput.println("ARRIVE-" + curFloor + "-" + id);
            }
        }
    }

    private void openAndClose() throws InterruptedException {
        TimableOutput.println("OPEN-" + curFloor + "-" + id);
        //下电梯
        HashSet<PersonRequest> custom = destMap.get(curFloor);
        if (custom != null) {
            for (PersonRequest person : custom) {
                curWeight -= person.getWeight();
                TimableOutput.println("OUT-S-" + person.getPersonId() + curFloor + "-" + id);
            }
            destMap.remove(curFloor);
        }
        Thread.sleep(400);
        //上电梯
        HashSet<PersonRequest> people =  requestTable.getRequest(curFloor);
        if (people != null) {
            for (PersonRequest person : people) {
                int weight = person.getWeight() + curWeight;
                if (weight <= 400) {
                    curWeight = weight;
                    requestTable.removeRequest(person);
                    String floorStr = person.getToFloor();
                    int floor = parseFloor(floorStr);
                    destMap.get(floor).add(person);
                    TimableOutput.println("IN-" + person.getPersonId() + curFloor + "-" + id);
                }
            }
        }
        TimableOutput.println("CLOSE-" + curFloor + "-" + id);
    }

    private int parseFloor(String s) {
        int num = Integer.parseInt(s.substring(1));
        if (s.charAt(0) == 'B') {
            return 5 - num;
        } else { // F
            return num + 4;
        }
    }
}
