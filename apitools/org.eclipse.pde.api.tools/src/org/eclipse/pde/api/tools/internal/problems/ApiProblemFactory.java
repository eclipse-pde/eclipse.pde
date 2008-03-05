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

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IResource;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.pde.api.tools.internal.provisional.comparator.IDelta;
import org.eclipse.pde.api.tools.internal.provisional.descriptors.IElementDescriptor;
import org.eclipse.pde.api.tools.internal.provisional.problems.IApiProblem;
import org.eclipse.pde.api.tools.internal.provisional.problems.IApiProblemTypes;

/**
 * Factory for creating {@link IApiProblem}s
 * 
 * @since 1.0.0
 */
public class ApiProblemFactory {

	/**
	 * Creates a new {@link IApiProblem}
	 * @param resource the resource this problem is on / in
	 * @param message the human readable message for the problem
	 * @param severity the severity of the problem. See {@link IMarker} for severity values.
	 * @param category the category of the problem. See {@link IApiProblem} for categories
	 * @param element the id of the backing element for this problem See {@link IElementDescriptor}, {@link IDelta} and {@link IJavaElement} for kinds
	 * @param kind the kind of the problem
	 * @param flags any additional flags for the kind
	 * @return a new {@link IApiProblem}
	 */
	public static IApiProblem newApiProblem(IResource resource, String message, int severity, int category, int element, int kind, int flags) {
		return new ApiProblem(resource, message, severity, category, element, kind, flags);
	}
	
	/**
	 * Creates a new {@link IApiProblem}
	 * @param resource the resource this problem is on / in
	 * @param message the human readable message for the problem
	 * @param severity the severity of the problem. See {@link IMarker} for severity values.
	 * @param id the composite id of the problem
	 * @return a new {@link IApiProblem}
	 */
	public static IApiProblem newApiProblem(IResource resource, String message, int severity, int id) {
		return new ApiProblem(resource, message, severity, id);
	}
	
	/**
	 * Creates a new API usage {@link IApiProblem}
	 * @param resource the backing resource
	 * @param message the message for the problem
	 * @param severity the severity of the problem
	 * @param element the element kind
	 * @param kind the kind
	 * @return a new {@link IApiProblem} for API usage
	 */
	public static IApiProblem newApiUsageProblem(IResource resource, String message, int severity, int element, int kind) {
		return new ApiProblem(resource, message, severity, IApiProblem.CATEGORY_USAGE, element, kind, IApiProblem.NO_FLAGS);
	}

	/**
	 * Creates a new since tag {@link IApiProblem}
	 * @param resource the backing resource
	 * @param message the message for the problem
	 * @param severity the severity
	 * @param element the element kind
	 * @param kind the kind
	 * @return a new {@link IApiProblem} for since tags
	 */
	public static IApiProblem newSinceTagProblem(IResource resource, String message, int severity, int element, int kind) {
		return new ApiProblem(resource, message, severity, IApiProblem.CATEGORY_SINCETAGS, element, kind, IApiProblem.NO_FLAGS);
	}
	
	/**
	 * Creates a new version number {@link IApiProblem}
	 * @param resource the backing resource
	 * @param message the message for the problem
	 * @param severity the severity
	 * @param element the element kind
	 * @param kind the kind
	 * @return a new {@link IApiProblem} for version numbers
	 */
	public static IApiProblem newVersionNumberProblem(IResource resource, String message, int severity, int element, int kind) {
		return new ApiProblem(resource, message, severity, IApiProblem.CATEGORY_VERSION, element, kind, IApiProblem.NO_FLAGS);
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
