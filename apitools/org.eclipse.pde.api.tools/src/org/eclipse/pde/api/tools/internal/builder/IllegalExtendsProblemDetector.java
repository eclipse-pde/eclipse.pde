/*******************************************************************************
 * Copyright (c) 2008, 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.api.tools.internal.builder;

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
import org.eclipse.pde.api.tools.internal.provisional.model.IApiMethod;
import org.eclipse.pde.api.tools.internal.provisional.model.IApiType;
import org.eclipse.pde.api.tools.internal.provisional.problems.IApiProblem;
import org.eclipse.pde.api.tools.internal.provisional.problems.IApiProblemTypes;
import org.eclipse.pde.api.tools.internal.util.Signatures;

/**
 * Detects when a type illegally extends another type.
 * 
 * @since 1.1
 */
public class IllegalExtendsProblemDetector extends AbstractIllegalTypeReference {
	
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
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.api.tools.internal.provisional.search.IApiProblemDetector#getReferenceKinds()
	 */
	public int getReferenceKinds() {
		return IReference.REF_EXTENDS;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.api.tools.internal.search.AbstractIllegalTypeReference#getProblemKind()
	 */
	protected int getProblemKind() {
		return IApiProblem.ILLEGAL_EXTEND;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.api.tools.internal.builder.AbstractIllegalTypeReference#getProblemFlags(org.eclipse.pde.api.tools.internal.provisional.builder.IReference)
	 */
	protected int getProblemFlags(IReference reference) {
		IApiType type = (IApiType) reference.getMember();
		if(type.isLocal()) {
			return IApiProblem.LOCAL_TYPE;
		}
		if(type.isAnonymous()) {
			return IApiProblem.ANONYMOUS_TYPE;
		}
		return super.getProblemFlags(reference);
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.api.tools.internal.search.AbstractIllegalTypeReference#getSeverityKey()
	 */
	protected String getSeverityKey() {
		return IApiProblemTypes.ILLEGAL_EXTEND;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.api.tools.internal.builder.AbstractIllegalTypeReference#getMessageArgs(org.eclipse.pde.api.tools.internal.provisional.builder.IReference)
	 */
	protected String[] getMessageArgs(IReference reference) throws CoreException {
		ApiType ltype = (ApiType) reference.getMember();
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
		return super.getMessageArgs(reference);
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.api.tools.internal.builder.AbstractIllegalTypeReference#getQualifiedMessageArgs(org.eclipse.pde.api.tools.internal.provisional.builder.IReference)
	 */
	protected String[] getQualifiedMessageArgs(IReference reference) throws CoreException {
		ApiType ltype = (ApiType) reference.getMember();
		if(ltype.isLocal() || ltype.isAnonymous()) {
			return this.getMessageArgs(reference);
		}
		return super.getQualifiedMessageArgs(reference);
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.api.tools.internal.builder.AbstractIllegalTypeReference#getSourceRange(org.eclipse.jdt.core.IType, org.eclipse.jface.text.IDocument, org.eclipse.pde.api.tools.internal.provisional.builder.IReference)
	 */
	protected Position getSourceRange(IType type, IDocument doc, IReference reference) throws CoreException, BadLocationException {
		ApiType ltype = (ApiType) reference.getMember();
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
		return super.getSourceRange(type, doc, reference);
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
	private IMethod getEnclosingMethod(final IType jtype, IReference reference, IDocument document) throws CoreException { 
		ApiType type = (ApiType) reference.getMember();
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
		return null;
	}
	private IMethod quickLookup(final IType jtype, IDocument document, IReference reference, int offset) throws JavaModelException {
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
}
