package buffer;

import producer.Person;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class RequestTable {
    private boolean endFlag = false;
    private final HashMap<Integer, HashSet<Person>> requestMap = new HashMap<>();
    private Integer newWeight = -1;

    private final ReentrantReadWriteLock rwLock = new ReentrantReadWriteLock();
    private final Lock readLock = rwLock.readLock();
    private final Lock writeLock = rwLock.writeLock();
    private final Object notifier = new Object();

    public void addRequest(Person person) {
        int floor = person.getFromFloor();
        writeLock.lock();
        try {
            //如果这一层没有集合，就新建一个HashSet
            HashSet<Person> set = requestMap.computeIfAbsent(floor, k -> new HashSet<>());
            // 然后把person加进去
            set.add(person);
        } finally {
            writeLock.unlock();
        }
        //通知
        synchronized (notifier) {
            notifier.notifyAll();
        }
    }

    public void removeRequest(Person person) {
        int floor = person.getFromFloor();
        writeLock.lock();
        try {
            HashSet<Person> set = requestMap.get(floor);
            if (set == null) {
                return;
            }
            set.remove(person);
            //如果没有请求了就顺手删除
            if (set.isEmpty()) {
                requestMap.remove(floor);
            }
        } finally {
            writeLock.unlock();
        }
    }

    //得到这一层的所有请求
    public HashSet<Person> getRequest(int floor) {
        readLock.lock();
        try {
            HashSet<Person> set = requestMap.get(floor);
            if (set == null) {
                return new HashSet<>();
            }
            else {
                return new HashSet<>(set);
            }
        } finally {
            readLock.unlock();
        }
    }

    //随便取一个请求
    public Person pollRequest() {
        writeLock.lock();
        try {
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
            return null;
        }
        finally {
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
            return endFlag;
        } finally {
            readLock.unlock();
        }
    }

    public void awaitNewRequest() throws InterruptedException {
        synchronized (notifier) {
            while (!endFlag && isEmpty()) {
                notifier.wait();
            }
        }
    }

    public Integer getNewWeight() {
        readLock.lock();
        try {
            return newWeight;
        } finally {
            readLock.unlock();
        }
    }
}
