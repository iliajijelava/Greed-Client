package fun.ogi.events;

public class
EventMouse {
    private final double mouseX, mouseY;
    private final int button;
    private final int action;

    public EventMouse(double mouseX, double mouseY, int button, int action) {
        this.mouseX = mouseX;
        this.mouseY = mouseY;
        this.button = button;
        this.action = action;
    }

    public double getMouseX() {
        return mouseX;
    }

    public double getMouseY() {
        return mouseY;
    }

    public int getButton() {
        return button;
    }

    public int getAction() {
        return action;

    }
}