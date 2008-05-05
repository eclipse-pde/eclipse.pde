/*******************************************************************************
 * Copyright (c) 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.api.tools.internal.comparator;

import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Locale;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

import org.eclipse.osgi.util.NLS;
import org.eclipse.pde.api.tools.internal.provisional.RestrictionModifiers;
import org.eclipse.pde.api.tools.internal.provisional.comparator.IDelta;
import org.eclipse.pde.api.tools.internal.util.Util;

import com.ibm.icu.text.MessageFormat;

/**
 * Class that manages the messages for compatible deltas.
 */
public class Messages extends NLS {
	/**
	 * The current mapping of problem id to message
	 */
	private static Hashtable fMessages = null;

	public static String problem_message_not_found;

	static {
		// initialize resource bundle
		NLS.initializeMessages("org.eclipse.pde.api.tools.internal.comparator.messages", Messages.class); //$NON-NLS-1$
	}

	private Messages() {
	}
	
	/**
	 * This method initializes the MessageTemplates class variable according
	 * to the current Locale.
	 * @param loc Locale
	 * @return HashtableOfInt
	 */
	public static Hashtable loadMessageTemplates(Locale loc) {
		ResourceBundle bundle = null;
		String bundleName = "org.eclipse.pde.api.tools.internal.comparator.compatible_delta_messages"; //$NON-NLS-1$
		try {
			bundle = ResourceBundle.getBundle(bundleName, loc); 
		} catch(MissingResourceException e) {
			System.out.println("Missing resource : " + bundleName.replace('.', '/') + ".properties for locale " + loc); //$NON-NLS-1$//$NON-NLS-2$
			throw e;
		}
		Hashtable templates = new Hashtable(700);
		Enumeration keys = bundle.getKeys();
		while (keys.hasMoreElements()) {
			String key = (String)keys.nextElement();
			try {
				int messageID = Integer.parseInt(key);
				templates.put(new Integer(messageID), bundle.getString(key));
			} catch(NumberFormatException e) {
				// key ill-formed
			} catch (MissingResourceException e) {
				// available ID
			}
		}
		return templates;
	}

	public static String getCompatibleLocalizedMessage(Delta delta) {
		if(fMessages == null) {
			fMessages = loadMessageTemplates(Locale.getDefault());
		}
		Integer key = new Integer(getKey(delta));
		String message = (String) fMessages.get(key);
		if(message == null) {
			return MessageFormat.format(Messages.problem_message_not_found, new String[] { String.valueOf(key)});
		}
		String[] arguments = delta.getArguments();
		if (arguments.length != 0)
			return MessageFormat.format(message, arguments);
		return message;
	}

