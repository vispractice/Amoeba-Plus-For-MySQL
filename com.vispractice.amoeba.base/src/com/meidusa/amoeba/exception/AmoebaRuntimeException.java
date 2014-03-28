package com.meidusa.amoeba.exception;

import java.io.PrintStream;
import java.io.PrintWriter;

public class AmoebaRuntimeException extends RuntimeException{
  private static final long serialVersionUID = 1L;

  Throwable throwable;

  /**
   * Constructs a <code>AmoebaRuntimeException</code> with no detail
   * message.
   */
  public AmoebaRuntimeException() {
  }

  /**
   * Constructs a <code>AmoebaRuntimeException</code> with the specified
   * detail message.
   * 
   * @param s  the detail message.
   */
  public AmoebaRuntimeException(String s) {
      super(s);
  }

  /**
   * Constructs a <code>AmoebaRuntimeException</code> with no detail
   * message.
   */
  public AmoebaRuntimeException(Throwable cause) {
      this.throwable = cause;
  }

  /**
   * Constructs a <code>AmoebaRuntimeException</code> with the specified
   * detail message.
   * 
   * @param s the detail message.
   */
  public AmoebaRuntimeException(String s, Throwable cause) {
      super(s);
      this.throwable = cause;
  }

  public Throwable getThrowable() {
      return throwable;
  }

  /**
   * Prints this <code>Throwable</code> and its backtrace to the specified
   * print stream.
   * 
   * @param s   <code>PrintStream</code> to use for output
   */
  public void printStackTrace(PrintStream s) {
      super.printStackTrace(s);

      if (throwable != null) {
          s.println("with nested exception " + throwable);
          throwable.printStackTrace(s);
      }
  }

  /**
   * Prints this <code>Throwable</code> and its backtrace to the specified
   * print writer.
   * 
   * @param s  <code>PrintWriter</code> to use for output
   * @since JDK1.1
   */
  public void printStackTrace(PrintWriter s) {
      super.printStackTrace(s);

      if (throwable != null) {
          s.println("with nested exception " + throwable);
          throwable.printStackTrace(s);
      }
  }

  public String toString() {
      if (throwable == null) {
          return super.toString();
      }

      return super.toString() + "\n    with nested exception \n"
              + throwable.toString();
  }
}
