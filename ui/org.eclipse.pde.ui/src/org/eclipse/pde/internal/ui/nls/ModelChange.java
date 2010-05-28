/*******************************************************************************
 * Copyright (c) 2005, 2010 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.nls;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Properties;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.pde.core.IModel;
import org.eclipse.pde.core.plugin.IPluginModelBase;
import org.eclipse.pde.internal.core.PDEManager;
import org.eclipse.pde.internal.core.project.PDEProject;

public class ModelChange {

	private static final String DEFAULT_LOCALIZATION_PREFIX = "plugin"; //$NON-NLS-1$
	public static final String LOCALIZATION_FILE_SUFFIX = ".properties"; //$NON-NLS-1$

	private ModelChangeFile fXMLCoupling;
	private ModelChangeFile fMFCoupling;

	private IPluginModelBase fParent;
	private boolean fPreSelected;

	private String fBundleLocalization;
	private Properties fProperties;
	private boolean fReloadProperties = true;

	protected static boolean modelLoaded(IModel model) {
		try {
			model.load();
		} catch (CoreException e) {
		}
		return (model.isLoaded());
	}

	public ModelChange(IPluginModelBase parent, boolean preSelected) {
		fParent = parent;
		fPreSelected = preSelected;
		fBundleLocalization = PDEManager.getBundleLocalization(fParent);
		if (fBundleLocalization == null)
			fBundleLocalization = DEFAULT_LOCALIZATION_PREFIX;
	}

	public void addChange(IFile file, ModelChangeElement change) {
		if (change == null || file == null)
			return;
		String ext = file.getFileExtension();
		if (ext.equalsIgnoreCase("xml")) //$NON-NLS-1$
			addXMLChange(file, change);
		else if (ext.equalsIgnoreCase("MF")) //$NON-NLS-1$
			addMFChange(file, change);
		else
			return;
	}

	private void addXMLChange(IFile file, ModelChangeElement change) {
		if (fXMLCoupling == null) {
			fXMLCoupling = new ModelChangeFile(file, this);
		}
		if (!fXMLCoupling.getFile().equals(file)) {
			return;
		}
		fXMLCoupling.add(change);
	}

	private void addMFChange(IFile file, ModelChangeElement change) {
		if (fMFCoupling == null) {
			fMFCoupling = new ModelChangeFile(file, this);
		}
		fMFCoupling.add(change);
	}

	public IFile[] getChangeFiles() {
		IFile xmlFile = fXMLCoupling != null ? fXMLCoupling.getFile() : null;
		IFile mfFile = fMFCoupling != null ? fMFCoupling.getFile() : null;
		if (xmlFile != null && mfFile != null)
			return new IFile[] {xmlFile, mfFile};
		if (xmlFile != null)
			return new IFile[] {xmlFile};
		if (mfFile != null)
			return new IFile[] {mfFile};
		return new IFile[0];
	}

	public IFile getPropertiesFile() {
		return PDEProject.getLocalizationFile(fParent.getUnderlyingResource().getProject());
	}

	public Properties getProperties() {
		if (fProperties == null || fReloadProperties) {
			try {
				fProperties = new Properties();
				IFile propertiesFile = getPropertiesFile();
				if (propertiesFile != null && propertiesFile.exists()) {
					InputStream stream = propertiesFile.getContents();
					fProperties.load(stream);
					stream.close();
				}
			} catch (CoreException e) {
			} catch (IOException e) {
			}
			fReloadProperties = false;
		}
		return fProperties;
	}

	public ArrayList getChangesInFile(IFile file) {
		if (fXMLCoupling != null && file == fXMLCoupling.getFile())
			return fXMLCoupling.getChanges();
		if (fMFCoupling != null && file == fMFCoupling.getFile())
			return fMFCoupling.getChanges();
		return null;
	}

	public int getNumberOfChangesInFile(IFile file) {
		if (fXMLCoupling != null && file == fXMLCoupling.getFile())
			return fXMLCoupling.getNumChanges();
		if (fMFCoupling != null && file == fMFCoupling.getFile())
			return fMFCoupling.getNumChanges();
		return 0;
	}

	public boolean wasPreSelected() {
		return fPreSelected;
	}

	public IPluginModelBase getParentModel() {
		return fParent;
	}

	public ModelChangeFile[] getModelChangeFiles() {
		if (fXMLCoupling != null && fMFCoupling != null)
			return new ModelChangeFile[] {fXMLCoupling, fMFCoupling};
		if (fXMLCoupling != null)
			return new ModelChangeFile[] {fXMLCoupling};
		if (fMFCoupling != null)
			return new ModelChangeFile[] {fMFCoupling};
		return new ModelChangeFile[0];
	}

	public void setBundleLocalization(String bundleLocalization) {
		if (bundleLocalization == null || bundleLocalization.endsWith(LOCALIZATION_FILE_SUFFIX))
			throw new IllegalArgumentException();
		if (bundleLocalization.equals(fBundleLocalization))
			return;
		fBundleLocalization = bundleLocalization;
		fReloadProperties = true;
	}

	public String getBundleLocalization() {
		return fBundleLocalization;
	}

	public boolean localizationSet() {
		String localization = PDEManager.getBundleLocalization(fParent);
		return localization != null && localization.length() > 0;
	}
}
