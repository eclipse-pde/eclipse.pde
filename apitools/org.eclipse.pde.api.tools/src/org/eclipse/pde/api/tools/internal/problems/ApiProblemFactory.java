/*******************************************************************************
 * Copyright (c) 2008, 2021 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.api.tools.internal.problems;

import java.text.ChoiceFormat;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.List;
import java.util.Locale;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

import org.eclipse.core.resources.IMarker;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.pde.api.tools.internal.builder.BuilderMessages;
import org.eclipse.pde.api.tools.internal.provisional.IApiMarkerConstants;
import org.eclipse.pde.api.tools.internal.provisional.comparator.IDelta;
import org.eclipse.pde.api.tools.internal.provisional.descriptors.IElementDescriptor;
import org.eclipse.pde.api.tools.internal.provisional.problems.IApiProblem;
import org.eclipse.pde.api.tools.internal.provisional.problems.IApiProblemFilter;
import org.eclipse.pde.api.tools.internal.provisional.problems.IApiProblemTypes;
import org.eclipse.pde.api.tools.internal.util.Util;

/**
 * Factory for creating {@link IApiProblem}s
 *
 * @since 1.0.0
 */
public class ApiProblemFactory {

	public static final int TYPE_CONVERSION_ID = 76;

	/**
	 * The current mapping of problem id to message
	 */
	private static Hashtable<Comparable<? extends Object>, String> fMessages = null;

	/**
	 * Creates a new {@link IApiProblemFilter}
	 *
	 * @param componentid
	 * @param problem
	 * @param comment
	 * @return the new {@link IApiProblemFilter}
	 * @since 1.1
	 */
	public static IApiProblemFilter newProblemFilter(String componentid, IApiProblem problem, String comment) {
		return new ApiProblemFilter(componentid, problem, comment);
	}

	/**
	 * Computes an {@link IApiProblem} hashcode from the given filter handle. If
	 * the handle is <code>null</code> this method returns <code>-1</code>.
	 *
	 * @param filterhandle the problem handle
	 * @return a new hashcode for the {@link IApiProblem} described in the
	 *         filter or <code>-1</code>
	 * @see ApiProblem#hashCode()
	 * @see ApiProblemFilter#getHandle()
	 * @since 1.0.500
	 */
	public static int getProblemHashcode(String filterhandle) {
		if (filterhandle != null) {
			String[] args = filterhandle.split(ApiProblemFilter.HANDLE_DELIMITER);
			int hashcode = 0;
			try {
				// the problem id
				hashcode += Integer.parseInt(args[0]);
				// the resource path
				hashcode += args[1].hashCode();
				// the type name, do not add to the hashcode if the type name is
				// null
				// https://bugs.eclipse.org/bugs/show_bug.cgi?id=404173
				if (args[2] != null && !args[2].equalsIgnoreCase("null")) { //$NON-NLS-1$
					hashcode += args[2].hashCode();
				}
				// the message arguments
				String[] margs = splitHandle(args[3], ApiProblemFilter.HANDLE_ARGUMENTS_DELIMITER);
				hashcode += argumentsHashcode(margs);
			} catch (Exception e) {
				// ignore
			}
			return hashcode;
		}
		return -1;
	}

	/**
	 * Returns the deep hash code of the complete listing of message arguments
	 *
	 * @param arguments
	 * @return the hash code of the message arguments
	 */
	private static int argumentsHashcode(String[] arguments) {
		if (arguments == null) {
			return 0;
		}
		int hashcode = 0;
		for (String argument : arguments) {
			hashcode += argument.hashCode();
		}
		return hashcode;
	}

	private static String[] splitHandle(String messageArguments, String delimiter) {
		List<String> matches = null;
		char[] argumentsChars = messageArguments.toCharArray();
		char[] delimiterChars = delimiter.toCharArray();
		int delimiterLength = delimiterChars.length;
		int start = 0;
		int argumentsCharsLength = argumentsChars.length;
		int balance = 0;
		for (int i = 0; i < argumentsCharsLength;) {
			char c = argumentsChars[i];
			switch (c) {
				case '(': {
					balance++;
					break;
				}
				case ')': {
					balance--;
					break;
				}
				default:
					break;
			}
			if (c == delimiterChars[0] && balance == 0) {
				// see if this is a matching delimiter start only if not within
				// parenthesis (balance == 0)
				if (i + delimiterLength < argumentsCharsLength) {
					boolean match = true;
					loop: for (int j = 1; j < delimiterLength; j++) {
						if (argumentsChars[i + j] != delimiterChars[j]) {
							match = false;
							break loop;
						}
					}
					if (match) {
						// record the matching substring and proceed
						if (matches == null) {
							matches = new ArrayList<>();
						}
						matches.add(messageArguments.substring(start, i));
						start = i + delimiterLength;
						i += delimiterLength;
					} else {
						i++;
					}
				} else {
					i++;
				}
			} else {
				i++;
			}
		}
		if (matches == null) {
			return new String[] { messageArguments };
		} else {
			matches.add(messageArguments.substring(start, argumentsCharsLength));
		}
		return matches.toArray(new String[matches.size()]);
	}

