package server.logic;

import shared.GameMessage;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;

public class ActionQueue {
    private ConcurrentLinkedQueue<GameMessage> queue;

    public ActionQueue() {
        this.queue = new ConcurrentLinkedQueue<>();
    }

    public void addAction(GameMessage action) {
        queue.add(action);
    }

    public void drainTo(List<GameMessage> target) {
        GameMessage action;
        while ((action = queue.poll()) != null) {
            target.add(action);
        }
    }

    public GameMessage pollAction() {
        return queue.poll();
    }

    public boolean isEmpty() {
        return queue.isEmpty();
    }
}
