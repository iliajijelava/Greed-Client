package fun.ogi.util.animation;

public enum Easing {
    LINEAR(t -> t),
    QUAD_IN(t -> t * t),
    QUAD_OUT(t -> t * (2 - t)),
    QUAD_IN_OUT(t -> t < 0.5 ? 2 * t * t : -1 + (4 - 2 * t) * t),
    CUBIC_IN(t -> t * t * t),
    CUBIC_OUT(t -> --t * t * t + 1),
    CUBIC_IN_OUT(t -> t < 0.5 ? 4 * t * t * t : (t - 1) * (2 * t - 2) * (2 * t - 2) + 1),
    QUART_IN(t -> t * t * t * t),
    QUART_OUT(t -> 1 - (--t) * t * t * t),
    QUART_IN_OUT(t -> t < 0.5 ? 8 * t * t * t * t : 1 - 8 * (--t) * t * t * t),
    QUINT_IN(t -> t * t * t * t * t),
    QUINT_OUT(t -> 1 + (--t) * t * t * t * t),
    QUINT_IN_OUT(t -> t < 0.5 ? 16 * t * t * t * t * t : 1 + 16 * (--t) * t * t * t * t),
    EXPO_IN(t -> Math.pow(2, 10 * (t - 1))),
    EXPO_OUT(t -> 1 - Math.pow(2, -10 * t)),
    EXPO_IN_OUT(t -> (t *= 2) < 1 ? 0.5 * Math.pow(2, 10 * (t - 1)) : 0.5 * (2 - Math.pow(2, -10 * (t - 1)))),
    CIRC_IN(t -> 1 - Math.sqrt(1 - t * t)),
    CIRC_OUT(t -> Math.sqrt(1 - (t - 1) * (t - 1))),
    CIRC_IN_OUT(t -> (t *= 2) < 1 ? -0.5 * (Math.sqrt(1 - t * t) - 1) : 0.5 * (Math.sqrt(1 - (t - 2) * (t - 2)) + 1)),
    BACK_IN(t -> t * t * (2.70158 * t - 1.70158)),
    BACK_OUT(t -> --t * t * (2.70158 * t + 1.70158) + 1),
    BACK_IN_OUT(t -> (t *= 2) < 1 ? 0.5 * (t * t * (3.5949095 * t - 2.5949095)) : 0.5 * ((t -= 2) * t * (3.5949095 * t + 2.5949095) + 2)),
    ELASTIC_IN(t -> t == 0 ? 0 : t == 1 ? 1 : -Math.pow(2, 10 * t - 10) * Math.sin((t * 10 - 10.75) * (2 * Math.PI) / 3)),
    ELASTIC_OUT(t -> t == 0 ? 0 : t == 1 ? 1 : Math.pow(2, -10 * t) * Math.sin((t * 10 - 0.75) * (2 * Math.PI) / 3) + 1),
    ELASTIC_IN_OUT(t -> t == 0 ? 0 : t == 1 ? 1 : t < 0.5 ? -0.5 * Math.pow(2, 20 * t - 10) * Math.sin((20 * t - 11.125) * (2 * Math.PI) / 4.5) : 0.5 * Math.pow(2, -20 * t + 10) * Math.sin((20 * t - 11.125) * (2 * Math.PI) / 4.5) + 1);

    private final Function function;

    Easing(Function function) {
        this.function = function;
    }

    public double ease(double t) {
        return function.ease(t);
    }

    public interface Function {
        double ease(double t);
    }
}

