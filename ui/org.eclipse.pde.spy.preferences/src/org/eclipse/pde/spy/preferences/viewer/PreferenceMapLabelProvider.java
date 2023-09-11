/*******************************************************************************
 * Copyright (c) 2015 vogella GmbH.
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

import org.eclipse.core.databinding.observable.map.IObservableMap;
import org.eclipse.core.runtime.Assert;
import org.eclipse.jface.databinding.viewers.ObservableMapLabelProvider;
import org.eclipse.jface.resource.FontDescriptor;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.resource.LocalResourceManager;
import org.eclipse.jface.resource.ResourceManager;
import org.eclipse.jface.viewers.IFontProvider;
import org.eclipse.pde.spy.preferences.model.PreferenceEntry;
import org.eclipse.swt.graphics.Font;

public class PreferenceMapLabelProvider extends ObservableMapLabelProvider implements IFontProvider {

	private LocalResourceManager resourceManager;
	private final FontDescriptor fontDescriptor;


	public PreferenceMapLabelProvider(FontDescriptor fontDescriptor, IObservableMap<Object,Object>[] attributeMaps) {
		super(attributeMaps);
		Assert.isNotNull(fontDescriptor, "<fontDescriptor> must not be null");
		this.fontDescriptor = fontDescriptor;
	}

	@Override
	public String getColumnText(Object element, int columnIndex) {
		String columnText = super.getColumnText(element, columnIndex);
		if ("".equals(columnText) && element instanceof PreferenceEntry entry) {
			switch (columnIndex) {
			case 1:
				columnText = entry.getKey();
				break;
			case 2:
				columnText = entry.getOldValue();
				break;
			case 3:
				columnText = entry.getNewValue();
				break;
			default:
				columnText = entry.getNodePath();
				break;
			}
		}
		return columnText;
	}

	@Override
	public Font getFont(Object element) {
		if (element instanceof PreferenceEntry preferenceEntry && preferenceEntry.isRecentlyChanged()) {
			return getResourceManager().create(fontDescriptor);
		}
		return null;
	}

	@Override
	public void dispose() {
		super.dispose();
		if (resourceManager != null) {
			resourceManager.dispose();
		}
	}

	protected ResourceManager getResourceManager() {
		if (null == resourceManager) {
			resourceManager = new LocalResourceManager(JFaceResources.getResources());
		}
		return resourceManager;
	}
}
