package no.statnett.larm.hvdc.rpt.chat;

import java.io.Closeable;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Kan brukes til å motta UDP-meldinger.
 */
public final class ChatReceiver implements Closeable {

    public static final int MAX_PACKAGE_SIZE = 1024;
    private static final Logger logger = LoggerFactory.getLogger(ChatReceiver.class);

    // Which port should we listen to
    private final int port;
    // Which address
    private final String group;
    private final MulticastSocket multicastSocket;
    private final List<StringMessageListener> listeners = new ArrayList<>();

    public ChatReceiver() throws IOException {
        this(50000, "225.4.5.6");
    }

    public ChatReceiver(int port, String group) throws IOException {
        this.port = port;
        this.group = group;
        multicastSocket = new MulticastSocket(this.port);
    }

    public void receive(int maxMessages) throws IOException {
        // join the multicast group
        multicastSocket.joinGroup(InetAddress.getByName(group));
        // Now the socket is set up and we are ready to receive packets
        new Thread(new Runnable() {
            int count = 0;
            @Override public void run() {

                while (count < maxMessages) {
                    byte[] buf = new byte[MAX_PACKAGE_SIZE];
                    DatagramPacket pack = new DatagramPacket(buf, buf.length);
                    try {
                        multicastSocket.receive(pack);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                    InetAddress address = pack.getAddress();
                    logger.debug("Received data from: {}:{} with length: {}", address == null ? null : address.toString(), pack.getPort(), pack.getLength());

                    String utf8 = new String(pack.getData(), 0, pack.getLength(), Charset.forName("UTF8"));
                    for (StringMessageListener listener : listeners) {
                        listener.receive(utf8);
                    }
                    count++;
                }
            }
        }).start();
    }

    public void registerListener(StringMessageListener stringMessageListener) {
        listeners.add(stringMessageListener);
    }

    @Override public void close() throws IOException {
        // And when we have finished receiving data leave the multicast group and
        // close the socket
        System.out.println("Leaving you");
        multicastSocket.leaveGroup(InetAddress.getByName(group));
        multicastSocket.close();
    }

    public static void main(String[] args) throws Exception {
        ChatReceiver receiver = new ChatReceiver(5000, "225.4.5.6");
        try {
            receiver.registerListener(new StringMessageListener() {
                @Override public void receive(String utf8Message) {
                    logger.info("Received message '{}'", utf8Message);
                }
            });
            receiver.receive(Integer.MAX_VALUE);
            while (true) {Thread.yield();}
        } finally {
            receiver.close();
        }
    }
}
