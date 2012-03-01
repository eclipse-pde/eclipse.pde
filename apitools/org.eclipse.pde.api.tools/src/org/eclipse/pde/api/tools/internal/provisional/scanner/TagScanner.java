/*******************************************************************************
 * Copyright (c) 2007, 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.api.tools.internal.provisional.scanner;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Iterator;
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
import org.eclipse.jdt.core.dom.AbstractTypeDeclaration;
import org.eclipse.jdt.core.dom.AnnotationTypeDeclaration;
import org.eclipse.jdt.core.dom.BodyDeclaration;
import org.eclipse.jdt.core.dom.EnumDeclaration;
import org.eclipse.jdt.core.dom.FieldDeclaration;
import org.eclipse.jdt.core.dom.Javadoc;
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
import org.eclipse.pde.api.tools.internal.provisional.IApiDescription;
import org.eclipse.pde.api.tools.internal.provisional.IApiJavadocTag;
import org.eclipse.pde.api.tools.internal.provisional.RestrictionModifiers;
import org.eclipse.pde.api.tools.internal.provisional.descriptors.IElementDescriptor;
import org.eclipse.pde.api.tools.internal.provisional.descriptors.IMethodDescriptor;
import org.eclipse.pde.api.tools.internal.provisional.descriptors.IPackageDescriptor;
import org.eclipse.pde.api.tools.internal.provisional.descriptors.IReferenceTypeDescriptor;
import org.eclipse.pde.api.tools.internal.provisional.model.IApiMethod;
import org.eclipse.pde.api.tools.internal.provisional.model.IApiType;
import org.eclipse.pde.api.tools.internal.provisional.model.IApiTypeContainer;
import org.eclipse.pde.api.tools.internal.provisional.model.IApiTypeRoot;
import org.eclipse.pde.api.tools.internal.util.Signatures;
import org.eclipse.pde.api.tools.internal.util.Util;

/**
 * Scans the source of a *.java file for any API javadoc tags
 * @since 1.0.0
 */
public class TagScanner {

	/**
	 * Visitor to scan a compilation unit. We only care about javadoc nodes that have either 
	 * type or enum declarations as parents, so we have to override the ones we don't care 
	 * about.
	 */
	static class Visitor extends ASTVisitor {
		
		private IApiDescription fDescription = null;
		
		/**
		 * Package descriptor. Initialized to default package, and overridden if a package
		 * declaration is visited.
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
		 * List of exceptions encountered, or <code>null</code>
		 */
		private CoreException fException;
		
		/**
		 * Constructor
		 * @param description API description to annotate
		 * @param container class file container or <code>null</code>, used
		 * 	to resolve method signatures
		 */
		public Visitor(IApiDescription description, IApiTypeContainer container) {
			fDescription = description;
			fContainer = container;
		}
		
		/* (non-Javadoc)
		 * @see org.eclipse.jdt.core.dom.ASTVisitor#visit(org.eclipse.jdt.core.dom.Javadoc)
		 */
		public boolean visit(Javadoc node) {
			List tags = node.tags();
			ASTNode parent = node.getParent();
			if(parent != null) {
				switch(parent.getNodeType()) {
					case ASTNode.TYPE_DECLARATION: {
						TypeDeclaration type = (TypeDeclaration) parent;
						if(type.isInterface()) {
							processTags(fType, pruneTags(tags, type), IApiJavadocTag.TYPE_INTERFACE, IApiJavadocTag.MEMBER_NONE);
						}
						else {
							processTags(fType, pruneTags(tags, type), IApiJavadocTag.TYPE_CLASS, IApiJavadocTag.MEMBER_NONE);
						}
						break;
					}
					case ASTNode.METHOD_DECLARATION: {
						MethodDeclaration method = (MethodDeclaration) parent;
						String signature = Signatures.getMethodSignatureFromNode(method);
						if(signature != null) {
							String methodname = method.getName().getFullyQualifiedName();
							int member = IApiJavadocTag.MEMBER_METHOD;
							if(method.isConstructor()) {
								member = IApiJavadocTag.MEMBER_CONSTRUCTOR;
								methodname = "<init>"; //$NON-NLS-1$
							}
							IMethodDescriptor descriptor = fType.getMethod(methodname, signature);
							processTags(descriptor, pruneTags(tags, method), getEnclosingType(method), member);
						}
						break;
					}
					case ASTNode.FIELD_DECLARATION: {
						FieldDeclaration field = (FieldDeclaration) parent;
						List fields = field.fragments();
						VariableDeclarationFragment fragment = null;
						for(Iterator iter = fields.iterator(); iter.hasNext();) {
							fragment = (VariableDeclarationFragment) iter.next();
							processTags(fType.getField(fragment.getName().getFullyQualifiedName()), pruneTags(tags, field), getEnclosingType(field), IApiJavadocTag.MEMBER_FIELD);
						}
						break;
					}
				}
			}
			return false;
		}
		
