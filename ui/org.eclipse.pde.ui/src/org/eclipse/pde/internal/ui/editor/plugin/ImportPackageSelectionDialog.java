/*
 * IBM Confidential
 *
 * OCO Source Materials
 *
 *  Workplace Client Technology - Bundle Developer Kit
 *
 * (C) Copyright IBM Corp. 2003,2004.
 *
 * The source code for this program is not published or other-
 * wise divested of its trade secrets, irrespective of what has
 * been deposited with the U. S. Copyright Office.
 */
package org.eclipse.pde.internal.ui.editor.plugin;

import java.util.Vector;

import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.dialogs.ElementListSelectionDialog;

/**
 * @author Sherry Loats (xiaotong@us.ibm.com)
 *
 */
public class ImportPackageSelectionDialog extends ElementListSelectionDialog {
	
	/**
	 * @param parent
	 * @param renderer
	 */
	public ImportPackageSelectionDialog(Shell parent, ILabelProvider renderer, Vector availablePackages) {
		super(parent, renderer);
		setElements(availablePackages);
		setMultipleSelection(true);
		setTitle(PDEPlugin.getResourceString("ImportPackageSelectionDialog.title")); //$NON-NLS-1$
		setMessage(PDEPlugin.getResourceString("ImportPackageSelectionDialog.description")); //$NON-NLS-1$
	}
	
	private void setElements(Vector availablePackages) {
		setElements(availablePackages.toArray());
	}
}
