package rmi;

import common.CommonFunctions;


import java.io.*;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.*;
import java.util.ArrayList;

/** RMI skeleton

    <p>
    A skeleton encapsulates a multithreaded TCP server. The server's clients are
    intended to be RMI stubs created using the <code>Stub</code> class.

    <p>
    The skeleton class is parametrized by a type variable. This type variable
    should be instantiated with an interface. The skeleton will accept from the
    stub requests for calls to the methods of this interface. It will then
    forward those requests to an object. The object is specified when the
    skeleton is constructed, and must implement the remote interface. Each
    method in the interface should be marked as throwing
    <code>RMIException</code>, in addition to any other exceptions that the user
    desires.

    <p>
    Exceptions may occur at the top level in the listening and service threads.
    The skeleton's response to these exceptions can be customized by deriving
    a class from <code>Skeleton</code> and overriding <code>listen_error</code>
    or <code>service_error</code>.
*/
public class Skeleton<T>
{
    InetSocketAddress inetSocketAddress;
    private Class<T> remoteInterface=null;
    private T server=null;
    private ServerSocket serverSocket;
    private ListenThread<T> listenThread;

    public Skeleton(){

    }

    /** Creates a <code>Skeleton</code> with no initial server address. The
        address will be determined by the system when <code>start</code> is
        called. Equivalent to using <code>Skeleton(null)</code>.

        <p>
        This constructor is for skeletons that will not be used for
        bootstrapping RMI - those that therefore do not require a well-known
        port.

        @param c An object representing the class of the interface for which the
                 skeleton server is to handle method call requests.
        @param server An object implementing said interface. Requests for method
                      calls are forwarded by the skeleton to this object.
        @throws Error If <code>c</code> does not represent a remote interface -
                      an interface whose methods are all marked as throwing
                      <code>RMIException</code>.
        @throws NullPointerException If either of <code>c</code> or
                                     <code>server</code> is <code>null</code>.
     */
    public Skeleton(Class<T> c, T server)
    {
        if(c==null || server==null)
            throw new NullPointerException("parameters are null while initializing");
        if(!c.isInterface())
            throw new Error("Variable c not an interface");
        if(!CommonFunctions.throwsRemoteException(c))
            throw new Error("Constructor cannot accept not remote interface");


       this.remoteInterface=c;
       this.server=server;

    }

    /** Creates a <code>Skeleton</code> with the given initial server address.

        <p>
        This constructor should be used when the port number is significant.

        @param c An object representing the class of the interface for which the
                 skeleton server is to handle method call requests.
        @param server An object implementing said interface. Requests for method
                      calls are forwarded by the skeleton to this object.
        @param address The address at which the skeleton is to run. If
                       <code>null</code>, the address will be chosen by the
                       system when <code>start</code> is called.
        @throws Error If <code>c</code> does not represent a remote interface -
                      an interface whose methods are all marked as throwing
                      <code>RMIException</code>.
        @throws NullPointerException If either of <code>c</code> or
                                     <code>server</code> is <code>null</code>.
     */
    public Skeleton(Class<T> c, T server, InetSocketAddress address)
    {
        this(c,server);
        this.inetSocketAddress=address;
    }

    /** Called when the listening thread exits.

        <p>
        The listening thread may exit due to a top-level exception, or due to a
        call to <code>stop</code>.

        <p>
        When this method is called, the calling thread owns the lock on the
        <code>Skeleton</code> object. Care must be taken to avoid deadlocks when
        calling <code>start</code> or <code>stop</code> from different threads
        during this call.

        <p>
        The default implementation does nothing.

        @param cause The exception that stopped the skeleton, or
                     <code>null</code> if the skeleton stopped normally.
     */
    protected void stopped(Throwable cause)
    {
        //Todo not sure
        if (cause != null) {
            cause.printStackTrace();
        }
    }

    /** Called when an exception occurs at the top level in the listening
        thread.

        <p>
        The intent of this method is to allow the user to report exceptions in
        the listening thread to another thread, by a mechanism of the user's
        choosing. The user may also ignore the exceptions. The default
        implementation simply stops the server. The user should not use this
        method to stop the skeleton. The exception will again be provided as the
        argument to <code>stopped</code>, which will be called later.

        @param exception The exception that occurred.
        @return <code>true</code> if the server is to resume accepting
                connections, <code>false</code> if the server is to shut down.
     */
    protected boolean listen_error(Exception exception)
    {
        stop();
        return false;
    }

    /** Called when an exception occurs at the top level in a service thread.

        <p>
        The default implementation does nothing.

        @param exception The exception that occurred.
     */
    protected void service_error(RMIException exception)
    {
        if (!exception.getClass().equals(EOFException.class)) {
            exception.printStackTrace();
        }
    }

