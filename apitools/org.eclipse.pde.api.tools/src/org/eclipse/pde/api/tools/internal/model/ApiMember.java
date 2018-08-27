/*******************************************************************************
 * Copyright (c) 2008, 2009 IBM Corporation and others.
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
package org.eclipse.pde.api.tools.internal.model;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.pde.api.tools.internal.provisional.ApiPlugin;
import org.eclipse.pde.api.tools.internal.provisional.model.IApiComponent;
import org.eclipse.pde.api.tools.internal.provisional.model.IApiElement;
import org.eclipse.pde.api.tools.internal.provisional.model.IApiMember;
import org.eclipse.pde.api.tools.internal.provisional.model.IApiType;

/**
 * Base implementation of {@link IApiMember}
 *
 * @since 1.0.0
 * @noextend This class is not intended to be subclassed by clients.
 */
public abstract class ApiMember extends ApiElement implements IApiMember {

	private int fFlags = -1;
	private String fSignature;
	private String fGenericSignature;

	/**
	 * Constructs a member enclosed by the given type with the specified name,
	 * type constant, and flags (modifiers).
	 *
	 * @param parent the parent {@link IApiElement} for this type
	 * @param name member name
	 * @param signature signature (type or method)
	 * @param type see element type constants in {@link IApiMember}
	 * @param flags modifier flags
	 */
	protected ApiMember(IApiElement parent, String name, String signature, String genericSig, int type, int flags) {
		super(parent, type, name);
		fFlags = flags;
		fSignature = signature;
		fGenericSignature = genericSig;
	}

	/**
	 * Returns this member's signature.
	 *
	 * @return signature
	 */
	@Override
	public String getSignature() {
		return fSignature;
	}

	/**
	 * Returns generic signature or <code>null</code> if none.
	 *
	 * @return
	 */
	@Override
	public String getGenericSignature() {
		return fGenericSignature;
	}

	@Override
	public int getModifiers() {
		return fFlags;
	}

	@Override
	public IApiType getEnclosingType() throws CoreException {
		return (IApiType) getAncestor(IApiElement.TYPE);
	}

	@Override
	public IApiComponent getApiComponent() {
		return (IApiComponent) getAncestor(IApiElement.COMPONENT);
	}

	@Override
	public String getPackageName() {
		try {
			IApiType type = getEnclosingType();
			if (type != null) {
				return type.getPackageName();
			}
		} catch (CoreException ce) {
			ApiPlugin.log("Failed to read package name for " + getName(), ce); //$NON-NLS-1$
		}
		return ""; //$NON-NLS-1$
	}

	@Override
	public boolean equals(Object obj) {
		if (obj instanceof IApiElement) {
			IApiElement element = (IApiElement) obj;
			if (element.getType() == this.getType()) {
				return enclosingTypesEqual(this, element) && getName().equals(element.getName());
			}
		}
		return false;
	}

	/**
	 * Returns if the immediate enclosing {@link IApiType}s of the two members
	 * are equal: where both enclosing type being <code>null</code> is
	 * considered equal
	 *
	 * @param e1
	 * @param e2
	 * @return true if both immediate enclosing type are equal
	 */
	protected boolean enclosingTypesEqual(IApiElement e1, IApiElement e2) {
		IApiType t1 = (IApiType) e1.getAncestor(IApiElement.TYPE);
		IApiType t2 = (IApiType) e2.getAncestor(IApiElement.TYPE);
		if (t1 == null) {
			return t2 == null;
		}
		return t1.equals(t2);
	}

	@Override
	public int hashCode() {
		IApiType enclosing = (IApiType) getAncestor(IApiElement.TYPE);
		return getType() + getName().hashCode() + (enclosing == null ? 0 : enclosing.hashCode());
	}

	/**
	 * Used when building a member type.
	 *
	 * @param access modifiers
	 */
	public void setModifiers(int access) {
		fFlags = access;
	}
}
