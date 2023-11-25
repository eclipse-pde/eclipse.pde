/*******************************************************************************
 *  Copyright (c) 2023 Christoph Läubrich and others.
 *
 *  This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License 2.0
 *  which accompanies this distribution, and is available at
 *  https://www.eclipse.org/legal/epl-2.0/
 *
 *  SPDX-License-Identifier: EPL-2.0
 *
 *  Contributors:
 *     Christoph Läubrich - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.wizards.tools.change;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.internal.corext.util.InfoFilesUtil;
import org.eclipse.ltk.core.refactoring.Change;
import org.eclipse.ltk.core.refactoring.resource.DeleteResourceChange;
import org.eclipse.ltk.core.refactoring.resource.ResourceChange;
import org.eclipse.osgi.util.NLS;
import org.eclipse.pde.internal.ui.PDEUIMessages;
import org.osgi.framework.Version;

@SuppressWarnings("restriction")
public class CreatePackageInfoChange extends ResourceChange {

	public static final String PACKAGE_INFO_JAVA = org.eclipse.jdt.internal.corext.util.JavaModelUtil.PACKAGE_INFO_JAVA;

	private IPackageFragment fragment;

	private String name;

	private Version version;

	public CreatePackageInfoChange(IPackageFragment fragment, String name, Version version) {
		this.fragment = fragment;
		this.name = name;
		this.version = version;
	}

	@Override
	protected IResource getModifiedResource() {
		try {
			return fragment.getCorrespondingResource();
		} catch (JavaModelException e) {
			return null;
		}
	}

	@Override
	public String getName() {
		return NLS.bind(PDEUIMessages.CreatePackageInfoChange_name, PACKAGE_INFO_JAVA, fragment.getElementName());
	}

	@Override
	public Change perform(IProgressMonitor pm) throws CoreException {
		StringBuilder fileContent = new StringBuilder(
				"@org.osgi.annotation.bundle.Export(substitution = org.osgi.annotation.bundle.Export.Substitution.NOIMPORT)"); //$NON-NLS-1$
		if (version != null && !version.equals(Version.emptyVersion)) {
			fileContent.append("@org.osgi.annotation.versioning.Version(\""); //$NON-NLS-1$
			fileContent.append(version.getMajor());
			fileContent.append('.');
			fileContent.append(version.getMinor());
			fileContent.append('.');
			fileContent.append(version.getMicro());
			fileContent.append("\")"); //$NON-NLS-1$
		}
		fileContent.append("package "); //$NON-NLS-1$
		fileContent.append(name);
		fileContent.append(";"); //$NON-NLS-1$
		InfoFilesUtil.createInfoJavaFile(PACKAGE_INFO_JAVA, fileContent.toString(), fragment, false, pm);
		return new DeleteResourceChange(fragment.getPath(), true);
	}

}
