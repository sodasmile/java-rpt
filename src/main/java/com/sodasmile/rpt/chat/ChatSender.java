package no.statnett.larm.hvdc.rpt.chat;

import java.io.Closeable;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.nio.charset.Charset;
import java.util.Date;

/**
 * Kan benyttes til å sende en UDP-melding.
 */
public class ChatSender implements Closeable {

    private final int port;
    private final String group;
    /**
     * Which ttl.
     * 0 is restricted to the same host
     * 1 is restricted to the same subnet
     * 32 is restricted to the same site
     * 64 is restricted to the same region
     * 128 is restricted to the same continent
     * 255 is unrestricted
     **/
    private int ttl = 255;
    private final MulticastSocket s;

    public ChatSender() throws IOException {
        this(50000, "225.4.5.6");
    }

    public ChatSender(int port, String group) throws IOException {
        this.port = port;
        this.group = group;
        // Create the socket but we don't bind it as we are only going to send data
        s = new MulticastSocket();
    }

    public void send(String data) throws IOException {
        send(data.getBytes(Charset.forName("UTF8")));
    }

    public void send(byte[] data) throws IOException {
        // Note that we don't have to join the multicast group if we are only
        // sending data and not receiving

        // Create a DatagramPacket
        DatagramPacket pack = new DatagramPacket(data, data.length, InetAddress.getByName(group), port);
        // Do a send. Note that send takes a byte for the ttl and not an int.
        int oldttl = s.getTimeToLive();
        s.setTimeToLive(ttl);
        s.send(pack);
        s.setTimeToLive(oldttl);
    }

    public ChatSender withTimeToLive(int ttl) {
        this.ttl = ttl;
        return this;
    }

    public static void main(String[] args) throws IOException {
        System.out.println(new Date(1447061545633L));
        // Fill the buffer with some data
        byte[] buf = new byte[10];
        for (int i = 0; i < buf.length; i++) {
            buf[i] = (byte) i;
        }

        try (ChatSender chatSender = new ChatSender(5000, "225.4.5.6")) {
            chatSender.send(buf);
            chatSender.send("Hei hei alle sammen!");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override public void close() {
        // And when we have finished sending data close the socket
        s.close();
    }
}
