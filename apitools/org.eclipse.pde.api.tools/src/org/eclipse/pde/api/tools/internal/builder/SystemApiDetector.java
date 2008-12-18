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
import org.eclipse.jdt.core.ISourceRange;
import org.eclipse.jdt.core.IType;
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
import org.eclipse.pde.api.tools.internal.util.Signatures;
import org.eclipse.pde.api.tools.internal.util.Util;

/**
 * Detects references to fields, methods and types that are not available for a specific EE.
 * 
 * @since 1.1
 */
public class SystemApiDetector extends AbstractProblemDetector {
	
	private Map referenceEEs;
	
	/**
	 * Constructor
	 */
	public SystemApiDetector() {}
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.api.tools.internal.builder.AbstractProblemDetector#getElementType(org.eclipse.pde.api.tools.internal.provisional.builder.IReference)
	 */
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

	/* (non-Javadoc)
	 * @see org.eclipse.pde.api.tools.internal.builder.AbstractProblemDetector#getMessageArgs(org.eclipse.pde.api.tools.internal.provisional.builder.IReference)
	 */
	protected String[] getMessageArgs(IReference reference)
			throws CoreException {
		IApiMember resolvedReference = reference.getResolvedReference();
		IApiMember member = reference.getMember();
		String eeValue = ProfileModifiers.getName(((Integer) this.referenceEEs.get(reference)).intValue());
		String simpleTypeName = getSimpleTypeName(resolvedReference);
		if (simpleTypeName.indexOf('$') != -1) {
			simpleTypeName = simpleTypeName.replace('$', '.');
		}
		switch(resolvedReference.getType()) {
			case IApiElement.TYPE : {
				return new String[] {
						getDisplay(member, false),
						simpleTypeName,
						eeValue,
				};
			}
			case IApiElement.FIELD : {
				IApiField field = (IApiField) resolvedReference;
				return new String[] {
						getDisplay(member, false),
						simpleTypeName,
						field.getName(),
						eeValue,
				};
			}
			case IApiElement.METHOD : {
				IApiMethod method = (IApiMethod) resolvedReference;
				if (method.isConstructor()) {
					return new String[] {
							getDisplay(member, false),
							Signature.toString(method.getSignature(), simpleTypeName, null, false, false),
							eeValue,
					};
				} else {
					return new String[] {
							getDisplay(member, false),
							simpleTypeName,
							Signature.toString(method.getSignature(), method.getName(), null, false, false),
							eeValue,
					};
				}
			}
			default :
				return null;
		}
	}

