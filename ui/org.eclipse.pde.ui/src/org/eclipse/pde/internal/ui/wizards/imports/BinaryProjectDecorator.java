/*
 * (c) Copyright 2001 MyCorporation.
 * All Rights Reserved.
 */
package org.eclipse.pde.internal.ui.wizards.imports;

import org.eclipse.jface.viewers.*;
import org.eclipse.swt.graphics.Image;
import org.eclipse.core.resources.*;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.pde.internal.ui.*;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.pde.internal.ui.util.ImageOverlayIcon;
import org.eclipse.jface.resource.ImageRegistry;
import org.eclipse.swt.widgets.Shell;
import java.util.*;
import org.eclipse.pde.internal.core.*;

/**
 * @version 	1.0
 * @author
 */
public class BinaryProjectDecorator
	extends LabelProvider
	implements ILabelDecorator, IResourceChangeListener {
	private Hashtable registry = new Hashtable();

	public BinaryProjectDecorator() {
		ResourcesPlugin.getWorkspace().addResourceChangeListener(
			this,
			IResourceChangeEvent.POST_AUTO_BUILD);
	}

	/*
	 * @see ILabelDecorator#decorateImage(Image, Object)
	 */
	public Image decorateImage(Image image, Object element) {
		String property = getExternalProjectProperty(element);
		if (property != null) {
			if (property.equalsIgnoreCase(PDECore.EXTERNAL_PROJECT_VALUE)) {
				return findImage(image, true);
			} else if (
				property.equalsIgnoreCase(PDECore.BINARY_PROJECT_VALUE)) {
				return findImage(image, false);
			}
		}
		return image;
	}

	private Image findImage(Image srcImage, boolean external) {
		String key = srcImage.hashCode() + (external ? "e" : "b");
		Image image = (Image) registry.get(key);
		if (image == null) {
			ImageDescriptor desc =
				external
					? PDEPluginImages.DESC_EXTERNAL_CO
					: PDEPluginImages.DESC_BINARY_CO;
			ImageDescriptor overDesc =
				new ImageOverlayIcon(srcImage, new ImageDescriptor[][] { {
				}, {
				}, {
				}, {
					desc }
			});
			image = overDesc.createImage();
			registry.put(key, image);
		}
		return image;
	}

	private String getExternalProjectProperty(Object element) {
		IProject project = null;
		if (element instanceof IProject)
			project = (IProject) element;
		else if (element instanceof IJavaProject)
			project = ((IJavaProject) element).getProject();
		if (project != null) {
			if (!WorkspaceModelManager.isBinaryPluginProject(project))
				return null;
			try {
				return project.getPersistentProperty(
					PDECore.EXTERNAL_PROJECT_PROPERTY);
			} catch (CoreException e) {
			}
		}
		return null;
	}

	/*
	 * @see ILabelDecorator#decorateText(String, Object)
	 */
	public String decorateText(String text, Object element) {
		return text;
	}

	/*
	 * @see IBaseLabelProvider#dispose()
	 */
	public void dispose() {
		for (Enumeration enum = registry.elements(); enum.hasMoreElements();) {
			Image image = (Image) enum.nextElement();
			if (image.isDisposed() == false)
				image.dispose();
		}
		registry.clear();
		ResourcesPlugin.getWorkspace().removeResourceChangeListener(this);
		super.dispose();
	}
	protected LabelProviderChangedEvent[] processDelta(IResourceDelta delta) {
		final ArrayList events = new ArrayList();
		try {
			delta.accept(new IResourceDeltaVisitor() {
				public boolean visit(IResourceDelta delta)
					throws CoreException {
					IResource resource = delta.getResource();
					//skip workspace root
					if (resource.getType() == IResource.ROOT) {
						return true;
					}
					//don't care about deletions
					if (delta.getKind() == IResourceDelta.REMOVED) {
						return false;
					}
					if (resource.getType() == IResource.PROJECT) {
						if (WorkspaceModelManager
							.isPluginProject((IProject) resource)) {
							events.add(createLabelEvent(resource));
						}
						return false;
					}
					return true;
				}
			});
		} catch (CoreException e) {
			PDEPlugin.log(e.getStatus());
		}
		//convert event list to array
		LabelProviderChangedEvent[] result =
			new LabelProviderChangedEvent[events.size()];
		events.toArray(result);
		return result;
	}

	protected LabelProviderChangedEvent createLabelEvent(IResource resource) {
		return new LabelProviderChangedEvent(this, resource);
	}

	public void resourceChanged(IResourceChangeEvent event) {
		//first collect the label change events
		final LabelProviderChangedEvent[] events =
			processDelta(event.getDelta());
		Shell shell = PDEPlugin.getActiveWorkbenchShell();

		//now post the change events to the UI thread
		if (events.length > 0 && shell != null && !shell.isDisposed()) {
			shell.getDisplay().asyncExec(new Runnable() {
				public void run() {
					fireLabelUpdates(events);
				}
			});
		}
	}

	void fireLabelUpdates(final LabelProviderChangedEvent[] events) {
		for (int i = 0; i < events.length; i++) {
			fireLabelProviderChanged(events[i]);
		}
	}
}