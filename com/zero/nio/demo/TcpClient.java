package com.zero.nio.demo;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.nio.charset.Charset;
import java.util.Scanner;

public class TcpClient {

    private SocketChannel socket;
    private Selector selector;

    public TcpClient(String hostname,int port) throws Exception{
        socket = SocketChannel.open();
        socket.configureBlocking(false);
        socket.connect(new InetSocketAddress(hostname,port));

        selector = Selector.open();
        socket.register(selector,SelectionKey.OP_CONNECT);
    }

    public void start() throws Exception{

        Scanner scanner = new Scanner(System.in);

        while (true){

            if(socket.isConnected()){
                writeMessageToServer(scanner);
            }

            int readyChannel = selector.select(5*1000);
            if(readyChannel>0){
                for(SelectionKey selectionKey : selector.selectedKeys()){
                    SocketChannel socketChannel = (SocketChannel) selectionKey.channel();
                    if(selectionKey.isConnectable()){
                        registerAndDoConnect(socketChannel);
                    }else if(selectionKey.isReadable()){
                        readMessageFromServer(socketChannel);
                    }
                }
                selector.selectedKeys().clear();
            }else{
                System.err.println("Handle select timeout exception");
                socket.close();
            }
        }

    }

    private void writeMessageToServer(Scanner scanner) throws IOException {
        String sendToServerMessage = scanner.nextLine();
        socket.write(Charset.forName("UTF-8").encode(sendToServerMessage));
        if(isShutdownInstruction(sendToServerMessage.trim())){
            System.out.println("The Client going to Shutdown!");
            scanner.close();
            socket.close();
            selector.close();
            System.exit(0);
        }
    }

    private boolean isShutdownInstruction(String sendToServerMessage) {
        return TcpConstants.CLIENT_CLOSE.equalsIgnoreCase(sendToServerMessage) || TcpConstants.SERVER_CLOSE.equalsIgnoreCase(sendToServerMessage);
    }

    private void registerAndDoConnect(SocketChannel channel) throws Exception{
        channel.configureBlocking(false);
        channel.register(selector,SelectionKey.OP_READ);
        channel.finishConnect();
    }

    private void readMessageFromServer(SocketChannel socketChannel) throws IOException {
        StringBuffer messageFromServer = new StringBuffer();

        ByteBuffer readMessageFromServerBuffer = ByteBuffer.allocate(1024);
        int bytesRead = socketChannel.read(readMessageFromServerBuffer);

        while (bytesRead>0){
            readMessageFromServerBuffer.flip();
            messageFromServer.append(Charset.forName("UTF-8").decode(readMessageFromServerBuffer));
            readMessageFromServerBuffer.clear();
            bytesRead = socketChannel.read(readMessageFromServerBuffer);
        }
        System.out.println(messageFromServer);
    }

    public static void main(String[] args) throws Exception {

        TcpClient client = new TcpClient(TcpConstants.SERVER_HOSTNAME, TcpConstants.SERVER_PORT);
        client.start();

    }

}
