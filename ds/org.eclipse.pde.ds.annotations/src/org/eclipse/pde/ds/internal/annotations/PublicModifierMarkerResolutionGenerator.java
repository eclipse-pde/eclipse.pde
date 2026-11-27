/*******************************************************************************
 * Copyright (c) 2026 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.ds.internal.annotations;

import java.util.Map;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.Annotation;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.Modifier;
import org.eclipse.jdt.core.dom.NodeFinder;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.jdt.core.dom.rewrite.ListRewrite;
import org.eclipse.jdt.ui.ISharedImages;
import org.eclipse.jdt.ui.JavaUI;
import org.eclipse.jface.text.Document;
import org.eclipse.swt.graphics.Image;
import org.eclipse.text.edits.MalformedTreeException;
import org.eclipse.text.edits.TextEdit;
import org.eclipse.ui.IMarkerResolution;
import org.eclipse.ui.IMarkerResolution2;
import org.eclipse.ui.IMarkerResolutionGenerator2;

/**
 * Provides a quick fix to add public modifier when a constructor annotated with
 * <code>@Activate</code> is not public and DS>=1.4
 */
public class PublicModifierMarkerResolutionGenerator implements IMarkerResolutionGenerator2 {

	@Override
	public boolean hasResolutions(IMarker marker) {
		return DSAnnotationProblem.CONSTRUCTOR_NOT_PUBLIC
				.equals(marker.getAttribute(DSAnnotationProblem.ATTRIBUTE_TYPE, null));
	}

	@Override
	public IMarkerResolution[] getResolutions(IMarker marker) {
		if (!hasResolutions(marker)) {
			return new IMarkerResolution[0];
		}

		return new IMarkerResolution[] { new MakeConstructorPublicResolution() };
	}

	/**
	 * Adds public modifier for the for injectable constructors where DS>=1.4
	 * @param source  the source of the compilation unit
	 * @param offset  offset inside the constructor's <code>@Activate</code>
	 *                annotation
	 * @param options the Java compiler options
	 * @return the edit, or <code>null</code> if no constructor was found at the
	 *         offset or it is already public
	 */
	static TextEdit addPublicModifier(String source, int offset, Map<String, String> options) {
		ASTParser parser = ASTParser.newParser(AST.getJLSLatest());
		parser.setSource(source.toCharArray());
		parser.setKind(ASTParser.K_COMPILATION_UNIT);
		parser.setCompilerOptions(options);
		CompilationUnit ast = (CompilationUnit) parser.createAST(null);

		ASTNode node = NodeFinder.perform(ast, offset, 0);
		while (node != null && !(node instanceof MethodDeclaration)) {
			node = node.getParent();
		}

		if (!(node instanceof MethodDeclaration method) || !method.isConstructor()
				|| Modifier.isPublic(method.getModifiers())) {
			return null;
		}

		AST dom = method.getAST();
		ASTRewrite rewrite = ASTRewrite.create(dom);
		ListRewrite modifiers = rewrite.getListRewrite(method, MethodDeclaration.MODIFIERS2_PROPERTY);
		Modifier publicModifier = dom.newModifier(Modifier.ModifierKeyword.PUBLIC_KEYWORD);
		Modifier accessModifier = null;
		Annotation lastAnnotation = null;
		for (Object item : method.modifiers()) {
			if (item instanceof Modifier modifier && (modifier.isPrivate() || modifier.isProtected())) {
				accessModifier = modifier;
			} else if (item instanceof Annotation annotation) {
				lastAnnotation = annotation;
			}
		}

		if (accessModifier != null) {
			modifiers.replace(accessModifier, publicModifier, null);
		} else if (lastAnnotation != null) {
			modifiers.insertAfter(publicModifier, lastAnnotation, null);
		} else {
			modifiers.insertFirst(publicModifier, null);
		}
		return rewrite.rewriteAST(new Document(source), options);
	}

	private static class MakeConstructorPublicResolution implements IMarkerResolution2 {

		@Override
		public String getLabel() {
			return Messages.DSAnnotationPublicModifierMarkerResolutionGenerator_label;
		}

		@Override
		public String getDescription() {
			return Messages.DSAnnotationPublicModifierMarkerResolutionGenerator_description;
		}

		@Override
		public Image getImage() {
			return JavaUI.getSharedImages().getImage(ISharedImages.IMG_FIELD_PUBLIC);
		}

		@Override
		public void run(IMarker marker) {
			IResource resource = marker.getResource();
			if (!(resource instanceof IFile file)) {
				return;
			}

			int offset = marker.getAttribute(IMarker.CHAR_START, -1);
			if (offset < 0) {
				return;
			}

			ICompilationUnit unit = JavaCore.createCompilationUnitFrom(file);
			if (unit == null) {
				return;
			}

			try {
				unit.becomeWorkingCopy(null);
				try {
					TextEdit edit = addPublicModifier(unit.getSource(), offset,
							unit.getJavaProject().getOptions(true));
					if (edit != null) {
						unit.applyTextEdit(edit, null);
						unit.commitWorkingCopy(true, null);
					}
				} finally {
					unit.discardWorkingCopy();
				}
			} catch (CoreException | MalformedTreeException | IllegalArgumentException e) {
				Activator.log(e);
			}
		}
	}
}
