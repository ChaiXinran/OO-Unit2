import buffer.RequestQueue;
import buffer.RequestTable;
import buffer.Schedule;
import consumer.Strategy;
import consumer.ElevatorThread;
import producer.InputThread;
import com.oocourse.elevator2.TimableOutput;

import java.util.ArrayList;
import java.util.HashMap;

public class MainClass {
    public static void main(String[] args) {
        TimableOutput.initStartTimestamp();

        HashMap<Integer, ElevatorThread> elevatorMap = new HashMap<>();
        ArrayList<RequestQueue>  requestQueueList = new ArrayList<>();
        RequestTable allRequests = new RequestTable(elevatorMap);
        for (int i = 1; i <= 6; i++) {
            RequestQueue requestQueue = new RequestQueue();
            Strategy strategy = new Strategy(allRequests, requestQueue);
            ElevatorThread elevatorThread = new ElevatorThread(i,allRequests,requestQueue,strategy);
            requestQueueList.add(requestQueue);
            elevatorMap.put(i,elevatorThread);
            elevatorThread.start();
        }
        InputThread inputThread = new InputThread(allRequests);
        inputThread.start();
        Schedule schedule = new Schedule(allRequests,requestQueueList,elevatorMap);
        schedule.start();
    }
}
