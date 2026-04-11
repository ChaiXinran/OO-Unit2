import buffer.RequestTable;
import buffer.Schedule;
import consumer.Strategy;
import consumer.ElevatorThread;
import producer.InputThread;
import com.oocourse.elevator2.TimableOutput;

import java.util.HashMap;

public class MainClass {
    public static void main(String[] args) {
        TimableOutput.initStartTimestamp();

        RequestTable allRequests = new RequestTable();
        RequestTable maintainRequests = new RequestTable();
        HashMap<Integer, ElevatorThread> elevatorMap = new HashMap<>();
        for (int i = 1; i <= 6; i++) {
            Strategy strategy = new Strategy(allRequests);
            ElevatorThread elevatorThread = new ElevatorThread(i,allRequests,strategy);
            elevatorMap.put(i,elevatorThread);
            elevatorThread.start();
        }
        InputThread inputThread = new InputThread(allRequests,maintainRequests);
        inputThread.start();
        Schedule schedule = new Schedule(allRequests,maintainRequests,elevatorMap);
        schedule.start();
    }
}
