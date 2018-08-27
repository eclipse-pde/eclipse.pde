/*******************************************************************************
 * Copyright (c) 2012, 2017 Ecliptical Software Inc. and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Ecliptical Software Inc. - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.ds.internal.annotations;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import org.eclipse.core.resources.ICommand;
import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.compiler.BuildContext;
import org.eclipse.jdt.core.compiler.CategorizedProblem;
import org.eclipse.jdt.core.dom.ASTRequestor;
import org.eclipse.jdt.core.dom.CompilationUnit;

public class AnnotationProcessor extends ASTRequestor {

	private static final String DS_BUILDER = "org.eclipse.pde.ds.core.builder"; //$NON-NLS-1$

	static final Debug debug = Debug.getDebug("ds-annotation-builder/processor"); //$NON-NLS-1$

	private final ProjectContext context;

	private final Map<ICompilationUnit, BuildContext> fileMap;

	private boolean hasBuilder;

	public AnnotationProcessor(ProjectContext context, Map<ICompilationUnit, BuildContext> fileMap) {
		this.context = context;
		this.fileMap = fileMap;
	}

	static String getCompilationUnitKey(ICompilationUnit source) {
		IJavaElement parent = source.getParent();
		if (parent == null) {
			return source.getElementName();
		}

		return String.format("%s/%s", parent.getElementName().replace('.', '/'), source.getElementName()); //$NON-NLS-1$
	}

	@Override
	public void acceptAST(ICompilationUnit source, CompilationUnit ast) {
		// determine CU key
		String cuKey = getCompilationUnitKey(source);

		context.getUnprocessed().remove(cuKey);

		ProjectState state = context.getState();
		HashMap<String, String> dsKeys = new HashMap<>();
		HashSet<DSAnnotationProblem> problems = new HashSet<>();

		ast.accept(new AnnotationVisitor(this, state, dsKeys, problems));

		// track abandoned files (may be garbage)
		Collection<String> oldDSKeys = state.updateMappings(cuKey, dsKeys);
		if (oldDSKeys != null) {
			oldDSKeys.removeAll(dsKeys.values());
			context.getAbandoned().addAll(oldDSKeys);
		}

		if (!problems.isEmpty()) {
			char[] filename = source.getResource().getFullPath().toString().toCharArray();
			for (DSAnnotationProblem problem : problems) {
				problem.setOriginatingFileName(filename);
				if (problem.getSourceStart() >= 0) {
					problem.setSourceLineNumber(ast.getLineNumber(problem.getSourceStart()));
				}
			}

			BuildContext buildContext = fileMap.get(source);
			if (buildContext != null) {
				buildContext.recordNewProblems(problems.toArray(new CategorizedProblem[problems.size()]));
			}
		}
	}

	private void ensureDSProject(IProject project) throws CoreException {
		IProjectDescription description = project.getDescription();
		ICommand[] commands = description.getBuildSpec();

		for (ICommand command : commands) {
			if (DS_BUILDER.equals(command.getBuilderName())) {
				return;
			}
		}

		ICommand[] newCommands = new ICommand[commands.length + 1];
		System.arraycopy(commands, 0, newCommands, 0, commands.length);
		ICommand command = description.newCommand();
		command.setBuilderName(DS_BUILDER);
		newCommands[newCommands.length - 1] = command;
		description.setBuildSpec(newCommands);
		project.setDescription(description, null);
	}

	private void ensureExists(IFolder folder) throws CoreException {
		if (folder.exists()) {
			return;
		}

		IContainer parent = folder.getParent();
		if (parent != null && parent.getType() == IResource.FOLDER) {
			ensureExists((IFolder) parent);
		}

		folder.create(true, true, null);
	}

	void verifyOutputLocation(IFile file) throws CoreException {
		if (hasBuilder) {
			return;
		}

		hasBuilder = true;
		IProject project = file.getProject();

		IPath parentPath = file.getParent().getProjectRelativePath();
		if (!parentPath.isEmpty()) {
			IFolder folder = project.getFolder(parentPath);
			ensureExists(folder);
		}

		try {
			ensureDSProject(project);
		} catch (CoreException e) {
			Activator.log(e);
		}
	}
}