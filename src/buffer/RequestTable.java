package buffer;

import consumer.ElevatorThread;

import producer.Person;
import producer.Worker;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class RequestTable {
    private boolean endFlag = false;
    private final HashMap<Integer, HashSet<Person>> requestMap = new HashMap<>();

    private Integer maintainRequestNum = 0;
    private Integer maintainNotDealNum = 0;

    private final ReentrantReadWriteLock rwLock = new ReentrantReadWriteLock();
    private final Lock readLock = rwLock.readLock();
    private final Lock writeLock = rwLock.writeLock();
    private final Object notifier = new Object();

    private final HashMap<Integer, ElevatorThread> elevatorMap;

    public RequestTable(HashMap<Integer, ElevatorThread> elevatorMap) {
        this.elevatorMap = elevatorMap;
    }

    public void addRequest(Person person) {
        int floor = person.getFromFloor();
        writeLock.lock();
        try {
            //如果这一层没有集合，就新建一个HashSet
            HashSet<Person> set = requestMap.computeIfAbsent(floor, k -> new HashSet<>());
            // 然后把person加进去
            set.add(person);
            if (person instanceof Worker) {
                maintainRequestNum++;
                maintainNotDealNum++;
            }
        } finally {
            writeLock.unlock();
        }
        //通知
        synchronized (notifier) {
            notifier.notifyAll();
        }
    }

    public void signal() {
        synchronized (notifier) {
            notifier.notifyAll();
        }
    }

    public void addRequests(ArrayList<Person> people) {
        if (people == null || people.isEmpty()) {
            return;
        }
        writeLock.lock();
        try {
            for (Person person : people) {
                int floor = person.getFromFloor();
                HashSet<Person> set = requestMap.computeIfAbsent(floor, k -> new HashSet<>());
                set.add(person);
                if (person instanceof Worker) {
                    maintainRequestNum++;
                    maintainNotDealNum++;
                }
            }
        } finally {
            writeLock.unlock();
        }
        synchronized (notifier) {
            notifier.notifyAll();
        }
    }

    //随便取一个请求
    public Person pollRequest() {
        writeLock.lock();
        try {
            if (maintainRequestNum > 0) {
                for (Integer floor : requestMap.keySet()) {
                    HashSet<Person> set = requestMap.get(floor);
                    if (set == null || set.isEmpty()) {
                        continue;
                    }
                    Person target = null;
                    for (Person person : set) {
                        if (person instanceof Worker) {
                            target = person;
                            break;
                        }
                    }
                    if (target != null) {
                        maintainRequestNum--;
                        set.remove(target);
                        if (set.isEmpty()) {
                            requestMap.remove(floor);
                        }
                        return target;
                    }
                }
            }
            else {
                for (Integer floor : requestMap.keySet()) {
                    HashSet<Person> set = requestMap.get(floor);
                    if (!set.isEmpty()) {
                        Person person = set.iterator().next();
                        set.remove(person);
                        if (set.isEmpty()) {
                            requestMap.remove(floor);
                        }
                        return person;
                    }
                }
            }
            return null;
        }
        finally {
            writeLock.unlock();
        }
    }

    public boolean isEmpty() {
        readLock.lock();
        try {
            return requestMap.isEmpty();
        } finally {
            readLock.unlock();
        }
    }

    public void setEndFlag(boolean endFlag) {
        writeLock.lock();
        try {
            this.endFlag = endFlag;
        } finally {
            writeLock.unlock();
        }
        //通知
        synchronized (notifier) {
            notifier.notifyAll();
        }
    }

    public boolean isEnd() {
        readLock.lock();
        try {
            return endFlag && (maintainNotDealNum == 0);
        } finally {
            readLock.unlock();
        }
    }

    public void awaitNewRequest() throws InterruptedException {
        synchronized (notifier) {
            while (isEmpty() && !isEnd()) {
                notifier.wait();
            }
        }
    }

    public void subMaintainNum() {
        writeLock.lock();
        try {
            maintainNotDealNum--;
        }
        finally {
            writeLock.unlock();
        }
        synchronized (notifier) {
            notifier.notifyAll();
        }
    }
}
