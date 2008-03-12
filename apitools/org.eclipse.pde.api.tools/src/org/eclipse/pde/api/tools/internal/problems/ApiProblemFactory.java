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
package org.eclipse.pde.api.tools.internal.problems;

import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Locale;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

import org.eclipse.core.resources.IMarker;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.pde.api.tools.internal.builder.BuilderMessages;
import org.eclipse.pde.api.tools.internal.provisional.comparator.IDelta;
import org.eclipse.pde.api.tools.internal.provisional.descriptors.IElementDescriptor;
import org.eclipse.pde.api.tools.internal.provisional.problems.IApiProblem;
import org.eclipse.pde.api.tools.internal.provisional.problems.IApiProblemTypes;

import com.ibm.icu.text.MessageFormat;

/**
 * Factory for creating {@link IApiProblem}s
 * 
 * @since 1.0.0
 */
public class ApiProblemFactory {
	
	/**
	 * The current mapping of problem id to message
	 */
	private static Hashtable fMessages = null;
	
	/**
	 * Creates a new {@link IApiProblem}
	 * @param resourcepath the path to the resource this problem was found in
	 * @param messageargs listing of arguments to pass in to the localized message.
	 * The arguments are passed into the string in the order they appear in the array.
	 * @param argumentids the ids of arguments passed into the problem
	 * @param arguments the arguments that correspond to the listing of ids
	 * @param linenumber the number of the line the problem occurred on
	 * @param charstart the start of a char selection range
	 * @param charend the end of a char selection range
	 * @param severity the severity of the problem. See {@link IMarker} for severity values.
	 * @param category the category of the problem. See {@link IApiProblem} for categories
	 * @param element the id of the backing element for this problem See {@link IElementDescriptor}, {@link IDelta} and {@link IJavaElement} for kinds
	 * @param kind the kind of the problem
	 * @param flags any additional flags for the kind
	 * @return a new {@link IApiProblem}
	 */
	public static IApiProblem newApiProblem(String resourcepath, String[] messageargs, String[] argumentids, Object[] arguments, int linenumber, int charstart, int charend, int severity, int category, int element, int kind, int flags) {
		return newApiProblem(resourcepath, messageargs, argumentids, arguments, linenumber, charstart, charend, severity, createProblemId(category, element, kind, flags));
	}
	
	/**
	 * Creates a new {@link IApiProblem}
	 * @param resourcepath the path to the resource this problem was found in
	 * @param messageargs listing of arguments to pass in to the localized message.
	 * The arguments are passed into the string in the order they appear in the array.
	 * @param argumentids the ids of arguments passed into the problem
	 * @param arguments the arguments that correspond to the listing of ids
	 * @param linenumber the number of the line the problem occurred on
	 * @param charstart the start of a char selection range
	 * @param charend the end of a char selection range
	 * @param severity the severity of the problem. See {@link IMarker} for severity values.
	 * @param id the composite id of the problem
	 * @return a new {@link IApiProblem}
	 */
	public static IApiProblem newApiProblem(String resourcepath, String[] messageargs, String[] argumentids, Object[] arguments, int linenumber, int charstart, int charend, int severity, int id) {
		return new ApiProblem(resourcepath, messageargs, argumentids, arguments, linenumber, charstart, charend, severity, id);
	}
	
	/**
	 * Creates a new API usage {@link IApiProblem}
	 * @param resourcepath the path to the resource this problem was found in
	 * @param messageargs listing of arguments to pass in to the localized message.
	 * The arguments are passed into the string in the order they appear in the array.
	 * @param argumentids the ids of arguments passed into the problem
	 * @param arguments the arguments that correspond to the listing of ids
	 * @param linenumber the number of the line the problem occurred on
	 * @param charstart the start of a char selection range
	 * @param charend the end of a char selection range
	 * @param severity the severity of the problem
	 * @param element the element kind
	 * @param kind the kind
	 * @return a new {@link IApiProblem} for API usage
	 */
	public static IApiProblem newApiUsageProblem(String resourcepath, String[] messageargs, String[] argumentids, Object[] arguments, int linenumber, int charstart, int charend, int severity, int element, int kind) {
		int id = createProblemId(IApiProblem.CATEGORY_USAGE, element, kind, IApiProblem.NO_FLAGS);
		return newApiProblem(resourcepath, messageargs, argumentids, arguments, linenumber, charstart, charend, severity, id);
	}

