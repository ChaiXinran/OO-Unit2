package producer;

import buffer.RequestTable;
import com.oocourse.elevator2.ElevatorInput;
import com.oocourse.elevator2.PersonRequest;
import com.oocourse.elevator2.Request;
import com.oocourse.elevator2.MaintRequest;

import java.io.IOException;

public class InputThread extends Thread {
    private final RequestTable allRequests;

    public InputThread(RequestTable allRequests) {
        this.allRequests = allRequests;
    }

    @Override
    public void run() {
        try {
            ElevatorInput elevatorInput = new ElevatorInput(System.in);
            while (true) {
                Request request = elevatorInput.nextRequest();
                if (request == null) {
                    allRequests.setEndFlag(true);
                    break;
                }
                else {
                    if (request instanceof PersonRequest) {
                        PersonRequest personRequest = (PersonRequest) request;
                        Passenger person = new Passenger(personRequest.getPersonId(),
                                personRequest.getFromFloor(),
                                personRequest.getToFloor(), personRequest.getWeight());
                        allRequests.addRequest(person);
                    }
                    else if (request instanceof MaintRequest) {
                        MaintRequest maintRequest = (MaintRequest) request;
                        Worker worker = new Worker(maintRequest.getWorkerId(),
                                maintRequest.getToFloor(),
                                maintRequest.getElevatorId());
                        allRequests.addRequest(worker);
                    }
                }
            }
            elevatorInput.close();
        } catch (IOException e) {
            Thread.currentThread().interrupt();
        }
    }
}
