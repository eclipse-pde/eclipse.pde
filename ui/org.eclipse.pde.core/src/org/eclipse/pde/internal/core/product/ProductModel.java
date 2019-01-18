/*******************************************************************************
 *  Copyright (c) 2005, 2008 IBM Corporation and others.
 *
 *  This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License 2.0
 *  which accompanies this distribution, and is available at
 *  https://www.eclipse.org/legal/epl-2.0/
 *
 *  SPDX-License-Identifier: EPL-2.0
 *
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.core.product;

import java.io.IOException;
import java.io.InputStream;
import javax.xml.parsers.SAXParser;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.pde.core.IModelChangedEvent;
import org.eclipse.pde.core.ModelChangedEvent;
import org.eclipse.pde.internal.core.AbstractModel;
import org.eclipse.pde.internal.core.PDECore;
import org.eclipse.pde.internal.core.XMLDefaultHandler;
import org.eclipse.pde.internal.core.iproduct.IProduct;
import org.eclipse.pde.internal.core.iproduct.IProductModel;
import org.eclipse.pde.internal.core.iproduct.IProductModelFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

public class ProductModel extends AbstractModel implements IProductModel {

	private static final long serialVersionUID = 1L;

	private IProductModelFactory fFactory;
	private IProduct fProduct;

	@Override
	protected void updateTimeStamp() {
	}

	@Override
	public IProduct getProduct() {
		if (fProduct == null) {
			fProduct = getFactory().createProduct();
		}
		return fProduct;
	}

	@Override
	public IProductModelFactory getFactory() {
		if (fFactory == null) {
			fFactory = new ProductModelFactory(this);
		}
		return fFactory;
	}

	@Override
	public String getInstallLocation() {
		return null;
	}

	@Override
	public boolean isInSync() {
		return true;
	}

	@Override
	public void load() throws CoreException {
	}

	@Override
	public void load(InputStream stream, boolean outOfSync) throws CoreException {
		try {
			SAXParser parser = getSaxParser();
			XMLDefaultHandler handler = new XMLDefaultHandler();
			parser.parse(stream, handler);
			if (handler.isPrepared()) {
				processDocument(handler.getDocument());
				setLoaded(true);
			}
		} catch (Exception e) {
			PDECore.logException(e);
		} finally {
			try {
				if (stream != null) {
					stream.close();
				}
			} catch (IOException e) {
			}
		}
	}

	@Override
	public void reload(InputStream source, boolean outOfSync) throws CoreException {
		load(source, outOfSync);
		fireModelChanged(new ModelChangedEvent(this, IModelChangedEvent.WORLD_CHANGED, new Object[] {fProduct}, null));
	}

	@Override
	public boolean isEditable() {
		return false;
	}

	private void processDocument(Document doc) {
		Node rootNode = doc.getDocumentElement();
		if (fProduct == null) {
			fProduct = getFactory().createProduct();
		} else {
			fProduct.reset();
		}
		fProduct.parse(rootNode);
	}
}
