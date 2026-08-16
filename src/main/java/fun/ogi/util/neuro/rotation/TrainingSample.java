package fun.ogi.util.neuro.rotation;

public class TrainingSample {
    private float[] input;
    private float[] output;

    public TrainingSample() {
    }

    public TrainingSample(float[] input, float[] output) {
        this.input = input;
        this.output = output;
    }

    public float[] getInput() {
        return input;
    }

    public void setInput(float[] input) {
        this.input = input;
    }

    public float[] getOutput() {
        return output;
    }

    public void setOutput(float[] output) {
        this.output = output;
    }
}

