/*******************************************************************************
 * Copyright (c) 2000, 2003 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.neweditor.plugin;
import java.util.*;
import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.*;
import org.eclipse.jdt.core.*;
import org.eclipse.jdt.ui.*;
import org.eclipse.jface.dialogs.*;
import org.eclipse.jface.viewers.*;
import org.eclipse.pde.internal.ui.*;
import org.eclipse.swt.graphics.*;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.*;
import org.eclipse.ui.dialogs.*;
import org.eclipse.ui.plugin.*;
/**
 * Dialog to browse for package fragments.
 */
public class PackageSelectionDialog extends ElementListSelectionDialog {
	public static final int F_REMOVE_DUPLICATES = 1;
	public static final int F_SHOW_PARENTS = 2;
	public static final int F_HIDE_DEFAULT_PACKAGE = 4;
	private static final String SECTION_TITLE = "ManifestEditor.ExportSection.title"; //$NON-NLS-1$
	private static final String NO_PACKAGES = "PackageSelectionDialog.nopackages.message";
	/** The dialog location. */
	private Point fLocation;
	/** The dialog size. */
	private Point fSize;
	private IProject project;
	private IJavaProject javaProject;
	private int fFlags;
	private IPackageFragment[] packages;
	/**
	 * Creates a package selection dialog.
	 * 
	 * @param parent
	 *            The parent shell
	 * @param context
	 *            The runnable context to run the search in
	 * @param flags
	 *            A combination of <code>F_REMOVE_DUPLICATES</code>,
	 *            <code>F_SHOW_PARENTS</code> and
	 *            <code>F_HIDE_DEFAULT_PACKAGE</code>
	 * @param The
	 *            scope defining the avaiable packages.
	 */
	public PackageSelectionDialog(Shell parent, int flags, IProject project) {
		super(parent, createLabelProvider(flags));
		this.fFlags = flags;
		this.project = project;
		this.packages = null;
		try {
			if (project.hasNature(JavaCore.NATURE_ID))
				this.javaProject = JavaCore.create(project);
		} catch (CoreException e) {
			PDEPlugin.logException(e);
		}
		setMultipleSelection(true);
	}
	private static ILabelProvider createLabelProvider(int dialogFlags) {
		int flags = JavaElementLabelProvider.SHOW_DEFAULT;
		if ((dialogFlags & F_REMOVE_DUPLICATES) == 0) {
			flags = flags | JavaElementLabelProvider.SHOW_ROOT;
		}
		return new PackageLabelProvider(flags, true);
	}
	public int open() {
		try {
			if (javaProject == null) {
				return handleEmptyPackageList();
			}
			IPackageFragmentRoot[] roots = javaProject
					.getPackageFragmentRoots();
			ArrayList packageList = new ArrayList();
			for (int i = 0; i < roots.length; i++) {
				if (roots[i].getKind() != IPackageFragmentRoot.K_BINARY) {
					IJavaElement[] elements = roots[i].getChildren();
					for (int j = 0; j < elements.length; j++) {
						if (!packageList.contains(elements[j])
								&& ((IPackageFragment) elements[j])
										.hasChildren())
							packageList.add(elements[j]);
					}
				}
			}
			if (packageList.isEmpty()) {
				return handleEmptyPackageList();
			}
			setElements(packageList.toArray(new IPackageFragment[packageList
					.size()]));
		} catch (JavaModelException e) {
			PDEPlugin.logException(e);
		}
		return super.open();
	}
	private int handleEmptyPackageList() {
		String title = PDEPlugin.getResourceString(SECTION_TITLE); //$NON-NLS-1$
		String message = PDEPlugin.getFormattedMessage(NO_PACKAGES, project
				.getName()); //$NON-NLS-1$
		MessageDialog.openInformation(getShell(), title, message);
		return CANCEL;
	}
	public boolean close() {
		writeSettings();
		return super.close();
	}
	protected void okPressed() {
		Object[] elements = getSelectedElements();
		packages = new IPackageFragment[elements.length];
		for (int i = 0; i<elements.length; i++)
			packages[i] = (IPackageFragment)elements[i];
		super.okPressed();
	}
	protected Control createContents(Composite parent) {
		Control control = super.createContents(parent);
		readSettings();
		return control;
	}
	protected Point getInitialSize() {
		Point result = super.getInitialSize();
		if (fSize != null) {
			result.x = Math.max(result.x, fSize.x);
			result.y = Math.max(result.y, fSize.y);
			Rectangle display = getShell().getDisplay().getClientArea();
			result.x = Math.min(result.x, display.width);
			result.y = Math.min(result.y, display.height);
		}
		return result;
	}
	protected Point getInitialLocation(Point initialSize) {
		Point result = super.getInitialLocation(initialSize);
		if (fLocation != null) {
			result.x = fLocation.x;
			result.y = fLocation.y;
			Rectangle display = getShell().getDisplay().getClientArea();
			int xe = result.x + initialSize.x;
			if (xe > display.width) {
				result.x -= xe - display.width;
			}
			int ye = result.y + initialSize.y;
			if (ye > display.height) {
				result.y -= ye - display.height;
			}
		}
		return result;
	}
	public IPackageFragment[] getSelectedPackages(){
		if (packages == null)
			return new IPackageFragment[0];
		return packages;
	}
	/**
	 * Initializes itself from the dialog settings with the same state as at the
	 * previous invocation.
	 */
	private void readSettings() {
		IDialogSettings s = getDialogSettings();
		try {
			int x = s.getInt("x"); //$NON-NLS-1$
			int y = s.getInt("y"); //$NON-NLS-1$
			fLocation = new Point(x, y);
			int width = s.getInt("width"); //$NON-NLS-1$
			int height = s.getInt("height"); //$NON-NLS-1$
			fSize = new Point(width, height);
		} catch (NumberFormatException e) {
			fLocation = null;
			fSize = null;
		}
	}
	/**
	 * Stores it current configuration in the dialog store.
	 */
	private void writeSettings() {
		IDialogSettings s = getDialogSettings();
		Point location = getShell().getLocation();
		s.put("x", location.x); //$NON-NLS-1$
		s.put("y", location.y); //$NON-NLS-1$
		Point size = getShell().getSize();
		s.put("width", size.x); //$NON-NLS-1$
		s.put("height", size.y); //$NON-NLS-1$
	}
	/**
	 * Returns the dialog settings object used to share state between several
	 * find/replace dialogs.
	 * 
	 * @return the dialog settings to be used
	 */
	private IDialogSettings getDialogSettings() {
		AbstractUIPlugin plugin = (AbstractUIPlugin) Platform
				.getPlugin(PlatformUI.PLUGIN_ID);
		IDialogSettings settings = plugin.getDialogSettings();
		String sectionName = getClass().getName();
		IDialogSettings subSettings = settings.getSection(sectionName);
		if (subSettings == null)
			subSettings = settings.addNewSection(sectionName);
		return subSettings;
	}
}