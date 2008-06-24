/*******************************************************************************
 * Copyright (c) 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Chris Aniszczyk <caniszczyk@gmail.com>
 *     Rafael Oliveira Nóbrega <rafael.oliveira@gmail.com>
 *******************************************************************************/
package org.eclipse.pde.internal.ds.ui.editor;

import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.pde.internal.ds.core.IDSConstants;
import org.eclipse.pde.internal.ds.core.IDSObject;
import org.eclipse.pde.internal.ds.ui.SharedImages;
import org.eclipse.swt.graphics.Image;

public class DSLabelProvider extends LabelProvider {

	public String getText(Object obj) {

		if (obj instanceof IDSObject) {
			return getObjectText((IDSObject) obj);
		}

		return super.getText(obj);
	}

	public String getObjectText(IDSObject obj) {
		return obj.getName();
	}

	public Image getImage(Object obj) {
		if (obj instanceof IDSObject) {
			return getObjectImage((IDSObject) obj);
		}
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
			return SharedImages.getImage(SharedImages.DESC_PROVIDE);
		} else if (obj.getType() == IDSConstants.TYPE_REFERENCE) {
			return SharedImages.getImage(SharedImages.DESC_REFERENCE);
		} else if (obj.getType() == IDSConstants.TYPE_COMPONENT) {
			return SharedImages.getImage(SharedImages.DESC_ROOT);
		} else if (obj.getType() == IDSConstants.TYPE_SERVICE) {
			return SharedImages.getImage(SharedImages.DESC_SERVICE);
		}
		return null;
	}

}
