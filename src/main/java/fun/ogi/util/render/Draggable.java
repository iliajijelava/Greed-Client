package fun.ogi.util.render;

public class Draggable {
    private float x, y, width, height;
    private boolean dragging;
    private float dragX, dragY;

    public Draggable(float x, float y, float width, float height) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
    }

    public void onDraw(int mouseX, int mouseY, float screenWidth, float screenHeight) {
        if (dragging) {
            x = mouseX - dragX;
            y = mouseY - dragY;
        }        
        if (x < 0) x = 0;
        if (y < 0) y = 0;
        if (x + width > screenWidth) x = screenWidth - width;
        if (y + height > screenHeight) y = screenHeight - height;
    }

    public boolean onClick(double mouseX, double mouseY, int button) {
        if (button == 0 && isHovered(mouseX, mouseY)) {
            dragging = true;
            dragX = (float) (mouseX - x);
            dragY = (float) (mouseY - y);
            return true;
        }
        return false;
    }

    public void onRelease(int button) {
        if (button == 0) {
            dragging = false;

        }
    }

    public boolean isHovered(double mouseX, double mouseY) {
        return mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height;
    }

    public float getX() {
        return x;
    }

    public void setX(float x) {
        this.x = x;
    }

    public float getY() {
        return y;
    }

    public void setY(float y) {
        this.y = y;
    }

    public float getWidth() {
        return width;
    }

    public void setWidth(float width) {
        this.width = width;
    }

    public float getHeight() {
        return height;
    }

    public void setHeight(float height) {
        this.height = height;
    }

    public boolean isDragging() {
        return dragging;

    }
}