    /** Starts the skeleton server.

        <p>
        A thread is created to listen for connection requests, and the method
        returns immediately. Additional threads are created when connections are
        accepted. The network address used for the server is determined by which
        constructor was used to create the <code>Skeleton</code> object.

        @throws RMIException When the listening socket cannot be created or
                             bound, when the listening thread cannot be created,
                             or when the server has already been started and has
                             not since stopped.
     */
    public synchronized void start() throws RMIException
    {
        if(this.serverSocket != null && !this.serverSocket.isClosed()) {
            throw new RMIException("The server has already been started and has not since stopped");
        }
            try {

                if (inetSocketAddress == null
                        || this.inetSocketAddress.getPort() == 0 || this.inetSocketAddress.getHostName() == null) {
                    this.serverSocket = new ServerSocket(0);
                    //TOdo to add socketaddress?
                    this.inetSocketAddress = new InetSocketAddress(this.serverSocket.getLocalPort());
                }
                else
                {
                    this.serverSocket = new ServerSocket(
                            this.inetSocketAddress.getPort(),
                            1000,
                            this.inetSocketAddress.getAddress()
                    );
                }
            } catch (Exception e) {
                throw new RMIException(e);
            }
            try {
                this.listenThread = (new ListenThread<T>(this, this.serverSocket, this.inetSocketAddress,
                        this.remoteInterface, this.server));
                this.listenThread.start();
            } catch (Exception e) {
                listen_error(e);

                throw new RMIException(e);
            }
    }

    /** Stops the skeleton server, if it is already running.

        <p>
        The listening thread terminates. Threads created to service connections
        may continue running until their invocations of the <code>service</code>
        method return. The server stops at some later time; the method
        <code>stopped</code> is called at that point. The server may then be
        restarted.
     */
    public synchronized void stop()
    {
            //TODO force close is causing timeout , try again?
//        forceClose(serverSocket);
        try {
            if(this.serverSocket != null && !this.serverSocket.isClosed())
                serverSocket.close();
        }
        catch (Exception e) {
            listen_error(e);
        }
        try {
            listenThread.join();
            stopped(null);
        }
        catch(Exception e) {
            e.printStackTrace();
            listen_error(e);
        }
    }


    private class ListenThread<T> extends Thread {

        ArrayList<ServiceThread<T>> threads = new ArrayList<ServiceThread<T>>();
        InetSocketAddress inetSocketAddress;
        Class<T> remoteInterface;
        T server;
        ServerSocket serverSocket;
        Skeleton<T> skeleton;

        public ListenThread(Skeleton<T> skeleton, ServerSocket serverSocket, InetSocketAddress inetSocketAddress, Class<T> remoteInterface, T server) {
            this.skeleton = skeleton;
            this.serverSocket = serverSocket;
            this.inetSocketAddress = inetSocketAddress;
            this.remoteInterface = remoteInterface;
            this.server = server;
        }

        public void run() {
            try {
                while (true) {
                    Socket socket = this.serverSocket.accept();
                    ServiceThread<T> thread = (new ServiceThread<T>(this.skeleton, socket, this.server, this.remoteInterface));
                    thread.start();
                    threads.add(thread);
                }
            } catch (SocketException e) {
                try {
                    if (this.serverSocket.isClosed())
                        for (ServiceThread<T> t : this.threads) {
                            t.join();
                        }
                } catch (InterruptedException ep) {

                }
            } catch (IOException e) {
                this.skeleton.listen_error(e);
            }
        }
    }

    private class ServiceThread<T> extends Thread {
            //TODO add access modifier change later
            Socket socket;
        T server = null;
        Class<T> remoteInterface;
        Skeleton<T> skeleton;


        public ServiceThread(Skeleton<T> skeleton, Socket socket, T server, Class<T> remoteInterface) {

            this.skeleton = skeleton;
            this.socket = socket;
            this.server = server;
            this.remoteInterface = remoteInterface;
        }


        public void run() {
            ObjectOutputStream objOutput = null;
            ObjectInputStream objInput = null;
            Object ret = null;

            try {
                objOutput = new ObjectOutputStream(this.socket.getOutputStream());
                objOutput.flush();
                objInput = new ObjectInputStream(this.socket.getInputStream());
            }
            catch(Exception e) {
                close(objInput);
                close(objOutput);
                // Exception thrown in service response.
                // this.skeleton.service_error(new RMIException("Exception thrown in service response."));
                return;
            }

            try {
                @SuppressWarnings("unchecked")
                String methodName = (String) objInput.readObject();
                @SuppressWarnings("unchecked")
                Class<T>[] paramTypes = (Class<T>[]) objInput.readObject();
                @SuppressWarnings("unchecked")
                Object[] params = (Object[]) objInput.readObject();

                this.remoteInterface.getMethod(methodName, paramTypes);

                Method method = this.server.getClass().getMethod(methodName, paramTypes);


                if(!method.isAccessible()) {
                    method.setAccessible(true);
                }

                ret = method.invoke(this.server, params);
            }
            catch(Exception e) {
                if(e instanceof InvocationTargetException) {
                    ret = e;
                }
                else {
                    //throw new RMIException(e);
                    close(objInput);
                    close(objOutput);
                    this.skeleton.service_error(new RMIException("Exception thrown in service response.", e));
                    return;
                }
            }

            try {
                objOutput.writeObject(ret);
            }
            catch(Exception e) {
                this.skeleton.service_error(new RMIException("Exception thrown in service response."));
            }
            finally {
                close(objInput);
                close(objOutput);
            }
        }

        public void close(Closeable c) {
            if (c == null) return;
            try {
                c.close();
            } catch (IOException e) {
                // ignore the exception
            }
        }

        }



//    private static void forceClose(ServerSocket serverSocket) {
//        try {
//            serverSocket.close();
//        } catch (Throwable var2) {
//        }
//
//    }
}
