/*******************************************************************************
 * Copyright (c) 2014 OPCoach.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Olivier Prouvost <olivier.prouvost@opcoach.com> - initial API and implementation
 *     Olivier Prouvost <olivier.prouvost@opcoach.com> - Bug 428903 - Having a common 'debug' window for all spies
 *******************************************************************************/
package org.eclipse.pde.spy.core;

import java.text.MessageFormat;
import java.util.List;

import javax.inject.Named;

import org.eclipse.e4.core.di.annotations.Execute;
import org.eclipse.e4.core.di.annotations.Optional;
import org.eclipse.e4.ui.model.application.MApplication;
import org.eclipse.e4.ui.model.application.commands.MCommand;
import org.eclipse.e4.ui.model.application.commands.MParameter;
import org.eclipse.e4.ui.model.application.descriptor.basic.MPartDescriptor;
import org.eclipse.e4.ui.model.application.ui.MElementContainer;
import org.eclipse.e4.ui.model.application.ui.MUIElement;
import org.eclipse.e4.ui.model.application.ui.SideValue;
import org.eclipse.e4.ui.model.application.ui.basic.MPart;
import org.eclipse.e4.ui.model.application.ui.basic.MPartStack;
import org.eclipse.e4.ui.model.application.ui.basic.MStackElement;
import org.eclipse.e4.ui.model.application.ui.basic.MTrimBar;
import org.eclipse.e4.ui.model.application.ui.basic.MTrimmedWindow;
import org.eclipse.e4.ui.model.application.ui.basic.MWindow;
import org.eclipse.e4.ui.model.application.ui.menu.MHandledToolItem;
import org.eclipse.e4.ui.model.application.ui.menu.MToolBar;
import org.eclipse.e4.ui.workbench.modeling.EModelService;
import org.eclipse.e4.ui.workbench.modeling.EPartService;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.util.EcoreUtil;

public class SpyHandler {
	private static final String E4_SPIES_WINDOW = "org.eclipse.pde.spy.core.window";
	private static final String E4_SPIES_PART_STACK = "org.eclipse.pde.spy.core.partStack";

	/**
	 * This method open the spy view part at the correct location : - if it has
	 * never been opened, it add this spy in the E4 window - if it is already
	 * opened, just activate it - if it has already opened and closed, find it, and
	 * open it at the same location
	 *
	 * @param ps           part Service to manage parts elements
	 * @param viewID       the spy view to be displayed
	 * @param appli        current application
	 * @param modelService model service to create elements
	 */
	@Execute
	public void run(EPartService ps, @Optional @Named(SpyProcessor.SPY_COMMAND_PARAM) String viewID, MApplication appli,
			EModelService modelService) {

		// Fix #427 : search for the part in application model instead of current window
		// because the part can be outside of the spy window or searched from another
		// main window
		List<MPart> parts = modelService.findElements(appli, viewID, MPart.class);
		MPart p = (parts.size() >= 1) ? parts.get(0) : null;
		if (p == null) {
			// Create the part in the spyWindow...
			MWindow spyWindow = prepareSpyWindow(appli, modelService);
			MPartStack partStack = (MPartStack) modelService.find(E4_SPIES_PART_STACK, spyWindow);
			p = ps.createPart(viewID);
			partStack.getChildren().add(p);
			partStack.setSelectedElement(p);
		}

		ps.activate(p, true);
		modelService.bringToTop(p);

	}

	/**
	 * Prepare the spy window : add the toolbar Item to be called for each spies in
	 * the tool bar The structure is defined in the fragment.
	 *
	 * @param appli
	 * @param modelService
	 * @return the model Spies window found in fragment.
	 */

	private MWindow prepareSpyWindow(MApplication appli, EModelService modelService) {

		// If window already in application, it is already prepared -> Return it
		List<MWindow> existingWindow = modelService.findElements(appli, E4_SPIES_WINDOW, MWindow.class, null);
		if (existingWindow.size() >= 1)
			return existingWindow.get(0);

		// No spy window in main windows for the moment... extract the structure
		// from the
		// snippet.
		MTrimmedWindow tws = (MTrimmedWindow) modelService.findSnippet(appli, E4_SPIES_WINDOW);

		// Fix #579332 : must copy the snippet to keep it in the snippet list if it must
		// be re-created later.
		EObject eObj = (EObject) tws;
		MTrimmedWindow tw = (MTrimmedWindow) EcoreUtil.copy(eObj);

		MTrimBar trimBar = tw.getTrimBars().stream().filter(t -> t.getSide() == SideValue.TOP).findFirst().get();
		MToolBar toolbar = (MToolBar) trimBar.getChildren().get(0);

		// Get the spy command (added by fragment)
		MCommand spyCmd = appli.getCommand(SpyProcessor.SPY_COMMAND);

		// Create one toolbar element for each 'spy' tagged descriptor
		for (MPartDescriptor mp : appli.getDescriptors()) {
			if (mp.getTags().contains(SpyProcessor.SPY_TAG)) {
				// Create a toolitem bound to the command.
				MHandledToolItem toolItem = modelService.createModelElement(MHandledToolItem.class);
				toolItem.setContributorURI(mp.getContributorURI());

				toolItem.setCommand(spyCmd);
				toolItem.setIconURI(mp.getIconURI());
				toolItem.setLabel(mp.getLabel());
				toolItem.setTooltip(MessageFormat.format(Messages.SpyHandler_Open, mp.getLocalizedLabel()));
				toolbar.getChildren().add(toolItem);

				// Add the parameter for the view to open
				MParameter p = modelService.createModelElement(MParameter.class);
				p.setName(SpyProcessor.SPY_COMMAND_PARAM);
				p.setValue(mp.getElementId());
				toolItem.getParameters().add(p);

			}
		}

		// Can not use move here because it is only for MWindowElement
		centerSpyWindow(appli, tw);
		appli.getChildren().get(0).getWindows().add(tw);

		return tw;

	}

	/**
	 * Make the spy window centered on top of main window.
	 *
	 * @param appli current appli
	 * @param tw    main trim window
	 */
	private void centerSpyWindow(MApplication appli, MTrimmedWindow tw) {
		MWindow mainWindow = appli.getChildren().get(0);
		float ratio = 0.75f;
		int spyW = (int) (mainWindow.getWidth() * ratio);
		int spyH = (int) (mainWindow.getHeight() * ratio);

		tw.setX(mainWindow.getX() + (mainWindow.getWidth() - spyW) / 2);
		tw.setY(mainWindow.getY() + (mainWindow.getHeight() - spyH) / 2);
		tw.setWidth(spyW);
		tw.setHeight(spyH);
	}

}
