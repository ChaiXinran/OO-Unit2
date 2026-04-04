package buffer;

import consumer.ElevatorThread;
import com.oocourse.elevator1.PersonRequest;
import com.oocourse.elevator1.TimableOutput;

import java.util.ArrayList;
import java.util.HashMap;

public class Schedule extends Thread {
    private RequestTable allRequests;
    private ArrayList<RequestTable> requestTables;
    private HashMap<Integer, ElevatorThread> elevatorMap;
    private final String[] strFloor = {"B4","B3","B2","B1","F1","F2","F3","F4","F5","F6","F7"};

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
                PersonRequest person = allRequests.pollRequest();
                if (person == null) {
                    //还没结束
                    if (!allRequests.isEnd()) {
                        allRequests.awaitNewRequest();
                    }
                }
                else {
                    //将person加入电梯表
                    int num = bestElevator(person);
                    requestTables.get(num - 1).addRequest(person);
                    TimableOutput.println("RECEIVE-" +
                            person.getPersonId() + "-"
                            + strFloor[num - 1]);
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    public int bestElevator(PersonRequest person) {
        return person.getElevatorId();
    }
}
