/*******************************************************************************
 *  Copyright (c) 2000, 2008 IBM Corporation and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 * 
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *     Gary Duprex <Gary.Duprex@aspectstools.com> - bug 179213
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

	/* (non-Javadoc)
	 * @see org.eclipse.pde.ui.IFieldData2#getId()
	 */
	public String getId() {
		return fId;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.ui.IFieldData2#getVersion()
	 */
	public String getVersion() {
		return fVersion;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.ui.IFieldData2#getName()
	 */
	public String getName() {
		return fName;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.ui.IFieldData2#getProvider()
	 */
	public String getProvider() {
		return fProvider;
	}

	public boolean isLegacy() {
		return fLegacy;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.ui.IFieldData2#getLibraryName()
	 */
	public String getLibraryName() {
		return fLibraryName;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.ui.IFieldData2#getSourceFolderName()
	 */
	public String getSourceFolderName() {
		return fSourceFolderName;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.ui.IFieldData2#getOutputFolderName()
	 */
	public String getOutputFolderName() {
		return fOutputFolderName;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.ui.IFieldData2#hasBundleStructure()
	 */
	public boolean hasBundleStructure() {
		return fHasBundleStructure;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.ui.IFieldData2#isSimple()
	 */
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

	/* (non-Javadoc)
	 * @see org.eclipse.pde.ui.IFieldData#getTargetVersion()
	 */
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