		private int getEnclosingType(ASTNode node) {
			ASTNode lnode = node;
			while (!(lnode instanceof AbstractTypeDeclaration)) {
				lnode = lnode.getParent();
			}
			if (lnode instanceof TypeDeclaration) {
				if (((TypeDeclaration)lnode).isInterface()) {
					return IApiJavadocTag.TYPE_INTERFACE;
				}
			}
			return IApiJavadocTag.TYPE_CLASS;
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
		
		/**
		 * Processes the tags for the given {@link IElementDescriptor}
		 * @param descriptor the descriptor
		 * @param tags the listing of tags from the AST
		 * @param type one of <code>CLASS</code> or <code>INTERFACE</code>
		 * @param member one of <code>METHOD</code> or <code>FIELD</code> or <code>NONE</code>
		 */
		protected void processTags(IElementDescriptor descriptor, List tags, int type, int member) {
			JavadocTagManager jtm = ApiPlugin.getJavadocTagManager();
			TagElement tag = null;
			String tagname = null;
			int restrictions = RestrictionModifiers.NO_RESTRICTIONS;
			for(Iterator iter = tags.iterator(); iter.hasNext();) {
				tag = (TagElement) iter.next();
				tagname = tag.getTagName();
				restrictions |= jtm.getRestrictionsForTag(tagname, type, member);
			}
			if (restrictions != RestrictionModifiers.NO_RESTRICTIONS) {
				IElementDescriptor ldesc = descriptor;
				if (ldesc.getElementType() == IElementDescriptor.METHOD) {
					try {
						ldesc = resolveMethod((IMethodDescriptor)ldesc);
					} catch (CoreException e) {
						fException = e;
					}
				}
				fDescription.setRestrictions(ldesc, restrictions);
			}
		}
		
		/**
		 * Method to post process returned flags from the {@link Javadoc} node of the element
		 * @param tags the tags to process
		 * @param element the {@link ASTNode} the tag appears on
		 * @return the list of valid tags to process restrictions for
		 */
		private List pruneTags(final List tags, ASTNode node) {
			ArrayList pruned = new ArrayList(tags.size());
			TagElement tag = null;
			switch(node.getNodeType()) {
				case ASTNode.TYPE_DECLARATION: {
					TypeDeclaration type = (TypeDeclaration) node;
					int flags = type.getModifiers();
					for (Iterator iterator = tags.iterator(); iterator.hasNext();) {
						tag = (TagElement) iterator.next();
						String tagname = tag.getTagName();
						if(type.isInterface() && 
								("@noextend".equals(tagname) || //$NON-NLS-1$
								"@noimplement".equals(tagname))) { //$NON-NLS-1$
							pruned.add(tag);
						}
						else {
							if("@noextend".equals(tagname)) { //$NON-NLS-1$
								if(!Flags.isFinal(flags)) {
									pruned.add(tag);
									continue;
								}
							}
							if("@noinstantiate".equals(tagname)) { //$NON-NLS-1$
								if(!Flags.isAbstract(flags)) {
									pruned.add(tag);
									continue;
								}
							}
						}
					}
					break;
				}
				case ASTNode.METHOD_DECLARATION: {
					MethodDeclaration method = (MethodDeclaration) node;
					int flags = method.getModifiers();
					for (Iterator iterator = tags.iterator(); iterator.hasNext();) {
						tag = (TagElement) iterator.next();
						if("@noreference".equals(tag.getTagName())) { //$NON-NLS-1$
							pruned.add(tag);
							continue;
						}
						if("@nooverride".equals(tag.getTagName())) { //$NON-NLS-1$
							ASTNode parent = method.getParent();
							int pflags = 0;
							if(parent instanceof BodyDeclaration) {
								pflags = ((BodyDeclaration)parent).getModifiers();
							}
							if(!Flags.isFinal(flags) && 
									!Flags.isStatic(flags) &&
									!Flags.isFinal(pflags)) {
								pruned.add(tag);
								continue;
							}
						}
					}
					break;
				}
				case ASTNode.FIELD_DECLARATION: {
					FieldDeclaration field = (FieldDeclaration) node;
					for (Iterator iterator = tags.iterator(); iterator.hasNext();) {
						tag = (TagElement) iterator.next();
						boolean isfinal = Flags.isFinal(field.getModifiers());
						if(isfinal || (isfinal && Flags.isStatic(field.getModifiers()))) {
							break;
						}
						if("@noreference".equals(tag.getTagName())) { //$NON-NLS-1$
							pruned.add(tag);
							break;
						}
					}
					break;
				}
			}
			return pruned;
		}
		
		/**
		 * Returns whether to continue processing children.
		 * 
		 * @return whether to continue processing children.
		 */
		private boolean isContinue() {
			return fException == null;
		}
		
		/**
		 * Returns an exception that aborted processing, or <code>null</code> if none.
		 * 
		 * @return an exception that aborted processing, or <code>null</code> if none
		 */
		CoreException getException() {
			return fException;
		}
		
		/* (non-Javadoc)
		 * @see org.eclipse.jdt.core.dom.ASTVisitor#visit(org.eclipse.jdt.core.dom.TypeDeclaration)
		 */
		public boolean visit(TypeDeclaration node) {
			if(isPrivate(node.getModifiers())) {
				return false;
			}
			enterType(node.getName());
			return isContinue();
		}
		/* (non-Javadoc)
		 * @see org.eclipse.jdt.core.dom.ASTVisitor#endVisit(org.eclipse.jdt.core.dom.TypeDeclaration)
		 */
		public void endVisit(TypeDeclaration node) {
			if(!isPrivate(node.getModifiers())) {
				exitType();
			}
		}
		/* (non-Javadoc)
		 * @see org.eclipse.jdt.core.dom.ASTVisitor#endVisit(org.eclipse.jdt.core.dom.AnnotationTypeDeclaration)
		 */
		public void endVisit(AnnotationTypeDeclaration node) {
			if(!isPrivate(node.getModifiers())) {
				exitType();
			}
		}
		/* (non-Javadoc)
		 * @see org.eclipse.jdt.core.dom.ASTVisitor#visit(org.eclipse.jdt.core.dom.AnnotationTypeDeclaration)
		 */
		public boolean visit(AnnotationTypeDeclaration node) {
			if(isPrivate(node.getModifiers())) {
				return false;
			}
			enterType(node.getName());
			return isContinue();
		}
		/* (non-Javadoc)
		 * @see org.eclipse.jdt.core.dom.ASTVisitor#visit(org.eclipse.jdt.core.dom.EnumDeclaration)
		 */
		public boolean visit(EnumDeclaration node) {
			if(isPrivate(node.getModifiers())) {
				return false;
			}
			enterType(node.getName());
			return isContinue();
		}
		/* (non-Javadoc)
		 * @see org.eclipse.jdt.core.dom.ASTVisitor#endVisit(org.eclipse.jdt.core.dom.EnumDeclaration)
		 */
		public void endVisit(EnumDeclaration node) {
			if(!isPrivate(node.getModifiers())) {
				exitType();
			}
		}
		/* (non-Javadoc)
		 * @see org.eclipse.jdt.core.dom.ASTVisitor#visit(org.eclipse.jdt.core.dom.PackageDeclaration)
		 */
		public boolean visit(PackageDeclaration node) {
			Name name = node.getName();
			fPackage = Factory.packageDescriptor(name.getFullyQualifiedName());
			return false;
		}
		/* (non-Javadoc)
		 * @see org.eclipse.jdt.core.dom.ASTVisitor#visit(org.eclipse.jdt.core.dom.MethodDeclaration)
		 */
		public boolean visit(MethodDeclaration node) {
			if(isPrivate(node.getModifiers())) {
				return false;
			}
			return isContinue();
		}
		/* (non-Javadoc)
		 * @see org.eclipse.jdt.core.dom.ASTVisitor#visit(org.eclipse.jdt.core.dom.FieldDeclaration)
		 */
		public boolean visit(FieldDeclaration node) {
			if(isPrivate(node.getModifiers())) {
				return false;
			}
			return isContinue();
		}
		private boolean isPrivate(int flags) {
			return Flags.isPrivate(flags);
		}
		/**
		 * Returns a method descriptor with a resolved signature for the given method
		 * descriptor with an unresolved signature.
		 * 
		 * @param descriptor method to resolve
		 * @return resolved method descriptor or the same method descriptor if unable to
		 * 	resolve
		 * @exception CoreException if unable to resolve the method and a class file
		 *  container was provided for this purpose
		 */
		private IMethodDescriptor resolveMethod(IMethodDescriptor descriptor) throws CoreException {
			if (fContainer != null) {
				IReferenceTypeDescriptor type = descriptor.getEnclosingType();
				IApiTypeRoot classFile = fContainer.findTypeRoot(type.getQualifiedName());
				if(classFile != null) {
					IApiType structure = classFile.getStructure();
					if(structure != null) {
						IApiMethod[] methods = structure.getMethods();
						for (int i = 0; i < methods.length; i++) {
							IApiMethod method = methods[i];
							if (descriptor.getName().equals(method.getName())) {
								String signature = method.getSignature();
								String descriptorSignature = descriptor.getSignature().replace('/', '.');
								if (Signatures.matchesSignatures(descriptorSignature, signature.replace('/', '.'))) {
									return descriptor.getEnclosingType().getMethod(method.getName(), signature);
								}
								String genericSignature = method.getGenericSignature();
								if (genericSignature != null) {
									if (Signatures.matchesSignatures(descriptorSignature, genericSignature.replace('/', '.'))) {
										return descriptor.getEnclosingType().getMethod(method.getName(), signature);
									}
								}
							}
						}
					}
				}
				throw new CoreException(new Status(IStatus.ERROR, ApiPlugin.PLUGIN_ID,
					MessageFormat.format("Unable to resolve method signature: {0}", new String[]{descriptor.toString()}), null)); //$NON-NLS-1$
			}
			return descriptor;
		}
		
	}

