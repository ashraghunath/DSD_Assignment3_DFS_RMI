package common;

import java.io.*;
import java.util.*;

/** Distributed filesystem paths.

    <p>
    Objects of type <code>Path</code> are used by all filesystem interfaces.
    Path objects are immutable.

    <p>
    The string representation of paths is a forward-slash-delimeted sequence of
    path components. The root directory is represented as a single forward
    slash.

    <p>
    The colon (<code>:</code>) and forward slash (<code>/</code>) characters are
    not permitted within path components. The forward slash is the delimeter,
    and the colon is reserved as a delimeter for application use.
 */
public class Path implements Iterable<String>, Serializable
{
    private String filePath;
    private List<String> componentList;


    /** Creates a new path which represents the root directory. */
    public Path()
    {
        String rootPath = "/";
        setValues(rootPath);
    }

    /** Creates a new path by appending the given component to an existing path.

     @param path The existing path.
     @param component The new component.
     @throws IllegalArgumentException If <code>component</code> includes the
     separator, a colon, or
     <code>component</code> is the empty
     string.
     */
    public Path(Path path, String component)
    {
        if(!Objects.nonNull(component) || component.isEmpty() || !isValidComponent(component))
            throw new IllegalArgumentException("invalid component string");

        String newPath = path.filePath + "/" + component;
        setValues(newPath);
    }

    /** Creates a new path from a path string.

     <p>
     The string is a sequence of components delimited with forward slashes.
     Empty components are dropped. The string must begin with a forward
     slash.

     @param path The path string.
     @throws IllegalArgumentException If the path string does not begin with
     a forward slash, or if the path
     contains a colon character.
     */
    public Path(String path)
    {
        setValues(path);
    }




    /** Returns an iterator over the components of the path.

     <p>
     The iterator cannot be used to modify the path object - the
     <code>remove</code> method is not supported.

     @return The iterator.
     */
    @Override
    public Iterator<String> iterator()
    {
        return new PathIterator(componentList.iterator());
    }

    private class PathIterator implements Iterator<String>
    {
        private Iterator<String> iterator;

        public PathIterator(Iterator<String> iterator)
        {
            this.iterator = iterator;
        }

        @Override
        public boolean hasNext()
        {
            return iterator.hasNext();
        }

        @Override
        public String next()
        {
            return iterator.next();
        }

    }


    /** Lists the paths of all files in a directory tree on the local
     filesystem.

     @param directory The root directory of the directory tree.
     @return An array of relative paths, one for each file in the directory
     tree.
     @throws FileNotFoundException If the root directory does not exist.
     @throws IllegalArgumentException If <code>directory</code> exists but
     does not refer to a directory.
     */
    public static Path[] list(File directory) throws FileNotFoundException
    {
        if (directory == null)
        {
            throw new FileNotFoundException("directory parameter is null");
        }

        if (!directory.exists())
        {
            throw new FileNotFoundException(directory.getPath() + "directory non existent");
        }

        if (!directory.isDirectory())
        {
            throw new IllegalArgumentException(directory.getPath() +  "is not a directory");
        }

        List<String> files = getAllFiles(directory, "");
        Path[]       paths = new Path[files.size()];


        int startInd = directory.getName().length() + 1;

        for (int i =0 ; i < files.size() ; i++)
        {
            paths[i] = new Path(files.get(i).substring(startInd));
        }

        return paths;
    }

    private static List<String> getAllFiles(File file, String path)
    {
        List<String> files = new ArrayList<>();

        if (file.isDirectory())
        {
            String exPath = path + "/" + file.getName();
            for (File f : file.listFiles())
            {
                files.addAll(getAllFiles(f, exPath));
            }
        }
        else
        {

            files.add(path + "/" + file.getName());
        }

        return files;
    }

    /** Determines whether the path represents the root directory.

     @return <code>true</code> if the path does represent the root directory,
     and <code>false</code> if it does not.
     */
    public boolean isRoot()
    {
        return (filePath.equals("/"));
    }

    /** Returns the path to the parent of this path.

     @throws IllegalArgumentException If the path represents the root
     directory, and therefore has no parent.
     */
    public Path parent()
    {
        if (isRoot())
        {
            throw new IllegalArgumentException("Root cannot have parent");
        }

        StringBuffer sBuffer = new StringBuffer("/");

        for (int i = 0 ; i < componentList.size() - 1; i++)
        {
            sBuffer.append("/").append(componentList.get(i));
        }

        return new Path(sBuffer.toString());
    }

    /** Returns the last component in the path.

     @throws IllegalArgumentException If the path represents the root
     directory, and therefore has no last
     component.
     */
    public String last()
    {
        if (isRoot())
        {
            throw new IllegalArgumentException("Root cannot have parent");
        }

        return componentList.get(componentList.size() - 1);
    }

    /** Determines if the given path is a subpath of this path.

     <p>
     The other path is a subpath of this path if is a prefix of this path.
     Note that by this definition, each path is a subpath of itself.

     @param other The path to be tested.
     @return <code>true</code> If and only if the other path is a subpath of
     this path.
     */
    public boolean isSubpath(Path other)
    {
        return (filePath.contains(other.filePath));
    }

    /** Converts the path to <code>File</code> object.

     @param root The resulting <code>File</code> object is created relative
     to this directory.
     @return The <code>File</code> object.
     */
    public File toFile(File root)
    {
        if (root != null)
        {
            return new File(root, this.toString());
        }
        else {
            return new File(this.toString());
        }
    }

    /** Compares two paths for equality.

     <p>
     Two paths are equal if they share all the same components.

     @param other The other path.
     @return <code>true</code> if and only if the two paths are equal.
     */
    @Override
    public boolean equals(Object other)
    {
        if (!(other instanceof Path))
        {
            return false;
        }

        return filePath.equals(((Path)other).filePath);
    }

    /** Returns the hash code of the path. */
    @Override
    public int hashCode()
    {
        return filePath.hashCode();
    }

    //Checks if component has separators
    private boolean isValidComponent(String component) {
        for (char c : component.toCharArray()) {
            if(c=='/' || c==':')
                return false;
        }
        return true;
    }

    private void setValues(String path)
    {
        if (path == null)
        {
            throw new IllegalArgumentException("Path is null");
        }

        if (path.length() == 0)
        {
            throw new IllegalArgumentException("Path is empty");
        }

        if (!path.startsWith("/"))
        {
            throw new IllegalArgumentException("Path doesn't start with /");
        }

        if (path.contains(":"))
        {
            throw new IllegalArgumentException("illegal character in path");
        }

        StringBuffer sBuffer = new StringBuffer("/");
        componentList = new ArrayList<>();

        String[] components  = path.split("/");

        for (String c : components)
        {
            if (c.length() == 0)
            {
                continue;
            }
            componentList.add(c);
            if (sBuffer.charAt(sBuffer.length() - 1) != '/')
            {
                sBuffer.append('/');
            }
            sBuffer.append(c);
        }

        filePath = sBuffer.toString();

    }
    /** Converts the path to a string.

     <p>
     The string may later be used as an argument to the
     <code>Path(String)</code> constructor.

     @return The string representation of the path.
     */
    @Override
    public String toString()
    {
        return filePath;
    }
}
