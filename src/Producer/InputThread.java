package Producer;

import Buffer.RequestTable;
import com.oocourse.elevator1.ElevatorInput;
import com.oocourse.elevator1.PersonRequest;
import com.oocourse.elevator1.Request;

import java.io.IOException;

public class InputThread extends Thread {
    private RequestTable allRequests;

    public InputThread(RequestTable allRequests) {
        this.allRequests = allRequests;
    }

    @Override
    public void run() {
        try {
            ElevatorInput elevatorInput = new ElevatorInput(System.in);
            while(true) {
                Request request = elevatorInput.nextRequest();
                if(request == null) {
                    allRequests.setEndFlag(true);
                    break;
                }
                else {
                    allRequests.addRequest((PersonRequest) request);
                }
            }
            elevatorInput.close();
        }catch (IOException e){
            Thread.currentThread().interrupt();
        }
    }
}