	/**
	 * Creates a new {@link IApiProblem}
	 *
	 * @param resourcepath the path to the resource this problem was found in
	 * @param typeName the type name this problem was found in
	 * @param messageargs listing of arguments to pass in to the localized
	 *            message. The arguments are passed into the string in the order
	 *            they appear in the array.
	 * @param argumentids the ids of arguments passed into the problem
	 * @param arguments the arguments that correspond to the listing of ids
	 * @param linenumber the number of the line the problem occurred on
	 * @param charstart the start of a char selection range
	 * @param charend the end of a char selection range
	 * @param category the category of the problem. See {@link IApiProblem} for
	 *            categories
	 * @param element the id of the backing element for this problem See
	 *            {@link IElementDescriptor}, {@link IDelta} and
	 *            {@link IJavaElement} for kinds
	 * @param kind the kind of the problem
	 * @param flags any additional flags for the kind
	 * @return a new {@link IApiProblem}
	 */
	public static IApiProblem newApiProblem(String resourcepath, String typeName, String[] messageargs, String[] argumentids, Object[] arguments, int linenumber, int charstart, int charend, int category, int element, int kind, int flags) {
		return newApiProblem(resourcepath, typeName, messageargs, argumentids, arguments, linenumber, charstart, charend, createProblemId(category, element, kind, flags));
	}

	/**
	 * Creates a new {@link IApiProblem}
	 *
	 * @param resourcepath the path to the resource this problem was found in
	 * @param typeName the type name this problem was found in
	 * @param messageargs listing of arguments to pass in to the localized
	 *            message. The arguments are passed into the string in the order
	 *            they appear in the array.
	 * @param argumentids the ids of arguments passed into the problem
	 * @param arguments the arguments that correspond to the listing of ids
	 * @param linenumber the number of the line the problem occurred on
	 * @param charstart the start of a char selection range
	 * @param charend the end of a char selection range
	 * @param id the composite id of the problem
	 * @return a new {@link IApiProblem}
	 */
	public static IApiProblem newApiProblem(String resourcepath, String typeName, String[] messageargs, String[] argumentids, Object[] arguments, int linenumber, int charstart, int charend, int id) {
		return new ApiProblem(resourcepath, typeName, messageargs, argumentids, arguments, linenumber, charstart, charend, id);
	}

	/**
	 * Creates a new API usage {@link IApiProblem}
	 *
	 * @param resourcepath the path to the resource this problem was found in
	 * @param typeName the type name this problem was found in
	 * @param messageargs listing of arguments to pass in to the localized
	 *            message. The arguments are passed into the string in the order
	 *            they appear in the array.
	 * @param argumentids the ids of arguments passed into the problem
	 * @param arguments the arguments that correspond to the listing of ids
	 * @param linenumber the number of the line the problem occurred on
	 * @param charstart the start of a char selection range
	 * @param charend the end of a char selection range
	 * @param element the element kind
	 * @param kind the kind
	 * @return a new {@link IApiProblem} for API usage
	 */
	public static IApiProblem newApiUsageProblem(String resourcepath, String typeName, String[] messageargs, String[] argumentids, Object[] arguments, int linenumber, int charstart, int charend, int element, int kind) {
		int id = createProblemId(IApiProblem.CATEGORY_USAGE, element, kind, IApiProblem.NO_FLAGS);
		return newApiProblem(resourcepath, typeName, messageargs, argumentids, arguments, linenumber, charstart, charend, id);
	}

	/**
	 * Creates a new API usage {@link IApiProblem}
	 *
	 * @param resourcepath the path to the resource this problem was found in
	 * @param typeName the type name this problem was found in
	 * @param messageargs listing of arguments to pass in to the localized
	 *            message. The arguments are passed into the string in the order
	 *            they appear in the array.
	 * @param argumentids the ids of arguments passed into the problem
	 * @param arguments the arguments that correspond to the listing of ids
	 * @param linenumber the number of the line the problem occurred on
	 * @param charstart the start of a char selection range
	 * @param charend the end of a char selection range
	 * @param element the element kind
	 * @param kind the kind
	 * @param flags the flags
	 * @return a new {@link IApiProblem} for API usage
	 */
	public static IApiProblem newApiUsageProblem(String resourcepath, String typeName, String[] messageargs, String[] argumentids, Object[] arguments, int linenumber, int charstart, int charend, int element, int kind, int flags) {
		int id = createProblemId(IApiProblem.CATEGORY_USAGE, element, kind, flags);
		return newApiProblem(resourcepath, typeName, messageargs, argumentids, arguments, linenumber, charstart, charend, id);
	}

	/**
	 * Creates a new API baseline {@link IApiProblem}
	 *
	 * @param resourcepath the path to the resource this problem was found in
	 *            The arguments are passed into the string in the order they
	 *            appear in the array.
	 * @param argumentids the ids of arguments passed into the problem
	 * @param arguments the arguments that correspond to the listing of ids
	 * @param element the element kind
	 * @param kind the kind
	 * @return a new {@link IApiProblem} for API usage
	 */
	public static IApiProblem newApiBaselineProblem(String resourcepath, String[] argumentids, Object[] arguments, int element, int kind) {
		int id = createProblemId(IApiProblem.CATEGORY_API_BASELINE, element, kind, IApiProblem.NO_FLAGS);
		return newApiProblem(resourcepath, null, null, argumentids, arguments, -1, -1, -1, id);
	}

