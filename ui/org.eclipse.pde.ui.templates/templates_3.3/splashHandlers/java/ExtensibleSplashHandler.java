/*******************************************************************************
 * Copyright (c) 2007 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 ******************************************************************************/

package $packageName$;

import java.util.ArrayList;
import java.util.Iterator;

import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtension;
import org.eclipse.core.runtime.Platform;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.eclipse.ui.splash.AbstractSplashHandler;

public class ExtensibleSplashHandler extends AbstractSplashHandler {
	private ArrayList list = new ArrayList();

	public void init(Shell splash) {
		super.init(splash);
		
		// make our composite inherit the splash background
		splash.setBackgroundMode(SWT.INHERIT_DEFAULT);
		
		IExtension[] extensions = Platform.getExtensionRegistry()
				.getExtensionPoint(
						"$pluginId$.splashExtension")
				.getExtensions();

		ArrayList tooltips = new ArrayList();
		for (int i = 0; i < extensions.length; i++) {
			IExtension extension = extensions[i];
			IConfigurationElement[] elements = extension
					.getConfigurationElements();
			for (int j = 0; j < elements.length; j++) {
				IConfigurationElement configurationElement = elements[j];
				String iconName = configurationElement.getAttribute("icon");
				if (iconName == null)
					continue;
				ImageDescriptor imageDesc = AbstractUIPlugin
						.imageDescriptorFromPlugin(configurationElement
								.getNamespaceIdentifier(), iconName);
				Image image = imageDesc.createImage();
				if (image.getBounds().width != 50
						|| image.getBounds().height != 50)
					image.dispose();
				else {
					list.add(image);
					tooltips.add(configurationElement.getAttribute("tooltip"));
				}
			}
		}
		if (list.isEmpty())
			return;

		// determine the number of images we can show per row
		int numPerRow = splash.getSize().x / 50;
		
		Composite iconPanel = new Composite(splash, SWT.NONE);
		//iconPanel.setBackground(splash.getDisplay().getSystemColor(
		//		SWT.COLOR_WIDGET_BACKGROUND));
		
		
		//iconPanel.setBackgroundImage(splash.getBackgroundImage().);
		//
		//iconPanel.setBackgroundMode(SWT.INHERIT_FORCE);
		
		GridLayout gridLayout = new GridLayout(
				Math.min(list.size(), numPerRow), true);
		gridLayout.horizontalSpacing = gridLayout.verticalSpacing = gridLayout.marginHeight = gridLayout.marginWidth = 0;
		iconPanel.setLayout(gridLayout);
		for (Iterator i = list.iterator(), k = tooltips.iterator(); i.hasNext();) {
			Image image = (Image) i.next();
			Label label = new Label(iconPanel, SWT.NONE);
			label.setImage(image);
			label.setToolTipText((String) k.next());
		}
		// create a new background panel to avoid scaling
		//Composite backgroundPanel = new Composite(splash, SWT.NONE);
		//backgroundPanel.setBackgroundImage(splash.getBackgroundImage());
		//backgroundPanel.setBounds(0, 0, splash.getSize().x, splash.getSize().y);

		// determine the size for the new icon panel and position it at the
		// bottom of the splash.
		Point panelSize = iconPanel.computeSize(SWT.DEFAULT, SWT.DEFAULT, true);
		iconPanel.setBounds(splash.getSize().x  - panelSize.x - 5, splash
				.getSize().y - panelSize.y - 5, panelSize.x, panelSize.y);

		// resize the splash to contain the icon panel
		//splash.setSize(splash.getSize().x, splash.getSize().y + panelSize.y);
		iconPanel.layout(true);
		iconPanel.update();


		while (iconPanel.getDisplay().readAndDispatch())
			;
	}

	public void dispose() {
		super.dispose();
		if (!list.isEmpty())
			for (Iterator i = list.iterator(); i.hasNext();) {
				Image image = (Image) i.next();
				image.dispose();
			}
	}
}
