package Buffer;

import com.oocourse.elevator1.PersonRequest;

import java.util.HashMap;
import java.util.HashSet;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class RequestTable {
    private boolean endFlag = false;
    private final HashMap<Integer, HashSet<PersonRequest>> requestMap = new HashMap<>();

    private final ReentrantReadWriteLock rwLock = new ReentrantReadWriteLock();
    private final Lock readLock = rwLock.readLock();
    private final Lock writeLock = rwLock.writeLock();
    private final Object notifier = new Object();

    public void addRequest(PersonRequest person) {
        String floorStr = person.getFromFloor();
        int floor = parseFloor(floorStr);
        writeLock.lock();
        try {
            //如果这一层没有集合，就新建一个HashSet
            HashSet<PersonRequest> set = requestMap.computeIfAbsent(floor, k -> new HashSet<>());
            // 然后把person加进去
            set.add(person);
        }finally {
            writeLock.unlock();
        }
        //通知
        synchronized (notifier){
            notifier.notifyAll();
        }
    }

    public void removeRequest(PersonRequest person) {
        String floorStr = person.getFromFloor();
        int floor = parseFloor(floorStr);
        writeLock.lock();
        try {
            HashSet<PersonRequest> set = requestMap.get(floor);
            if (set == null) {
                return;
            }
            set.remove(person);
            //如果没有请求了就顺手删除
            if (set.isEmpty()) {
                requestMap.remove(floor);
            }
        }finally {
            writeLock.unlock();
        }
    }

    //得到这一层的所有请求
    public HashSet<PersonRequest> getRequest(int floor) {
        readLock.lock();
        try {
            HashSet<PersonRequest> set = requestMap.get(floor);
            if (set == null) {
                return new HashSet<>();
            }
            else{
                return new HashSet<>(set);
            }
        }finally {
            readLock.unlock();
        }
    }

    //随便取一个请求
    public PersonRequest pollRequest() {
        writeLock.lock();
        try {
            for(Integer floor : requestMap.keySet()) {
                HashSet<PersonRequest> set = requestMap.get(floor);
                if(!set.isEmpty()) {
                    PersonRequest person = set.iterator().next();
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

    public boolean isEmpty() {
        readLock.lock();
        try {
            return requestMap.isEmpty();
        }finally {
            readLock.unlock();
        }
    }

    public void setEndFlag(boolean endFlag) {
        writeLock.lock();
        try {
            this.endFlag = endFlag;
        }finally {
            writeLock.unlock();
        }
        //通知
        synchronized (notifier){
            notifier.notifyAll();
        }
    }

    public boolean isEnd() {
        readLock.lock();
        try {
            return endFlag;
        }finally {
            readLock.unlock();
        }
    }

    public void awaitNewRequest() throws InterruptedException {
        synchronized (notifier) {
            while (!endFlag && !isEmpty()) {
                notifier.wait();
            }
        }
    }

    public int parseFloor(String s) {
        int num = Integer.parseInt(s.substring(1));
        if (s.charAt(0) == 'B') {
            return 5 - num;
        } else { // F
            return num + 4;
        }
    }
}
