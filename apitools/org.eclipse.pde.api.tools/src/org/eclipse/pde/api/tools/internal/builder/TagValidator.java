/*******************************************************************************
 * Copyright (c) 2008, 2016 IBM Corporation and others.
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

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.Flags;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.AnnotationTypeDeclaration;
import org.eclipse.jdt.core.dom.EnumDeclaration;
import org.eclipse.jdt.core.dom.FieldDeclaration;
import org.eclipse.jdt.core.dom.Javadoc;
import org.eclipse.jdt.core.dom.MarkerAnnotation;
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
 * Visit Javadoc comments of types and member to find API javadoc tags that are
 * being misused <br>
 * <br>
 * The logic in this class must be kept in sync with how we determine what is
 * visible in out completion proposal code
 *
 * @see org.eclipse.pde.api.tools.ui.internal.completion.APIToolsJavadocCompletionProposalComputer
 *
 * @since 1.0.0
 */
public class TagValidator extends Validator {

	/**
	 * If Javadoc tags should be scanned
	 */
	boolean fScanTags = true;
	/**
	 * If annotations should be scanned
	 */
	boolean fScanAnnotations = true;

	/**
	 * A collection of annotation names so we can detect multiple usage
	 */
	private Set<String> fProcessedAnnotations = new HashSet<>();

	/**
	 * Constructor
	 *
	 * @param parent
	 * @param tags
	 * @param annotations
	 */
	public TagValidator(ICompilationUnit parent, boolean tags, boolean annotations) {
		fCompilationUnit = parent;
		fScanTags = tags;
		fScanAnnotations = annotations;
	}

	@Override
	public boolean visit(TypeDeclaration node) {
		fProcessedAnnotations.clear();
		return super.visit(node);
	}

	@Override
	public boolean visit(AnnotationTypeDeclaration node) {
		fProcessedAnnotations.clear();
		return super.visit(node);
	}

	@Override
	public boolean visit(EnumDeclaration node) {
		fProcessedAnnotations.clear();
		return super.visit(node);
	}

	@Override
	public boolean visit(FieldDeclaration node) {
		fProcessedAnnotations.clear();
		return super.visit(node);
	}

	@Override
	public boolean visit(MethodDeclaration node) {
		fProcessedAnnotations.clear();
		return super.visit(node);
	}

	@Override
	public boolean visit(Javadoc node) {
		if (!fScanTags) {
			return false;
		}
		ASTNode parent = node.getParent();
		if (parent != null) {
			List<TagElement> tags = node.tags();
			validateTags(parent, tags);
		}
		return false;
	}

