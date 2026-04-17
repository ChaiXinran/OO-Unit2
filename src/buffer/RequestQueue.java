package buffer;

import consumer.ElevatorThread;
import producer.Passenger;
import producer.Person;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class RequestQueue {
    private boolean endFlag = false;
    private final HashMap<Integer, HashSet<Person>> requestMap = new HashMap<>();
    private Integer newWeight = -1;
    private int assignedCount = 0;

    private final ReentrantReadWriteLock rwLock = new ReentrantReadWriteLock();
    private final Lock readLock = rwLock.readLock();
    private final Lock writeLock = rwLock.writeLock();
    private final Object notifier = new Object();

    public void addRequest(Person person,ElevatorThread elevator) {
        int floor = person.getFromFloor();
        writeLock.lock();
        try {
            //如果这一层没有集合，就新建一个HashSet
            HashSet<Person> set = requestMap.computeIfAbsent(floor, k -> new HashSet<>());
            // 然后把person加进去
            set.add(person);
            assignedCount++;
        } finally {
            writeLock.unlock();
        }
        //通知
        synchronized (notifier) {
            notifier.notifyAll();
        }
        //唤醒电梯线程
        elevator.wakeUp();
    }

    public boolean hasRequestAt(int floor) {
        readLock.lock();
        try {
            HashSet<Person> set = requestMap.get(floor);
            return set != null && !set.isEmpty();
        } finally {
            readLock.unlock();
        }
    }

    public ArrayList<Person> drainAllRequests() {
        ArrayList<Person> people = new ArrayList<>();
        writeLock.lock();
        try {
            for (HashSet<Person> set : requestMap.values()) {
                people.addAll(set);
            }
            requestMap.clear();
            assignedCount = 0;
            return people;
        } finally {
            writeLock.unlock();
        }
    }

    public ArrayList<Person> letInRequests(int curFloor, int curWeight, boolean direction,
                                           HashMap<Integer, HashSet<Person>> destMap) {
        ArrayList<Person> letIn = new ArrayList<>();
        writeLock.lock();
        try {
            newWeight = curWeight;
            HashSet<Person> set = requestMap.get(curFloor);
            if (set == null || set.isEmpty()) {
                return letIn;
            }

            Iterator<Person> iterator = set.iterator();
            while (iterator.hasNext()) {
                Person person = iterator.next();
                if (person.isDirection() == direction) {
                    int weight = person.getWeight();
                    if (newWeight + weight <= 400) {
                        newWeight += weight;
                        letIn.add(person);
                        destMap.computeIfAbsent(
                                person.getToFloor(), k -> new HashSet<>()).add(person);
                        iterator.remove(); // 关键：在锁内直接删
                        assignedCount--;
                    }
                }
            }

            if (set.isEmpty()) {
                requestMap.remove(curFloor);
            }
            return letIn;
        } finally {
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

    public void setEndFlag(boolean endFlag,ElevatorThread elevator) {
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
        //唤醒电梯线程
        elevator.wakeUp();
    }

    public boolean isEnd() {
        readLock.lock();
        try {
            return endFlag;
        } finally {
            readLock.unlock();
        }
    }

    public boolean hasPickableRequestAt(int curFloor, int curWeight, boolean direction) {
        readLock.lock();
        try {
            HashSet<Person> set = requestMap.get(curFloor);
            if (set == null || set.isEmpty()) {
                return false;
            }
            for (Person person : set) {
                if (person instanceof Passenger
                        && person.isDirection() == direction
                        && curWeight + person.getWeight() <= 400) {
                    return true;
                }
            }
            return false;
        } finally {
            readLock.unlock();
        }
    }

    public int getAssignedCount() {
        readLock.lock();
        try {
            return assignedCount;
        }
        finally {
            readLock.unlock();
        }
    }
}
