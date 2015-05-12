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
        //Receive(msg)
        //Discard acknowledged messages
        while (iterator.hasNext()){
            if (iterator.next().getLocalTime() < mte.getOtherTime()){
                iterator.remove();
            }
        }
        //ASSERT msg.myMsgs == otherMsgs
        int i = 0;
        while (iterator.hasNext()){
            //Transform new message and the ones in the queue
            //{msg, outgoing[i]} = xform(msg, outgoing[i]);
            MyTextEvent[] xformed = Transformer.xform(mte, iterator.next());
            mte = xformed[0];
            outgoing.set(i, xformed[1]);
            i++;
        }
        otherMsgs++;
        return mte;
    }

}
