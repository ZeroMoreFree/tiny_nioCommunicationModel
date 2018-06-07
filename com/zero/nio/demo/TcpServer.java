package com.zero.nio.demo;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.charset.Charset;
import java.util.Iterator;
import java.util.Set;

public class TcpServer{

    private ServerSocketChannel serverSocket ;
    private Selector selector;

    public TcpServer(int port) throws Exception{

        serverSocket = ServerSocketChannel.open();
        serverSocket.configureBlocking(false);
        serverSocket.bind(new InetSocketAddress(port));

        selector = Selector.open();
        serverSocket.register(selector,SelectionKey.OP_ACCEPT);

    }

    public void start() throws Exception{

        while (true){

            //判断是否有就绪的channel
            if(selector.select() <= 0){
                continue;
            }

            Set<SelectionKey> selectionKeySet = selector.selectedKeys();
            Iterator<SelectionKey> it = selectionKeySet.iterator();

            while (it.hasNext()){

                SelectionKey selectionKey = it.next();
                if(selectionKey.isAcceptable()){//查看serverSocket是否已经接收到socket了，有的话，则将接收到的socket注册到selector中，兴趣为读
                    checkHasAcceptedSocketAndDoRegister();
                }else if(selectionKey.isReadable()){//查看接收到的socket是否准备好了被读

                    SocketChannel socket = (SocketChannel) selectionKey.channel();

                    StringBuffer message = readMessageFromClient(socket);
                    System.out.println("Receving Message from client:" + message);

                    if(receivingCloseClientInstructions(message.toString())){
                        closeClient(socket);
                    }else if(receivingCloseServerInstructions(message.toString())){
                        closeServer(socket);
                    }else{
                        responseClient(socket);
                    }
                }
                //已经处理过的，则移出SelectionKeySet
                it.remove();
            }
        }
    }

    private void checkHasAcceptedSocketAndDoRegister() throws IOException {
        SocketChannel socket = serverSocket.accept();
        if(socket != null){
            socket.configureBlocking(false);
            socket.register(selector,SelectionKey.OP_READ);
        }
    }

    private StringBuffer readMessageFromClient(SocketChannel socket) throws IOException {
        ByteBuffer readFromSocketBuffer = ByteBuffer.allocate(1024);
        int bytesRead = socket.read(readFromSocketBuffer);
        StringBuffer message = new StringBuffer();
        while (bytesRead >0){
            readFromSocketBuffer.flip();
            message.append(Charset.forName("UTF-8").decode(readFromSocketBuffer));
            readFromSocketBuffer.clear();
            bytesRead = socket.read(readFromSocketBuffer);
        }
        return message;
    }

    private boolean receivingCloseClientInstructions(String message){
        return TcpConstants.CLIENT_CLOSE.equalsIgnoreCase(message.trim());
    }

    private void closeClient(SocketChannel channel) throws Exception{
        System.out.println("The client is going to shutdown!");
        channel.close();
    }

    private boolean receivingCloseServerInstructions(String message){
        return TcpConstants.SERVER_CLOSE.equalsIgnoreCase(message.trim());
    }

    private void closeServer(SocketChannel channel) throws Exception{
        System.out.println("The server is going to shutdown!");
        channel.close();
        selector.close();
        serverSocket.close();
        System.exit(0);
    }

    private void responseClient(SocketChannel socket){
        String serverResponse = "Thanks for your request!";
        try {
            socket.write(Charset.forName("UTF-8").encode(serverResponse));
        } catch (IOException e) {
            e.printStackTrace();
            System.out.println("Response client fail!");
        }

    }

    public static void main(String[] args) {
        try {
            TcpServer server = new TcpServer(TcpConstants.SERVER_PORT);
            server.start();
        }catch (Exception e){
            e.printStackTrace();
        }
    }
}
