package org.eclipse.pde.internal.editor;
/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */

import org.eclipse.ui.views.properties.*;

public interface IOpenablePropertySource {
	public boolean isOpenable(IPropertySheetEntry entry);
	public void openInEditor(IPropertySheetEntry entry);
}
