import java.util.ArrayList;
import java.util.Iterator;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Created by Kresten on 12-05-2015.
 */
public class JupiterSynchronizer {

    private CopyOnWriteArrayList<MyTextEvent> outgoing = new CopyOnWriteArrayList<>();
    private int myMsgs = 0; /** number of messages generated */
    private int otherMsgs = 0; /** number of messages received */

    public synchronized MyTextEvent generate(MyTextEvent mte){
        //Generate(op)
        mte.setLocalTime(myMsgs);
        mte.setOtherTime(otherMsgs);
        outgoing.add(mte);
        myMsgs++;
        return mte;
    }

    public synchronized MyTextEvent receive(MyTextEvent mte){
        Iterator<MyTextEvent> iterator = outgoing.iterator();
        ArrayList<MyTextEvent> deletionArray = new ArrayList<>();
        //Receive(msg)
        //Discard acknowledged messages
        while (iterator.hasNext()){
            MyTextEvent m = iterator.next();
            if (m.getLocalTime() < mte.getOtherTime()){
                deletionArray.add(m);
            }
        }
        outgoing.removeAll(deletionArray);
        //ASSERT msg.myMsgs == otherMsgs
        int i = 0;
        while (iterator.hasNext()){
            //Transform new message and the ones in the queue
            //{msg, outgoing[i]} = xform(msg, outgoing[i]);
            MyTextEvent[] xformed = Transformer.xform(mte, iterator.next());
            mte = xformed[0];
            xformed[1].setLocalTime(iterator.next().getLocalTime());
            xformed[1].setOtherTime(iterator.next().getOtherTime());
            outgoing.set(i, xformed[1]);
            i++;
        }
        otherMsgs++;
        return mte;
    }

    public void clear(){
        outgoing.clear();
    }

}
