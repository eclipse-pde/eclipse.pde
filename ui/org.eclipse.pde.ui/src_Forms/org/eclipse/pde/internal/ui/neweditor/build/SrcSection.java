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

package org.eclipse.pde.internal.ui.neweditor.build;

import org.eclipse.core.resources.*;
import org.eclipse.pde.core.IModelChangedListener;
import org.eclipse.pde.core.build.*;
import org.eclipse.pde.internal.build.IXMLConstants;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.swt.widgets.Composite;

public class SrcSection
	extends BuildContentsSection
	implements IModelChangedListener {

	private static String SECTION_TITLE =
		"BuildPropertiesEditor.SrcSection.title";
	private static String SECTION_DESC =
		"BuildPropertiesEditor.SrcSection.desc";

	public SrcSection(BuildPage page, Composite parent) {
		super(page, parent);
		getSection().setText(PDEPlugin.getResourceString(SECTION_TITLE));
		getSection().setDescription(PDEPlugin.getResourceString(SECTION_DESC));

	}

	protected void initializeCheckState() {
		super.initializeCheckState();
		IBuild build = buildModel.getBuild();
		IBuildEntry srcIncl = build.getEntry(IXMLConstants.PROPERTY_SRC_INCLUDES);
		IBuildEntry srcExcl = build.getEntry(IXMLConstants.PROPERTY_SRC_EXCLUDES);
		
		if (srcIncl == null)
			return;

		super.initializeCheckState(srcIncl, srcExcl);
	}

	protected void deleteFolderChildrenFromEntries(IFolder folder) {
		IBuild build = buildModel.getBuild();
		IBuildEntry srcIncl = build.getEntry(IXMLConstants.PROPERTY_SRC_INCLUDES);
		IBuildEntry srcExcl = build.getEntry(IXMLConstants.PROPERTY_SRC_EXCLUDES);
		String parentFolder = getResourceFolderName(folder.getProjectRelativePath().toString());
		
		removeChildren(srcIncl, parentFolder);
		removeChildren(srcExcl, parentFolder);
	}

	protected void handleBuildCheckStateChange(
		IResource resource,
		boolean checked,
		boolean wasTopParentChecked) {
		String resourceName = resource.getFullPath().removeFirstSegments(1).toString();
		IBuild build = buildModel.getBuild();
		IBuildEntry includes = build.getEntry(IXMLConstants.PROPERTY_SRC_INCLUDES);
		IBuildEntry excludes = build.getEntry(IXMLConstants.PROPERTY_SRC_EXCLUDES);		
		
		resourceName = handleResourceFolder(resource, resourceName);
	
		if (checked)
			handleCheck(includes, excludes, resourceName, resource, wasTopParentChecked, IXMLConstants.PROPERTY_SRC_INCLUDES);
		else
			handleUncheck(includes, excludes, resourceName, resource, IXMLConstants.PROPERTY_SRC_EXCLUDES);
		
		deleteEmptyEntries();
	}
}
