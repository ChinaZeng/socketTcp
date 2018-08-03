package com.zzw.socketdemo.socket;

import android.os.SystemClock;
import android.support.annotation.NonNull;

import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class SocketThread extends Thread {

    private LinkedBlockingQueue<Packet> packetsQueue = new LinkedBlockingQueue<>();

    private volatile boolean flog = true;
    public final Socket socket;
    private InputStream inputStream;
    private OutputStream outputStream;

    private List<SocketMessageListener> listeners = new ArrayList<>();
    private SocketThreadStatusListener socketThreadStatusListener;


    public SocketThread(String name, Socket socket, SocketThreadStatusListener socketThreadStatusListener) {
        setName(name);
        MyLog.e(name, "SocketThread <init> name:" + name);
        this.socket = socket;
        this.socketThreadStatusListener = socketThreadStatusListener;
        if (socketThreadStatusListener != null) {
            socketThreadStatusListener.onStatusChange(this, STATUS.INIT);
        }

    }

    private int count;
    private SocketReader socketReader;

    @Override
    public void run() {
        if (socket == null)
            return;
        socketReader = new SocketReader();
        init();

        //获取数据流
        try {
            inputStream = socket.getInputStream();
            outputStream = socket.getOutputStream();
            if (socketThreadStatusListener != null) {
                socketThreadStatusListener.onStatusChange(this, STATUS.RUNNING);
            }

//            new SendDataThread().start();

            while (flog) {
                //5秒
                if (count > 5 * 5) {
                    break;
                }
                SystemClock.sleep(5);
                if (!socket.isConnected()) {
                    Thread.sleep(200);
                    count++;
                    continue;
                }
                count = 0;
                if (inputStream.available() > 0) {
                    Packet packet = socketReader.readData(socket, inputStream);
                    if (packet != null) {
                        onReciveMsg(packet);
                    }
                }

            }

        } catch (IOException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            MyLog.e("socket退出");
            close();
            if (socketThreadStatusListener != null) {
                socketThreadStatusListener.onStatusChange(this, STATUS.END);
            }
        }
    }

    protected void init() {
        addListener(SocketMessageListener.DEF);
    }


    private void close() {
        flog = false;

        closeCloseable(inputStream);
        closeCloseable(outputStream);
        inputStream = null;
        outputStream = null;
        try {
            if (socket != null)
                socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void closeCloseable(Closeable closeable) {
        if (closeable != null) {
            try {
                closeable.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public Socket getSocket() {
        return socket;
    }

    public void sendTextMsg(final String content) {
        Dispatcher.getInstance().executor.submit(new Runnable() {
            @Override
            public void run() {
                Packet packet = PacketHelper.getTextMsgPacket(socket);
                packet.data = ByteUtils.getBytes(content);
                sendQueue(packet);
            }
        });
    }


    private final static int FILE_BUFFER = 2048;

    public void sendFileMsg(final String path) {
        Dispatcher.getInstance().executor.submit(new Runnable() {
            @Override
            public void run() {
                InputStream is = null;
                try {
                    Packet packetStart = PacketHelper.getFileMsgPacket(socket);
                    packetStart.flog = CMD.FLOG.FLOG_FILE_START;//表示开始
                    sendQueue(packetStart);

                    File file = new File(path);
                    is = new FileInputStream(file);
                    byte[] buffer = new byte[FILE_BUFFER];
                    int len;
                    while ((len = is.read(buffer, 0, buffer.length)) > 0) {
                        Packet packetData = PacketHelper.getFileMsgPacket(socket);
                        packetData.flog = CMD.FLOG.FLOG_FILE_DATA;//内容

                        byte[] data = buffer;
                        if (len < buffer.length) {
                            data = ByteUtils.subBytes(buffer, 0, len);
                        }
                        packetData.data = data;
                        sendQueue(packetData);
                    }

                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                } finally {
                    closeCloseable(is);
                    Packet packetEnd = PacketHelper.getFileMsgPacket(socket);
                    packetEnd.flog = CMD.FLOG.FLOG_FILE_END;//表示结束
                    sendQueue(packetEnd);
                }
            }
        });
    }


    public void sendQueue(Packet packet) {
        realSendData(packet);

//        try {
//            packetsQueue.put(packet);
//        } catch (InterruptedException e) {
//            e.printStackTrace();
//        }
    }

    public void realSendData(Packet packet) {
        if (flog && socket.isConnected() && outputStream != null) {
            try {
                onSendMsgBefore(packet);
                outputStream.write(packet.realData());
//                outputStream.flush();
                onSendMsgAgo(true, packet);
            } catch (IOException e) {
                e.printStackTrace();
                onSendMsgAgo(false, packet);
            }
        } else {
            onSendMsgAgo(false, packet);
        }
    }


    public void exit() {
        flog = false;
    }


    private void onSendMsgAgo(boolean sendSuccess, Packet packet) {
        for (SocketMessageListener listener : listeners) {
            if (packet != null) {
                packet = listener.onSendMsgAgo(this, sendSuccess, packet);
            }
        }
    }

    private void onReciveMsg(Packet packet) {
        for (SocketMessageListener listener : listeners) {
            if (packet != null) {
                packet = listener.onReciveMsg(this, packet);
            }
        }
    }

    private void onSendMsgBefore(Packet packet) {
        for (SocketMessageListener listener : listeners) {
            if (packet != null) {
                packet = listener.onSendMsgBefore(this, packet);
            }
        }
    }


    public void addListener(SocketMessageListener listener) {
        if (listener == null)
            return;
        listeners.add(listener);
    }


    class SendDataThread extends Thread {

        @Override
        public void run() {
            while (flog) {
                try {
                    Packet packet = packetsQueue.take();
                    if (packet != null) {
                        realSendData(packet);
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

            }
        }
    }
}