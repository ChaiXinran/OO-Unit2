package buffer;

import consumer.ElevatorThread;

import producer.Person;
import producer.Worker;

import java.util.HashMap;

public class Schedule extends Thread {
    private final RequestTable allRequests;
    private final RequestTable maintainRequests;
    private final HashMap<Integer, ElevatorThread> elevatorMap;
    //private final String[] strFloor = {"B4","B3","B2","B1","F1","F2","F3","F4","F5","F6","F7"};

    public Schedule(RequestTable allRequests,
                    RequestTable maintainRequests,
                    HashMap<Integer, ElevatorThread> elevatorMap) {
        this.allRequests = allRequests;
        this.maintainRequests = maintainRequests;
        this.elevatorMap = elevatorMap;
    }

    @Override
    public void run() {
        try {
            while (true) {
                if (allRequests.isEmpty() && allRequests.isEnd()) {
                    if (maintainRequests.isEmpty() && maintainRequests.isEnd()) {
                        return;
                    }
                }
                Person person = allRequests.pollRequest();
                if (person == null) {
                    //还没结束
                    if (!allRequests.isEnd()) {
                        allRequests.awaitNewRequest();
                    }
                    if (!maintainRequests.isEnd()) {
                        maintainRequests.awaitNewRequest();
                    }
                }
                else {
                    //将MaintainRequest分配给电梯
                    Person worker = maintainRequests.pollRequest();
                    int num = bestElevator((Worker)worker);
                    elevatorMap.get(num - 1).setWorker((Worker)worker);
                    elevatorMap.get(num - 1).setNormal(false);
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    public int bestElevator(Worker worker) {
        return worker.getElevatorId();
    }
}
