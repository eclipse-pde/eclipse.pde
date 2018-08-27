/*******************************************************************************
 * Copyright (c) 2005, 2016 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Lars Vogel <Lars.Vogel@vogella.com> - Bug 487943
 *******************************************************************************/
package org.eclipse.pde.internal.ui.wizards.exports;

import java.io.File;
import java.util.*;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.viewers.*;
import org.eclipse.pde.core.plugin.TargetPlatform;
import org.eclipse.pde.internal.core.PDECore;
import org.eclipse.pde.internal.core.ifeature.IFeatureModel;
import org.eclipse.pde.internal.ui.IHelpContextIds;
import org.eclipse.pde.internal.ui.PDEUIMessages;
import org.eclipse.pde.internal.ui.parts.WizardCheckboxTablePart;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.PlatformUI;

public class CrossPlatformExportPage extends AbstractExportWizardPage {

	private static String CROSS_PLATFORM = "cross-platform"; //$NON-NLS-1$

	class Configuration {
		String os;
		String ws;
		String arch;

		@Override
		public String toString() {
			return os + " (" + ws + "/" + arch + ")"; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		}

		@Override
		public boolean equals(Object obj) {
			if (obj instanceof Configuration) {
				Configuration config = (Configuration) obj;
				return os.equals(config.os) && ws.equals(config.ws) && arch.equals(config.arch);
			}
			return super.equals(obj);
		}

		@Override
		public int hashCode() {
			return os.hashCode() + ws.hashCode() + arch.hashCode();
		}

	}

	class ContentProvider implements IStructuredContentProvider {
		@Override
		public Object[] getElements(Object parent) {
			return getListElements();
		}
	}

	class PlatformPart extends WizardCheckboxTablePart {
		public PlatformPart(String label, String[] buttonLabels) {
			super(label, buttonLabels);
		}

		@Override
		public void updateCounter(int count) {
			super.updateCounter(count);
			pageChanged();
		}

		@Override
		protected void buttonSelected(Button button, int index) {
			switch (index) {
				case 0 :
					handleSelectAll(true);
					break;
				case 1 :
					handleSelectAll(false);
					break;
			}
		}
	}

	private PlatformPart fPlatformPart;
	private IFeatureModel fModel;

	public CrossPlatformExportPage(String pageName, IFeatureModel model) {
		super(pageName);
		fPlatformPart = new PlatformPart(PDEUIMessages.CrossPlatformExportPage_available, new String[] {PDEUIMessages.WizardCheckboxTablePart_selectAll, PDEUIMessages.WizardCheckboxTablePart_deselectAll});
		setTitle(PDEUIMessages.CrossPlatformExportPage_title);
		setDescription(PDEUIMessages.CrossPlatformExportPage_desc);
		fModel = model;
	}

	@Override
	public void createControl(Composite parent) {
		Composite container = new Composite(parent, SWT.NULL);
		container.setLayout(new GridLayout(2, false));

		fPlatformPart.createControl(container);
		TableViewer viewer = fPlatformPart.getTableViewer();
		viewer.setContentProvider(new ContentProvider());
		viewer.setLabelProvider(new LabelProvider());
		fPlatformPart.getTableViewer().setInput(PDECore.getDefault().getFeatureModelManager());

		initialize();
		setControl(container);

		PlatformUI.getWorkbench().getHelpSystem().setHelp(container, IHelpContextIds.CROSS_PLATFORM_EXPORT);
	}

	private void initialize() {
		String value = getDialogSettings().get(CROSS_PLATFORM);
		if (value != null) {
			HashSet<String> set = new HashSet<>();
			StringTokenizer tok = new StringTokenizer(value, ","); //$NON-NLS-1$
			while (tok.hasMoreTokens()) {
				set.add(tok.nextToken());
			}
			ArrayList<Configuration> selected = new ArrayList<>();
			TableItem[] items = fPlatformPart.getTableViewer().getTable().getItems();
			for (TableItem item : items) {
				Configuration config = (Configuration) item.getData();
				if (set.contains(config.toString())) {
					selected.add(config);
				}
			}
			fPlatformPart.setSelection(selected.toArray());
		} else { // we should select at least one, which is the current default
			// TODO clean this horrible mess of a class up
			Configuration config = new Configuration();
			config.ws = TargetPlatform.getWS();
			config.os = TargetPlatform.getOS();
			config.arch = TargetPlatform.getOSArch();
			TableItem[] items = fPlatformPart.getTableViewer().getTable().getItems();
			for (TableItem item : items) {
				Configuration c = (Configuration) item.getData();
				if (c.equals(config)) {
					fPlatformPart.getTableViewer().setChecked(c, true);
					fPlatformPart.updateCounter(1);
				}
			}
		}
		pageChanged();
	}

	@Override
	public void saveSettings(IDialogSettings settings) {
		Object[] objects = fPlatformPart.getSelection();
		StringBuilder buffer = new StringBuilder();
		for (Object object : objects) {
			if (buffer.length() > 0)
				buffer.append(","); //$NON-NLS-1$
			buffer.append(object.toString());
		}
		settings.put(CROSS_PLATFORM, buffer.toString());
	}

	private Configuration[] getListElements() {
		ArrayList<Configuration> list = new ArrayList<>();
		if (fModel != null) {
			File bin = new File(fModel.getInstallLocation(), "bin"); //$NON-NLS-1$
			if (bin.exists() && bin.isDirectory()) {
				File[] children = bin.listFiles();
				for (File child : children) {
					if (child.isDirectory())
						getWS(list, child);
				}
			}
		}
		return list.toArray(new Configuration[list.size()]);
	}

	private void getWS(ArrayList<Configuration> list, File file) {
		File[] children = file.listFiles();
		for (File child : children) {
			if (child.isDirectory())
				getOS(list, child, file.getName());
		}
	}

	private void getOS(ArrayList<Configuration> list, File file, String ws) {
		File[] children = file.listFiles();
		for (int i = 0; i < children.length; i++) {
			if (children[i].isDirectory() && !"CVS".equalsIgnoreCase(children[i].getName())) { //$NON-NLS-1$
				Configuration config = new Configuration();
				config.ws = ws;
				config.os = file.getName();
				config.arch = children[i].getName();
				list.add(config);
			}
		}
	}

	@Override
	protected void pageChanged() {
		setPageComplete(fPlatformPart.getSelectionCount() > 0);
	}

	public String[][] getTargets() {
		Object[] objects = fPlatformPart.getSelection();
		String[][] targets = new String[objects.length][4];
		for (int i = 0; i < objects.length; i++) {
			Configuration config = (Configuration) objects[i];
			String[] combo = new String[4];
			combo[0] = config.os;
			combo[1] = config.ws;
			combo[2] = config.arch;
			combo[3] = ""; //$NON-NLS-1$
			targets[i] = combo;
		}
		return targets;
	}
}
