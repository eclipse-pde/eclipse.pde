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
package org.eclipse.pde.api.tools.internal.search;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.Flags;
import org.eclipse.jdt.core.IField;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.ISourceRange;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.Signature;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.Position;
import org.eclipse.pde.api.tools.internal.provisional.ApiPlugin;
import org.eclipse.pde.api.tools.internal.provisional.IApiComponent;
import org.eclipse.pde.api.tools.internal.provisional.IApiDescription;
import org.eclipse.pde.api.tools.internal.provisional.ProfileModifiers;
import org.eclipse.pde.api.tools.internal.provisional.descriptors.IElementDescriptor;
import org.eclipse.pde.api.tools.internal.provisional.model.IApiBaseline;
import org.eclipse.pde.api.tools.internal.provisional.model.IApiElement;
import org.eclipse.pde.api.tools.internal.provisional.model.IApiField;
import org.eclipse.pde.api.tools.internal.provisional.model.IApiMember;
import org.eclipse.pde.api.tools.internal.provisional.model.IApiMethod;
import org.eclipse.pde.api.tools.internal.provisional.model.IApiType;
import org.eclipse.pde.api.tools.internal.provisional.model.IReference;
import org.eclipse.pde.api.tools.internal.provisional.problems.IApiProblem;
import org.eclipse.pde.api.tools.internal.provisional.problems.IApiProblemTypes;
import org.eclipse.pde.api.tools.internal.provisional.search.ReferenceModifiers;
import org.eclipse.pde.api.tools.internal.util.Util;

/**
 * Detects references to fields, methods and types that are not available for a specific EE.
 * 
 * @since 1.1
 */
public class SystemApiDetector extends AbstractProblemDetector {
	public SystemApiDetector() {
	}
	protected int getElementType(IReference reference) {
		IApiMember member = reference.getMember();
		switch(member.getType()) {
			case IApiElement.TYPE :
				return IElementDescriptor.TYPE;
			case IApiElement.METHOD :
				return IElementDescriptor.METHOD;
			case IApiElement.FIELD :
				return IElementDescriptor.FIELD;
			default :
				return 0;
		}
	}

	protected String[] getMessageArgs(IReference reference)
			throws CoreException {
		IApiMember resolvedReference = reference.getResolvedReference();
		IApiMember member = reference.getMember();
		String eeValue = getDisplay(member.getApiComponent().getLowestEEs());
		switch(resolvedReference.getType()) {
			case IApiElement.TYPE : {
				return new String[] {
						getSimpleTypeName(member),
						getSimpleTypeName(resolvedReference),
						eeValue,
				};
			}
			case IApiElement.FIELD : {
				IApiField field = (IApiField) resolvedReference;
				return new String[] {
						getSimpleTypeName(member),
						getSimpleTypeName(resolvedReference),
						field.getName(),
						eeValue,
				};
			}
			case IApiElement.METHOD : {
				IApiMethod method = (IApiMethod) resolvedReference;
				if (method.isConstructor()) {
					return new String[] {
							getSimpleTypeName(member),
							Signature.toString(method.getSignature(), getSimpleTypeName(method), null, false, false),
							eeValue,
					};
				} else {
					return new String[] {
							getSimpleTypeName(member),
							getSimpleTypeName(resolvedReference),
							Signature.toString(method.getSignature(), method.getName(), null, false, false),
							eeValue,
					};
				}
			}
			default :
				return null;
		}
	}

	protected int getProblemFlags(IReference reference) {
		IApiMember resolvedReference = reference.getResolvedReference();
		switch(resolvedReference.getType()) {
			case IApiElement.TYPE : {
				return IApiProblem.NO_FLAGS;
			}
			case IApiElement.METHOD : {
				IApiMethod method = (IApiMethod) resolvedReference;
				if (method.isConstructor()) {
					return IApiProblem.CONSTRUCTOR_METHOD;
				}
				return IApiProblem.METHOD;
			}
			case IApiElement.FIELD :
				return IApiProblem.FIELD;
			default :
				return 0;
		}
	}

	protected int getProblemKind() {
		return IApiProblem.INVALID_REFERENCE_IN_SYSTEM_LIBRARIES;
	}

