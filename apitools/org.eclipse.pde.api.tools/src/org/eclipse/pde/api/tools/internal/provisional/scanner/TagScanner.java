/*******************************************************************************
 * Copyright (c) 2007, 2015 IBM Corporation and others.
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
package org.eclipse.pde.api.tools.internal.provisional.scanner;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.text.MessageFormat;
import java.util.List;
import java.util.Map;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.jdt.core.Flags;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.AnnotationTypeDeclaration;
import org.eclipse.jdt.core.dom.AnonymousClassDeclaration;
import org.eclipse.jdt.core.dom.EnumDeclaration;
import org.eclipse.jdt.core.dom.FieldDeclaration;
import org.eclipse.jdt.core.dom.Javadoc;
import org.eclipse.jdt.core.dom.MarkerAnnotation;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.Name;
import org.eclipse.jdt.core.dom.PackageDeclaration;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.TagElement;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;
import org.eclipse.pde.api.tools.internal.CompilationUnit;
import org.eclipse.pde.api.tools.internal.JavadocTagManager;
import org.eclipse.pde.api.tools.internal.provisional.ApiPlugin;
import org.eclipse.pde.api.tools.internal.provisional.Factory;
import org.eclipse.pde.api.tools.internal.provisional.IApiAnnotations;
import org.eclipse.pde.api.tools.internal.provisional.IApiDescription;
import org.eclipse.pde.api.tools.internal.provisional.RestrictionModifiers;
import org.eclipse.pde.api.tools.internal.provisional.descriptors.IElementDescriptor;
import org.eclipse.pde.api.tools.internal.provisional.descriptors.IMethodDescriptor;
import org.eclipse.pde.api.tools.internal.provisional.descriptors.IPackageDescriptor;
import org.eclipse.pde.api.tools.internal.provisional.descriptors.IReferenceTypeDescriptor;
import org.eclipse.pde.api.tools.internal.provisional.model.IApiTypeContainer;
import org.eclipse.pde.api.tools.internal.util.Signatures;
import org.eclipse.pde.api.tools.internal.util.Util;

/**
 * Scans the source of a *.java file for any API javadoc tags
 *
 * @since 1.0.0
 */
public class TagScanner {

	/**
	 * Visitor to scan a compilation unit. We only care about Javadoc nodes that
	 * have either type or enum declarations as parents, so we have to override
	 * the ones we don't care about.
	 */
	static class Visitor extends ASTVisitor {

		private IApiDescription fDescription = null;

		/**
		 * Package descriptor. Initialized to default package, and overridden if
		 * a package declaration is visited.
		 */
		private IPackageDescriptor fPackage = Factory.packageDescriptor(""); //$NON-NLS-1$

		/**
		 * Type descriptor for type currently being visited.
		 */
		private IReferenceTypeDescriptor fType = null;

		/**
		 * Used to look up binaries when resolving method signatures, or
		 * <code>null</code> if not provided.
		 */
		private IApiTypeContainer fContainer = null;

		/**
		 * Constructor
		 *
		 * @param description API description to annotate
		 * @param container class file container or <code>null</code>, used to
		 *            resolve method signatures
		 */
		public Visitor(IApiDescription description, IApiTypeContainer container) {
			fDescription = description;
			fContainer = container;
		}

		/**
		 * A type has been entered - update the type being visited.
		 *
		 * @param name name from type node
		 */
		private void enterType(SimpleName name) {
			if (fType == null) {
				fType = fPackage.getType(name.getFullyQualifiedName());
			} else {
				fType = fType.getType(name.getFullyQualifiedName());
			}
		}

		/**
		 * A type has been exited - update the type being visited.
		 */
		private void exitType() {
			fType = fType.getEnclosingType();
		}

