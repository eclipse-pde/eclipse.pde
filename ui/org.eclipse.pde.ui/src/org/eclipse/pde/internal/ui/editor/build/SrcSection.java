/*******************************************************************************
 * Copyright (c) 2000, 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.pde.internal.ui.editor.build;

import org.eclipse.core.resources.*;
import org.eclipse.pde.core.IModelChangedListener;
import org.eclipse.pde.core.build.*;
import org.eclipse.pde.internal.build.IBuildPropertiesConstants;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.swt.widgets.Composite;

public class SrcSection extends BuildContentsSection
		implements
			IModelChangedListener {

	private static String SECTION_TITLE = "BuildEditor.SrcSection.title"; //$NON-NLS-1$
	private static String SECTION_DESC = "BuildEditor.SrcSection.desc"; //$NON-NLS-1$

	public SrcSection(BuildPage page, Composite parent) {
		super(page, parent);
		getSection().setText(PDEPlugin.getResourceString(SECTION_TITLE));
		getSection().setDescription(PDEPlugin.getResourceString(SECTION_DESC));

	}

	protected void initializeCheckState() {

		super.initializeCheckState();
		IBuild build = fBuildModel.getBuild();
		IBuildEntry srcIncl = build
				.getEntry(IBuildPropertiesConstants.PROPERTY_SRC_INCLUDES);
		IBuildEntry srcExcl = build
				.getEntry(IBuildPropertiesConstants.PROPERTY_SRC_EXCLUDES);

		if (srcIncl == null)
			return;

		super.initializeCheckState(srcIncl, srcExcl);
	}

	protected void deleteFolderChildrenFromEntries(IFolder folder) {
		IBuild build = fBuildModel.getBuild();
		IBuildEntry srcIncl = build
				.getEntry(IBuildPropertiesConstants.PROPERTY_SRC_INCLUDES);
		IBuildEntry srcExcl = build
				.getEntry(IBuildPropertiesConstants.PROPERTY_SRC_EXCLUDES);
		String parentFolder = getResourceFolderName(folder
				.getProjectRelativePath().toString());

		removeChildren(srcIncl, parentFolder);
		removeChildren(srcExcl, parentFolder);
	}

	protected void handleBuildCheckStateChange(boolean wasTopParentChecked) {
		IResource resource = fParentResource;
		String resourceName = fParentResource.getFullPath()
				.removeFirstSegments(1).toString();
		IBuild build = fBuildModel.getBuild();
		IBuildEntry includes = build
				.getEntry(IBuildPropertiesConstants.PROPERTY_SRC_INCLUDES);
		IBuildEntry excludes = build
				.getEntry(IBuildPropertiesConstants.PROPERTY_SRC_EXCLUDES);

		resourceName = handleResourceFolder(resource, resourceName);

		if (isChecked)
			handleCheck(includes, excludes, resourceName, resource,
					wasTopParentChecked,
					IBuildPropertiesConstants.PROPERTY_SRC_INCLUDES);
		else
			handleUncheck(includes, excludes, resourceName, resource,
					IBuildPropertiesConstants.PROPERTY_SRC_EXCLUDES);

		deleteEmptyEntries();
	}
}
