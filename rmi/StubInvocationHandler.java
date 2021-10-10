package rmi;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.UnknownHostException;

public class StubInvocationHandler implements InvocationHandler {
    InetSocketAddress skeletonAddress;
    Class<?> intf;

    public StubInvocationHandler(Class<?> c, InetSocketAddress address) {
        this.intf = c;
        this.skeletonAddress = address;
    }


    public boolean equalsStub(Object stub) {
        if(stub == null) {
            return false;
        }

        try {
            StubInvocationHandler stubhandler = (StubInvocationHandler) Proxy.getInvocationHandler(stub);
            if(this.skeletonAddress.toString().equals(stubhandler.skeletonAddress.toString())
                    && this.intf.toString().equals(stubhandler.intf.toString())) {
                return true;
            }
            else {
                return false;
            }
        }
        catch(Throwable e) {
            return false;
        }
    }

    public int hashCodeStub() {
        return (this.intf.toString() + this.skeletonAddress.toString()).hashCode();
    }

    public String toStringStub() {
        return this.intf.toString() + this.skeletonAddress.toString();
    }

    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        Object result = null;
        Socket socket = null;
        ObjectOutputStream objOutput = null;
        ObjectInputStream objInput = null;
        try {
            this.intf.getMethod(method.getName(), method.getParameterTypes());
        }
        catch(NoSuchMethodException e) {
            switch(method.getName()) {
                case "equals":                  return this.equalsStub(args[0]);
                case "hashCode":                return this.hashCodeStub();
                case "toString":                return this.toStringStub();
                default:                        break;
            }
        }

        try {
            socket = new Socket(this.skeletonAddress.getAddress(),
                                       this.skeletonAddress.getPort());
            objOutput = new ObjectOutputStream(socket.getOutputStream());
            objOutput.flush();
            objInput = new ObjectInputStream(socket.getInputStream());

            objOutput.writeObject(method.getName());
            objOutput.writeObject(method.getParameterTypes());
            objOutput.writeObject(args);
            objOutput.flush();

            result = objInput.readObject();
        }
        catch(UnknownHostException e) {
            throw new RMIException(e);
        }
        catch(IOException e) {
            throw new RMIException(e);
        }
        finally {
            if(objInput != null) objInput.close();
            if(objOutput != null) objOutput.close();
            if(socket != null) socket.close();
        }
        if(result instanceof InvocationTargetException) {
            throw ((InvocationTargetException) result).getTargetException();
        }
        return result;
    }
}
