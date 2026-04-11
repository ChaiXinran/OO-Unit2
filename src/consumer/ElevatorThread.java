package consumer;

import buffer.AdviceType;
import buffer.RequestTable;

import com.oocourse.elevator2.TimableOutput;
import producer.Passenger;
import producer.Person;
import producer.Worker;

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
    private boolean normal = true;
    private Worker maintain = null;

    public ElevatorThread(int id,RequestTable requestTable, Strategy strategy) {
        this.id = id;
        this.requestTable = requestTable;
        this.strategy = strategy;
    }

    @Override
    public void run() {
        while (true) {
            AdviceType advice =
                    strategy.getAdvice(curFloor,curWeight,direction,destMap,normal);
            if (advice == AdviceType.MAINTAIN) {
                try {
                    maintain();
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
            else if (advice == AdviceType.MOVE) {
                try {
                    move();
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
            else if (advice == AdviceType.OPEN) {
                try {
                    ArrayList<Person> peopleServed;//缓冲队列
                    ArrayList<Person> peopleOut = letOutCurrentFloor();
                    synchronized (requestTable) {
                        //二次咨询
                        AdviceType checkOpen = strategy.getAdvice(
                                curFloor,curWeight,direction,destMap,normal);
                        if (checkOpen != AdviceType.OPEN) {
                            continue;
                        }
                        //将能接的人从请求队列中移出，加入到缓冲队列中
                        peopleServed = requestTable.letInRequests(
                                curFloor,curWeight,direction,destMap);
                    }
                    openAndServe(peopleServed,peopleOut);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
            else if (advice == AdviceType.OVER) {
                closeDoor(curFloor);
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

    private void maintain() throws InterruptedException {
        clearPeople();
        //repair 至少1s
        TimableOutput.println("MAINT1-BEGIN-" + id);
        sleep(1000);
        //测试阶段直奔目标楼层
        TimableOutput.println("MAINT2-BEGIN-" + id);
        districtTo(maintain.getToFloor());
        //返回
        districtTo(5);
        //开门，维修人员下来
        TimableOutput.println("MAINT-END-" + id);
        doorOpen = true;
        normal = true;
    }

    private void move() throws InterruptedException {
        closeDoor(curFloor);
        if (direction) {
            if (curFloor < 11) {
                Thread.sleep(400);
                curFloor++;
                arrive(curFloor);
            }
        }
        else {
            if (curFloor > 1) {
                Thread.sleep(400);
                curFloor--;
                arrive(curFloor);
            }
        }
    }

    private void openAndServe(ArrayList<Person> peopleIn,
                              ArrayList<Person> peopleOut) throws InterruptedException {
        openDoor(curFloor);
        letOutPrint(peopleOut);
        letInCurrentFloor(peopleIn);
        //不关门
    }

    private void letInCurrentFloor(ArrayList<Person> inList) {
        if (inList.isEmpty()) {
            return;
        }
        curWeight = requestTable.getNewWeight();
        for (Person person : inList) {
            TimableOutput.println("IN-" + person.getId() + "-"
                    + strFloor[curFloor - 1] + "-" + id);
        }
    }

    private ArrayList<Person> letOutCurrentFloor() {
        ArrayList<Person> out = new ArrayList<>();
        HashSet<Person> outSet = destMap.get(curFloor);
        if (outSet == null || outSet.isEmpty()) {
            return null;
        }
        for (Person person : outSet) {
            curWeight -= person.getWeight();
            out.add(person);
        }
        destMap.remove(curFloor);
        return out;
    }

    private void letOutPrint(ArrayList<Person> people) {
        if (people == null || people.isEmpty()) {
            return;
        }
        for (Person person : people) {
            TimableOutput.println("OUT-S-" + person.getId() + "-"
                    + strFloor[curFloor - 1] + "-" + id);
        }
    }

    private void letOut(Integer floor) {
        HashSet<Person> outSet = destMap.get(floor);
        if (outSet == null || outSet.isEmpty()) {
            return;
        }
        for (Person person : outSet) {
            curWeight -= person.getWeight();
            TimableOutput.println("OUT-S-" + person.getId() + "-"
                    + strFloor[floor - 1] + "-" + id);
        }
        destMap.remove(floor);
    }

    private void clearRequest(Integer floor) {
        for (Integer f : destMap.keySet()) {
            if (floor.equals(f)) {
                //到达目的地
                for (Person person : destMap.get(f)) {
                    curWeight -= person.getWeight();
                    TimableOutput.println("OUT-S-" + person.getId() + "-"
                            + strFloor[floor - 1] + "-" + id);
                }
            }
            else  {
                //没有到达目的地
                for (Person person : destMap.get(f)) {
                    curWeight -= person.getWeight();
                    TimableOutput.println("OUT-F-" + person.getId() + "-"
                            + strFloor[floor - 1] + "-" + id);
                    Person other = new Passenger(person.getId(),
                            strFloor[person.getFromFloor() - 1],
                            strFloor[person.getToFloor() - 1], person.getWeight());
                    //放回请求列表
                    requestTable.addRequest(other);
                }
            }
        }
        //清空电梯内的人
        destMap.clear();
    }

    private Integer preClear() {
        if (curFloor == 11 && destMap.size() >= 7) {
            int cnt = 0;//1-7楼中需要停靠的楼的数量
            int size = 10;//电梯最多承载8个人
            int leastFloor = -1;
            for (Integer floor : destMap.keySet()) {
                if (floor > 5 && floor < 11) {
                    cnt++;
                    if (destMap.get(floor).size() < size) {
                        size = destMap.get(floor).size();
                        leastFloor = floor;
                    }
                }
            }
            if (cnt < 7) {
                return -1;
            }
            else {
                return leastFloor;
            }
        }
        return -1;
    }

    private void clearPeople() throws InterruptedException {
        if (curFloor != 5) {
            openDoor(curFloor);
            letOut(curFloor);
            closeDoor(curFloor);
            if (curFloor < 5) {
                direction = true;
                for (int i = curFloor + 1; i < 5; i++) {
                    sleep(400);
                    arrive(i);
                    openDoor(i);
                    letOut(i);
                    closeDoor(i);
                }
            }
            else {
                Integer noStop;
                noStop = preClear();
                direction = false;
                for (int i = curFloor - 1; i > 5; i--) {
                    sleep(400);
                    arrive(i);
                    if (i != noStop) {
                        openDoor(i);
                        letOut(i);
                        closeDoor(i);
                    }
                }
            }
            sleep(400);
            curFloor = 5;
            arrive(curFloor);
        }

        //电梯在一楼如何处理
        openDoor(curFloor);
        clearRequest(curFloor);
        closeDoor(curFloor);
    }

    private void districtTo(Integer floor) throws InterruptedException {
        if (floor > curFloor) {
            direction = true;
            for (int i = curFloor; i <= floor; i++) {
                sleep(200);
                arrive(i);
            }
        }
        else {
            direction = false;
            for (int i = curFloor; i >= floor; i--) {
                sleep(200);
                arrive(i);
            }
        }
        curFloor = floor;
    }

    private void arrive(Integer floor) {
        TimableOutput.println("ARRIVE-" + strFloor[floor - 1] + "-" + id);
    }

    private void closeDoor(Integer floor) {
        if (doorOpen) {
            TimableOutput.println("CLOSE-" + strFloor[floor - 1] + "-" + id);
            doorOpen = false;
        }
    }

    private void openDoor(Integer floor) throws InterruptedException {
        if (!doorOpen) {
            TimableOutput.println("OPEN-" + strFloor[floor - 1] + "-" + id);
            sleep(400);
            doorOpen = true;
        }
    }

    public void setNormal(boolean normal) {
        this.normal = normal;
    }

    public void setWorker(Worker maintain) {
        this.maintain = maintain;
    }
}
