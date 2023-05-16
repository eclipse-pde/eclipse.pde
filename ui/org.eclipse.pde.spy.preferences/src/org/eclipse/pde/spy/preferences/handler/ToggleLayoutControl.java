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
package org.eclipse.pde.spy.preferences.handler;

import java.net.URL;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.inject.Inject;

import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.e4.core.di.extensions.Preference;
import org.eclipse.e4.core.services.log.Logger;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.resource.LocalResourceManager;
import org.eclipse.jface.resource.ResourceManager;
import org.eclipse.pde.spy.preferences.constants.PreferenceConstants;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.swt.widgets.ToolItem;
import org.osgi.framework.Bundle;
import org.osgi.framework.FrameworkUtil;
import org.osgi.service.prefs.BackingStoreException;

@SuppressWarnings("restriction")
public class ToggleLayoutControl {

	@Inject
	Logger LOG;

	private ToolItem toolItem;
	private ResourceManager resourceManager;

	@Inject
	public void tracePreferenceChanged(
			@Preference(value = PreferenceConstants.HIERARCHICAL_LAYOUT) boolean hierarchicalLayoutPreference) {
		if (toolItem != null && !toolItem.isDisposed()) {
			toolItem.setSelection(hierarchicalLayoutPreference);
			toolItem.setImage(
					hierarchicalLayoutPreference ? getResourceManager().createImage(getHierarchicalImageDescriptor())
							: getResourceManager().createImage(getFlatImageDescriptor()));
			toolItem.setToolTipText(
					hierarchicalLayoutPreference ? Messages.ToggleLayoutControl_Toggle_to_flat_layout : Messages.ToggleLayoutControl_Toggle_to_hierarchical_layout);
		}
	}

	@PostConstruct
	public void createGui(Composite parent, final @Preference IEclipsePreferences preferences,
			@Preference(value = PreferenceConstants.HIERARCHICAL_LAYOUT) boolean hierarchicalLayoutPreference) {
		ToolBar toolBar = new ToolBar(parent, SWT.NONE);
		toolItem = new ToolItem(toolBar, SWT.CHECK);
		toolItem.setToolTipText(
				hierarchicalLayoutPreference ? Messages.ToggleLayoutControl_Toggle_to_flat_layout : Messages.ToggleLayoutControl_Toggle_to_hierarchical_layout);
		toolItem.addSelectionListener(SelectionListener.widgetSelectedAdapter(event -> {
			Object source = event.getSource();
			if (source instanceof ToolItem) {
				preferences.putBoolean(PreferenceConstants.HIERARCHICAL_LAYOUT, ((ToolItem) source).getSelection());
				try {
					preferences.flush();
				} catch (BackingStoreException e) {
					LOG.error(e);
				}
			}
		}));
		tracePreferenceChanged(hierarchicalLayoutPreference);
	}

	@PreDestroy
	public void dispose() {
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

	protected ImageDescriptor getFlatImageDescriptor() {
		Bundle bundle = FrameworkUtil.getBundle(getClass());
		URL url = FileLocator.find(bundle, IPath.fromOSString("$nl$/icons/flatLayout.png"), null);
		return ImageDescriptor.createFromURL(url);
	}

	protected ImageDescriptor getHierarchicalImageDescriptor() {
		Bundle bundle = FrameworkUtil.getBundle(getClass());
		URL url = FileLocator.find(bundle, IPath.fromOSString("$nl$/icons/hierarchicalLayout.png"), null);
		return ImageDescriptor.createFromURL(url);
	}
}