	/**
	 * Creates a new API component resolution {@link IApiProblem}
	 *
	 * @param resourcepath the path to the resource this problem was found in
	 * @param messageargs listing of arguments to pass in to the localized
	 *            message. The arguments are passed into the string in the order
	 *            they appear in the array.
	 * @param argumentids the ids of arguments passed into the problem
	 * @param arguments the arguments that correspond to the listing of ids
	 * @param element the element kind
	 * @param kind the kind
	 * @return a new {@link IApiProblem} for API usage
	 */
	public static IApiProblem newApiComponentResolutionProblem(String resourcepath, String[] messageargs, String[] argumentids, Object[] arguments, int element, int kind) {
		int id = createProblemId(IApiProblem.CATEGORY_API_COMPONENT_RESOLUTION, element, kind, IApiProblem.NO_FLAGS);
		return newApiProblem(resourcepath, null, messageargs, argumentids, arguments, -1, -1, -1, id);
	}

	/**
	 * Creates a new fatal {@link IApiProblem}
	 *
	 * @param resourcepath the path to the resource this problem was found in
	 * @param messageargs listing of arguments to pass in to the localized
	 *            message. The arguments are passed into the string in the order
	 *            they appear in the array.
	 * @param kind the kind
	 * @return a new {@link IApiProblem} for API usage
	 */
	public static IApiProblem newFatalProblem(String resourcepath, String[] messageargs, int kind) {
		int id = createProblemId(IApiProblem.CATEGORY_FATAL_PROBLEM, IElementDescriptor.RESOURCE, kind, IApiProblem.NO_FLAGS);
		return newApiProblem(resourcepath, null, messageargs, null, null, -1, -1, -1, id);
	}

	/**
	 * Creates a new since tag {@link IApiProblem}
	 *
	 * @param resourcepath the path to the resource this problem was found in
	 * @param typeName the type name this problem was found in
	 * @param messageargs listing of arguments to pass in to the localized
	 *            message. The arguments are passed into the string in the order
	 *            they appear in the array.
	 * @param argumentids the ids of arguments passed into the problem
	 * @param arguments the arguments that correspond to the listing of ids
	 * @param linenumber the number of the line the problem occurred on
	 * @param charstart the start of a char selection range
	 * @param charend the end of a char selection range
	 * @param element the element kind
	 * @param kind the kind
	 * @return a new {@link IApiProblem} for since tags
	 */
	public static IApiProblem newApiSinceTagProblem(String resourcepath, String typeName, String[] messageargs, String[] argumentids, Object[] arguments, int linenumber, int charstart, int charend, int element, int kind) {
		int id = createProblemId(IApiProblem.CATEGORY_SINCETAGS, element, kind, IApiProblem.NO_FLAGS);
		return newApiProblem(resourcepath, typeName, messageargs, argumentids, arguments, linenumber, charstart, charend, id);
	}

	/**
	 * Creates a new version number {@link IApiProblem}
	 *
	 * @param resourcepath the path to the resource this problem was found in
	 * @param typeName the type name this problem was found in
	 * @param messageargs listing of arguments to pass in to the localized
	 *            message. The arguments are passed into the string in the order
	 *            they appear in the array.
	 * @param argumentids the ids of arguments passed into the problem
	 * @param arguments the arguments that correspond to the listing of ids
	 * @param linenumber the number of the line the problem occurred on
	 * @param charstart the start of a char selection range
	 * @param charend the end of a char selection range
	 * @param element the element kind
	 * @param kind the kind
	 * @return a new {@link IApiProblem} for version numbers
	 */
	public static IApiProblem newApiVersionNumberProblem(String resourcepath, String typeName, String[] messageargs, String[] argumentids, Object[] arguments, int linenumber, int charstart, int charend, int element, int kind) {
		int id = createProblemId(IApiProblem.CATEGORY_VERSION, element, kind, IApiProblem.NO_FLAGS);
		return newApiProblem(resourcepath, typeName, messageargs, argumentids, arguments, linenumber, charstart, charend, id);
	}

	/**
	 * Creates a new API Use Scan breakage {@link IApiProblem}
	 *
	 * @param resourcePath path of the resource associated with the problem
	 * @param typeName the type name this problem was found in
	 * @param messageargs listing of arguments to pass in to the localized
	 *            message. The arguments are passed into the string in the order
	 *            they appear in the array.
	 * @param argumentids the ids of arguments passed into the problem
	 * @param arguments the arguments that correspond to the listing of ids
	 * @param linenumber the number of the line the problem occurred on
	 * @param charstart the start of a char selection range
	 * @param charend the end of a char selection range
	 * @param element the element kind
	 * @param kind the kind
	 * @param flags flags the reason for problem. <code>0</code> if the type
	 *            could not be resolved. <code>1</code> if member could not be
	 *            located in the type.
	 * @return a new {@link IApiProblem} for API Use Scan breakage
	 */
	public static IApiProblem newApiUseScanProblem(String resourcePath, String typeName, String[] messageargs, String[] argumentids, Object[] arguments, int linenumber, int charstart, int charend, int element, int kind, int flags) {
		int id = createProblemId(IApiProblem.CATEGORY_API_USE_SCAN_PROBLEM, element, kind, flags);
		return newApiProblem(resourcePath, typeName, messageargs, argumentids, arguments, linenumber, charstart, charend, id);
	}

