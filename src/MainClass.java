import buffer.RequestTable;
import buffer.Schedule;
import buffer.Strategy;
import consumer.ElevatorThread;
import producer.InputThread;
import com.oocourse.elevator1.TimableOutput;

import java.util.ArrayList;
import java.util.HashMap;

public class MainClass {
    public static void main(String[] args) {
        TimableOutput.initStartTimestamp();

        RequestTable allRequests = new RequestTable();
        ArrayList<RequestTable> requestTables = new ArrayList<>();
        HashMap<Integer, ElevatorThread> elevatorMap = new HashMap<>();
        for (int i = 1; i <= 6; i++) {
            RequestTable requestTable = new RequestTable();
            Strategy strategy = new Strategy(requestTable);
            ElevatorThread elevatorThread = new ElevatorThread(i,requestTable,strategy);
            requestTables.add(requestTable);
            elevatorMap.put(i,elevatorThread);
            elevatorThread.start();
        }
        InputThread inputThread = new InputThread(allRequests);
        inputThread.start();
        Schedule schedule = new Schedule(allRequests,requestTables,elevatorMap);
        schedule.start();
    }
}
