/*******************************************************************************
 * Copyright (c) 2000, 2003 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.pde.internal.ui.editor.build;

import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IResource;
import org.eclipse.pde.core.IModelChangedListener;
import org.eclipse.pde.core.build.IBuild;
import org.eclipse.pde.core.build.IBuildEntry;
import org.eclipse.pde.internal.build.IBuildPropertiesConstants;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.swt.widgets.Composite;

public class BinSection
	extends BuildContentsSection
	implements IModelChangedListener {

	private static String SECTION_TITLE =
		"BuildEditor.BinSection.title";
	private static String SECTION_DESC =
		"BuildEditor.BinSection.desc";

	public BinSection(BuildPage page, Composite parent) {
		super(page, parent);
		getSection().setText(PDEPlugin.getResourceString(SECTION_TITLE));
		getSection().setDescription(PDEPlugin.getResourceString(SECTION_DESC));
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

	protected void handleBuildCheckStateChange(
		IResource resource,
		boolean checked,
		boolean wasTopParentChecked) {
		String resourceName = resource.getFullPath().removeFirstSegments(1).toString();
		IBuild build = fBuildModel.getBuild();
		IBuildEntry includes = build.getEntry(IBuildPropertiesConstants.PROPERTY_BIN_INCLUDES);
		IBuildEntry excludes = build.getEntry(IBuildPropertiesConstants.PROPERTY_BIN_EXCLUDES);

		resourceName = handleResourceFolder(resource, resourceName);

		if (checked)
			handleCheck(includes, excludes, resourceName, resource, wasTopParentChecked, IBuildPropertiesConstants.PROPERTY_BIN_INCLUDES);
		else
			handleUncheck(includes, excludes, resourceName, resource, IBuildPropertiesConstants.PROPERTY_BIN_EXCLUDES);

		deleteEmptyEntries();
	}
}
