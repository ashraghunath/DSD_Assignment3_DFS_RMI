package common;

import rmi.RMIException;

import java.lang.reflect.Array;
import java.lang.reflect.Method;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Arrays;

public class CommonFunctions {

    public static <T> boolean throwsRemoteException(Class<T> c){
//        ArrayList exceptions = new ArrayList();
//
//        Method[] methods = c.getMethods();
//        for (Method method : methods) {
//            Class<?>[] exceptionTypes = method.getExceptionTypes();
//            for (Class<?> exceptionType : exceptionTypes) {
//                    exceptions.add(exceptionType);
//            }
//            if(!exceptions.contains(RemoteException.class)){
//                return false;
//            }
//            exceptions.clear();
//        }
//        return true;

        for (Method myMethod : c.getDeclaredMethods()) {
            Class<?>[] exceptions = myMethod.getExceptionTypes();
            if (!Arrays.asList(exceptions).contains(RMIException.class)) {
                return false;
            }
        }
        return true;
    }
}
