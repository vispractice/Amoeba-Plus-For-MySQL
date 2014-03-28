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
package com.meidusa.amoeba.mysql.jdbc;

import java.text.MessageFormat;
import java.util.Locale;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

/**
 * 
 * @author <a href=mailto:piratebase@sina.com>Struct chen</a>
 *
 */
public class Messages {

	private static final String BUNDLE_NAME = "com.meidusa.amoeba.mysql.jdbc.LocalizedErrorMessages";

	private static final ResourceBundle RESOURCE_BUNDLE;

	static {
		ResourceBundle temp = null;
		try {
			temp = ResourceBundle.getBundle(BUNDLE_NAME, Locale.getDefault(),
					Messages.class.getClassLoader());
		} catch (Throwable t) {
			try {
				temp = ResourceBundle.getBundle(BUNDLE_NAME);
			} catch (Throwable t2) {
				throw new RuntimeException(
						"Can't load resource bundle due to underlying exception "
								+ t.toString());
			}
		} finally {
			RESOURCE_BUNDLE = temp;
		}
	}

	/**
	 * Returns the localized message for the given message key
	 * 
	 * @param key the message key
	 * @return The localized message for the key
	 */
	public static String getString(String key) {
		if (RESOURCE_BUNDLE == null) {
			throw new RuntimeException(
					"Localized messages from resource bundle '" + BUNDLE_NAME
							+ "' not loaded during initialization.");
		}

		try {
			if (key == null) {
				throw new IllegalArgumentException("Message key can not be null");
			}

			String message = RESOURCE_BUNDLE.getString(key);

			if (message == null) {
				message = "Missing error message for key '" + key + "'";
			}

			return message;
		} catch (MissingResourceException e) {
			return '!' + key + '!';
		}
	}

	public static String getString(String key, Object[] args) {
		return MessageFormat.format(getString(key), args);
	}

	private Messages() {

	}
}
