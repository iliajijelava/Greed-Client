package fun.ogi.events.render;


import fun.ogi.events.EventCancellable;

public class AspectRatioEvent extends EventCancellable {
    float ratio;
    public float getRatio(){
        return ratio;
    }
    public void setRatio(float value){
        this.ratio = value;
    }
}

