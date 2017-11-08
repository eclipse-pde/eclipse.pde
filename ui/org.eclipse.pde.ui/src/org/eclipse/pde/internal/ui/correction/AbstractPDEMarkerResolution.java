/*******************************************************************************
 *  Copyright (c) 2005, 2017 IBM Corporation and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 *
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.correction;

import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.pde.core.IBaseModel;
import org.eclipse.pde.internal.ui.PDEPluginImages;
import org.eclipse.pde.internal.ui.util.ModelModification;
import org.eclipse.pde.internal.ui.util.PDEModelUtility;
import org.eclipse.swt.graphics.Image;
import org.eclipse.ui.views.markers.WorkbenchMarkerResolution;

public abstract class AbstractPDEMarkerResolution extends WorkbenchMarkerResolution {

	public static final int CREATE_TYPE = 1;
	public static final int RENAME_TYPE = 2;
	public static final int REMOVE_TYPE = 3;
	public static final int CONFIGURE_TYPE = 4;

	protected Image image = null;
	protected int fType;
	/**
	 * This variable will only be available after run() is called.
	 * ie. its not the be used in getImage()/getType()/getDesciption()
	 */
	protected IResource fResource;

	public AbstractPDEMarkerResolution(int type) {
		fType = type;
	}

	@Override
	public IMarker[] findOtherMarkers(IMarker[] markers) {
		return new IMarker[0];
	}

	@Override
	public Image getImage() {
		if (image == null) {
			switch (this.getType()) {
			case AbstractPDEMarkerResolution.CREATE_TYPE:
				image = PDEPluginImages.DESC_ADD_ATT.createImage();
				break;
			case AbstractPDEMarkerResolution.REMOVE_TYPE:
				image = PDEPluginImages.DESC_DELETE.createImage();
				break;
			case AbstractPDEMarkerResolution.RENAME_TYPE:
				image = PDEPluginImages.DESC_REFRESH.createImage();
				break;
			case AbstractPDEMarkerResolution.CONFIGURE_TYPE:
				image = PDEPluginImages.DESC_CON_SEV.createImage();
				break;
			}
		}
		return image;
	}

	public int getType() {
		return fType;
	}

	@Override
	public String getDescription() {
		return getLabel();
	}

	@Override
	public void run(IMarker marker) {
		fResource = marker.getResource();
		ModelModification modification = new ModelModification((IFile) marker.getResource()) {
			@Override
			protected void modifyModel(IBaseModel model, IProgressMonitor monitor) throws CoreException {
				createChange(model);
			}
		};
		PDEModelUtility.modifyModel(modification, null);

	}


	@Override
	protected void finalize() throws Throwable {
		super.finalize();
		if (image != null)
			image.dispose();
	}

	protected abstract void createChange(IBaseModel model);

}
