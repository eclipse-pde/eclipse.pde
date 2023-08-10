/*******************************************************************************
 * Copyright (c) 2021 Christoph Läubrich and others.
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
package org.eclipse.pde.internal.ui.shared.target;

import java.util.Arrays;
import java.util.Optional;

import org.eclipse.core.runtime.Adapters;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdapterFactory;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.viewers.DelegatingStyledCellLabelProvider.IStyledLabelProvider;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.StyledString;
import org.eclipse.jface.viewers.TreePath;
import org.eclipse.jface.wizard.IWizard;
import org.eclipse.pde.core.target.ITargetDefinition;
import org.eclipse.pde.core.target.ITargetLocation;
import org.eclipse.pde.internal.core.target.TargetReferenceBundleContainer;
import org.eclipse.pde.internal.ui.PDEPluginImages;
import org.eclipse.pde.ui.target.ITargetLocationHandler;
import org.eclipse.swt.graphics.Image;

public class TargetReferenceBundleContainerAdapterFactory implements IAdapterFactory {

	private static final Object[] EMPTY_OBJECTS = new Object[0];

	private static final IStyledLabelProvider STYLE_LABEL_PROVIDER = new WrappedStyledBundleLabelProvider();

	public static final ILabelProvider LABEL_PROVIDER = new LabelProvider() {

		private Image image;

		@Override
		public String getText(Object element) {
			if (element instanceof TargetReferenceBundleContainer container) {
				String name = container.targetDefinition().map(ITargetDefinition::getName).orElse(null);
				if (name != null && !name.isBlank()) {
					return name;
				}
				try {
					return container.getLocation(true);
				} catch (CoreException e) {
					return container.getUri();
				}
			}
			return null;
		}

		@Override
		public Image getImage(Object element) {
			if (element instanceof TargetReferenceBundleContainer) {
				if (image == null) {
					image = PDEPluginImages.DESC_TARGET_DEFINITION.createImage();
				}
				return image;
			}
			return null;
		}

		@Override
		public void dispose() {
			super.dispose();
			if (image != null) {
				image.dispose();
				image = null;
			}
			STYLE_LABEL_PROVIDER.dispose();
		}
	};

	private static final ITargetLocationHandler LOCATION_HANDLER = new ITargetLocationHandler() {

		@Override
		public IStatus reload(ITargetDefinition target, ITargetLocation[] targetLocations, IProgressMonitor monitor) {
			for (ITargetLocation location : targetLocations) {
				if (location instanceof TargetReferenceBundleContainer targetRefrenceBundleContainer) {
					targetRefrenceBundleContainer.reload();
				}
			}
			return Status.OK_STATUS;
		}

		@Override
		public IWizard getEditWizard(ITargetDefinition target, TreePath treePath) {
			Object segment = treePath.getLastSegment();
			if (segment instanceof TargetReferenceBundleContainer) {
				TargetReferenceLocationWizard wizard = new TargetReferenceLocationWizard();
				wizard.setTarget(target);
				wizard.setBundleContainer((TargetReferenceBundleContainer) segment);
				return wizard;
			}
			return null;
		}

		@Override
		public boolean canEdit(ITargetDefinition target, TreePath treePath) {
			return treePath.getLastSegment() instanceof TargetReferenceBundleContainer;
		}
	};

	private static final ITreeContentProvider TREE_CONTENT_PROVIDER = new ITreeContentProvider() {

		@Override
		public boolean hasChildren(Object element) {
			if (element instanceof TargetReferenceBundleContainer) {
				return true;
			}
			if (element instanceof TargetLocationWrapper wrapper) {
				return wrapper.as(ITreeContentProvider.class).map(provider -> provider.hasChildren(wrapper.wrappedItem))
						.orElse(Boolean.FALSE);
			}
			return false;
		}

		@Override
		public Object getParent(Object element) {
			return null;
		}

		@Override
		public Object[] getElements(Object inputElement) {
			return EMPTY_OBJECTS; // will never be called...
		}

		@Override
		public Object[] getChildren(Object parentElement) {
			if (parentElement instanceof TargetReferenceBundleContainer container) {
				ITargetLocation[] targetLocations = container.targetDefinition()
						.map(ITargetDefinition::getTargetLocations).orElse(null);
				if (targetLocations != null && targetLocations.length > 0) {
					return Arrays.stream(targetLocations).map(TargetLocationWrapper::new).toArray();
				}
			}
			if (parentElement instanceof TargetLocationWrapper wrapper) {
				return wrapper.as(ITreeContentProvider.class).map(provider -> provider.getChildren(wrapper.wrappedItem))
						.stream().flatMap(Arrays::stream).map(TargetLocationWrapper::new).toArray();
			}
			return EMPTY_OBJECTS;
		}
	};

	@Override
	public <T> T getAdapter(Object adaptableObject, Class<T> adapterType) {
		if (adaptableObject instanceof TargetReferenceBundleContainer) {
			if (adapterType == ILabelProvider.class) {
				return adapterType.cast(LABEL_PROVIDER);
			}
			if (adapterType == ITargetLocationHandler.class) {
				return adapterType.cast(LOCATION_HANDLER);
			}
			if (adapterType == ITreeContentProvider.class) {
				return adapterType.cast(TREE_CONTENT_PROVIDER);
			}
		}
		if (adaptableObject instanceof TargetLocationWrapper) {
			if (adapterType == IStyledLabelProvider.class) {
				return adapterType.cast(STYLE_LABEL_PROVIDER);
			}
			if (adapterType == ITreeContentProvider.class) {
				return adapterType.cast(TREE_CONTENT_PROVIDER);
			}
		}
		return null;
	}

	@Override
	public Class<?>[] getAdapterList() {
		return new Class<?>[] { ILabelProvider.class, ITargetLocationHandler.class, ITreeContentProvider.class,
				IStyledLabelProvider.class };
	}

	/**
	 * this wrapper prevent several action to take place in case wo would have
	 * returned the {@link ITargetLocation} directly
	 *
	 */
	public static final class TargetLocationWrapper {
		private Object wrappedItem;

		public TargetLocationWrapper(Object location) {
			this.wrappedItem = location;
		}

		<T> Optional<T> as(Class<T> target) {
			return Optional.ofNullable(Adapters.adapt(wrappedItem, target));
		}
	}

	private static final class WrappedStyledBundleLabelProvider extends StyledBundleLabelProvider
			implements IStyledLabelProvider {

		public WrappedStyledBundleLabelProvider() {
			super(false, false);
		}

		@Override
		protected StyledString getStyledString(Object element) {
			if (element instanceof TargetLocationWrapper) {
				element = ((TargetLocationWrapper) element).wrappedItem;
			}
			return super.getStyledString(element);
		}

		@Override
		public Image getImage(Object element) {
			if (element instanceof TargetLocationWrapper) {
				element = ((TargetLocationWrapper) element).wrappedItem;
			}
			return super.getImage(element);
		}

		@Override
		public StyledString getStyledText(Object element) {
			return getStyledString(element);
		}

	}

}
