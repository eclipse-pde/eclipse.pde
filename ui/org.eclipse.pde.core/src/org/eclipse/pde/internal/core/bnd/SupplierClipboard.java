/*******************************************************************************
 * Copyright (c) 2024 Christoph Läubrich and others.
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
package org.eclipse.pde.internal.core.bnd;

import java.util.Optional;
import java.util.function.Supplier;

import aQute.bnd.service.clipboard.Clipboard;

public class SupplierClipboard implements Clipboard {

	private final Supplier<Clipboard> supplier;

	public SupplierClipboard(Supplier<Clipboard> supplier) {
		this.supplier = supplier;
	}

	@Override
	public <T> boolean copy(T content) {
		Clipboard clipboard = supplier.get();
		if (clipboard != null) {
			return clipboard.copy(content);
		}
		return false;
	}

	@Override
	public <T> Optional<T> paste(Class<T> type) {
		return Optional.ofNullable(supplier.get()).flatMap(clipboard -> clipboard.paste(type));
	}

}
