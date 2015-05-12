/**
 * Created by Kresten Axelsen on 06-05-2015.
 */
public class Transformer {
    private static MyTextEvent[] pair = new MyTextEvent[1];
    public static MyTextEvent[] xform(MyTextEvent received, MyTextEvent local) {
        pair[0] = received;
        pair[1] = local;
        if(received instanceof TextInsertEvent && local instanceof TextInsertEvent){
            TextInsertEvent receivedIns = (TextInsertEvent) received;
            TextInsertEvent localIns = (TextInsertEvent) local;
            pair = insertInsert(receivedIns, localIns);
        }
        else if(received instanceof TextRemoveEvent && local instanceof TextRemoveEvent){
            TextRemoveEvent receivedRem = (TextRemoveEvent) received;
            TextRemoveEvent localRem = (TextRemoveEvent) local;
            pair = removeRemove(receivedRem, localRem);
        }
        else if(received instanceof TextInsertEvent && local instanceof TextRemoveEvent){
            TextInsertEvent receivedIns = (TextInsertEvent) received;
            TextRemoveEvent localRem = (TextRemoveEvent) local;
            pair = insertRemove(receivedIns, localRem);
        }
        else if(received instanceof TextRemoveEvent && local instanceof TextInsertEvent){
            TextRemoveEvent receivedRem = (TextRemoveEvent) received;
            TextInsertEvent localIns = (TextInsertEvent) local;
            pair = removeInsert(receivedRem, localIns);
        }
        return pair;
    }

    private static MyTextEvent[] insertInsert(TextInsertEvent receivedIns, TextInsertEvent localIns){
        if (receivedIns.getOffset() > localIns.getOffset() ) {
            receivedIns = new TextInsertEvent(receivedIns.getOffset() + localIns.getText().length(), receivedIns.getText());
        }
        else if (receivedIns.getOffset() < localIns.getOffset() ) {
            localIns = new TextInsertEvent(localIns.getOffset() + receivedIns.getText().length(), localIns.getText());
        }

        else{ //receivedIns.getOffset() == localIns.getOffset()
            if(receivedIns.getId()<localIns.getId()){
                localIns = new TextInsertEvent(localIns.getOffset() + receivedIns.getText().length(), localIns.getText());
            }
            else{
                receivedIns = new TextInsertEvent(receivedIns.getOffset() + localIns.getText().length(), receivedIns.getText());
            }
        }
        pair[0] = receivedIns;
        pair[1] = localIns;
        return pair;
    }

    private static MyTextEvent[] removeRemove(TextRemoveEvent receivedRem, TextRemoveEvent localRem){
        if (receivedRem.getOffset() > localRem.getOffset() ) {
            if(localRem.getOffset()+localRem.getLength() >= receivedRem.getOffset()){
                if(localRem.getOffset()+localRem.getLength() >= receivedRem.getOffset()+receivedRem.getLength()){
                    receivedRem = new TextRemoveEvent(0,0);
                }
                else localRem = new TextRemoveEvent(localRem.getOffset(),receivedRem.getOffset()+receivedRem.getLength()-localRem.getOffset());
            }
            else receivedRem = new TextRemoveEvent(receivedRem.getOffset() - localRem.getLength(), receivedRem.getLength());
        }
        else if (receivedRem.getOffset() < localRem.getOffset() ) {
            if(receivedRem.getOffset()+receivedRem.getLength() >= localRem.getOffset()){
                if(receivedRem.getOffset()+receivedRem.getLength() >= localRem.getOffset()+localRem.getLength()){
                    localRem = new TextRemoveEvent(0,0);
                }
                else receivedRem = new TextRemoveEvent(receivedRem.getOffset(),localRem.getOffset()+localRem.getLength()-receivedRem.getOffset());
            }
            else localRem = new TextRemoveEvent(localRem.getOffset() - receivedRem.getLength(), localRem.getLength());
        }
        else{
            if(receivedRem.getOffset() + receivedRem.getLength() >= localRem.getOffset()+localRem.getLength()){
                localRem = new TextRemoveEvent(0,0);
            }
            else receivedRem = new TextRemoveEvent(0,0);
        }
        pair[0] = receivedRem;
        pair[1] = localRem;
        return pair;
    }

    private static MyTextEvent[] insertRemove(TextInsertEvent receivedIns, TextRemoveEvent localRem){
        if (receivedIns.getOffset() > localRem.getOffset() ) {
            receivedIns = new TextInsertEvent(receivedIns.getOffset() - localRem.getLength(), receivedIns.getText());
        }
        else if (receivedIns.getOffset() <= localRem.getOffset()) {
            localRem = new TextRemoveEvent(localRem.getOffset() + receivedIns.getText().length(), localRem.getLength());
        }
        pair[0] = receivedIns;
        pair[1] = localRem;
        return pair;
    }

    private static MyTextEvent[] removeInsert(TextRemoveEvent receivedRem, TextInsertEvent localIns){
        if (receivedRem.getOffset() >= localIns.getOffset() ) {
            receivedRem = new TextRemoveEvent(receivedRem.getOffset() + localIns.getText().length(), receivedRem.getLength());
        }
        else if (receivedRem.getOffset() < localIns.getOffset() ) {
            localIns = new TextInsertEvent(localIns.getOffset() - receivedRem.getLength(), localIns.getText());
        }
        pair[0] = receivedRem;
        pair[1] = localIns;
        return pair;
    }

}