		@Override
		public boolean visit(MarkerAnnotation node) {
			String name = node.getTypeName().getFullyQualifiedName();
			if (JavadocTagManager.ALL_ANNOTATIONS.contains(name)) {
				ASTNode parent = node.getParent();
				if (parent != null) {
					switch (parent.getNodeType()) {
						case ASTNode.TYPE_DECLARATION: {
							scanTypeAnnotation(name, (TypeDeclaration) parent);
							break;
						}
						case ASTNode.ANNOTATION_TYPE_DECLARATION:
						case ASTNode.ENUM_DECLARATION: {
							IApiAnnotations annots = fDescription.resolveAnnotations(fType);
							int restrictions = annots != null ? annots.getRestrictions() : RestrictionModifiers.NO_RESTRICTIONS;
							if (JavadocTagManager.ANNOTATION_NOREFERENCE.equals(name)) {
								restrictions |= RestrictionModifiers.NO_REFERENCE;
								fDescription.setRestrictions(fType, restrictions);
							}
							break;
						}
						case ASTNode.FIELD_DECLARATION: {
							scanFieldAnnotation(name, (FieldDeclaration) parent);
							break;
						}
						case ASTNode.METHOD_DECLARATION: {
							scanMethodAnnotation(name, (MethodDeclaration) parent);
							break;
						}
						default:
							break;
					}
				}
			}
			return false;
		}

		/**
		 * Checks the annotation name found on the given {@link TypeDeclaration}
		 *
		 * @param name the name of the annotation
		 * @param node the parent {@link TypeDeclaration}
		 *
		 * @since 1.0.600
		 */
		void scanTypeAnnotation(String name, TypeDeclaration node) {
			int flags = node.getModifiers();
			IApiAnnotations annots = fDescription.resolveAnnotations(fType);
			int restrictions = annots != null ? annots.getRestrictions() : RestrictionModifiers.NO_RESTRICTIONS;
			if (JavadocTagManager.ANNOTATION_NOREFERENCE.equals(name)) {
				restrictions |= RestrictionModifiers.NO_REFERENCE;
			}
			if (JavadocTagManager.ANNOTATION_NOEXTEND.equals(name) && !Flags.isFinal(flags)) {
				restrictions |= RestrictionModifiers.NO_EXTEND;
			}
			if (node.isInterface()) {
				if (JavadocTagManager.ANNOTATION_NOIMPLEMENT.equals(name)) {
					restrictions |= RestrictionModifiers.NO_IMPLEMENT;
				}
			} else {
				if (JavadocTagManager.ANNOTATION_NOINSTANTIATE.equals(name) && !Flags.isAbstract(flags)) {
					restrictions |= RestrictionModifiers.NO_INSTANTIATE;
				}
			}
			if (restrictions != RestrictionModifiers.NO_RESTRICTIONS) {
				fDescription.setRestrictions(fType, restrictions);
			}
		}

		/**
		 * Checks the annotation name found on the given
		 * {@link FieldDeclaration}
		 *
		 * @param name the annotation name
		 * @param node the parent {@link FieldDeclaration}
		 */
		void scanFieldAnnotation(String name, FieldDeclaration node) {
			List<VariableDeclarationFragment> fields = node.fragments();
			int flags = node.getModifiers();

			if (!Flags.isFinal(flags) && JavadocTagManager.ANNOTATION_NOREFERENCE.equals(name)) {
				for (VariableDeclarationFragment fragment : fields) {
					IElementDescriptor descriptor = fType.getField(fragment.getName().getFullyQualifiedName());
					fDescription.setRestrictions(descriptor, RestrictionModifiers.NO_REFERENCE);
				}
			}
		}

