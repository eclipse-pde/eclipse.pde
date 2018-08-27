/*******************************************************************************
 * Copyright (c) Sep 11, 2013 IBM Corporation and others.
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

import java.util.ArrayList;
import java.util.Stack;

import org.eclipse.jdt.core.Flags;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.AbstractTypeDeclaration;
import org.eclipse.jdt.core.dom.AnnotationTypeDeclaration;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.EnumDeclaration;
import org.eclipse.jdt.core.dom.PackageDeclaration;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.eclipse.pde.api.tools.internal.provisional.IApiJavadocTag;
import org.eclipse.pde.api.tools.internal.provisional.problems.IApiProblem;

/**
 * Base class for validators
 *
 * @since 1.0.600
 */
public abstract class Validator extends ASTVisitor {

	class Item {
		String typename;
		int flags;
		boolean visible = false;

		Item(String name, int flags, boolean vis) {
			typename = name;
			this.flags = flags;
			visible = vis;
		}
	}

	private boolean isvisible = true;
	/**
	 * Type stack that tracks types as the visitor descends
	 */
	private Stack<Item> fStack = new Stack<>();
	/**
	 * The compilation unit we are scanning
	 */
	protected ICompilationUnit fCompilationUnit = null;
	/**
	 * Backing collection of problems, if any
	 */
	private ArrayList<IApiProblem> fProblems = null;

	@Override
	public boolean visit(AnnotationTypeDeclaration node) {
		isvisible &= !Flags.isPrivate(node.getModifiers());
		fStack.push(new Item(getTypeName(node), node.getModifiers(), isvisible));
		return true;
	}

	@Override
	public void endVisit(AnnotationTypeDeclaration node) {
		fStack.pop();
		if (!fStack.isEmpty()) {
			Item item = fStack.peek();
			isvisible = item.visible;
		}
	}

	@Override
	public boolean visit(TypeDeclaration node) {
		int flags = node.getModifiers();
		if (Flags.isPrivate(flags)) {
			isvisible &= false;
		} else {
			if (node.isMemberTypeDeclaration()) {
				isvisible &= (Flags.isPublic(flags) && Flags.isStatic(flags)) || Flags.isPublic(flags) || Flags.isProtected(flags) || node.isInterface();
			} else {
				isvisible &= (!Flags.isPrivate(flags) && !Flags.isPackageDefault(flags)) || node.isInterface();
			}
		}
		fStack.push(new Item(getTypeName(node), node.getModifiers(), isvisible));
		return true;
	}

	@Override
	public void endVisit(TypeDeclaration node) {
		fStack.pop();
		if (!fStack.isEmpty()) {
			Item item = fStack.peek();
			isvisible = item.visible;
		}
	}

	@Override
	public boolean visit(EnumDeclaration node) {
		int flags = node.getModifiers();
		if (node.isMemberTypeDeclaration()) {
			isvisible &= Flags.isPublic(flags);
		} else {
			isvisible &= !Flags.isPrivate(flags) && !Flags.isPackageDefault(flags);
		}
		fStack.push(new Item(getTypeName(node), node.getModifiers(), isvisible));
		return true;
	}

	@Override
	public void endVisit(EnumDeclaration node) {
		fStack.pop();
		if (!fStack.isEmpty()) {
			Item item = fStack.peek();
			isvisible = item.visible;
		}
	}

	@Override
	public void endVisit(CompilationUnit node) {
		fStack.clear();
	}

	/**
	 * Returns the next item on the tip of the stack
	 *
	 * @return the next {@link Item}
	 */
	protected Item getItem() {
		return fStack.peek();
	}

	/**
	 * Returns the fully qualified name of the enclosing type for the given node
	 *
	 * @param node
	 * @return the fully qualified name of the enclosing type
	 */
	protected String getTypeName(ASTNode node) {
		return getTypeName(node, new StringBuilder());
	}

	/**
	 * Constructs the qualified name of the enclosing parent type
	 *
	 * @param node the node to get the parent name for
	 * @param buffer the buffer to write the name into
	 * @return the fully qualified name of the parent
	 */
	protected String getTypeName(ASTNode node, StringBuilder buffer) {
		switch (node.getNodeType()) {
			case ASTNode.COMPILATION_UNIT: {
				CompilationUnit unit = (CompilationUnit) node;
				PackageDeclaration packageDeclaration = unit.getPackage();
				if (packageDeclaration != null) {
					buffer.insert(0, '.');
					buffer.insert(0, packageDeclaration.getName().getFullyQualifiedName());
				}
				return String.valueOf(buffer);
			}
			default: {
				if (node instanceof AbstractTypeDeclaration) {
					AbstractTypeDeclaration typeDeclaration = (AbstractTypeDeclaration) node;
					if (typeDeclaration.isPackageMemberTypeDeclaration()) {
						buffer.insert(0, typeDeclaration.getName().getIdentifier());
					} else {
						buffer.insert(0, typeDeclaration.getName().getFullyQualifiedName());
						buffer.insert(0, '$');
					}
				}
			}
		}
		return getTypeName(node.getParent(), buffer);
	}

	/**
	 * Returns the {@link IApiJavadocTag} kind of the parent {@link ASTNode} for
	 * the given node or -1 if the parent is not found or not a
	 * {@link TypeDeclaration}
	 *
	 * @param node
	 * @return the {@link IApiJavadocTag} kind of the parent or -1
	 */
	protected int getParentKind(ASTNode node) {
		if (node == null) {
			return -1;
		}
		if (node instanceof TypeDeclaration) {
			return ((TypeDeclaration) node).isInterface() ? IApiJavadocTag.TYPE_INTERFACE : IApiJavadocTag.TYPE_CLASS;
		} else if (node instanceof AnnotationTypeDeclaration) {
			return IApiJavadocTag.TYPE_ANNOTATION;
		} else if (node instanceof EnumDeclaration) {
			return IApiJavadocTag.TYPE_ENUM;
		}
		return getParentKind(node.getParent());
	}

	/**
	 * Returns the modifiers from the smallest enclosing type containing the
	 * given node
	 *
	 * @param node
	 * @return the modifiers for the smallest enclosing type or 0
	 */
	protected int getParentModifiers(ASTNode node) {
		if (node == null) {
			return 0;
		}
		if (node instanceof AbstractTypeDeclaration) {
			AbstractTypeDeclaration type = (AbstractTypeDeclaration) node;
			return type.getModifiers();
		}
		return getParentModifiers(node.getParent());
	}

	/**
	 * Adds a found problem to the listing
	 *
	 * @param problem
	 */
	protected void addProblem(IApiProblem problem) {
		if (fProblems == null) {
			fProblems = new ArrayList<>();
		}
		fProblems.add(problem);
	}

	/**
	 * Returns the complete listing of API annotation problems found during the
	 * scan or an empty array, never <code>null</code>
	 *
	 * @return the complete listing of API annotation problems found
	 */
	public IApiProblem[] getProblems() {
		if (fProblems == null) {
			return new IApiProblem[0];
		}
		return fProblems.toArray(new IApiProblem[fProblems.size()]);
	}
}
