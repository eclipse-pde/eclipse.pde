package org.eclipse.pde.api.tools.ui.internal.markers;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.osgi.util.NLS;
import org.eclipse.pde.api.tools.internal.provisional.IApiMarkerConstants;

/**
 * Custom resolution for duplicate javadoc tags
 * 
 * @since 1.0.0
 */
public class DuplicateTagResolution extends UnsupportedTagResolution {

	/**
	 * Constructor
	 * @param marker
	 */
	public DuplicateTagResolution(IMarker marker) {
		super(marker);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ui.IMarkerResolution#getLabel()
	 */
	public String getLabel() {
		try {
			String arg = (String) fBackingMarker.getAttribute(IApiMarkerConstants.MARKER_ATTR_MESSAGE_ARGUMENTS);
			String[] args = arg.split("#"); //$NON-NLS-1$
			return NLS.bind(MarkerMessages.DuplicateTagResolution_remove_dupe_tag_resolution_label, new String[] {args[0]});
		} 
		catch (CoreException e) {}
		return null;
	}
}
