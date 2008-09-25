/*******************************************************************************
 * Copyright (c) 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.api.tools.ui.internal.markers;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.BodyDeclaration;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.Javadoc;
import org.eclipse.jdt.core.dom.TagElement;
import org.eclipse.jdt.core.dom.TextElement;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.jdt.core.dom.rewrite.ListRewrite;
import org.eclipse.jdt.ui.JavaUI;
import org.eclipse.pde.api.tools.internal.ApiJavadocTag;
import org.eclipse.pde.api.tools.internal.JavadocTagManager;
import org.eclipse.pde.api.tools.internal.provisional.ApiPlugin;
import org.eclipse.pde.api.tools.internal.provisional.IApiJavadocTag;
import org.eclipse.text.edits.TextEdit;
import org.eclipse.ui.PartInitException;

/**
 * Operation to add a no reference tag to a member (method or field)
 * 
 * @since 1.0.0
 */
public class AddNoReferenceTagOperation extends JavadocTagOperation {

	/**
	 * Constructor
	 * @param marker
	 */
	public AddNoReferenceTagOperation(IMarker marker) {
		super(marker);
	}
	
	/**
	 * Runs the operation
	 * @param monitor
	 */
	public void run(IProgressMonitor monitor) {
		if(monitor != null && monitor.isCanceled()) {
			return;
		}
		IProgressMonitor localMonitor = SubMonitor.convert(monitor, MarkerMessages.RemoveUnsupportedTagOperation_removeing_unsupported_tag, 5);
		// retrieve the AST node compilation unit
		IResource resource = getMarker().getResource();
		IJavaElement javaElement = JavaCore.create(resource);
		try {
			if (javaElement != null && javaElement.getElementType() == IJavaElement.COMPILATION_UNIT) {
				ICompilationUnit cunit = (ICompilationUnit) javaElement;
				if (!cunit.isWorkingCopy()) {
					// open an editor of the corresponding unit to "show" the quick-fix change
					JavaUI.openInEditor(cunit);
				}
				updateMonitor(localMonitor, 1);
				int focalpos = getFocalPosition();
				final CompilationUnit unit = createAST(cunit);
				NodeFinder finder = new NodeFinder(focalpos);
				unit.accept(finder);
				updateMonitor(localMonitor, 1);
				BodyDeclaration node = finder.getNode();
				if (node != null) {
					JavadocTagManager jtm = ApiPlugin.getJavadocTagManager();
					ApiJavadocTag tag = (ApiJavadocTag) jtm.getTag(IApiJavadocTag.NO_REFERENCE_TAG_ID);
					if(tag == null) {
						return;
					}
					unit.recordModifications();
					AST ast = unit.getAST();
					ASTRewrite rewrite = ASTRewrite.create(ast);
					Javadoc docnode = node.getJavadoc();
					if (docnode == null) {
						docnode = ast.newJavadoc();
						//we do not want to create a new empty Javadoc node in
						//the AST if there are no missing tags
						rewrite.set(node, node.getJavadocProperty(), docnode, null);
					}  
					ListRewrite lrewrite = rewrite.getListRewrite(docnode, Javadoc.TAGS_PROPERTY);
					// check the existing tags list
					TagElement newtag = ast.newTagElement();
					newtag.setTagName(tag.getTagName());
					TextElement textElement = ast.newTextElement();
					textElement.setText(tag.getTagComment(getParentKind(node), isConstructor(node) ? IApiJavadocTag.MEMBER_CONSTRUCTOR : IApiJavadocTag.MEMBER_METHOD));
					newtag.fragments().add(textElement);
					lrewrite.insertLast(newtag, null);
					try {
						TextEdit edit = rewrite.rewriteAST();
						cunit.applyTextEdit(edit, monitor);
						updateMonitor(localMonitor, 1);
					}
					finally {
						cunit.reconcile(ICompilationUnit.NO_AST, false, null, null);
						localMonitor.done();
					}
				}
			}
		}
		catch(JavaModelException jme) {} 
		catch (PartInitException e) {}
	}
	
}
