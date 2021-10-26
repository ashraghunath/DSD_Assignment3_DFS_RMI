package naming;

import java.io.*;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;

import rmi.*;
import common.*;
import storage.*;

/** Naming server.

    <p>
    Each instance of the filesystem is centered on a single naming server. The
    naming server maintains the filesystem directory tree. It does not store any
    file data - this is done by separate storage servers. The primary purpose of
    the naming server is to map each file name (path) to the storage server
    which hosts the file's contents.

    <p>
    The naming server provides two interfaces, <code>Service</code> and
    <code>Registration</code>, which are accessible through RMI. Storage servers
    use the <code>Registration</code> interface to inform the naming server of
    their existence. Clients use the <code>Service</code> interface to perform
    most filesystem operations. The documentation accompanying these interfaces
    provides details on the methods supported.

    <p>
    Stubs for accessing the naming server must typically be created by directly
    specifying the remote network address. To make this possible, the client and
    registration interfaces are available at well-known ports defined in
    <code>NamingStubs</code>.
 */
public class NamingServer implements Service, Registration
{

    private boolean started = false;
    private boolean stopping = false;
    private Skeleton<Registration> registrationSkeleton = null;
    private Skeleton<Service> serviceSkeleton = null;
    private Random random = null;
    private List<ServerStub> serverStubList = null;
    private HashTree tree = null;

    /** Creates the naming server object.

        <p>
        The naming server is not started.
     */
    public NamingServer()
    {
        serverStubList = new LinkedList<>();
        random =  new Random();

    }

    /** Starts the naming server.

        <p>
        After this method is called, it is possible to access the client and
        registration interfaces of the naming server remotely.

        @throws RMIException If either of the two skeletons, for the client or
                             registration server interfaces, could not be
                             started. The user should not attempt to start the
                             server again if an exception occurs.
     */
    public synchronized void start() throws RMIException
    {
        if (started)
        {
            throw new RMIException("Naming server running");
        }

        if (stopping)
        {
            throw new RMIException("Naming server stopping");
        }

        serviceSkeleton = new Skeleton<>(Service.class, this, new InetSocketAddress(NamingStubs.SERVICE_PORT));

        registrationSkeleton = new Skeleton<>(Registration.class, this, new InetSocketAddress(NamingStubs.REGISTRATION_PORT));

        serviceSkeleton.start();
        registrationSkeleton.start();

        started = true;
    }

    /** Stops the naming server.

        <p>
        This method waits for both the client and registration interface
        skeletons to stop. It attempts to interrupt as many of the threads that
        are executing naming server code as possible. After this method is
        called, the naming server is no longer accessible remotely. The naming
        server should not be restarted.
     */
    public void stop()
    {
        synchronized(this)
        {
            stopping = true;
        }
        try
        {
            serviceSkeleton.stop();
            registrationSkeleton.stop();

            synchronized(this)
            {
                started = false;
                stopping = false;
            }
            stopped(null);
        }
        catch (Throwable throwableStop)
        {
            stopped(throwableStop);
        }
    }

    /** Indicates that the server has completely shut down.

        <p>
        This method should be overridden for error reporting and application
        exit purposes. The default implementation does nothing.

        @param cause The cause for the shutdown, or <code>null</code> if the
                     shutdown was by explicit user request.
     */
    protected void stopped(Throwable cause)
    {
    }

    // The following methods are documented in Service.java.
    @Override
    public boolean isDirectory(Path path) throws FileNotFoundException
    {
        return this.tree.isDirectory(path);
    }

    @Override
    public String[] list(Path directory) throws FileNotFoundException
    {
        return this.tree.list(directory);
    }

    @Override
    public boolean createFile(Path file)
        throws RMIException, FileNotFoundException
    {
        if(file.isRoot())
            return false;

        ServerStub serverStub = serverStubList.get(random.nextInt(serverStubList.size()));


        if(!tree.isDirectory(file.parent()))
        {
            throw new FileNotFoundException("The parent of "+file.toString()+" is not a directory.");
        }

        boolean success = tree.createFile(file, serverStub);

        if(!success)
            return false;

        success = serverStub.commandStub.create(file);

        if(!success)
        {
            this.tree.delete(file);
        }

        return success;
    }

    @Override
    public boolean createDirectory(Path directory) throws FileNotFoundException
    {
        if(directory.isRoot())
            return false;

        if(!this.tree.isDirectory(directory.parent()))
        {
            throw new FileNotFoundException("The parent of " + directory.toString() + " is not a directory.");
        }

        return this.tree.createDirectory(directory);
    }

    @Override
    public boolean delete(Path path) throws FileNotFoundException
    {
        return (!path.isRoot()) && this.tree.delete(path);
    }

    @Override
    public Storage getStorage(Path file) throws FileNotFoundException
    {
        return this.tree.getStorage(file).storageStub;
    }

    // The method register is documented in Registration.java.
    @Override
    public Path[] register(Storage client_stub, Command command_stub,
                           Path[] files)
    {
        if (client_stub == null || command_stub == null || files == null)
        {
            throw new NullPointerException("Registering with null arguments");
        }

        ArrayList<Path> deleteList = new ArrayList<>();
        ServerStub newStub = new ServerStub(command_stub, client_stub);

        for (ServerStub s : serverStubList)
        {
            if (newStub.equals(s))
            {
                throw new IllegalStateException("Duplicate storage server registration");
            }
        }

        synchronized (serverStubList)
        {
            serverStubList.add(newStub);
        }

        for (Path path : files)
        {
            if (!path.isRoot() && !this.tree.createFileRecursive(path, newStub))
            {
                deleteList.add(path);
            }
        }

        Path[] deleteArray = new Path[deleteList .size()];
        deleteArray = deleteList.toArray(deleteArray);

        return deleteArray;
    }
}


class ServerStub{
    public Command commandStub;
    public Storage storageStub;

    public ServerStub(Command commandStub, Storage storageStub) {
        this.commandStub = commandStub;
        this.storageStub = storageStub;
    }

    public boolean equals(ServerStub serverStub) {
        return storageStub.equals(serverStub.storageStub) && commandStub.equals(serverStub.commandStub);
    }
}
