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

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Status;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.ltk.core.refactoring.Change;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.osgi.util.NLS;
import org.eclipse.pde.internal.ui.PDEUIMessages;

public class CreateJarChange extends Change {

	private IFile jarInstructionFile;
	private String jarName;
	private IProject project;
	private String[] sourceTokens;
	private String outputFolder;

	public CreateJarChange(IProject project, IFile instructionsFile, String jarName, String[] sourceTokens,
			String outputFolder) {
		this.project = project;
		this.jarName = jarName;
		this.sourceTokens = sourceTokens;
		this.outputFolder = outputFolder;
		this.jarInstructionFile = project.getFolder(".jars") //$NON-NLS-1$
				.getFile(IPath.fromPortableString(jarName.replace(".jar", ".bnd"))); //$NON-NLS-1$//$NON-NLS-2$
	}

	@Override
	public String getName() {
		return NLS.bind(PDEUIMessages.CreateJarChange_instruction_jar, jarName);
	}

	@Override
	public Change perform(IProgressMonitor pm) throws CoreException {
		StringBuilder sb = new StringBuilder();
		sb.append("-includeresource: "); //$NON-NLS-1$
		sb.append(getOutputFolde());
		sb.append("\r\n"); //$NON-NLS-1$
		sb.append("-nomanifest: true\r\n"); //$NON-NLS-1$
		sb.append("-sources: false\r\n"); //$NON-NLS-1$
		mkdirs(jarInstructionFile.getParent(), pm);
		if (!jarInstructionFile.exists()) {
			jarInstructionFile.create(new ByteArrayInputStream(sb.toString().getBytes(StandardCharsets.UTF_8)), true,
					pm);
		}
		return null;
	}

	private void mkdirs(IContainer container, IProgressMonitor pm) throws CoreException {
		if (container instanceof IFolder folder) {
			mkdirs(folder.getParent(), pm);
			if (!folder.exists()) {
				folder.create(true, true, pm);
			}
		}
	}

	private String getOutputFolde() throws CoreException {
		if (outputFolder == null) {
			IJavaProject javaProject = JavaCore.create(project);
			IPath outputLocation = javaProject.getOutputLocation();
			IClasspathEntry[] classpath = javaProject.getRawClasspath();
			for (IClasspathEntry entry : classpath) {
				if (entry.getEntryKind() == IClasspathEntry.CPE_SOURCE) {
					for (String token : sourceTokens) {
						if (IPath.fromPortableString(token).equals(entry.getPath())) {
							IPath srcLoc = entry.getOutputLocation();
							if (srcLoc != null) {
								outputLocation = srcLoc;
							}
							break;
						}
					}
				}
			}
			return project.getWorkspace().getRoot().getFolder(outputLocation).getProjectRelativePath()
					.toPortableString();
		}
		return outputFolder;
	}

	@Override
	public void initializeValidationData(IProgressMonitor pm) {
	}

	@Override
	public RefactoringStatus isValid(IProgressMonitor pm) throws CoreException, OperationCanceledException {
		return RefactoringStatus.create(Status.OK_STATUS);
	}

	@Override
	public Object getModifiedElement() {
		return jarInstructionFile;
	}
}
