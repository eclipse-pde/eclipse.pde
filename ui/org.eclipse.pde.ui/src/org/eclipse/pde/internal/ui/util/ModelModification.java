/*******************************************************************************
 *  Copyright (c) 2005, 2010 IBM Corporation and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 * 
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.util;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.*;
import org.eclipse.pde.core.IBaseModel;
import org.eclipse.pde.internal.core.ICoreConstants;
import org.eclipse.pde.internal.core.project.PDEProject;

/**
 * ModelModification class used my the PDEModelUtility
 * Subclass me to create changes to your models.
 *
 */
public abstract class ModelModification {

	private IFile fModelFile;
	private IFile fManifestFile;
	private IFile fXMLFile;
	private IFile fPropertiesFile;
	private boolean fIsBundleModel;

	/**
	 * Create a single model modification - used for modifying single AbstractEditingModels
	 * @param modelFile the basic underlying file for the model you wish to modify.
	 */
	public ModelModification(IFile modelFile) {
		singleFileModification(modelFile);
	}

	/**
	 * Create a full IBundlePluginModelBase modification
	 * @param bundleFile the MANIFEST.MF file
	 * @param xmlFile the plugin.xml/fragment.xml file for this modification (optional - can be null)
	 * @pre bundleFile must not be <code>null</code>
	 */
	public ModelModification(IFile bundleFile, IFile xmlFile) {
		createFullBundleModification(bundleFile, xmlFile);
	}

	/**
	 * Create a ModelModification based on the contents of the project
	 * ie. if the project contains a MANIFEST.MF this will be tagged as a 
	 * fullBundleModification, otherwise (this project is an old-style plugin)
	 * this will be a PluginModel/FragmentModel modification.
	 * @param project
	 */
	public ModelModification(IProject project) {
		IFile xml = PDEProject.getPluginXml(project);
		if (!xml.exists())
			xml = PDEProject.getFragmentXml(project);
		if (!xml.exists())
			xml = null;
		IFile manifest = PDEProject.getManifest(project);
		if (!manifest.exists() && xml != null)
			singleFileModification(xml);
		else if (manifest.exists())
			createFullBundleModification(manifest, xml);
	}

	private void singleFileModification(IFile file) {
		assignFile(file);
		if (fManifestFile != null)
			fModelFile = fManifestFile;
		else if (fXMLFile != null)
			fModelFile = fXMLFile;
		else if (fPropertiesFile != null)
			fModelFile = fPropertiesFile;
		fIsBundleModel = file.getName().equals(ICoreConstants.MANIFEST_FILENAME);
	}

	private void createFullBundleModification(IFile bundleFile, IFile xmlFile) {
		assignFile(bundleFile);
		assignFile(xmlFile);

		Assert.isNotNull(fManifestFile);
		fModelFile = fManifestFile;
		fIsBundleModel = true;
	}

	private void assignFile(IFile file) {
		if (file == null)
			return;
		String name = file.getName();
		if (name.equals(ICoreConstants.MANIFEST_FILENAME))
			fManifestFile = file;
		else if (name.equals(ICoreConstants.PLUGIN_FILENAME_DESCRIPTOR) || name.equals(ICoreConstants.FRAGMENT_FILENAME_DESCRIPTOR))
			fXMLFile = file;
		else if (name.endsWith(PDEModelUtility.F_PROPERTIES))
			fPropertiesFile = file;
	}

	/**
	 * Invoke this using PDEModelUtility.modifyModel(ModelModification modification)
	 * Clients / subclasses should not invoke this method.
	 * @param model
	 * @param monitor
	 * @throws CoreException
	 */
	protected abstract void modifyModel(IBaseModel model, IProgressMonitor monitor) throws CoreException;

	protected final IFile getFile() {
		return fModelFile;
	}

	protected final IFile getManifestFile() {
		return fManifestFile;
	}

	protected final IFile getXMLFile() {
		return fXMLFile;
	}

	protected final IFile getPropertiesFile() {
		return fPropertiesFile;
	}

	protected final boolean isFullBundleModification() {
		return fIsBundleModel;
	}

	public boolean saveOpenEditor() {
		return true;
	}
}
