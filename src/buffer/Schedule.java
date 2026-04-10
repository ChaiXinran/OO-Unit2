package buffer;

import consumer.ElevatorThread;

import com.oocourse.elevator1.TimableOutput;
import producer.Person;

import java.util.ArrayList;
import java.util.HashMap;

public class Schedule extends Thread {
    private final RequestTable allRequests;
    private final ArrayList<RequestTable> requestTables;
    private final HashMap<Integer, ElevatorThread> elevatorMap;
    //private final String[] strFloor = {"B4","B3","B2","B1","F1","F2","F3","F4","F5","F6","F7"};

    public Schedule(RequestTable allRequests,
                    ArrayList<RequestTable> requestTables,
                    HashMap<Integer, ElevatorThread> elevatorMap) {
        this.allRequests = allRequests;
        this.requestTables = requestTables;
        this.elevatorMap = elevatorMap;
    }

    @Override
    public void run() {
        try {
            while (true) {
                if (allRequests.isEmpty() && allRequests.isEnd()) {
                    //标志endFlag
                    for (RequestTable requestTable : requestTables) {
                        requestTable.setEndFlag(true);
                    }
                    return;
                }
                Person person = allRequests.pollRequest();
                if (person == null) {
                    //还没结束
                    if (!allRequests.isEnd()) {
                        allRequests.awaitNewRequest();
                    }
                }
                else {
                    //将person加入电梯表
                    int num = bestElevator(person);
                    TimableOutput.println("RECEIVE-" +
                            person.getId() + "-"
                            + num);
                    requestTables.get(num - 1).addRequest(person);
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    public int bestElevator(Person person) {
        return person.getElevatorId();
    }
}