	/**
	 * Returns the localized message for the given {@link IApiProblem}. Returns
	 * <code>null</code> if no localized message cannot be created.
	 *
	 * @param problemid the id of the problem to create a message for
	 * @param arguments the arguments to pass into the localized string. The
	 *            arguments are passed in to the string in the order they appear
	 *            in the array.
	 *
	 * @return a localized message for the given {@link IApiProblem} or
	 *         <code>null</code>
	 */
	public static String getLocalizedMessage(IApiProblem problem) {
		return getLocalizedMessage(problem.getMessageid(), problem.getMessageArguments());
	}

	/**
	 * Returns the localized message for the given problem id and message
	 * arguments. Returns a not found message if no localized message cannot be
	 * created.
	 *
	 * @param messageid
	 * @param messageargs
	 * @return a localized message for the given arguments or a 'not found'
	 *         message
	 */
	public static String getLocalizedMessage(int messageid, String[] messageargs) {
		if (fMessages == null) {
			fMessages = loadMessageTemplates(Locale.getDefault());
		}
		String pattern = fMessages.get(Integer.valueOf(messageid));
		if (pattern == null) {
			return MessageFormat.format(BuilderMessages.ApiProblemFactory_problem_message_not_found, Integer.toString(messageid));
		}
		if (messageid == TYPE_CONVERSION_ID) {
			MessageFormat messageFormat = new MessageFormat(pattern);
			double[] typeElementTypes = {
					IDelta.ANNOTATION_ELEMENT_TYPE, IDelta.CLASS_ELEMENT_TYPE,
					IDelta.ENUM_ELEMENT_TYPE, IDelta.INTERFACE_ELEMENT_TYPE, };
			String[] typeElementTypesStrings = {
					fMessages.get(Util.getDeltaElementType(IDelta.ANNOTATION_ELEMENT_TYPE)),
					fMessages.get(Util.getDeltaElementType(IDelta.CLASS_ELEMENT_TYPE)),
					fMessages.get(Util.getDeltaElementType(IDelta.ENUM_ELEMENT_TYPE)),
					fMessages.get(Util.getDeltaElementType(IDelta.INTERFACE_ELEMENT_TYPE)), };
			ChoiceFormat choiceFormat = new ChoiceFormat(typeElementTypes, typeElementTypesStrings);
			messageFormat.setFormatByArgumentIndex(1, choiceFormat);
			messageFormat.setFormatByArgumentIndex(2, choiceFormat);
			Object[] args = new Object[messageargs.length];
			args[0] = messageargs[0];
			args[1] = Integer.decode(messageargs[1]);
			args[2] = Integer.decode(messageargs[2]);
			return messageFormat.format(args);
		}
		return MessageFormat.format(pattern, (Object[]) messageargs);
	}

	/**
	 * This method initializes the MessageTemplates class variable according to
	 * the current Locale.
	 *
	 * @param loc Locale
	 * @return HashtableOfInt
	 */
	public static Hashtable<Comparable<? extends Object>, String> loadMessageTemplates(Locale loc) {
		ResourceBundle bundle = null;
		String bundleName = "org.eclipse.pde.api.tools.internal.problems.problemmessages"; //$NON-NLS-1$
		try {
			bundle = ResourceBundle.getBundle(bundleName, loc);
		} catch (MissingResourceException e) {
			System.out.println("Missing resource : " + bundleName.replace('.', '/') + ".properties for locale " + loc); //$NON-NLS-1$//$NON-NLS-2$
			throw e;
		}
		Hashtable<Comparable<? extends Object>, String> templates = new Hashtable<>(700);
		Enumeration<String> keys = bundle.getKeys();
		while (keys.hasMoreElements()) {
			String key = keys.nextElement();
			try {
				int messageID = Integer.parseInt(key);
				templates.put(Integer.valueOf(messageID), bundle.getString(key));
			} catch (NumberFormatException e) {
				// key is not a number
				templates.put(key, bundle.getString(key));
			} catch (MissingResourceException e) {
				// available ID
			}
		}
		return templates;
	}

	/**
	 * Creates a problem id from the composite members of a problem id.
	 *
	 * @param category
	 * @param element
	 * @param kind
	 * @param flags
	 * @return a new problem id
	 */
	public static int createProblemId(int category, int element, int kind, int flags) {
		return category | element << IApiProblem.OFFSET_ELEMENT | kind << IApiProblem.OFFSET_KINDS | flags << IApiProblem.OFFSET_FLAGS | getProblemMessageId(category, element, kind, flags);
	}

	/**
	 * Returns the {@link IApiProblem} id from the given marker or
	 * <code>-1</code> if the marker is <code>null</code> or the marker does not
	 * contain the {@link IApiMarkerConstants#MARKER_ATTR_PROBLEM_ID} attribute
	 *
	 * @param marker
	 * @return the {@link IApiProblem} id or <code>-1</code>
	 * @since 1.0.400
	 */
	public static int getProblemId(IMarker marker) {
		if (marker != null) {
			return marker.getAttribute(IApiMarkerConstants.MARKER_ATTR_PROBLEM_ID, -1);
		}
		return -1;
	}

	/**
	 * Returns the kind of the problem from the given problem id. The returned
	 * kind is not checked to see if it is correct or existing.
	 *
	 * @see IApiProblem#getKind()
	 * @see IDelta#getKind()
	 *
	 * @param problemid
	 * @return the kind from the given problem id
	 */
	public static int getProblemKind(int problemid) {
		return (problemid & ApiProblem.KIND_MASK) >> IApiProblem.OFFSET_KINDS;
	}

