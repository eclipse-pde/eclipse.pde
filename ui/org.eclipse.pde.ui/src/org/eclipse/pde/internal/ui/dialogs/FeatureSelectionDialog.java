/*******************************************************************************
 * Copyright (c) 2000, 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Lars Vogel <Lars.Vogel@vogella.com> - Fix for bug 376057 - Wildcard suport 
 *     for adding features in product configuration editor 
 *******************************************************************************/

package org.eclipse.pde.internal.ui.dialogs;

import java.util.Comparator;
import org.eclipse.core.runtime.*;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.pde.internal.core.ifeature.IFeature;
import org.eclipse.pde.internal.core.ifeature.IFeatureModel;
import org.eclipse.pde.internal.ui.*;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.dialogs.FilteredItemsSelectionDialog;

public class FeatureSelectionDialog extends FilteredItemsSelectionDialog {

	private static final String DIALOG_SETTINGS = "org.eclipse.pde.ui.dialogs.FeatureSelectionDialog"; //$NON-NLS-1$
	private IFeatureModel[] fModels;

	private class FeatureSearchItemsFilter extends ItemsFilter {

		public boolean isConsistentItem(Object item) {
			return true;
		}

		public boolean matchItem(Object item) {
			String id = null;
			if (item instanceof IFeatureModel) {
				IFeatureModel model = (IFeatureModel) item;
				id = model.getFeature().getId();
			}

			return (matches(id));
		}

		protected boolean matches(String text) {
			String pattern = patternMatcher.getPattern();
			if (pattern.indexOf("*") != 0 & pattern.indexOf("?") != 0 & pattern.indexOf(".") != 0) {//$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
				pattern = "*" + pattern; //$NON-NLS-1$
				patternMatcher.setPattern(pattern);
			}
			return patternMatcher.matches(text);
		}
	}

	private class FeatureSearchComparator implements Comparator {

		public int compare(Object o1, Object o2) {
			int id1 = getId(o1);
			int id2 = getId(o2);

			if (id1 != id2)
				return id1 - id2;
			return compareSimilarObjects(o1, o2);
		}

		private int getId(Object element) {
			if (element instanceof IFeatureModel) {
				return 100;
			}
			return 0;
		}

		private int compareSimilarObjects(Object o1, Object o2) {
			if (o1 instanceof IFeatureModel && o2 instanceof IFeatureModel) {
				IFeatureModel ipmb1 = (IFeatureModel) o1;
				IFeatureModel ipmb2 = (IFeatureModel) o2;
				return compareFeatures(ipmb1.getFeature(), ipmb2.getFeature());
			}
			return 0;
		}

		private int compareFeatures(IFeature ipmb1, IFeature ipmb2) {
			return ipmb1.getId().compareTo(ipmb2.getId());
		}

	}

	/**
	 * @param parent
	 * @param renderer
	 */
	public FeatureSelectionDialog(Shell parent, IFeatureModel[] models, boolean multiSelect) {
		super(parent, multiSelect);
		setTitle(PDEUIMessages.FeatureSelectionDialog_title);
		setMessage(PDEUIMessages.FeatureSelectionDialog_message);
		this.fModels = models;
		PDEPlugin.getDefault().getLabelProvider().connect(this);
		setListLabelProvider(PDEPlugin.getDefault().getLabelProvider());
	}

	/*
	 * @see org.eclipse.jface.window.Window#configureShell(Shell)
	 */
	protected void configureShell(Shell newShell) {
		super.configureShell(newShell);
		PlatformUI.getWorkbench().getHelpSystem().setHelp(newShell, IHelpContextIds.FEATURE_SELECTION);
	}

	public boolean close() {
		PDEPlugin.getDefault().getLabelProvider().disconnect(this);
		return super.close();
	}

	protected Control createExtendedContentArea(Composite parent) {
		return null;
	}

	protected IDialogSettings getDialogSettings() {
		IDialogSettings settings = PDEPlugin.getDefault().getDialogSettings().getSection(DIALOG_SETTINGS);

		if (settings == null) {
			settings = PDEPlugin.getDefault().getDialogSettings().addNewSection(DIALOG_SETTINGS);
		}

		return settings;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ui.dialogs.FilteredItemsSelectionDialog#validateItem(java.lang.Object)
	 */
	protected IStatus validateItem(Object item) {
		return new Status(IStatus.OK, IPDEUIConstants.PLUGIN_ID, 0, "", null); //$NON-NLS-1$
	}

	protected ItemsFilter createFilter() {
		return new FeatureSearchItemsFilter();
	}

	protected Comparator getItemsComparator() {
		return new FeatureSearchComparator();
	}

	protected void fillContentProvider(AbstractContentProvider contentProvider, ItemsFilter itemsFilter, IProgressMonitor progressMonitor) throws CoreException {
		for (int i = 0; i < fModels.length; i++) {
			contentProvider.add(fModels[i], itemsFilter);
			progressMonitor.worked(1);
		}
		progressMonitor.done();

	}

	public String getElementName(Object item) {
		if (item instanceof IFeatureModel) {
			IFeatureModel model = (IFeatureModel) item;
			return model.getFeature().getId();
		}
		return null;
	}

}
