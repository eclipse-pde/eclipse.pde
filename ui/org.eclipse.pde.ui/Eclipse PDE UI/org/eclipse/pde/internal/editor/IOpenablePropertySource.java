package org.eclipse.pde.internal.editor;

import org.eclipse.ui.views.properties.*;

public interface IOpenablePropertySource {
	public boolean isOpenable(IPropertySheetEntry entry);
	public void openInEditor(IPropertySheetEntry entry);
}
