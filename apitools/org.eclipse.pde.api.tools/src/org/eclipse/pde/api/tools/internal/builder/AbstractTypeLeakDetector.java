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
import org.eclipse.jdt.core.ISourceRange;
import org.eclipse.jdt.core.IType;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.Position;
import org.eclipse.pde.api.tools.internal.provisional.ApiPlugin;
import org.eclipse.pde.api.tools.internal.provisional.IApiAnnotations;
import org.eclipse.pde.api.tools.internal.provisional.VisibilityModifiers;
import org.eclipse.pde.api.tools.internal.provisional.builder.IReference;
import org.eclipse.pde.api.tools.internal.provisional.descriptors.IElementDescriptor;
import org.eclipse.pde.api.tools.internal.provisional.model.IApiMember;
import org.eclipse.pde.api.tools.internal.provisional.problems.IApiProblem;

import com.ibm.icu.text.MessageFormat;

/**
 * Detects leaked types.
 *
 * @since 1.1
 */
public abstract class AbstractTypeLeakDetector extends AbstractLeakProblemDetector {

	/**
	 * @param nonApiPackageNames
	 */
	public AbstractTypeLeakDetector(Set<String> nonApiPackageNames) {
		super(nonApiPackageNames);
	}

	@Override
	public boolean considerReference(IReference reference) {
		// consider the reference if the location the reference is made from is
		// visible:
		// i.e. a public or protected class in an API package
		if (super.considerReference(reference) && isNonAPIReference(reference)) {
			IApiMember member = reference.getMember();
			int modifiers = member.getModifiers();
			if (((Flags.AccPublic | Flags.AccProtected) & modifiers) > 0) {
				try {
					IApiAnnotations annotations = member.getApiComponent().getApiDescription().resolveAnnotations(member.getHandle());
					// annotations can be null for members in top level non
					// public types, but they are not visible/API
					if (annotations != null) {
						if (isApplicable(annotations) && isEnclosingTypeVisible(member)) {
							retainReference(reference);
							return true;
						}
					}
				} catch (CoreException e) {
					ApiPlugin.log(e.getStatus());
				}
			}
		}
		return false;
	}

	@Override
	protected boolean isProblem(IReference reference) {
		IApiMember member = reference.getResolvedReference();
		try {
			IApiAnnotations annotations = member.getApiComponent().getApiDescription().resolveAnnotations(member.getHandle());
			if (annotations != null) {
				return VisibilityModifiers.isPrivate(annotations.getVisibility());
			} else {
				// could be a reference to a top level secondary/non-public type
				if (isEnclosingTypeVisible(member)) {
					// this is an unexpected condition - the enclosing type is
					// visible, but it has no annotations - log an error
					String memberName = member.getName();
					if (memberName != null) {
						if (!memberName.startsWith("javax.")) { //$NON-NLS-1$
							ApiPlugin.log(new Status(IStatus.INFO, ApiPlugin.PLUGIN_ID, MessageFormat.format(BuilderMessages.AbstractTypeLeakDetector_vis_type_has_no_api_description, memberName)));
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

	/**
	 * Returns whether the given annotations should be considered.
	 */
	protected boolean isApplicable(IApiAnnotations annotations) {
		return VisibilityModifiers.isAPI(annotations.getVisibility());
	}

	@Override
	protected String[] getQualifiedMessageArgs(IReference reference) throws CoreException {
		return new String[] {
				getQualifiedTypeName(reference.getResolvedReference()),
				getQualifiedTypeName(reference.getMember()) };
	}

	@Override
	protected String[] getMessageArgs(IReference reference) throws CoreException {
		return new String[] {
				getSimpleTypeName(reference.getResolvedReference()),
				getSimpleTypeName(reference.getMember()) };
	}

	@Override
	protected Position getSourceRange(IType type, IDocument doc, IReference reference) throws CoreException, BadLocationException {
		ISourceRange range = type.getNameRange();
		Position pos = null;
		if (range != null) {
			pos = new Position(range.getOffset(), range.getLength());
		}
		if (pos == null) {
			return defaultSourcePosition(type, reference);
		}
		return pos;
	}

	@Override
	protected int getElementType(IReference reference) {
		return IElementDescriptor.TYPE;
	}

	@Override
	protected int getProblemKind() {
		return IApiProblem.API_LEAK;
	}

}