	/**
	 * Returns the signature to display for found problems
	 * @param member the member to get the signature from
	 * @param qualified if the returned signature should be type-qualified or not
	 * @return
	 * @throws CoreException
	 */
	private String getDisplay(IApiMember member, boolean qualified) throws CoreException {
		String typeName = qualified ? getTypeName(member) : getSimpleTypeName(member);
		if (typeName.indexOf('$') != -1) {
			typeName = typeName.replace('$', '.');
		}
		switch(member.getType()) {
			case IApiElement.FIELD : {
				StringBuffer buffer = new StringBuffer();
				IApiField field = (IApiField) member;
				buffer
					.append(typeName)
					.append('.')
					.append(field.getName());
				return String.valueOf(buffer);
			}
			case IApiElement.METHOD : {
				// reference in a method declaration
				IApiMethod method = (IApiMethod) member;
				if(qualified) {
					return Signatures.getMethodSignature(method);
				}
				return Signatures.getQualifiedMethodSignature(method);
			}
		}
		return typeName;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.api.tools.internal.builder.AbstractProblemDetector#getProblemFlags(org.eclipse.pde.api.tools.internal.provisional.builder.IReference)
	 */
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
			case IApiElement.FIELD : {
				return IApiProblem.FIELD;
			}
			default : {
				return IApiProblem.NO_FLAGS;
			}
		}
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.api.tools.internal.builder.AbstractProblemDetector#getProblemKind()
	 */
	protected int getProblemKind() {
		return IApiProblem.INVALID_REFERENCE_IN_SYSTEM_LIBRARIES;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.api.tools.internal.builder.AbstractProblemDetector#getQualifiedMessageArgs(org.eclipse.pde.api.tools.internal.provisional.builder.IReference)
	 */
	protected String[] getQualifiedMessageArgs(IReference reference) throws CoreException {
		IApiMember resolvedReference = reference.getResolvedReference();
		IApiMember member = reference.getMember();
		String eeValue = ProfileModifiers.getName(((Integer) this.referenceEEs.get(reference)).intValue());
		String typeName = getQualifiedTypeName(resolvedReference);
		if (typeName.indexOf('$') != -1) {
			typeName = typeName.replace('$', '.');
		}
		switch(resolvedReference.getType()) {
			case IApiElement.TYPE : {
				return new String[] {
						getDisplay(member, true),
						typeName,
						eeValue,
				};
			}
			case IApiElement.FIELD : {
				IApiField field = (IApiField) resolvedReference;
				return new String[] {
						getDisplay(member, true),
						typeName,
						field.getName(),
						eeValue,
				};
			}
			case IApiElement.METHOD : {
				IApiMethod method = (IApiMethod) resolvedReference;
				if (method.isConstructor()) {
					String simpleTypeName = getSimpleTypeName(method);
					if (simpleTypeName.indexOf('$') != -1) {
						simpleTypeName = simpleTypeName.replace('$', '.');
					}
					return new String[] {
							getDisplay(member, true),
							Signature.toString(method.getSignature(), simpleTypeName, null, false, false),
							eeValue,
					};
				} else {
					return new String[] {
							getDisplay(member, true),
							typeName,
							Signature.toString(method.getSignature(), method.getName(), null, false, false),
							eeValue,
					};
				}
			}
			default :
				return null;
		}
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.api.tools.internal.builder.AbstractProblemDetector#getSeverityKey()
	 */
	protected String getSeverityKey() {
		return IApiProblemTypes.INVALID_REFERENCE_IN_SYSTEM_LIBRARIES;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.api.tools.internal.builder.AbstractProblemDetector#getSourceRange(org.eclipse.jdt.core.IType, org.eclipse.jface.text.IDocument, org.eclipse.pde.api.tools.internal.provisional.builder.IReference)
	 */
	protected Position getSourceRange(IType type, IDocument document, IReference reference) throws CoreException, BadLocationException {
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
							return getSourceRangeForField(type, reference, field);
						}
						case IApiElement.METHOD : {
							// reference in a method declaration
							IApiMethod method = (IApiMethod) reference.getMember();
							return getSourceRangeForMethod(type, reference, method);
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
					return getFieldNameRange((IApiField) resolvedReference, document, reference);
				}
				// reference in a field declaration
				IApiField field = (IApiField) reference.getMember();
				return getSourceRangeForField(type, reference, field);
			}
			case IApiElement.METHOD : {
				if (reference.getLineNumber() >= 0) {
					IApiMethod method = (IApiMethod) resolvedReference;
					Position pos = getMethodNameRange(Signatures.getMethodName(method), document, reference);
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

	/* (non-Javadoc)
	 * @see org.eclipse.pde.api.tools.internal.search.AbstractProblemDetector#isProblem(org.eclipse.pde.api.tools.internal.provisional.model.IReference)
	 */
	protected boolean isProblem(IReference reference) {
		// the reference must be in the system library
		try {
			IApiMember member = reference.getMember();
			IApiComponent apiComponent = member.getApiComponent();
			String[] lowestEEs = apiComponent.getLowestEEs();
			if (lowestEEs == null) {
				// this should not be true for Eclipse bundle as they should always have a EE set
				return false;
			}
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
	
	/**
	 * Returns the name of the parent packages for the given {@link IElementDescriptor}
	 * @param elementDescriptor
	 * @return the parent package for the given {@link IElementDescriptor}
	 */
	private String getPackageName(IElementDescriptor elementDescriptor) {
		IElementDescriptor currentDescriptor = elementDescriptor;
		while (currentDescriptor != null && currentDescriptor.getElementType() != IElementDescriptor.PACKAGE) {
			currentDescriptor = currentDescriptor.getParent();
		}
		return currentDescriptor == null ? Util.EMPTY_STRING : ((IPackageDescriptor) currentDescriptor).getName();
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.api.tools.internal.provisional.builder.IApiProblemDetector#considerReference(org.eclipse.pde.api.tools.internal.provisional.builder.IReference)
	 */
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
				case 1 : {
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
			}
		} catch (CoreException e) {
			ApiPlugin.log(e);
		}
		return false;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.api.tools.internal.provisional.builder.IApiProblemDetector#getReferenceKinds()
	 */
	public int getReferenceKinds() {
		return ReferenceModifiers.MASK_REF_ALL & ~ReferenceModifiers.REF_OVERRIDE;
	}
}