	protected String[] getQualifiedMessageArgs(IReference reference)
			throws CoreException {
		IApiMember resolvedReference = reference.getResolvedReference();
		IApiMember member = reference.getMember();
		String eeValue = getDisplay(member.getApiComponent().getLowestEEs());
		switch(resolvedReference.getType()) {
			case IApiElement.TYPE : {
				return new String[] {
						getTypeName(member),
						getTypeName(resolvedReference),
						eeValue,
				};
			}
			case IApiElement.FIELD : {
				IApiField field = (IApiField) resolvedReference;
				return new String[] {
						getTypeName(member),
						getTypeName(resolvedReference),
						field.getName(),
						eeValue,
				};
			}
			case IApiElement.METHOD : {
				IApiMethod method = (IApiMethod) resolvedReference;
				if (method.isConstructor()) {
					return new String[] {
							getTypeName(member),
							Signature.toString(method.getSignature(), getSimpleTypeName(method), null, false, false),
							eeValue,
					};
				} else {
					return new String[] {
							getTypeName(member),
							getTypeName(resolvedReference),
							Signature.toString(method.getSignature(), method.getName(), null, false, false),
							eeValue,
					};
				}
			}
			default :
				return null;
		}
	}

	protected String getSeverityKey() {
		return IApiProblemTypes.INVALID_REFERENCE_IN_SYSTEM_LIBRARIES;
	}

