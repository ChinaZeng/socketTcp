package com.zzw.socketdemo.socket;

import java.io.UnsupportedEncodingException;
import java.net.Socket;
import java.util.Arrays;

public class Packet {
    enum TYPE {
        SEND,
        RECIVER
    }

    private String id = UUIDUtils.creatUUID();
    private TYPE type;
    private final Socket socket;
    public byte[] data = new byte[0];
    public byte cmd = CMD.EMPTY;
    public byte flog = CMD.EMPTY;


    public int size() {
        //xxxx(内容长度)+cmd+flog+data    //4+1+1+data.length
        return 4 + 1 + 1 + data.length;
    }

    public int contentSize() {
        //cmd+flog+data
        return size() - 4;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getId() {
        return id;
    }

    public boolean isSend() {
        return type == TYPE.SEND;
    }

    public Packet(Socket socket, TYPE type) {
        this.socket = socket;
        this.type = type;
    }

    //xxxx(内容长度)+cmd+flog+data    //4+1+1+data.length
    public byte[] realData() {
        int size = size();
        byte[] realData = new byte[size];

        //lenByte
        byte[] sizeByte = ByteUtils.getBytes(contentSize());
        //len
        System.arraycopy(sizeByte, 0, realData, 0, sizeByte.length);
        //cmd
        realData[4] = cmd;
        //flog
        realData[5] = flog;
        //realData
        System.arraycopy(data, 0, realData, 6, data.length);
        return realData;
    }

    public void putByteArray(byte[] content) {
        this.data = ByteUtils.mergerByte(data, content);
    }

    public String key() {
        return KeyUtils.getKey(socket);
    }

    public String string() {
        try {
            return new String(data, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
            return null;
        }
    }

    public String ip() {
        return socket.getInetAddress().toString();
    }

    public int port() {
        return socket.getPort();
    }
}