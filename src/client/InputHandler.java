package client;

import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;

public class InputHandler implements KeyListener {

    private GameClient client;

    private boolean upPressed = false;
    private boolean downPressed = false;
    private boolean leftPressed = false;
    private boolean rightPressed = false;

    public InputHandler(GameClient client) {
        this.client = client;
    }

    @Override
    public void keyPressed(KeyEvent e) {
        int key = e.getKeyCode();

        switch(key) {
            case KeyEvent.VK_W:
            case KeyEvent.VK_UP:
                if(!upPressed) {
                    upPressed = true;
                    sendMovementUpdate();
                }
                break;

            case KeyEvent.VK_S:
            case KeyEvent.VK_DOWN:
                if(!downPressed) {
                    downPressed = true;
                    sendMovementUpdate();
                }
                break;

            case KeyEvent.VK_A:
            case KeyEvent.VK_LEFT:
                if(!leftPressed) {
                    leftPressed = true;
                    sendMovementUpdate();
                }
                break;

            case KeyEvent.VK_D:
            case KeyEvent.VK_RIGHT:
                if(!rightPressed) {
                    rightPressed = true;
                    sendMovementUpdate();
                }
                break;

            case KeyEvent.VK_SPACE:
                client.sendAction("FREEZE_RAY", getLastDirection());
                break;

            case KeyEvent.VK_E:
                client.sendAction("USE_POWERUP", "");
                break;
        }
    }

    @Override
    public void keyReleased(KeyEvent e) {
        int key = e.getKeyCode();

        switch(key) {
            case KeyEvent.VK_W:
            case KeyEvent.VK_UP:
                upPressed = false;
                sendMovementUpdate();
                break;

            case KeyEvent.VK_S:
            case KeyEvent.VK_DOWN:
                downPressed = false;
                sendMovementUpdate();
                break;

            case KeyEvent.VK_A:
            case KeyEvent.VK_LEFT:
                leftPressed = false;
                sendMovementUpdate();
                break;

            case KeyEvent.VK_D:
            case KeyEvent.VK_RIGHT:
                rightPressed = false;
                sendMovementUpdate();
                break;
        }
    }

    @Override
    public void keyTyped(KeyEvent e) {
        // not used
    }

    private void sendMovementUpdate() {
        int dx = 0;
        int dy = 0;

        if (leftPressed) dx -= 1;
        if (rightPressed) dx += 1;
        if (upPressed) dy -= 1;
        if (downPressed) dy += 1;

        client.sendMovement(dx, dy);
    }

    private String getLastDirection() {
        
        if (leftPressed) return "LEFT";
        if (rightPressed) return "RIGHT";
        if (upPressed) return "UP";
        if (downPressed) return "DOWN";

        return "RIGHT";
    }

    public boolean isUpPressed() { return upPressed; }
    public boolean isDownPressed() { return downPressed; }
    public boolean isLeftPressed() { return leftPressed; }
    public boolean isRightPressed() { return rightPressed; }

    public int getDirectionX() {
        int dx = 0;
        if (leftPressed) dx -= 1;
        if (rightPressed) dx += 1;
        return dx;
    }

    public int getDirectionY() {
        int dy = 0;
        if (upPressed) dy += 1;
        if (downPressed) dy -= 1;
        return dy;
    }
}