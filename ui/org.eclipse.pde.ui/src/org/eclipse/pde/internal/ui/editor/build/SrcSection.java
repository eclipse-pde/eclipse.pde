/*******************************************************************************
 *  Copyright (c) 2000, 2013 IBM Corporation and others.
 *
 *  This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License 2.0
 *  which accompanies this distribution, and is available at
 *  https://www.eclipse.org/legal/epl-2.0/
 *
 *  SPDX-License-Identifier: EPL-2.0
 *
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.pde.internal.ui.editor.build;

import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IResource;
import org.eclipse.pde.core.build.IBuild;
import org.eclipse.pde.core.build.IBuildEntry;
import org.eclipse.pde.internal.build.IBuildPropertiesConstants;
import org.eclipse.pde.internal.ui.PDEUIMessages;
import org.eclipse.swt.widgets.Composite;

public class SrcSection extends BuildContentsSection {

	public SrcSection(BuildPage page, Composite parent) {
		super(page, parent);
		getSection().setText(PDEUIMessages.BuildEditor_SrcSection_title);
		getSection().setDescription(PDEUIMessages.BuildEditor_SrcSection_desc);

	}

	@Override
	protected void initializeCheckState() {

		super.initializeCheckState();
		IBuild build = fBuildModel.getBuild();
		IBuildEntry srcIncl = build.getEntry(IBuildPropertiesConstants.PROPERTY_SRC_INCLUDES);
		IBuildEntry srcExcl = build.getEntry(IBuildPropertiesConstants.PROPERTY_SRC_EXCLUDES);

		if (srcIncl == null)
			return;

		super.initializeCheckState(srcIncl, srcExcl);
	}

	@Override
	protected void deleteFolderChildrenFromEntries(IFolder folder) {
		IBuild build = fBuildModel.getBuild();
		IBuildEntry srcIncl = build.getEntry(IBuildPropertiesConstants.PROPERTY_SRC_INCLUDES);
		IBuildEntry srcExcl = build.getEntry(IBuildPropertiesConstants.PROPERTY_SRC_EXCLUDES);
		String parentFolder = getResourceFolderName(folder.getProjectRelativePath().toString());

		removeChildren(srcIncl, parentFolder);
		removeChildren(srcExcl, parentFolder);
	}

	@Override
	protected void handleBuildCheckStateChange(boolean wasTopParentChecked) {
		IResource resource = fParentResource;
		String resourceName = fParentResource.getProjectRelativePath().makeRelativeTo(fBundleRoot.getProjectRelativePath()).toPortableString();
		IBuild build = fBuildModel.getBuild();
		IBuildEntry includes = build.getEntry(IBuildPropertiesConstants.PROPERTY_SRC_INCLUDES);
		IBuildEntry excludes = build.getEntry(IBuildPropertiesConstants.PROPERTY_SRC_EXCLUDES);

		resourceName = handleResourceFolder(resource, resourceName);

		if (isChecked)
			handleCheck(includes, excludes, resourceName, resource, wasTopParentChecked, IBuildPropertiesConstants.PROPERTY_SRC_INCLUDES);
		else
			handleUncheck(includes, excludes, resourceName, resource, IBuildPropertiesConstants.PROPERTY_SRC_EXCLUDES);

		deleteEmptyEntries();
	}
}
