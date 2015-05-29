/**
 * Created by Kresten Axelsen on 29-05-2015.
 */
public class AllTextEvent extends MyTextEvent {
    private String text;

    public AllTextEvent(String text) {
        super(0);
        this.text = text;
    }
    public String getText() { return text; }
}
