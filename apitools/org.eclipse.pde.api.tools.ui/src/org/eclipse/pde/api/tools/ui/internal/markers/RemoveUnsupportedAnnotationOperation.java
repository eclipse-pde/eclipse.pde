/*******************************************************************************
 * Copyright (c) Sep 12, 2018 IBM Corporation and others.
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
package org.eclipse.pde.api.tools.ui.internal.markers;

import java.util.HashMap;
import java.util.Map.Entry;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.AnnotationTypeDeclaration;
import org.eclipse.jdt.core.dom.Block;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.EnumDeclaration;
import org.eclipse.jdt.core.dom.FieldDeclaration;
import org.eclipse.jdt.core.dom.MarkerAnnotation;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.jdt.ui.JavaUI;
import org.eclipse.text.edits.TextEdit;
import org.eclipse.text.edits.TextEditGroup;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.progress.UIJob;

/**
 * {@link UIJob} to remove unsupported annotations from a marker resolution
 *
 * @since 1.0.500
 */
public class RemoveUnsupportedAnnotationOperation extends UIJob {

	/**
	 * Find the annotation we want to remove
	 */
	class AnnotationFinder extends ASTVisitor {

		int fPosition = -1;
		MarkerAnnotation fNode = null;
		boolean found = false;

		AnnotationFinder(int position) {
			fPosition = position;
		}

		@Override
		public boolean visit(AnnotationTypeDeclaration node) {
			return !found;
		}

		@Override
		public boolean visit(EnumDeclaration node) {
			return !found;
		}

		@Override
		public boolean visit(FieldDeclaration node) {
			return !found;
		}

		@Override
		public boolean visit(MethodDeclaration node) {
			return !found;
		}

		@Override
		public boolean visit(TypeDeclaration node) {
			return !found;
		}

		@Override
		public boolean visit(Block node) {
			return !found;
		}

		@Override
		public boolean visit(MarkerAnnotation node) {
			int start = node.getStartPosition();
			int end = node.getLength() - 1 + start;
			if (start <= fPosition && fPosition <= end) {
				fNode = node;
				found = true;
			}
			return false;
		}
	}

	private IMarker[] fMarkers = null;

	/**
	 * @param name
	 */
	public RemoveUnsupportedAnnotationOperation(IMarker[] markers) {
		super(MarkerMessages.RemoveUnsupportedAnnotationOperation_remove_unsupported_annotations);
		fMarkers = markers;
	}

	@Override
	public IStatus runInUIThread(IProgressMonitor monitor) {
		SubMonitor localMonitor = SubMonitor.convert(monitor, MarkerMessages.RemoveUnsupportedTagOperation_removeing_unsupported_tag, fMarkers.length + 6);
		HashMap<ICompilationUnit, Boolean> seen = new HashMap<>();
		for (IMarker fMarker : fMarkers) {
			// retrieve the AST node compilation unit
			IResource resource = fMarker.getResource();
			IJavaElement javaElement = JavaCore.create(resource);
			try {
				if (javaElement != null && javaElement.getElementType() == IJavaElement.COMPILATION_UNIT) {
					ICompilationUnit compilationUnit = (ICompilationUnit) javaElement;
					if (!seen.containsKey(compilationUnit)) {
						seen.put(compilationUnit, Boolean.valueOf(compilationUnit.hasUnsavedChanges()));
					}
					if (!compilationUnit.isWorkingCopy()) {
						// open an editor of the corresponding unit to "show"
						// the quick-fix change
						JavaUI.openInEditor(compilationUnit);
					}
					if (!compilationUnit.isConsistent()) {
						compilationUnit.reconcile(ICompilationUnit.NO_AST, false, null, null);
						localMonitor.split(1);
					}
					localMonitor.split(1);
					ASTParser parser = ASTParser.newParser(AST.JLS10);
					parser.setSource(compilationUnit);
					Integer charStartAttribute = null;
					charStartAttribute = (Integer) fMarker.getAttribute(IMarker.CHAR_START);
					int intValue = charStartAttribute.intValue();
					parser.setFocalPosition(intValue);
					final CompilationUnit unit = (CompilationUnit) parser.createAST(new NullProgressMonitor());
					AnnotationFinder finder = new AnnotationFinder(intValue);
					unit.accept(finder);
					localMonitor.split(1);
					if (finder.fNode != null) {
						unit.recordModifications();
						AST ast = unit.getAST();
						ASTRewrite rewrite = ASTRewrite.create(ast);
						TextEditGroup group = new TextEditGroup("Removing API tools annotations"); //$NON-NLS-1$
						rewrite.remove(finder.fNode, group);
						localMonitor.split(1);
						TextEdit edit = rewrite.rewriteAST();
						compilationUnit.applyTextEdit(edit, monitor);
						localMonitor.split(1);
					}
				}
			} catch (JavaModelException jme) {
			} catch (PartInitException e) {
			} catch (CoreException e) {
			}
		}
		// try saving the compilation units if they were in a saved state when
		// the quick-fix started
		for (Entry<ICompilationUnit, Boolean> entry : seen.entrySet()) {
			if (!entry.getValue().booleanValue()) {
				try {
					entry.getKey().commitWorkingCopy(true, null);
				} catch (JavaModelException jme) {
				}
			}
		}
		return Status.OK_STATUS;
	}
}