	/**
	 * Returns the kind of element from the given problem id. The returned
	 * element kind is not checked to see if it is correct or existing.
	 *
	 * @see IElementDescriptor#getElementType()
	 * @see IDelta#getElementType()
	 *
	 * @param problemid
	 * @return the element kind from the given problem id
	 */
	public static int getProblemElementKind(int problemid) {
		return (problemid & ApiProblem.ELEMENT_KIND_MASK) >> IApiProblem.OFFSET_ELEMENT;
	}

	/**
	 * Returns the flags from the given problem id. The returned flags are not
	 * checked to see if they are correct or existing.
	 *
	 * @see IDelta#getFlags()
	 *
	 * @param problemid
	 * @return the flags from the given problem id
	 */
	public static int getProblemFlags(int problemid) {
		return (problemid & ApiProblem.FLAGS_MASK) >> IApiProblem.OFFSET_FLAGS;
	}

	/**
	 * Returns the category of the given problem id. The returned category is
	 * not checked to see if it is correct or existing.
	 *
	 * @see IApiProblem#getCategory()
	 *
	 * @param problemid
	 * @return the category of this problem id
	 */
	public static int getProblemCategory(int problemid) {
		return (problemid & ApiProblem.CATEGORY_MASK);
	}

	/**
	 * Convenience method to get the message id from a problem id
	 *
	 * @param problemid
	 * @return the message id to use for the given problem id
	 */
	public static int getProblemMessageId(int problemid) {
		return getProblemMessageId(getProblemCategory(problemid), getProblemElementKind(problemid), getProblemKind(problemid), getProblemFlags(problemid));
	}

