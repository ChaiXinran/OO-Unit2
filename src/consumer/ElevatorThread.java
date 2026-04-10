package consumer;

import buffer.AdviceType;
import buffer.RequestTable;
import buffer.Strategy;

import com.oocourse.elevator1.TimableOutput;
import producer.Person;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

public class ElevatorThread extends Thread {
    private final int id;
    private int curWeight = 0;
    private int curFloor = 5;
    private final String[] strFloor = {"B4","B3","B2","B1","F1","F2","F3","F4","F5","F6","F7"};
    private boolean direction = true;
    private final HashMap<Integer, HashSet<Person>> destMap = new HashMap<>();
    private final RequestTable requestTable;
    private final Strategy strategy;
    private boolean doorOpen = false;

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
                    openAndServe();
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
            else if (advice == AdviceType.OVER) {
                if (doorOpen) {
                    TimableOutput.println("CLOSE-" + strFloor[curFloor - 1] + "-" + id);
                    doorOpen = false;
                }
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
        if (doorOpen) {
            TimableOutput.println("CLOSE-" + strFloor[curFloor - 1] + "-" + id);
            doorOpen = false;
        }
        if (direction) {
            if (curFloor < 11) {
                Thread.sleep(400);
                curFloor++;
                TimableOutput.println("ARRIVE-" + strFloor[curFloor - 1] + "-" + id);
            }
        }
        else {
            if (curFloor > 1) {
                Thread.sleep(400);
                curFloor--;
                TimableOutput.println("ARRIVE-" + strFloor[curFloor - 1] + "-" + id);
            }
        }
    }

    private void openAndClose() throws InterruptedException {
        TimableOutput.println("OPEN-" + strFloor[curFloor - 1] + "-" + id);
        //下电梯
        HashSet<Person> custom = destMap.get(curFloor);
        if (custom != null) {
            for (Person person : custom) {
                curWeight -= person.getWeight();
                TimableOutput.println("OUT-S-" +
                        person.getId() + "-" +
                        strFloor[curFloor - 1] + "-" + id);
            }
            destMap.remove(curFloor);
        }
        Thread.sleep(400);
        //上电梯
        ArrayList<Person> customers =
                requestTable.letInRequests(curFloor,curWeight,direction,destMap);
        if (!customers.isEmpty()) {
            String str = strFloor[curFloor - 1];
            curWeight = requestTable.getNewWeight();
            for (Person person : customers) {
                requestTable.removeRequest(person);
                TimableOutput.println("IN-" +
                        person.getId() + "-" +
                        str + "-" + id);
            }
        }
        TimableOutput.println("CLOSE-" + strFloor[curFloor - 1] + "-" + id);
    }

    private void openAndServe() throws InterruptedException {
        if (!doorOpen) {
            TimableOutput.println("OPEN-" + strFloor[curFloor - 1] + "-" + id);
            doorOpen = true;
            sleep(400);
        }
        letOutCurrentFloor();
        letInCurrentFloor();
        //不关门
    }

    private void letOutCurrentFloor() {
        HashSet<Person> outSet = destMap.get(curFloor);
        if (outSet == null || outSet.isEmpty()) {
            return;
        }
        for (Person person : outSet) {
            curWeight -= person.getWeight();
            TimableOutput.println("OUT-S-" + person.getId() + "-"
                    + strFloor[curFloor - 1] + "-" + id);
        }
        destMap.remove(curFloor);
    }

    private void letInCurrentFloor() {
        ArrayList<Person> inList =
                requestTable.letInRequests(curFloor, curWeight, direction, destMap);
        if (inList.isEmpty()) {
            return;
        }
        curWeight = requestTable.getNewWeight();
        for (Person person : inList) {
            //requestTable.removeRequest(person);
            TimableOutput.println("IN-" + person.getId() + "-"
                    + strFloor[curFloor - 1] + "-" + id);
        }
    }
}
