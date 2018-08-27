/*******************************************************************************
 * Copyright (c) 2007, 2017 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.pde.internal.ua.core.cheatsheet.simple.text;

import org.eclipse.jface.text.IDocument;
import org.eclipse.pde.core.IModel;
import org.eclipse.pde.core.IWritable;
import org.eclipse.pde.internal.core.NLResourceHelper;
import org.eclipse.pde.internal.core.text.XMLEditingModel;
import org.eclipse.pde.internal.ua.core.cheatsheet.simple.ISimpleCS;
import org.eclipse.pde.internal.ua.core.cheatsheet.simple.ISimpleCSModel;
import org.eclipse.pde.internal.ua.core.cheatsheet.simple.ISimpleCSModelFactory;
import org.xml.sax.helpers.DefaultHandler;

public class SimpleCSModel extends XMLEditingModel implements ISimpleCSModel {

	private SimpleCSDocumentHandler fHandler;

	private SimpleCSDocumentFactory fFactory;

	private ISimpleCS fSimpleCS;

	/**
	 * @param document
	 * @param isReconciling
	 */
	public SimpleCSModel(IDocument document, boolean isReconciling) {
		super(document, isReconciling);

		fHandler = null;
		fFactory = new SimpleCSDocumentFactory(this);
		fSimpleCS = null;
	}

	@Override
	protected DefaultHandler createDocumentHandler(IModel model,
			boolean reconciling) {

		if (fHandler == null) {
			fHandler = new SimpleCSDocumentHandler(this, reconciling);
		}
		return fHandler;
	}

	@Override
	protected NLResourceHelper createNLResourceHelper() {
		// Not needed
		return null;
	}

	@Override
	public ISimpleCSModelFactory getFactory() {
		return fFactory;
	}

	@Override
	public ISimpleCS getSimpleCS() {
		if (fSimpleCS == null) {
			fSimpleCS = getFactory().createSimpleCS();
		}
		// TODO: MP: TEO: LOW: Remove cast once interface method created
		((SimpleCSObject) fSimpleCS).setInTheModel(true);
		return fSimpleCS;
	}

	@Override
	protected IWritable getRoot() {
		return getSimpleCS();
	}

}
