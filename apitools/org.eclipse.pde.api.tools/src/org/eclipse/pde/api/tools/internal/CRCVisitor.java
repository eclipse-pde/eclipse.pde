/*******************************************************************************
 * Copyright (c) 2010 IBM Corporation and others.
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
package org.eclipse.pde.api.tools.internal;

import java.util.zip.CRC32;

import org.eclipse.pde.api.tools.internal.provisional.ApiDescriptionVisitor;
import org.eclipse.pde.api.tools.internal.provisional.IApiAnnotations;
import org.eclipse.pde.api.tools.internal.provisional.descriptors.IElementDescriptor;
import org.eclipse.pde.api.tools.internal.provisional.descriptors.IFieldDescriptor;
import org.eclipse.pde.api.tools.internal.provisional.descriptors.IMethodDescriptor;
import org.eclipse.pde.api.tools.internal.provisional.descriptors.IReferenceTypeDescriptor;

/**
 * Builds a CRC code for a type's API annotations
 */
public class CRCVisitor extends ApiDescriptionVisitor {

	private CRC32 fCrc = new CRC32();

	@Override
	public boolean visitElement(IElementDescriptor element, IApiAnnotations description) {
		String signature = null;
		String name = null;
		switch (element.getElementType()) {
			case IElementDescriptor.TYPE:
				signature = ((IReferenceTypeDescriptor) element).getSignature();
				break;
			case IElementDescriptor.METHOD:
				signature = ((IMethodDescriptor) element).getSignature();
				name = ((IMethodDescriptor) element).getName();
				break;
			case IElementDescriptor.FIELD:
				name = ((IFieldDescriptor) element).getName();
				break;
			default:
				break;
		}
		if (signature != null) {
			fCrc.update(signature.getBytes());
		}
		if (name != null) {
			fCrc.update(name.getBytes());
		}
		fCrc.update(description.getRestrictions());
		fCrc.update(description.getVisibility());
		return true;
	}

	public long getValue() {
		return fCrc.getValue();
	}

}
