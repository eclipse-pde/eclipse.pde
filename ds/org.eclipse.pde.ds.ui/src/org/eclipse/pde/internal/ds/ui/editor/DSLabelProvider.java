/*******************************************************************************
 * Copyright (c) 2008, 2009 Code 9 Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Code 9 Corporation - initial API and implementation
 *     Chris Aniszczyk <caniszczyk@gmail.com>
 *     Rafael Oliveira Nobrega <rafael.oliveira@gmail.com> - bug 242028, 248197
 *******************************************************************************/
package org.eclipse.pde.internal.ds.ui.editor;

import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.pde.internal.ds.core.IDSConstants;
import org.eclipse.pde.internal.ds.core.IDSObject;
import org.eclipse.pde.internal.ds.core.IDSReference;
import org.eclipse.pde.internal.ds.ui.Messages;
import org.eclipse.pde.internal.ds.ui.SharedImages;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.pde.internal.ui.PDEPluginImages;
import org.eclipse.swt.graphics.Image;
import org.eclipse.ui.forms.editor.IFormPage;

public class DSLabelProvider extends LabelProvider {

	public String getText(Object obj) {

		if (obj instanceof IDSObject) {
			return getObjectText((IDSObject) obj);
		}

		if (obj instanceof IFormPage)
			return ((IFormPage) obj).getTitle();

		return super.getText(obj);
	}

	public String getObjectText(IDSObject obj) {
		if (obj.getType() == IDSConstants.TYPE_SERVICE) {
			return Messages.DSService_title;
		}
		// if we're a reference and have no name, return the interface
		if (obj.getType() == IDSConstants.TYPE_REFERENCE) {
			IDSReference reference = (IDSReference) obj;
			if (reference.getName() == null
					|| reference.getName().length() == 0)
				return reference.getReferenceInterface();
		}
		return obj.getName();
	}

	public Image getImage(Object obj) {
		if (obj instanceof IDSObject) {
			return getObjectImage((IDSObject) obj);
		}

		// TODO consider changing this
		if (obj instanceof IFormPage)
			return PDEPlugin.getDefault().getLabelProvider().get(
					PDEPluginImages.DESC_PAGE_OBJ);

		return super.getImage(obj);
	}

	private Image getObjectImage(IDSObject obj) {
		if (obj.getType() == IDSConstants.TYPE_IMPLEMENTATION) {
			return SharedImages.getImage(SharedImages.DESC_IMPLEMENTATION);
		} else if (obj.getType() == IDSConstants.TYPE_PROPERTIES) {
			return SharedImages.getImage(SharedImages.DESC_PROPERTIES);
		} else if (obj.getType() == IDSConstants.TYPE_PROPERTY) {
			return SharedImages.getImage(SharedImages.DESC_PROPERTY);
		} else if (obj.getType() == IDSConstants.TYPE_PROVIDE) {
			return SharedImages.getImage(SharedImages.DESC_SERVICE);
		} else if (obj.getType() == IDSConstants.TYPE_REFERENCE) {
			IDSReference reference = (IDSReference) obj;
			int flags = 0;
			
			if (reference == null || reference.getReferencePolicy() == null
					|| reference.getReferenceCardinality() == null)
				return SharedImages
						.getImage(SharedImages.DESC_REFERENCE, flags); 
				
			if (reference.getReferencePolicy().equals(
					IDSConstants.VALUE_REFERENCE_POLICY_DYNAMIC)) {
				flags |= SharedImages.F_DYNAMIC;
			}
			String cardinality = reference.getReferenceCardinality();
			if (cardinality.equals(
					IDSConstants.VALUE_REFERENCE_CARDINALITY_ZERO_ONE)) {
				return SharedImages.getImage(
						SharedImages.DESC_REFERENCE_ZERO_ONE, flags);
			} else if (cardinality.equals(
					IDSConstants.VALUE_REFERENCE_CARDINALITY_ZERO_N)) {
				return SharedImages.getImage(
						SharedImages.DESC_REFERENCE_ZERO_N, flags);
			} else if (cardinality.equals(
					IDSConstants.VALUE_REFERENCE_CARDINALITY_ONE_N)) {
				return SharedImages.getImage(SharedImages.DESC_REFERENCE_ONE_N,
						flags);
			}
			return SharedImages.getImage(SharedImages.DESC_REFERENCE, flags);
		} else if (obj.getType() == IDSConstants.TYPE_COMPONENT) {
			return SharedImages.getImage(SharedImages.DESC_ROOT);
		} else if (obj.getType() == IDSConstants.TYPE_SERVICE) {
			return SharedImages.getImage(SharedImages.DESC_SERVICES);
		}
		return null;
	}
}
