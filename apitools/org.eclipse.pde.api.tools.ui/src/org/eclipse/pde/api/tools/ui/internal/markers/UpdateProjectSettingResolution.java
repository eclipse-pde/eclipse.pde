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
package org.eclipse.pde.api.tools.ui.internal.markers;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.pde.api.tools.internal.problems.ApiProblemFactory;
import org.eclipse.pde.api.tools.internal.provisional.IApiMarkerConstants;
import org.eclipse.pde.api.tools.ui.internal.ApiUIPlugin;
import org.eclipse.pde.api.tools.ui.internal.preferences.PreferenceMessages;
import org.eclipse.swt.graphics.Image;
import org.eclipse.ui.IMarkerResolution2;

import com.ibm.icu.text.MessageFormat;

/**
 * Marker resolution for adding an API filter for the specific member the marker appears on
 * 
 * @since 1.0.0
 */
public class UpdateProjectSettingResolution implements IMarkerResolution2 {

	protected IMarker fBackingMarker = null;
	protected IJavaElement fResolvedElement = null;
	protected String fCategory = null;
	
	/**
	 * Constructor
	 * @param marker the backing marker for the resolution
	 */
	public UpdateProjectSettingResolution(IMarker marker) {
		fBackingMarker = marker;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.ui.IMarkerResolution2#getDescription()
	 */
	public String getDescription() {
		try {
			String value = (String) fBackingMarker.getAttribute(IApiMarkerConstants.MARKER_ATTR_MESSAGE_ARGUMENTS);
			String[] args = new String[0];
			if(value != null) {
				args = value.split("#"); //$NON-NLS-1$
			}
			int id = fBackingMarker.getAttribute(IApiMarkerConstants.MARKER_ATTR_PROBLEM_ID, 0);
			return ApiProblemFactory.getLocalizedMessage(ApiProblemFactory.getProblemMessageId(id), args);
		} catch (CoreException e) {}
		return null;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.ui.IMarkerResolution2#getImage()
	 */
	public Image getImage() {
		return null;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ui.IMarkerResolution#getLabel()
	 */
	public String getLabel() {
		return MessageFormat.format(MarkerMessages.UpdateProjectSettingsResolution_0, new String[] { PreferenceMessages.ReportApiComponentResolutionFailureDescription });
	}
	/**
	 * Resolves the {@link IJavaElement} from the infos in the marker.
	 * 
	 * @return the associated {@link IJavaElement} for the infos in the {@link IMarker}
	 */
	protected IJavaElement resolveElementFromMarker() {
		if(fResolvedElement == null) {
			try {
				String handle = (String) fBackingMarker.getAttribute(IApiMarkerConstants.MARKER_ATTR_HANDLE_ID);
				if(handle != null) {
					fResolvedElement = JavaCore.create(handle);
				}
			}
			catch(CoreException ce) {
				ApiUIPlugin.log(ce);
			}
		}
		return fResolvedElement;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.ui.IMarkerResolution#run(org.eclipse.core.resources.IMarker)
	 */
	public void run(IMarker marker) {
		UpdateProjectSettingsOperation op = new UpdateProjectSettingsOperation(fBackingMarker);
		op.setSystem(true);
		op.schedule();
	}
}
