/*******************************************************************************
 * Copyright (c) 2007, 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.api.tools.internal.provisional;

/**
 * Interface that defines all the constants used to create the Api tooling markers.
 * 
 * This interface is not intended to be extended or implemented.
 *
 * @since 1.0.0
 */
public interface IApiMarkerConstants {

	/**
	 * Constant representing the name of the 'category' attribute on api tooling markers.
	 * Value is: <code>category</code>
	 */
	public static final String MARKER_ATTR_CATEGORY = "category"; //$NON-NLS-1$
	/**
	 * Constant representing the name of the 'kind' attribute on API tooling markers.
	 * Value is: <code>kind</code>
	 */
	public static final String MARKER_ATTR_KIND = "kind"; //$NON-NLS-1$
	/**
	 * Constant representing the name of the 'flags' attribute on API tooling markers.
	 * Value is: <code>flags</code>
	 */
	public static final String MARKER_ATTR_FLAGS = "flags"; //$NON-NLS-1$
	/**
	 * Constant representing the name of the 'elementkind' attribute on API tooling markers
	 * Value is: <code>elementkind</code>
	 */
	public static final String MARKER_ATTR_ELEMENT_KIND = "elementkind"; //$NON-NLS-1$
	/**
	 * Constant representing the handle id attribute of a java element.
	 * Value is: <code>org.eclipse.jdt.internal.core.JavaModelManager.handleId</code>
	 */
	public static final String MARKER_ATT_HANDLE_ID = "org.eclipse.jdt.internal.core.JavaModelManager.handleId" ; //$NON-NLS-1$
	/**
	 * Constant representing the name of the @since tag version attribute on API tooling markers,
	 * or the new value for the bundle version.
	 * Value is: <code>version</code>
	 */
	public static final String MARKER_ATTR_VERSION = "version"; //$NON-NLS-1$
	/**
	 * Constant representing the id for the default API profile problem marker.
	 * Value is: <code>org.eclipse.pde.api.tools.api_profile</code>
	 */
	public static final String DEFAULT_API_PROFILE_PROBLEM_MARKER = ApiPlugin.PLUGIN_ID + ".api_profile"; //$NON-NLS-1$
	/**
	 * Constant representing the id for the binary compatibility problem marker.
	 * Value is: <code>org.eclipse.pde.api.tools.binary_compatibility</code> 
	 */
	public static final String BINARY_COMPATIBILITY_PROBLEM_MARKER = ApiPlugin.PLUGIN_ID + ".binary_compatibility"; //$NON-NLS-1$
	/**
	 * Constant representing the id for the API usage problem marker.
	 * Value is: <code>org.eclipse.pde.api.tools.api_usage</code> 
	 */
	public static final String API_USAGE_PROBLEM_MARKER = ApiPlugin.PLUGIN_ID + ".api_usage"; //$NON-NLS-1$
	/**
	 * Constant representing the id for the version numbering problem marker.
	 * Value is: <code>org.eclipse.pde.api.tools.version_numbering</code> 
	 */
	public static final String VERSION_NUMBERING_PROBLEM_MARKER = ApiPlugin.PLUGIN_ID + ".version_numbering"; //$NON-NLS-1$
	/**
	 * Constant representing the id for the missing @since tag problem marker.
	 * Value is: <code>org.eclipse.pde.api.tools.marker.sincetags</code> 
	 */
	public static final String SINCE_TAGS_PROBLEM_MARKER = ApiPlugin.PLUGIN_ID + ".marker.sincetags"; //$NON-NLS-1$
}