		/**
		 * Checks the annotation name found on the given
		 * {@link MethodDeclaration}
		 *
		 * @param name the name of the annotation
		 * @param node the parent {@link MethodDeclaration}
		 *
		 * @since 1.0.600
		 */
		void scanMethodAnnotation(String name, MethodDeclaration node) {
			String signature = Signatures.getMethodSignatureFromNode(node, true);
			if (signature != null) {
				String methodname = node.getName().getFullyQualifiedName();
				if (node.isConstructor()) {
					methodname = "<init>"; //$NON-NLS-1$
				}
				IMethodDescriptor descriptor = fType.getMethod(methodname, signature);
				try {
					descriptor = Factory.resolveMethod(fContainer, descriptor);
				} catch (CoreException e) {
					if (ApiPlugin.DEBUG_TAG_SCANNER) {
						System.err.println(e.getLocalizedMessage());
					}
				}
				IApiAnnotations annots = fDescription.resolveAnnotations(descriptor);
				int restrictions = annots != null ? annots.getRestrictions() : RestrictionModifiers.NO_RESTRICTIONS;
				if (JavadocTagManager.ANNOTATION_NOREFERENCE.equals(name)) {
					restrictions |= RestrictionModifiers.NO_REFERENCE;
				}
				if (JavadocTagManager.ANNOTATION_NOOVERRIDE.equals(name)) {
					if (!Flags.isFinal(node.getModifiers()) && !Flags.isStatic(node.getModifiers())) {
						ASTNode parent = node.getParent();
						if (parent instanceof TypeDeclaration) {
							TypeDeclaration type = (TypeDeclaration) parent;
							if (type.isInterface()) {
								if (Flags.isDefaultMethod(node.getModifiers())) {
									restrictions |= RestrictionModifiers.NO_OVERRIDE;
								}
							} else if (!Flags.isFinal(type.getModifiers())) {
								restrictions |= RestrictionModifiers.NO_OVERRIDE;
							}
						} else if (parent instanceof AnonymousClassDeclaration) {
							restrictions |= RestrictionModifiers.NO_OVERRIDE;
						}
					}
				}
				if (restrictions != RestrictionModifiers.NO_RESTRICTIONS) {
					fDescription.setRestrictions(descriptor, restrictions);
				}
			}
		}

		@Override
		public boolean visit(TypeDeclaration node) {
			if (isNotVisible(node.getModifiers())) {
				return false;
			}
			enterType(node.getName());
			scanTypeJavaDoc(node);
			return true;
		}

		/**
		 * Scans the JavaDoc of a {@link TypeDeclaration}
		 *
		 * @param node the type
		 * @since 1.0.600
		 */
		void scanTypeJavaDoc(TypeDeclaration node) {
			Javadoc doc = node.getJavadoc();
			if (doc != null) {
				List<TagElement> tags = doc.tags();
				IApiAnnotations annots = fDescription.resolveAnnotations(fType);
				int restrictions = annots != null ? annots.getRestrictions() : RestrictionModifiers.NO_RESTRICTIONS;
				for (TagElement tag : tags) {
					String tagname = tag.getTagName();
					if (!JavadocTagManager.ALL_TAGS.contains(tagname)) {
						continue;
					}
					if (JavadocTagManager.TAG_NOREFERENCE.equals(tagname)) {
						restrictions |= RestrictionModifiers.NO_REFERENCE;
					}
					if (node.isInterface()) {
						if (JavadocTagManager.TAG_NOEXTEND.equals(tagname)) {
							restrictions |= RestrictionModifiers.NO_EXTEND;
						} else if (JavadocTagManager.TAG_NOIMPLEMENT.equals(tagname)) {
							restrictions |= RestrictionModifiers.NO_IMPLEMENT;
						}
					} else {
						int flags = node.getModifiers();
						if (JavadocTagManager.TAG_NOEXTEND.equals(tagname)) {
							if (!Flags.isFinal(flags)) {
								restrictions |= RestrictionModifiers.NO_EXTEND;
								continue;
							}
						}
						if (JavadocTagManager.TAG_NOINSTANTIATE.equals(tagname)) {
							if (!Flags.isAbstract(flags)) {
								restrictions |= RestrictionModifiers.NO_INSTANTIATE;
								continue;
							}
						}
					}
				}
				if (restrictions != RestrictionModifiers.NO_RESTRICTIONS) {
					fDescription.setRestrictions(fType, restrictions);
				}
			}
		}