	/**
	 * Returns the problem message id for the given problem parameters.
	 *
	 * @param category
	 * @param element
	 * @param kind
	 * @param flags
	 * @return the id of the message to use for the given problem parameters or
	 *         <code>0</code>
	 */
	public static int getProblemMessageId(int category, int element, int kind, int flags) {
		switch (category) {
			case IApiProblem.CATEGORY_API_BASELINE: {
				switch (kind) {
					case IApiProblem.API_BASELINE_MISSING:
						return 1;
					case IApiProblem.API_BASELINE_MISMATCH:
						return 57;
					case IApiProblem.API_PLUGIN_NOT_PRESENT_IN_BASELINE:
						return 60;

					default:
						break;
				}
				break;
			}
			case IApiProblem.CATEGORY_SINCETAGS: {
				switch (kind) {
					case IApiProblem.SINCE_TAG_INVALID:
						return 2;
					case IApiProblem.SINCE_TAG_MALFORMED:
						return 3;
					case IApiProblem.SINCE_TAG_MISSING:
						return 4;
					default:
						break;
				}
				break;
			}

			case IApiProblem.CATEGORY_VERSION: {
				switch (kind) {
					case IApiProblem.MAJOR_VERSION_CHANGE:
						return 5;
					case IApiProblem.MAJOR_VERSION_CHANGE_NO_BREAKAGE:
						return 6;
					case IApiProblem.MINOR_VERSION_CHANGE:
						return 7;
					case IApiProblem.MINOR_VERSION_CHANGE_NO_NEW_API:
						return 56;
					case IApiProblem.REEXPORTED_MAJOR_VERSION_CHANGE:
						return 19;
					case IApiProblem.REEXPORTED_REMOVAL_OF_REEXPORT_MAJOR_VERSION_CHANGE:
						return 62;
					case IApiProblem.REEXPORTED_MINOR_VERSION_CHANGE:
						return 20;
					case IApiProblem.MINOR_VERSION_CHANGE_EXECUTION_ENV_CHANGED:
						return 43;
					case IApiProblem.MICRO_VERSION_CHANGE_UNNECESSARILY:
						return 58;
					case IApiProblem.MINOR_VERSION_CHANGE_UNNECESSARILY:
						return 59;

					default:
						break;
				}
				break;
			}
			case IApiProblem.CATEGORY_USAGE: {
				switch (kind) {
					case IApiProblem.ILLEGAL_IMPLEMENT: {
						switch (flags) {
							case IApiProblem.NO_FLAGS:
								return 8;
							case IApiProblem.INDIRECT_REFERENCE:
								return 24;
							case IApiProblem.LOCAL_TYPE:
								return 18;
							case IApiProblem.INDIRECT_LOCAL_REFERENCE:
								return 37;
							case IApiProblem.ANONYMOUS_TYPE:
								return 27;
							default:
								break;
						}
						break;
					}
					case IApiProblem.ILLEGAL_EXTEND: {
						switch (flags) {
							case IApiProblem.NO_FLAGS:
								return 9;
							case IApiProblem.LOCAL_TYPE:
								return 25;
							case IApiProblem.ANONYMOUS_TYPE:
								return 28;
							default:
								break;
						}
						break;
					}
					case IApiProblem.ILLEGAL_INSTANTIATE:
						return 10;
					case IApiProblem.ILLEGAL_OVERRIDE:
						return 11;
					case IApiProblem.ILLEGAL_REFERENCE: {
						switch (flags) {
							case IApiProblem.FIELD:
								return 12;
							case IApiProblem.CONSTRUCTOR_METHOD:
								return 110;
							case IApiProblem.METHOD:
								return 111;
							case IApiProblem.ANNOTATION:
								return 42;
							default:
								break;
						}
						break;
					}
					case IApiProblem.API_LEAK: {
						switch (flags) {
							case IApiProblem.LEAK_EXTENDS:
								return 13;
							case IApiProblem.LEAK_BY_EXTENDING_NO_EXTEND_CLASS_TYPE:
								return 48;
							case IApiProblem.LEAK_BY_EXTENDING_NO_EXTEND_INTERFACE_TYPE:
								return 50;
							case IApiProblem.LEAK_IMPLEMENTS:
								return 14;
							case IApiProblem.LEAK_FIELD:
								return 15;
							case IApiProblem.LEAK_RETURN_TYPE:
								return 16;
							case IApiProblem.LEAK_METHOD_PARAMETER:
								return 17;
							case IApiProblem.LEAK_CONSTRUCTOR_PARAMETER:
								return 109;
							default:
								break;
						}
						break;
					}
					case IApiProblem.UNSUPPORTED_TAG_USE:
						return 112;
					case IApiProblem.DUPLICATE_TAG_USE:
						return 22;
					case IApiProblem.DUPLICATE_ANNOTATION_USE:
						return 45;
					case IApiProblem.UNSUPPORTED_ANNOTATION_USE:
						return 46;
					case IApiProblem.INVALID_REFERENCE_IN_SYSTEM_LIBRARIES:
						switch (flags) {
							case IApiProblem.METHOD:
								return 33;
							case IApiProblem.CONSTRUCTOR_METHOD:
								return 34;
							case IApiProblem.FIELD:
								return 35;
							default:
								return 36;
						}
					case IApiProblem.UNUSED_PROBLEM_FILTERS:
						return 30;

					default:
						break;
				}
				break;
			}
			case IApiProblem.CATEGORY_COMPATIBILITY: {
				switch (kind) {
					case IDelta.ADDED: {
						switch (element) {
							case IDelta.CLASS_ELEMENT_TYPE: {
								switch (flags) {
									case IDelta.FIELD:
										return 49;
									case IDelta.METHOD:
										return 41;
									case IDelta.RESTRICTIONS:
										return 72;
									default:
										break;
								}
								break;
							}
							case IDelta.ANNOTATION_ELEMENT_TYPE: {
								switch (flags) {
									case IDelta.FIELD:
										return 39;
									default:
										break;
								}
								break;
							}
							case IDelta.INTERFACE_ELEMENT_TYPE: {
								switch (flags) {
									case IDelta.FIELD:
										return 40;
									case IDelta.METHOD:
										return 44;
									case IDelta.DEFAULT_METHOD:
									case IDelta.SUPER_INTERFACE_DEFAULT_METHOD:
										return 47;
									case IDelta.RESTRICTIONS:
										return 72;
									case IDelta.SUPER_INTERFACE_WITH_METHODS:
										return 133;
									default:
										break;

								}
								break;
							}
							case IDelta.METHOD_ELEMENT_TYPE: {
								switch (flags) {
									case IDelta.RESTRICTIONS:
										return 132;
									default:
										break;
								}
								break;
							}
							default:
								break;
						}
						switch (flags) {
							case IDelta.CLASS_BOUND:
								return 21;
							case IDelta.CONSTRUCTOR:
								return 23;
							case IDelta.INTERFACE_BOUND:
								return 26;
							case IDelta.METHOD_WITHOUT_DEFAULT_VALUE:
								return 29;
							case IDelta.TYPE_PARAMETER:
								return 32;
							case IDelta.TYPE_ARGUMENT:
								return 106;
							case IDelta.EXPANDED_SUPERINTERFACES_SET_BREAKING:
								return 51;
							case IDelta.SUPERCLASS_BREAKING:
								return 53;
							default:
								break;
						}
						break;
					}
					case IDelta.CHANGED: {
						switch (element) {
							case IDelta.FIELD_ELEMENT_TYPE: {
								switch (flags) {
									case IDelta.TYPE:
										return 81;
									case IDelta.VALUE:
										return 84;
									case IDelta.DECREASE_ACCESS:
										return 114;
									case IDelta.NON_FINAL_TO_FINAL:
										return 118;
									case IDelta.STATIC_TO_NON_STATIC:
										return 121;
									case IDelta.NON_STATIC_TO_STATIC:
										return 69;
									default:
										break;
								}
								break;
							}
							case IDelta.METHOD_ELEMENT_TYPE: {
								switch (flags) {
									case IDelta.DECREASE_ACCESS:
										return 115;
									case IDelta.NON_ABSTRACT_TO_ABSTRACT:
										return 117;
									case IDelta.NON_FINAL_TO_FINAL:
										return 119;
									case IDelta.NON_STATIC_TO_STATIC:
										return 120;
									case IDelta.STATIC_TO_NON_STATIC:
										return 122;
									default:
										break;
								}
								break;
							}
							case IDelta.CONSTRUCTOR_ELEMENT_TYPE: {
								switch (flags) {
									case IDelta.DECREASE_ACCESS:
										return 116;
									default:
										break;
								}
								break;
							}
							default:
								break;
						}
						switch (flags) {
							case IDelta.CLASS_BOUND:
								return 52;
							case IDelta.CONTRACTED_SUPERINTERFACES_SET:
								return 54;
							case IDelta.DECREASE_ACCESS:
								return 55;
							case IDelta.FINAL_TO_NON_FINAL_STATIC_CONSTANT:
								return 61;
							case IDelta.INTERFACE_BOUND:
								return 64;
							case IDelta.NON_ABSTRACT_TO_ABSTRACT:
								return 66;
							case IDelta.NON_FINAL_TO_FINAL:
								return 67;
							case IDelta.NON_STATIC_TO_STATIC:
								return 123;
							case IDelta.STATIC_TO_NON_STATIC:
								return 73;
							case IDelta.TYPE_CONVERSION:
								return TYPE_CONVERSION_ID;
							case IDelta.VARARGS_TO_ARRAY:
								return 85;
							case IDelta.TYPE_ARGUMENT:
								return 124;
							case IDelta.EXPANDED_SUPERINTERFACES_SET_BREAKING:
								return 51;

							default:
								break;
						}
						break;
					}
					case IDelta.REMOVED: {
						switch (flags) {
							case IDelta.ANNOTATION_DEFAULT_VALUE:
								return 86;
							case IDelta.API_COMPONENT:
								return 87;
							case IDelta.CLASS_BOUND:
								return 89;
							case IDelta.CONSTRUCTOR:
								return 91;
							case IDelta.ENUM_CONSTANT:
								return 92;
							case IDelta.FIELD:
								return 94;
							case IDelta.INTERFACE_BOUND:
								return 96;
							case IDelta.METHOD:
								return 98;
							case IDelta.METHOD_WITH_DEFAULT_VALUE:
								return 100;
							case IDelta.METHOD_WITHOUT_DEFAULT_VALUE:
								return 101;
							case IDelta.TYPE:
								return 102;
							case IDelta.TYPE_ARGUMENTS:
								return 103;
							case IDelta.TYPE_MEMBER:
								return 104;
							case IDelta.TYPE_PARAMETER:
								return 105;
							case IDelta.VALUE:
								return 108;
							case IDelta.API_TYPE:
								return 113;
							case IDelta.API_FIELD:
								return 125;
							case IDelta.API_METHOD:
								return 126;
							case IDelta.API_CONSTRUCTOR:
								return 127;
							case IDelta.API_ENUM_CONSTANT:
								return 128;
							case IDelta.API_METHOD_WITH_DEFAULT_VALUE:
								return 129;
							case IDelta.API_METHOD_WITHOUT_DEFAULT_VALUE:
								return 130;
							case IDelta.TYPE_ARGUMENT:
								return 107;
							case IDelta.SUPERCLASS:
								return 131;
							case IDelta.REEXPORTED_API_TYPE:
								return 134;
							case IDelta.REEXPORTED_TYPE:
								return 135;
							default:
								break;
						}
						break;
					}
					default:
						break;
				}
				break;
			}
			case IApiProblem.CATEGORY_API_COMPONENT_RESOLUTION: {
				switch (kind) {
					case IApiProblem.API_COMPONENT_RESOLUTION:
						return 99;
					default:
						break;
				}
				break;
			}
			case IApiProblem.CATEGORY_FATAL_PROBLEM: {
				switch (kind) {
					case IApiProblem.FATAL_JDT_BUILDPATH_PROBLEM:
						return 31;
					default:
						break;
				}
				break;
			}
			case IApiProblem.CATEGORY_API_USE_SCAN_PROBLEM: {
				switch (kind) {
					case IApiProblem.API_USE_SCAN_TYPE_PROBLEM:
						return 136 + flags;
					case IApiProblem.API_USE_SCAN_METHOD_PROBLEM:
						return 138 + flags;
					case IApiProblem.API_USE_SCAN_FIELD_PROBLEM:
						return 140 + flags;
					default:
						break;
				}
				break;
			}
			default:
				break;
		}
		return 0;
	}

