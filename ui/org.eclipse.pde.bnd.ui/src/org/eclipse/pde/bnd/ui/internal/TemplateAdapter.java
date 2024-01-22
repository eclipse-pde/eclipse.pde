/*******************************************************************************
 * Copyright (c) 2023 Christoph Läubrich and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 * 
 * Contributors:
 *     Christoph Läubrich - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.bnd.ui.internal;

import java.net.MalformedURLException;
import java.net.URI;

import org.bndtools.templating.Template;
import org.eclipse.core.runtime.AdapterTypes;
import org.eclipse.core.runtime.IAdapterFactory;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.resource.ImageRegistry;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.pde.bnd.ui.templating.RepoTemplateLabelProvider;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Display;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;

@Component
@AdapterTypes(adaptableClass = Template.class, adapterNames = { ILabelProvider.class, Image.class })
public class TemplateAdapter implements IAdapterFactory {

	private RepoTemplateLabelProvider labelProvider = new RepoTemplateLabelProvider();
	private ImageRegistry imageRegistry;

	@Override
	public <T> T getAdapter(Object adaptableObject, Class<T> adapterType) {
		if (adaptableObject instanceof Template template) {
			if (adapterType == ILabelProvider.class) {
				return adapterType.cast(labelProvider);
			}
			if (adapterType == Image.class) {
				URI icon = template.getIcon();
				ImageRegistry registry = getImageRegistry();
				String key = icon.toASCIIString();
				ImageDescriptor descriptor = registry.getDescriptor(key);
				if (descriptor == null) {
					try {
						registry.put(key, ImageDescriptor.createFromURL(icon.toURL()));
					} catch (MalformedURLException e) {
						return null;
					}
				}
				return adapterType.cast(registry.get(key));
			}
		}
		return null;
	}

	@Deactivate
	void dispose() {
		labelProvider.dispose();
		synchronized (this) {
			if (imageRegistry != null) {
				imageRegistry.dispose();
			}
		}
	}

	private ImageRegistry getImageRegistry() {
		synchronized (this) {
			if (imageRegistry == null) {
				imageRegistry = new ImageRegistry(Display.getCurrent());
			}
			return imageRegistry;
		}
	}

}
