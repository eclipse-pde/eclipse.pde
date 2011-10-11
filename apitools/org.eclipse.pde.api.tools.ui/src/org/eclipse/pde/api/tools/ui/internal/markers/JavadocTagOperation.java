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

import java.util.Map;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.BodyDeclaration;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.eclipse.pde.api.tools.internal.provisional.IApiJavadocTag;
import org.eclipse.pde.api.tools.internal.provisional.IApiMarkerConstants;

/**
 * Parent class for operations that want to modify Javadoc tags in some way.
 * 
 * @since 1.0.0
 */
public class JavadocTagOperation {

	private IMarker fBackingMarker = null;
	
	/**
	 * Constructor
	 * @param marker
	 */
	public JavadocTagOperation(IMarker marker) {
		fBackingMarker = marker;
	}
	
	/**
	 * Sets the backing marker for this operation
	 * @param marker
	 */
	protected void setMarker(IMarker marker) {
		fBackingMarker = marker;
	}
	
	/**
	 * Gets the current {@link IMarker} set for this operation
	 * @return
	 */
	protected IMarker getMarker() {
		return fBackingMarker;
	}
	
	/**
	 * Finds an {@link ASTNode} for the given compilation unit at the 
	 * backing focal position
	 * @param unit
	 * @return the {@link BodyDeclaration} or <code>null</code>
	 */
	protected BodyDeclaration findNode(ICompilationUnit unit) {
		int focalpos = getFocalPosition();
		final CompilationUnit cunit = createAST(unit, focalpos, false);
		NodeFinder finder = new NodeFinder(focalpos);
		cunit.accept(finder);
		return finder.getNode();
	}
	
	/**
	 * Creates an {@link AST} with the given {@link ICompilationUnit} as source and the 
	 * {@link JavaCore#COMPILER_DOC_COMMENT_SUPPORT} options set.
	 * @param unit
	 * @param focalposition
	 * @param resolvebindings
	 * @return a new {@link AST}
	 */
	protected CompilationUnit createAST(final ICompilationUnit unit, int focalposition, boolean resolvebindings) {
		ASTParser parser = ASTParser.newParser(AST.JLS4);
		parser.setSource(unit);
		parser.setFocalPosition(focalposition);
		parser.setResolveBindings(resolvebindings);
		Map options = unit.getJavaProject().getOptions(true);
		options.put(JavaCore.COMPILER_DOC_COMMENT_SUPPORT, JavaCore.ENABLED);
		parser.setCompilerOptions(options);
		return (CompilationUnit) parser.createAST(new NullProgressMonitor());
	}
	
	/**
	 * Returns the marker message arguments from the backing {@link IMarker}. An empty 
	 * array is returned if the underlying {@link IMarker} is <code>null</code>
	 * @return the message arguments form the backing marker
	 */
	protected String[] getMarkerMessageArguments() {
		if(fBackingMarker != null) { 
			String arg = (String) fBackingMarker.getAttribute(IApiMarkerConstants.MARKER_ATTR_MESSAGE_ARGUMENTS, null);
			if(arg != null) {
				return arg.split("#"); //$NON-NLS-1$
			}
		}
		return new String[0];
	}
	
	/**
	 * Creates an {@link AST} with the given {@link ICompilationUnit} as source and the 
	 * {@link JavaCore#COMPILER_DOC_COMMENT_SUPPORT} options set.
	 * @param unit
	 * @return a new {@link AST}
	 */
	protected CompilationUnit createAST(final ICompilationUnit unit) {
		return createAST(unit, getFocalPosition(), false);
	}
	
	/**
	 * Returns the focal position to start AST scanning from (A.K.A) the {@link IMarker#CHAR_START} attribute
	 * @return the focal position
	 */
	protected int getFocalPosition() {
		if(fBackingMarker == null) {
			return 0;
		}
		return fBackingMarker.getAttribute(IMarker.CHAR_START, 0);
	}
	
	/**
	 * Returns the {@link IApiJavadocTag} kind of the parent {@link ASTNode} for the given 
	 * node or -1 if the parent is not found or not a {@link TypeDeclaration}
	 * @param node
	 * @return the {@link IApiJavadocTag} kind of the parent or -1
	 */
	protected int getParentKind(ASTNode node) {
		if(node == null) {
			return -1;
		}
		if(node instanceof TypeDeclaration) {
			return ((TypeDeclaration)node).isInterface() ? IApiJavadocTag.TYPE_INTERFACE : IApiJavadocTag.TYPE_CLASS;
		}
		return getParentKind(node.getParent());
	}
	
	/**
	 * Returns if the given body declaration is a constructor or not
	 * @param body
	 * @return
	 */
	protected boolean isConstructor(BodyDeclaration body) {
		if(body.getNodeType() == ASTNode.METHOD_DECLARATION) {
			return ((MethodDeclaration)body).isConstructor();
		}
		return false;
	}
	
	/**
	 * Updates the given monitor with the given tick count and polls for cancellation. If the monitor
	 * is cancelled an {@link OperationCanceledException} is thrown
	 * @param monitor
	 * @param ticks
	 * @throws OperationCanceledException
	 */
	protected void updateMonitor(final IProgressMonitor monitor, int ticks) throws OperationCanceledException {
		if(monitor != null) {
			monitor.worked(ticks);
			if (monitor.isCanceled()) {
				throw new OperationCanceledException();
			}
		}
	}
}
