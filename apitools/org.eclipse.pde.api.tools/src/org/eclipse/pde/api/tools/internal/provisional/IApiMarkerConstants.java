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
	 * Constant representing the value of the @since tag missing type attribute on API tooling markers.
	 * Value is: <code>missing</code>
	 */
	public static final String MARKER_ATTR_SINCE_TAG_MISSING = "missing"; //$NON-NLS-1$
	/**
	 * Constant representing the value of the @since tag malformed type attribute on API tooling markers.
	 * Value is: <code>malformed</code>
	 */
	public static final String MARKER_ATTR_SINCE_TAG_MALFORMED = "malformed"; //$NON-NLS-1$
	/**
	 * Constant representing the value of the @since tag invalid type attribute on API tooling markers.
	 * Value is: <code>invalid</code>
	 */
	public static final String MARKER_ATTR_SINCE_TAG_INVALID = "invalid"; //$NON-NLS-1$
	
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
	 * Constant representing the value of the version numbering marker for major version change.
	 * Value is: <code>major</code>
	 */
	public static final String MARKER_ATTR_MAJOR_VERSION_CHANGE = "major"; //$NON-NLS-1$
	/**
	 * Constant representing the value of the version numbering marker for minor version change.
	 * Value is: <code>minor</code>
	 */
	public static final String MARKER_ATTR_MINOR_VERSION_CHANGE = "minor"; //$NON-NLS-1$
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
