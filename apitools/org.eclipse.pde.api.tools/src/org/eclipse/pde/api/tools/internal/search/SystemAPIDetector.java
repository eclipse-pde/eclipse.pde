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
public class SystemAPIDetector extends AbstractProblemDetector {

	int eeValue;
	
	public SystemAPIDetector(String eeIdentifier) {
		this.eeValue= ProfileModifiers.getValue(eeIdentifier);
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
		switch(resolvedReference.getType()) {
			case IApiElement.TYPE : {
				return new String[] {getSimpleTypeName(resolvedReference), getSimpleTypeName(reference.getMember())};
			}
			case IApiElement.FIELD : {
				IApiField field = (IApiField) reference.getMember();
				return new String[] {getSimpleTypeName(resolvedReference), getSimpleTypeName(field), field.getName()};
			}
			case IApiElement.METHOD : {
				IApiMethod method = (IApiMethod) resolvedReference;
				String methodName = method.getName();
				if (method.isConstructor()) {
					methodName = getSimpleTypeName(method);
				}
				return new String[] {getSimpleTypeName(resolvedReference), getSimpleTypeName(reference.getMember()), Signature.toString(method.getSignature(), methodName, null, false, false)};
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
		switch(resolvedReference.getType()) {
			case IApiElement.TYPE : {
				return new String[] {getTypeName(resolvedReference), getTypeName(reference.getMember())};
			}
			case IApiElement.FIELD : {
				IApiField field = (IApiField) reference.getMember();
				return new String[] {getTypeName(resolvedReference), getTypeName(field), field.getName()};
			}
			case IApiElement.METHOD : {
				IApiMethod method = (IApiMethod) resolvedReference;
				String methodName = method.getName();
				if (method.isConstructor()) {
					methodName = getSimpleTypeName(method);
				}
				return new String[] {getTypeName(resolvedReference), getTypeName(reference.getMember()), Signature.toString(method.getSignature(), methodName, null, false, false)};
			}
			default :
				return null;
		}
	}

	protected String getSeverityKey() {
		return IApiProblemTypes.INVALID_REFERENCE_IN_SYSTEM_LIBRARIES;
	}

	protected Position getSourceRange(IType type, IDocument doc,
			IReference reference) throws CoreException, BadLocationException {
		IApiMember resolvedReference = reference.getResolvedReference();
		switch(resolvedReference.getType()) {
			case IApiElement.TYPE : {
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
			case IApiElement.FIELD : {
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
			case IApiElement.METHOD : {
				IApiMethod method = (IApiMethod) resolvedReference;
				String[] parameterTypes = Signature.getParameterTypes(method.getSignature());
				for (int i = 0; i < parameterTypes.length; i++) {
					parameterTypes[i] = parameterTypes[i].replace('/', '.');
				}
				IMethod Qmethod = type.getMethod(method.getName(), parameterTypes);
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
			default :
				return null;
		}
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.api.tools.internal.search.AbstractProblemDetector#isProblem(org.eclipse.pde.api.tools.internal.provisional.model.IReference)
	 */
	protected boolean isProblem(IReference reference) {
		// the reference must be in the system library
		if (this.eeValue == ProfileModifiers.NO_PROFILE_VALUE) {
			return false;
		}
		IApiMember member = reference.getMember();
		try {
			IElementDescriptor elementDescriptor = reference.getResolvedReference().getHandle();
			IApiDescription systemApiDescription = member.getApiComponent().getSystemApiDescription();
			return !Util.isAPI(this.eeValue, elementDescriptor, systemApiDescription);
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
