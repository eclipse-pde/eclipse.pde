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

import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.BodyDeclaration;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.Javadoc;
import org.eclipse.jdt.core.dom.TagElement;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.jdt.core.dom.rewrite.ListRewrite;
import org.eclipse.jdt.ui.JavaUI;
import org.eclipse.pde.api.tools.internal.provisional.IApiMarkerConstants;
import org.eclipse.text.edits.TextEdit;
import org.eclipse.ui.PartInitException;

/**
 * The operation for removing an unsupported API Javadoc tag form a member
 * 
 * @since 1.0.0
 */
public class RemoveUnsupportedTagOperation {

	/**
	 * The backing marker for the quick-fix
	 */
	private IMarker fBackingMarker = null;
	
	/**
	 * Constructor
	 * @param marker
	 */
	public RemoveUnsupportedTagOperation(IMarker marker) {
		fBackingMarker = marker;
	}

	/**
	 * Runs the operation reporting progress to the supplied monitor
	 * @param monitor
	 */
	public void run(IProgressMonitor monitor) {
		if(monitor != null && monitor.isCanceled()) {
			return;
		}
		IProgressMonitor localMonitor = SubMonitor.convert(monitor, MarkerMessages.RemoveUnsupportedTagOperation_removeing_unsupported_tag, 5);
		// retrieve the AST node compilation unit
		IResource resource = fBackingMarker.getResource();
		IJavaElement javaElement = JavaCore.create(resource);
		try {
			if (javaElement != null && javaElement.getElementType() == IJavaElement.COMPILATION_UNIT) {
				ICompilationUnit compilationUnit = (ICompilationUnit) javaElement;
				if (!compilationUnit.isWorkingCopy()) {
					// open an editor of the corresponding unit to "show" the quick-fix change
					JavaUI.openInEditor(compilationUnit);
				}
				if(localMonitor.isCanceled()) {
					return;
				}
				localMonitor.worked(1);
				ASTParser parser = ASTParser.newParser(AST.JLS3);
				parser.setSource(compilationUnit);
				Integer charStartAttribute = null;
				charStartAttribute = (Integer) fBackingMarker.getAttribute(IMarker.CHAR_START);
				int intValue = charStartAttribute.intValue();
				parser.setFocalPosition(intValue);
				parser.setResolveBindings(true);
				Map options = compilationUnit.getJavaProject().getOptions(true);
				options.put(JavaCore.COMPILER_DOC_COMMENT_SUPPORT, JavaCore.ENABLED);
				parser.setCompilerOptions(options);
				final CompilationUnit unit = (CompilationUnit) parser.createAST(new NullProgressMonitor());
				NodeFinder finder = new NodeFinder(intValue);
				unit.accept(finder);
				if(localMonitor.isCanceled()) {
					return;
				}
				localMonitor.worked(1);
				BodyDeclaration node = finder.getNode();
				if (node != null) {
					unit.recordModifications();
					AST ast = unit.getAST();
					ASTRewrite rewrite = ASTRewrite.create(ast);
					Javadoc docnode = node.getJavadoc();
					if (docnode == null) {
						return;
					} else {
						List tags = docnode.tags();
						String arg = (String) fBackingMarker.getAttribute(IApiMarkerConstants.MARKER_ATTR_MESSAGE_ARGUMENTS);
						String[] args = arg.split("#"); //$NON-NLS-1$
						TagElement tag = null;
						for (Iterator iterator = tags.iterator(); iterator.hasNext();) {
							tag = (TagElement) iterator.next();
							if (args[0].equals(tag.getTagName())) {
								break;
							}
						}
						if(tag == null) {
							return;
						}
						ListRewrite lrewrite = rewrite.getListRewrite(docnode, Javadoc.TAGS_PROPERTY);
						lrewrite.remove(tag, null);
						if(localMonitor.isCanceled()) {
							return;
						}
						localMonitor.worked(1);
					}
					try {
						TextEdit edit = rewrite.rewriteAST();
						compilationUnit.applyTextEdit(edit, monitor);
						if(localMonitor.isCanceled()) {
							return;
						}
						localMonitor.worked(1);
					}
					finally {
						compilationUnit.reconcile(ICompilationUnit.NO_AST, false, null, null);
						if(localMonitor.isCanceled()) {
							return;
						}
						localMonitor.worked(1);
					}
				}
			}
					
		}
		catch(JavaModelException jme) {} 
		catch (PartInitException e) {}
		catch (CoreException e) {}
	}
	
}
