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

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.Flags;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.AbstractTypeDeclaration;
import org.eclipse.jdt.core.dom.AnnotationTypeDeclaration;
import org.eclipse.jdt.core.dom.AnnotationTypeMemberDeclaration;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.EnumConstantDeclaration;
import org.eclipse.jdt.core.dom.EnumDeclaration;
import org.eclipse.jdt.core.dom.FieldDeclaration;
import org.eclipse.jdt.core.dom.Javadoc;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.PackageDeclaration;
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
	private ArrayList fTagProblems = null;
	
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
				String context = BuilderMessages.TagValidator_an_interface;
				if(!type.isInterface()) {
					context = BuilderMessages.TagValidator_a_class;
					if(Flags.isAbstract(type.getModifiers())) {
						context = BuilderMessages.TagValidator_an_abstract_class;
						ArrayList vtags = new ArrayList(validtags.length);
						for(int i = 0; i < validtags.length; i++) {
							if(validtags[i].getTagName().equals("@noinstantiate")) { //$NON-NLS-1$
								continue;
							}
							vtags.add(validtags[i]);
						}
						validtags = (IApiJavadocTag[]) vtags.toArray(new IApiJavadocTag[vtags.size()]);
					}
				}
				processTags(getTypeName(type), tags, validtags, IElementDescriptor.T_REFERENCE_TYPE, context);
				break;
			}
			case ASTNode.ENUM_DECLARATION: {
				EnumDeclaration enumm = (EnumDeclaration) node;
				IApiJavadocTag[] validtags = jtm.getTagsForType(IApiJavadocTag.TYPE_ENUM, IApiJavadocTag.MEMBER_NONE);
				processTags(getTypeName(enumm), tags, validtags, IElementDescriptor.T_REFERENCE_TYPE, BuilderMessages.TagValidator_an_enum);
				break;
			}
			case ASTNode.ENUM_CONSTANT_DECLARATION: {
				EnumConstantDeclaration decl = (EnumConstantDeclaration) node;
				IApiJavadocTag[] validtags = jtm.getTagsForType(IApiJavadocTag.TYPE_ENUM, IApiJavadocTag.MEMBER_ENUM_CONSTANT);
				processTags(getTypeName(decl), tags, validtags, IElementDescriptor.T_FIELD, BuilderMessages.TagValidator_an_enum_constant);
				break;
			}
			case ASTNode.ANNOTATION_TYPE_DECLARATION: {
				AnnotationTypeDeclaration annot = (AnnotationTypeDeclaration) node;
				IApiJavadocTag[] validtags = jtm.getTagsForType(IApiJavadocTag.TYPE_ANNOTATION, IApiJavadocTag.MEMBER_NONE);
				processTags(getTypeName(annot), tags, validtags, IElementDescriptor.T_REFERENCE_TYPE, BuilderMessages.TagValidator_an_annotation);
				break;
			}
			case ASTNode.METHOD_DECLARATION: {
				MethodDeclaration method = (MethodDeclaration) node;
				int pkind = getParentKind(node);
				String context = null;
				switch(pkind) {
					case IApiJavadocTag.TYPE_ENUM: {
						context = BuilderMessages.TagValidator_an_enum_method;
						break;
					}
					default: {
						context = method.isConstructor() ? BuilderMessages.TagValidator_a_constructor : BuilderMessages.TagValidator_a_method;
						break;
					}
				}
				IApiJavadocTag[] validtags = jtm.getTagsForType(pkind, method.isConstructor() ? IApiJavadocTag.MEMBER_CONSTRUCTOR : IApiJavadocTag.MEMBER_METHOD);
				processTags(getTypeName(method), tags, validtags, IElementDescriptor.T_METHOD, context);
				break;
			}
			case ASTNode.ANNOTATION_TYPE_MEMBER_DECLARATION: {
				AnnotationTypeMemberDeclaration decl = (AnnotationTypeMemberDeclaration) node;
				IApiJavadocTag[] validtags = jtm.getTagsForType(IApiJavadocTag.TYPE_ANNOTATION, IApiJavadocTag.MEMBER_METHOD);
				processTags(getTypeName(decl), tags, validtags, IElementDescriptor.T_METHOD, BuilderMessages.TagValidator_an_annotation_method);
				break;
			}
			case ASTNode.FIELD_DECLARATION: {
				FieldDeclaration field = (FieldDeclaration) node;
				int pkind = getParentKind(node);
				String context = null;
				boolean isfinal = Flags.isFinal(field.getModifiers());
				switch(pkind) {
					case IApiJavadocTag.TYPE_ANNOTATION: {
						context = BuilderMessages.TagValidator_annotation_field;
						break;
					}
					case IApiJavadocTag.TYPE_ENUM: {
						context = BuilderMessages.TagValidator_enum_field;
						break;
					}
					default: {
						context = isfinal ? BuilderMessages.TagValidator_a_final_field : BuilderMessages.TagValidator_a_field;
						break;
					}
				}
				IApiJavadocTag[] validtags = jtm.getTagsForType(pkind, IApiJavadocTag.MEMBER_FIELD);
				processTags(getTypeName(field), tags, isfinal ? new IApiJavadocTag[0] : validtags, IElementDescriptor.T_FIELD, context);
				break;
			}
		}
	}
	
	/**
	 * Returns the fully qualified name of the enclosing type for the given node
	 * @param node
	 * @return the fully qualified name of the enclosing type
	 */
	private String getTypeName(ASTNode node) {
		return getTypeName(node, new StringBuffer());
	}
	
	/**
	 * Constructs the qualified name of the enclosing parent type
	 * @param node the node to get the parent name for
	 * @param buffer the buffer to write the name into
	 * @return the fully qualified name of the parent 
	 */
	private String getTypeName(ASTNode node, StringBuffer buffer) {
		switch(node.getNodeType()) {
			case ASTNode.COMPILATION_UNIT : {
				CompilationUnit unit = (CompilationUnit) node;
				PackageDeclaration packageDeclaration = unit.getPackage();
				if (packageDeclaration != null) {
					buffer.insert(0, '.');
					buffer.insert(0, packageDeclaration.getName().getFullyQualifiedName());
				}
				return String.valueOf(buffer);
			}
			default : {
				if (node instanceof AbstractTypeDeclaration) {
					AbstractTypeDeclaration typeDeclaration = (AbstractTypeDeclaration) node;
					if (typeDeclaration.isPackageMemberTypeDeclaration()) {
						buffer.insert(0, typeDeclaration.getName().getIdentifier());
					}
					else {
						buffer.insert(0, typeDeclaration.getName().getFullyQualifiedName());
						buffer.insert(0, '$');
					}
				}
			}
		}
		return getTypeName(node.getParent(), buffer);
	}

	/**
	 * Processes the listing of valid tags against the listing of existing tags on the node, and
	 * creates errors if disallowed tags are found.
	 * @param tags
	 * @param validtags
	 * @param element
	 * @param context
	 */
	private void processTags(String typeName, List tags, IApiJavadocTag[] validtags, int element, String context) {
		IApiJavadocTag[] alltags = ApiPlugin.getJavadocTagManager().getAllTags();
		HashSet invalidtags = new HashSet(alltags.length);
		for(int i = 0; i < alltags.length; i++) {
			invalidtags.add(alltags[i].getTagName());
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
				processTagProblem(typeName, tag, element, context);
			}
		}
	}
	
	/**
	 * Creates a new {@link IApiProblem} for the given tag and adds it to the cache
	 * @param tag
	 * @param element
	 * @param context
	 */
	private void processTagProblem(String typeName, TagElement tag, int element, String context) {
		if(fTagProblems == null) {
			fTagProblems = new ArrayList(10);
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
					typeName,
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
		else if(node instanceof AnnotationTypeDeclaration) {
			return IApiJavadocTag.TYPE_ANNOTATION;
		}
		else if(node instanceof EnumDeclaration) {
			return IApiJavadocTag.TYPE_ENUM;
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
