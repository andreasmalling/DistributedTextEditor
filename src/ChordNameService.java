import java.net.InetSocketAddress;

/**
 *
 * Interface for the Chord naming service. Each peer is named by an IP
 * address and a port, technically an InetSocketAddress. Each
 * InetSocketAddress is mapped into a key, an unsigned 31-bit integer,
 * by taking hash=InetSocketAddress.hashCode() and letting key =
 * abs(hash*1073741651 % 2147483647), where abs() is absolute value.
 * This key is used to arrange all InetSocketAddress's into a ring
 * with the current peers being responsible for each their interval of
 * the key space, according to the Chord network topology. The
 * interface allows to enter and leave a chord group and allows to
 * find the name of a peer currently responsible for a given key. 
 */


public interface ChordNameService extends Runnable {

    /**
     * Compute the key of a given name.  Returns a positive 31-bit
     * integer, hased as to be "random looking" even for similar
     * names.
     */
    public int keyOfName(InetSocketAddress name);

    /**
     * Used by the first group member.  Specifies the port on
     * which this founding peer is listening for new peers to join
     * or leave.  The name of the foudning peer is its local IP
     * address and the given port. Its key is derived from the
     * name using the method described above.
     */
    public void createGroup();

    /**
     * Used to join a Chord group. This takes place by contacting
     * one of the existing peers of the Chord group.  The new peer
     * has the name specified by the local IP address and the
     * given port. The key of the new peer is derived from its
     * name using the method described above.
     *
     * @param knownPeer The IP address and port of the known peer.
     */
    public void joinGroup(InetSocketAddress knownPeer);

    /**
     * Returns the name of this peer. May only be called after a
     * group has been formed or joined.
     */
    public InetSocketAddress getChordName();

    /**
     * Makes this instance of ChordNameService leave the peer
     * group. The other peers should be informed of this and the
     * Chord network updated appropriately.
     */
    public void leaveGroup();

    /**
     * Starts the thread which manages this peers participation in
     * the Chord network.
     */
    public void run();

}