# 服务端与客户端各有四个类 #
服务端： 
  AioTcpServer---服务器主类。它与客户端通信由回调连接处理器AioAcceptHandler完成。 <br>
  AioAcceptHandler-----连接处理器。处理服务器与客户端的通信。具体的读操作，回调AioReadHandler处理器 <br>
  AioReadHandler-----读处理器。处理连接处理器交给的 读任务。即具体的读客户端的信息由它完成。如果服务器要回应该客户端，需要回调AioWriteHandler写处理器。 <br>
  AioWriteHandler----写处理器。完成读处理器交给的任务，即向客户端回应消息。 <br>

客户端： 
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
