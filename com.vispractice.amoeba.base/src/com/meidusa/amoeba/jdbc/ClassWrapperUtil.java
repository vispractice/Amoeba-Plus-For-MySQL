package com.meidusa.amoeba.jdbc;

import java.lang.reflect.Method;

public class ClassWrapperUtil {
  @SuppressWarnings("unchecked")
  public static <T> T invoke(Object obje,Class<T> returnClass ,String methodName,Object[] parameters,Class... parameterTypes){
      try {
          Method method = obje.getClass().getMethod(methodName, parameterTypes);
          return (T)method.invoke(obje, parameters);
      } catch (Exception e) {
          throw new UnsupportedOperationException(e);
      }
  }
}
