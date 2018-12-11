# 服务端与客户端各有四个类 #
# 服务端：
  AioTcpServer---服务器主类。它与客户端通信由回调连接处理器AioAcceptHandler完成。 <br>
  AioAcceptHandler-----连接处理器。处理服务器与客户端的通信。具体的读操作，回调AioReadHandler处理器 <br>
  AioReadHandler-----读处理器。处理连接处理器交给的 读任务。即具体的读客户端的信息由它完成。如果服务器要回应该客户端，需要回调AioWriteHandler写处理器。 <br>
  AioWriteHandler----写处理器。完成读处理器交给的任务，即向客户端回应消息。 <br>

# 客户端：
   AioTcpClient-------客户端主类。它与服务器的通讯由回调AioConnectHandler连接处理器完成。具体写信息由客户端socket回调AioSendHandler完成。具体读信息由客户端socket回调AioReadHandler完成。 <br>
   AioConnectHandler-------连接处理器。完成与服务器的通讯。具体的读信息由客户端socket回调读处理器AioReadHandler完成，即完成读取服务器的信息。 <br>
   AioReadHandler-------读处理器，完成读取服务端信息。 <br>
   AioSendHandler----写处理器，向服务端发送信息。 <br>
   
   
# 异步socket编程，一样分成客户端与服务端 #
      AsynchronousServerSocketChannel  -------服务端socket; 
      AsynchronousSocketChannel------客户端socket. 
      AsynchronousChannelGroup-----socket管理器。服务端socket与客户端socket都由它生成。它的管理需要线程池。它的工作方式之一是把必要的资源交给客户端与服务端的处理器，并调用该处理器进行工作。 
      ExecutorService-----线程池。是socket管理器需要的东西。 
      CompletionHandler-------处理器。它有两个泛型参数A、V。这是个回调函数所用的标准接口。
      Socket管理器 会把相关实参放到这个A,V的参数中，让用户处理后，然后调用这个处理器的方法进行执行。
      如果用户有一个方法中的参数的类型是该处理器，那么在其他地方再次调用这个方法，尽管方法不同，
      但是传给该方法的CompletionHandler的处理器的A、V参数 却是不相同的，不仅是值不同，类型也有可能完全不同。

