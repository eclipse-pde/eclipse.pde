/*******************************************************************************
 * Copyright (c) 2009, 2010 IBM Corporation and others.
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
package org.eclipse.pde.api.tools.internal.search;

import org.eclipse.pde.api.tools.internal.provisional.builder.IReference;
import org.eclipse.pde.api.tools.internal.provisional.descriptors.IComponentDescriptor;
import org.eclipse.pde.api.tools.internal.provisional.descriptors.IElementDescriptor;
import org.eclipse.pde.api.tools.internal.provisional.descriptors.IMemberDescriptor;

/**
 * Implementation of a reference descriptor
 */
public class ReferenceDescriptor implements IReferenceDescriptor {

	private IMemberDescriptor origin;
	private IMemberDescriptor target;
	private IComponentDescriptor from;
	private IComponentDescriptor to;

	private int line;
	private int kind;
	private int flags;
	private int visibility;
	private String[] messages = null;

	public ReferenceDescriptor(IComponentDescriptor from, IMemberDescriptor origin, int line, IComponentDescriptor to, IMemberDescriptor target, int kind, int flags, int vis, String[] messages) {
		this.origin = origin;
		this.target = target;
		this.from = from;
		this.to = to;
		this.line = line;
		this.kind = kind;
		this.flags = flags;
		this.visibility = vis;
		this.messages = messages;
	}

	@Override
	public boolean equals(Object obj) {
		if (obj instanceof ReferenceDescriptor) {
			ReferenceDescriptor rd = (ReferenceDescriptor) obj;
			return origin.equals(rd.origin) && target.equals(rd.target) && from.equals(rd.from) && to.equals(rd.to) && line == rd.line && kind == rd.kind && visibility == rd.visibility;
		}
		return false;
	}

	@Override
	public int hashCode() {
		return origin.hashCode() + target.hashCode() + from.hashCode() + to.hashCode() + line + kind + visibility;
	}

	@Override
	public IComponentDescriptor getComponent() {
		return from;
	}

	@Override
	public int getLineNumber() {
		return line;
	}

	@Override
	public IMemberDescriptor getMember() {
		return origin;
	}

	@Override
	public int getReferenceKind() {
		return kind;
	}

	@Override
	public int getReferenceFlags() {
		return flags;
	}

	@Override
	public int getReferenceType() {
		switch (getReferencedMember().getElementType()) {
			case IElementDescriptor.TYPE:
				return IReference.T_TYPE_REFERENCE;
			case IElementDescriptor.METHOD:
				return IReference.T_METHOD_REFERENCE;
			case IElementDescriptor.FIELD:
				return IReference.T_FIELD_REFERENCE;
			default:
				return -1;
		}
	}

	@Override
	public IComponentDescriptor getReferencedComponent() {
		return to;
	}

	@Override
	public IMemberDescriptor getReferencedMember() {
		return target;
	}

	@Override
	public int getVisibility() {
		return visibility;
	}

	@Override
	public String[] getProblemMessages() {
		return this.messages;
	}
}
