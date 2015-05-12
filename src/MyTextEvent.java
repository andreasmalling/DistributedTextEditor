import java.io.Serializable;

/**
 *
 * @author Jesper Buus Nielsen
 *
 */
public class MyTextEvent implements Serializable{
    MyTextEvent(int offset) {
        this.offset = offset;
    }

    private int id;
    private int offset;
    private int localTime;
    private int otherTime;
    int getOffset() { return offset; }

    public int getLocalTime() {
        return localTime;
    }

    public void setLocalTime(int localTime) {
        this.localTime = localTime;
    }

    public int getOtherTime() {
        return otherTime;
    }

    public void setOtherTime(int otherTime) {
        this.otherTime = otherTime;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getId() {
        return id;
    }
}