	/**
	 * The singleton instance of the scanner
	 */
	private static TagScanner fSingleton = null;
	
	/**
	 * Delegate for getting the singleton instance of the scanner
	 * @return
	 */
	public static final TagScanner newScanner() {
		if(fSingleton == null) {
			fSingleton = new TagScanner();
		}
		return fSingleton;
	}
	
	/**
	 * Constructor
	 * Cannot be instantiated
	 */
	private TagScanner() {}
	
	/**
	 * Scans the specified {@link ICompilationUnit} for contributed API Javadoc tags.
	 * Tags on methods will have unresolved signatures.
	 * @param unit the compilation unit source
	 * @param description the API description to annotate with any new tag rules found
	 * @param container optional class file container containing the class file for the given source
	 * 	that can be used to resolve method signatures if required (for tags on methods). If 
	 * 	not provided (<code>null</code>), method signatures will be unresolved.
	 * @param monitor
	 * 
	 * @throws CoreException
	 */
	public void scan(ICompilationUnit unit, IApiDescription description, IApiTypeContainer container, IProgressMonitor monitor) throws CoreException {
		scan(new CompilationUnit(unit), description, container, unit.getJavaProject().getOptions(true), monitor);
	}
	
	/**
	 * Scans the specified source {@linkplain CompilationUnit} for contributed API javadoc tags.
	 * Tags on methods will have unresolved signatures.
	 * 
	 * @param source the source file to scan for tags
	 * @param description the API description to annotate with any new tag rules found
	 * @param container optional class file container containing the class file for the given source
	 * 	that can be used to resolve method signatures if required (for tags on methods). If 
	 * 	not provided (<code>null</code>), method signatures will be unresolved.
	 * @param options a map of Java compiler options to use when creating the AST to scan
	 *  or <code>null</code> if default options should be used 
	 *  @param monitor
	 * 
	 * @throws CoreException 
	 */
	public void scan(CompilationUnit source, IApiDescription description, IApiTypeContainer container, Map options, IProgressMonitor monitor) throws CoreException {
		SubMonitor localmonitor = SubMonitor.convert(monitor, 2);
		ASTParser parser = ASTParser.newParser(AST.JLS4);
		InputStream inputStream = null;
		try {
			inputStream = source.getInputStream();
			parser.setSource(Util.getInputStreamAsCharArray(inputStream, -1, System.getProperty("file.encoding"))); //$NON-NLS-1$
		} catch (FileNotFoundException e) {
			throw new CoreException(new Status(IStatus.ERROR, ApiPlugin.PLUGIN_ID,
					MessageFormat.format("Compilation unit source not found: {0}", new String[]{source.getName()}), e)); //$NON-NLS-1$
		} catch (IOException e) {
			if (ApiPlugin.DEBUG_TAG_SCANNER) {
				System.err.println(source.getName());
			}
			throw new CoreException(new Status(IStatus.ERROR, ApiPlugin.PLUGIN_ID,
					MessageFormat.format("Error reading compilation unit: {0}", new String[]{source.getName()}), e)); //$NON-NLS-1$
		} finally {
			if (inputStream != null) {
				try {
					inputStream.close();
				} catch(IOException e) {
					ApiPlugin.log(e);
				}
			}
		}
		Util.updateMonitor(localmonitor);
		Map loptions = options;
		if(loptions == null) {
			loptions = JavaCore.getOptions();
		}
		loptions.put(JavaCore.COMPILER_DOC_COMMENT_SUPPORT, JavaCore.ENABLED);
		parser.setCompilerOptions(loptions);
		org.eclipse.jdt.core.dom.CompilationUnit cunit = (org.eclipse.jdt.core.dom.CompilationUnit) parser.createAST(localmonitor.newChild(1));
		Visitor visitor = new Visitor(description, container);
		cunit.accept(visitor);
		if (visitor.getException() != null) {
			throw visitor.getException();
		}
	}	
}
