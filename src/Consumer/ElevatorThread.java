package Consumer;

import Buffer.AdviceType;
import Buffer.RequestTable;
import Buffer.Strategy;
import com.oocourse.elevator1.ElevatorInput;
import com.oocourse.elevator1.PersonRequest;

import java.util.HashMap;
import java.util.HashSet;

public class ElevatorThread extends Thread {
    private int id;
    private int curWeight = 0;
    private int curFloor = 5;
    private boolean direction = true;
    private HashMap<Integer, HashSet<PersonRequest>> destMap = new HashMap<>();
    private RequestTable requestTable;
    private Strategy strategy;

    public ElevatorThread(int id,RequestTable requestTable, Strategy strategy) {
        this.id = id;
        this.requestTable = requestTable;
        this.strategy = strategy;
    }

    @Override
    public void run() {
        while (true) {
            AdviceType advice =
                    strategy.getAdvice(curFloor,curWeight,direction,destMap);
            if (advice == AdviceType.MOVE){
                move();
            }
            else if (advice == AdviceType.OPEN){
                try {
                    openAndClose();
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
            else if(advice == AdviceType.OVER){
                break;
            }
            else if(advice == AdviceType.WAIT){
                try {
                    requestTable.awaitNewRequest();
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
            else if (advice == AdviceType.REVERSE){
                direction = !direction;
            }
        }
    }

    private void move(){
        if (direction){
            curFloor++;
        }
        else {
            curFloor--;
        }
    }

    private void openAndClose() throws InterruptedException {
        //下电梯
        HashSet<PersonRequest> custom = destMap.get(curFloor);
        for (PersonRequest person : custom) {
            curWeight -= person.getWeight();
        }
        destMap.remove(curFloor);
        Thread.sleep(200);
        //上电梯
        HashSet<PersonRequest> people =  requestTable.getRequest(curFloor);
        for (PersonRequest person : people){
            int weight = person.getWeight() + curWeight;
            if (weight <= 400){
                curWeight = weight;
                requestTable.removeRequest(person);
                destMap.get(person.getToFloor()).add(person);
            }
        }
    }
}
