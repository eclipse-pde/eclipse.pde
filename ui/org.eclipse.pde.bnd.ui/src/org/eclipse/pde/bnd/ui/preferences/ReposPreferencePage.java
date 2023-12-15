/*******************************************************************************
 * Copyright (c) 2015, 2023 bndtools project and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Neil Bartlett <njbartlett@gmail.com>  - initial API and implementation
 *     Sean Bright <sean.bright@gmail.com> - ongoing enhancements
 *     BJ Hargrave <bj@hargrave.dev> - ongoing enhancements
 *     Christoph Läubrich - adapt to PDE code base
 *******************************************************************************/
package org.eclipse.pde.bnd.ui.preferences;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.jface.fieldassist.ControlDecoration;
import org.eclipse.jface.fieldassist.FieldDecorationRegistry;
import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.window.Window;
import org.eclipse.pde.bnd.ui.AddRemoveButtonBarPart;
import org.eclipse.pde.bnd.ui.AddRemoveButtonBarPart.AddRemoveListener;
import org.eclipse.pde.bnd.ui.URLDialog;
import org.eclipse.pde.bnd.ui.URLLabelProvider;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Table;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.osgi.framework.FrameworkUtil;
import org.osgi.service.prefs.BackingStoreException;


public class ReposPreferencePage extends PreferencePage implements IWorkbenchPreferencePage, ReposPreference {

	private static final String REPO_DEFAULT = "https://raw.githubusercontent.com/bndtools/bundle-hub/master/index.xml.gz";
	private boolean			enableTemplateRepo;
	private List<String>	templateRepos;
	private TableViewer		vwrRepos;
	private IEclipsePreferences preferences;

	@Override
	public void init(IWorkbench workbench) {
		preferences = (IEclipsePreferences) InstanceScope.INSTANCE
				.getNode(FrameworkUtil.getBundle(ReposPreferencePage.class).getSymbolicName())
				.node(TEMPLATE_LOADER_NODE);
		enableTemplateRepo = preferences.getBoolean(KEY_ENABLE_TEMPLATE_REPOSITORIES, DEF_ENABLE_TEMPLATE_REPOSITORIES);
		templateRepos = TEMPLATE_REPOSITORIES_PARSER.apply(preferences.get(KEY_TEMPLATE_REPO_URI_LIST,
				REPO_DEFAULT));
	}

	@Override
	protected Control createContents(Composite parent) {
		Composite composite = new Composite(parent, SWT.NONE);
		GridLayout layout = new GridLayout(1, false);
		layout.marginRight = 10;
		composite.setLayout(layout);

		Group group = new Group(composite, SWT.NONE);
		group.setText("Templates Repositories");
		group.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		group.setLayout(new GridLayout(2, false));

		final Button btnEnableTemplateRepo = new Button(group, SWT.CHECK);
		btnEnableTemplateRepo.setText("Enable templates repositories");
		btnEnableTemplateRepo.setSelection(enableTemplateRepo);
		btnEnableTemplateRepo.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false, 2, 1));

		ControlDecoration decoration = new ControlDecoration(btnEnableTemplateRepo, SWT.RIGHT | SWT.TOP, composite);
		decoration.setImage(FieldDecorationRegistry.getDefault()
			.getFieldDecoration(FieldDecorationRegistry.DEC_INFORMATION)
			.getImage());
		decoration.setMarginWidth(3);
		decoration.setDescriptionText(
			"These repositories are used to load\ntemplates, in addition to repositories\nconfigured in the Bnd OSGi Workspace.");
		decoration.setShowHover(true);
		decoration.setShowOnlyOnFocus(false);

		Label lblRepos = new Label(group, SWT.NONE);
		lblRepos.setText("Repository URLs:");
		lblRepos.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false, 2, 1));

		final Table tblRepos = new Table(group, SWT.BORDER | SWT.MULTI);
		vwrRepos = new TableViewer(tblRepos);
		vwrRepos.setContentProvider(ArrayContentProvider.getInstance());
		vwrRepos.setLabelProvider(new URLLabelProvider());
		vwrRepos.setInput(templateRepos);

		GridData gd = new GridData(SWT.FILL, SWT.CENTER, true, false);
		gd.widthHint = 260;
		gd.heightHint = 80;
		tblRepos.setLayoutData(gd);
		tblRepos.setEnabled(enableTemplateRepo);

		final AddRemoveButtonBarPart addRemoveRepoPart = new AddRemoveButtonBarPart();
		Control addRemovePanel = addRemoveRepoPart.createControl(group, SWT.FLAT | SWT.VERTICAL);
		addRemovePanel.setLayoutData(new GridData(SWT.FILL, SWT.TOP, false, false));
		addRemoveRepoPart.setRemoveEnabled(false);
		addRemoveRepoPart.addListener(new AddRemoveListener() {
			@Override
			public void addSelected() {
				doAddRepo();
			}

			@Override
			public void removeSelected() {
				doRemoveRepo();
			}
		});
		vwrRepos.addSelectionChangedListener(event -> addRemoveRepoPart.setRemoveEnabled(!vwrRepos.getSelection()
			.isEmpty()));
		tblRepos.addKeyListener(new KeyAdapter() {
			@Override
			public void keyPressed(KeyEvent e) {
				if (e.keyCode == SWT.DEL && e.stateMask == 0)
					doRemoveRepo();
			}
		});

		btnEnableTemplateRepo.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent ev) {
				enableTemplateRepo = btnEnableTemplateRepo.getSelection();
				tblRepos.setEnabled(enableTemplateRepo);
				validate();
			}
		});

		return composite;
	}

	private void doAddRepo() {
		URLDialog dialog = new URLDialog(getShell(), "Add repository URL", false);
		if (dialog.open() == Window.OK) {
			URI location = dialog.getLocation();

			String locationStr = location.toString();
			templateRepos.add(locationStr);
			vwrRepos.add(locationStr);
		}
	}

	private void doRemoveRepo() {
		int[] selectedIndexes = vwrRepos.getTable()
			.getSelectionIndices();
		if (selectedIndexes == null)
			return;
		List<Object> selected = new ArrayList<>(selectedIndexes.length);
		for (int index : selectedIndexes) {
			selected.add(templateRepos.get(index));
		}
		templateRepos.removeAll(selected);
		vwrRepos.remove(selected.toArray());
		validate();
	}

	private void validate() {
		String error = null;
		if (enableTemplateRepo) {
			for (String templateRepo : templateRepos) {
				try {
					@SuppressWarnings("unused")
					URI uri = new URI(templateRepo);
				} catch (URISyntaxException e) {
					error = "Invalid URL: " + e.getMessage();
				}
			}
		}
		setErrorMessage(error);
		setValid(error == null);
	}

	@Override
	public boolean performOk() {
		String repoList = templateRepos.stream().collect(Collectors.joining("\t"));
		if (enableTemplateRepo == DEF_ENABLE_TEMPLATE_REPOSITORIES) {
			preferences.remove(KEY_ENABLE_TEMPLATE_REPOSITORIES);
			if (REPO_DEFAULT.equals(repoList)) {
				preferences.remove(KEY_TEMPLATE_REPO_URI_LIST);
			} else {
				preferences.put(KEY_TEMPLATE_REPO_URI_LIST, repoList);
			}
		} else {
			preferences.putBoolean(KEY_ENABLE_TEMPLATE_REPOSITORIES, enableTemplateRepo);
			preferences.put(KEY_TEMPLATE_REPO_URI_LIST, repoList);
		}
		try {
			preferences.flush();
		} catch (BackingStoreException e) {
		}
		return true;
	}

}
