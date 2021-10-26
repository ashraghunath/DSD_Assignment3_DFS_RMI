package rmi;

import common.CommonFunctions;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.net.*;
import java.util.Objects;

/** RMI stub factory.

 <p>
 RMI stubs hide network communication with the remote server and provide a
 simple object-like interface to their users. This class provides methods for
 creating stub objects dynamically, when given pre-defined interfaces.

 <p>
 The network address of the remote server is set when a stub is created, and
 may not be modified afterwards. Two stubs are equal if they implement the
 same interface and carry the same remote server address - and would
 therefore connect to the same skeleton. Stubs are serializable.
 */
public abstract class Stub
{
    /** Creates a stub, given a skeleton with an assigned adress.

     <p>
     The stub is assigned the address of the skeleton. The skeleton must
     either have been created with a fixed address, or else it must have
     already been started.

     <p>
     This method should be used when the stub is created together with the
     skeleton. The stub may then be transmitted over the network to enable
     communication with the skeleton.

     @param c A <code>Class</code> object representing the interface
     implemented by the remote object.
     @param skeleton The skeleton whose network address is to be used.
     @return The stub created.
     @throws IllegalStateException If the skeleton has not been assigned an
     address by the user and has not yet been
     started.
     @throws UnknownHostException When the skeleton address is a wildcard and
     a port is assigned, but no address can be
     found for the local host.
     @throws NullPointerException If any argument is <code>null</code>.
     @throws Error If <code>c</code> does not represent a remote interface
     - an interface in which each method is marked as throwing
     <code>RMIException</code>, or if an object implementing
     this interface cannot be dynamically created.
     */
    public static <T> T create(Class<T> c, Skeleton<T> skeleton)
            throws UnknownHostException
    {
        if(skeleton.getInetSocketAddress()==null)
            throw new IllegalStateException("Skeleton not assigned an address");
        return create(c,skeleton.getInetSocketAddress());
    }

    /** Creates a stub, given a skeleton with an assigned address and a hostname
     which overrides the skeleton's hostname.

     <p>
     The stub is assigned the port of the skeleton and the given hostname.
     The skeleton must either have been started with a fixed port, or else
     it must have been started to receive a system-assigned port, for this
     method to succeed.

     <p>
     This method should be used when the stub is created together with the
     skeleton, but firewalls or private networks prevent the system from
     automatically assigning a valid externally-routable address to the
     skeleton. In this case, the creator of the stub has the option of
     obtaining an externally-routable address by other means, and specifying
     this hostname to this method.

     @param c A <code>Class</code> object representing the interface
     implemented by the remote object.
     @param skeleton The skeleton whose port is to be used.
     @param hostname The hostname with which the stub will be created.
     @return The stub created.
     @throws IllegalStateException If the skeleton has not been assigned a
     port.
     @throws NullPointerException If any argument is <code>null</code>.
     @throws Error If <code>c</code> does not represent a remote interface
     - an interface in which each method is marked as throwing
     <code>RMIException</code>, or if an object implementing
     this interface cannot be dynamically created.
     */
    public static <T> T create(Class<T> c, Skeleton<T> skeleton,
                               String hostname)
    {
        if(hostname==null)
            throw new NullPointerException("Hostname is null");
        InetSocketAddress inetSocketAddress = skeleton.getInetSocketAddress();
        if (inetSocketAddress == null){
            throw new IllegalStateException("Skeleton is not assigned any address");
        }
        inetSocketAddress = new InetSocketAddress(hostname,inetSocketAddress.getPort());
        return create(c,inetSocketAddress);
    }

