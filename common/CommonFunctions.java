package common;

import rmi.RMIException;

import java.lang.reflect.Method;

/**
 * @author Ashwin Raghunath
 * @since 21-10-21
 * <h1>Class to have common functionalities like throwsRemoteException</h1>
 */
public class CommonFunctions {

    public static <T> boolean throwsRemoteException(Class<T> c){

        boolean found;

        Method[] methods = c.getDeclaredMethods();
        for (Method method : methods) {
            found=false;
            Class<?>[] exceptionTypes = method.getExceptionTypes();
            for (Class<?> exceptionType : exceptionTypes) {
                    if(exceptionType==RMIException.class)
                        found=true;
            }
            if(!found){
                return false;
            }
        }
        return true;
    }
}
