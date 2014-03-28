/*
 * 	This program is free software; you can redistribute it and/or modify it under the terms of 
 * the GNU AFFERO GENERAL PUBLIC LICENSE as published by the Free Software Foundation; either version 3 of the License, 
 * or (at your option) any later version. 
 * 
 * 	This program is distributed in the hope that it will be useful, 
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  
 * See the GNU AFFERO GENERAL PUBLIC LICENSE for more details. 
 * 	You should have received a copy of the GNU AFFERO GENERAL PUBLIC LICENSE along with this program; 
 * if not, write to the Free Software Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */
package com.meidusa.amoeba.exception;

import java.io.PrintStream;
import java.io.PrintWriter;

/**
 * 
 * @author <a href=mailto:piratebase@sina.com>Struct chen</a>
 * @version $Id: ConfigurationException.java 2418 2006-09-19 02:54:18Z struct $
 */
public class ConfigurationException extends RuntimeException {
	private static final long serialVersionUID = 1L;

	Throwable throwable;

	/**
	 * Constructs a <code>ConfigurationException</code> with no detail
	 * message.
	 */
	public ConfigurationException() {
	}

	/**
	 * Constructs a <code>ConfigurationException</code> with the specified
	 * detail message.
	 * 
	 * @param s  the detail message.
	 */
	public ConfigurationException(String s) {
		super(s);
	}

	/**
	 * Constructs a <code>ConfigurationException</code> with no detail
	 * message.
	 */
	public ConfigurationException(Throwable cause) {
		this.throwable = cause;
	}

	/**
	 * Constructs a <code>ConfigurationException</code> with the specified
	 * detail message.
	 * 
	 * @param s the detail message.
	 */
	public ConfigurationException(String s, Throwable cause) {
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
