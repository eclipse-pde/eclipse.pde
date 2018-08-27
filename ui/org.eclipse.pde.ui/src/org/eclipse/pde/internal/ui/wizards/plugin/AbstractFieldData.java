/*******************************************************************************
 *  Copyright (c) 2000, 2015 IBM Corporation and others.
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
 *     Gary Duprex <Gary.Duprex@aspectstools.com> - bug 179213
 *     Johannes Ahlers <Johannes.Ahlers@gmx.de> - bug 477677
 *******************************************************************************/
package org.eclipse.pde.internal.ui.wizards.plugin;

import org.eclipse.pde.ui.IFieldData;
import org.eclipse.ui.IWorkingSet;

public abstract class AbstractFieldData implements IFieldData {

	private String fId;
	private String fVersion;
	private String fName;
	private String fProvider;
	private boolean fLegacy;
	private String fLibraryName;
	private String fSourceFolderName;
	private String fOutputFolderName;
	private boolean fHasBundleStructure;
	private boolean fSimple;
	private String fTargetVersion = "3.1"; //$NON-NLS-1$
	private String fFramework;
	private IWorkingSet[] fWorkingSets;
	private String fExecutionEnvironment;

	@Override
	public String getId() {
		return fId;
	}

	@Override
	public String getVersion() {
		return fVersion;
	}

	@Override
	public String getName() {
		return fName;
	}

	@Override
	public String getProvider() {
		return fProvider;
	}

	@Override
	public boolean isLegacy() {
		return fLegacy;
	}

	@Override
	public String getLibraryName() {
		return fLibraryName;
	}

	@Override
	public String getSourceFolderName() {
		return fSourceFolderName;
	}

	@Override
	public String getOutputFolderName() {
		return fOutputFolderName;
	}

	@Override
	public boolean hasBundleStructure() {
		return fHasBundleStructure;
	}

	@Override
	public boolean isSimple() {
		return fSimple;
	}

	public void setId(String id) {
		fId = id;
	}

	public void setName(String name) {
		fName = name;
	}

	public void setProvider(String provider) {
		fProvider = provider;
	}

	public void setVersion(String version) {
		fVersion = version;
	}

	public void setLegacy(boolean isLegacy) {
		fLegacy = isLegacy;
	}

	public void setLibraryName(String name) {
		fLibraryName = name;
	}

	public void setSourceFolderName(String name) {
		fSourceFolderName = name;
	}

	public void setOutputFolderName(String name) {
		fOutputFolderName = name;
	}

	public void setHasBundleStructure(boolean isBundle) {
		fHasBundleStructure = isBundle;
	}

	public void setSimple(boolean simple) {
		fSimple = simple;
	}

	public String getTargetVersion() {
		return fTargetVersion;
	}

	public void setTargetVersion(String version) {
		fTargetVersion = version;
	}

	public String getOSGiFramework() {
		return fFramework;
	}

	public void setOSGiFramework(String framework) {
		fFramework = framework;
	}

	public IWorkingSet[] getWorkingSets() {
		return fWorkingSets;
	}

	public void setWorkingSets(IWorkingSet[] workingSets) {
		fWorkingSets = workingSets;
	}

	public void setExecutionEnvironment(String executionEnvironment) {
		fExecutionEnvironment = executionEnvironment;
	}

	public String getExecutionEnvironment() {
		return fExecutionEnvironment;
	}
}
