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

import java.util.HashMap;
import java.util.Map;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.ISourceRange;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.Signature;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.AnonymousClassDeclaration;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.Position;
import org.eclipse.pde.api.tools.internal.model.ApiType;
import org.eclipse.pde.api.tools.internal.provisional.builder.IReference;
import org.eclipse.pde.api.tools.internal.provisional.descriptors.IElementDescriptor;
import org.eclipse.pde.api.tools.internal.provisional.descriptors.IReferenceTypeDescriptor;
import org.eclipse.pde.api.tools.internal.provisional.model.IApiElement;
import org.eclipse.pde.api.tools.internal.provisional.model.IApiMember;
import org.eclipse.pde.api.tools.internal.provisional.model.IApiMethod;
import org.eclipse.pde.api.tools.internal.provisional.model.IApiType;
import org.eclipse.pde.api.tools.internal.provisional.problems.IApiProblem;
import org.eclipse.pde.api.tools.internal.util.Signatures;


/**
 * Base implementation of a problem detector for type references
 * 
 * @since 1.1
 * @noextend This class is not intended to be subclassed by clients.
 */
public abstract class AbstractIllegalTypeReference extends AbstractProblemDetector {

	/**
	 * Class used to look up the name of the enclosing method for an {@link IApiType} when we do not have any 
	 * enclosing method infos (pre Java 1.5 class files 
	 */
	class MethodFinder extends ASTVisitor {
		IMethod method = null;
		private IType jtype = null;
		private ApiType type = null;
		
		public MethodFinder(ApiType type, IType jtype) {
			this.type = type;
			this.jtype = jtype;
		}
		public boolean visit(AnonymousClassDeclaration node) {
			if(method == null) {
				ITypeBinding binding = node.resolveBinding();
				String binaryName = binding.getBinaryName();
				if(type.getName().endsWith(binaryName)) {
					try {
						IJavaElement element = jtype.getCompilationUnit().getElementAt(node.getStartPosition());
						if(element != null) {
							IJavaElement ancestor = element.getAncestor(IJavaElement.METHOD);
							if(ancestor != null) {
								method = (IMethod) ancestor;
							}
						}
					}
					catch(JavaModelException jme) {}
					return false;
				}
			}
			return true;
		}
		public boolean visit(TypeDeclaration node) {
			if(method == null && node.isLocalTypeDeclaration()) {
				ITypeBinding binding = node.resolveBinding();
				String binaryName = binding.getBinaryName();
				if(type.getName().endsWith(binaryName)) {
					try {
						IJavaElement element = jtype.getCompilationUnit().getElementAt(node.getStartPosition());
						if(element.getElementType() == IJavaElement.TYPE) {
							IType ltype = (IType) element;
							IJavaElement parent = ltype.getParent();
							if(parent.getElementType() == IJavaElement.METHOD) {
								method = (IMethod) parent;
							}
						}
					}
					catch(JavaModelException jme) {}
					return false;
				}
			}
			return true;
		}
	};
	
	/**
	 * Map of fully qualified type names to associated component IDs that
	 * represent illegal references 
	 */
	private Map fIllegalTypes = new HashMap();
	
	/**
	 * Adds the given type as not to be extended.
	 * 
	 * @param type a type that is marked no extend
	 * @param componentId the component the type is located in
	 */
	void addIllegalType(IReferenceTypeDescriptor type, String componentId) {
		fIllegalTypes.put(type.getQualifiedName(), componentId);
	}	
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.api.tools.internal.provisional.search.IApiProblemDetector#considerReference(org.eclipse.pde.api.tools.internal.provisional.model.IReference)
	 */
	public boolean considerReference(IReference reference) {
		if (super.considerReference(reference) && fIllegalTypes.containsKey(reference.getReferencedTypeName())) {
			retainReference(reference);
			return true;
		}
		return false;
	}	
	