		@Override
		public void endVisit(TypeDeclaration node) {
			if (!isNotVisible(node.getModifiers())) {
				exitType();
			}
		}

		@Override
		public void endVisit(AnnotationTypeDeclaration node) {
			if (!isNotVisible(node.getModifiers())) {
				exitType();
			}
		}

		@Override
		public boolean visit(AnnotationTypeDeclaration node) {
			if (isNotVisible(node.getModifiers())) {
				return false;
			}
			enterType(node.getName());
			scanAnnotationJavaDoc(node);
			return true;
		}

		/**
		 * Scans the JavaDoc of an {@link AnnotationTypeDeclaration}
		 *
		 * @param node the annotation
		 * @since 1.0.600
		 */
		void scanAnnotationJavaDoc(AnnotationTypeDeclaration node) {
			Javadoc doc = node.getJavadoc();
			if (doc != null) {
				List<TagElement> tags = doc.tags();
				IApiAnnotations annots = fDescription.resolveAnnotations(fType);
				int restrictions = annots != null ? annots.getRestrictions() : RestrictionModifiers.NO_RESTRICTIONS;
				for (TagElement tag : tags) {
					String tagname = tag.getTagName();
					if (!JavadocTagManager.ALL_TAGS.contains(tagname)) {
						continue;
					}
					if (JavadocTagManager.TAG_NOREFERENCE.equals(tagname)) {
						restrictions |= RestrictionModifiers.NO_REFERENCE;
						fDescription.setRestrictions(fType, restrictions);
					}
				}
			}
		}

		@Override
		public boolean visit(EnumDeclaration node) {
			if (isNotVisible(node.getModifiers())) {
				return false;
			}
			enterType(node.getName());
			scanEnumJavaDoc(node);
			return true;
		}

		/**
		 * Scans the JavaDoc of an {@link EnumDeclaration}
		 *
		 * @param node the enum
		 * @since 1.0.600
		 */
		void scanEnumJavaDoc(EnumDeclaration node) {
			Javadoc doc = node.getJavadoc();
			if (doc != null) {
				List<TagElement> tags = doc.tags();
				IApiAnnotations annots = fDescription.resolveAnnotations(fType);
				int restrictions = annots != null ? annots.getRestrictions() : RestrictionModifiers.NO_RESTRICTIONS;
				for (TagElement tag : tags) {
					String tagname = tag.getTagName();
					if (!JavadocTagManager.ALL_TAGS.contains(tagname)) {
						continue;
					}
					if (JavadocTagManager.TAG_NOREFERENCE.equals(tagname)) {
						restrictions |= RestrictionModifiers.NO_REFERENCE;
						fDescription.setRestrictions(fType, restrictions);
					}
				}
			}
		}

		@Override
		public void endVisit(EnumDeclaration node) {
			if (!isNotVisible(node.getModifiers())) {
				exitType();
			}
		}

		@Override
		public boolean visit(PackageDeclaration node) {
			Name name = node.getName();
			fPackage = Factory.packageDescriptor(name.getFullyQualifiedName());
			return false;
		}

		@Override
		public boolean visit(MethodDeclaration node) {
			if (isNotVisible(node.getModifiers())) {
				ASTNode parent = node.getParent();
				if (parent instanceof TypeDeclaration) {
					TypeDeclaration type = (TypeDeclaration) parent;
					if (!type.isInterface()) {
						return false;
					}
				} else {
					return false;
				}
			}
			scanMethodJavaDoc(node);
			return true;
		}