	/**
	 * Creates a new API usage {@link IApiProblem}
	 * @param resourcepath the path to the resource this problem was found in
	 * @param messageargs listing of arguments to pass in to the localized message.
	 * The arguments are passed into the string in the order they appear in the array.
	 * @param argumentids the ids of arguments passed into the problem
	 * @param arguments the arguments that correspond to the listing of ids
	 * @param linenumber the number of the line the problem occurred on
	 * @param charstart the start of a char selection range
	 * @param charend the end of a char selection range
	 * @param severity the severity of the problem
	 * @param element the element kind
	 * @param kind the kind
	 * @return a new {@link IApiProblem} for API usage
	 */
	public static IApiProblem newApiProfileProblem(String resourcepath, String[] messageargs, String[] argumentids, Object[] arguments, int linenumber, int charstart, int charend, int severity, int element, int kind) {
		int id = createProblemId(IApiProblem.CATEGORY_API_PROFILE, element, kind, IApiProblem.NO_FLAGS);
		return newApiProblem(resourcepath, messageargs, argumentids, arguments, linenumber, charstart, charend, severity, id);
	}
	
	/**
	 * Creates a new since tag {@link IApiProblem}
	 * @param resourcepath the path to the resource this problem was found in
	 * @param messageargs listing of arguments to pass in to the localized message.
	 * The arguments are passed into the string in the order they appear in the array.
	 * @param argumentids the ids of arguments passed into the problem
	 * @param arguments the arguments that correspond to the listing of ids
	 * @param linenumber the number of the line the problem occurred on
	 * @param charstart the start of a char selection range
	 * @param charend the end of a char selection range
	 * @param severity the severity
	 * @param element the element kind
	 * @param kind the kind
	 * @return a new {@link IApiProblem} for since tags
	 */
	public static IApiProblem newSinceTagProblem(String resourcepath, String[] messageargs, String[] argumentids, Object[] arguments, int linenumber, int charstart, int charend, int severity, int element, int kind) {
		int id = createProblemId(IApiProblem.CATEGORY_SINCETAGS, element, kind, IApiProblem.NO_FLAGS);
		return newApiProblem(resourcepath, messageargs, argumentids, arguments, linenumber, charstart, charend, severity, id);
	}
	
	/**
	 * Creates a new version number {@link IApiProblem}
	 * @param resourcepath the path to the resource this problem was found in
	 * @param messageargs listing of arguments to pass in to the localized message.
	 * The arguments are passed into the string in the order they appear in the array.
	 * @param argumentids the ids of arguments passed into the problem
	 * @param arguments the arguments that correspond to the listing of ids
	 * @param linenumber the number of the line the problem occurred on
	 * @param charstart the start of a char selection range
	 * @param charend the end of a char selection range
	 * @param severity the severity
	 * @param element the element kind
	 * @param kind the kind
	 * @return a new {@link IApiProblem} for version numbers
	 */
	public static IApiProblem newVersionNumberProblem(String resourcepath, String[] messageargs, String[] argumentids, Object[] arguments, int linenumber, int charstart, int charend, int severity, int element, int kind) {
		int id = createProblemId(IApiProblem.CATEGORY_VERSION, element, kind, IApiProblem.NO_FLAGS);
		return newApiProblem(resourcepath, messageargs, argumentids, arguments, linenumber, charstart, charend, severity, id);
	}
	
	/**
	 * Returns the localized message for the given {@link IApiProblem}. Returns
	 * <code>null</code> if no localized message cannot be created.
	 * @param problemid the id of the problem to create a message for
	 * @param arguments the arguments to pass into the localized string. The arguments are passed in to the string
	 * in the order they appear in the array.
	 * 
	 * @return a localized message for the given {@link IApiProblem} or <code>null</code>
	 */
	public static String getLocalizedMessage(IApiProblem problem) {
		return getLocalizedMessage(problem.getCategory(), problem.getElementKind(), problem.getKind(), problem.getFlags(), problem.getMessageArguments());
	}
	
