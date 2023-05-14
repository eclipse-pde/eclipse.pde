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

import java.util.List;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.ITypeRoot;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.Annotation;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.IAnnotationBinding;
import org.eclipse.jdt.core.dom.PackageDeclaration;
import org.eclipse.pde.core.IBaseModel;
import org.eclipse.pde.core.IModel;
import org.eclipse.pde.internal.core.WorkspaceModelManager;
import org.eclipse.pde.internal.core.annotations.CustomHeaderAnnotationProcessor;
import org.eclipse.pde.internal.core.annotations.ExportPackageAnnotationProcessor;
import org.eclipse.pde.internal.core.annotations.OSGiAnnotationProcessor;
import org.eclipse.pde.internal.core.natures.PDE;
import org.eclipse.pde.internal.ui.util.ModelModification;
import org.eclipse.pde.internal.ui.util.PDEModelUtility;

public class OSGiAnnotationsASTVisitor extends ASTVisitor {

	private CompilationUnit unit;

	public OSGiAnnotationsASTVisitor(CompilationUnit compilationUnit) {
		this.unit = compilationUnit;
	}

	@Override
	public boolean visit(PackageDeclaration packageDeclaration) {
		ITypeRoot typeRoot = unit.getTypeRoot();
		if (typeRoot == null || !"package-info.java".equals(typeRoot.getElementName())) { //$NON-NLS-1$
			return true;
		}
		IJavaProject javaProject = typeRoot.getJavaProject();
		if (javaProject == null) {
			return true;
		}
		IProject project = javaProject.getProject();
		if (!PDE.hasPluginNature(project) || WorkspaceModelManager.isBinaryProject(project)) {
			return true;
		}
		List<OSGiAnnotationProcessor> processors = getPackageProcessors(packageDeclaration.getName().toString());
		for (Object item : packageDeclaration.annotations()) {
			if (item instanceof Annotation) {
				Annotation annotation = (Annotation) item;
				IAnnotationBinding annotationBinding = annotation.resolveAnnotationBinding();
				if (annotationBinding == null) {
					// not resolvable
					continue;
				}
				String name = annotationBinding.getAnnotationType().getQualifiedName();
				for (OSGiAnnotationProcessor processor : processors) {
					processor.processAnnotation(annotation, name);
				}
			}
		}
		PDEModelUtility.modifyModel(new ModelModification(project) {
			@Override
			protected void modifyModel(IBaseModel model, IProgressMonitor monitor) throws CoreException {
				if (model instanceof IModel) {
					if (isDerived(((IModel) model).getUnderlyingResource())) {
						// do not modify a derived manifest...
						return;
					}
				}
				for (OSGiAnnotationProcessor processor : processors) {
					processor.apply(model);
				}
			}
		}, null);
		return true;
	}

	private static boolean isDerived(IResource resource) {
		if (resource != null) {
			if (resource.isDerived()) {
				return true;
			}
			return isDerived(resource.getParent());
		}
		return false;
	}

	private static List<OSGiAnnotationProcessor> getPackageProcessors(String packageName) {
		return List.of(new ExportPackageAnnotationProcessor(packageName), new CustomHeaderAnnotationProcessor());
	}

}
