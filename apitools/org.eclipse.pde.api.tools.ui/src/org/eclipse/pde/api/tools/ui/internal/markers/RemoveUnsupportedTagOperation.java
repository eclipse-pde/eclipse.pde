/*******************************************************************************
 * Copyright (c) 2008, 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.api.tools.ui.internal.markers;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
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
import org.eclipse.jdt.core.dom.BodyDeclaration;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.Javadoc;
import org.eclipse.jdt.core.dom.TagElement;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.jdt.core.dom.rewrite.ListRewrite;
import org.eclipse.jdt.ui.JavaUI;
import org.eclipse.pde.api.tools.internal.provisional.IApiMarkerConstants;
import org.eclipse.pde.api.tools.internal.util.Util;
import org.eclipse.text.edits.TextEdit;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.progress.UIJob;

/**
 * The operation for removing an unsupported API Javadoc tag from a member
 * 
 * @since 1.0.0
 */
public class RemoveUnsupportedTagOperation extends UIJob {

	/**
	 * The backing marker for the quick-fix
	 */
	private IMarker[] markers = null;
	
	/**
	 * Constructor
	 * @param marker
	 */
	public RemoveUnsupportedTagOperation(IMarker[] markers) {
		super(MarkerMessages.UnsupportedTagResolution_remove_unsupported_tags);
		this.markers = markers;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ui.progress.UIJob#runInUIThread(org.eclipse.core.runtime.IProgressMonitor)
	 */
	public IStatus runInUIThread(IProgressMonitor monitor) {
		SubMonitor localMonitor = SubMonitor.convert(monitor, MarkerMessages.RemoveUnsupportedTagOperation_removeing_unsupported_tag, this.markers.length + 6);
		HashMap seen = new HashMap();
		for (int i = 0; i < this.markers.length; i++) {
			// retrieve the AST node compilation unit
			IResource resource = this.markers[i].getResource();
			IJavaElement javaElement = JavaCore.create(resource);
			try {
				if (javaElement != null && javaElement.getElementType() == IJavaElement.COMPILATION_UNIT) {
					ICompilationUnit compilationUnit = (ICompilationUnit) javaElement;
					if(!seen.containsKey(compilationUnit)) {
						seen.put(compilationUnit, Boolean.valueOf(compilationUnit.hasUnsavedChanges()));
					}
					if (!compilationUnit.isWorkingCopy()) {
						// open an editor of the corresponding unit to "show" the quick-fix change
						JavaUI.openInEditor(compilationUnit);
					}
					if(!compilationUnit.isConsistent()) {
						compilationUnit.reconcile(ICompilationUnit.NO_AST, false, null, null);
						Util.updateMonitor(localMonitor, 1);
					}
					Util.updateMonitor(localMonitor, 1);
					ASTParser parser = ASTParser.newParser(AST.JLS4);
					parser.setSource(compilationUnit);
					Integer charStartAttribute = null;
					charStartAttribute = (Integer) this.markers[i].getAttribute(IMarker.CHAR_START);
					int intValue = charStartAttribute.intValue();
					parser.setFocalPosition(intValue);
					Map options = compilationUnit.getJavaProject().getOptions(true);
					options.put(JavaCore.COMPILER_DOC_COMMENT_SUPPORT, JavaCore.ENABLED);
					parser.setCompilerOptions(options);
					final CompilationUnit unit = (CompilationUnit) parser.createAST(new NullProgressMonitor());
					NodeFinder finder = new NodeFinder(intValue);
					unit.accept(finder);
					Util.updateMonitor(localMonitor, 1);
					BodyDeclaration node = finder.getNode();
					if (node != null) {
						unit.recordModifications();
						AST ast = unit.getAST();
						ASTRewrite rewrite = ASTRewrite.create(ast);
						Javadoc docnode = node.getJavadoc();
						if (docnode == null) {
							return Status.CANCEL_STATUS;
						} else {
							List tags = docnode.tags();
							String arg = (String) this.markers[i].getAttribute(IApiMarkerConstants.MARKER_ATTR_MESSAGE_ARGUMENTS);
							String[] args = arg.split("#"); //$NON-NLS-1$
							TagElement tag = null;
							for (Iterator iterator = tags.iterator(); iterator.hasNext();) {
								tag = (TagElement) iterator.next();
								if (args[0].equals(tag.getTagName()) && tag.getStartPosition() == intValue) {
									break;
								}
							}
							if(tag == null) {
								return Status.CANCEL_STATUS;
							}
							ListRewrite lrewrite = rewrite.getListRewrite(docnode, Javadoc.TAGS_PROPERTY);
							lrewrite.remove(tag, null);
							Util.updateMonitor(localMonitor, 1);
						}
						TextEdit edit = rewrite.rewriteAST();
						compilationUnit.applyTextEdit(edit, monitor);
						Util.updateMonitor(localMonitor, 1);
					}
				}
			}
			catch(JavaModelException jme) {} 
			catch (PartInitException e) {}
			catch (CoreException e) {}
		}
		//try saving the compilation units if they were in a saved state when the quick-fix started
		Map.Entry entry = null;
		for (Iterator itr = seen.entrySet().iterator(); itr.hasNext();) {
			entry = (Entry) itr.next();
			if(!((Boolean)entry.getValue()).booleanValue()) {
				try {
					((ICompilationUnit)entry.getKey()).commitWorkingCopy(true, null);
				} catch (JavaModelException jme) {}
			}
		}
		return Status.OK_STATUS;
	}
}
