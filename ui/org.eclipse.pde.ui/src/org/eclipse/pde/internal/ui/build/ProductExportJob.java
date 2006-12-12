/*******************************************************************************
 * Copyright (c) 2005, 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.build;

import java.io.File;

import org.eclipse.pde.internal.core.exports.FeatureExportInfo;
import org.eclipse.pde.internal.core.exports.FeatureExportOperation;
import org.eclipse.pde.internal.core.exports.ProductExportOperation;
import org.eclipse.pde.internal.core.iproduct.IProduct;
import org.eclipse.pde.internal.core.iproduct.IProductModel;

public class ProductExportJob extends FeatureExportJob {
	
	private IProduct fProduct;
	private String fRoot;
	private File fJRELocation;
	
	public ProductExportJob(FeatureExportInfo info, IProductModel model, String productRoot, File jreLocation) {
		super(info);
		fProduct = model.getProduct();
		fRoot = productRoot;
		fJRELocation = jreLocation;
	}
	
	protected FeatureExportOperation createOperation() {
		return new ProductExportOperation(fInfo, fProduct, fRoot, fJRELocation);
	}

}
