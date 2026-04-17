package consumer;

import buffer.AdviceType;
import buffer.RequestQueue;
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
    private final RequestQueue requestQueue;
    private final Strategy strategy;
    private boolean doorOpen = false;
    private boolean normal = true;//需要上锁
    private Worker maintain = null;//需要上锁
    private final Object stateLock = new Object();

    public ElevatorThread(int id,RequestTable requestTable,
                          RequestQueue requestQueue,Strategy strategy) {
        this.id = id;
        this.requestTable = requestTable;
        this.requestQueue = requestQueue;
        this.strategy = strategy;
    }

    @Override
    public void run() {
        while (true) {
            AdviceType advice = getAdviceSafely();
            //TimableOutput.println(id + " " + advice.toString());
            if (advice == AdviceType.MAINTAIN) {
                try {
                    maintain();
                    continue;
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
            if (advice == AdviceType.MOVE) {
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
                    peopleServed = requestQueue.letInRequests(
                            curFloor,curWeight,direction,destMap);
                    //TimableOutput.println("Success in get PeopleServed");
                    /*synchronized (requestTable) {
                        //二次咨询
                        AdviceType checkOpen = getAdviceSafely();
                        //TimableOutput.println("Success in get Advice 2");
                        if (checkOpen != AdviceType.OPEN
                                && peopleOut == null && peopleServed == null) {
                            //TimableOutput.println("Success in continue");
                            continue;
                        }
                        //将能接的人从请求队列中移出，加入到缓冲队列中
                        if (checkOpen == AdviceType.OPEN) {
                            requestTable.letInRequests(
                                    curFloor,curWeight,direction,id,destMap,peopleServed);
                        }
                    }*/
                    //TimableOutput.println("Success to open and Serve");
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
                    awaitWork();
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
        //TimableOutput.println("Maintain Begin");
        Worker worker = getMaintainWorkerSafely();
        if (worker == null) {
            return;
        }
        clearPeople(worker);
        //repair 至少1s
        TimableOutput.println("MAINT1-BEGIN-" + id);
        clearQueue();
        sleep(1000);
        //测试阶段直奔目标楼层
        TimableOutput.println("MAINT2-BEGIN-" + id);
        districtTo(worker.getToFloor());
        //返回
        districtTo(5);
        //开门，维修人员下来
        openDoor(5);
        TimableOutput.println("OUT-S-" + worker.getId() + "-"
                + strFloor[curFloor - 1] + "-" + id);
        closeDoor(5);
        TimableOutput.println("MAINT-END-" + id);

        synchronized (stateLock) {
            normal = true;
            maintain = null;
        }
        //完成检修
        requestTable.subMaintainNum();
        //有一个电梯完成检修，可以通知全局，唤醒调度器
        requestTable.signal();
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
        //TimableOutput.println("OpenAndServe");
        openDoor(curFloor);
        letOutPrint(peopleOut);
        letInCurrentFloor(peopleIn);
        //不关门
        //叫醒调度器
        requestTable.signal();
    }

    private void letInCurrentFloor(ArrayList<Person> inList) {
        if (inList.isEmpty()) {
            return;
        }
        for (Person person : inList) {
            curWeight += person.getWeight();
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

    private void letOut(Integer floor) throws InterruptedException {
        HashSet<Person> outSet = destMap.get(floor);
        if (outSet == null || outSet.isEmpty()) {
            closeDoor(floor);
            return;
        }
        openDoor(floor);
        for (Person person : outSet) {
            curWeight -= person.getWeight();
            TimableOutput.println("OUT-S-" + person.getId() + "-"
                    + strFloor[floor - 1] + "-" + id);
        }
        destMap.remove(floor);
        closeDoor(floor);
    }

    private void clearRequest(Integer floor) {
        ArrayList<Person> recycled = new ArrayList<>();
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
                    Person other = new Passenger(person.getId(), "F1",
                            strFloor[person.getToFloor() - 1], person.getWeight());
                    //放回请求列表
                    recycled.add(other);
                }
            }
        }
        //清空电梯内的人
        destMap.clear();
        requestTable.addRequests(recycled);
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

    private void clearQueue() {
        ArrayList<Person> waitingPeople = requestQueue.drainAllRequests();
        requestTable.addRequests(waitingPeople);
    }

    private void clearPeople(Worker worker) throws InterruptedException {
        if (curFloor != 5) {
            letOut(curFloor);
            if (curFloor < 5) {
                direction = true;
                for (int i = curFloor + 1; i < 5; i++) {
                    sleep(400);
                    arrive(i);
                    letOut(i);
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
                        letOut(i);
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
        TimableOutput.println("IN-" + worker.getId() + "-"
                + strFloor[curFloor - 1] + "-" + id);
        closeDoor(curFloor);
    }

    private void districtTo(Integer floor) throws InterruptedException {
        if (floor > curFloor) {
            direction = true;
            for (int i = curFloor + 1; i <= floor; i++) {
                sleep(200);
                arrive(i);
            }
        }
        else {
            direction = false;
            for (int i = curFloor - 1; i >= floor; i--) {
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

    public void awaitWork() throws InterruptedException {
        synchronized (stateLock) {
            while (normal && requestQueue.isEmpty() && !requestQueue.isEnd()) {
                stateLock.wait();
            }
        }
    }

    public void wakeUpForMaintain(Worker worker) {
        synchronized (stateLock) {
            this.maintain = worker;
            this.normal = false;
            stateLock.notifyAll();
        }
    }

    public void wakeUp() {
        synchronized (stateLock) {
            stateLock.notifyAll();
        }
    }

    private AdviceType getAdviceSafely() {
        synchronized (stateLock) {
            return strategy.getAdvice(curFloor, curWeight, direction, destMap, normal);
        }
    }

    private Worker getMaintainWorkerSafely() {
        synchronized (stateLock) {
            return maintain;
        }
    }

    public int getCost(Person person) {
        synchronized (stateLock) {
            if (!normal) {
                return Integer.MAX_VALUE;
            }

            int from = person.getFromFloor();
            boolean reqDir = person.isDirection();

            int pickup;
            pickup = Math.abs(curFloor - from);
            int pathPenalty;
            int busyPenalty;
            int loadPenalty;

            boolean hasPassengers = curWeight > 0;
            int stopCount = destMap.size();

            if (assignNum() >= 8) {
                return Integer.MAX_VALUE;
            }

            if (!hasPassengers) {
                // 空载：鼓励主动竞争，方向只做轻微修正
                pathPenalty = (direction == reqDir ? 0 : 3);
                busyPenalty = 0;
            } else {

                boolean sameDir = (direction == reqDir);
                boolean onTheWay =
                        (direction && from >= curFloor) || (!direction && from <= curFloor);

                if (sameDir && onTheWay) {
                    // 顺路捎带：最优先
                    pathPenalty = 0;
                    busyPenalty = 2 * stopCount;
                } else if (sameDir) {
                    // 同向但在背后：要回头
                    pathPenalty = 12;
                    busyPenalty = 3 * stopCount;
                } else {
                    // 反向：尽量少接
                    pathPenalty = 22;
                    busyPenalty = 4 * stopCount;
                }
            }

            if (curWeight >= 350) {
                loadPenalty = 12;
            } else if (curWeight >= 250) {
                loadPenalty = 5;
            } else {
                loadPenalty = 0;
            }

            int pickupWeight = hasPassengers ? 8 : 10;
            return pickupWeight * pickup + pathPenalty + busyPenalty + loadPenalty;
        }
    }

    public synchronized boolean isNormal() {
        return normal;
    }

    public synchronized int assignNum() {
        return requestQueue.getAssignedCount();
    }
}
