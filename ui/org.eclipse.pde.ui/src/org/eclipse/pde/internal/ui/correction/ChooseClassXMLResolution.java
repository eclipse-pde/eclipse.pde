package org.eclipse.pde.internal.ui.correction;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.search.SearchEngine;
import org.eclipse.jdt.ui.IJavaElementSearchConstants;
import org.eclipse.jdt.ui.JavaUI;
import org.eclipse.jface.window.Window;
import org.eclipse.pde.core.plugin.IPluginModelBase;
import org.eclipse.pde.internal.core.text.IDocumentNode;
import org.eclipse.pde.internal.core.text.plugin.PluginAttribute;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.pde.internal.ui.PDEUIMessages;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.dialogs.SelectionDialog;

public class ChooseClassXMLResolution extends AbstractXMLMarkerResolution {

	public ChooseClassXMLResolution(int resolutionType, IMarker marker) {
		super(resolutionType, marker);
	}

	protected void createChange(IPluginModelBase model) {
		Object object = findNode(model);
		if (!(object instanceof PluginAttribute))
			return;
		PluginAttribute attrib = (PluginAttribute)object;
		IDocumentNode element = attrib.getEnclosingElement();
		try {
			SelectionDialog dialog = JavaUI.createTypeDialog(
					PDEPlugin.getActiveWorkbenchShell(),
					PlatformUI.getWorkbench().getProgressService(),
					SearchEngine.createWorkspaceScope(),
					IJavaElementSearchConstants.CONSIDER_ALL_TYPES, 
                    false, ""); //$NON-NLS-1$
			dialog.setTitle(PDEUIMessages.ClassAttributeRow_dialogTitle); 
			if (dialog.open() == Window.OK) {
				IType type = (IType) dialog.getResult()[0];
				element.setXMLAttribute(attrib.getName(), type.getFullyQualifiedName('$'));
			}
		} catch (CoreException e) {
		}
	}

	public String getDescription() {
		return getLabel();
	}

	public String getLabel() {
		return PDEUIMessages.ChooseClassXMLResolution_label;
	}

}
