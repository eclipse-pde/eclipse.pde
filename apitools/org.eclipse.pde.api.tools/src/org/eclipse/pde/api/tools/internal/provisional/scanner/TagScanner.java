/*******************************************************************************
 * Copyright (c) 2007, 2008 IBM Corporation and others.
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
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Status;
import org.eclipse.jdt.core.Signature;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.AbstractTypeDeclaration;
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
import org.eclipse.pde.api.tools.internal.provisional.IClassFile;
import org.eclipse.pde.api.tools.internal.provisional.IClassFileContainer;
import org.eclipse.pde.api.tools.internal.provisional.RestrictionModifiers;
import org.eclipse.pde.api.tools.internal.provisional.descriptors.IElementDescriptor;
import org.eclipse.pde.api.tools.internal.provisional.descriptors.IMethodDescriptor;
import org.eclipse.pde.api.tools.internal.provisional.descriptors.IPackageDescriptor;
import org.eclipse.pde.api.tools.internal.provisional.descriptors.IReferenceTypeDescriptor;
import org.eclipse.pde.api.tools.internal.search.MethodExtractor;
import org.eclipse.pde.api.tools.internal.util.HashtableOfInt;
import org.eclipse.pde.api.tools.internal.util.Util;
import org.objectweb.asm.ClassReader;

/**
 * Scans the source of a *.java file for the javadoc tags on types that have been contributed to the apiJavadocTags extension point:
 * @since 1.0.0
 */
public class TagScanner {

	/**
	 * Constant used for controlling tracing in the scanner
	 */
	private static boolean DEBUG = Util.DEBUG;
	
	/**
	 * Method used for initializing tracing in the scanner
	 */
	public static void setDebug(boolean debugValue) {
		DEBUG = debugValue || Util.DEBUG;
	}
	
	/**
	 * Visitor to scan a compilation unit. We only care about javadoc nodes that have either 
	 * type or enum declarations as parents, so we have to override the ones we don't care 
	 * about.
	 */
	class Visitor extends ASTVisitor {
		
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
		private IClassFileContainer fContainer = null;
		
