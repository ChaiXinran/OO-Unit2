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
                if (person != null) {
                    //还没结束
                    if (!allRequests.isEnd()) {
                        allRequests.awaitNewRequest();
                    }
                    //将person加入电梯表
                    int num = bestElevator(person);
                    requestTables.get(num - 1).addRequest(person);
                    TimableOutput.println("RECEIVE-" + person.getPersonId() + "-" + num);
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
