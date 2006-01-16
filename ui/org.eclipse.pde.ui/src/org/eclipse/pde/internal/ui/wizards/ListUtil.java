/*******************************************************************************
 * Copyright (c) 2000, 2003 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.wizards;

import org.eclipse.jface.viewers.IBasicPropertyConstants;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerSorter;
import org.eclipse.pde.core.plugin.IPluginBase;
import org.eclipse.pde.core.plugin.IPluginModelBase;
import org.eclipse.pde.internal.core.ModelEntry;
import org.eclipse.pde.internal.core.ifeature.IFeature;
import org.eclipse.pde.internal.core.ifeature.IFeatureModel;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.pde.internal.ui.elements.ElementLabelProvider;
import org.eclipse.pde.internal.ui.nls.ModelChange;
import org.eclipse.swt.graphics.Image;


public class ListUtil {
	static class NameSorter extends ViewerSorter {
		public boolean isSorterProperty(Object element, Object propertyId) {
			return propertyId.equals(IBasicPropertyConstants.P_TEXT);
		}
	}
	static class FeatureSorter extends NameSorter {
		public int compare(Viewer viewer, Object e1, Object e2) {
			if (e1 instanceof IFeatureModel && e2 instanceof IFeatureModel) {
				IFeature feature1 = ((IFeatureModel)e1).getFeature();
				IFeature feature2 = ((IFeatureModel)e2).getFeature();
				int result = collator.compare(feature1.getId(),feature2.getId());
				if (result != 0) {
					return result;
				}
			}
			return super.compare(viewer,e1,e2);
		}
	}
	public static class PluginSorter extends NameSorter {
		public int compare(Viewer viewer, Object e1, Object e2) {
			int result = 0;
			String name1 = getName(e1);
			String name2 = getName(e2);
			if (name1 != null && name2 != null)
				result = collator.compare(name1, name2);
			return (result != 0) ? result : super.compare(viewer, e1, e2);
		}

		private String getName(Object object) {
			if (object instanceof IPluginBase)
				return getPluginName((IPluginBase) object);
			if (object instanceof IPluginModelBase)
				return getPluginName(
					((IPluginModelBase) object).getPluginBase());
			if (object instanceof ModelEntry)
				return getPluginName(
					((ModelEntry) object).getActiveModel().getPluginBase());
			if (object instanceof ModelChange)
				return getPluginName(
						((ModelChange)object).getParentModel().getPluginBase());
			return null;
		}

		private String getPluginName(IPluginBase pluginBase) {
			return PDEPlugin.isFullNameModeEnabled()
				? pluginBase.getTranslatedName()
				: pluginBase.getId();
		}
	}
	

	public static final ViewerSorter NAME_SORTER = new NameSorter();
	
	public static final ViewerSorter PLUGIN_SORTER = new PluginSorter();
	
	public static final ViewerSorter FEATURE_SORTER = new FeatureSorter();

	static class TableLabelProvider extends ElementLabelProvider implements ITableLabelProvider {
		public String getColumnText(Object o, int index) {
			return getText(o);
		}
		public Image getColumnImage(Object o, int index) {
			return getImage(o);
		}
	}

	public static final ILabelProvider TABLE_LABEL_PROVIDER = new TableLabelProvider();

public ListUtil() {
	super();
}
}
