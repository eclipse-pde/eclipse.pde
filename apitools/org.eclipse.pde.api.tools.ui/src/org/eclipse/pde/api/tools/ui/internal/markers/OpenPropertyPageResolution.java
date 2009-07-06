/*******************************************************************************
 * Copyright (c) 2008, 2009 IBM Corporation and others.
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
import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.osgi.util.NLS;
import org.eclipse.pde.api.tools.ui.internal.ApiUIPlugin;
import org.eclipse.pde.api.tools.ui.internal.IApiToolsConstants;
import org.eclipse.pde.api.tools.ui.internal.SWTFactory;
import org.eclipse.swt.graphics.Image;
import org.eclipse.ui.IMarkerResolution2;
import org.eclipse.ui.progress.UIJob;

/**
 * This resolution is used to open a property page
 * 
 * @since 1.0.0
 */
public class OpenPropertyPageResolution implements IMarkerResolution2 {

	/**
	 * A human readable name for the page
	 */
	private String fPageName = null;
	
	/**
	 * The id of the property page to open
	 */
	String fPageId = null;
	
	/**
	 * The element the page is to be opened on
	 */
	IAdaptable fElement = null;
	
	/**
	 * Constructor
	 * @param pageid
	 */
	public OpenPropertyPageResolution(String pagename, String pageid, IAdaptable element) {
		Assert.isNotNull(pagename, MarkerMessages.OpenPropertyPageResolution_the_page_name_cannot_be_null);
		fPageName = pagename;
		Assert.isNotNull(pageid, MarkerMessages.OpenPropertyPageResolution_page_id_cannot_be_null);
		fPageId = pageid;
		Assert.isNotNull(element, MarkerMessages.OpenPropertyPageResolution_element_cannot_be_null);
		fElement = element;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.ui.IMarkerResolution2#getDescription()
	 */
	public String getDescription() {
		return NLS.bind(MarkerMessages.OpenPropertyPageResolution_opens_the_property_page, fPageName);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ui.IMarkerResolution2#getImage()
	 */
	public Image getImage() {
		return ApiUIPlugin.getSharedImage(IApiToolsConstants.IMG_ELCL_OPEN_PAGE);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ui.IMarkerResolution#getLabel()
	 */
	public String getLabel() {
		return NLS.bind(MarkerMessages.OpenPropertyPageResolution_open_the_property_page, fPageName);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ui.IMarkerResolution#run(org.eclipse.core.resources.IMarker)
	 */
	public void run(IMarker marker) {
		UIJob job = new UIJob(MarkerMessages.OpenPropertyPageResolution_opening_property_page_job_name) {
			public IStatus runInUIThread(IProgressMonitor monitor) {
				SWTFactory.showPropertiesDialog(ApiUIPlugin.getShell(), fPageId, fElement, null);
				return Status.OK_STATUS;
			}
		};
		job.setSystem(true);
		job.setPriority(Job.INTERACTIVE);
		job.schedule();
	}
}
