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
package org.eclipse.pde.api.tools.internal.builder;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.Flags;
import org.eclipse.jdt.core.IField;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.ISourceRange;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.Signature;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.Position;
import org.eclipse.osgi.service.resolver.BundleDescription;
import org.eclipse.osgi.service.resolver.ImportPackageSpecification;
import org.eclipse.pde.api.tools.internal.model.BundleApiComponent;
import org.eclipse.pde.api.tools.internal.provisional.ApiPlugin;
import org.eclipse.pde.api.tools.internal.provisional.IApiDescription;
import org.eclipse.pde.api.tools.internal.provisional.ProfileModifiers;
import org.eclipse.pde.api.tools.internal.provisional.builder.IReference;
import org.eclipse.pde.api.tools.internal.provisional.builder.ReferenceModifiers;
import org.eclipse.pde.api.tools.internal.provisional.descriptors.IElementDescriptor;
import org.eclipse.pde.api.tools.internal.provisional.descriptors.IPackageDescriptor;
import org.eclipse.pde.api.tools.internal.provisional.model.IApiBaseline;
import org.eclipse.pde.api.tools.internal.provisional.model.IApiComponent;
import org.eclipse.pde.api.tools.internal.provisional.model.IApiElement;
import org.eclipse.pde.api.tools.internal.provisional.model.IApiField;
import org.eclipse.pde.api.tools.internal.provisional.model.IApiMember;
import org.eclipse.pde.api.tools.internal.provisional.model.IApiMethod;
import org.eclipse.pde.api.tools.internal.provisional.model.IApiType;
import org.eclipse.pde.api.tools.internal.provisional.problems.IApiProblem;
import org.eclipse.pde.api.tools.internal.provisional.problems.IApiProblemTypes;
import org.eclipse.pde.api.tools.internal.util.Util;

/**
 * Detects references to fields, methods and types that are not available for a specific EE.
 * 
 * @since 1.1
 */
public class SystemApiDetector extends AbstractProblemDetector {
	private Map referenceEEs;
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
		String eeValue = ProfileModifiers.getName(((Integer) this.referenceEEs.get(reference)).intValue());
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
		String eeValue = ProfileModifiers.getName(((Integer) this.referenceEEs.get(reference)).intValue());
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
					IApiMember apiMember = reference.getMember();
					switch(apiMember.getType()) {
						case IApiElement.FIELD : {
							IApiField field = (IApiField) reference.getMember();
							return getSourceRangeForField(type, reference,
									field);
						}
						case IApiElement.METHOD : {
							// reference in a method declaration
							IApiMethod method = (IApiMethod) reference.getMember();
							return getSourceRangeForMethod(type, reference,
									method);
						}
					}
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
				}
				// reference in a field declaration
				IApiField field = (IApiField) reference.getMember();
				return getSourceRangeForField(type, reference, field);
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
				}
				// reference in a method declaration
				IApiMethod method = (IApiMethod) reference.getMember();
				return getSourceRangeForMethod(type, reference, method);
			}
			default :
				return null;
		}
	}
	private Position getSourceRangeForField(IType type, IReference reference,
			IApiField field) throws JavaModelException, CoreException {
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
	private Position getSourceRangeForMethod(IType type, IReference reference,
			IApiMethod method) throws CoreException, JavaModelException {
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

	/* (non-Javadoc)
	 * @see org.eclipse.pde.api.tools.internal.search.AbstractProblemDetector#isProblem(org.eclipse.pde.api.tools.internal.provisional.model.IReference)
	 */
	protected boolean isProblem(IReference reference) {
		// the reference must be in the system library
		try {
			IApiMember member = reference.getMember();
			IApiComponent apiComponent = member.getApiComponent();
			String[] lowestEEs = apiComponent.getLowestEEs();
			loop: for (int i = 0, max = lowestEEs.length; i < max; i++) {
				String lowestEE = lowestEEs[i];
				int eeValue = ProfileModifiers.getValue(lowestEE); 
				if (eeValue == ProfileModifiers.NO_PROFILE_VALUE) {
					return false;
				}
				IApiMember resolvedReference = reference.getResolvedReference();
				IElementDescriptor elementDescriptor = resolvedReference.getHandle();
				IApiDescription systemApiDescription = apiComponent.getSystemApiDescription(eeValue);
				boolean value = !Util.isAPI(eeValue, elementDescriptor, systemApiDescription);
				if (value) {
					/*
					 * Make sure that the resolved reference doesn't below to one of the imported package of
					 * the current component
					 */
					if (apiComponent instanceof BundleApiComponent) {
						BundleDescription bundle = ((BundleApiComponent)apiComponent).getBundleDescription();
						ImportPackageSpecification[] importPackages = bundle.getImportPackages();
						String packageName = getPackageName(elementDescriptor);
						for (int j = 0, max2 = importPackages.length; j < max2; j++) {
							ImportPackageSpecification importPackageSpecification = importPackages[j];
							// get the IPackageDescriptor for the element descriptor
							String importPackageName = importPackageSpecification.getName();
							if (importPackageName.equals(packageName)) {
								continue loop;
							}
						}
					}
					if (this.referenceEEs == null) {
						this.referenceEEs = new HashMap(3);
					}
					this.referenceEEs.put(reference, new Integer(eeValue));
					return true;
				}
			}
		} catch (CoreException e) {
			ApiPlugin.log(e);
		}
		return false;
	}
	private static String getPackageName(IElementDescriptor elementDescriptor) {
		IElementDescriptor currentDescriptor = elementDescriptor;
		while (currentDescriptor != null && currentDescriptor.getElementType() != IElementDescriptor.PACKAGE) {
			currentDescriptor = currentDescriptor.getParent();
		}
		return currentDescriptor == null ? Util.EMPTY_STRING : ((IPackageDescriptor) currentDescriptor).getName();
	}
	public boolean considerReference(IReference reference) {
		try {
			IApiComponent apiComponent = reference.getMember().getApiComponent();
			IApiBaseline baseline = apiComponent.getBaseline();
			if (baseline == null) return false;
			String referencedTypeName = reference.getReferencedTypeName();
			// extract the package name
			int index = referencedTypeName.lastIndexOf('.');
			if (index == -1) return false;
			String substring = referencedTypeName.substring(0, index);
			IApiComponent[] resolvePackages = baseline.resolvePackage(apiComponent, substring);
			switch(resolvePackages.length) {
				case 1 :
					if (resolvePackages[0].isSystemComponent()) {
						switch(reference.getReferenceKind()) {
							case ReferenceModifiers.REF_OVERRIDE :
							case ReferenceModifiers.REF_CONSTANTPOOL :
								return false;
						}
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
		return ReferenceModifiers.MASK_REF_ALL & ~ReferenceModifiers.REF_OVERRIDE;
	}
}
