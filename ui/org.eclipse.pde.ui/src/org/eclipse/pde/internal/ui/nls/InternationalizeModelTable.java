/*******************************************************************************
 * Copyright (c) 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.pde.internal.ui.nls;

import java.util.ArrayList;
import java.util.List;

/**
 * 
 * Stores the list of BundlePluginModels and ExternalPluginModels to be passed to the
 * InternationalizeWizard. This class could also used to populate the list of locales
 * to which plug-ins will be internationalized.
 * 
 * @author Team Azure
 *
 */
public class InternationalizeModelTable {
	private List fModels;
	private List fPreSelected; //Models preselected by the user

	public InternationalizeModelTable() {
		fModels = new ArrayList();
		fPreSelected = new ArrayList();
	}

	/**
	 * Adds the model to the model table. Takes into consideration the specified
	 * selection.
	 * @param model
	 * @param selected
	 */
	public void addToModelTable(Object model, boolean selected) {
		if (selected)
			fPreSelected.add(model);
		else
			fModels.add(model);
	}

	/**
	 * Adds the model to the model table.
	 * @param model
	 */
	public void addModel(Object model) {
		fModels.add(model);
	}

	/**
	 * Removes the specified model from the model table.
	 * @param model
	 */
	public void removeModel(Object model) {
		fModels.remove(model);
	}

	/**
	 * 
	 * @return the number of models in the table
	 */
	public int getModelCount() {
		return fPreSelected.size() + fModels.size();
	}

	/**
	 * Returns the list of models stored in the model table
	 * @return the array of models
	 */
	public Object[] getModels() {
		return fModels.toArray();
	}

	/**
	 * Returns the list of preselected models stored in the model table
	 * @return the array of preselected models
	 */
	public Object[] getPreSelected() {
		return fPreSelected.toArray();
	}

	/**
	 * 
	 * @return whether or not the model table contains preselected models
	 */
	public boolean hasPreSelected() {
		return fPreSelected.size() > 0;
	}

	/**
	 * 
	 * @return whether or not the list of models is empty
	 */
	public boolean isEmpty() {
		return fModels.size() == 0;
	}
}
