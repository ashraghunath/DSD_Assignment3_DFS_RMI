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
    private ArrayList<String> path;
    public static final String DELIMITER = "/";
    /** Creates a new path which represents the root directory. */
    public Path()
    {
        this.path = new ArrayList<String>();
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
        if(component==null || component.length()==0 || !isValidComponent(component))
            throw new IllegalArgumentException("invalid component string");
        Iterator<String> pathIt = path.iterator();
        this.path = new ArrayList<String>();
        while (pathIt.hasNext()) {
            this.path.add(pathIt.next());
        }
        this.path.add(component);

    }

    //Checks if component has separators
    private boolean isValidComponent(String component) {
        for (char c : component.toCharArray()) {
            if(c=='/' || c==':')
                return false;
        }
        return true;
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

        if (path == null)
            throw new IllegalArgumentException("The path given was null.");
        if (path.length() == 0 || path.charAt(0) != '/')
            throw new IllegalArgumentException("The path did not start with /.");
        this.path = new ArrayList<String>();
        for (String s : path.split("/")) {
            if (!isValidComponent(s)) {
                throw new IllegalArgumentException("The path had / or :.");
            } else if (s.length() == 0) {
                continue;
            } else {
                this.path.add(s);
            }
        }
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
        return new Iterator<String>() {
            private int i = 0;

            public boolean hasNext() {
                if (i >= path.size())
                    return false;
                return true;
            }

            public String next() {
                String temp = "";
                if (i < path.size())
                    temp = path.get(i);
                else {
                    throw new NoSuchElementException(
                            "Trying to advance past end of iterator.");
                }
                i++;
                return temp;
            }

        };
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

        ArrayList<Path> paths = listHelper(directory, directory.getPath()
                .length());
        return paths.toArray(new Path[paths.size()]);
    }

    /** Determines whether the path represents the root directory.

        @return <code>true</code> if the path does represent the root directory,
                and <code>false</code> if it does not.
     */
    public boolean isRoot()
    {
        return this.path.isEmpty();
    }

    /** Returns the path to the parent of this path.

        @throws IllegalArgumentException If the path represents the root
                                         directory, and therefore has no parent.
     */
    public Path parent()
    {
        if(isRoot())
        {
            throw new IllegalArgumentException("Root has no parent ");
        }
        String parentPath = "";
        for(int index=0; index<this.path.size()-1; index++)
        {
            parentPath+="/"+this.path.get(0);
        }
        return new Path(parentPath);
    }

    /** Returns the last component in the path.

        @throws IllegalArgumentException If the path represents the root
                                         directory, and therefore has no last
                                         component.
     */
    public String last()
    {
        if (this.isRoot())
            throw new IllegalArgumentException(
                    "Current path is the root.(Thrown from last())");
        return this.path.get(this.path.size() - 1);
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
        String thisPath = this.toString();
        String otherPath = other.toString();
        return thisPath.startsWith(otherPath);
    }

    /** Converts the path to <code>File</code> object.

        @param root The resulting <code>File</code> object is created relative
                    to this directory.
        @return The <code>File</code> object.
     */
    public File toFile(File root)
    {
        throw new UnsupportedOperationException("not implemented");
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
        return this.toString().equals(other.toString());
    }

    /** Returns the hash code of the path. */
    @Override
    public int hashCode()
    {
        return this.toString().hashCode();
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
        if (this.path.size() == 0)
            return "/";

        String path = "";
        for (int i = 0; i < this.path.size(); i++) {
            path += "/" + this.path.get(i);
        }
        return path;
    }

    private static ArrayList<Path> listHelper(File directory, int parentLength) {
        ArrayList<Path> paths = new ArrayList<Path>();

        for (File f : directory.listFiles()) {
            if (f.isFile()) {
                paths.add(new Path(f.getPath().substring(parentLength)));
            } else if (f.isDirectory()) {
                for (Path p : listHelper(f, parentLength))
                    paths.add(p);
            }
        }

        Collections.reverse(paths);
        return paths;
    }


}
