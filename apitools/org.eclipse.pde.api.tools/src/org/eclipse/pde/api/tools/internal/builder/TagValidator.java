/*******************************************************************************
 * Copyright (c) 2008, 2013 IBM Corporation and others.
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
import java.util.Set;

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
	
	private static final IApiJavadocTag[] NO_TAGS = new IApiJavadocTag[0];
	
	/**
	 * Temporarily stores a node (type, enum, annotation) that is restricted 
	 * (private or package default).  Any tags in children of this node are
	 * invalid.
	 */
	private AbstractTypeDeclaration fRestrictedNode;
	
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
	
	
	public boolean visit(AnnotationTypeDeclaration node) {
		if (fRestrictedNode == null && (Flags.isPrivate(node.getModifiers()) || Flags.isPackageDefault(node.getModifiers()))){
			fRestrictedNode = node;
		}
		return true;
	}
	
	public void endVisit(AnnotationTypeDeclaration node) {
		if (fRestrictedNode != null && fRestrictedNode.equals(node)){
			fRestrictedNode = null;
		}
	}
	
	public boolean visit(TypeDeclaration node) {
		if (fRestrictedNode == null && (Flags.isPrivate(node.getModifiers()) || Flags.isPackageDefault(node.getModifiers()))){
			fRestrictedNode = node;
		}
		return true;
	}
	
	public void endVisit(TypeDeclaration node) {
		if (fRestrictedNode != null && fRestrictedNode.equals(node)){
			fRestrictedNode = null;
		}
	}
	
	public boolean visit(EnumDeclaration node) {
		if (fRestrictedNode == null && (Flags.isPrivate(node.getModifiers()) || Flags.isPackageDefault(node.getModifiers()))){
			fRestrictedNode = node;
		}
		return true;
	}
	
	public void endVisit(EnumDeclaration node){
		if (fRestrictedNode != null && fRestrictedNode.equals(node)){
			fRestrictedNode = null;
		}
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
		
		IApiJavadocTag[] validtags = NO_TAGS;
		JavadocTagManager jtm = ApiPlugin.getJavadocTagManager();
		switch(node.getNodeType()) {
			case ASTNode.TYPE_DECLARATION: {
				TypeDeclaration type = (TypeDeclaration) node;
				validtags = jtm.getTagsForType(type.isInterface() ? IApiJavadocTag.TYPE_INTERFACE : IApiJavadocTag.TYPE_CLASS, IApiJavadocTag.MEMBER_NONE);
				HashSet invalidtags = new HashSet(validtags.length);
				String context = BuilderMessages.TagValidator_an_interface;
				if(!type.isInterface()) {
					context = BuilderMessages.TagValidator_a_class;
					int flags = type.getModifiers();
					if(Flags.isPrivate(flags)) {
						context = BuilderMessages.TagValidator_a_private_class;
						invalidtags.add(JavadocTagManager.TAG_NOINSTANTIATE);
						invalidtags.add(JavadocTagManager.TAG_NOEXTEND);
					}
					else if(Flags.isPackageDefault(flags)) {
						context = BuilderMessages.TagValidator_a_package_default_class;
						invalidtags.add(JavadocTagManager.TAG_NOINSTANTIATE);
						invalidtags.add(JavadocTagManager.TAG_NOEXTEND);
					}
					else if(Flags.isAbstract(flags)) {
						context = BuilderMessages.TagValidator_an_abstract_class;
						invalidtags.add(JavadocTagManager.TAG_NOINSTANTIATE);
					}
					else if(Flags.isFinal(flags)) {
						context = BuilderMessages.TagValidator_a_final_class;
						invalidtags.add(JavadocTagManager.TAG_NOEXTEND);
					}
				} else {
					int flags = type.getModifiers();
					if(Flags.isPackageDefault(flags)) {
						context = BuilderMessages.TagValidator_a_package_default_interface;
						invalidtags.add(JavadocTagManager.TAG_NOIMPLEMENT);
						invalidtags.add(JavadocTagManager.TAG_NOEXTEND);
					}
				}
				if(invalidtags.size() > 0) {
					ArrayList vtags = new ArrayList(validtags.length);
					for(int i = 0; i < validtags.length; i++) {
						if(invalidtags.contains(validtags[i].getTagName())) {
							continue;
						}
						vtags.add(validtags[i]);
					}
					validtags = (IApiJavadocTag[]) vtags.toArray(new IApiJavadocTag[vtags.size()]);
				}
				processTags(getTypeName(type), tags, validtags, IElementDescriptor.TYPE, context);
				break;
			}
			case ASTNode.ENUM_DECLARATION: {
				EnumDeclaration enumm = (EnumDeclaration) node;
				validtags = jtm.getTagsForType(IApiJavadocTag.TYPE_ENUM, IApiJavadocTag.MEMBER_NONE);
				processTags(getTypeName(enumm), tags, validtags, IElementDescriptor.TYPE, BuilderMessages.TagValidator_an_enum);
				break;
			}
			case ASTNode.ENUM_CONSTANT_DECLARATION: {
				EnumConstantDeclaration decl = (EnumConstantDeclaration) node;
				processTags(getTypeName(decl), tags, validtags, IElementDescriptor.FIELD, BuilderMessages.TagValidator_an_enum_constant);
				break;
			}
			case ASTNode.ANNOTATION_TYPE_DECLARATION: {
				AnnotationTypeDeclaration annot = (AnnotationTypeDeclaration) node;
				validtags = jtm.getTagsForType(IApiJavadocTag.TYPE_ANNOTATION, IApiJavadocTag.MEMBER_NONE);
				processTags(getTypeName(annot), tags, validtags, IElementDescriptor.TYPE, BuilderMessages.TagValidator_an_annotation);
				break;
			}
			case ASTNode.METHOD_DECLARATION: {
				MethodDeclaration method = (MethodDeclaration) node;
				int pkind = getParentKind(node);
				String context = null;
				int mods = method.getModifiers();
				boolean isprivate = Flags.isPrivate(mods);
				boolean ispackage = Flags.isPackageDefault(mods);
				boolean isconstructor = method.isConstructor();
				boolean isfinal = Flags.isFinal(mods);
				boolean isstatic = Flags.isStatic(mods);
				boolean pfinal = false;
				switch(pkind) {
					case IApiJavadocTag.TYPE_ENUM: {
						context = isprivate ? BuilderMessages.TagValidator_private_enum_method : BuilderMessages.TagValidator_an_enum_method;
						break;
					}
					case IApiJavadocTag.TYPE_INTERFACE: {
						context = BuilderMessages.TagValidator_an_interface_method;
						break;
					}
					default: {
						pfinal = Flags.isFinal(getParentModifiers(method));
						if(isprivate) {
							context = isconstructor ? BuilderMessages.TagValidator_private_constructor : BuilderMessages.TagValidator_private_method;
						}
						else if(!isstatic && ispackage) {
							context = isconstructor ? BuilderMessages.TagValidator_a_package_default_constructor : BuilderMessages.TagValidator_a_package_default_method;
						}
						else if(isstatic && isfinal) {
							context = BuilderMessages.TagValidator_a_static_final_method;
						}
						else if(isstatic && ispackage) {
							context = BuilderMessages.TagValidator_a_static_package_default_method;
						}
						else if (isfinal) {
							context = BuilderMessages.TagValidator_a_final_method;
						}
						else if(isstatic) {
							context = BuilderMessages.TagValidator_a_static_method;
						}
						else if(pfinal) {
							context = BuilderMessages.TagValidator_a_method_in_a_final_class;
						}
						else {
							context = isconstructor ? BuilderMessages.TagValidator_a_constructor : BuilderMessages.TagValidator_a_method;
						}
						break;
					}
				}
				if(!isprivate && !ispackage) {
					validtags = jtm.getTagsForType(pkind, isconstructor ? IApiJavadocTag.MEMBER_CONSTRUCTOR : IApiJavadocTag.MEMBER_METHOD);
				}
				if(isfinal || isstatic || pfinal) {
					ArrayList ttags = new ArrayList(validtags.length);
					for(int i = 0; i < validtags.length; i++) {
						if(!validtags[i].getTagName().equals(JavadocTagManager.TAG_NOOVERRIDE)) {
							ttags.add(validtags[i]);
						}
					}
					validtags = (IApiJavadocTag[]) ttags.toArray(new IApiJavadocTag[ttags.size()]);
				}
				processTags(getTypeName(method), tags, validtags, IElementDescriptor.METHOD, context);
				break;
			}
			case ASTNode.ANNOTATION_TYPE_MEMBER_DECLARATION: {
				AnnotationTypeMemberDeclaration decl = (AnnotationTypeMemberDeclaration) node;
				validtags = jtm.getTagsForType(IApiJavadocTag.TYPE_ANNOTATION, IApiJavadocTag.MEMBER_METHOD);
				processTags(getTypeName(decl), tags, validtags, IElementDescriptor.METHOD, BuilderMessages.TagValidator_an_annotation_method);
				break;
			}
			case ASTNode.FIELD_DECLARATION: {
				FieldDeclaration field = (FieldDeclaration) node;
				int pkind = getParentKind(node);
				String context = null;
				int flags = field.getModifiers();
				boolean isfinal = Flags.isFinal(flags);
				boolean isprivate = Flags.isPrivate(flags);
				boolean ispackage = Flags.isPackageDefault(flags);
				switch(pkind) {
					case IApiJavadocTag.TYPE_ANNOTATION: {
						context = BuilderMessages.TagValidator_annotation_field;
						if(isfinal) {
							context = BuilderMessages.TagValidator_a_final_annotation_field;
						}
						break;
					}
					case IApiJavadocTag.TYPE_ENUM: {
						context = isprivate ? BuilderMessages.TagValidator_private_enum_field : BuilderMessages.TagValidator_enum_field;
						break;
					}
					default: {
						if(isprivate) {
							context = BuilderMessages.TagValidator_private_field;
						}
						else if(ispackage) {
							context = BuilderMessages.TagValidator_a_package_default_field;
						}
						else {
							context = isfinal ? BuilderMessages.TagValidator_a_final_field : BuilderMessages.TagValidator_a_field;
						}
						break;
					}
				}
				
				if(!isprivate && !isfinal && !ispackage) {
					validtags = jtm.getTagsForType(pkind, IApiJavadocTag.MEMBER_FIELD);
				}
				processTags(getTypeName(field), tags, validtags, IElementDescriptor.FIELD, context);
				break;
			}
		default:
			break;
		}
		
		// If a parent type is private or package default this element will not be included in the api description so no tags are valid
		if (fRestrictedNode != null && fRestrictedNode != node){
			validateTagsWithRestrictedParent(node, tags, validtags);
		}
	}
		
	/**
	 * Creates problems for any tags on a node that has a parent type that is restricted
	 * (private or package default).  Only adds problems to tags if they are not already
	 * marked as invalid.
	 * 
	 * @param node the node to process 
	 * @param tags a list of tags on the node
	 * @param validTags list of tags that are valid for the node
	 */
	private void validateTagsWithRestrictedParent(ASTNode node, List tags, IApiJavadocTag[] validTags){
		switch(node.getNodeType()) {
		case ASTNode.TYPE_DECLARATION: {
			TypeDeclaration type = (TypeDeclaration) node;
			String context = type.isInterface() ? BuilderMessages.TagValidator_an_interface_that_is_not_visible : BuilderMessages.TagValidator_a_class_that_is_not_visible;
			processRestrictedParentTags(getTypeName(type), tags, IElementDescriptor.TYPE, context, validTags);
			break;
		}
		case ASTNode.ENUM_DECLARATION: {
			EnumDeclaration enumm = (EnumDeclaration) node;
			processRestrictedParentTags(getTypeName(enumm), tags, IElementDescriptor.TYPE, BuilderMessages.TagValidator_an_enum_that_is_not_visible, validTags);
			break;
		}
		case ASTNode.ENUM_CONSTANT_DECLARATION: {
			EnumConstantDeclaration decl = (EnumConstantDeclaration) node;
			processRestrictedParentTags(getTypeName(decl), tags, IElementDescriptor.FIELD, BuilderMessages.TagValidator_an_enum_constant_that_is_not_visible, validTags);
			break;
		}
		case ASTNode.ANNOTATION_TYPE_DECLARATION: {
			AnnotationTypeDeclaration annot = (AnnotationTypeDeclaration) node;
			processRestrictedParentTags(getTypeName(annot), tags, IElementDescriptor.TYPE, BuilderMessages.TagValidator_an_annotation_that_is_not_visible, validTags);
			break;
		}
		case ASTNode.METHOD_DECLARATION: {
			MethodDeclaration method = (MethodDeclaration) node;
			processRestrictedParentTags(getTypeName(method), tags, IElementDescriptor.METHOD, BuilderMessages.TagValidator_a_method_that_is_not_visible, validTags);
			break;
		}
		case ASTNode.ANNOTATION_TYPE_MEMBER_DECLARATION: {
			AnnotationTypeMemberDeclaration decl = (AnnotationTypeMemberDeclaration) node;
			processRestrictedParentTags(getTypeName(decl), tags, IElementDescriptor.METHOD, BuilderMessages.TagValidator_an_annotation_method_that_is_not_visible, validTags);
			break;
		}
		case ASTNode.FIELD_DECLARATION: {
			FieldDeclaration field = (FieldDeclaration) node;
			processRestrictedParentTags(getTypeName(field), tags, IElementDescriptor.FIELD, BuilderMessages.TagValidator_a_field_that_is_not_visible, validTags);
			break;
		}
		default:
			break;
		}
	}
	
	/**
	 * Returns the modifiers from the smallest enclosing type containing the given node
	 * @param node
	 * @return the modifiers for the smallest enclosing type or 0
	 */
	private int getParentModifiers(ASTNode node) {
		if(node == null) {
			return 0;
		}
		if(node instanceof AbstractTypeDeclaration) {
			AbstractTypeDeclaration type = (AbstractTypeDeclaration) node;
			return type.getModifiers();
		}
		return getParentModifiers(node.getParent());
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
		Set tagnames = ApiPlugin.getJavadocTagManager().getAllTagNames();
		HashSet invalidtags = new HashSet(alltags.length);
		for(int i = 0; i < alltags.length; i++) {
			invalidtags.add(alltags[i].getTagName());
		}
		for(int i = 0; i < validtags.length; i++) {
			invalidtags.remove(validtags[i].getTagName());
		}
		if(invalidtags.size() == 0) {
			return;
		}
		TagElement tag = null;
		HashSet tagz = new HashSet(tags.size());
		String tagname = null;
		for(Iterator iter = tags.iterator(); iter.hasNext();) {
			tag = (TagElement) iter.next();
			tagname = tag.getTagName();
			if(invalidtags.contains(tag.getTagName())) {
				processTagProblem(typeName, tag, element, IApiProblem.UNSUPPORTED_TAG_USE, IApiMarkerConstants.UNSUPPORTED_TAG_MARKER_ID, context);
			}
			if(tagnames.contains(tag.getTagName()) && !tagz.add(tagname)) {
				processTagProblem(typeName, tag, element, IApiProblem.DUPLICATE_TAG_USE, IApiMarkerConstants.DUPLICATE_TAG_MARKER_ID, null);
			}
		}
	}
	
	/**
	 * Creates problem markers for existing tags that are inside of a private or package default node.
	 * Will only create markers if the tag would otherwise be valid (duplicates and tags not in the given
	 * valid tag list are ignored).
	 * @param typeName the name of the type these tags belong to
	 * @param tags the list of tags to process
	 * @param element the element type
	 * @param context context string to append to the problem message
	 * @param validTags list of tags that would be valid for this element to avoid creating multiple markers on a tag
	 */
	private void processRestrictedParentTags(String typeName, List tags, int element, String context, IApiJavadocTag[] validTags) {
		if (validTags.length == 0){
			return;
		}
		HashSet validtags = new HashSet(validTags.length);
		for(int i = 0; i < validTags.length; i++) {
			validtags.add(validTags[i].getTagName());
		}
		
		HashSet duplicates = new HashSet();
		for (Iterator iterator = tags.iterator(); iterator.hasNext();) {
			String tag = ((TagElement) iterator.next()).getTagName();
			if (duplicates.contains(tag)){
				validtags.remove(tag);
			}
			duplicates.add(tag);
		}
		
		TagElement tag = null;
		for(Iterator iter = tags.iterator(); iter.hasNext();) {
			tag = (TagElement) iter.next();
			if(validtags.contains(tag.getTagName())) {
				processTagProblem(typeName, tag, element, IApiProblem.UNSUPPORTED_TAG_USE, IApiMarkerConstants.UNSUPPORTED_TAG_MARKER_ID, context);
			}
		}
	}
	
	/**
	 * Creates a new {@link IApiProblem} for the given tag and adds it to the cache
	 * @param tag
	 * @param element
	 * @param context
	 */
	private void processTagProblem(String typeName, TagElement tag, int element, int kind, int markerid, String context) {
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
					new Object[] {new Integer(markerid), fCompilationUnit.getHandleIdentifier()}, 
					linenumber,
					charstart,
					charend,
					IApiProblem.CATEGORY_USAGE,
					element,
					kind,
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
