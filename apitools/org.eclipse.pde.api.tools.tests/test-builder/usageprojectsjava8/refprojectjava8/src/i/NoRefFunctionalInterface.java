/*******************************************************************************
 * Copyright (c) 2014 IBM Corporation and others.
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
package i;

/**
 * Copy of the functional interface Supplier, but with a reference api restriction on the method
 */
@FunctionalInterface
public interface NoRefFunctionalInterface<T> {

    /**
     * Gets a result.
     *
     * @return a result
     * @noreference This method is not intended to be referenced by clients.
     */
    T get();
}