/*******************************************************************************
 * Copyright (c) 2019 Ed Scadding.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Ed Scadding <edscadding@secondfiddle.org.uk> - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.views.features.action;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import org.eclipse.core.resources.IResource;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredViewer;
import org.eclipse.pde.core.IModel;
import org.eclipse.pde.core.plugin.IPluginModelBase;
import org.eclipse.pde.internal.core.ifeature.IFeatureModel;
import org.eclipse.pde.internal.core.iproduct.IProductModel;
import org.eclipse.pde.internal.ui.PDEUIMessages;
import org.eclipse.pde.internal.ui.views.features.support.FeaturesViewInput;
import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.dnd.FileTransfer;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.widgets.Event;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.actions.ActionFactory;
import org.eclipse.ui.part.ResourceTransfer;

public class FeatureAndPluginCopyAction extends Action {

	private final StructuredViewer fStructuredViewer;

	private final Clipboard fClipboard;

	private final FeaturesViewInput fFeaturesViewInput;

	public FeatureAndPluginCopyAction(StructuredViewer structuredViewer, Clipboard clipboard,
			FeaturesViewInput featuresViewInput) {
		super(PDEUIMessages.FeaturesView_FeatureAndPluginCopyAction_label,
				PlatformUI.getWorkbench().getSharedImages().getImageDescriptor(ISharedImages.IMG_TOOL_COPY));
		fStructuredViewer = structuredViewer;
		fClipboard = clipboard;
		fFeaturesViewInput = featuresViewInput;

		setActionDefinitionId(ActionFactory.COPY.getCommandId());
	}

	@Override
	public void runWithEvent(Event event) {
		IStructuredSelection selection = fStructuredViewer.getStructuredSelection();
		List<IModel> models = toModels(selection);

		String textData = toTextData(models);
		IResource[] projectResources = toProjectResources(models);
		String[] fileData = toFileData(projectResources);

		Object[] data = { textData, projectResources, fileData };
		Transfer[] dataTypes = { TextTransfer.getInstance(), ResourceTransfer.getInstance(),
				FileTransfer.getInstance() };
		fClipboard.setContents(data, dataTypes);
	}

	private List<IModel> toModels(IStructuredSelection selection) {
		List<Object> selectionList = selection.toList();
		return selectionList.stream()
				.map(this::toModel)
				.filter(Objects::nonNull)
				.collect(Collectors.toList());
	}

	private IModel toModel(Object element) {
		IModel model = fFeaturesViewInput.getFeatureSupport().toFeatureModel(element);
		if (model == null) {
			model = fFeaturesViewInput.getPluginSupport().toPluginModel(element);
		}
		if (model == null) {
			model = fFeaturesViewInput.getProductSupport().toProductModel(element);
		}

		return model;
	}

	private String toTextData(Collection<IModel> models) {
		return models.stream()
				.map(this::toModelId)
				.collect(Collectors.joining(System.lineSeparator()));
	}

	private String toModelId(IModel model) {
		if (model instanceof IFeatureModel) {
			return ((IFeatureModel) model).getFeature().getId();
		} else if (model instanceof IPluginModelBase) {
			return ((IPluginModelBase) model).getPluginBase().getId();
		} else if (model instanceof IProductModel) {
			return ((IProductModel) model).getProduct().getId();
		}

		return null;
	}

	private IResource[] toProjectResources(Collection<IModel> models) {
		return models.stream()
				.map(IModel::getUnderlyingResource)
				.filter(Objects::nonNull)
				.map(IResource::getProject)
				.distinct().toArray(IResource[]::new);
	}

	private String[] toFileData(IResource[] resources) {
		return Arrays.stream(resources)
				.map(resource -> resource.getLocation().toOSString())
				.toArray(String[]::new);
	}

}
