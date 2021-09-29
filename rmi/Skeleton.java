package rmi;

import common.CommonFunctions;


import java.io.*;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.*;

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
    private InetSocketAddress inetSocketAddress;
    private Class<T> remoteInterface=null;
    private T server=null;
    private ServerSocket serverSocket;
    private Object lock = new Object() ;
    private boolean started=false;

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
        if(c==null || server==null)
            throw new NullPointerException("parameters are null while initializing");
        if(!c.isInterface())
            throw new Error("Variable c not an interface");
        if(!CommonFunctions.throwsRemoteException(c))
            throw new Error("Constructor cannot accept not remote interface");
        this.remoteInterface=c;
        this.server=server;
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
        return false;
    }

    /** Called when an exception occurs at the top level in a service thread.

        <p>
        The default implementation does nothing.

        @param exception The exception that occurred.
     */
    protected void service_error(RMIException exception)
    {
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
        if(started)
            return;

        try{

            if (inetSocketAddress == null){
                serverSocket = new ServerSocket(0);
                this.inetSocketAddress = (InetSocketAddress) serverSocket.getLocalSocketAddress();
            }else{
                serverSocket = new ServerSocket();
                serverSocket.bind(inetSocketAddress);
            }
            started=true;
        }catch (Exception e)
        {
            started=false;
            throw new RMIException(e);
        }
        try
        {
            new Thread(new ListenThread()).start();
        }catch (Exception e)
        {
            started=false;
            forceClose(serverSocket);
        }
//        throw new UnsupportedOperationException("not implemented");
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
        forceClose(serverSocket);
    }


    private class ListenThread implements Runnable {
        private ListenThread() {
        }

        public void run() {
            Socket socket = null;
            try {
                serverSocket = new ServerSocket(inetSocketAddress.getPort());
            } catch (IOException e) {
                e.printStackTrace();
            }
            while (true) {
                synchronized (lock) {
                    try {
                        socket = serverSocket.accept();
                        (new Thread(Skeleton.this.new ServiceThread(socket))).start();
                    } catch (IOException e) {
                        System.out.println("Server stopped : " + e.getMessage());
                        break;
                    }
                }
            }
        }
    }

    private class ServiceThread implements Runnable {

        private final Socket socket;

        ServiceThread(Socket socket) {
            this.socket = socket;
        }

        private void feedback(String var1, Throwable var2, ObjectOutputStream methodName) throws RMIException {
            try {
                RMIException var4 = new RMIException("remote exception: " + var1, var2);
                methodName.writeBoolean(false);
                methodName.writeObject(var4);
            } catch (Exception var5) {
            }

            throw new RMIException(var1, var2);
        }

        public void run() {

            ObjectOutputStream objOutput = null;
            ObjectInputStream objInput = null;


            try {
                try {

                    objOutput = new ObjectOutputStream(this.socket.getOutputStream());
                    objOutput.flush();
                    objInput = new ObjectInputStream(this.socket.getInputStream());

                } catch (Throwable throwable) {
                    throw new RMIException("unable to create object output stream", throwable);
                }

                String methodName = null;
                Class[] paramTypes = null;
                Object[] params = null;
                Method var6 = null;
                Object var7 = null;
                boolean var8 = true;

                try {
                    methodName = (String) objInput.readObject();
                } catch (Throwable throwable) {
                    this.feedback("unable to read method name", throwable, objOutput);
                }

                try {
                    paramTypes = (Class<T>[]) objInput.readObject();
                } catch (Throwable throwable) {
                    this.feedback("unable to read method parameter types", throwable, objOutput);
                }

                try {
                    params = (Object[])(objInput.readObject());
                } catch (Throwable throwable) {
                    this.feedback("unable to read method arguments", throwable, objOutput);
                }

                try {
                    var6 = Skeleton.this.remoteInterface.getMethod(methodName, paramTypes);
                } catch (Throwable throwable) {
                    this.feedback("method not found", throwable, objOutput);
                }

                try {
                    var7 = var6.invoke(Skeleton.this.server, params);
                } catch (InvocationTargetException e) {
                    var7 = e.getCause();
                    var8 = false;
                } catch (Throwable throwable) {
                    this.feedback("unable to invoke method", throwable, objOutput);
                }

                try {
                    objOutput.writeBoolean(var8);
                    objOutput.writeObject(var7);
                } catch (IOException e) {
                    throw new RMIException("unable to send result to peer", e);
                } catch (Throwable throwable) {
                    this.feedback("unable to send result to peer", throwable, objOutput);
                }
            } catch (RMIException var46) {
                Skeleton.this.service_error(var46);
            } finally {
                if (objOutput != null) {
                    try {
                        objOutput.flush();
                    } catch (Exception e) {
                    }

                    try {
                        objOutput.close();
                    } catch (Exception e) {
                    }
                }

                if (objInput != null) {
                    try {
                        objInput.close();
                    } catch (Exception e) {
                    }
                }

                Skeleton.forceClose(this.socket);
            }

        }

        protected void finalize() throws Throwable {
            try {
                Skeleton.forceClose(this.socket);
            } finally {
                super.finalize();
            }

        }
    }

    private static void forceClose(Socket socket) {
        try {
            socket.close();
        } catch (Throwable var2) {
        }

    }

    private static void forceClose(ServerSocket serverSocket) {
        try {
            serverSocket.close();
        } catch (Throwable var2) {
        }

    }
}
