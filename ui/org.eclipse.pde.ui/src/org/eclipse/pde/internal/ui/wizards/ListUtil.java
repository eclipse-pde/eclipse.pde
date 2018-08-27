/*******************************************************************************
 * Copyright (c) 2000, 2015 IBM Corporation and others.
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
 *******************************************************************************/
package org.eclipse.pde.internal.ui.wizards;

import java.util.Comparator;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.viewers.*;
import org.eclipse.pde.core.plugin.*;
import org.eclipse.pde.internal.core.ifeature.IFeature;
import org.eclipse.pde.internal.core.ifeature.IFeatureModel;
import org.eclipse.pde.internal.ui.IPreferenceConstants;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.pde.internal.ui.elements.ElementLabelProvider;
import org.eclipse.pde.internal.ui.nls.ModelChange;
import org.eclipse.swt.graphics.Image;

public class ListUtil {

	private static final Comparator<String> stringComparator = (arg0, arg1) -> arg0.compareToIgnoreCase(arg1);

	static class NameComparator extends ViewerComparator {
		public NameComparator() {
			// when comparing names, always use the comparator above to do a String comparison
			super(stringComparator);
		}

		public boolean isSorterProperty(Object element, Object propertyId) {
			return propertyId.equals(IBasicPropertyConstants.P_TEXT);
		}
	}

	static class FeatureComparator extends NameComparator {
		@Override
		public int compare(Viewer viewer, Object e1, Object e2) {
			if (e1 instanceof IFeatureModel && e2 instanceof IFeatureModel) {
				IFeature feature1 = ((IFeatureModel) e1).getFeature();
				IFeature feature2 = ((IFeatureModel) e2).getFeature();
				int result = getComparator().compare(feature1.getId(), feature2.getId());
				if (result != 0) {
					return result;
				}
			}
			return super.compare(viewer, e1, e2);
		}
	}

	public static class PluginComparator extends NameComparator {

		private static IPropertyChangeListener listener = event -> {
			if (IPreferenceConstants.PROP_SHOW_OBJECTS.equals(event.getProperty())) {
				cachedIsFullNameModelEnabled = IPreferenceConstants.VALUE_USE_NAMES.equals(event.getNewValue());
			}
		};

		static {
			PDEPlugin.getDefault().getPreferenceStore().addPropertyChangeListener(listener);
		}

		private static boolean cachedIsFullNameModelEnabled = PDEPlugin.isFullNameModeEnabled();

		@Override
		public int compare(Viewer viewer, Object e1, Object e2) {
			int result = 0;
			String name1 = getName(e1);
			String name2 = getName(e2);
			if (name1 != null && name2 != null)
				result = getComparator().compare(name1, name2);
			return (result != 0) ? result : super.compare(viewer, e1, e2);
		}

		private String getName(Object object) {

			if (object instanceof IPluginBase)
				return getPluginName((IPluginBase) object);
			if (object instanceof IPluginModelBase)
				return getPluginName(((IPluginModelBase) object).getPluginBase());
			if (object instanceof ModelEntry) {
				return getPluginName(((ModelEntry) object).getModel().getPluginBase());
			}
			if (object instanceof ModelChange)
				return getPluginName(((ModelChange) object).getParentModel().getPluginBase());
			return null;
		}

		private String getPluginName(IPluginBase pluginBase) {
			return cachedIsFullNameModelEnabled ? pluginBase.getTranslatedName() : pluginBase.getId();
		}
	}

	public static final ViewerComparator NAME_COMPARATOR = new NameComparator();

	public static final ViewerComparator PLUGIN_COMPARATOR = new PluginComparator();

	public static final ViewerComparator FEATURE_COMPARATOR = new FeatureComparator();

	static class TableLabelProvider extends ElementLabelProvider implements ITableLabelProvider {
		@Override
		public String getColumnText(Object o, int index) {
			return getText(o);
		}

		@Override
		public Image getColumnImage(Object o, int index) {
			return getImage(o);
		}
	}

	public static final ILabelProvider TABLE_LABEL_PROVIDER = new TableLabelProvider();

	public ListUtil() {
		super();
	}
}
