/*******************************************************************************
 * Copyright (c) 2009, 2013 IBM Corporation and others.
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
	 *
	 * @param marker
	 */
	public DuplicateTagResolution(IMarker marker) {
		super(marker);
	}

	@Override
	public String getLabel() {
		try {
			String arg = (String) fBackingMarker.getAttribute(IApiMarkerConstants.MARKER_ATTR_MESSAGE_ARGUMENTS);
			String[] args = arg.split("#"); //$NON-NLS-1$
			return NLS.bind(MarkerMessages.DuplicateTagResolution_remove_dupe_tag_resolution_label, new String[] { args[0] });
		} catch (CoreException e) {
		}
		return null;
	}
}
