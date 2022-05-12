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
package org.eclipse.pde.spy.preferences.viewer;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.databinding.observable.masterdetail.IObservableFactory;
import org.eclipse.core.databinding.observable.set.IObservableSet;
import org.eclipse.jface.databinding.viewers.ObservableSetTreeContentProvider;
import org.eclipse.jface.databinding.viewers.TreeStructureAdvisor;
import org.eclipse.pde.spy.preferences.model.PreferenceEntry;
import org.eclipse.pde.spy.preferences.model.PreferenceNodeEntry;

@SuppressWarnings("rawtypes")
public class PreferenceEntriesContentProvider extends ObservableSetTreeContentProvider {

	private boolean hierarchicalLayout;

	@SuppressWarnings("unchecked")
	public PreferenceEntriesContentProvider(IObservableFactory setFactory, TreeStructureAdvisor structureAdvisor) {
		super(setFactory, structureAdvisor);
	}

	@Override
	public Object[] getElements(Object inputElement) {
		Object[] children = super.getElements(inputElement);
		if (isHierarchicalLayout()) {
			return children;
		}

		List<PreferenceEntry> childList = new ArrayList<>();

		for (Object object : children) {
			getChildren(object, childList);
		}

		return childList.toArray();
	}

	private void getChildren(Object element, List<PreferenceEntry> childList) {
		if (element instanceof PreferenceNodeEntry) {
			IObservableSet preferenceEntries = ((PreferenceNodeEntry) element).getPreferenceEntries();
			for (Object object : preferenceEntries) {
				getChildren(object, childList);
			}
		} else if (element instanceof PreferenceEntry) {
			childList.add((PreferenceEntry) element);
		}
	}

	public boolean isHierarchicalLayout() {
		return hierarchicalLayout;
	}

	public void setHierarchicalLayout(boolean hierarchicalLayout) {
		this.hierarchicalLayout = hierarchicalLayout;
	}
}
