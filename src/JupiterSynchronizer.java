import java.util.ArrayList;

/**
 * Created by Kresten on 12-05-2015.
 */
public class JupiterSynchronizer {

    private ArrayList<MyTextEvent> outgoing = new ArrayList<>();
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

        //Receive(msg)
        //Discard acknowledged messages
        for(MyTextEvent m : outgoing){
            if(m.getLocalTime() < mte.getOtherTime()){
                outgoing.remove(m);
            }
        }
        //ASSERT msg.myMsgs == otherMsgs
        for (int i = 0; i < outgoing.size(); i++){
            //Transform new message and the ones in the queue
            //{msg, outgoing[i]} = xform(msg, outgoing[i]);
            MyTextEvent[] xformed = Transformer.xform(mte, outgoing.get(i));
            mte = xformed[0];
            outgoing.set(i, xformed[1]);
        }
        otherMsgs++;
        return mte;
    }

}
