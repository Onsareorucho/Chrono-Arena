package server.logic;

import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;

// import javax.swing.Action;

// import shared.protocol.ActionMessage; // uncomment when P4 is ready

public class ActionQueue {
 private ConcurrentLinkedQueue<Object> queue; //temporary until p4 is ready, replace Object with ActionMessage when it is

    public ActionQueue() {
        this.queue = new ConcurrentLinkedQueue<>();
    }

    public void addAction(Object action) {
        queue.add(action);
    }

    public void drainTo(List<Object> target) {
    Object action;
    while ((action = queue.poll()) != null) {
        target.add(action);
    }
}

    public Object pollAction() {
        return queue.poll();
    }

    public boolean isEmpty() {
        return queue.isEmpty();
    }
}
