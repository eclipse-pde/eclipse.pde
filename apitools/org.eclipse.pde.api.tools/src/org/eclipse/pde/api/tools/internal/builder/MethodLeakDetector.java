/*******************************************************************************
 * Copyright (c) 2008, 2018 IBM Corporation and others.
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
package org.eclipse.pde.api.tools.internal.builder;

import java.util.Set;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jdt.core.Flags;
import org.eclipse.jdt.core.IType;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.Position;
import org.eclipse.pde.api.tools.internal.provisional.ApiPlugin;
import org.eclipse.pde.api.tools.internal.provisional.IApiAnnotations;
import org.eclipse.pde.api.tools.internal.provisional.IApiDescription;
import org.eclipse.pde.api.tools.internal.provisional.RestrictionModifiers;
import org.eclipse.pde.api.tools.internal.provisional.VisibilityModifiers;
import org.eclipse.pde.api.tools.internal.provisional.builder.IReference;
import org.eclipse.pde.api.tools.internal.provisional.descriptors.IElementDescriptor;
import org.eclipse.pde.api.tools.internal.provisional.model.IApiComponent;
import org.eclipse.pde.api.tools.internal.provisional.model.IApiMember;
import org.eclipse.pde.api.tools.internal.provisional.model.IApiMethod;
import org.eclipse.pde.api.tools.internal.provisional.model.IApiType;
import org.eclipse.pde.api.tools.internal.provisional.problems.IApiProblem;
import org.eclipse.pde.api.tools.internal.provisional.problems.IApiProblemTypes;
import org.eclipse.pde.api.tools.internal.util.Signatures;

import com.ibm.icu.text.MessageFormat;

/**
 * Detects leaks in method return types and parameters
 *
 * @since 1.1
 * @noextend This class is not intended to be sub-classed by clients.
 */
public abstract class MethodLeakDetector extends AbstractLeakProblemDetector {

	/**
	 * @param nonApiPackageNames
	 */
	public MethodLeakDetector(Set<String> nonApiPackageNames) {
		super(nonApiPackageNames);
	}

	@Override
	protected int getElementType(IReference reference) {
		return IElementDescriptor.METHOD;
	}

	@Override
	protected int getProblemKind() {
		return IApiProblem.API_LEAK;
	}

	@Override
	protected String getSeverityKey() {
		return IApiProblemTypes.LEAK_METHOD_RETURN_TYPE;
	}

	@Override
	protected boolean isProblem(IReference reference) {
		IApiMethod method = (IApiMethod) reference.getMember();
		IApiType type = (IApiType) reference.getResolvedReference();
		try {
			// referenced type is non-API
			IApiAnnotations annotations = type.getApiComponent().getApiDescription().resolveAnnotations(type.getHandle());
			if (annotations != null) {
				if (VisibilityModifiers.isPrivate(annotations.getVisibility())) {
					if ((Flags.AccProtected & method.getModifiers()) > 0) {
						// ignore protected members if contained in a @noextend
						// type
						// TODO: we could perform this check before resolution -
						// it's on the source location
						IApiDescription description = method.getApiComponent().getApiDescription();
						annotations = description.resolveAnnotations(method.getHandle().getEnclosingType());
						if (annotations == null || RestrictionModifiers.isExtendRestriction(annotations.getRestrictions())) {
							// ignore
							return false;
						}
					}
					return true;
				}
			} else {
				// could be a reference to a top level secondary/non-public type
				if (isEnclosingTypeVisible(type)) {
					// this is an unexpected condition - the enclosing type is
					// visible, but it has no annotations - log an error
					String typeName = type.getName();
					if (typeName != null) {
						if (!typeName.startsWith("javax.")) { //$NON-NLS-1$
							ApiPlugin.log(new Status(IStatus.INFO, ApiPlugin.PLUGIN_ID, MessageFormat.format(BuilderMessages.AbstractTypeLeakDetector_vis_type_has_no_api_description, typeName)));
						}
					}
				} else {
					// enclosing type is not visible - this is a problem
					return true;
				}
			}
		} catch (CoreException e) {
			ApiPlugin.log(e);
		}
		return false;
	}

