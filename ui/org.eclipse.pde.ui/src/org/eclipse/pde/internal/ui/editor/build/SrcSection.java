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
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.Path;
import org.eclipse.pde.core.IModelChangedListener;
import org.eclipse.pde.core.build.IBuild;
import org.eclipse.pde.core.build.IBuildEntry;
import org.eclipse.pde.internal.build.IXMLConstants;
import org.eclipse.pde.internal.ui.PDEPlugin;

public class SrcSection
	extends BuildContentsSection
	implements IModelChangedListener {

	private static String SECTION_TITLE =
		"BuildPropertiesEditor.SrcSection.title";
	private static String SECTION_DESC =
		"BuildPropertiesEditor.SrcSection.desc";

	public SrcSection(BuildPage page) {
		super(page);
		this.setHeaderText(PDEPlugin.getResourceString(SECTION_TITLE));
		this.setDescription(PDEPlugin.getResourceString(SECTION_DESC));

	}

	protected void initializeCheckState() {
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
		String[] tokens;
		String parentFolder = folder.getProjectRelativePath().toString();
		try {
			if (srcIncl != null) {
				tokens = srcIncl.getTokens();
				for (int i = 0; i < tokens.length; i++) {
					if (tokens[i].indexOf(Path.SEPARATOR) != -1
						&& tokens[i].startsWith(parentFolder)) {
						srcIncl.removeToken(tokens[i]);
					}
				}
			}

			if (srcExcl != null) {
				tokens = srcExcl.getTokens();
				for (int i = 0; i < tokens.length; i++) {
					if (tokens[i].indexOf(Path.SEPARATOR) != -1
						&& tokens[i].startsWith(parentFolder)) {
						srcExcl.removeToken(tokens[i]);
					}
				}
			}
		} catch (CoreException e) {
			PDEPlugin.logException(e);
		}
	}

	protected void handleBuildCheckStateChange(
		IResource resource,
		boolean checked,
		boolean wasTopParentChecked) {
		
		String resourceName = resource.getFullPath().removeFirstSegments(1).toString();
		if (resource instanceof IFolder){
			resourceName = resourceName + Path.SEPARATOR;
			deleteFolderChildrenFromEntries((IFolder)resource);
		}
	
		IBuild build = buildModel.getBuild();
		IBuildEntry includes = build.getEntry(IXMLConstants.PROPERTY_SRC_INCLUDES);
		IBuildEntry excludes = build.getEntry(IXMLConstants.PROPERTY_SRC_EXCLUDES);

		try {
			if (checked){
				if (includes==null){
					includes = buildModel.getFactory().createEntry(IXMLConstants.PROPERTY_SRC_INCLUDES);
					build.add(includes);
				}
				if (excludes !=null && excludes.contains(resourceName))
					excludes.removeToken(resourceName);
				if (!wasTopParentChecked && !includes.contains(resourceName) ||
					excludes!=null && !excludes.contains(resourceName) && wasTopParentChecked){
					includes.addToken(resourceName);
				}
			} else {
				if (includes !=null){
					if (includes.contains(resourceName))
						includes.removeToken(resourceName);
					if (includes.contains("*." + resource.getFileExtension())) 
						includes.removeToken("*." + resource.getFileExtension());
				}
				if(treeViewer.getChecked(resource.getParent())){
					if (excludes == null){
						excludes = buildModel.getFactory().createEntry(IXMLConstants.PROPERTY_SRC_EXCLUDES);
						build.add(excludes);
					}
					if (!excludes.contains(resourceName))
						excludes.addToken(resourceName);
				}
			}
			deleteEmptyEntries();
		} catch (CoreException e) {
			PDEPlugin.logException(e);
		}
	}

}
