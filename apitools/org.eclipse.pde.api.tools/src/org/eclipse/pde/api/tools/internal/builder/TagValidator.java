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
package org.eclipse.pde.api.tools.internal.builder;

import java.util.HashSet;
import java.util.Iterator;
import java.util.List;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.Flags;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.FieldDeclaration;
import org.eclipse.jdt.core.dom.Javadoc;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.TagElement;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.pde.api.tools.internal.JavadocTagManager;
import org.eclipse.pde.api.tools.internal.problems.ApiProblemFactory;
import org.eclipse.pde.api.tools.internal.provisional.ApiPlugin;
import org.eclipse.pde.api.tools.internal.provisional.IApiJavadocTag;
import org.eclipse.pde.api.tools.internal.provisional.IApiMarkerConstants;
import org.eclipse.pde.api.tools.internal.provisional.descriptors.IElementDescriptor;
import org.eclipse.pde.api.tools.internal.provisional.problems.IApiProblem;
import org.eclipse.pde.api.tools.internal.util.Util;

/**
 * Visit Javadoc comments of types and member to find API javadoc tags that are being misused
 * 
 * @since 1.0.0
 */
public class TagValidator extends ASTVisitor {

	/**
	 * backing collection of tag problems, if any
	 */
	private HashSet fTagProblems = null;
	
	private ICompilationUnit fCompilationUnit = null;
	
	/**
	 * Constructor
	 * @param parent
	 */
	public TagValidator(ICompilationUnit parent) {
		fCompilationUnit = parent;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.jdt.core.dom.ASTVisitor#visit(org.eclipse.jdt.core.dom.Javadoc)
	 */
	public boolean visit(Javadoc node) {
		ASTNode parent = node.getParent();
		if(parent != null) {
			List tags = node.tags();
			validateTags(parent, tags);
		}
		return false;
	}
	
	/**
	 * Validates the set of tags for the given parent node and the given listing of {@link TagElement}s
	 * @param node
	 * @param tags
	 */
	private void validateTags(ASTNode node, List tags) {
		if(tags.size() == 0) {
			return;
		}
		JavadocTagManager jtm = ApiPlugin.getJavadocTagManager();
		switch(node.getNodeType()) {
			case ASTNode.TYPE_DECLARATION: {
				TypeDeclaration type = (TypeDeclaration) node;
				IApiJavadocTag[] validtags = jtm.getTagsForType(type.isInterface() ? IApiJavadocTag.TYPE_INTERFACE : IApiJavadocTag.TYPE_CLASS, IApiJavadocTag.MEMBER_NONE);
				String context = type.isInterface() ? BuilderMessages.TagValidator_an_interface : BuilderMessages.TagValidator_a_class;
				processTags(tags, validtags, IElementDescriptor.T_REFERENCE_TYPE, context);
				break;
			}
			case ASTNode.METHOD_DECLARATION: {
				MethodDeclaration method = (MethodDeclaration) node;
				IApiJavadocTag[] validtags = jtm.getTagsForType(getParentKind(node), method.isConstructor() ? IApiJavadocTag.MEMBER_CONSTRUCTOR : IApiJavadocTag.MEMBER_METHOD);
				String context = method.isConstructor() ? BuilderMessages.TagValidator_a_constructor : BuilderMessages.TagValidator_a_method;
				processTags(tags, validtags, IElementDescriptor.T_METHOD, context);
				break;
			}
			case ASTNode.FIELD_DECLARATION: {
				FieldDeclaration field = (FieldDeclaration) node;
				IApiJavadocTag[] validtags = jtm.getTagsForType(getParentKind(node), IApiJavadocTag.MEMBER_FIELD);
				boolean isfinal = Flags.isFinal(field.getModifiers());
				String context = isfinal ? BuilderMessages.TagValidator_a_final_field : BuilderMessages.TagValidator_a_field;
				processTags(tags, isfinal ? new IApiJavadocTag[0] : validtags, IElementDescriptor.T_FIELD, context);
				break;
			}
		}
	}
	
	/**
	 * Processes the listing of valid tags against the listing of existing tags on the node, and
	 * creates errors if disallowed tags are found.
	 * @param tags
	 * @param validtags
	 * @param element
	 * @param context
	 */
	private void processTags(List tags, IApiJavadocTag[] validtags, int element, String context) {
		IApiJavadocTag[] alltags = ApiPlugin.getJavadocTagManager().getAllTags();
		HashSet invalidtags = new HashSet(alltags.length);
		for(int i = 0; i < alltags.length; i++) {
			invalidtags.add(alltags[i].getTagLabel());
		}
		for(int i = 0; i < validtags.length; i++) {
			invalidtags.remove(validtags[i]);
		}
		if(invalidtags.size() == 0) {
			return;
		}
		TagElement tag = null;
		for(Iterator iter = tags.iterator(); iter.hasNext();) {
			tag = (TagElement) iter.next();
			if(invalidtags.contains(tag.getTagName())) {
				processTagProblem(tag, element, context);
			}
		}
	}
	
	/**
	 * Creates a new {@link IApiProblem} for the given tag and adds it to the cache
	 * @param tag
	 * @param element
	 * @param context
	 */
	private void processTagProblem(TagElement tag, int element, String context) {
		if(fTagProblems == null) {
			fTagProblems = new HashSet(10);
		}
		int charstart = tag.getStartPosition();
		int charend = charstart + tag.getTagName().length();
		int linenumber = -1;
		try {
			// unit cannot be null
			IDocument document = Util.getDocument(fCompilationUnit);
			linenumber = document.getLineOfOffset(charstart);
		} 
		catch (BadLocationException e) {} 
		catch (CoreException e) {}
		try {
			IApiProblem problem = ApiProblemFactory.newApiProblem(fCompilationUnit.getCorrespondingResource().getProjectRelativePath().toPortableString(), 
					new String[] {tag.getTagName(), context}, 
					new String[] {IApiMarkerConstants.API_MARKER_ATTR_ID, IApiMarkerConstants.MARKER_ATTR_HANDLE_ID}, 
					new Object[] {new Integer(IApiMarkerConstants.UNSUPPORTED_TAG_MARKER_ID), fCompilationUnit.getHandleIdentifier()}, 
					linenumber, 
					charstart, 
					charend, 
					IApiProblem.CATEGORY_USAGE, 
					element, 
					IApiProblem.UNSUPPORTED_TAG_USE, 
					IApiProblem.NO_FLAGS);
			
			fTagProblems.add(problem);
		} 
		catch (JavaModelException e) {}
	}
	
	/**
	 * Returns the {@link IApiJavadocTag} kind of the parent {@link ASTNode} for the given 
	 * node or -1 if the parent is not found or not a {@link TypeDeclaration}
	 * @param node
	 * @return the {@link IApiJavadocTag} kind of the parent or -1
	 */
	private int getParentKind(ASTNode node) {
		if(node == null) {
			return -1;
		}
		if(node instanceof TypeDeclaration) {
			return ((TypeDeclaration)node).isInterface() ? IApiJavadocTag.TYPE_INTERFACE : IApiJavadocTag.TYPE_CLASS;
		}
		return getParentKind(node.getParent());
	}
	
	/**
	 * Returns the complete listing of API tag problems found during the scan or 
	 * an empty array, never <code>null</code>
	 * @return the complete listing of API tag problems found
	 */
	public IApiProblem[] getTagProblems() {
		if(fTagProblems == null) {
			return new IApiProblem[0];
		}
		return (IApiProblem[]) fTagProblems.toArray(new IApiProblem[fTagProblems.size()]);
	}
}