	private static int getKey(IDelta delta) {
		switch(delta.getElementType()) {
			case IDelta.ANNOTATION_ELEMENT_TYPE :
				if (delta.getKind() == IDelta.ADDED && delta.getFlags() == IDelta.METHOD_WITH_DEFAULT_VALUE) {
					return 1;
				}
				break;
			case IDelta.API_COMPONENT_ELEMENT_TYPE :
				switch(delta.getKind()) {
					case IDelta.ADDED :
						switch(delta.getFlags()) {
							case IDelta.EXECUTION_ENVIRONMENT :
								return 2;
							case IDelta.TYPE :
								return 3;
						}
						break;
					case IDelta.CHANGED :
						if (delta.getFlags() == IDelta.TYPE_VISIBILITY) {
							return 4;
						}
						break;
					case IDelta.REMOVED :
						if (delta.getFlags() == IDelta.EXECUTION_ENVIRONMENT) {
							return 5;
						}
				}
				break;
			case IDelta.API_PROFILE_ELEMENT_TYPE :
				switch(delta.getKind()) {
					case IDelta.ADDED :
						if (delta.getFlags() == IDelta.API_COMPONENT) {
							return 6;
						}
				}
				break;
			case IDelta.CLASS_ELEMENT_TYPE :
				switch(delta.getKind()) {
					case IDelta.ADDED :
						switch(delta.getFlags()) {
							case IDelta.CLINIT :
								return 7;
							case IDelta.CONSTRUCTOR :
								return 8;
							case IDelta.FIELD :
								if (Util.isProtected(delta.getModifiers())) {
									return 9;
								}
								if (Util.isStatic(delta.getModifiers())) {
									return 10;
								}
								if (RestrictionModifiers.isExtendRestriction(delta.getRestrictions())) {
									return 11;
								}
								return 80;
							case IDelta.METHOD :
								if (!Util.isAbstract(delta.getModifiers())) {
									return 12;
								}
								return 13;
							case IDelta.OVERRIDEN_METHOD :
								return 14;
							case IDelta.SUPERCLASS :
								return 15;
							case IDelta.TYPE_MEMBER :
								return 16;
							case IDelta.TYPE_PARAMETERS :
								return 17;
							case IDelta.TYPE_ARGUMENTS :
								return 18;
						}
						break;
					case IDelta.CHANGED :
						switch(delta.getFlags()) {
							case IDelta.ABSTRACT_TO_NON_ABSTRACT :
								return 19;
							case IDelta.EXPANDED_SUPERCLASS_SET :
								return 20;
							case IDelta.EXPANDED_SUPERINTERFACES_SET :
								return 21;
							case IDelta.FINAL_TO_NON_FINAL :
								return 22;
							case IDelta.INCREASE_ACCESS :
								return 23;
							case IDelta.TYPE_PARAMETER_NAME :
								return 24;
						}
						break;
					case IDelta.REMOVED :
						switch(delta.getFlags()) {
							case IDelta.CLINIT :
								return 25;
							case IDelta.CONSTRUCTOR :
								return 26;
							case IDelta.FIELD_MOVED_UP :
								return 27;
							case IDelta.FIELD :
								if (Util.isProtected(delta.getModifiers())) {
									return 28;
								}
								return 29;
							case IDelta.METHOD_MOVED_UP :
								return 30;
							case IDelta.METHOD :
								if (Util.isProtected(delta.getModifiers())) {
									return 31;
								}
								return 32;
							case IDelta.TYPE_MEMBER :
								if (Util.isProtected(delta.getModifiers())) {
									return 33;
								}
								return 34;
						}
						break;
				}
				break;
			case IDelta.ENUM_ELEMENT_TYPE :
				switch(delta.getKind()) {
					case IDelta.ADDED :
						switch(delta.getFlags()) {
							case IDelta.CONSTRUCTOR :
								return 35;
							case IDelta.ENUM_CONSTANT :
								return 36;
							case IDelta.METHOD :
								return 37;
						}
						break;
					case IDelta.REMOVED :
						if (delta.getFlags() == IDelta.CONSTRUCTOR) {
							return 38;
						}
				}
				break;
			case IDelta.FIELD_ELEMENT_TYPE :
				switch(delta.getKind()) {
					case IDelta.ADDED :
						switch(delta.getFlags()) {
							case IDelta.TYPE_ARGUMENTS :
								return 18;
							case IDelta.VALUE :
								return 40;
						}
						break;
					case IDelta.CHANGED :
						switch(delta.getFlags()) {
							case IDelta.FINAL_TO_NON_FINAL_NON_STATIC :
								return 41;
							case IDelta.FINAL_TO_NON_FINAL_STATIC_CONSTANT :
								if (Util.isProtected(delta.getModifiers())) {
									return 42;
								}
								return 43;
							case IDelta.FINAL_TO_NON_FINAL_STATIC_NON_CONSTANT :
								return 44;
							case IDelta.INCREASE_ACCESS :
								return 45;
							case IDelta.NON_FINAL_TO_FINAL :
								return 46;
							case IDelta.NON_TRANSIENT_TO_TRANSIENT :
								return 47;
							case IDelta.TRANSIENT_TO_NON_TRANSIENT :
								return 48;
							case IDelta.TYPE_PARAMETER_NAME :
								return 49;
							case IDelta.VALUE :
								if (Util.isProtected(delta.getModifiers())) {
									return 50;
								}
								return 51;
							case IDelta.TYPE :
								return 78;
						}
						break;
					case IDelta.REMOVED :
						if (delta.getFlags() == IDelta.VALUE) {
							if (Util.isProtected(delta.getModifiers())) {
								return 52;
							}
							return 53;
						}
				}
				break;
			case IDelta.INTERFACE_ELEMENT_TYPE :
				switch(delta.getKind()) {
					case IDelta.ADDED :
						switch(delta.getFlags()) {
							case IDelta.FIELD :
								return 54;
							case IDelta.METHOD :
								return 55;
							case IDelta.TYPE_MEMBER :
								return 56;
							case IDelta.OVERRIDEN_METHOD :
								return 57;
							case IDelta.TYPE_PARAMETERS :
								return 58;
							case IDelta.TYPE_ARGUMENTS :
								return 18;
						}
						break;
					case IDelta.CHANGED :
						switch(delta.getFlags()) {
							case IDelta.TYPE_PARAMETER_NAME :
								return 59;
							case IDelta.EXPANDED_SUPERINTERFACES_SET :
								return 79;
						}
						break;
					case IDelta.REMOVED :
						switch(delta.getFlags()) {
							case IDelta.FIELD_MOVED_UP :
								return 60;
							case IDelta.METHOD_MOVED_UP :
								return 61;
						}
				}
				break;
			case IDelta.METHOD_ELEMENT_TYPE :
				switch(delta.getKind()) {
					case IDelta.ADDED :
						switch(delta.getFlags()) {
							case IDelta.ANNOTATION_DEFAULT_VALUE :
								return 62;
							case IDelta.CHECKED_EXCEPTION :
								return 63;
							case IDelta.TYPE_PARAMETERS :
								return 64;
							case IDelta.UNCHECKED_EXCEPTION :
								return 65;
							case IDelta.TYPE_ARGUMENTS :
								return 18;
						}
						break;
					case IDelta.CHANGED :
						switch(delta.getFlags()) {
							case IDelta.ABSTRACT_TO_NON_ABSTRACT :
								return 66;
							case IDelta.ANNOTATION_DEFAULT_VALUE :
								return 67;
							case IDelta.ARRAY_TO_VARARGS :
								return 68;
							case IDelta.FINAL_TO_NON_FINAL :
								return 69;
							case IDelta.INCREASE_ACCESS :
								return 70;
							case IDelta.NATIVE_TO_NON_NATIVE :
								return 71;
							case IDelta.NON_NATIVE_TO_NATIVE :
								return 72;
							case IDelta.NON_SYNCHRONIZED_TO_SYNCHRONIZED :
								return 73;
							case IDelta.SYNCHRONIZED_TO_NON_SYNCHRONIZED :
								return 74;
							case IDelta.TYPE_PARAMETER_NAME :
								return 75;
							case IDelta.NON_FINAL_TO_FINAL :
								if (RestrictionModifiers.isExtendRestriction(delta.getRestrictions())) {
									return 81;
								}
								if (RestrictionModifiers.isOverrideRestriction(delta.getRestrictions())) {
									return 82;
								}
								return 83;
							case IDelta.NON_STATIC_TO_STATIC :
								return 84;
						}
					case IDelta.REMOVED :
						switch(delta.getFlags()) {
							case IDelta.CHECKED_EXCEPTION :
								return 76;
							case IDelta.UNCHECKED_EXCEPTION :
								return 77;
						}
					}
				break;
		}
		return 0;
	}
}