	@Override
	protected String[] getMessageArgs(IReference reference) throws CoreException {
		IApiMethod method = (IApiMethod) reference.getMember();
		IApiType type = (IApiType) reference.getResolvedReference();
		return new String[] {
				getSimpleTypeName(type), getSimpleTypeName(method),
				Signatures.getMethodSignature(method) };
	}

	@Override
	protected String[] getQualifiedMessageArgs(IReference reference) throws CoreException {
		IApiMethod method = (IApiMethod) reference.getMember();
		IApiType type = (IApiType) reference.getResolvedReference();
		return new String[] {
				getQualifiedTypeName(type), getQualifiedTypeName(method),
				Signatures.getMethodSignature(method) };
	}

	@Override
	protected Position getSourceRange(IType type, IDocument doc, IReference reference) throws CoreException, BadLocationException {
		return getSourceRangeForMethod(type, reference, (IApiMethod) reference.getMember());
	}

	@Override
	public boolean considerReference(IReference reference) {
		if (super.considerReference(reference) && isNonAPIReference(reference)) {
			IApiMember member = reference.getMember();
			if (member != null && matchesSourceModifiers(member) && matchesSourceApiRestrictions(member)) {
				retainReference(reference);
				return true;
			}
		}
		return false;
	}

	/**
	 * Returns if the source API restrictions for the given member matches the
	 * restrictions in the parent API description
	 *
	 * @param member
	 * @return true if it matches, false otherwise
	 */
	protected boolean matchesSourceApiRestrictions(IApiMember member) {
		IApiComponent apiComponent = member.getApiComponent();
		try {
			IApiMethod method = (IApiMethod) member;
			IApiAnnotations annotations = apiComponent.getApiDescription().resolveAnnotations(method.getHandle());
			if (annotations != null) {
				if (VisibilityModifiers.isAPI(annotations.getVisibility())) {
					int ares = annotations.getRestrictions();
					if (ares != 0) {
						if (method.isConstructor()) {
							return (ares & RestrictionModifiers.NO_REFERENCE) == 0;
						}
						if ((ares & RestrictionModifiers.NO_OVERRIDE) == 0) {
							IApiAnnotations annot = apiComponent.getApiDescription().resolveAnnotations(method.getEnclosingType().getHandle());
							int pres = 0;
							if (annot != null) {
								pres = annot.getRestrictions();
							}
							return (ares & RestrictionModifiers.NO_REFERENCE) != 0 && (!Flags.isFinal(method.getModifiers()) && !Flags.isStatic(method.getModifiers()) && !Flags.isFinal(method.getEnclosingType().getModifiers()) && ((pres & RestrictionModifiers.NO_EXTEND) == 0));
						}
						return (ares & RestrictionModifiers.NO_REFERENCE) == 0;
					} else {
						return !(Flags.isProtected(method.getModifiers()) && Flags.isFinal(method.getEnclosingType().getModifiers()));
					}
				}
			} else {
				return true;
			}
		} catch (CoreException e) {
			ApiPlugin.log(e);
		}
		return false;
	}

	/**
	 * Returns if the source modifiers for the given member match the ones
	 * specified in the detector
	 *
	 * @param member
	 * @return true if the modifiers match, false otherwise
	 */
	protected boolean matchesSourceModifiers(IApiMember member) {
		IApiMember lmember = member;
		while (lmember != null) {
			int modifiers = lmember.getModifiers();
			if (Flags.isPublic(modifiers) || Flags.isProtected(modifiers)) {
				try {
					lmember = lmember.getEnclosingType();
				} catch (CoreException e) {
					ApiPlugin.log(e.getStatus());
					return false;
				}
			} else {
				return false;
			}
		}
		return true;
	}

}