		/**
		 * Cache of class files to maps of unresolved method descriptors to resolved descriptors.
		 */
		private Map fMethodMappings = null;
		
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
		public Visitor(IApiDescription description, IClassFileContainer container) {
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
					case ASTNode.ENUM_DECLARATION: {
						processTags(fType, tags, IApiJavadocTag.TYPE_CLASS, IApiJavadocTag.MEMBER_NONE);
						break;
					}
					case ASTNode.TYPE_DECLARATION: {
						TypeDeclaration type = (TypeDeclaration) parent;
						if(type.isInterface()) {
							processTags(fType, tags, IApiJavadocTag.TYPE_INTERFACE, IApiJavadocTag.MEMBER_NONE);
						}
						else {
							processTags(fType, tags, IApiJavadocTag.TYPE_CLASS, IApiJavadocTag.MEMBER_NONE);
						}
						break;
					}
					case ASTNode.METHOD_DECLARATION: {
						MethodDeclaration method = (MethodDeclaration) parent;
						String signature = Util.getMethodSignatureFromNode(method);
						if(signature != null) {
							IMethodDescriptor descriptor = fType.getMethod(method.getName().getFullyQualifiedName(), signature);
							processTags(descriptor, tags, getEnclosingType(method), IApiJavadocTag.MEMBER_METHOD);
						}
						break;
					}
					case ASTNode.FIELD_DECLARATION: {
						FieldDeclaration field = (FieldDeclaration) parent;
						List fields = field.fragments();
						VariableDeclarationFragment fragment = null;
						for(Iterator iter = fields.iterator(); iter.hasNext();) {
							fragment = (VariableDeclarationFragment) iter.next();
							processTags(fType.getField(fragment.getName().getFullyQualifiedName()), tags, getEnclosingType(field), IApiJavadocTag.MEMBER_FIELD);
						}
						break;
					}
				}
			}
			return false;
		}
		
		private int getEnclosingType(ASTNode node) {
			while (!(node instanceof AbstractTypeDeclaration)) {
				node = node.getParent();
			}
			if (node instanceof TypeDeclaration) {
				if (((TypeDeclaration)node).isInterface()) {
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
				if (descriptor.getElementType() == IElementDescriptor.T_METHOD) {
					try {
						descriptor = resolveMethod((IMethodDescriptor)descriptor);
					} catch (CoreException e) {
						fException = e;
					}
				}
				fDescription.setRestrictions(null, descriptor, restrictions);
			}
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
			enterType(node.getName());
			return isContinue();
		}
		/* (non-Javadoc)
		 * @see org.eclipse.jdt.core.dom.ASTVisitor#endVisit(org.eclipse.jdt.core.dom.TypeDeclaration)
		 */
		public void endVisit(TypeDeclaration node) {
			exitType();
		}
		/* (non-Javadoc)
		 * @see org.eclipse.jdt.core.dom.ASTVisitor#visit(org.eclipse.jdt.core.dom.EnumDeclaration)
		 */
		public boolean visit(EnumDeclaration node) {
			enterType(node.getName());
			return isContinue();
		}
		/* (non-Javadoc)
		 * @see org.eclipse.jdt.core.dom.ASTVisitor#endVisit(org.eclipse.jdt.core.dom.EnumDeclaration)
		 */
		public void endVisit(EnumDeclaration node) {
			exitType();
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
			return isContinue();
		}
		/* (non-Javadoc)
		 * @see org.eclipse.jdt.core.dom.ASTVisitor#visit(org.eclipse.jdt.core.dom.FieldDeclaration)
		 */
		public boolean visit(FieldDeclaration node) {
			return isContinue();
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
				IClassFile classFile = fContainer.findClassFile(type.getQualifiedName());
				if(classFile != null) {
					Map methodMapping = getMethodMapping(classFile);
					Object object = methodMapping.get(descriptor.getName());
					if (object == null) {
						throw new CoreException(new Status(IStatus.ERROR, ApiPlugin.PLUGIN_ID,
								MessageFormat.format("Unable to resolve method signature: {0}", new String[]{descriptor.toString()}), null)); //$NON-NLS-1$
					}
					if (object instanceof IMethodDescriptor) {
						return (IMethodDescriptor) object;
					}
					if (object instanceof HashtableOfInt) {
						HashtableOfInt hashtableOfInt = (HashtableOfInt) object;
						int numberOfParameters = Signature.getParameterCount(descriptor.getSignature());
						Object object2 = hashtableOfInt.get(numberOfParameters);
						if (object2 instanceof IMethodDescriptor) {
							return (IMethodDescriptor) object2;
						}
						// this is a list of method descriptors and we need to find the better match
						List methodList = (List) object2;
						for (Iterator iterator = methodList.iterator(); iterator.hasNext(); ) {
							IMethodDescriptor methodDescriptor = (IMethodDescriptor) iterator.next();
							if (matches(descriptor, methodDescriptor)) {
								return methodDescriptor;
							}
						}
					}
				}
				throw new CoreException(new Status(IStatus.ERROR, ApiPlugin.PLUGIN_ID,
					MessageFormat.format("Unable to resolve method signature: {0}", new String[]{descriptor.toString()}), null)); //$NON-NLS-1$
			}
			return descriptor;
		}
		
		private boolean matches(IMethodDescriptor descriptor, IMethodDescriptor methodDescriptor) {
			String signature = descriptor.getSignature();
			String signature2 = methodDescriptor.getSignature();
			return Util.matchesSignatures(signature, signature2);
		}
		/**
		 * Returns resolved method descriptors from the given class file.
		 * 
		 * @param file class file
		 * @return method descriptors for all methods in the file
		 * @throws CoreException
		 */
		private IMethodDescriptor[] getMethods(IClassFile file) throws CoreException {
			MethodExtractor extractor = new MethodExtractor();
			ClassReader reader = new ClassReader(file.getContents());
			reader.accept(extractor, ClassReader.SKIP_CODE | ClassReader.SKIP_FRAMES);
			return extractor.getMethods();
		}
		
		
		/**
		 * Returns a map of unresolved methods descriptors to resolved method descriptors
		 * for the methods in the given class file.
		 * 
		 * @param file class file
		 * @return mapping of unresolved methods to resolved methods
		 * @throws CoreException 
		 */
		private Map getMethodMapping(IClassFile file) throws CoreException {
			if (fMethodMappings == null) {
				fMethodMappings = new HashMap();
			}
			Map mapping = (Map) fMethodMappings.get(file);
			if (mapping == null) {
				mapping = new HashMap();
				IMethodDescriptor[] methods = getMethods(file);
				for (int i = 0; i < methods.length; i++) {
					IMethodDescriptor resolved = methods[i];
					String selector = resolved.getName();
					Object methodsCache = mapping.get(selector);
					if (methodsCache != null) {
						// already an existing method with the same selector
						int numberOfParameter = Signature.getParameterCount(resolved.getSignature());
						if (methodsCache instanceof HashtableOfInt) {
							HashtableOfInt hashtableOfInt = (HashtableOfInt) methodsCache;
							Object object = hashtableOfInt.get(numberOfParameter);
							if (object == null) {
								// first method with this name and number of parameters
								hashtableOfInt.put(numberOfParameter, resolved);
							} else if (object instanceof List) {
								// already more than one method with this name and number of parameters
								List existingMethodsList = (List) object;
								existingMethodsList.add(resolved);
							} else {
								// insert the second method with this name and number of parameters
								List methodsList = new ArrayList();
								methodsList.add(object);
								methodsList.add(resolved);
								hashtableOfInt.put(numberOfParameter, methodsList);
							}
						} else {
							// this is a IMethodDescriptor
							IMethodDescriptor previousMethod = (IMethodDescriptor) methodsCache;
							HashtableOfInt hashtableOfInt = new HashtableOfInt();
							int numberOfParametersForPrevious = Signature.getParameterCount(previousMethod.getSignature());
							if (numberOfParametersForPrevious != numberOfParameter) {
								hashtableOfInt.put(numberOfParameter, resolved);
								hashtableOfInt.put(numberOfParametersForPrevious, previousMethod);
							} else {
								List methodsList = new ArrayList();
								methodsList.add(previousMethod);
								methodsList.add(resolved);
								hashtableOfInt.put(numberOfParameter, methodsList);
							}
							mapping.put(selector, hashtableOfInt);
						}
					} else {
						// we insert the IMethodDescriptor in the map
						mapping.put(selector, resolved);
					}
				}
				fMethodMappings.put(file, mapping);
			}
			return mapping;
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
	 * Scans the specified source {@linkplain CompilationUnit} for contributed API javadoc tags.
	 * Tags on methods will have unresolved signatures.
	 * 
	 * @param source the source file to scan for tags
	 * @param description the API description to annotate with any new tag rules found
	 * @throws CoreException
	 */
	public void scan(CompilationUnit source, IApiDescription description) throws CoreException {
		scan(source, description, null);
	}
	
	/**
	 * Scans the specified source {@linkplain CompilationUnit} for contributed API javadoc tags.
	 * 
	 * @param source the source file to scan for tags
	 * @param description the API description to annotate with any new tag rules found
	 * @param container optional class file container containing the class file for the given source
	 * 	that can be used to resolve method signatures if required (for tags on methods). If 
	 * 	not provided (<code>null</code>), method signatures will be unresolved.
	 * @throws CoreException 
	 */
	public void scan(CompilationUnit source, IApiDescription description, IClassFileContainer container) throws CoreException {
		ASTParser parser = ASTParser.newParser(AST.JLS3);
		InputStream inputStream = null;
		try {
			inputStream = source.getInputStream();
			parser.setSource(Util.getInputStreamAsCharArray(inputStream, -1, System.getProperty("file.encoding"))); //$NON-NLS-1$
		} catch (FileNotFoundException e) {
			throw new CoreException(new Status(IStatus.ERROR, ApiPlugin.PLUGIN_ID,
					MessageFormat.format("Compilation unit source not found: {0}", new String[]{source.getName()}), e)); //$NON-NLS-1$
		} catch (IOException e) {
			if (DEBUG) {
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
		org.eclipse.jdt.core.dom.CompilationUnit cunit = (org.eclipse.jdt.core.dom.CompilationUnit) parser.createAST(new NullProgressMonitor());
		Visitor visitor = new Visitor(description, container);
		cunit.accept(visitor);
		if (visitor.getException() != null) {
			throw visitor.getException();
		}
	}	
}