	protected Position getSourceRange(IType type, IDocument document,
			IReference reference) throws CoreException, BadLocationException {
		IApiMember resolvedReference = reference.getResolvedReference();
		switch(resolvedReference.getType()) {
			case IApiElement.TYPE : {
				int linenumber = reference.getLineNumber();
				if (linenumber > 0) {
					linenumber--;
				}
				if (linenumber > 0) {
					int offset = document.getLineOffset(linenumber);
					String line = document.get(offset, document.getLineLength(linenumber));
					IApiType resolvedType = (IApiType) resolvedReference;
					String qname = resolvedType.getName();
					int first = line.indexOf(qname);
					if(first < 0) {
						qname = resolvedType.getSimpleName();
						first = line.indexOf(qname);
					}
					Position pos = null;
					if(first > -1) {
						pos = new Position(offset + first, qname.length());
					}
					else {
						//optimistically select the whole line since we can't find the correct variable name and we can't just select
						//the first occurrence
						pos = new Position(offset, line.length());
					}
					return pos;
				} else {
					// reference in a type declaration
					ISourceRange range = type.getNameRange();
					Position pos = null;
					if(range != null) {
						pos = new Position(range.getOffset(), range.getLength());
					}
					if(pos == null) {
						noSourcePosition(type, reference);
					}
					return pos;
				}
			}
			case IApiElement.FIELD : {
				int linenumber = reference.getLineNumber();
				if (linenumber > 0) {
					linenumber--;
				}
				if (linenumber > 0) {
					IApiField field = (IApiField) resolvedReference;
					String name = field.getName();
					int offset = document.getLineOffset(linenumber);
					String line = document.get(offset, document.getLineLength(linenumber));
					IApiType parent = field.getEnclosingType();
					String qname = parent.getName()+"."+name; //$NON-NLS-1$
					int first = line.indexOf(qname);
					if(first < 0) {
						qname = parent.getName()+"."+name; //$NON-NLS-1$
						first = line.indexOf(qname);
					}
					if(first < 0) {
						qname = "super."+name; //$NON-NLS-1$
						first = line.indexOf(qname);
					}
					if(first < 0) {
						qname = "this."+name; //$NON-NLS-1$
						first = line.indexOf(qname);
					}
					if(first < 0) {
						//try a pattern [.*fieldname] 
						//the field might be ref'd via a constant, e.g. enum constant
						int idx = line.indexOf(name);
						while(idx > -1) {
							if(line.charAt(idx-1) == '.') {
								first = idx;
								qname = name;
								break;
							}
							idx = line.indexOf(name, idx+1);
						}
					}
					Position pos = null;
					if(first > -1) {
						pos = new Position(offset + first, qname.length());
					}
					else {
						//optimistically select the whole line since we can't find the correct variable name and we can't just select
						//the first occurrence
						pos = new Position(offset, line.length());
					}
					return pos;
				} else {
					// reference in a field declaration
					IApiField field = (IApiField) reference.getMember();
					IField javaField = type.getField(field.getName());
					Position pos = null;
					if (javaField.exists()) {
						ISourceRange range = javaField.getNameRange();
						if(range != null) {
							pos = new Position(range.getOffset(), range.getLength()); 
						}
					}
					if(pos == null) {
						noSourcePosition(type, reference);
					}
					return pos;
				}
			}
			case IApiElement.METHOD : {
				if (reference.getLineNumber() >= 0) {
					IApiMethod method = (IApiMethod) resolvedReference;
					String name = method.getName();
					if(method.isConstructor()) {
						name = getSimpleTypeName(method);
					}
					Position pos = getMethodNameRange(name, document, reference);
					if(pos == null) {
						noSourcePosition(type, reference);
					}
					return pos;
				} else {
					// reference in a method declaration
					IApiMethod method = (IApiMethod) reference.getMember();
					String[] parameterTypes = Signature.getParameterTypes(method.getSignature());
					for (int i = 0; i < parameterTypes.length; i++) {
						parameterTypes[i] = parameterTypes[i].replace('/', '.');
					}
					String methodname = method.getName();
					if(method.isConstructor()) {
						IApiType enclosingType = method.getEnclosingType();
						if (enclosingType.isMemberType() && !Flags.isStatic(enclosingType.getModifiers())) {
							// remove the synthetic argument that corresponds to the enclosing type
							int length = parameterTypes.length - 1;
							System.arraycopy(parameterTypes, 1, (parameterTypes = new String[length]), 0, length);
						}
						methodname = enclosingType.getSimpleName();
					}
					IMethod Qmethod = type.getMethod(methodname, parameterTypes);
					IMethod[] methods = type.getMethods();
					IMethod match = null;
					for (int i = 0; i < methods.length; i++) {
						IMethod m = methods[i];
						if (m.isSimilar(Qmethod)) {
							match = m;
							break;
						}
					}
					Position pos = null;
					if (match != null) {
						ISourceRange range = match.getNameRange();
						if(range != null) {
							pos = new Position(range.getOffset(), range.getLength());
						}
					}
					if(pos == null) {
						noSourcePosition(type, reference);
					}
					return pos;
				}
			}
			default :
				return null;
		}
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.api.tools.internal.search.AbstractProblemDetector#isProblem(org.eclipse.pde.api.tools.internal.provisional.model.IReference)
	 */
	protected boolean isProblem(IReference reference) {
		// the reference must be in the system library
		IApiMember member = reference.getMember();
		String[] lowestEEs = member.getApiComponent().getLowestEEs();
		for (int i = 0, max = lowestEEs.length; i < max; i++) {
			String lowestEE = lowestEEs[i];
			int eeValue = ProfileModifiers.getValue(lowestEE); 
			if (eeValue == ProfileModifiers.NO_PROFILE_VALUE) {
				return false;
			}
			try {
				if (!ProfileModifiers.isJRE(eeValue)) {
					return false;
				}
				IElementDescriptor elementDescriptor = reference.getResolvedReference().getHandle();
				IApiDescription systemApiDescription = member.getApiComponent().getSystemApiDescription(eeValue);
				boolean value = !Util.isAPI(eeValue, elementDescriptor, systemApiDescription);
				if (value) {
					return true;
				}
			} catch (CoreException e) {
				ApiPlugin.log(e);
			}
		}
		return false;
	}
	public boolean considerReference(IReference reference) {
		IApiComponent apiComponent = reference.getMember().getApiComponent();
		IApiBaseline baseline = apiComponent.getBaseline();
		if (baseline == null) return false;
		String referencedTypeName = reference.getReferencedTypeName();
		// extract the package name
		int index = referencedTypeName.lastIndexOf('.');
		if (index == -1) return false;
		try {
			IApiComponent[] resolvePackages = baseline.resolvePackage(apiComponent, referencedTypeName.substring(0, index));
			switch(resolvePackages.length) {
				case 1 :
					if (resolvePackages[0].isSystemComponent()) {
						retainReference(reference);
						return true;
					}
			}
		} catch (CoreException e) {
			ApiPlugin.log(e);
		}
		return false;
	}

	public int getReferenceKinds() {
		return ReferenceModifiers.MASK_REF_ALL;
	}
	
	private String getDisplay(String[] eeValues) {
		StringBuffer buffer = new StringBuffer();
		for (int i = 0; i < eeValues.length; i++) {
			String eeValue = eeValues[i];
			if (i > 0) {
				buffer.append(", "); //$NON-NLS-1$
			}
			buffer.append(eeValue);
		}
		return String.valueOf(buffer);
	}
}
