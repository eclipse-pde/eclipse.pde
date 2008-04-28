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

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.AnnotationTypeDeclaration;
import org.eclipse.jdt.core.dom.AnnotationTypeMemberDeclaration;
import org.eclipse.jdt.core.dom.BodyDeclaration;
import org.eclipse.jdt.core.dom.EnumConstantDeclaration;
import org.eclipse.jdt.core.dom.EnumDeclaration;
import org.eclipse.jdt.core.dom.FieldDeclaration;
import org.eclipse.jdt.core.dom.Initializer;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.TypeDeclaration;

/**
 * Finds an AST node at the given position in the AST, and allows access to the {@link BodyDeclaration} at that position
 * 
 * @since 1.0.0
 * @noextend This class is not intended to be subclassed by clients.
 */
public class NodeFinder extends ASTVisitor {
		
	private BodyDeclaration declaration;
	private int position;
	
	/**
	 * Constructor
	 * @param position
	 */
	public NodeFinder(int position) {
		this.position = position;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jdt.core.dom.ASTVisitor#visit(org.eclipse.jdt.core.dom.AnnotationTypeDeclaration)
	 */
	public boolean visit(AnnotationTypeDeclaration node) {
		return visitNode(node);
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.jdt.core.dom.ASTVisitor#visit(org.eclipse.jdt.core.dom.EnumDeclaration)
	 */
	public boolean visit(EnumDeclaration node) {
		return visitNode(node);
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.jdt.core.dom.ASTVisitor#visit(org.eclipse.jdt.core.dom.TypeDeclaration)
	 */
	public boolean visit(TypeDeclaration node) {
		return visitNode(node);
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.jdt.core.dom.ASTVisitor#visit(org.eclipse.jdt.core.dom.MethodDeclaration)
	 */
	public boolean visit(MethodDeclaration node) {
		return visitNode(node);
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.jdt.core.dom.ASTVisitor#visit(org.eclipse.jdt.core.dom.FieldDeclaration)
	 */
	public boolean visit(FieldDeclaration node) {
		return visitNode(node);
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.jdt.core.dom.ASTVisitor#visit(org.eclipse.jdt.core.dom.EnumConstantDeclaration)
	 */
	public boolean visit(EnumConstantDeclaration node) {
		return visitNode(node);
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.jdt.core.dom.ASTVisitor#visit(org.eclipse.jdt.core.dom.AnnotationTypeMemberDeclaration)
	 */
	public boolean visit(AnnotationTypeMemberDeclaration node) {
		return visitNode(node);
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.jdt.core.dom.ASTVisitor#visit(org.eclipse.jdt.core.dom.Initializer)
	 */
	public boolean visit(Initializer node) {
		return false;
	}
	
	/**
	 * Visit the {@link BodyDeclaration} node to see if it is at the specified position
	 * @param bodyDeclaration
	 * @return
	 */
	private boolean visitNode(BodyDeclaration bodyDeclaration) {
		int start = bodyDeclaration.getStartPosition();
		int end = bodyDeclaration.getLength() - 1 + start;
		switch(bodyDeclaration.getNodeType()) {
			case ASTNode.TYPE_DECLARATION :
			case ASTNode.ENUM_DECLARATION :
			case ASTNode.ANNOTATION_TYPE_DECLARATION :
				if (start <= this.position && this.position <= end) {
					this.declaration = bodyDeclaration;
					return true;
				}
				return false;
			case ASTNode.ENUM_CONSTANT_DECLARATION :
			case ASTNode.FIELD_DECLARATION :
			case ASTNode.METHOD_DECLARATION :
			case ASTNode.ANNOTATION_TYPE_MEMBER_DECLARATION :
				if (start <= this.position && this.position <= end) {
					this.declaration = bodyDeclaration;
				}
				return false;
			default :
				return false;
		}
	}
	/**
	 * @return the {@link BodyDeclaration} at the given position or <code>null</code> if one was not found at that position
	 */
	public BodyDeclaration getNode() {
		return this.declaration;
	}
}