	/**
	 * Validates the set of tags for the given parent node and the given listing
	 * of {@link TagElement}s
	 *
	 * @param node
	 * @param tags
	 */
	private void validateTags(ASTNode node, List<TagElement> tags) {
		if (tags.isEmpty()) {
			return;
		}
		switch (node.getNodeType()) {
			case ASTNode.TYPE_DECLARATION: {
				TypeDeclaration type = (TypeDeclaration) node;
				processTypeNode(type, tags);
				break;
			}
			case ASTNode.ENUM_DECLARATION: {
				Item item = getItem();
				Set<String> supported = getSupportedTagNames(IApiJavadocTag.TYPE_ENUM, IApiJavadocTag.MEMBER_NONE);
				HashSet<String> processed = new HashSet<>();
				for (TagElement tag : tags) {
					String tagname = tag.getTagName();
					if (tagname == null || !JavadocTagManager.ALL_TAGS.contains(tagname)) {
						continue;
					}
					if (processed.contains(tagname)) {
						createTagProblem(item.typename, tag, IElementDescriptor.METHOD, IApiProblem.DUPLICATE_TAG_USE, IApiMarkerConstants.DUPLICATE_TAG_MARKER_ID, null);
					} else if (!supported.contains(tagname)) {
						createTagProblem(item.typename, tag, IElementDescriptor.TYPE, IApiProblem.UNSUPPORTED_TAG_USE, IApiMarkerConstants.UNSUPPORTED_TAG_MARKER_ID, BuilderMessages.TagValidator_an_enum);
					} else if (!item.visible) {
						createTagProblem(item.typename, tag, IElementDescriptor.TYPE, IApiProblem.UNSUPPORTED_TAG_USE, IApiMarkerConstants.UNSUPPORTED_TAG_MARKER_ID, BuilderMessages.TagValidator_enum_not_visible);
					}
					processed.add(tagname);
				}
				break;
			}
			case ASTNode.ENUM_CONSTANT_DECLARATION: {
				Item item = getItem();
				for (TagElement tag : tags) {
					String tagname = tag.getTagName();
					if (tagname == null || !JavadocTagManager.ALL_TAGS.contains(tagname)) {
						continue;
					}
					createTagProblem(item.typename, tag, IElementDescriptor.FIELD, IApiProblem.UNSUPPORTED_TAG_USE, IApiMarkerConstants.UNSUPPORTED_TAG_MARKER_ID, BuilderMessages.TagValidator_an_enum_constant);
				}
				break;
			}
			case ASTNode.ANNOTATION_TYPE_DECLARATION: {
				Item item = getItem();
				Set<String> supported = getSupportedTagNames(IApiJavadocTag.TYPE_ANNOTATION, IApiJavadocTag.MEMBER_NONE);
				HashSet<String> processed = new HashSet<>();
				for (TagElement tag : tags) {
					String tagname = tag.getTagName();
					if (tagname == null || !JavadocTagManager.ALL_TAGS.contains(tagname)) {
						continue;
					}
					if (processed.contains(tagname)) {
						createTagProblem(item.typename, tag, IElementDescriptor.METHOD, IApiProblem.DUPLICATE_TAG_USE, IApiMarkerConstants.DUPLICATE_TAG_MARKER_ID, null);
					} else if (!supported.contains(tagname)) {
						createTagProblem(item.typename, tag, IElementDescriptor.TYPE, IApiProblem.UNSUPPORTED_TAG_USE, IApiMarkerConstants.UNSUPPORTED_TAG_MARKER_ID, BuilderMessages.TagValidator_an_annotation);
					} else if (!item.visible) {
						createTagProblem(item.typename, tag, IElementDescriptor.TYPE, IApiProblem.UNSUPPORTED_TAG_USE, IApiMarkerConstants.UNSUPPORTED_TAG_MARKER_ID, BuilderMessages.TagValidator_annotation_not_visible);
					}
					processed.add(tagname);
				}
				break;
			}
			case ASTNode.METHOD_DECLARATION: {
				MethodDeclaration method = (MethodDeclaration) node;
				processMethodNode(method, tags);
				break;
			}
			case ASTNode.ANNOTATION_TYPE_MEMBER_DECLARATION: {
				Item item = getItem();
				for (TagElement tag : tags) {
					String tagname = tag.getTagName();
					if (tagname == null || !JavadocTagManager.ALL_TAGS.contains(tagname)) {
						continue;
					}
					createTagProblem(item.typename, tag, IElementDescriptor.METHOD, IApiProblem.UNSUPPORTED_TAG_USE, IApiMarkerConstants.UNSUPPORTED_TAG_MARKER_ID, BuilderMessages.TagValidator_an_annotation_method);
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
	void processTypeNode(TypeDeclaration type, List<TagElement> tags) {
		HashSet<String> processed = new HashSet<>();
		Item item = getItem();
		for (TagElement tag : tags) {
			String tagname = tag.getTagName();
			if (tagname == null || !JavadocTagManager.ALL_TAGS.contains(tagname)) {
				continue;
			}
			if (processed.contains(tagname)) {
				createTagProblem(item.typename, tag, IElementDescriptor.METHOD, IApiProblem.DUPLICATE_TAG_USE, IApiMarkerConstants.DUPLICATE_TAG_MARKER_ID, null);
			} else {
				Set<String> supportedtags = getSupportedTagNames(type.isInterface() ? IApiJavadocTag.TYPE_INTERFACE : IApiJavadocTag.TYPE_CLASS, IApiJavadocTag.MEMBER_NONE);
				if (!type.isInterface()) {
					int flags = type.getModifiers();
					if (!supportedtags.contains(tagname)) {
						createTagProblem(item.typename, tag, IElementDescriptor.TYPE, IApiProblem.UNSUPPORTED_TAG_USE, IApiMarkerConstants.UNSUPPORTED_TAG_MARKER_ID, BuilderMessages.TagValidator_a_class);
					} else if (Flags.isPrivate(flags)) {
						createTagProblem(item.typename, tag, IElementDescriptor.TYPE, IApiProblem.UNSUPPORTED_TAG_USE, IApiMarkerConstants.UNSUPPORTED_TAG_MARKER_ID, BuilderMessages.TagValidator_a_private_class);
					} else if (Flags.isPackageDefault(flags)) {
						createTagProblem(item.typename, tag, IElementDescriptor.TYPE, IApiProblem.UNSUPPORTED_TAG_USE, IApiMarkerConstants.UNSUPPORTED_TAG_MARKER_ID, BuilderMessages.TagValidator_a_package_default_class);
					} else if (Flags.isAbstract(flags) && JavadocTagManager.TAG_NOINSTANTIATE.equals(tagname)) {
						createTagProblem(item.typename, tag, IElementDescriptor.TYPE, IApiProblem.UNSUPPORTED_TAG_USE, IApiMarkerConstants.UNSUPPORTED_TAG_MARKER_ID, BuilderMessages.TagValidator_an_abstract_class);
					} else if (Flags.isFinal(flags) && JavadocTagManager.TAG_NOEXTEND.equals(tagname)) {
						createTagProblem(item.typename, tag, IElementDescriptor.TYPE, IApiProblem.UNSUPPORTED_TAG_USE, IApiMarkerConstants.UNSUPPORTED_TAG_MARKER_ID, BuilderMessages.TagValidator_a_final_class);
					} else if (!item.visible) {
						createTagProblem(item.typename, tag, IElementDescriptor.TYPE, IApiProblem.UNSUPPORTED_TAG_USE, IApiMarkerConstants.UNSUPPORTED_TAG_MARKER_ID, BuilderMessages.TagValidator_a_class_that_is_not_visible);
					}
				} else {
					if (!supportedtags.contains(tagname)) {
						createTagProblem(item.typename, tag, IElementDescriptor.TYPE, IApiProblem.UNSUPPORTED_TAG_USE, IApiMarkerConstants.UNSUPPORTED_TAG_MARKER_ID, BuilderMessages.TagValidator_an_interface);
					} else if (!item.visible) {
						createTagProblem(item.typename, tag, IElementDescriptor.TYPE, IApiProblem.UNSUPPORTED_TAG_USE, IApiMarkerConstants.UNSUPPORTED_TAG_MARKER_ID, BuilderMessages.TagValidator_an_interface_that_is_not_visible);
					}
				}
			}
			processed.add(tagname);
		}
	}

	/**
	 * Processes all of the tags for the given {@link FieldDeclaration}
	 *
	 * @param field
	 * @param tags
	 * @since 1.0.400
	 */
	void processFieldNode(FieldDeclaration field, List<TagElement> tags) {
		HashSet<String> processed = new HashSet<>();
		Item item = getItem();
		for (TagElement tag : tags) {
			String tagname = tag.getTagName();
			if (tagname == null || !JavadocTagManager.ALL_TAGS.contains(tagname)) {
				continue;
			}
			if (processed.contains(tagname)) {
				createTagProblem(item.typename, tag, IElementDescriptor.FIELD, IApiProblem.DUPLICATE_TAG_USE, IApiMarkerConstants.DUPLICATE_TAG_MARKER_ID, null);
			} else {
				int pkind = getParentKind(field);
				int flags = field.getModifiers();
				boolean isprivate = Flags.isPrivate(flags);
				boolean ispackage = Flags.isPackageDefault(flags);
				Set<String> supportedtags = getSupportedTagNames(pkind, IApiJavadocTag.MEMBER_FIELD);
				switch (pkind) {
					case IApiJavadocTag.TYPE_ANNOTATION: {
						createTagProblem(item.typename, tag, IElementDescriptor.FIELD, IApiProblem.UNSUPPORTED_TAG_USE, IApiMarkerConstants.UNSUPPORTED_TAG_MARKER_ID, BuilderMessages.TagValidator_annotation_field);
						break;
					}
					case IApiJavadocTag.TYPE_ENUM: {
						if (!supportedtags.contains(tagname)) {
							createTagProblem(item.typename, tag, IElementDescriptor.FIELD, IApiProblem.UNSUPPORTED_TAG_USE, IApiMarkerConstants.UNSUPPORTED_TAG_MARKER_ID, BuilderMessages.TagValidator_enum_field);
						} else if (isprivate) {
							createTagProblem(item.typename, tag, IElementDescriptor.FIELD, IApiProblem.UNSUPPORTED_TAG_USE, IApiMarkerConstants.UNSUPPORTED_TAG_MARKER_ID, BuilderMessages.TagValidator_private_enum_field);
						} else if (!item.visible) {
							createTagProblem(item.typename, tag, IElementDescriptor.FIELD, IApiProblem.UNSUPPORTED_TAG_USE, IApiMarkerConstants.UNSUPPORTED_TAG_MARKER_ID, BuilderMessages.TagValidator_not_visible_enum_field);
						}
						break;
					}
					case IApiJavadocTag.TYPE_INTERFACE: {
						if (!supportedtags.contains(tagname)) {
							createTagProblem(item.typename, tag, IElementDescriptor.FIELD, IApiProblem.UNSUPPORTED_TAG_USE, IApiMarkerConstants.UNSUPPORTED_TAG_MARKER_ID, BuilderMessages.TagValidator_an_interface_field);
						} else if (!item.visible) {
							createTagProblem(item.typename, tag, IElementDescriptor.FIELD, IApiProblem.UNSUPPORTED_TAG_USE, IApiMarkerConstants.UNSUPPORTED_TAG_MARKER_ID, BuilderMessages.TagValidator_not_visible_interface_field);
						}
						break;
					}
					case IApiJavadocTag.TYPE_CLASS: {
						if (!supportedtags.contains(tagname)) {
							createTagProblem(item.typename, tag, IElementDescriptor.FIELD, IApiProblem.UNSUPPORTED_TAG_USE, IApiMarkerConstants.UNSUPPORTED_TAG_MARKER_ID, BuilderMessages.TagValidator_a_field);
						} else if (isprivate) {
							createTagProblem(item.typename, tag, IElementDescriptor.FIELD, IApiProblem.UNSUPPORTED_TAG_USE, IApiMarkerConstants.UNSUPPORTED_TAG_MARKER_ID, BuilderMessages.TagValidator_private_field);
						} else if (ispackage) {
							createTagProblem(item.typename, tag, IElementDescriptor.FIELD, IApiProblem.UNSUPPORTED_TAG_USE, IApiMarkerConstants.UNSUPPORTED_TAG_MARKER_ID, BuilderMessages.TagValidator_a_package_default_field);
						} else if (!item.visible) {
							createTagProblem(item.typename, tag, IElementDescriptor.FIELD, IApiProblem.UNSUPPORTED_TAG_USE, IApiMarkerConstants.UNSUPPORTED_TAG_MARKER_ID, BuilderMessages.TagValidator_a_field_that_is_not_visible);
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
	 *
	 * @param method
	 * @param tags
	 * @since 1.0.400
	 */
	void processMethodNode(MethodDeclaration method, List<TagElement> tags) {
		int pkind = getParentKind(method);
		int mods = method.getModifiers();
		boolean isconstructor = method.isConstructor();
		boolean isstatic = Flags.isStatic(mods);
		Item item = getItem();
		Set<String> supportedtags = getSupportedTagNames(pkind, isconstructor ? IApiJavadocTag.MEMBER_CONSTRUCTOR : IApiJavadocTag.MEMBER_METHOD);
		HashSet<String> processed = new HashSet<>();
		for (TagElement tag : tags) {
			String tagname = tag.getTagName();
			if (tagname == null || !JavadocTagManager.ALL_TAGS.contains(tagname)) {
				continue;
			}
			if (processed.contains(tagname)) {
				createTagProblem(item.typename, tag, IElementDescriptor.METHOD, IApiProblem.DUPLICATE_TAG_USE, IApiMarkerConstants.DUPLICATE_TAG_MARKER_ID, null);
			} else {
				switch (pkind) {
					case IApiJavadocTag.TYPE_ENUM: {
						if (!supportedtags.contains(tagname)) {
							createTagProblem(item.typename, tag, IElementDescriptor.METHOD, IApiProblem.UNSUPPORTED_TAG_USE, IApiMarkerConstants.UNSUPPORTED_TAG_MARKER_ID, BuilderMessages.TagValidator_an_enum_method);
						} else if (Flags.isPrivate(mods)) {
							createTagProblem(item.typename, tag, IElementDescriptor.METHOD, IApiProblem.UNSUPPORTED_TAG_USE, IApiMarkerConstants.UNSUPPORTED_TAG_MARKER_ID, BuilderMessages.TagValidator_private_enum_method);
						} else if (Flags.isPackageDefault(mods)) {
							createTagProblem(item.typename, tag, IElementDescriptor.METHOD, IApiProblem.UNSUPPORTED_TAG_USE, IApiMarkerConstants.UNSUPPORTED_TAG_MARKER_ID, BuilderMessages.TagValidator_a_package_default_enum);
						} else if (!item.visible) {
							createTagProblem(item.typename, tag, IElementDescriptor.METHOD, IApiProblem.UNSUPPORTED_TAG_USE, IApiMarkerConstants.UNSUPPORTED_TAG_MARKER_ID, BuilderMessages.TagValidator_not_visible_enum_method);
						}
						break;
					}
					case IApiJavadocTag.TYPE_INTERFACE: {
						if (!supportedtags.contains(tagname)) {
							createTagProblem(item.typename, tag, IElementDescriptor.METHOD, IApiProblem.UNSUPPORTED_TAG_USE, IApiMarkerConstants.UNSUPPORTED_TAG_MARKER_ID, BuilderMessages.TagValidator_an_interface_method);
						} else if (!item.visible) {
							createTagProblem(item.typename, tag, IElementDescriptor.METHOD, IApiProblem.UNSUPPORTED_TAG_USE, IApiMarkerConstants.UNSUPPORTED_TAG_MARKER_ID, BuilderMessages.TagValidator_not_visible_interface_method);
						} else if (!Flags.isDefaultMethod(mods) && JavadocTagManager.TAG_NOOVERRIDE.equals(tagname)) {
							createTagProblem(item.typename, tag, IElementDescriptor.METHOD, IApiProblem.UNSUPPORTED_TAG_USE, IApiMarkerConstants.UNSUPPORTED_TAG_MARKER_ID, BuilderMessages.TagValidator_nondefault_interface_method);
						}
						break;
					}
					case IApiJavadocTag.TYPE_CLASS: {
						if (!supportedtags.contains(tagname)) {
							createTagProblem(item.typename, tag, IElementDescriptor.METHOD, IApiProblem.UNSUPPORTED_TAG_USE, IApiMarkerConstants.UNSUPPORTED_TAG_MARKER_ID, isconstructor ? BuilderMessages.TagValidator_a_constructor : BuilderMessages.TagValidator_a_method);
						} else if (Flags.isPrivate(mods)) {
							createTagProblem(item.typename, tag, IElementDescriptor.METHOD, IApiProblem.UNSUPPORTED_TAG_USE, IApiMarkerConstants.UNSUPPORTED_TAG_MARKER_ID, isconstructor ? BuilderMessages.TagValidator_private_constructor : BuilderMessages.TagValidator_private_method);
						} else if (Flags.isPackageDefault(mods)) {
							if (isstatic) {
								createTagProblem(item.typename, tag, IElementDescriptor.METHOD, IApiProblem.UNSUPPORTED_TAG_USE, IApiMarkerConstants.UNSUPPORTED_TAG_MARKER_ID, isconstructor ? BuilderMessages.TagValidator_static_package_constructor : BuilderMessages.TagValidator_a_static_package_default_method);
							} else {
								createTagProblem(item.typename, tag, IElementDescriptor.METHOD, IApiProblem.UNSUPPORTED_TAG_USE, IApiMarkerConstants.UNSUPPORTED_TAG_MARKER_ID, isconstructor ? BuilderMessages.TagValidator_a_package_default_constructor : BuilderMessages.TagValidator_a_package_default_method);
							}
						} else if (JavadocTagManager.TAG_NOOVERRIDE.equals(tagname)) {
							if (Flags.isFinal(mods)) {
								if (isstatic) {
									createTagProblem(item.typename, tag, IElementDescriptor.METHOD, IApiProblem.UNSUPPORTED_TAG_USE, IApiMarkerConstants.UNSUPPORTED_TAG_MARKER_ID, isconstructor ? BuilderMessages.TagValidator_static_final_constructor : BuilderMessages.TagValidator_a_static_final_method);
								} else {
									createTagProblem(item.typename, tag, IElementDescriptor.METHOD, IApiProblem.UNSUPPORTED_TAG_USE, IApiMarkerConstants.UNSUPPORTED_TAG_MARKER_ID, isconstructor ? BuilderMessages.TagValidator_final_constructor : BuilderMessages.TagValidator_a_final_method);
								}
							} else if (isstatic) {
								createTagProblem(item.typename, tag, IElementDescriptor.METHOD, IApiProblem.UNSUPPORTED_TAG_USE, IApiMarkerConstants.UNSUPPORTED_TAG_MARKER_ID, isconstructor ? BuilderMessages.TagValidator_a_static_constructor : BuilderMessages.TagValidator_a_static_method);
							} else if (Flags.isFinal(getParentModifiers(method))) {
								createTagProblem(item.typename, tag, IElementDescriptor.METHOD, IApiProblem.UNSUPPORTED_TAG_USE, IApiMarkerConstants.UNSUPPORTED_TAG_MARKER_ID, isconstructor ? BuilderMessages.TagValidator_constructor_in_final_class : BuilderMessages.TagValidator_a_method_in_a_final_class);
							} else if (!item.visible) {
								createTagProblem(item.typename, tag, IElementDescriptor.METHOD, IApiProblem.UNSUPPORTED_TAG_USE, IApiMarkerConstants.UNSUPPORTED_TAG_MARKER_ID, isconstructor ? BuilderMessages.TagValidator_not_visible_constructor : BuilderMessages.TagValidator_a_method_that_is_not_visible);
							}
						} else if (!item.visible) {
							createTagProblem(item.typename, tag, IElementDescriptor.METHOD, IApiProblem.UNSUPPORTED_TAG_USE, IApiMarkerConstants.UNSUPPORTED_TAG_MARKER_ID, isconstructor ? BuilderMessages.TagValidator_not_visible_constructor : BuilderMessages.TagValidator_a_method_that_is_not_visible);
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
	 * Returns the complete listing of supported tags for the given type and
	 * member
	 *
	 * @param type
	 * @param member
	 * @return the list of supported tag names or the
	 *         {@link Collections#EMPTY_SET}, never <code>null</code>
	 * @since 1.0.400
	 */
	Set<String> getSupportedTagNames(int type, int member) {
		IApiJavadocTag[] tags = ApiPlugin.getJavadocTagManager().getTagsForType(type, member);
		if (tags.length > 0) {
			HashSet<String> valid = new HashSet<>(tags.length, 1);
			for (IApiJavadocTag tag : tags) {
				valid.add(tag.getTagName());
			}
			return valid;
		}
		return Collections.EMPTY_SET;
	}

	/**
	 * Creates a new {@link IApiProblem} for the given tag and adds it to the
	 * cache
	 *
	 * @param tag
	 * @param element
	 * @param context
	 */
	private void createTagProblem(String typeName, TagElement tag, int element, int kind, int markerid, String context) {
		int charstart = tag.getStartPosition();
		int charend = charstart + tag.getTagName().length();
		int linenumber = -1;
		try {
			// unit cannot be null
			IDocument document = Util.getDocument(fCompilationUnit);
			linenumber = document.getLineOfOffset(charstart);
		} catch (BadLocationException | CoreException e) {
			// ignore
		}
		try {
			IApiProblem problem = ApiProblemFactory.newApiProblem(fCompilationUnit.getCorrespondingResource().getProjectRelativePath().toPortableString(), typeName, new String[] {
					tag.getTagName(), context }, new String[] {
					IApiMarkerConstants.API_MARKER_ATTR_ID,
					IApiMarkerConstants.MARKER_ATTR_HANDLE_ID }, new Object[] {
					Integer.valueOf(markerid),
					fCompilationUnit.getHandleIdentifier() }, linenumber, charstart, charend, IApiProblem.CATEGORY_USAGE, element, kind, IApiProblem.NO_FLAGS);

			addProblem(problem);
		} catch (JavaModelException e) {
			ApiPlugin.log("Failed to report problem for " + fCompilationUnit.getElementName(), e); //$NON-NLS-1$
		}
	}

	@Override
	public boolean visit(MarkerAnnotation node) {
		String name = node.getTypeName().getFullyQualifiedName();
		if (JavadocTagManager.ALL_ANNOTATIONS.contains(name)) {
			ASTNode parent = node.getParent();
			switch (parent.getNodeType()) {
				case ASTNode.TYPE_DECLARATION: {
					checkType(node, (TypeDeclaration) node.getParent());
					break;
				}
				case ASTNode.ENUM_DECLARATION: {
					checkEnum(node, (EnumDeclaration) parent);
					break;
				}
				case ASTNode.ENUM_CONSTANT_DECLARATION: {
					Item item = getItem();
					createAnnotationProblem(item.typename, node, IElementDescriptor.FIELD, IApiProblem.UNSUPPORTED_ANNOTATION_USE, IApiMarkerConstants.UNSUPPORTED_ANNOTATION_MARKER_ID, BuilderMessages.TagValidator_an_enum_constant);
					break;
				}
				case ASTNode.ANNOTATION_TYPE_DECLARATION: {
					checkAnnotation(node, (AnnotationTypeDeclaration) parent);
					break;
				}
				case ASTNode.ANNOTATION_TYPE_MEMBER_DECLARATION: {
					Item item = getItem();
					createAnnotationProblem(item.typename, node, IElementDescriptor.METHOD, IApiProblem.UNSUPPORTED_ANNOTATION_USE, IApiMarkerConstants.UNSUPPORTED_ANNOTATION_MARKER_ID, BuilderMessages.TagValidator_an_annotation_method);
					break;
				}
				case ASTNode.FIELD_DECLARATION: {
					checkField(node, (FieldDeclaration) parent);
					break;
				}
				case ASTNode.METHOD_DECLARATION: {
					checkMethod(node, (MethodDeclaration) parent);
					break;
				}
				default:
					break;
			}
		}
		return false;
	}

	/**
	 * Checks the annotation that appears on the given parent
	 *
	 * @param node the annotation
	 * @param type the parent
	 */
	void checkType(MarkerAnnotation node, TypeDeclaration type) {
		String name = node.getTypeName().getFullyQualifiedName();
		Item item = getItem();
		if (fProcessedAnnotations.contains(name)) {
			createAnnotationProblem(item.typename, node, IElementDescriptor.TYPE, IApiProblem.DUPLICATE_ANNOTATION_USE, IApiMarkerConstants.DUPLICATE_ANNOTATION_MARKER_ID, null);
		} else {
			if (type.isInterface()) {
				Set<String> supported = ApiPlugin.getJavadocTagManager().getAnntationsForType(IApiJavadocTag.TYPE_INTERFACE, IApiJavadocTag.MEMBER_NONE);
				if (!supported.contains(name)) {
					createAnnotationProblem(item.typename, node, IElementDescriptor.TYPE, IApiProblem.UNSUPPORTED_ANNOTATION_USE, IApiMarkerConstants.UNSUPPORTED_ANNOTATION_MARKER_ID, BuilderMessages.TagValidator_an_interface);
				} else if (!item.visible) {
					createAnnotationProblem(item.typename, node, IElementDescriptor.TYPE, IApiProblem.UNSUPPORTED_ANNOTATION_USE, IApiMarkerConstants.UNSUPPORTED_ANNOTATION_MARKER_ID, BuilderMessages.TagValidator_an_interface_that_is_not_visible);
				}
			} else {
				int flags = type.getModifiers();
				Set<String> supported = ApiPlugin.getJavadocTagManager().getAnntationsForType(IApiJavadocTag.TYPE_CLASS, IApiJavadocTag.MEMBER_NONE);
				if (!supported.contains(name)) {
					createAnnotationProblem(item.typename, node, IElementDescriptor.TYPE, IApiProblem.UNSUPPORTED_ANNOTATION_USE, IApiMarkerConstants.UNSUPPORTED_ANNOTATION_MARKER_ID, BuilderMessages.TagValidator_a_class);
				} else if (Flags.isPrivate(flags)) {
					createAnnotationProblem(item.typename, node, IElementDescriptor.TYPE, IApiProblem.UNSUPPORTED_ANNOTATION_USE, IApiMarkerConstants.UNSUPPORTED_ANNOTATION_MARKER_ID, BuilderMessages.TagValidator_a_private_class);
				} else if (Flags.isPackageDefault(flags)) {
					createAnnotationProblem(item.typename, node, IElementDescriptor.TYPE, IApiProblem.UNSUPPORTED_ANNOTATION_USE, IApiMarkerConstants.UNSUPPORTED_ANNOTATION_MARKER_ID, BuilderMessages.TagValidator_a_package_default_class);
				} else if (Flags.isAbstract(flags) && JavadocTagManager.ANNOTATION_NOINSTANTIATE.equals(name)) {
					createAnnotationProblem(item.typename, node, IElementDescriptor.TYPE, IApiProblem.UNSUPPORTED_ANNOTATION_USE, IApiMarkerConstants.UNSUPPORTED_ANNOTATION_MARKER_ID, BuilderMessages.TagValidator_an_abstract_class);
				} else if (Flags.isFinal(flags) && JavadocTagManager.ANNOTATION_NOEXTEND.equals(name)) {
					createAnnotationProblem(item.typename, node, IElementDescriptor.TYPE, IApiProblem.UNSUPPORTED_ANNOTATION_USE, IApiMarkerConstants.UNSUPPORTED_ANNOTATION_MARKER_ID, BuilderMessages.TagValidator_a_final_class);
				} else if (!item.visible) {
					createAnnotationProblem(item.typename, node, IElementDescriptor.TYPE, IApiProblem.UNSUPPORTED_ANNOTATION_USE, IApiMarkerConstants.UNSUPPORTED_ANNOTATION_MARKER_ID, BuilderMessages.TagValidator_a_class_that_is_not_visible);
				}
			}
		}
		fProcessedAnnotations.add(name);
	}

	/**
	 * Checks the annotation that appears on the given parent
	 *
	 * @param node the annotation
	 * @param type the parent
	 */
	void checkEnum(MarkerAnnotation node, EnumDeclaration type) {
		String name = node.getTypeName().getFullyQualifiedName();
		Item item = getItem();
		if (fProcessedAnnotations.contains(name)) {
			createAnnotationProblem(item.typename, node, IElementDescriptor.TYPE, IApiProblem.DUPLICATE_ANNOTATION_USE, IApiMarkerConstants.DUPLICATE_ANNOTATION_MARKER_ID, null);
		} else {
			Set<String> supported = ApiPlugin.getJavadocTagManager().getAnntationsForType(IApiJavadocTag.TYPE_ENUM, IApiJavadocTag.MEMBER_NONE);
			if (!supported.contains(name)) {
				createAnnotationProblem(item.typename, node, IElementDescriptor.TYPE, IApiProblem.UNSUPPORTED_ANNOTATION_USE, IApiMarkerConstants.UNSUPPORTED_ANNOTATION_MARKER_ID, BuilderMessages.TagValidator_an_enum);
			} else if (!item.visible) {
				createAnnotationProblem(item.typename, node, IElementDescriptor.TYPE, IApiProblem.UNSUPPORTED_ANNOTATION_USE, IApiMarkerConstants.UNSUPPORTED_ANNOTATION_MARKER_ID, BuilderMessages.TagValidator_enum_not_visible);
			}
		}
		fProcessedAnnotations.add(name);
	}

	/**
	 * Checks the annotation that appears on the given parent
	 *
	 * @param node the annotation
	 * @param type the parent
	 */
	void checkAnnotation(MarkerAnnotation node, AnnotationTypeDeclaration type) {
		String name = node.getTypeName().getFullyQualifiedName();
		Item item = getItem();
		if (fProcessedAnnotations.contains(name)) {
			createAnnotationProblem(item.typename, node, IElementDescriptor.TYPE, IApiProblem.DUPLICATE_ANNOTATION_USE, IApiMarkerConstants.DUPLICATE_ANNOTATION_MARKER_ID, null);
		} else {
			Set<String> supported = ApiPlugin.getJavadocTagManager().getAnntationsForType(IApiJavadocTag.TYPE_ANNOTATION, IApiJavadocTag.MEMBER_NONE);
			if (!supported.contains(name)) {
				createAnnotationProblem(item.typename, node, IElementDescriptor.TYPE, IApiProblem.UNSUPPORTED_ANNOTATION_USE, IApiMarkerConstants.UNSUPPORTED_ANNOTATION_MARKER_ID, BuilderMessages.TagValidator_an_annotation);
			} else if (!item.visible) {
				createAnnotationProblem(item.typename, node, IElementDescriptor.TYPE, IApiProblem.UNSUPPORTED_ANNOTATION_USE, IApiMarkerConstants.UNSUPPORTED_ANNOTATION_MARKER_ID, BuilderMessages.TagValidator_annotation_not_visible);
			}
		}
	}

	/**
	 * Checks the annotation that appears on the given parent
	 *
	 * @param node the annotation
	 * @param parent the parent
	 */
	void checkField(MarkerAnnotation node, FieldDeclaration parent) {
		String name = node.getTypeName().getFullyQualifiedName();
		Item item = getItem();
		if (fProcessedAnnotations.contains(name)) {
			createAnnotationProblem(item.typename, node, IElementDescriptor.FIELD, IApiProblem.DUPLICATE_ANNOTATION_USE, IApiMarkerConstants.DUPLICATE_ANNOTATION_MARKER_ID, null);
		} else {
			int pkind = getParentKind(parent);
			int flags = parent.getModifiers();
			boolean isprivate = Flags.isPrivate(flags);
			boolean ispackage = Flags.isPackageDefault(flags);
			Set<String> supportedtags = ApiPlugin.getJavadocTagManager().getAnntationsForType(pkind, IApiJavadocTag.MEMBER_FIELD);
			switch (pkind) {
				case IApiJavadocTag.TYPE_ANNOTATION: {
					createAnnotationProblem(item.typename, node, IElementDescriptor.FIELD, IApiProblem.UNSUPPORTED_ANNOTATION_USE, IApiMarkerConstants.UNSUPPORTED_ANNOTATION_MARKER_ID, BuilderMessages.TagValidator_annotation_field);
					break;
				}
				case IApiJavadocTag.TYPE_ENUM: {
					if (!supportedtags.contains(name)) {
						createAnnotationProblem(item.typename, node, IElementDescriptor.FIELD, IApiProblem.UNSUPPORTED_ANNOTATION_USE, IApiMarkerConstants.UNSUPPORTED_ANNOTATION_MARKER_ID, BuilderMessages.TagValidator_enum_field);
					} else if (isprivate) {
						createAnnotationProblem(item.typename, node, IElementDescriptor.FIELD, IApiProblem.UNSUPPORTED_ANNOTATION_USE, IApiMarkerConstants.UNSUPPORTED_ANNOTATION_MARKER_ID, BuilderMessages.TagValidator_private_enum_field);
					} else if (!item.visible) {
						createAnnotationProblem(item.typename, node, IElementDescriptor.FIELD, IApiProblem.UNSUPPORTED_ANNOTATION_USE, IApiMarkerConstants.UNSUPPORTED_ANNOTATION_MARKER_ID, BuilderMessages.TagValidator_not_visible_enum_field);
					}
					break;
				}
				case IApiJavadocTag.TYPE_INTERFACE: {
					if (!supportedtags.contains(name)) {
						createAnnotationProblem(item.typename, node, IElementDescriptor.FIELD, IApiProblem.UNSUPPORTED_ANNOTATION_USE, IApiMarkerConstants.UNSUPPORTED_ANNOTATION_MARKER_ID, BuilderMessages.TagValidator_an_interface_field);
					} else if (!item.visible) {
						createAnnotationProblem(item.typename, node, IElementDescriptor.FIELD, IApiProblem.UNSUPPORTED_ANNOTATION_USE, IApiMarkerConstants.UNSUPPORTED_ANNOTATION_MARKER_ID, BuilderMessages.TagValidator_not_visible_interface_field);
					}
					break;
				}
				case IApiJavadocTag.TYPE_CLASS: {
					if (!supportedtags.contains(name)) {
						createAnnotationProblem(item.typename, node, IElementDescriptor.FIELD, IApiProblem.UNSUPPORTED_ANNOTATION_USE, IApiMarkerConstants.UNSUPPORTED_ANNOTATION_MARKER_ID, BuilderMessages.TagValidator_a_field);
					} else if (isprivate) {
						createAnnotationProblem(item.typename, node, IElementDescriptor.FIELD, IApiProblem.UNSUPPORTED_ANNOTATION_USE, IApiMarkerConstants.UNSUPPORTED_ANNOTATION_MARKER_ID, BuilderMessages.TagValidator_private_field);
					} else if (ispackage) {
						createAnnotationProblem(item.typename, node, IElementDescriptor.FIELD, IApiProblem.UNSUPPORTED_ANNOTATION_USE, IApiMarkerConstants.UNSUPPORTED_ANNOTATION_MARKER_ID, BuilderMessages.TagValidator_a_package_default_field);
					} else if (!item.visible) {
						createAnnotationProblem(item.typename, node, IElementDescriptor.FIELD, IApiProblem.UNSUPPORTED_ANNOTATION_USE, IApiMarkerConstants.UNSUPPORTED_ANNOTATION_MARKER_ID, BuilderMessages.TagValidator_a_field_that_is_not_visible);
					}
					break;
				}
				default: {
					break;
				}
			}
			fProcessedAnnotations.add(name);
		}
	}

	/**
	 * Checks the given annotation
	 *
	 * @param node the annotation
	 */
	void checkMethod(MarkerAnnotation node, MethodDeclaration parent) {
		String name = node.getTypeName().getFullyQualifiedName();
		int pkind = getParentKind(parent);
		int flags = parent.getModifiers();
		boolean isconstructor = parent.isConstructor();
		boolean isstatic = Flags.isStatic(flags);
		Item item = getItem();
		Set<String> supportedtags = ApiPlugin.getJavadocTagManager().getAnntationsForType(pkind, isconstructor ? IApiJavadocTag.MEMBER_CONSTRUCTOR : IApiJavadocTag.MEMBER_METHOD);
		if (fProcessedAnnotations.contains(name)) {
			createAnnotationProblem(item.typename, node, IElementDescriptor.METHOD, IApiProblem.DUPLICATE_ANNOTATION_USE, IApiMarkerConstants.DUPLICATE_ANNOTATION_MARKER_ID, null);
		} else {
			switch (pkind) {
				case IApiJavadocTag.TYPE_ENUM: {
					if (!supportedtags.contains(name)) {
						createAnnotationProblem(item.typename, node, IElementDescriptor.METHOD, IApiProblem.UNSUPPORTED_ANNOTATION_USE, IApiMarkerConstants.UNSUPPORTED_ANNOTATION_MARKER_ID, BuilderMessages.TagValidator_an_enum_method);
					} else if (Flags.isPrivate(flags)) {
						createAnnotationProblem(item.typename, node, IElementDescriptor.METHOD, IApiProblem.UNSUPPORTED_ANNOTATION_USE, IApiMarkerConstants.UNSUPPORTED_ANNOTATION_MARKER_ID, BuilderMessages.TagValidator_private_enum_method);
					} else if (Flags.isPackageDefault(flags)) {
						createAnnotationProblem(item.typename, node, IElementDescriptor.METHOD, IApiProblem.UNSUPPORTED_ANNOTATION_USE, IApiMarkerConstants.UNSUPPORTED_ANNOTATION_MARKER_ID, BuilderMessages.TagValidator_a_package_default_enum);
					} else if (!item.visible) {
						createAnnotationProblem(item.typename, node, IElementDescriptor.METHOD, IApiProblem.UNSUPPORTED_ANNOTATION_USE, IApiMarkerConstants.UNSUPPORTED_ANNOTATION_MARKER_ID, BuilderMessages.TagValidator_not_visible_enum_method);
					}
					break;
				}
				case IApiJavadocTag.TYPE_INTERFACE: {
					if (!supportedtags.contains(name)) {
						createAnnotationProblem(item.typename, node, IElementDescriptor.METHOD, IApiProblem.UNSUPPORTED_ANNOTATION_USE, IApiMarkerConstants.UNSUPPORTED_ANNOTATION_MARKER_ID, BuilderMessages.TagValidator_an_interface_method);
					} else if (!item.visible) {
						createAnnotationProblem(item.typename, node, IElementDescriptor.METHOD, IApiProblem.UNSUPPORTED_ANNOTATION_USE, IApiMarkerConstants.UNSUPPORTED_ANNOTATION_MARKER_ID, BuilderMessages.TagValidator_not_visible_interface_method);
					} else if (!Flags.isDefaultMethod(flags) && JavadocTagManager.ANNOTATION_NOOVERRIDE.equals(name)) {
						createAnnotationProblem(item.typename, node, IElementDescriptor.METHOD, IApiProblem.UNSUPPORTED_ANNOTATION_USE, IApiMarkerConstants.UNSUPPORTED_ANNOTATION_MARKER_ID, BuilderMessages.TagValidator_nondefault_interface_method);
					}
					break;
				}
				case IApiJavadocTag.TYPE_CLASS: {
					if (!supportedtags.contains(name)) {
						createAnnotationProblem(item.typename, node, IElementDescriptor.METHOD, IApiProblem.UNSUPPORTED_ANNOTATION_USE, IApiMarkerConstants.UNSUPPORTED_ANNOTATION_MARKER_ID, isconstructor ? BuilderMessages.TagValidator_a_constructor : BuilderMessages.TagValidator_a_method);
					} else if (Flags.isPrivate(flags)) {
						createAnnotationProblem(item.typename, node, IElementDescriptor.METHOD, IApiProblem.UNSUPPORTED_ANNOTATION_USE, IApiMarkerConstants.UNSUPPORTED_ANNOTATION_MARKER_ID, isconstructor ? BuilderMessages.TagValidator_private_constructor : BuilderMessages.TagValidator_private_method);
					} else if (Flags.isPackageDefault(flags)) {
						if (isstatic) {
							createAnnotationProblem(item.typename, node, IElementDescriptor.METHOD, IApiProblem.UNSUPPORTED_ANNOTATION_USE, IApiMarkerConstants.UNSUPPORTED_ANNOTATION_MARKER_ID, isconstructor ? BuilderMessages.TagValidator_static_package_constructor : BuilderMessages.TagValidator_a_static_package_default_method);
						} else {
							createAnnotationProblem(item.typename, node, IElementDescriptor.METHOD, IApiProblem.UNSUPPORTED_ANNOTATION_USE, IApiMarkerConstants.UNSUPPORTED_ANNOTATION_MARKER_ID, isconstructor ? BuilderMessages.TagValidator_a_package_default_constructor : BuilderMessages.TagValidator_a_package_default_method);
						}
					} else if (JavadocTagManager.ANNOTATION_NOOVERRIDE.equals(name)) {
						if (Flags.isFinal(flags)) {
							if (isstatic) {
								createAnnotationProblem(item.typename, node, IElementDescriptor.METHOD, IApiProblem.UNSUPPORTED_ANNOTATION_USE, IApiMarkerConstants.UNSUPPORTED_ANNOTATION_MARKER_ID, isconstructor ? BuilderMessages.TagValidator_static_final_constructor : BuilderMessages.TagValidator_a_static_final_method);
							} else {
								createAnnotationProblem(item.typename, node, IElementDescriptor.METHOD, IApiProblem.UNSUPPORTED_ANNOTATION_USE, IApiMarkerConstants.UNSUPPORTED_ANNOTATION_MARKER_ID, isconstructor ? BuilderMessages.TagValidator_final_constructor : BuilderMessages.TagValidator_a_final_method);
							}
						} else if (isstatic) {
							createAnnotationProblem(item.typename, node, IElementDescriptor.METHOD, IApiProblem.UNSUPPORTED_ANNOTATION_USE, IApiMarkerConstants.UNSUPPORTED_ANNOTATION_MARKER_ID, isconstructor ? BuilderMessages.TagValidator_a_static_constructor : BuilderMessages.TagValidator_a_static_method);
						} else if (Flags.isFinal(getParentModifiers(parent))) {
							createAnnotationProblem(item.typename, node, IElementDescriptor.METHOD, IApiProblem.UNSUPPORTED_ANNOTATION_USE, IApiMarkerConstants.UNSUPPORTED_ANNOTATION_MARKER_ID, isconstructor ? BuilderMessages.TagValidator_constructor_in_final_class : BuilderMessages.TagValidator_a_method_in_a_final_class);
						} else if (!item.visible) {
							createAnnotationProblem(item.typename, node, IElementDescriptor.METHOD, IApiProblem.UNSUPPORTED_ANNOTATION_USE, IApiMarkerConstants.UNSUPPORTED_ANNOTATION_MARKER_ID, isconstructor ? BuilderMessages.TagValidator_not_visible_constructor : BuilderMessages.TagValidator_a_method_that_is_not_visible);
						}
					} else if (!item.visible) {
						createAnnotationProblem(item.typename, node, IElementDescriptor.METHOD, IApiProblem.UNSUPPORTED_ANNOTATION_USE, IApiMarkerConstants.UNSUPPORTED_ANNOTATION_MARKER_ID, isconstructor ? BuilderMessages.TagValidator_not_visible_constructor : BuilderMessages.TagValidator_a_method_that_is_not_visible);
					}
					break;
				}
				default: {
					break;
				}
			}
		}
		fProcessedAnnotations.add(name);
	}

	/**
	 * Creates a new problem
	 *
	 * @param typeName
	 * @param node
	 * @param element
	 * @param kind
	 * @param markerid
	 * @param context
	 */
	void createAnnotationProblem(String typeName, MarkerAnnotation node, int element, int kind, int markerid, String context) {
		String name = '@' + node.getTypeName().getFullyQualifiedName();
		int charstart = node.getStartPosition();
		int charend = charstart + name.length();
		int linenumber = -1;
		try {
			// unit cannot be null
			IDocument document = Util.getDocument(fCompilationUnit);
			linenumber = document.getLineOfOffset(charstart);
		} catch (BadLocationException e) {
			e.printStackTrace();
		} catch (CoreException e) {
			e.printStackTrace();
		}
		try {
			IApiProblem problem = ApiProblemFactory.newApiProblem(fCompilationUnit.getCorrespondingResource().getProjectRelativePath().toPortableString(), typeName, new String[] {
					name, context }, new String[] {
					IApiMarkerConstants.API_MARKER_ATTR_ID,
					IApiMarkerConstants.MARKER_ATTR_HANDLE_ID }, new Object[] {
					Integer.valueOf(markerid),
					fCompilationUnit.getHandleIdentifier() }, linenumber, charstart, charend, IApiProblem.CATEGORY_USAGE, element, kind, IApiProblem.NO_FLAGS);

			addProblem(problem);
		} catch (JavaModelException e) {
			ApiPlugin.log("Failed to report problem for " + fCompilationUnit.getElementName(), e); //$NON-NLS-1$
		}
	}
}
