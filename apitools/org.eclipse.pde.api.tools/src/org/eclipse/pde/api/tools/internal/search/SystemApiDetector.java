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
		String eeValue = member.getApiComponent().getLowestEE();
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
				String methodName = method.getName();
				if (method.isConstructor()) {
					methodName = getSimpleTypeName(method);
				}
				return new String[] {
						getSimpleTypeName(member),
						getSimpleTypeName(resolvedReference),
						Signature.toString(method.getSignature(), methodName, null, false, false),
						eeValue,
				};
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
		String eeValue = member.getApiComponent().getLowestEE();
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
				String methodName = method.getName();
				if (method.isConstructor()) {
					methodName = getSimpleTypeName(method);
				}
				return new String[] {
						getTypeName(member),
						getTypeName(resolvedReference),
						Signature.toString(method.getSignature(), methodName, null, false, false),
						eeValue,
				};
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
			}
			case IApiElement.FIELD : {
				IApiField field = (IApiField) resolvedReference;
				String name = field.getName();
				int linenumber = reference.getLineNumber();
				if (linenumber > 0) {
					linenumber--;
				}
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
			}
			case IApiElement.METHOD : {
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
		int eeValue = ProfileModifiers.getValue(member.getApiComponent().getLowestEE()); 
		if (eeValue == ProfileModifiers.NO_PROFILE_VALUE) {
			return false;
		}
		try {
			IElementDescriptor elementDescriptor = reference.getResolvedReference().getHandle();
			IApiDescription systemApiDescription = member.getApiComponent().getSystemApiDescription();
			boolean value = !Util.isAPI(eeValue, elementDescriptor, systemApiDescription);
			return value;
		} catch (CoreException e) {
			ApiPlugin.log(e);
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
			IApiComponent[] resolvePackages = baseline.resolvePackage(apiComponent, referencedTypeName.substring(0, index - 1));
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
}
