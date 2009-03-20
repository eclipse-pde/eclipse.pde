/*******************************************************************************
 * Copyright (c) 2000, 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.wizards;

import java.util.Comparator;
import org.eclipse.jface.util.*;
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

	private static final Comparator stringComparator = new Comparator() {

		public int compare(Object arg0, Object arg1) {
			if (arg0 instanceof String && arg1 instanceof String)
				return ((String) arg0).compareToIgnoreCase((String) arg1);
			// if not two Strings like we expect, then use default comparator
			return Policy.getComparator().compare(arg0, arg1);
		}

	};

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

		private static IPropertyChangeListener listener = new IPropertyChangeListener() {

			public void propertyChange(PropertyChangeEvent event) {
				if (IPreferenceConstants.PROP_SHOW_OBJECTS.equals(event.getProperty())) {
					cachedIsFullNameModelEnabled = IPreferenceConstants.VALUE_USE_NAMES.equals(event.getNewValue());
				}
			}
		};

		static {
			PDEPlugin.getDefault().getPreferenceStore().addPropertyChangeListener(listener);
		}

		private static boolean cachedIsFullNameModelEnabled = PDEPlugin.isFullNameModeEnabled();

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
