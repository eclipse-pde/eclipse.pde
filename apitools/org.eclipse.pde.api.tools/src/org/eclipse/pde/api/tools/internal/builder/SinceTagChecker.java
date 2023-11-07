/*******************************************************************************
 * Copyright (c) 2008, 2020 IBM Corporation and others.
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
package org.eclipse.pde.api.tools.internal.builder;

import java.util.List;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.AbstractTypeDeclaration;
import org.eclipse.jdt.core.dom.AnnotationTypeDeclaration;
import org.eclipse.jdt.core.dom.AnnotationTypeMemberDeclaration;
import org.eclipse.jdt.core.dom.BodyDeclaration;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.EnumConstantDeclaration;
import org.eclipse.jdt.core.dom.EnumDeclaration;
import org.eclipse.jdt.core.dom.FieldDeclaration;
import org.eclipse.jdt.core.dom.Initializer;
import org.eclipse.jdt.core.dom.Javadoc;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.RecordDeclaration;
import org.eclipse.jdt.core.dom.TagElement;
import org.eclipse.jdt.core.dom.TextElement;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;
import org.eclipse.pde.api.tools.internal.util.Util;

/**
 * An AST visitor used to find missing or incorrect @since tags
 *
 * @since 1.0.0
 */
public class SinceTagChecker extends ASTVisitor {

	private static final int ABORT = 0x01;
	private static final int MISSING = 0x02;
	private static final int HAS_JAVA_DOC = 0x04;
	private static final int HAS_NO_COMMENT = 0x10;

	private final int nameStart;
	int bits;
	private String sinceVersion;

	/**
	 * Constructor
	 */
	public SinceTagChecker(int nameStart) {
		this.nameStart = nameStart;
	}

	@Override
	public boolean visit(CompilationUnit compilationUnit) {
		return true;
	}

	@Override
	public boolean visit(VariableDeclarationFragment node) {
		if ((this.bits & ABORT) != 0) {
			return false;
		}
		if (node.getName().getStartPosition() == this.nameStart) {
			this.bits |= ABORT;
			ASTNode parent = node.getParent();
			if (parent.getNodeType() == ASTNode.FIELD_DECLARATION) {
				FieldDeclaration fieldDeclaration = (FieldDeclaration) parent;
				processJavadoc(fieldDeclaration);
			}
		}
		return false;
	}

	@Override
	public boolean visit(EnumDeclaration node) {
		return visitAbstractTypeDeclaration(node);
	}

	@Override
	public boolean visit(TypeDeclaration node) {
		return visitAbstractTypeDeclaration(node);
	}

	@Override
	public boolean visit(RecordDeclaration node) {
		return visitAbstractTypeDeclaration(node);
	}

	private boolean visitAbstractTypeDeclaration(AbstractTypeDeclaration declaration) {
		if ((this.bits & ABORT) != 0) {
			return false;
		}
		if (declaration.getName().getStartPosition() == this.nameStart) {
			this.bits |= ABORT;
			processJavadoc(declaration);
		}
		return true;
	}

	@Override
	public boolean visit(AnnotationTypeDeclaration node) {
		return visitAbstractTypeDeclaration(node);
	}

	@Override
	public boolean visit(MethodDeclaration node) {
		if ((this.bits & ABORT) != 0) {
			return false;
		}
		if (node.getName().getStartPosition() == this.nameStart) {
			this.bits |= ABORT;
			processJavadoc(node);
		}
		return false;
	}

	@Override
	public boolean visit(AnnotationTypeMemberDeclaration node) {
		if ((this.bits & ABORT) != 0) {
			return false;
		}
		if (node.getName().getStartPosition() == this.nameStart) {
			this.bits |= ABORT;
			processJavadoc(node);
		}
		return false;
	}

	@Override
	public boolean visit(Initializer node) {
		return false;
	}

	@Override
	public boolean visit(EnumConstantDeclaration node) {
		if ((this.bits & ABORT) != 0) {
			return false;
		}
		if (node.getName().getStartPosition() == this.nameStart) {
			this.bits |= ABORT;
			processJavadoc(node);
		}
		return false;
	}

	/**
	 * Processes a javadoc tag
	 */
	private void processJavadoc(BodyDeclaration bodyDeclaration) {
		Javadoc javadoc = bodyDeclaration.getJavadoc();
		boolean found = false;
		if (javadoc != null) {
			this.bits |= HAS_JAVA_DOC;
			List<TagElement> tags = javadoc.tags();
			for (TagElement element : tags) {
				String tagName = element.getTagName();
				if (TagElement.TAG_SINCE.equals(tagName)) {
					// @since is present
					// check if valid
					found = true;
					List<ASTNode> fragments = element.fragments();
					if (fragments.size() >= 1) {
						ASTNode fragment = fragments.get(0);
						if (fragment.getNodeType() == ASTNode.TEXT_ELEMENT) {
							this.sinceVersion = ((TextElement) fragment).getText();
						}
					} else {
						this.sinceVersion = Util.EMPTY_STRING;
					}
					break;
				}
			}
			if (!found) {
				this.bits |= MISSING;
			}
		} else {
			this.bits |= HAS_NO_COMMENT;
		}
	}

	/**
	 * @return if the javadoc tag is missing
	 */
	public boolean isMissing() {
		return (this.bits & MISSING) != 0;
	}

	/**
	 * @return if there is no javadoc tag
	 */
	public boolean hasNoComment() {
		return (this.bits & HAS_NO_COMMENT) != 0;
	}

	/**
	 * @return if there already is a doc comment
	 */
	public boolean hasJavadocComment() {
		return (this.bits & HAS_JAVA_DOC) != 0;
	}

	/**
	 * @return the version the should be placed in the tag
	 */
	public String getSinceVersion() {
		if (this.sinceVersion != null) {
			return this.sinceVersion.trim();
		}
		return null;
	}
}
