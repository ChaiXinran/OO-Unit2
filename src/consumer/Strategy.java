package consumer;

import buffer.AdviceType;
import buffer.RequestTable;
import producer.Person;

import java.util.HashMap;
import java.util.HashSet;

public class Strategy {

    private final RequestTable requestTable;

    public Strategy(RequestTable requestTable) {
        this.requestTable = requestTable;
    }

    public AdviceType getAdvice(int curFloor, int curWeight, boolean direction,
                                HashMap<Integer,HashSet<Person>> destMap,boolean normal) {
        if (!normal) {
            return AdviceType.MAINTAIN;
        }
        if (canOpenForOut(curFloor, destMap) ||
                canOpenForIn(curFloor, curWeight, direction)) {
            return AdviceType.OPEN;
        }
        //如果电梯里有人
        if (curWeight != 0) {
            //有人的目的地在电梯行动方向上
            return AdviceType.MOVE;
        }
        //如果电梯里没有人
        else {
            //如果请求队列里没有人
            if (requestTable.isEmpty()) {
                //如果输入结束
                if (requestTable.isEnd()) {
                    return AdviceType.OVER;
                }
                else {
                    return AdviceType.WAIT;
                }
            }
            //如果请求队列里有人
            else if (hasReqInOriginDirection(curFloor,direction)) {
                //如果请求发出地在电梯前方
                return AdviceType.MOVE;
            }
            else {
                //电梯转向，但不移动
                return AdviceType.REVERSE;
            }
        }
    }

    //判断现在是否可以把一些人放下来
    private boolean canOpenForOut(int curFloor, HashMap<Integer, HashSet<Person>> destMap) {
        //判断是否有人需要在这一层下电梯
        return destMap.containsKey(curFloor);
    }

    //判断现在是否可以上人
    //判断现在是否可以上人
    private boolean canOpenForIn(int curFloor, int curWeight, boolean direction) {
        //判断体重是否超标
        if (curWeight <= 350) {
            //得到在该层中需要上电梯的所有人
            HashSet<Person> people = requestTable.getRequest(curFloor);
            //判断这一层是否有人需要上电梯
            for (Person person : people) {
                if (person.isDirection() == direction
                        && curWeight + person.getWeight() <= 400) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean hasReqInOriginDirection(int curFloor, boolean direction) {
        //如果有请求发出在电梯前方
        if (direction) {
            //得到在该层前方中需要上电梯的所有人
            for (int i = curFloor + 1;i <= 11; i++) {
                HashSet<Person> people = requestTable.getRequest(i);
                if (!people.isEmpty()) {
                    return true;
                }
            }
        }
        else {
            for (int i = curFloor - 1; i >= 1; i--) {
                HashSet<Person> people = requestTable.getRequest(i);
                if (!people.isEmpty()) {
                    return true;
                }
            }
        }
        //判断这一层是否有人需要上电梯
        return false;
    }
}
