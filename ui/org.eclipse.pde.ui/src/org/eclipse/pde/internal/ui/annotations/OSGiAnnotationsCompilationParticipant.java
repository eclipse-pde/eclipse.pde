/*******************************************************************************
 * Copyright (c) 2022 Christoph Läubrich and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Christoph Läubrich - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.annotations;

import org.eclipse.core.resources.IProject;
import org.eclipse.jdt.core.*;
import org.eclipse.jdt.core.compiler.BuildContext;
import org.eclipse.jdt.core.compiler.CompilationParticipant;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.pde.internal.core.WorkspaceModelManager;
import org.eclipse.pde.internal.core.annotations.OSGiAnnotations;
import org.eclipse.pde.internal.core.natures.PDE;

public class OSGiAnnotationsCompilationParticipant extends CompilationParticipant {

	@Override
	public boolean isAnnotationProcessor() {
		return true;
	}

	@Override
	public boolean isActive(IJavaProject javaProject) {
		IProject project = javaProject.getProject();
		if (project.isOpen() && PDE.hasPluginNature(project) && !WorkspaceModelManager.isBinaryProject(project)) {
			for (String annotation : OSGiAnnotations.SUPPORTED_ANNOTATIONS) {
				try {
					IType annotationType = javaProject.findType(annotation);
					if (annotationType != null && annotationType.isAnnotation()) {
						return true;
					}
				} catch (JavaModelException e) {
				}
			}
		}
		return false;
	}

	@Override
	public void processAnnotations(BuildContext[] files) {
		for (BuildContext file : files) {
			ICompilationUnit cu = JavaCore.createCompilationUnitFrom(file.getFile());
			if (cu == null) {
				// can't process...
				continue;
			}
			@SuppressWarnings("deprecation")
			ASTParser parser = ASTParser.newParser(AST.JLS3);
			parser.setResolveBindings(true);
			parser.setBindingsRecovery(true);
			parser.setProject(cu.getJavaProject());
			parser.setKind(ASTParser.K_COMPILATION_UNIT);
			parser.createASTs(new ICompilationUnit[] { cu }, new String[0],
					new OSGiAnnotationsASTRequestor(), null);
		}
	}
}