	/**
	 * Returns if the mapping contains the referenced type name
	 * @param reference
	 * @return true of the mapping contains the key false otherwise
	 */
	protected boolean isIllegalType(IReference reference) {
		return fIllegalTypes.containsKey(reference.getReferencedTypeName());
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.api.tools.internal.search.AbstractProblemDetector#isProblem(org.eclipse.pde.api.tools.internal.provisional.model.IReference)
	 */
	protected boolean isProblem(IReference reference) {
		if(!super.isProblem(reference)) {
			return false;
		}
		IApiMember type = reference.getResolvedReference();
		Object componentId = fIllegalTypes.get(type.getName());
		return isReferenceFromComponent(reference, componentId);
	}
	
	/**
	 * Returns the enclosing {@link IMethod} for the given type or <code>null</code>
	 * if it cannot be computed
	 * @param type
	 * @param jtype
	 * @param reference
	 * @param document
	 * @return the {@link IMethod} enclosing the given type or <code>null</code>
	 * @throws CoreException
	 */
	protected IMethod getEnclosingMethod(final IType jtype, IReference reference, IDocument document) throws CoreException { 
		IApiMember member = reference.getMember();
		if((member.getType() == IApiElement.TYPE)) {
			ApiType type = (ApiType) member;
			IApiMethod apimethod = type.getEnclosingMethod();
			if(apimethod != null) {
				String signature = Signatures.processMethodSignature(apimethod);
				String methodname = Signatures.getMethodName(apimethod);
				IMethod method = jtype.getMethod(methodname, Signature.getParameterTypes(signature));
				if(method.exists()) {
					return method;
				}
			}
			else {
				//try to look it up
				IMethod method = null;
				if(reference.getLineNumber() > -1) {
					try {
						int offset = document.getLineOffset(reference.getLineNumber());
						method = quickLookup(jtype, document, reference, offset);
					}
					catch(BadLocationException ble) {}
				}
				if(method == null) {
					//look it up the hard way
					ISourceRange range = jtype.getCompilationUnit().getSourceRange();
					ASTParser parser = ASTParser.newParser(AST.JLS4);
					parser.setSource(jtype.getCompilationUnit());
					parser.setSourceRange(range.getOffset(), range.getLength());
					parser.setResolveBindings(true);
					ASTNode ptype = parser.createAST(null);
					MethodFinder finder = new MethodFinder(type, jtype);
					ptype.accept(finder);
					method = finder.method;
				}
				if(method != null && method.exists()) {
					ApiType etype = (ApiType) type.getEnclosingType();
					IApiMethod[] methods = etype.getMethods();
					String msig = null;
					for (int i = 0; i < methods.length; i++) {
						msig = methods[i].getSignature();
						if(Signatures.getMethodName(methods[i]).equals(method.getElementName()) &&
								Signatures.matchesSignatures(msig.replace('/', '.'), method.getSignature())) {
							type.setEnclosingMethodInfo(methods[i].getName(), msig);
						}
					}
					return method;
				}
			}
		}
		return null;
	}
	
	/**
	 * Performs a quick look-up using the offset into the the {@link ICompilationUnit}
	 * @param jtype
	 * @param document
	 * @param reference
	 * @param offset
	 * @return
	 * @throws JavaModelException
	 */
	protected IMethod quickLookup(final IType jtype, IDocument document, IReference reference, int offset) throws JavaModelException {
		if(offset > -1) {
			IJavaElement element = jtype.getCompilationUnit().getElementAt(offset);
			if(element != null) {
				IJavaElement ancestor = element.getAncestor(IJavaElement.METHOD);
				if (ancestor != null) {
					return (IMethod) ancestor;
				}
			}
		}
		return null;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.api.tools.internal.search.AbstractProblemDetector#getSourceRange(org.eclipse.jdt.core.IType, org.eclipse.jface.text.IDocument, org.eclipse.pde.api.tools.internal.provisional.model.IReference)
	 */
	protected Position getSourceRange(IType type, IDocument doc, IReference reference) throws CoreException, BadLocationException {
		IApiMember member = reference.getMember();
		if(member.getType() == IApiElement.TYPE) {
			ApiType ltype = (ApiType) member;
			IMethod method = null;
			if(ltype.isAnonymous()) {
				// has a side-effect on reference.getMember().setEnclosingMethodInfo(..)
				getEnclosingMethod(type, reference, doc);
				if(reference.getLineNumber() < 0) {
					return defaultSourcePosition(type, reference);
				}
				String name = getSimpleTypeName(reference.getResolvedReference());
				Position pos = getMethodNameRange(true, name, doc, reference);
				if(pos == null) {
					return defaultSourcePosition(type, reference);
				}
				return pos;
			}
			if(ltype.isLocal()) {
				String name = ltype.getSimpleName();
				ICompilationUnit cunit = type.getCompilationUnit();
				if(cunit.isWorkingCopy()) {
					cunit.reconcile(AST.JLS4, false, null, null);
				}
				IType localtype = type;
				method = getEnclosingMethod(type, reference, doc);
				if(method != null) {
					localtype = method.getType(name, 1);
				}
				if(localtype.exists()) {
					ISourceRange range = localtype.getNameRange();
					return new Position(range.getOffset(), range.getLength());
				}
				return defaultSourcePosition(type, reference);
			}
		}
		ISourceRange range = type.getNameRange();
		Position pos = null;
		if(range != null) {
			pos = new Position(range.getOffset(), range.getLength());
		}
		if(pos == null) {
			return defaultSourcePosition(type, reference);
		}
		return pos; 
	}	
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.api.tools.internal.search.AbstractProblemDetector#getElementType(org.eclipse.pde.api.tools.internal.provisional.model.IReference)
	 */
	protected int getElementType(IReference reference) {
		return IElementDescriptor.TYPE;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.api.tools.internal.search.AbstractProblemDetector#getMessageArgs(org.eclipse.pde.api.tools.internal.provisional.model.IReference)
	 */
	protected String[] getMessageArgs(IReference reference) throws CoreException {
		IApiMember member = reference.getMember();
		if(member.getType() == IApiElement.TYPE) {
			ApiType ltype = (ApiType) member;
			String simpleTypeName = getSimpleTypeName(reference.getResolvedReference());
			if(ltype.isAnonymous()) {
				IApiType etype = ltype.getEnclosingType();
				String signature = Signatures.getQualifiedTypeSignature(etype);
				IApiMethod method = ltype.getEnclosingMethod();
				if(method != null) {
					signature = Signatures.getQualifiedMethodSignature(method);
				}
				return new String[] {signature, simpleTypeName};
			}
			if(ltype.isLocal()) {
				//local types are always defined in methods, include enclosing method infos in message
				IApiType etype = ltype.getEnclosingType(); 
				IApiMethod method = ltype.getEnclosingMethod();
				if(method != null) {
					String methodsig = Signatures.getQualifiedMethodSignature(method);
					return new String[] {
							Signatures.getAnonymousTypeName(ltype.getName()),
							methodsig,
							simpleTypeName
					};
				}
				else {
					return new String[] {
							Signatures.getAnonymousTypeName(ltype.getName()), 
							getSimpleTypeName(etype), 
							simpleTypeName};
				}
			}
		}
		return new String[] {
				getSimpleTypeName(reference.getResolvedReference()), 
				getSimpleTypeName(reference.getMember())};
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.api.tools.internal.search.AbstractProblemDetector#getQualifiedMessageArgs(org.eclipse.pde.api.tools.internal.provisional.model.IReference)
	 */
	protected String[] getQualifiedMessageArgs(IReference reference) throws CoreException {
		IApiMember member = reference.getMember();
		if(member.getType() == IApiElement.TYPE) {
			ApiType ltype = (ApiType) member;
			if(ltype.isLocal() || ltype.isAnonymous()) {
				return getMessageArgs(reference);
			}
		}
		return new String[] {
				getQualifiedTypeName(reference.getResolvedReference()), 
				getQualifiedTypeName(reference.getMember())};
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.api.tools.internal.search.AbstractProblemDetector#getProblemFlags(org.eclipse.pde.api.tools.internal.provisional.model.IReference)
	 */
	protected int getProblemFlags(IReference reference) {
		IApiMember member = reference.getMember();
		if(member.getType() == IApiElement.TYPE) {
			IApiType type = (IApiType) reference.getMember();
			if(type.isLocal()) {
				return IApiProblem.LOCAL_TYPE;
			}
			if(type.isAnonymous()) {
				return IApiProblem.ANONYMOUS_TYPE;
			}
		}
		return IApiProblem.NO_FLAGS;
	}
}
