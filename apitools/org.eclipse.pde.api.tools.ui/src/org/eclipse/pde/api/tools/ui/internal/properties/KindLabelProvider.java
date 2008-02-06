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
package org.eclipse.pde.api.tools.ui.internal.properties;

import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.pde.api.tools.ui.internal.properties.ApiFiltersPropertyPage.ApiKindDescription;

/**
 * label provider for {@link ApiKindDescription}s
 * 
 * @see KindSelectionDialog
 * @see EditApiFilterDialog
 * 
 * @since 1.0.0
 */
public class KindLabelProvider extends LabelProvider {
	/* (non-Javadoc)
	 * @see org.eclipse.jface.viewers.LabelProvider#getText(java.lang.Object)
	 */
	public String getText(Object element) {
		if(element instanceof ApiKindDescription) {
			ApiKindDescription desc = (ApiKindDescription) element;
			return desc.kind;
		}
		return super.getText(element);
	}
}
