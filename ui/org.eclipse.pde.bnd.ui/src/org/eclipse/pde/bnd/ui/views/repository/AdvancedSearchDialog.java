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
 *     Neil Bartlett <njbartlett@gmail.com> - initial API and implementation
 *     BJ Hargrave <bj@bjhargrave.com> - ongoing enhancements
 *     Christoph Rueger <chrisrueger@gmail.com> - ongoing enhancements
 *******************************************************************************/
package org.eclipse.pde.bnd.ui.views.repository;

import java.beans.PropertyChangeListener;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.TabFolder;
import org.eclipse.swt.widgets.TabItem;
import org.eclipse.ui.IMemento;
import org.eclipse.ui.IPersistable;
import org.eclipse.ui.XMLMemento;
import org.osgi.resource.Requirement;

public class AdvancedSearchDialog extends TitleAreaDialog implements IPersistable {

	private final Map<String, SearchPanel>	panelMap		= new LinkedHashMap<>();

	private TabFolder						tabFolder;
	private int								activeTabIndex	= 0;
	private Requirement						requirement		= null;

	public AdvancedSearchDialog(Shell parentShell) {
		super(parentShell);
		setShellStyle(getShellStyle() | SWT.RESIZE);

		panelMap.put("Package", new PackageSearchPanel());
		panelMap.put("Service", new ServiceSearchPanel());
		panelMap.put("Other", new ArbitraryNamespaceSearchPanel());
	}

	@Override
	protected Control createDialogArea(Composite parent) {
		setTitle("Advanced Search");
		setMessage(
			"Perform a search for resource capabilities in the Repositories. Select the\ncapability namespace using the tab bar.");

		Composite area = (Composite) super.createDialogArea(parent);

		tabFolder = new TabFolder(area, SWT.TOP);
		tabFolder.setLayoutData(new GridData(GridData.FILL_BOTH));

		PropertyChangeListener changeListener = evt -> updateFromPanel();

		final List<Image> images = new LinkedList<>();

		for (Entry<String, SearchPanel> panelEntry : panelMap.entrySet()) {
			String title = panelEntry.getKey();
			SearchPanel panel = panelEntry.getValue();

			Composite container = new Composite(tabFolder, SWT.NONE);
			container.setLayout(new GridLayout(1, false));

			Control control = panel.createControl(container);
			GridData controlLayoutData = new GridData(SWT.FILL, SWT.CENTER, true, true);
			controlLayoutData.widthHint = 200;
			control.setLayoutData(controlLayoutData);

			TabItem item = new TabItem(tabFolder, SWT.NONE);
			item.setText(title);
			item.setControl(container);
			item.setData(panel);

			Image image = panel.createImage(tabFolder.getDisplay());
			if (image != null) {
				images.add(image);
				item.setImage(image);
			}

			panel.addPropertyChangeListener(changeListener);
		}

		tabFolder.addDisposeListener(e -> {
			for (Image image : images) {
				if (!image.isDisposed())
					image.dispose();
			}
		});

		tabFolder.setSelection(activeTabIndex);
		SearchPanel currentPanel = (SearchPanel) tabFolder.getItem(activeTabIndex)
			.getData();
		currentPanel.setFocus();
		requirement = currentPanel.getRequirement();

		tabFolder.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				activeTabIndex = tabFolder.getSelectionIndex();
				updateFromPanel();
				getSelectedPanel().setFocus();
			}
		});

		return area;
	}

	private SearchPanel getSelectedPanel() {
		int index = tabFolder.getSelectionIndex();
		TabItem item = tabFolder.getItem(index);
		return (SearchPanel) item.getData();
	}

	private void updateFromPanel() {
		SearchPanel panel = getSelectedPanel();
		setErrorMessage(panel.getError());
		requirement = panel.getRequirement();
		getButton(OK).setEnabled(requirement != null);
	}

	@Override
	protected void createButtonsForButtonBar(Composite parent) {
		super.createButtonsForButtonBar(parent);

		Button okButton = getButton(OK);
		okButton.setText("Search");
		okButton.setEnabled(requirement != null);
	}

	public Requirement getRequirement() {
		return requirement;
	}

	@Override
	public void saveState(IMemento memento) {
		memento.putInteger("tabIndex", activeTabIndex);

		for (Entry<String, SearchPanel> panelEntry : panelMap.entrySet()) {
			IMemento childMemento = memento.createChild("tab", panelEntry.getKey());
			SearchPanel panel = panelEntry.getValue();
			panel.saveState(childMemento);
		}
	}

	public void restoreState(IMemento memento) {
		activeTabIndex = memento.getInteger("tabIndex");

		IMemento[] children = memento.getChildren("tab");
		for (IMemento childMemento : children) {
			String key = childMemento.getID();
			SearchPanel panel = panelMap.get(key);
			if (panel != null)
				panel.restoreState(childMemento);
		}
	}

	/**
	 * @param req a requirement
	 * @return a state object for the given requirement to open advancedSearch
	 *         prefilled in the "Other" tab.
	 */
	public static IMemento toNamespaceSearchPanelMemento(Requirement req) {
		XMLMemento memento = XMLMemento.createWriteRoot("search");
		memento.putInteger("tabIndex", 2);
		IMemento other = memento.createChild("tab", "Other");
		other.putString("namespace", req.getNamespace());
		other.putString("filter", req.getDirectives()
			.get("filter"));
		return memento;
	}

}
