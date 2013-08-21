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
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.Stack;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.Flags;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.AbstractTypeDeclaration;
import org.eclipse.jdt.core.dom.AnnotationTypeDeclaration;
import org.eclipse.jdt.core.dom.CompilationUnit;
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
 * <br><br>
 * The logic in this class must be kept in sync with how we determine what is visible in out
 * completion proposal code
 * 
 * @see org.eclipse.pde.api.tools.ui.internal.completion.APIToolsJavadocCompletionProposalComputer
 * 
 * @since 1.0.0
 */
public class TagValidator extends ASTVisitor {

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
	
	/**
	 * backing collection of tag problems, if any
	 */
	private ArrayList fTagProblems = null;
	
	private ICompilationUnit fCompilationUnit = null;
	boolean isvisible = true;
	Stack fStack/*<Item>*/ = new Stack();
	
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
	
	/* (non-Javadoc)
	 * @see org.eclipse.jdt.core.dom.ASTVisitor#visit(org.eclipse.jdt.core.dom.AnnotationTypeDeclaration)
	 */
	public boolean visit(AnnotationTypeDeclaration node) {
		isvisible &= !Flags.isPrivate(node.getModifiers());
		fStack.push(new Item(getTypeName(node), node.getModifiers(), isvisible));
		return true;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.jdt.core.dom.ASTVisitor#endVisit(org.eclipse.jdt.core.dom.AnnotationTypeDeclaration)
	 */
	public void endVisit(AnnotationTypeDeclaration node) {
		fStack.pop();
		if(!fStack.isEmpty()) {
			Item item = (Item) fStack.peek();
			isvisible = item.visible;
		}
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.jdt.core.dom.ASTVisitor#visit(org.eclipse.jdt.core.dom.TypeDeclaration)
	 */
	public boolean visit(TypeDeclaration node) {
		int flags = node.getModifiers();
		if(Flags.isPrivate(flags)) {
			isvisible &= false;
		} else {
			if(node.isMemberTypeDeclaration()) {
				isvisible &= (Flags.isPublic(flags) && Flags.isStatic(flags)) || 
						Flags.isPublic(flags) ||
						Flags.isProtected(flags) ||
						node.isInterface();
			}
			else {
				isvisible &= (!Flags.isPrivate(flags) && !Flags.isPackageDefault(flags)) || node.isInterface();
			}
		}
		fStack.push(new Item(getTypeName(node), node.getModifiers(), isvisible));
		return true;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.jdt.core.dom.ASTVisitor#endVisit(org.eclipse.jdt.core.dom.TypeDeclaration)
	 */
	public void endVisit(TypeDeclaration node) {
		fStack.pop();
		if(!fStack.isEmpty()) {
			Item item = (Item) fStack.peek();
			isvisible = item.visible;
		}
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.jdt.core.dom.ASTVisitor#visit(org.eclipse.jdt.core.dom.EnumDeclaration)
	 */
	public boolean visit(EnumDeclaration node) {
		int flags = node.getModifiers();
		if(node.isMemberTypeDeclaration()) {
			isvisible &= Flags.isPublic(flags);
		}
		else {
			isvisible &= !Flags.isPrivate(flags) && !Flags.isPackageDefault(flags);
		}
		fStack.push(new Item(getTypeName(node), node.getModifiers(), isvisible));
		return true;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.jdt.core.dom.ASTVisitor#endVisit(org.eclipse.jdt.core.dom.EnumDeclaration)
	 */
	public void endVisit(EnumDeclaration node){
		fStack.pop();
		if(!fStack.isEmpty()) {
			Item item = (Item) fStack.peek();
			isvisible = item.visible;
		}
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.jdt.core.dom.ASTVisitor#endVisit(org.eclipse.jdt.core.dom.CompilationUnit)
	 */
	public void endVisit(CompilationUnit node) {
		fStack.clear();
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
		switch(node.getNodeType()) {
			case ASTNode.TYPE_DECLARATION: {
				TypeDeclaration type = (TypeDeclaration) node;
				processTypeNode(type, tags);
				break;
			}
			case ASTNode.ENUM_DECLARATION: {
				Item item = (Item) fStack.peek();
				Set supported = getSupportedTagNames(IApiJavadocTag.TYPE_ENUM, IApiJavadocTag.MEMBER_NONE);
				for (Iterator i = tags.iterator(); i.hasNext();) {
					TagElement tag = (TagElement) i.next();
					String tagname = tag.getTagName();
					if(tagname == null || !JavadocTagManager.ALL_TAGS.contains(tagname)) {
						continue;
					}
					if(!supported.contains(tagname)) {
						processTagProblem(item.typename, 
								tag, 
								IElementDescriptor.TYPE, 
								IApiProblem.UNSUPPORTED_TAG_USE, 
								IApiMarkerConstants.UNSUPPORTED_TAG_MARKER_ID, 
								BuilderMessages.TagValidator_an_enum);
						continue;
					}
					if(!item.visible) {
						processTagProblem(item.typename, 
								tag, 
								IElementDescriptor.TYPE, 
								IApiProblem.UNSUPPORTED_TAG_USE, 
								IApiMarkerConstants.UNSUPPORTED_TAG_MARKER_ID, 
								BuilderMessages.TagValidator_enum_not_visible);
					}
				}
				break;
			}
			case ASTNode.ENUM_CONSTANT_DECLARATION: {
				Item item = (Item) fStack.peek();
				for (Iterator i = tags.iterator(); i.hasNext();) {
					TagElement tag = (TagElement) i.next();
					String tagname = tag.getTagName();
					if(tagname == null || !JavadocTagManager.ALL_TAGS.contains(tagname)) {
						continue;
					}
					processTagProblem(item.typename, 
							tag, 
							IElementDescriptor.FIELD, 
							IApiProblem.UNSUPPORTED_TAG_USE, 
							IApiMarkerConstants.UNSUPPORTED_TAG_MARKER_ID, 
							BuilderMessages.TagValidator_an_enum_constant);
				}
				break;
			}
			case ASTNode.ANNOTATION_TYPE_DECLARATION: {
				Item item = (Item) fStack.peek();
				Set supported = getSupportedTagNames(IApiJavadocTag.TYPE_ANNOTATION, IApiJavadocTag.MEMBER_NONE);
				for (Iterator i = tags.iterator(); i.hasNext();) {
					TagElement tag = (TagElement) i.next();
					String tagname = tag.getTagName();
					if(tagname == null || !JavadocTagManager.ALL_TAGS.contains(tagname)) {
						continue;
					}
					if(!supported.contains(tagname)) {
						processTagProblem(item.typename, 
								tag, 
								IElementDescriptor.TYPE, 
								IApiProblem.UNSUPPORTED_TAG_USE, 
								IApiMarkerConstants.UNSUPPORTED_TAG_MARKER_ID, 
								BuilderMessages.TagValidator_an_annotation);
						continue;
					}
					if(!item.visible) {
						processTagProblem(item.typename, 
								tag, 
								IElementDescriptor.TYPE, 
								IApiProblem.UNSUPPORTED_TAG_USE, 
								IApiMarkerConstants.UNSUPPORTED_TAG_MARKER_ID, 
								BuilderMessages.TagValidator_annotation_not_visible);
					}
				}
				break;
			}
			case ASTNode.METHOD_DECLARATION: {
				MethodDeclaration method = (MethodDeclaration) node;
				processMethodNode(method, tags);
				break;
			}
			case ASTNode.ANNOTATION_TYPE_MEMBER_DECLARATION: {
				Item item = (Item) fStack.peek();
				for (Iterator i = tags.iterator(); i.hasNext();) {
					TagElement tag = (TagElement) i.next();
					String tagname = tag.getTagName();
					if(tagname == null || !JavadocTagManager.ALL_TAGS.contains(tagname)) {
						continue;
					}
					processTagProblem(item.typename, 
							tag, 
							IElementDescriptor.METHOD, 
							IApiProblem.UNSUPPORTED_TAG_USE, 
							IApiMarkerConstants.UNSUPPORTED_TAG_MARKER_ID, 
							BuilderMessages.TagValidator_an_annotation_method);
				}
				break;
			}
			case ASTNode.FIELD_DECLARATION: {
				FieldDeclaration field = (FieldDeclaration) node;
				processFieldNode(field, tags);
				break;
			}
		default:
			break;
		}
	}
		
	/**
	 * Process the tags for the given {@link TypeDeclaration} node
	 * 
	 * @param type
	 * @param tags
	 * @since 1.0.400
	 */
	void processTypeNode(TypeDeclaration type, List tags) {
		HashSet processed = new HashSet();
		Item item = (Item) fStack.peek();
		for (Iterator i = tags.iterator(); i.hasNext();) {
			TagElement tag = (TagElement) i.next();
			String tagname = tag.getTagName();
			if(tagname == null || !JavadocTagManager.ALL_TAGS.contains(tagname)) {
				continue;
			}
			if(processed.contains(tagname)) {
				processTagProblem(item.typename, 
						tag, 
						IElementDescriptor.METHOD, 
						IApiProblem.DUPLICATE_TAG_USE, 
						IApiMarkerConstants.DUPLICATE_TAG_MARKER_ID, 
						null);
			}
			else {
				Set supportedtags = getSupportedTagNames(type.isInterface() ? IApiJavadocTag.TYPE_INTERFACE : IApiJavadocTag.TYPE_CLASS, IApiJavadocTag.MEMBER_NONE);
				if(!type.isInterface()) {
					int flags = type.getModifiers();
					if(!supportedtags.contains(tagname)) {
						processTagProblem(item.typename, 
								tag, 
								IElementDescriptor.TYPE, 
								IApiProblem.UNSUPPORTED_TAG_USE, 
								IApiMarkerConstants.UNSUPPORTED_TAG_MARKER_ID, 
								BuilderMessages.TagValidator_a_class);
					}
					else if(Flags.isPrivate(flags)) {
						processTagProblem(item.typename, 
								tag, 
								IElementDescriptor.TYPE, 
								IApiProblem.UNSUPPORTED_TAG_USE, 
								IApiMarkerConstants.UNSUPPORTED_TAG_MARKER_ID, 
								BuilderMessages.TagValidator_a_private_class);
					}
					else if(Flags.isPackageDefault(flags)) {
						processTagProblem(item.typename, 
								tag, 
								IElementDescriptor.TYPE, 
								IApiProblem.UNSUPPORTED_TAG_USE, 
								IApiMarkerConstants.UNSUPPORTED_TAG_MARKER_ID, 
								BuilderMessages.TagValidator_a_package_default_class);
					}
					else if(Flags.isAbstract(flags) && JavadocTagManager.TAG_NOINSTANTIATE.equals(tagname)) {
						processTagProblem(item.typename, 
								tag, 
								IElementDescriptor.TYPE, 
								IApiProblem.UNSUPPORTED_TAG_USE, 
								IApiMarkerConstants.UNSUPPORTED_TAG_MARKER_ID, 
								BuilderMessages.TagValidator_an_abstract_class);
					}
					else if(Flags.isFinal(flags) && JavadocTagManager.TAG_NOEXTEND.equals(tagname)) {
						processTagProblem(item.typename, 
								tag, 
								IElementDescriptor.TYPE, 
								IApiProblem.UNSUPPORTED_TAG_USE, 
								IApiMarkerConstants.UNSUPPORTED_TAG_MARKER_ID, 
								BuilderMessages.TagValidator_a_final_class);
					}
					else if(!item.visible) {
						processTagProblem(item.typename, 
								tag, 
								IElementDescriptor.TYPE, 
								IApiProblem.UNSUPPORTED_TAG_USE, 
								IApiMarkerConstants.UNSUPPORTED_TAG_MARKER_ID, 
								BuilderMessages.TagValidator_a_class_that_is_not_visible);
					}
				} else {
					if(!supportedtags.contains(tagname)) {
						processTagProblem(item.typename, 
								tag, 
								IElementDescriptor.TYPE, 
								IApiProblem.UNSUPPORTED_TAG_USE, 
								IApiMarkerConstants.UNSUPPORTED_TAG_MARKER_ID, 
								BuilderMessages.TagValidator_an_interface);
					}
					else if(!item.visible) {
						processTagProblem(item.typename, 
								tag, 
								IElementDescriptor.TYPE, 
								IApiProblem.UNSUPPORTED_TAG_USE, 
								IApiMarkerConstants.UNSUPPORTED_TAG_MARKER_ID, 
								BuilderMessages.TagValidator_an_interface_that_is_not_visible);
					}
				}
			}
			processed.add(tagname);
		}
	}
	
	/**
	 * Processes all of the tags for the given {@link FieldDeclaration}
	 * @param field
	 * @param tags
	 * @since 1.0.400
	 */
	void processFieldNode(FieldDeclaration field, List tags) {
		HashSet processed = new HashSet();
		Item item = (Item) fStack.peek();
		for (Iterator i = tags.iterator(); i.hasNext();) {
			TagElement tag = (TagElement) i.next();
			String tagname = tag.getTagName();
			if(tagname == null || !JavadocTagManager.ALL_TAGS.contains(tagname)) {
				continue;
			}
			if(processed.contains(tagname)) {
				processTagProblem(item.typename, 
						tag, 
						IElementDescriptor.METHOD, 
						IApiProblem.DUPLICATE_TAG_USE, 
						IApiMarkerConstants.DUPLICATE_TAG_MARKER_ID, 
						null);
			}
			else {
				int pkind = getParentKind(field);
				int flags = field.getModifiers();
				boolean isprivate = Flags.isPrivate(flags);
				boolean ispackage = Flags.isPackageDefault(flags);
				Set supportedtags = getSupportedTagNames(pkind, IApiJavadocTag.MEMBER_FIELD);
				switch(pkind) {
					case IApiJavadocTag.TYPE_ANNOTATION: {
						processTagProblem(item.typename, 
								tag, 
								IElementDescriptor.FIELD, 
								IApiProblem.UNSUPPORTED_TAG_USE, 
								IApiMarkerConstants.UNSUPPORTED_TAG_MARKER_ID, 
								BuilderMessages.TagValidator_annotation_field);
						break;
					}
					case IApiJavadocTag.TYPE_ENUM: {
						if(!supportedtags.contains(tagname)) {
							processTagProblem(item.typename, 
									tag, 
									IElementDescriptor.FIELD, 
									IApiProblem.UNSUPPORTED_TAG_USE, 
									IApiMarkerConstants.UNSUPPORTED_TAG_MARKER_ID, 
									BuilderMessages.TagValidator_enum_field);
						}
						else if(isprivate) {
							processTagProblem(item.typename, 
									tag, 
									IElementDescriptor.FIELD, 
									IApiProblem.UNSUPPORTED_TAG_USE, 
									IApiMarkerConstants.UNSUPPORTED_TAG_MARKER_ID, 
									BuilderMessages.TagValidator_private_enum_field);
						}
						else if(!item.visible) {
							processTagProblem(item.typename, 
									tag, 
									IElementDescriptor.FIELD, 
									IApiProblem.UNSUPPORTED_TAG_USE, 
									IApiMarkerConstants.UNSUPPORTED_TAG_MARKER_ID, 
									BuilderMessages.TagValidator_not_visible_enum_field);
						}
						break;
					}
					case IApiJavadocTag.TYPE_INTERFACE: {
						if(!supportedtags.contains(tagname)) {
							processTagProblem(item.typename, 
									tag, 
									IElementDescriptor.FIELD, 
									IApiProblem.UNSUPPORTED_TAG_USE, 
									IApiMarkerConstants.UNSUPPORTED_TAG_MARKER_ID, 
									BuilderMessages.TagValidator_an_interface_field);
						}
						else if(!item.visible) {
							processTagProblem(item.typename, 
									tag, 
									IElementDescriptor.FIELD, 
									IApiProblem.UNSUPPORTED_TAG_USE, 
									IApiMarkerConstants.UNSUPPORTED_TAG_MARKER_ID, 
									BuilderMessages.TagValidator_not_visible_interface_field);
						}
						break;
					}
					case IApiJavadocTag.TYPE_CLASS: {
						if(!supportedtags.contains(tagname)) {
							processTagProblem(item.typename, 
									tag, 
									IElementDescriptor.FIELD, 
									IApiProblem.UNSUPPORTED_TAG_USE, 
									IApiMarkerConstants.UNSUPPORTED_TAG_MARKER_ID, 
									BuilderMessages.TagValidator_a_field);
						}
						else if(isprivate) {
							processTagProblem(item.typename, 
									tag, 
									IElementDescriptor.FIELD, 
									IApiProblem.UNSUPPORTED_TAG_USE, 
									IApiMarkerConstants.UNSUPPORTED_TAG_MARKER_ID, 
									BuilderMessages.TagValidator_private_field);
						}
						else if(ispackage) {
							processTagProblem(item.typename, 
									tag, 
									IElementDescriptor.FIELD, 
									IApiProblem.UNSUPPORTED_TAG_USE, 
									IApiMarkerConstants.UNSUPPORTED_TAG_MARKER_ID, 
									BuilderMessages.TagValidator_a_package_default_field);
						}
						else if(!item.visible) {
							processTagProblem(item.typename, 
									tag, 
									IElementDescriptor.FIELD, 
									IApiProblem.UNSUPPORTED_TAG_USE,
									IApiMarkerConstants.UNSUPPORTED_TAG_MARKER_ID, 
									BuilderMessages.TagValidator_a_field_that_is_not_visible);
						}
						break;
					}
					default: {
						break;
					}
				}
			}
			processed.add(tagname);
		}
	}
	
	/**
	 * Processes all of the tags for the given {@link MethodDeclaration}
	 * @param method
	 * @param tags
	 * @since 1.0.400
	 */
	void processMethodNode(MethodDeclaration method, List tags) {
		int pkind = getParentKind(method);
		int mods = method.getModifiers();
		boolean isconstructor = method.isConstructor();
		boolean isstatic = Flags.isStatic(mods);
		Item item = (Item) fStack.peek();
		Set supportedtags = getSupportedTagNames(pkind, isconstructor ? IApiJavadocTag.MEMBER_CONSTRUCTOR : IApiJavadocTag.MEMBER_METHOD);
		HashSet processed = new HashSet();
		for (Iterator i = tags.iterator(); i.hasNext();) {
			TagElement tag = (TagElement) i.next();
			String tagname = tag.getTagName();
			if(tagname == null || !JavadocTagManager.ALL_TAGS.contains(tagname)) {
				continue;
			}
			if(processed.contains(tagname)) {
				processTagProblem(item.typename, 
						tag, 
						IElementDescriptor.METHOD, 
						IApiProblem.DUPLICATE_TAG_USE, 
						IApiMarkerConstants.DUPLICATE_TAG_MARKER_ID, 
						null);
			}
			else {
				switch(pkind) {
				case IApiJavadocTag.TYPE_ENUM: {
					if(!supportedtags.contains(tagname)) {
						processTagProblem(item.typename, 
								tag, 
								IElementDescriptor.METHOD, 
								IApiProblem.UNSUPPORTED_TAG_USE, 
								IApiMarkerConstants.UNSUPPORTED_TAG_MARKER_ID, 
								BuilderMessages.TagValidator_an_enum_method);
					}
					else if(Flags.isPrivate(mods)) {
						processTagProblem(item.typename, 
								tag, 
								IElementDescriptor.METHOD, 
								IApiProblem.UNSUPPORTED_TAG_USE, 
								IApiMarkerConstants.UNSUPPORTED_TAG_MARKER_ID, 
								BuilderMessages.TagValidator_private_enum_method);
					}
					else if(Flags.isPackageDefault(mods)) {
						processTagProblem(item.typename, 
								tag, 
								IElementDescriptor.METHOD, 
								IApiProblem.UNSUPPORTED_TAG_USE, 
								IApiMarkerConstants.UNSUPPORTED_TAG_MARKER_ID, 
								BuilderMessages.TagValidator_a_package_default_enum);
					}
					else if(!item.visible) {
						processTagProblem(item.typename, 
								tag, 
								IElementDescriptor.METHOD, 
								IApiProblem.UNSUPPORTED_TAG_USE, 
								IApiMarkerConstants.UNSUPPORTED_TAG_MARKER_ID, 
								BuilderMessages.TagValidator_not_visible_enum_method);
					}
					break;
				}
				case IApiJavadocTag.TYPE_INTERFACE: {
					if(!supportedtags.contains(tagname)) {
						processTagProblem(item.typename, 
								tag, 
								IElementDescriptor.METHOD, 
								IApiProblem.UNSUPPORTED_TAG_USE, 
								IApiMarkerConstants.UNSUPPORTED_TAG_MARKER_ID, 
								BuilderMessages.TagValidator_an_interface_method);
					}
					else if(!item.visible) {
						processTagProblem(item.typename, 
								tag, 
								IElementDescriptor.METHOD, 
								IApiProblem.UNSUPPORTED_TAG_USE, 
								IApiMarkerConstants.UNSUPPORTED_TAG_MARKER_ID, 
								BuilderMessages.TagValidator_not_visible_interface_method);
					}
					break;
				}
				case IApiJavadocTag.TYPE_CLASS: {
					if(!supportedtags.contains(tagname)) {
						processTagProblem(item.typename, 
								tag, 
								IElementDescriptor.METHOD, 
								IApiProblem.UNSUPPORTED_TAG_USE, 
								IApiMarkerConstants.UNSUPPORTED_TAG_MARKER_ID, 
								isconstructor ? BuilderMessages.TagValidator_a_constructor : BuilderMessages.TagValidator_a_method);
					}
					else if(Flags.isPrivate(mods)) {
						processTagProblem(item.typename, 
								tag, 
								IElementDescriptor.METHOD, 
								IApiProblem.UNSUPPORTED_TAG_USE, 
								IApiMarkerConstants.UNSUPPORTED_TAG_MARKER_ID, 
								isconstructor ? BuilderMessages.TagValidator_private_constructor : BuilderMessages.TagValidator_private_method);
					}
					else if(Flags.isPackageDefault(mods)) {
						if(isstatic) {
							processTagProblem(item.typename, 
									tag, 
									IElementDescriptor.METHOD, 
									IApiProblem.UNSUPPORTED_TAG_USE, 
									IApiMarkerConstants.UNSUPPORTED_TAG_MARKER_ID, 
									isconstructor ? BuilderMessages.TagValidator_static_package_constructor : BuilderMessages.TagValidator_a_static_package_default_method);
						}
						else {
							processTagProblem(item.typename, 
									tag, 
									IElementDescriptor.METHOD, 
									IApiProblem.UNSUPPORTED_TAG_USE, 
									IApiMarkerConstants.UNSUPPORTED_TAG_MARKER_ID, 
									isconstructor ? BuilderMessages.TagValidator_a_package_default_constructor : BuilderMessages.TagValidator_a_package_default_method);
						}
					}
					else if(JavadocTagManager.TAG_NOOVERRIDE.equals(tagname)) {
						if (Flags.isFinal(mods)) {
							if(isstatic) {
								processTagProblem(item.typename, 
										tag, 
										IElementDescriptor.METHOD, 
										IApiProblem.UNSUPPORTED_TAG_USE, 
										IApiMarkerConstants.UNSUPPORTED_TAG_MARKER_ID, 
										isconstructor ? BuilderMessages.TagValidator_static_final_constructor : BuilderMessages.TagValidator_a_static_final_method);
							}
							else {
								processTagProblem(item.typename, 
										tag, 
										IElementDescriptor.METHOD, 
										IApiProblem.UNSUPPORTED_TAG_USE, 
										IApiMarkerConstants.UNSUPPORTED_TAG_MARKER_ID, 
										isconstructor ? BuilderMessages.TagValidator_final_constructor : BuilderMessages.TagValidator_a_final_method);
							}
						}
						else if(isstatic) {
							processTagProblem(item.typename, 
									tag, 
									IElementDescriptor.METHOD, 
									IApiProblem.UNSUPPORTED_TAG_USE, 
									IApiMarkerConstants.UNSUPPORTED_TAG_MARKER_ID, 
									isconstructor ? BuilderMessages.TagValidator_a_static_constructor : BuilderMessages.TagValidator_a_static_method);
						}
						else if(Flags.isFinal(getParentModifiers(method))) {
							processTagProblem(item.typename, 
									tag, 
									IElementDescriptor.METHOD, 
									IApiProblem.UNSUPPORTED_TAG_USE, 
									IApiMarkerConstants.UNSUPPORTED_TAG_MARKER_ID, 
									isconstructor ? BuilderMessages.TagValidator_constructor_in_final_class : BuilderMessages.TagValidator_a_method_in_a_final_class);
						}
						else if(!item.visible) {
							processTagProblem(item.typename, 
									tag, 
									IElementDescriptor.METHOD, 
									IApiProblem.UNSUPPORTED_TAG_USE, 
									IApiMarkerConstants.UNSUPPORTED_TAG_MARKER_ID, 
									isconstructor ? BuilderMessages.TagValidator_not_visible_constructor : BuilderMessages.TagValidator_a_method_that_is_not_visible);
						}
					}
					else if(!item.visible) {
						processTagProblem(item.typename, 
								tag, 
								IElementDescriptor.METHOD, 
								IApiProblem.UNSUPPORTED_TAG_USE, 
								IApiMarkerConstants.UNSUPPORTED_TAG_MARKER_ID, 
								isconstructor ? BuilderMessages.TagValidator_not_visible_constructor : BuilderMessages.TagValidator_a_method_that_is_not_visible);
					}
					break;
				}
				default: {
					break;
				}
			}
			}
			processed.add(tagname);
		}
	}
	
	/**
	 * Returns the complete listing of supported tags for the given type and member
	 * @param type
	 * @param member
	 * @return the list of supported tag names or the {@link Collections#EMPTY_SET}, never <code>null</code>
	 * @since 1.0.400
	 */
	Set getSupportedTagNames(int type, int member) {
		IApiJavadocTag[] tags = ApiPlugin.getJavadocTagManager().getTagsForType(type, member);
		if(tags.length > 0) {
			HashSet valid = new HashSet(tags.length, 1);
			for (int i = 0; i < tags.length; i++) {
				valid.add(tags[i].getTagName());
			}
			return valid;
		}
		return Collections.EMPTY_SET;
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