	/**
	 * Returns the problem severity id for the given problem parameters.
	 *
	 * @param category
	 * @param element
	 * @param kind
	 * @param flags
	 * @return the id of the preference to use to lookup the user specified
	 *         severity level for the given {@link IApiProblem}
	 */
	public static String getProblemSeverityId(IApiProblem problem) {
		switch (problem.getCategory()) {
			case IApiProblem.CATEGORY_FATAL_PROBLEM: {
				switch (problem.getKind()) {
					case IApiProblem.FATAL_JDT_BUILDPATH_PROBLEM:
						return IApiProblemTypes.FATAL_PROBLEMS;
					default:
						break;
				}
				break;
			}
			case IApiProblem.CATEGORY_API_COMPONENT_RESOLUTION: {
				switch (problem.getKind()) {
					case IApiProblem.API_COMPONENT_RESOLUTION:
						return IApiProblemTypes.REPORT_RESOLUTION_ERRORS_API_COMPONENT;

					default:
						break;
				}
				break;
			}
			case IApiProblem.CATEGORY_API_BASELINE: {
				switch (problem.getKind()) {
					case IApiProblem.API_BASELINE_MISSING:
					case IApiProblem.API_BASELINE_MISMATCH:
						return IApiProblemTypes.MISSING_DEFAULT_API_BASELINE;
					case IApiProblem.API_PLUGIN_NOT_PRESENT_IN_BASELINE:
						return IApiProblemTypes.MISSING_PLUGIN_IN_API_BASELINE;
					default:
						break;
				}
				break;
			}
			case IApiProblem.CATEGORY_SINCETAGS: {
				switch (problem.getKind()) {
					case IApiProblem.SINCE_TAG_INVALID:
						return IApiProblemTypes.INVALID_SINCE_TAG_VERSION;
					case IApiProblem.SINCE_TAG_MALFORMED:
						return IApiProblemTypes.MALFORMED_SINCE_TAG;
					case IApiProblem.SINCE_TAG_MISSING:
						return IApiProblemTypes.MISSING_SINCE_TAG;
					default:
						break;
				}
				break;
			}
			case IApiProblem.CATEGORY_VERSION: {
				switch (problem.getKind()) {
					case IApiProblem.MINOR_VERSION_CHANGE_NO_NEW_API:
					case IApiProblem.MICRO_VERSION_CHANGE_UNNECESSARILY:
					case IApiProblem.MINOR_VERSION_CHANGE_UNNECESSARILY:
						return IApiProblemTypes.INCOMPATIBLE_API_COMPONENT_VERSION_REPORT_MINOR_WITHOUT_API_CHANGE;
					case IApiProblem.MAJOR_VERSION_CHANGE_NO_BREAKAGE:
						return IApiProblemTypes.INCOMPATIBLE_API_COMPONENT_VERSION_REPORT_MAJOR_WITHOUT_BREAKING_CHANGE;
					case IApiProblem.MINOR_VERSION_CHANGE_EXECUTION_ENV_CHANGED:
						return IApiProblemTypes.CHANGED_EXECUTION_ENV;
					default:
						return IApiProblemTypes.INCOMPATIBLE_API_COMPONENT_VERSION;
				}

			}
			case IApiProblem.CATEGORY_USAGE: {
				switch (problem.getKind()) {
					case IApiProblem.ILLEGAL_IMPLEMENT:
						return IApiProblemTypes.ILLEGAL_IMPLEMENT;
					case IApiProblem.ILLEGAL_EXTEND:
						return IApiProblemTypes.ILLEGAL_EXTEND;
					case IApiProblem.ILLEGAL_INSTANTIATE:
						return IApiProblemTypes.ILLEGAL_INSTANTIATE;
					case IApiProblem.ILLEGAL_OVERRIDE:
						return IApiProblemTypes.ILLEGAL_OVERRIDE;
					case IApiProblem.ILLEGAL_REFERENCE:
						return IApiProblemTypes.ILLEGAL_REFERENCE;
					case IApiProblem.API_LEAK: {
						switch (problem.getFlags()) {
							case IApiProblem.LEAK_EXTENDS:
							case IApiProblem.LEAK_BY_EXTENDING_NO_EXTEND_CLASS_TYPE:
							case IApiProblem.LEAK_BY_EXTENDING_NO_EXTEND_INTERFACE_TYPE:
								return IApiProblemTypes.LEAK_EXTEND;
							case IApiProblem.LEAK_FIELD:
								return IApiProblemTypes.LEAK_FIELD_DECL;
							case IApiProblem.LEAK_IMPLEMENTS:
								return IApiProblemTypes.LEAK_IMPLEMENT;
							case IApiProblem.LEAK_CONSTRUCTOR_PARAMETER:
							case IApiProblem.LEAK_METHOD_PARAMETER:
								return IApiProblemTypes.LEAK_METHOD_PARAM;
							case IApiProblem.LEAK_RETURN_TYPE:
								return IApiProblemTypes.LEAK_METHOD_RETURN_TYPE;
							default:
								break;
						}
						break;
					}
					case IApiProblem.UNSUPPORTED_TAG_USE:
						return IApiProblemTypes.INVALID_JAVADOC_TAG;
					case IApiProblem.DUPLICATE_TAG_USE:
						return IApiProblemTypes.INVALID_JAVADOC_TAG;
					case IApiProblem.DUPLICATE_ANNOTATION_USE:
						return IApiProblemTypes.INVALID_ANNOTATION;
					case IApiProblem.UNSUPPORTED_ANNOTATION_USE:
						return IApiProblemTypes.INVALID_ANNOTATION;
					case IApiProblem.INVALID_REFERENCE_IN_SYSTEM_LIBRARIES:
						return IApiProblemTypes.INVALID_REFERENCE_IN_SYSTEM_LIBRARIES;
					case IApiProblem.UNUSED_PROBLEM_FILTERS:
						return IApiProblemTypes.UNUSED_PROBLEM_FILTERS;

					default:
						break;
				}
				break;
			}
			case IApiProblem.CATEGORY_COMPATIBILITY: {
				return  Util.getDeltaPrefererenceKey(problem.getElementKind(), problem.getKind(), problem.getFlags());
			}
			case IApiProblem.CATEGORY_API_USE_SCAN_PROBLEM: {
				switch (problem.getKind()) {
					case IApiProblem.API_USE_SCAN_TYPE_PROBLEM:
						return IApiProblemTypes.API_USE_SCAN_TYPE_SEVERITY;
					case IApiProblem.API_USE_SCAN_METHOD_PROBLEM:
						return IApiProblemTypes.API_USE_SCAN_METHOD_SEVERITY;
					case IApiProblem.API_USE_SCAN_FIELD_PROBLEM:
						return IApiProblemTypes.API_USE_SCAN_FIELD_SEVERITY;
					default:
						break;
				}
				break;
			}
			default:
				break;
		}
		return null;
	}
}
