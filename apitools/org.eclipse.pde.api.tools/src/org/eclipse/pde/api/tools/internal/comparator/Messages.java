/*******************************************************************************
 * Copyright (c) 2008, 2010 IBM Corporation and others.
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

import org.eclipse.jdt.core.Flags;
import org.eclipse.osgi.util.NLS;
import org.eclipse.pde.api.tools.internal.provisional.RestrictionModifiers;
import org.eclipse.pde.api.tools.internal.provisional.comparator.IDelta;

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
				switch(delta.getKind()) {
					case IDelta.ADDED :
						switch(delta.getFlags()) {
							case IDelta.METHOD_WITH_DEFAULT_VALUE :
								return 1;
							case IDelta.DEPRECATION :
								return 110;
						}
						break;
					case IDelta.CHANGED :
						switch(delta.getFlags()) {
							case IDelta.DECREASE_ACCESS :
								return 101;
						}
						break;
					case IDelta.REMOVED :
						switch(delta.getFlags()) {
							case IDelta.DEPRECATION :
								return 111;
						}
						break;
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
							case IDelta.REEXPORTED_TYPE :
								return 109;
						}
						break;
					case IDelta.CHANGED :
						switch(delta.getFlags()) {
							case IDelta.TYPE_VISIBILITY : return 4;
							case IDelta.MAJOR_VERSION : return 95;
							case IDelta.MINOR_VERSION : return 96;
						}
						break;
					case IDelta.REMOVED :
						if (delta.getFlags() == IDelta.EXECUTION_ENVIRONMENT) {
							return 5;
						}
				}
				break;
			case IDelta.API_BASELINE_ELEMENT_TYPE :
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
							case IDelta.DEPRECATION :
								return 110;
							case IDelta.CLINIT :
								return 7;
							case IDelta.CONSTRUCTOR :
								return 8;
							case IDelta.FIELD :
								if (Flags.isProtected(delta.getNewModifiers())) {
									return 9;
								}
								if (Flags.isStatic(delta.getNewModifiers())) {
									return 10;
								}
								if (Flags.isPublic(delta.getNewModifiers())) {
									return 11;
								}
								return 80;
							case IDelta.METHOD :
								if (!Flags.isAbstract(delta.getNewModifiers())) {
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
							case IDelta.METHOD_MOVED_DOWN :
								return 97;
							case IDelta.RESTRICTIONS :
								return 108;
						}
						break;
					case IDelta.CHANGED :
						switch(delta.getFlags()) {
							case IDelta.ABSTRACT_TO_NON_ABSTRACT :
								return 19;
							case IDelta.EXPANDED_SUPERINTERFACES_SET :
								return 21;
							case IDelta.FINAL_TO_NON_FINAL :
								return 22;
							case IDelta.INCREASE_ACCESS :
								return 23;
							case IDelta.NON_FINAL_TO_FINAL :
								return 94;
							case IDelta.DECREASE_ACCESS :
								return 101;
							case IDelta.NON_ABSTRACT_TO_ABSTRACT :
								return 105;
						}
						break;
					case IDelta.REMOVED :
						switch(delta.getFlags()) {
							case IDelta.DEPRECATION :
								return 111;
							case IDelta.CLINIT :
								return 25;
							case IDelta.CONSTRUCTOR :
							case IDelta.API_CONSTRUCTOR :
								return 26;
							case IDelta.FIELD_MOVED_UP :
								return 27;
							case IDelta.FIELD :
							case IDelta.API_FIELD :
								if (Flags.isProtected(delta.getOldModifiers())) {
									return 28;
								}
								return 29;
							case IDelta.METHOD_MOVED_UP :
								return 30;
							case IDelta.METHOD :
							case IDelta.API_METHOD :
								if (Flags.isProtected(delta.getOldModifiers())) {
									return 31;
								}
								return 32;
							case IDelta.TYPE_MEMBER :
								if (Flags.isProtected(delta.getOldModifiers())) {
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
							case IDelta.DEPRECATION :
								return 110;
						}
						break;
					case IDelta.REMOVED :
						switch(delta.getFlags()) {
							case IDelta.CONSTRUCTOR :
							case IDelta.API_CONSTRUCTOR :
								return 38;
							case IDelta.DEPRECATION :
								return 111;
						}
						break;
					case IDelta.CHANGED :
						switch(delta.getFlags()) {
							case IDelta.DECREASE_ACCESS :
								return 101;
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
							case IDelta.DEPRECATION :
								return 110;
						}
						break;
					case IDelta.CHANGED :
						switch(delta.getFlags()) {
							case IDelta.FINAL_TO_NON_FINAL_NON_STATIC :
								return 41;
							case IDelta.FINAL_TO_NON_FINAL_STATIC_CONSTANT :
								if (Flags.isProtected(delta.getNewModifiers())) {
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
							case IDelta.VALUE :
								if (Flags.isProtected(delta.getNewModifiers())) {
									return 50;
								}
								return 51;
							case IDelta.TYPE :
								return 78;
							case IDelta.NON_VOLATILE_TO_VOLATILE :
								return 92;
							case IDelta.VOLATILE_TO_NON_VOLATILE :
								return 93;
							case IDelta.DECREASE_ACCESS :
								return 98;
							case IDelta.NON_STATIC_TO_STATIC :
								return 103;
							case IDelta.STATIC_TO_NON_STATIC :
								return 104;
							}
						break;
					case IDelta.REMOVED :
						switch(delta.getFlags()) {
							case IDelta.VALUE :
								if (Flags.isProtected(delta.getNewModifiers())) {
									return 52;
								}
								return 53;
							case IDelta.DEPRECATION :
								return 111;
						}
				}
				break;
			case IDelta.TYPE_PARAMETER_ELEMENT_TYPE :
				if (delta.getKind() == IDelta.CHANGED && delta.getFlags() == IDelta.TYPE_PARAMETER_NAME) {
					return 24;
				}
				break;
			case IDelta.INTERFACE_ELEMENT_TYPE :
				switch(delta.getKind()) {
					case IDelta.ADDED :
						switch(delta.getFlags()) {
							case IDelta.DEPRECATION :
								return 110;
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
							case IDelta.METHOD_MOVED_DOWN :
								return 97;
							case IDelta.SUPER_INTERFACE_WITH_METHODS :
								return 107;
						}
						break;
					case IDelta.CHANGED :
						switch(delta.getFlags()) {
							case IDelta.EXPANDED_SUPERINTERFACES_SET :
								return 79;
							case IDelta.DECREASE_ACCESS :
								return 101;
						}
						break;
					case IDelta.REMOVED :
						switch(delta.getFlags()) {
							case IDelta.FIELD_MOVED_UP :
								return 60;
							case IDelta.METHOD_MOVED_UP :
								return 61;
							case IDelta.DEPRECATION :
								return 111;
						}
				}
				break;
			case IDelta.METHOD_ELEMENT_TYPE :
				switch(delta.getKind()) {
					case IDelta.ADDED :
						switch(delta.getFlags()) {
							case IDelta.DEPRECATION :
								return 110;
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
							case IDelta.RESTRICTIONS :
								return 106;
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
							case IDelta.NON_FINAL_TO_FINAL :
								if (RestrictionModifiers.isExtendRestriction(delta.getCurrentRestrictions())) {
									return 81;
								}
								if (RestrictionModifiers.isOverrideRestriction(delta.getCurrentRestrictions())) {
									return 82;
								}
								return 83;
							case IDelta.NON_STATIC_TO_STATIC :
								return 84;
							case IDelta.DECREASE_ACCESS :
								return 99;
							case IDelta.STATIC_TO_NON_STATIC :
								return 102;
						}
						break;
					case IDelta.REMOVED :
						switch(delta.getFlags()) {
							case IDelta.CHECKED_EXCEPTION :
								return 76;
							case IDelta.UNCHECKED_EXCEPTION :
								return 77;
							case IDelta.DEPRECATION :
								return 111;
						}
					}
				break;
			case IDelta.CONSTRUCTOR_ELEMENT_TYPE :
				switch(delta.getKind()) {
					case IDelta.ADDED :
						switch(delta.getFlags()) {
							case IDelta.DEPRECATION :
								return 110;
							case IDelta.CHECKED_EXCEPTION :
								return 85;
							case IDelta.TYPE_PARAMETERS :
								return 64;
							case IDelta.UNCHECKED_EXCEPTION :
								return 86;
							case IDelta.TYPE_ARGUMENTS :
								return 18;
						}
						break;
					case IDelta.CHANGED :
						switch(delta.getFlags()) {
							case IDelta.ARRAY_TO_VARARGS :
								return 87;
							case IDelta.INCREASE_ACCESS :
								return 89;
							case IDelta.DECREASE_ACCESS :
								return 100;
						}
						break;
					case IDelta.REMOVED :
						switch(delta.getFlags()) {
							case IDelta.CHECKED_EXCEPTION :
								return 90;
							case IDelta.UNCHECKED_EXCEPTION :
								return 91;
							case IDelta.DEPRECATION :
								return 111;
						}
					}
				break;
		}
		return 0;
	}
}
