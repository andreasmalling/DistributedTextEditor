public class RipEvent  extends MyTextEvent {
    private final boolean only2inChord;

    public RipEvent(boolean only2inChord) {
        super(0);
        this.only2inChord = only2inChord;
    }
    public boolean isOnly2inChord(){
        return only2inChord;
    }
}
