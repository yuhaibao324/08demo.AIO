/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package app1.server;

import java.io.IOException; 
import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer; 
import java.nio.channels.AsynchronousSocketChannel; 
import java.nio.channels.CompletionHandler; 
import java.nio.charset.CharacterCodingException; 
import java.nio.charset.Charset; 
import java.nio.charset.CharsetDecoder; 
import java.util.logging.Level;
import java.util.logging.Logger;

//这里的参数型号，受调用它的函数决定。这里是受客户端socket.read调用
public class AioReadHandler implements CompletionHandler
        <Integer,ByteBuffer>
{ 
    private AsynchronousSocketChannel socket; 
    public  String msg;
 
    public AioReadHandler(AsynchronousSocketChannel socket) { 
        this.socket = socket; 
    }     
    private CharsetDecoder decoder = Charset.forName("UTF-8").newDecoder();  
    
    @Override
    public void completed(Integer i, ByteBuffer buf) { 
        if (i > 0) { 
            buf.flip(); 
            try { 
                msg=decoder.decode(buf).toString();
                System.out.println("收到" + 
                        socket.getRemoteAddress().toString() + "的消息:" + msg
                ); 
                buf.compact(); 
            } catch (CharacterCodingException e) { 
                e.printStackTrace(); 
            } catch (IOException e) { 
                e.printStackTrace(); 
            } 
            socket.read(buf, buf, this); 
            try {
                write(socket);
            } catch (UnsupportedEncodingException ex) {
                Logger.getLogger(AioReadHandler.class.getName()).log(Level.SEVERE, null, ex);
            }
        } else if (i == -1) { 
            try { 
                System.out.println("客户端断线:" + socket.getRemoteAddress().toString()); 
                buf = null; 
            } catch (IOException e) { 
                e.printStackTrace(); 
            } 
        } 
    } 

    @Override
    public void failed(Throwable exc, ByteBuffer attachment) {
         System.out.println("cancelled"); 
    }
 
     //不是CompletionHandler的方法
    public void write(AsynchronousSocketChannel socket) throws UnsupportedEncodingException{
        String sendString="服务器回应,你输出的是:"+msg;
        ByteBuffer clientBuffer=ByteBuffer.wrap(sendString.getBytes("UTF-8"));        
        socket.write(clientBuffer, clientBuffer, new app1.server.AioWriteHandler(socket));
    }
}
