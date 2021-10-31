package rmi;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.Objects;

public class InvocationHandlerClass implements InvocationHandler, Serializable {
    private Class<?> c;
    private InetSocketAddress inetSocketAddress;

    public <T> InvocationHandlerClass(Class<T> c, InetSocketAddress address) {
        this.c=c;
        this.inetSocketAddress=address;
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {

        if(isRemote(method))
        {
            return invokeRemoteMethod(proxy,method,args);
        }
        else
        {
            if (Objects.equals(method.getName(), "hashCode")) {
                return hashCode (proxy, args);
            }
            if (Objects.equals(method.getName(), "equals")) {
                return equals(proxy, args);
            }
            if (Objects.equals(method.getName(), "toString")) {
                return toString (proxy, args);
            }

            return new RMIException(new NoSuchMethodException());
        }

    }

    private Object invokeRemoteMethod(Object proxy, Method method, Object[] args) throws Throwable {
        Object inputStreamObject = null;
        String rmiStatus;

        try {
            InvocationHandlerClass invocationInvocationHandlerClass = (InvocationHandlerClass) Proxy.getInvocationHandler(proxy);
            Socket clientSocket = new Socket(invocationInvocationHandlerClass.inetSocketAddress.getAddress(), invocationInvocationHandlerClass.inetSocketAddress.getPort());

            ObjectOutputStream outputStream = new ObjectOutputStream(clientSocket.getOutputStream());
            outputStream.flush();

            outputStream.writeObject(method.getName());
            outputStream.writeObject(method.getParameterTypes());
            outputStream.writeObject(args);


            ObjectInputStream inputStream = new ObjectInputStream(clientSocket.getInputStream());
            rmiStatus = (String) inputStream.readObject();
            inputStreamObject = inputStream.readObject();

            clientSocket.close();
            if (rmiStatus.equals(Status.OK.toString())) {
                return inputStreamObject;
            } else if (rmiStatus.equals(Status.RMIException.toString())) {
                throw (Throwable) inputStreamObject;
            }
        }
        catch (Exception e)
        {
            throw new RMIException(e);
        }
        throw (Throwable) inputStreamObject;
    }

    private boolean equals (Object proxy, Object[] args) {
        if (args == null){
            return false;
        }
        if (args.length != 1){
            return false;
        }

        if (args[0] == null){
            return false;
        }

        if (!args[0].getClass().equals(proxy.getClass())){
            return false;
        }

        InvocationHandlerClass stubInvocationHandlerClass1 = (InvocationHandlerClass) Proxy.getInvocationHandler(args[0]);
        InvocationHandlerClass stubInvocationHandlerClass2 = (InvocationHandlerClass) Proxy.getInvocationHandler(proxy);
        return stubInvocationHandlerClass1.inetSocketAddress.equals(stubInvocationHandlerClass2.inetSocketAddress);
    }

    private int hashCode(Object proxy, Object[] args) {
        if (args != null){
            throw new IllegalArgumentException("no argument found");
        }
        InvocationHandlerClass stubInvocationHandlerClass = (InvocationHandlerClass) Proxy.getInvocationHandler(proxy);
        return stubInvocationHandlerClass.inetSocketAddress.hashCode() + proxy.getClass().hashCode();
    }

    private String toString(Object proxy, Object[] args){
        if (args != null){
            throw new IllegalArgumentException("no argument found");
        }

        InvocationHandlerClass stubInvocationHandlerClass = (InvocationHandlerClass) Proxy.getInvocationHandler(proxy);
        String name = "Remote Interface : " + proxy.getClass().getInterfaces()[0].toString();
        String addr = "Remote Address: " + stubInvocationHandlerClass.inetSocketAddress.toString();

        return name + "\n" + addr + "\n";
    }

    private boolean isRemote(Method method) {

        for(Method value : c.getMethods())
        {
            if(method.equals(value))
                return true;
        }
        return false;
    }
}