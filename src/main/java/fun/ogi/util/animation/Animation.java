package fun.ogi.util.animation;

public class Animation {

    private float value;
    private float from;
    private float to;
    private long duration;
    private long startTime;
    private Easing easing;
    private boolean running;

    public Animation() {
        this.value = 0;
        this.running = false;
    }

    public Animation(long duration, Easing easing) {
        this.value = 0;
        this.duration = duration;
        this.easing = easing;
        this.running = false;
    }

    public void start(float from, float to, long duration, Easing easing) {
        this.from = from;
        this.to = to;
        this.value = from;
        this.duration = duration;
        this.startTime = System.currentTimeMillis();
        this.easing = easing;
        this.running = true;
    }

    public void start(float from, float to) {
        this.from = from;
        this.to = to;
        this.value = from;
        this.startTime = System.currentTimeMillis();
        this.running = true;
    }

    public void update() {
        if (!running) return;
        float elapsed = System.currentTimeMillis() - startTime;
        float t = Math.min(1f, elapsed / (float) duration);
        float eased = (float) easing.ease(t);
        value = from + (to - from) * eased;
        if (t >= 1f) {
            value = to;
            running = false;
        }
    }

    public boolean isRunning() {
        return running;
    }

    public boolean isFinished() {
        return !running;
    }

    public float getValue() {
        return value;
    }

    public float getProgress() {
        if (to == from) return 1f;
        return (value - from) / (to - from);
    }

    public void setValue(float v) {
        this.value = v;
        running = false;
    }

    public void setDuration(long duration) {
        this.duration = duration;
    }

    public void setEasing(Easing easing) {
        this.easing = easing;
    }

    public void stop() {
        this.running = false;
    }
}

