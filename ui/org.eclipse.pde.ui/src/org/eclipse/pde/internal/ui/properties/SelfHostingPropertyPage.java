/*******************************************************************************
 *  Copyright (c) 2003, 2016 IBM Corporation and others.
 *
 *  This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License 2.0
 *  which accompanies this distribution, and is available at
 *  https://www.eclipse.org/legal/epl-2.0/
 *
 *  SPDX-License-Identifier: EPL-2.0
 *
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *     Lars Vogel <Lars.Vogel@vogella.com> - Bug 487988
 *******************************************************************************/
package org.eclipse.pde.internal.ui.properties;

import java.util.ArrayList;
import java.util.StringTokenizer;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ProjectScope;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.jdt.core.*;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.jface.viewers.*;
import org.eclipse.pde.internal.core.ICoreConstants;
import org.eclipse.pde.internal.core.PDECore;
import org.eclipse.pde.internal.ui.*;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.dialogs.PropertyPage;
import org.osgi.service.prefs.BackingStoreException;
import org.osgi.service.prefs.Preferences;

public class SelfHostingPropertyPage extends PropertyPage {

	private Image fImage;
	private CheckboxTableViewer fViewer;

	class ContentProvider implements IStructuredContentProvider {
		@Override
		public Object[] getElements(Object input) {
			return getOutputFolders();
		}
	}

	class FolderLabelProvider extends LabelProvider {
		@Override
		public Image getImage(Object element) {
			return fImage;
		}
	}

	private String[] getOutputFolders() {
		IProject project = getElement().getAdapter(IProject.class);
		ArrayList<String> list = new ArrayList<>();
		try {
			if (project.hasNature(JavaCore.NATURE_ID)) {
				IJavaProject jProject = JavaCore.create(project);
				list.add(jProject.getOutputLocation().toString());
				IClasspathEntry[] entries = jProject.getRawClasspath();
				for (IClasspathEntry entry : entries) {
					if (entry.getEntryKind() == IClasspathEntry.CPE_SOURCE && entry.getContentKind() == IPackageFragmentRoot.K_SOURCE) {
						IPath path = entry.getOutputLocation();
						if (path != null)
							list.add(path.toString());
					}
				}
			}
		} catch (JavaModelException e) {
		} catch (CoreException e) {
		}
		return list.toArray(new String[list.size()]);
	}

	public SelfHostingPropertyPage() {
		fImage = PDEPluginImages.DESC_OUTPUT_FOLDER_OBJ.createImage();
	}

	@Override
	public void dispose() {
		if (fImage != null)
			fImage.dispose();
	}

	/**
	 * @see PreferencePage#createContents(Composite)
	 */
	@Override
	protected Control createContents(Composite parent) {
		Composite composite = new Composite(parent, SWT.NONE);
		composite.setLayout(new GridLayout());
		composite.setLayoutData(new GridData(GridData.FILL_BOTH));

		Label label = new Label(composite, SWT.WRAP);
		label.setText(PDEUIMessages.SelfHostingPropertyPage_label);
		GridData gd = new GridData(GridData.FILL_HORIZONTAL);
		gd.widthHint = 300;
		label.setLayoutData(gd);

		new Label(composite, SWT.NONE);

		label = new Label(composite, SWT.WRAP);
		label.setText(PDEUIMessages.SelfHostingPropertyPage_viewerLabel);

		fViewer = CheckboxTableViewer.newCheckList(composite, SWT.BORDER);
		fViewer.setContentProvider(new ContentProvider());
		fViewer.setLabelProvider(new FolderLabelProvider());
		fViewer.setInput(getElement());
		fViewer.setComparator(new ViewerComparator());
		fViewer.getControl().setLayoutData(new GridData(GridData.FILL_BOTH));

		initialize();

		Dialog.applyDialogFont(composite);
		return composite;
	}

	@Override
	public void createControl(Composite parent) {
		super.createControl(parent);
		PlatformUI.getWorkbench().getHelpSystem().setHelp(getControl(), IHelpContextIds.SELFHOSTING_PROPERTY_PAGE);
	}

	private void initialize() {
		fViewer.setAllChecked(true);
		Preferences pref = getPreferences(getElement().getAdapter(IProject.class));
		if (pref != null) {
			String binExcludes = pref.get(ICoreConstants.SELFHOSTING_BIN_EXCLUDES, ""); //$NON-NLS-1$
			StringTokenizer tokenizer = new StringTokenizer(binExcludes, ","); //$NON-NLS-1$
			while (tokenizer.hasMoreTokens()) {
				String token = tokenizer.nextToken().trim();
				fViewer.setChecked(token, false);
			}
		}
	}

	private Preferences getPreferences(IProject project) {
		return new ProjectScope(project).getNode(PDECore.PLUGIN_ID);
	}

	@Override
	protected void performDefaults() {
		fViewer.setAllChecked(true);
	}

	@Override
	public boolean performOk() {
		Preferences pref = getPreferences(getElement().getAdapter(IProject.class));
		StringBuilder buffer = new StringBuilder();
		for (int i = 0; i < fViewer.getTable().getItemCount(); i++) {
			Object object = fViewer.getElementAt(i);
			if (!fViewer.getChecked(object)) {
				if (buffer.length() > 0)
					buffer.append(","); //$NON-NLS-1$
				buffer.append(object.toString());
			}
		}
		if (pref != null) {
			if (buffer.length() > 0)
				pref.put(ICoreConstants.SELFHOSTING_BIN_EXCLUDES, buffer.toString());
			else
				pref.remove(ICoreConstants.SELFHOSTING_BIN_EXCLUDES);

			try {
				pref.flush();
			} catch (BackingStoreException e) {
				PDEPlugin.logException(e);
			}
		}
		return super.performOk();
	}

}
