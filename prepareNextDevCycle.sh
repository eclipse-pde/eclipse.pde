#!/bin/bash -xe

#*******************************************************************************
# Copyright (c) 2025, 2025 Hannes Wellmann and others.
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
# and applies the changes required individually for SWT.
# The calling pipeline also defines environment variables usable in this script.

# Update the link to N&N entries, which also implies the otherwise explicitly required qualifier update
whatsNewFile='org.eclipse.pde.doc.user/whatsNew/pde_whatsnew.html'
sed --expression "s|Eclipse ${PREVIOUS_RELEASE_VERSION}|Eclipse ${NEXT_RELEASE_VERSION}|" -i "${whatsNewFile}"
sed --expression "s|news/${PREVIOUS_RELEASE_VERSION}/pde.html|news/${NEXT_RELEASE_VERSION}/pde.html|" -i "${whatsNewFile}"

git commit --all --message "Reference New and Noteworthy for ${NEXT_RELEASE_VERSION} in PDE documentation"
