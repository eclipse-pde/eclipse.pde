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
import org.eclipse.jface.databinding.viewers.ObservableSetTreeContentProvider;
import org.eclipse.jface.databinding.viewers.TreeStructureAdvisor;
import org.eclipse.pde.spy.preferences.model.PreferenceEntry;
import org.eclipse.pde.spy.preferences.model.PreferenceNodeEntry;

@SuppressWarnings({ "rawtypes", "unchecked" })
public class PreferenceEntriesContentProvider extends ObservableSetTreeContentProvider {

	private boolean hierarchicalLayout;

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
			collectLeaves(object, childList);
		}

		return childList.toArray();
	}

	private void collectLeaves(Object element, List<PreferenceEntry> childList) {
		if (element instanceof PreferenceNodeEntry nodeEntry) {
			for (PreferenceEntry child : nodeEntry.getPreferenceEntries()) {
				collectLeaves(child, childList);
			}
		} else if (element instanceof PreferenceEntry preferenceEntry) {
			childList.add(preferenceEntry);
		}
	}

	public boolean isHierarchicalLayout() {
		return hierarchicalLayout;
	}

	public void setHierarchicalLayout(boolean hierarchicalLayout) {
		this.hierarchicalLayout = hierarchicalLayout;
	}
}