	/**
	 * Returns the localized message for the given category, element kind, kind, flags and message arguments. Returns
	 * <code>null</code> if no localized message cannot be created.
	 * @param category
	 * @param elementkind
	 * @param kind
	 * @param flags
	 * @param messageargs
	 * @return a localized message for the given arguments of <code>null</code>
	 */
	public static String getLocalizedMessage(int category, int elementkind, int kind, int flags, String[] messageargs){
		if(fMessages == null) {
			fMessages = loadMessageTemplates(Locale.getDefault());
		}
		int id = category | (kind << IApiProblem.OFFSET_KINDS) | (flags << IApiProblem.OFFSET_FLAGS);
		switch(category) {
			case IApiProblem.CATEGORY_BINARY: {
				if(kind == IDelta.ADDED) {
					switch(flags) {
						case IDelta.TYPE_MEMBER:
						case IDelta.FIELD:
						case IDelta.METHOD: {
							id |= (elementkind << IApiProblem.OFFSET_ELEMENT);
						}
					}
				}
			}
		}
		String message = (String) fMessages.get(new Integer(id));
		if(message == null) {
			return MessageFormat.format(BuilderMessages.ApiProblemFactory_problem_message_not_found, new String[] {Integer.toString(id)});
		}
		return MessageFormat.format(message, messageargs);
	}
	
	/**
	 * This method initializes the MessageTemplates class variable according
	 * to the current Locale.
	 * @param loc Locale
	 * @return HashtableOfInt
	 */
	public static Hashtable loadMessageTemplates(Locale loc) {
		ResourceBundle bundle = null;
		String bundleName = "org.eclipse.pde.api.tools.internal.problems.problemmessages"; //$NON-NLS-1$
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
	
	/**
	 * Creates a problem id from the composite members of a problem id.
	 * @param category
	 * @param element
	 * @param kind
	 * @param flags
	 * @return a new problem id
	 */
	public static int createProblemId(int category, int element, int kind, int flags) {
		return category | element << IApiProblem.OFFSET_ELEMENT | kind << IApiProblem.OFFSET_KINDS | flags << IApiProblem.OFFSET_FLAGS;
	}
	
	/**
	 * Returns the problem kind from the given preference key.
	 * 
	 * @see IApiProblemTypes for a listing of all preference keys
	 * @param prefkey
	 * @return the corresponding kind for the given preference key, or 0 if the pref key is unknown
	 */
	public static int getProblemKindFromPref(String prefkey) {
		if(IApiProblemTypes.ILLEGAL_EXTEND.equals(prefkey)) {
			return IApiProblem.ILLEGAL_EXTEND;
		}
		if(IApiProblemTypes.ILLEGAL_IMPLEMENT.equals(prefkey)) {
			return IApiProblem.ILLEGAL_IMPLEMENT;
		}
		if(IApiProblemTypes.ILLEGAL_INSTANTIATE.equals(prefkey)) {
			return IApiProblem.ILLEGAL_INSTANTIATE;
		}
		if(IApiProblemTypes.ILLEGAL_REFERENCE.equals(prefkey)) {
			return IApiProblem.ILLEGAL_REFERENCE;
		}
		if(IApiProblemTypes.ILLEGAL_OVERRIDE.equals(prefkey)) {
			return IApiProblem.ILLEGAL_OVERRIDE;
		}
		if(IApiProblemTypes.MISSING_SINCE_TAG.equals(prefkey)) {
			return IApiProblem.SINCE_TAG_MISSING;
		}
		if(IApiProblemTypes.MALFORMED_SINCE_TAG.equals(prefkey)) {
			return IApiProblem.SINCE_TAG_MALFORMED;
		}
		if(IApiProblemTypes.INVALID_SINCE_TAG_VERSION.equals(prefkey)) {
			return IApiProblem.SINCE_TAG_INVALID;
		}
		if(prefkey != null) {
			if(prefkey.indexOf("ADDED") > -1) { //$NON-NLS-1$
				return IDelta.ADDED;
			}
			if(prefkey.indexOf("CHANGED") > -1) { //$NON-NLS-1$
				return IDelta.CHANGED;
			}
			if(prefkey.indexOf("REMOVED") > -1) { //$NON-NLS-1$
				return IDelta.REMOVED;
			}
		}
		return 0;
	}
}
