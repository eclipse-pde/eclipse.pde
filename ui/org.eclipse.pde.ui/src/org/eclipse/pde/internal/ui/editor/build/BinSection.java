/*******************************************************************************
 *  Copyright (c) 2000, 2010 IBM Corporation and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 * 
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.pde.internal.ui.editor.build;

import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.Path;
import org.eclipse.pde.core.IModelChangedEvent;
import org.eclipse.pde.core.IModelChangedListener;
import org.eclipse.pde.core.build.IBuild;
import org.eclipse.pde.core.build.IBuildEntry;
import org.eclipse.pde.internal.build.IBuildPropertiesConstants;
import org.eclipse.pde.internal.ui.PDEUIMessages;
import org.eclipse.swt.widgets.Composite;

public class BinSection extends BuildContentsSection implements IModelChangedListener {

	public BinSection(BuildPage page, Composite parent) {
		super(page, parent);
		getSection().setText(PDEUIMessages.BuildEditor_BinSection_title);
		getSection().setDescription(PDEUIMessages.BuildEditor_BinSection_desc);
	}

	protected void initializeCheckState() {
		super.initializeCheckState();
		IBuild build = fBuildModel.getBuild();
		IBuildEntry binIncl = build.getEntry(IBuildPropertiesConstants.PROPERTY_BIN_INCLUDES);
		IBuildEntry binExcl = build.getEntry(IBuildPropertiesConstants.PROPERTY_BIN_EXCLUDES);

		if (binIncl == null)
			return;

		super.initializeCheckState(binIncl, binExcl);
	}

	protected void deleteFolderChildrenFromEntries(IFolder folder) {
		IBuild build = fBuildModel.getBuild();
		IBuildEntry binIncl = build.getEntry(IBuildPropertiesConstants.PROPERTY_BIN_INCLUDES);
		IBuildEntry binExcl = build.getEntry(IBuildPropertiesConstants.PROPERTY_BIN_EXCLUDES);
		String parentFolder = getResourceFolderName(folder.getProjectRelativePath().toString());

		removeChildren(binIncl, parentFolder);
		removeChildren(binExcl, parentFolder);
	}

	protected void handleBuildCheckStateChange(boolean wasTopParentChecked) {
		IResource resource = fParentResource;
		String resourceName = fParentResource.getProjectRelativePath().makeRelativeTo(fBundleRoot.getProjectRelativePath()).toPortableString();
		IBuild build = fBuildModel.getBuild();
		IBuildEntry includes = build.getEntry(IBuildPropertiesConstants.PROPERTY_BIN_INCLUDES);
		IBuildEntry excludes = build.getEntry(IBuildPropertiesConstants.PROPERTY_BIN_EXCLUDES);

		resourceName = handleResourceFolder(resource, resourceName);

		if (isChecked)
			handleCheck(includes, excludes, resourceName, resource, wasTopParentChecked, IBuildPropertiesConstants.PROPERTY_BIN_INCLUDES);
		else
			handleUncheck(includes, excludes, resourceName, resource, IBuildPropertiesConstants.PROPERTY_BIN_EXCLUDES);

		deleteEmptyEntries();
		fParentResource = fOriginalResource = null;
	}

	public void modelChanged(IModelChangedEvent event) {
		if (event.getChangeType() == IModelChangedEvent.WORLD_CHANGED) {
			markStale();
		}
		Object changeObject = event.getChangedObjects()[0];

		if (!(changeObject instanceof IBuildEntry && (((IBuildEntry) changeObject).getName().equals(IBuildPropertiesConstants.PROPERTY_BIN_EXCLUDES) || ((IBuildEntry) changeObject).getName().equals(IBuildPropertiesConstants.PROPERTY_BIN_INCLUDES))))
			return;

		if ((fParentResource == null && fOriginalResource != null) || (fOriginalResource == null && fParentResource != null)) {
			initializeCheckState();
			return;
		}
		if (fParentResource == null && fOriginalResource == null) {
			if (event.getChangedProperty() != null && event.getChangedProperty().equals(IBuildPropertiesConstants.PROPERTY_BIN_INCLUDES)) {

				if (event.getOldValue() == null && event.getNewValue() != null) {
					if (event.getNewValue().equals(".")) //$NON-NLS-1$
						return;
					// adding token
					IFile file = fBundleRoot.getFile(new Path(event.getNewValue().toString()));
					if (!file.exists())
						return;
					fParentResource = fOriginalResource = file;
					isChecked = true;
				} else if (event.getOldValue() != null && event.getNewValue() == null) {
					if (event.getOldValue().equals(".")) //$NON-NLS-1$
						return;
					// removing token
					IFile file = fBundleRoot.getFile(new Path(event.getOldValue().toString()));
					if (!file.exists())
						return;
					fParentResource = fOriginalResource = file;
					isChecked = false;
				} else {
					return;
				}
			}
			return;
		}
		fTreeViewer.setChecked(fParentResource, isChecked);
		fTreeViewer.setGrayed(fOriginalResource, false);
		fTreeViewer.setParentsGrayed(fParentResource, true);
		setParentsChecked(fParentResource);
		fTreeViewer.setGrayed(fParentResource, false);
		if (fParentResource instanceof IFolder) {
			fTreeViewer.setSubtreeChecked(fParentResource, isChecked);
			setChildrenGrayed(fParentResource, false);
		}
		while (!fOriginalResource.equals(fParentResource)) {
			fTreeViewer.setChecked(fOriginalResource, isChecked);
			fOriginalResource = fOriginalResource.getParent();
		}
		fParentResource = null;
		fOriginalResource = null;
	}
}
