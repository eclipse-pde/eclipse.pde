/*******************************************************************************
 * Copyright (c) 2015, 2022 vogella GmbH. and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Simon Scholz <simon.scholz@vogella.com> - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.spy.preferences.parts;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.annotation.PostConstruct;
import javax.inject.Inject;

import org.eclipse.core.databinding.beans.typed.BeanProperties;
import org.eclipse.core.databinding.observable.Realm;
import org.eclipse.core.databinding.observable.set.IObservableSet;
import org.eclipse.core.databinding.property.Properties;
import org.eclipse.core.runtime.preferences.IEclipsePreferences.PreferenceChangeEvent;
import org.eclipse.e4.core.di.annotations.Optional;
import org.eclipse.e4.core.di.extensions.Preference;
import org.eclipse.e4.ui.di.UIEventTopic;
import org.eclipse.e4.ui.dialogs.filteredtree.FilteredTree;
import org.eclipse.e4.ui.model.application.ui.basic.MWindow;
import org.eclipse.e4.ui.workbench.modeling.EModelService;
import org.eclipse.e4.ui.workbench.modeling.ESelectionService;
import org.eclipse.jface.databinding.swt.DisplayRealm;
import org.eclipse.jface.resource.FontDescriptor;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.TreeViewerColumn;
import org.eclipse.pde.spy.preferences.constants.PreferenceConstants;
import org.eclipse.pde.spy.preferences.constants.PreferenceSpyEventTopics;
import org.eclipse.pde.spy.preferences.model.PreferenceEntry;
import org.eclipse.pde.spy.preferences.model.PreferenceEntry.Fields;
import org.eclipse.pde.spy.preferences.model.PreferenceEntryManager;
import org.eclipse.pde.spy.preferences.model.PreferenceEntryPatternFilter;
import org.eclipse.pde.spy.preferences.model.PreferenceNodeEntry;
import org.eclipse.pde.spy.preferences.viewer.PreferenceEntriesContentProvider;
import org.eclipse.pde.spy.preferences.viewer.PreferenceEntryViewerComparator;
import org.eclipse.pde.spy.preferences.viewer.PreferenceMapLabelProvider;
import org.eclipse.pde.spy.preferences.viewer.PreferenceSpyEditingSupport;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Tree;

public class PreferenceSpyPart {

	private FilteredTree filteredTree;
	private boolean hierarchicalLayoutPreference;
	private PreferenceEntryManager preferenceEntryManager;

	@SuppressWarnings("unchecked")
	@PostConstruct
	public void postConstruct(Composite parent, final ESelectionService selectionService, EModelService modelService,
			MWindow window) {

		preferenceEntryManager = new PreferenceEntryManager();

		PreferenceEntryPatternFilter patternFilter = new PreferenceEntryPatternFilter();
		patternFilter.setIncludeLeadingWildcard(true);
		filteredTree = new FilteredTree(parent, SWT.MULTI | SWT.H_SCROLL | SWT.V_SCROLL | SWT.BORDER, patternFilter);

		Tree table = filteredTree.getViewer().getTree();
		table.setHeaderVisible(true);
		table.setLinesVisible(true);

		filteredTree.getViewer().addSelectionChangedListener(event -> {
			ISelection selection = event.getSelection();
			if (selection instanceof IStructuredSelection) {
				ArrayList<PreferenceEntry> preferenceEntries = new ArrayList<>(
						((IStructuredSelection) selection).toList());
				selectionService.setSelection(preferenceEntries);
			}
		});

		createColumn(Fields.nodePath, Messages.PreferenceSpyPart_Nodepath, 300);
		createColumn(Fields.key, Messages.PreferenceSpyPart_Key, 300);
		createColumn(Fields.oldValue, Messages.PreferenceSpyPart_Old_Value, 150);
		createColumn(Fields.newValue, Messages.PreferenceSpyPart_New_Value, 150);

		filteredTree.getViewer().setComparator(new PreferenceEntryViewerComparator());

		FontDescriptor fontDescriptor = getBoldFontDescriptor();

		Realm realm = DisplayRealm.getRealm(filteredTree.getViewer().getControl().getDisplay());
		PreferenceEntriesContentProvider contentProvider = new PreferenceEntriesContentProvider(
				BeanProperties.set("preferenceEntries", PreferenceNodeEntry.class).setFactory(realm), null);
		contentProvider.setHierarchicalLayout(hierarchicalLayoutPreference);
		filteredTree.getViewer().setContentProvider(contentProvider);
		filteredTree.getViewer().setLabelProvider(new PreferenceMapLabelProvider(fontDescriptor,
				Properties.observeEach(contentProvider.getKnownElements(), BeanProperties.values(PreferenceEntry.class, "nodePath", "key", "oldValue", "newValue"))));
		filteredTree.getViewer().setInput(preferenceEntryManager);
	}

	private FontDescriptor getBoldFontDescriptor() {
		Font origFont = filteredTree.getViewer().getControl().getFont();
		FontDescriptor fontDescriptor = FontDescriptor.createFrom(origFont);
		return fontDescriptor.setStyle(SWT.BOLD);
	}

	private void createColumn(Fields field, String columnName, int width) {
		TreeViewerColumn viewerColumn = new TreeViewerColumn(filteredTree.getViewer(), SWT.NONE);
		viewerColumn.getColumn().setWidth(width);
		viewerColumn.getColumn().setText(columnName);

		viewerColumn.setLabelProvider(new ColumnLabelProvider());
		viewerColumn.setEditingSupport(new PreferenceSpyEditingSupport(filteredTree.getViewer(), field));
	}

	@Inject
	public void layoutChanged(
			@Preference(value = PreferenceConstants.HIERARCHICAL_LAYOUT) boolean hierarchicalLayoutPreference) {
		this.hierarchicalLayoutPreference = hierarchicalLayoutPreference;
		if (filteredTree != null && !filteredTree.getViewer().getControl().isDisposed()) {
			PreferenceEntriesContentProvider contentProvider = (PreferenceEntriesContentProvider) filteredTree
					.getViewer().getContentProvider();
			contentProvider.setHierarchicalLayout(hierarchicalLayoutPreference);
			filteredTree.getViewer().refresh();
		}
	}

	@Inject
	@Optional
	public void preferenceChanged(
			@UIEventTopic(PreferenceSpyEventTopics.PREFERENCESPY_PREFERENCE_CHANGED) PreferenceChangeEvent event) {

		PreferenceNodeEntry preferenceNodeEntry = preferenceEntryManager
				.getRecentPreferenceNodeEntry(event.getNode().absolutePath());
		PreferenceEntry preferenceEntry = new PreferenceEntry(event.getNode().absolutePath(), event.getKey());
		preferenceEntry.setRecentlyChanged(true);
		if (null == preferenceNodeEntry) {
			preferenceNodeEntry = new PreferenceNodeEntry(event.getNode().absolutePath());
			preferenceNodeEntry.setRecentlyChanged(true);
			preferenceNodeEntry.addChildren(preferenceEntry);
			preferenceEntry.setParent(preferenceNodeEntry);
			preferenceEntryManager.addChildren(preferenceNodeEntry);
			filteredTree.getViewer().setInput(preferenceEntryManager);
			preferenceEntryManager.putRecentPreferenceEntry(event.getNode().absolutePath(), preferenceNodeEntry);
		} else {
			preferenceEntry.setParent(preferenceNodeEntry);
			PreferenceEntry existingPreferenceEntry = findPreferenceEntry(preferenceEntry);
			if (existingPreferenceEntry != null) {
				preferenceEntry = existingPreferenceEntry;
			} else {
				preferenceNodeEntry.addChildren(preferenceEntry);
			}
		}

		preferenceEntry.setOldValue(String.valueOf(event.getOldValue()));
		preferenceEntry.setNewValue(String.valueOf(event.getNewValue()));
		long currentTimeMillis = System.currentTimeMillis();
		preferenceEntry.setTime(currentTimeMillis);
		preferenceEntry.getParent().setTime(currentTimeMillis);

		filteredTree.getViewer().refresh();
	}

	private PreferenceEntry findPreferenceEntry(PreferenceEntry preferenceEntry) {
		PreferenceEntry parent = preferenceEntry.getParent();
		if (parent instanceof PreferenceNodeEntry) {
			IObservableSet<Object> preferenceEntries = ((PreferenceNodeEntry) parent).getPreferenceEntries();
			for (Object object : preferenceEntries) {
				if (object instanceof PreferenceEntry existingPreferenceEntry) {
					if (existingPreferenceEntry.getKey().equals(preferenceEntry.getKey())) {
						return existingPreferenceEntry;
					}
				}
			}
		}
		return null;
	}

	@Inject
	@Optional
	public void preferenceChanged(
			@UIEventTopic(PreferenceSpyEventTopics.PREFERENCESPY_PREFERENCE_SHOW) Collection<PreferenceEntry> preferenceEntries) {
		preferenceEntryManager.addChildren(preferenceEntries);
		filteredTree.getViewer().refresh();
	}

	@Inject
	@Optional
	public void DeletePreferenceEntries(
			@UIEventTopic(PreferenceSpyEventTopics.PREFERENCESPY_PREFERENCE_ENTRIES_DELETE) List<PreferenceEntry> preferenceEntries) {
		if (preferenceEntries != null && !preferenceEntries.isEmpty()) {
			for (PreferenceEntry preferenceEntry : preferenceEntries) {
				preferenceEntryManager.removeChildren(preferenceEntry);
			}
			preferenceEntryManager.removeChildren(preferenceEntries);
			filteredTree.getViewer().refresh();
		}
	}

	@Inject
	@Optional
	public void DeleteAllPreferenceEntries(
			@UIEventTopic(PreferenceSpyEventTopics.PREFERENCESPY_PREFERENCE_ENTRIES_DELETE_ALL) List<PreferenceEntry> preferenceEntries) {
		if (preferenceEntryManager != null) {
			preferenceEntryManager.clearRecentPreferenceNodeEntry();
			preferenceEntryManager.getPreferenceEntries().clear();
			filteredTree.getViewer().refresh();
		}
	}

	public TreeViewer getViewer() {
		return filteredTree.getViewer();
	}

}
