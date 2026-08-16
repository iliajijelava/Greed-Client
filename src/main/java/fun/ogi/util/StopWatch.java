package fun.ogi.util;

public class StopWatch {
    private long lastTime = System.currentTimeMillis();

    public void reset() {
        lastTime = System.currentTimeMillis();
    }

    public boolean finished(long delay) {
        return System.currentTimeMillis() - lastTime >= delay;
    }

    public boolean hasTimePassed(long ms) {
        return finished(ms);
    }

    public boolean every(long delay) {
        if (finished(delay)) {
            reset();
            return true;
        }
        return false;
    }

    public long elapsedTime() {
        return System.currentTimeMillis() - lastTime;
    }

    public long getElapsedTime() {
        return elapsedTime();
    }

    public void setTime(long time) {
        this.lastTime = time;
    }
}