		/**
		 * Scans the JavaDoc node of a {@link MethodDeclaration}
		 *
		 * @param node the method
		 * @since 1.0.600
		 */
		void scanMethodJavaDoc(MethodDeclaration node) {
			Javadoc doc = node.getJavadoc();
			if (doc != null) {
				String signature = Signatures.getMethodSignatureFromNode(node, true);
				if (signature != null) {
					String methodname = node.getName().getFullyQualifiedName();
					if (node.isConstructor()) {
						methodname = "<init>"; //$NON-NLS-1$
					}
					IMethodDescriptor descriptor = fType.getMethod(methodname, signature);
					try {
						descriptor = Factory.resolveMethod(fContainer, descriptor);
					} catch (CoreException e) {
						if (ApiPlugin.DEBUG_TAG_SCANNER) {
							System.err.println(e.getLocalizedMessage());
						}
					}
					List<TagElement> tags = doc.tags();
					IApiAnnotations annots = fDescription.resolveAnnotations(descriptor);
					int restrictions = annots != null ? annots.getRestrictions() : RestrictionModifiers.NO_RESTRICTIONS;
					for (TagElement tag : tags) {
						String tagname = tag.getTagName();
						if (!JavadocTagManager.ALL_TAGS.contains(tagname)) {
							continue;
						}
						if (JavadocTagManager.TAG_NOREFERENCE.equals(tagname)) {
							restrictions |= RestrictionModifiers.NO_REFERENCE;
						}
						if (JavadocTagManager.TAG_NOOVERRIDE.equals(tagname)) {
							if (Flags.isFinal(node.getModifiers()) || Flags.isStatic(node.getModifiers())) {
								continue;
							}
							ASTNode parent = node.getParent();
							if (parent instanceof TypeDeclaration) {
								TypeDeclaration type = (TypeDeclaration) parent;
								if (type.isInterface()) {
									if (Flags.isDefaultMethod(node.getModifiers())) {
										restrictions |= RestrictionModifiers.NO_OVERRIDE;
									}
								} else if (!Flags.isFinal(type.getModifiers())) {
									restrictions |= RestrictionModifiers.NO_OVERRIDE;
								}
							} else if (parent instanceof AnonymousClassDeclaration) {
								restrictions |= RestrictionModifiers.NO_OVERRIDE;
							}
						}
					}
					if (restrictions != RestrictionModifiers.NO_RESTRICTIONS) {
						fDescription.setRestrictions(descriptor, restrictions);
					}
				}
			}
		}

		@Override
		public boolean visit(FieldDeclaration node) {
			if (isNotVisible(node.getModifiers())) {
				return false;
			}
			scanFieldJavaDoc(node);
			return true;
		}

		/**
		 * Scans the JavaDoc nodes of a {@link FieldDeclaration}
		 *
		 * @param node the field
		 * @since 1.0.600
		 */
		void scanFieldJavaDoc(FieldDeclaration node) {
			Javadoc doc = node.getJavadoc();
			if (doc != null) {
				List<VariableDeclarationFragment> fields = node.fragments();
				List<TagElement> tags = doc.tags();
				int flags = node.getModifiers();
				for (TagElement tag : tags) {
					String tagname = tag.getTagName();
					if (!JavadocTagManager.ALL_TAGS.contains(tagname)) {
						continue;
					}
					if (!Flags.isFinal(flags) && JavadocTagManager.TAG_NOREFERENCE.equals(tagname)) {
						for (VariableDeclarationFragment fragment : fields) {
							IElementDescriptor descriptor = fType.getField(fragment.getName().getFullyQualifiedName());
							fDescription.setRestrictions(descriptor, RestrictionModifiers.NO_REFERENCE);
						}
					}
				}
			}
		}

		/**
		 * Determine if the flags contain private or package default flags
		 *
		 * @param flags
		 * @return <code>true</code> if the flags are private or default,
		 *         <code>false</code> otherwise
		 */
		private boolean isNotVisible(int flags) {
			return Flags.isPrivate(flags) || Flags.isPackageDefault(flags);
		}
	}

