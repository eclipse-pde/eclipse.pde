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
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.pde.core.IModelChangedListener;
import org.eclipse.pde.core.build.IBuild;
import org.eclipse.pde.core.build.IBuildEntry;
import org.eclipse.pde.internal.build.IXMLConstants;
import org.eclipse.pde.internal.ui.PDEPlugin;

public class BinSection
	extends BuildContentsSection
	implements IModelChangedListener {

	private static String SECTION_TITLE =
		"BuildPropertiesEditor.BinSection.title";
	private static String SECTION_DESC =
		"BuildPropertiesEditor.BinSection.desc";

	public BinSection(BuildPage page) {
		super(page);
		this.setHeaderText(PDEPlugin.getResourceString(SECTION_TITLE));
		this.setDescription(PDEPlugin.getResourceString(SECTION_DESC));
	}

	protected void initializeCheckState() {
		super.initializeCheckState();
		IBuild build = buildModel.getBuild();
		IBuildEntry binIncl = build.getEntry(IXMLConstants.PROPERTY_BIN_INCLUDES);
		IBuildEntry binExcl = build.getEntry(IXMLConstants.PROPERTY_BIN_EXCLUDES);
		if (binIncl == null)
			return;
		super.initializeCheckState(binIncl, binExcl);

	}

	protected void deleteFolderChildrenFromEntries(IFolder folder) {
		IBuild build = buildModel.getBuild();
		IBuildEntry binIncl = build.getEntry(IXMLConstants.PROPERTY_BIN_INCLUDES);
		IBuildEntry binExcl = build.getEntry(IXMLConstants.PROPERTY_BIN_EXCLUDES);
		String[] tokens;
		String parentFolder = folder.getProjectRelativePath().toString();
		try {
			if (binIncl != null) {
				tokens = binIncl.getTokens();
				for (int i = 0; i < tokens.length; i++) {
					if (tokens[i].indexOf(Path.SEPARATOR) != -1
						&& tokens[i].startsWith(parentFolder)) {
						binIncl.removeToken(tokens[i]);
					}
				}
			}

			if (binExcl != null) {
				tokens = binExcl.getTokens();
				for (int i = 0; i < tokens.length; i++) {
					if (tokens[i].indexOf(Path.SEPARATOR) != -1
						&& tokens[i].startsWith(parentFolder)) {
						binExcl.removeToken(tokens[i]);
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
		boolean isParentGrayed = treeViewer.getGrayed(resource.getParent());
		String resourceName = resource.getFullPath().removeFirstSegments(1).toString();
		if (resource instanceof IFolder){
			resourceName = resourceName + Path.SEPARATOR;
			deleteFolderChildrenFromEntries((IFolder)resource);
		}
		
		IBuild build = buildModel.getBuild();
		IBuildEntry includes = build.getEntry(IXMLConstants.PROPERTY_BIN_INCLUDES);
		IBuildEntry excludes = build.getEntry(IXMLConstants.PROPERTY_BIN_EXCLUDES);

		try {
			if (checked){
				if (includes==null){
					includes = buildModel.getFactory().createEntry(IXMLConstants.PROPERTY_BIN_INCLUDES);
					build.add(includes);
				}
				if (!wasTopParentChecked && !includes.contains(resourceName) ||
					isParentGrayed && wasTopParentChecked && (excludes!=null ? !excludes.contains(resourceName) : true)){
					includes.addToken(resourceName);
				}
				if (excludes !=null && excludes.contains(resourceName))
					excludes.removeToken(resourceName);
			} else {
				if(treeViewer.getChecked(resource.getParent())){
					if (excludes == null){
						excludes = buildModel.getFactory().createEntry(IXMLConstants.PROPERTY_BIN_EXCLUDES);
						build.add(excludes);
					}
					if (!excludes.contains(resourceName) && (includes !=null ? !includes.contains(resourceName) : true))
						excludes.addToken(resourceName);
				}
				if (includes !=null){
					if (includes.contains(resourceName))
						includes.removeToken(resourceName);
					if (includes.contains("*." + resource.getFileExtension())) {
						IResource[] members = project.members();
						for (int i = 0; i<members.length; i++){
							if (!(members[i] instanceof IFolder) &&
								!members[i].getName().equals(resource.getName())
								&& (resource.getFileExtension().equals(members[i].getFileExtension()))) {
									includes.addToken(members[i].getName());
							}
	
							IBuildEntry[] libraries = BuildUtil.getBuildLibraries(buildModel.getBuild().getBuildEntries());
							if (resource.getFileExtension().equals("jar") && libraries.length!=0){
								for (int j=0; j<libraries.length; j++){
									String libName = libraries[j].getName().substring(7);
									IPath path = project.getFile(libName).getProjectRelativePath();
									if (path.segmentCount()==1 && !includes.contains(libName) && !libName.equals(resource.getName()))
										includes.addToken(libName);
								}
							}
						}
						includes.removeToken("*." + resource.getFileExtension());
					}
				}

			}
			deleteEmptyEntries();
		} catch (CoreException e) {
			PDEPlugin.logException(e);
		}
	}
}