    /** Creates a stub, given the address of a remote server.

     <p>
     This method should be used primarily when bootstrapping RMI. In this
     case, the server is already running on a remote host but there is
     not necessarily a direct way to obtain an associated stub.

     @param c A <code>Class</code> object representing the interface
     implemented by the remote object.
     @param address The network address of the remote skeleton.
     @return The stub created.
     @throws NullPointerException If any argument is <code>null</code>.
     @throws Error If <code>c</code> does not represent a remote interface
     - an interface in which each method is marked as throwing
     <code>RMIException</code>, or if an object implementing
     this interface cannot be dynamically created.
     */
    public static <T> T create(Class<T> c, InetSocketAddress address)
    {
        if(c==null)
            throw new NullPointerException("class parameter is null");
        if(address==null)
            throw new NullPointerException("address parameter is null");
        if(!c.isInterface())
            throw new Error("c is not an interface");
        if(!CommonFunctions.throwsRemoteException(c))
            throw new Error("c is not a remote interface");
        try {
            return (T) Proxy.newProxyInstance(c.getClassLoader(), new Class<?>[] { c }, new StubHandler(c, address));
        } catch (Exception e){
            System.out.print(e);
            return null;
        }
    }

    private static class StubHandler implements InvocationHandler, Serializable {
        private Class<?> c;
        private InetSocketAddress inetSocketAddress; 
        
        public <T> StubHandler(Class<T> c, InetSocketAddress address) {
            this.c=c;
            this.inetSocketAddress=address;
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            
            if(isMethodRmote(method))
            {
                return invokeRemoteMethod(proxy,method,args);
            }
            else
            {
                if (Objects.equals(method.getName(), "equals")) {
                    return localEquals(proxy, args);
                }

                if (Objects.equals(method.getName(), "hashCode")) {
                    return localHashCode (proxy, args);
                }

                if (Objects.equals(method.getName(), "toString")) {
                    return localToString (proxy, args);
                }

                return new RMIException(new NoSuchMethodException());
            }
            
        }

        private Object invokeRemoteMethod(Object proxy, Method method, Object[] args) throws Throwable {
            Object inputStreamObject = null;
            String rmiStatus;

            try {
                StubHandler invocationHandler = (StubHandler) Proxy.getInvocationHandler(proxy);
                Socket clientSocket = new Socket(invocationHandler.inetSocketAddress.getAddress(), invocationHandler.inetSocketAddress.getPort());

                ObjectOutputStream outputStream = new ObjectOutputStream(clientSocket.getOutputStream());
                outputStream.flush();

                outputStream.writeObject(method.getName());
                outputStream.writeObject(method.getParameterTypes());
                outputStream.writeObject(args);


                ObjectInputStream inputStream = new ObjectInputStream(clientSocket.getInputStream());
                rmiStatus = (String) inputStream.readObject();
                inputStreamObject = inputStream.readObject();

                clientSocket.close();
                if (rmiStatus.equals(RMIStatus.OK.toString())) {
                    return inputStreamObject;
                } else if (rmiStatus.equals(RMIStatus.RMIException.toString())) {
                    throw (Throwable) inputStreamObject;
                }
            }
            catch (Exception e)
            {
                throw new RMIException(e);
            }
            throw (Throwable) inputStreamObject;
        }

        private boolean localEquals (Object proxy, Object[] args) {
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

            StubHandler stubHandler1 = (StubHandler) Proxy.getInvocationHandler(args[0]);
            StubHandler stubHandler2 = (StubHandler) Proxy.getInvocationHandler(proxy);
            return stubHandler1.inetSocketAddress.equals(stubHandler2.inetSocketAddress);
        }

        private int localHashCode(Object proxy, Object[] args) {
            if (args != null){
                throw new IllegalArgumentException("Method hashcode takes no argument!");
            }
            StubHandler stubHandler = (StubHandler) Proxy.getInvocationHandler(proxy);
            return stubHandler.inetSocketAddress.hashCode() + proxy.getClass().hashCode();
        }

        private String localToString(Object proxy, Object[] args){
            if (args != null){
                throw new IllegalArgumentException("Method to string takes no argument");
            }

            StubHandler stubHandler = (StubHandler) Proxy.getInvocationHandler(proxy);
            String name = "Remote Interface : " + proxy.getClass().getInterfaces()[0].toString();
            String addr = "Remote Address: " + stubHandler.inetSocketAddress.toString();

            return name + "\n" + addr + "\n";
        }

        private boolean isMethodRmote(Method method) {
            
            for(Method value : c.getMethods())
            {
                if(method.equals(value))
                    return true;
            }
            return false;
        }
    }
}