	/**
	 * The singleton instance of the scanner
	 */
	private static TagScanner fSingleton = null;

	/**
	 * Delegate for getting the singleton instance of the scanner
	 *
	 * @return
	 */
	public static final TagScanner newScanner() {
		if (fSingleton == null) {
			fSingleton = new TagScanner();
		}
		return fSingleton;
	}

	/**
	 * Constructor Cannot be instantiated
	 */
	private TagScanner() {
	}

	/**
	 * Scans the specified {@link ICompilationUnit} for contributed API Javadoc
	 * tags. Tags on methods will have unresolved signatures.
	 *
	 * @param unit the compilation unit source
	 * @param description the API description to annotate with any new tag rules
	 *            found
	 * @param container optional class file container containing the class file
	 *            for the given source that can be used to resolve method
	 *            signatures if required (for tags on methods). If not provided
	 *            (<code>null</code>), method signatures will be unresolved.
	 * @param monitor
	 *
	 * @throws CoreException if problems were encountered while scanning tags,
	 *             the description may still be modified
	 */
	public void scan(ICompilationUnit unit, IApiDescription description, IApiTypeContainer container, IProgressMonitor monitor) throws CoreException {
		scan(new CompilationUnit(unit), description, container, unit.getJavaProject().getOptions(true), monitor);
	}

	/**
	 * Scans the specified source {@linkplain CompilationUnit} for contributed
	 * API javadoc tags. Tags on methods will have unresolved signatures.
	 *
	 * @param source the source file to scan for tags
	 * @param description the API description to annotate with any new tag rules
	 *            found
	 * @param container optional class file container containing the class file
	 *            for the given source that can be used to resolve method
	 *            signatures if required (for tags on methods). If not provided
	 *            (<code>null</code>), method signatures will be unresolved.
	 * @param options a map of Java compiler options to use when creating the
	 *            AST to scan or <code>null</code> if default options should be
	 *            used
	 * @param monitor
	 *
	 * @throws CoreException if problems were encountered while scanning tags,
	 *             the description may still be modified
	 */
	public void scan(CompilationUnit source, IApiDescription description, IApiTypeContainer container, Map<String, String> options, IProgressMonitor monitor) throws CoreException {
		SubMonitor localmonitor = SubMonitor.convert(monitor, 2);
		ASTParser parser = ASTParser.newParser(AST.JLS10);
		InputStream inputStream = null;
		try {
			inputStream = source.getInputStream();
			parser.setSource(Util.getInputStreamAsCharArray(inputStream, -1, source.getEncoding()));
		} catch (FileNotFoundException e) {
			throw new CoreException(new Status(IStatus.ERROR, ApiPlugin.PLUGIN_ID, MessageFormat.format("Compilation unit source not found: {0}", source.getName()), e)); //$NON-NLS-1$
		} catch (IOException e) {
			if (ApiPlugin.DEBUG_TAG_SCANNER) {
				System.err.println(source.getName());
			}
			throw new CoreException(new Status(IStatus.ERROR, ApiPlugin.PLUGIN_ID, MessageFormat.format("Error reading compilation unit: {0}", source.getName()), e)); //$NON-NLS-1$
		} finally {
			if (inputStream != null) {
				try {
					inputStream.close();
				} catch (IOException e) {
					ApiPlugin.log(e);
				}
			}
		}
		localmonitor.split(1);
		Map<String, String> loptions = options;
		if (loptions == null) {
			loptions = JavaCore.getOptions();
		}
		loptions.put(JavaCore.COMPILER_DOC_COMMENT_SUPPORT, JavaCore.ENABLED);
		parser.setCompilerOptions(loptions);
		org.eclipse.jdt.core.dom.CompilationUnit cunit = (org.eclipse.jdt.core.dom.CompilationUnit) parser.createAST(localmonitor.split(1));
		Visitor visitor = new Visitor(description, container);
		cunit.accept(visitor);
	}
}