# 代码说明 #
1.解码：
  
  attachment.flip();
  CharsetDecoder decoder = Charset.forName("UTF-8").newDecoder();
  decoder.onMalformedInput(CodingErrorAction.IGNORE); // 注意
  content = decoder.decode(attachment).toString();
  attachment.compact();
  
  2.客户端连接：
  
  socket.connect(new InetSocketAddress("localhost", 8888), socket, new ConnectHandler());
  这里注意加上IP，不然连接打不开。
  
  下面贴上我自己的代码：
  
  Server:
  
 
  package com.demo.aio;
   
  import java.io.IOException;
  import java.net.InetSocketAddress;
  import java.nio.ByteBuffer;
  import java.nio.channels.AsynchronousChannelGroup;
  import java.nio.channels.AsynchronousServerSocketChannel;
  import java.nio.channels.AsynchronousSocketChannel;
  import java.nio.channels.CompletionHandler;
  import java.nio.charset.CharacterCodingException;
  import java.nio.charset.Charset;
  import java.nio.charset.CharsetDecoder;
  import java.nio.charset.CodingErrorAction;
  import java.util.concurrent.ExecutorService;
  import java.util.concurrent.Executors;
   
  public class Server {
   
      public void server() throws IOException {
          ExecutorService executor = Executors.newFixedThreadPool(20);
          AsynchronousChannelGroup asyncChannelGroup = AsynchronousChannelGroup.withThreadPool(executor);
          AsynchronousServerSocketChannel channel = AsynchronousServerSocketChannel.open(asyncChannelGroup).bind(new InetSocketAddress("localhost", 8888));
          channel.accept(channel, new AcceptHandler());
      }
       
      private class AcceptHandler implements CompletionHandler<AsynchronousSocketChannel, AsynchronousServerSocketChannel> {
           
          @Override
          public void completed(AsynchronousSocketChannel result, AsynchronousServerSocketChannel attachment) {
              attachment.accept(attachment, this);
              ByteBuffer buffer = ByteBuffer.allocate(1024);
              result.read(buffer, buffer, new ReaderHandler(result));
          }
   
          @Override
          public void failed(Throwable exc, AsynchronousServerSocketChannel attachment) {
          }
           
      }
       
      private class ReaderHandler implements CompletionHandler<Integer, ByteBuffer> {
   
          private AsynchronousSocketChannel socket;
           
          public ReaderHandler(AsynchronousSocketChannel socket) {
              this.socket = socket;
          }
           
          @Override
          public void completed(Integer result, ByteBuffer attachment) {
              if(result > 0) {
                  String content = null;
                  try {
                      attachment.flip();
                      CharsetDecoder decoder = Charset.forName("UTF-8").newDecoder();
  //                  decoder.onMalformedInput(CodingErrorAction.IGNORE);
                      content = decoder.decode(attachment).toString();
                      attachment.compact();
                      System.out.println("收到客户端消息：" + content);
                  } catch (CharacterCodingException e) {
                      e.printStackTrace();
                  }
                  socket.read(attachment, attachment, this);
                  ByteBuffer client = ByteBuffer.wrap(("服务器回复消息：" + content).getBytes());
                  socket.write(client, client, new WriterHandler(socket));
              } else if(result == 0) {
                  System.out.println("空消息");
              } else {
                  attachment = null;
                  System.out.println("断开");
              }
          }
   
          @Override
          public void failed(Throwable exc, ByteBuffer attachment) {
          }
           
      }
       
      private class WriterHandler implements CompletionHandler<Integer, ByteBuffer> {
           
          private AsynchronousSocketChannel socket;
           
          public WriterHandler(AsynchronousSocketChannel socket) {
              this.socket = socket;
          }
           
          @Override
          public void completed(Integer result, ByteBuffer attachment) {
              if(result > 0) {
                  socket.write(attachment, attachment, this);
              } else if(result == 0) {
                  System.out.println("空消息");
              } else {
                  attachment = null;
                  System.out.println("断开");
              }
          }
           
          @Override
          public void failed(Throwable exc, ByteBuffer attachment) {
          }
           
      }
       
      public static void main(String[] args) throws IOException, InterruptedException {
          new Server().server();
          Thread.sleep(Integer.MAX_VALUE);
      }
       
  }
  Client:
  
  
  package com.demo.aio;
   
  import java.io.IOException;
  import java.io.UnsupportedEncodingException;
  import java.net.InetSocketAddress;
  import java.net.StandardSocketOptions;
  import java.nio.ByteBuffer;
  import java.nio.channels.AsynchronousChannelGroup;
  import java.nio.channels.AsynchronousSocketChannel;
  import java.nio.channels.CompletionHandler;
  import java.nio.charset.CharacterCodingException;
  import java.nio.charset.Charset;
  import java.util.Scanner;
  import java.util.concurrent.ExecutorService;
  import java.util.concurrent.Executors;
   
  public class Client {
   
      private AsynchronousSocketChannel socket = null;
       
      public void client() throws IOException {
          ExecutorService executor = Executors.newFixedThreadPool(20);
          AsynchronousChannelGroup asyncChannelGroup = AsynchronousChannelGroup.withThreadPool(executor);
          socket = AsynchronousSocketChannel.open(asyncChannelGroup);
          socket.setOption(StandardSocketOptions.TCP_NODELAY, true);
          socket.setOption(StandardSocketOptions.SO_REUSEADDR, true);
          socket.setOption(StandardSocketOptions.SO_KEEPALIVE, true);
          socket.connect(new InetSocketAddress("localhost", 8888), socket, new ConnectHandler()); // 注意localhost
      }
   
      private class ConnectHandler implements CompletionHandler<Void, AsynchronousSocketChannel> {
   
          @Override
          public void completed(Void result, AsynchronousSocketChannel attachment) {
              socket.write(ByteBuffer.wrap("客户端开始连接".getBytes()));
              ByteBuffer clientBuffer = ByteBuffer.allocate(1024);
              attachment.read(clientBuffer, clientBuffer, new ReaderHandler(attachment));
          }
   
          @Override
          public void failed(Throwable exc, AsynchronousSocketChannel attachment) {
          }
           
      }
       
      private class ReaderHandler implements CompletionHandler<Integer, ByteBuffer> {
   
          private AsynchronousSocketChannel socket;
           
          public ReaderHandler(AsynchronousSocketChannel socket) {
              this.socket = socket;
          }
           
          @Override
          public void completed(Integer result, ByteBuffer attachment) {
              if(result > 0) {
                  String content = null;
                  try {
                      attachment.flip();
                      content = Charset.forName("UTF-8").newDecoder().decode(attachment).toString();
                      attachment.compact();
                  } catch (CharacterCodingException e) {
                      e.printStackTrace();
                  }
                  System.out.println(content);
                  socket.read(attachment, attachment, this);
              } else if(result == 0) {
                  System.out.println("空消息");
              } else {
                  attachment = null;
                  System.out.println("断开");
              }
          }
   
          @Override
          public void failed(Throwable exc, ByteBuffer attachment) {
          }
           
      }
       
      public void send() throws UnsupportedEncodingException {
          Scanner scanner = new Scanner(System.in);
          String tmp = null;
          while((tmp = scanner.next()) != null) {
              ByteBuffer buffer = ByteBuffer.wrap(tmp.getBytes("utf-8"));
              System.out.println("客户端发出信息：" + tmp);
              socket.write(buffer, buffer, new SenderHandler(socket));
          }
      }
       
      private class SenderHandler implements CompletionHandler<Integer, ByteBuffer> {
   
          private AsynchronousSocketChannel socket;
           
          public SenderHandler(AsynchronousSocketChannel socket) {
              this.socket = socket;
          }
           
          @Override
          public void completed(Integer result, ByteBuffer attachment) {
              if(result > 0)
                  socket.write(attachment, attachment, this);
              else
                  attachment = null;
          }
   
          @Override
          public void failed(Throwable exc, ByteBuffer attachment) {
          }
           
      }
       
      public static void main(String[] args) throws IOException {
          Client client = new Client();
          client.client();
          client.send();
      }
       
  }