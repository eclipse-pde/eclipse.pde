/*
 * Created on Apr 30, 2004
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
package org.eclipse.pde.internal.ui.wizards.imports;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.preference.*;
import org.eclipse.jface.preference.IPreferenceNode;
import org.eclipse.pde.internal.ui.preferences.TargetPlatformPreferenceNode;
import org.eclipse.swt.custom.BusyIndicator;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.PlatformUI;

/**
 * Action suitable for calling from cheat sheets and
 * other places.
 */
public class ShowTargetPlatformAction extends Action {
	public void run() {
		IPreferenceNode targetNode = new TargetPlatformPreferenceNode();
		boolean result = showPreferencePage(targetNode);
		notifyResult(result);
	}
	private boolean showPreferencePage(final IPreferenceNode targetNode) {
		PreferenceManager manager = new PreferenceManager();
		manager.addToRoot(targetNode);
		Shell shell = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell();
		final PreferenceDialog dialog =
			new PreferenceDialog(shell, manager);
		final boolean[] result = new boolean[] { false };
		BusyIndicator.showWhile(shell.getDisplay(), new Runnable() {
			public void run() {
				dialog.create();
				dialog.setMessage(targetNode.getLabelText());
				if (dialog.open() == PreferenceDialog.OK)
					result[0] = true;
			}
		});
		return result[0];
	}
}