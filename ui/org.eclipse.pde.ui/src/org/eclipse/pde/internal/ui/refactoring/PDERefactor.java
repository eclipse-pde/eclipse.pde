/*******************************************************************************
 *  Copyright (c) 2007, 2015 IBM Corporation and others.
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
package org.eclipse.pde.internal.ui.refactoring;

import org.eclipse.ltk.core.refactoring.participants.ProcessorBasedRefactoring;
import org.eclipse.ltk.core.refactoring.participants.RefactoringProcessor;

public class PDERefactor extends ProcessorBasedRefactoring {

	RefactoringProcessor fProcessor;

	public PDERefactor(RefactoringProcessor processor) {
		super(processor);
		fProcessor = processor;
	}

	@Override
	public RefactoringProcessor getProcessor() {
		return fProcessor;
	}

}
