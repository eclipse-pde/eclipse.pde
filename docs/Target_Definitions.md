PDE/Target Definitions
======================

Contents
--------

*   [1 Introduction](#Introduction)
*   [2 The Basics](#The-Basics)
*   [3 Examples](#Examples)
*   [4 Known Issues](#Known-Issues)
    *   [4.1 Redownloading of Bundles](#Redownloading-of-Bundles)
*   [5 Links](#Links)

Introduction
------------

The *Target Platform* is a critical part of developing using PDE. 
It defines what your workspace will be built and launched against.
PDE supports p2 targets which allows you to create a target that can grab bundles from remote update sites and repositories and add them to your target.
With the m2e extension you can also use regular Maven dependencies in your target platform.

Developing a target platform can become very complex. 
This page is used to collect known issues so that bug reports, workarounds and proper fixes can be developed.

The Basics
----------

The target platform refers to the plug-ins which your workspace will be built and run against. It describes the platform that you are developing for. When developing with PDE, the target platform is used to:

Whereas the **target platform** refers to your _currently_ active bundles, a **Target Definition** is a way of determining the plug-ins to _add to the state_. You can have multiple target definitions, but only one definition can be selected as the target platform.

The target platform and your target definitions are managed on the **Target Platform Preference Page**. This page lists all target definitions that PDE has access to and displays which definition is being used as your current target platform. Target definitions can be created and edited here using the **Target Definition Content Wizard**. To make for easier sharing among a development team, targets can also be created and edited in the workspace as XML files with the extension ".target". These files can be edited using the target definition editor and can be created using the **New Target Definition Wizard**.[\[1\]](#cite-note-1)

The [Eclipse Help Documentation](https://www.eclipse.org/documentation/) provides more detailed explanation on how to use the editors, wizards and preference pages in PDE.

Examples
--------

This section will contain examples of how different target definitions can be created. Some examples we should include are:

*   Default definition for Eclipse platform developers
*   Downloading a premade target definition file
*   Pointing at an install or a folder (the old way)
*   Pointing to a simple site, where using default include options works
*   Complex site-based target, where using default include options causes error

Going forward we will add information collected here to improve the official user help doc.

Known Issues
------------

If you have an issue, feel free to add it here. Make sure to link to the bug report and provide any workarounds available.

### Redownloading of Bundles

Each Eclipse [workspace](/Workspace "Workspace") has its own **cache** (aka _bundle pool_) of the target bundles. However on every new workspace the target bundles will be downloaded again.

Future [p2](/P2 "P2") version will consider additional artifact repositories. Once that it is enabled, the bundle pool can be added for the current running Eclipse IDE as well as the PDE target bundle pool for all known “recently used” workspaces. The net effect is that you pick features from one metadata repo and the content, if already local, is just copied. No downloading. Of course, new content is still downloaded as needed.

We are looking at exposing some preferences to allow additional artifact repositories to be listed. Makes sense, just need to put a UI on it.

Sidenote: People have suggested that PDE manage just one artifact repository/bundle pool for all target definitions for all workspaces. This would save disk space for sure but introduces some additional complexity in managing concurrent repo access as well as garbage collection. It would be great but for now, this is the next best thing.

Can you think of additional improvements? Please post your thoughts on [\[1\]](http://mcaffer.com/2010/12/populate-target-definitions-faster/).

Links
-----

Links to specific project's target definition help, links to how to generate metadata, and other useful links.

1.  [↑](#cite-ref-1) [https://help.eclipse.org/latest/index.jsp?topic=/org.eclipse.pde.doc.user/concepts/target.htm](https://help.eclipse.org/latest/index.jsp?topic=/org.eclipse.pde.doc.user/concepts/target.htm)

