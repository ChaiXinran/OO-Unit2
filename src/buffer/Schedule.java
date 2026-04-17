package buffer;

import com.oocourse.elevator2.TimableOutput;
import consumer.ElevatorThread;

import producer.Passenger;
import producer.Person;
import producer.Worker;

import java.util.ArrayList;
import java.util.HashMap;

public class Schedule extends Thread {
    private final RequestTable allRequests;
    private final HashMap<Integer, ElevatorThread> elevatorMap;
    private final ArrayList<RequestQueue> requestQueues;
    private int dispatchCursor = 1;
    //private final String[] strFloor = {"B4","B3","B2","B1","F1","F2","F3","F4","F5","F6","F7"};

    public Schedule(RequestTable allRequests,
                    ArrayList<RequestQueue> requestQueues,
                    HashMap<Integer, ElevatorThread> elevatorMap) {
        this.allRequests = allRequests;
        this.elevatorMap = elevatorMap;
        this.requestQueues = requestQueues;
    }

    @Override
    public void run() {
        try {
            while (true) {
                if (allRequests.isEmpty() && allRequests.isEnd()) {
                    for (int i = 1; i <= 6;i++) {
                        requestQueues.get(i - 1).setEndFlag(true, elevatorMap.get(i));
                    }
                    break;
                }

                if (!hasUsableElevator()) {
                    allRequests.awaitNewRequest();
                    continue;
                }

                Person person = allRequests.pollRequest();
                if (person == null) {
                    //还没结束
                    if (!allRequests.isEnd()) {
                        allRequests.awaitNewRequest();
                    }
                }
                else {
                    if (person instanceof Worker) {
                        Worker worker = (Worker) person;
                        int num = maintainElevator(worker);
                        //TimableOutput.println("Maintain");
                        elevatorMap.get(num).wakeUpForMaintain(worker);
                        //TimableOutput.println(elevatorMap.get(num).getNormal());
                        //TimableOutput.println("WakeUp End");
                    }
                    else if (person instanceof Passenger) {
                        Passenger passenger = (Passenger) person;
                        int num = bestElevator(passenger);
                        TimableOutput.println("RECEIVE-" + passenger.getId() + "-" + num);
                        requestQueues.get(num - 1).addRequest(passenger,elevatorMap.get(num));
                    }
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private int maintainElevator(Worker worker) {
        return worker.getElevatorId();
    }

    private int bestElevator(Passenger person) {
        int leastCost = Integer.MAX_VALUE;
        ArrayList<Integer> candidates = new ArrayList<>();

        for (int i = 1; i <= 6; i++) {
            ElevatorThread elevator = elevatorMap.get(i);
            int cost = elevator.getCost(person);

            if (cost < leastCost) {
                leastCost = cost;
                candidates.clear();
                candidates.add(i);
            } else if (cost == leastCost) {
                candidates.add(i);
            }
        }

        if (leastCost == Integer.MAX_VALUE) {
            return -1;
        }

        if (candidates.size() == 1) {
            return candidates.get(0);
        }

        return pickByCursor(candidates);
    }

    private int pickByCursor(ArrayList<Integer> candidates) {
        for (int k = 0; k < 6; k++) {
            int id = (dispatchCursor + k - 1) % 6 + 1;
            if (candidates.contains(id)) {
                dispatchCursor = id % 6 + 1;
                return id;
            }
        }
        return candidates.get(0);
    }

    private boolean hasUsableElevator() {
        for (int i = 1; i <= 6; i++) {
            ElevatorThread elevator = elevatorMap.get(i);
            if (elevator.isNormal() && elevator.assignNum() < 8) {
                return true;
            }
        }
        return false;
    }
}
