#!/bin/bash -xeu

#*******************************************************************************
# Copyright (c) 2025, 2026 Hannes Wellmann and others.
#
# This program and the accompanying materials
# are made available under the terms of the Eclipse Public License 2.0
# which accompanies this distribution, and is available at
# https://www.eclipse.org/legal/epl-2.0/
#
# SPDX-License-Identifier: EPL-2.0
#
# Contributors:
#     Hannes Wellmann - initial API and implementation
#*******************************************************************************

# This script is called by the pipeline for preparing the next development cycle (this file's name is crucial!)
# and applies the changes required individually for PDE.
# The calling pipeline also defines environment variables usable in this script.

# Update the link to N&N entries, which also implies the otherwise explicitly required qualifier update
sed --in-place 'org.eclipse.pde.doc.user/whatsNew/pde_whatsnew.html' \
	--expression "s|${PREVIOUS_RELEASE_VERSION//./\\.}|${NEXT_RELEASE_VERSION}|g"


git commit --all --message "Reference New and Noteworthy for ${NEXT_RELEASE_VERSION} in PDE documentation"
