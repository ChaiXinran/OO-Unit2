package consumer;

import buffer.RequestTable;
import producer.Worker;

public class MaintainStrategy {
    private Worker worker;
    private RequestTable requestTable;

    public MaintainStrategy(Worker worker, RequestTable requestTable) {
        this.worker = worker;
        this.requestTable = requestTable;
    }


}
