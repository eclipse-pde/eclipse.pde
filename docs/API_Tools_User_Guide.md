PDE/API Tools/User Guide
========================

*   **User Guide**
*   [Developers](/index.php?title=PDE/API_Tools/Developers&action=edit&redlink=1 "PDE/API Tools/Developers (page does not exist)")
*   [Resources](/PDE/API_Tools/Resources "PDE/API Tools/Resources")
*   [Use Cases](/PDE/API_Tools/Use_Cases "PDE/API Tools/Use Cases")
*   [Testing](/PDE/API_Tools/Testing "PDE/API Tools/Testing")
*   [Java 7](/PDE/API_Tools/Java7 "PDE/API Tools/Java7")
*   [Java 8](/PDE/API_Tools/Java8 "PDE/API Tools/Java8")

| **API Tools** |
| :-: |
| [Website](http://www.eclipse.org/pde/pde-api-tools/) |
| [Download](http://download.eclipse.org/eclipse/downloads/) |
| **Community** |
| [Mailing List](https://dev.eclipse.org/mailman/listinfo/pde-dev) • [Forums](http://www.eclipse.org/forums/eclipse.platform) • [IRC](irc://irc.freenode.net/#eclipse) • [mattermost](https://mattermost.eclipse.org/eclipse/channels/town-square) |  |
| **Issues** |
| [Open](https://bugs.eclipse.org/bugs/buglist.cgi?query_format=advanced&bug_status=UNCONFIRMED&bug_status=NEW&bug_status=ASSIGNED&bug_status=REOPENED&product=PDE&component=API+Tools) • [Help Wanted](https://bugs.eclipse.org/bugs/buglist.cgi?query_format=advanced&bug_status=UNCONFIRMED&bug_status=NEW&bug_status=ASSIGNED&bug_status=REOPENED&product=PDE&component=API+Tools&keywords=helpwanted) • [Bug Day](https://bugs.eclipse.org/bugs/buglist.cgi?query_format=advanced&bug_status=UNCONFIRMED&bug_status=NEW&bug_status=ASSIGNED&bug_status=REOPENED&product=PDE&component=API+Tools&keywords=bugday) |
| **Contribute** |
| [Browse Source](http://dev.eclipse.org/viewcvs/index.cgi/) |

Contents
--------

*   [1 Current State](#Current-State)
*   [2 API Tooling Setup](#API-Tooling-Setup)
    *   [2.1 Define an API Baseline](#Define-an-API-Baseline)
    *   [2.2 Configure Bundles for API Tooling](#Configure-Bundles-for-API-Tooling)
*   [3 API Problems](#API-Problems)
    *   [3.1 Adjusting Problem Severities](#Adjusting-Problem-Severities)
    *   [3.2 Filtering Known Problems](#Filtering-Known-Problems)
*   [4 Ant Tasks](#Ant-Tasks)

Current State
-------------

API tooling has been integrated into the Eclipse SDK and lives in the PDE project. 
This guide shows developers how to set up and use API tooling features in daily development scenarios.

[Execution Environments User Guide](/PDE/API_Tools/Target_Environment/User_Guide "PDE/API Tools/Target Environment/User Guide")

API Tooling Setup
-----------------

API tooling provides a builder that reports API usage and binary compatibility errors in the workspace. 
You must configure bundles/projects that you want API tooling to report errors on and you must define an API baseline to compare against workspace projects (to report compatibility errors, missing @since tags, incorrect version numbers, etc.).

#### Define an API Baseline

An API baseline defines the state you want to compare your development workspace bundles against for the purposes of binary compatibility, bundle version numbers, and @since tags. 
For example, if you are developing bundles for Eclipse 3.4, you will use Eclipse 3.3 as your baseline.

API baselines are managed on the **Plug-in Development > API Baselines** preference page. Here you can create, edit, and delete API baselines. 
You can select one baseline as the 'default' - this is the baseline that the workspace will be compared against.

![Api_profiles_pref_page_2.PNG](https://raw.githubusercontent.com/eclipse-pde/eclipse.pde/master/docs/images/Api_profiles_pref_page_2.PNG)

Use the **Add Baseline...** button to open a wizard to define a baseline. 
Any Eclipse SDK can be used as a baseline simply by giving it a name and specifying the root location of the plug-ins.

Note: Currently, Eclipse builds do not contain API description metadata that assists API tooling. 
This information will be added to builds sometime during the 3.4 development cycle. 
An SDK can be used as a baseline without the metadata, although some problems may not be detected when this information is missing.
For example, if you are implementing an interface that originates from a binary required plug-in, API tooling cannot verify that the interface is allowed to implemented unless the API description metadata is present.

#### Configure Bundles for API Tooling

API tooling provides a builder that analyzes bundles and reports API errors. 
You need to configure existing projects to use the builder (this effectively adds the "API Tooling" nature to your project). 
As well, you need to describe the API of your bundle. 
Special API Javadoc tags can be placed on classes, interfaces, and methods to describe their API (@noimplement, @noinstantiate, @noextend). 
You can use content assist to insert tags with appropriate comments.

**Note**: API tooling can insert Javadoc tags into source code based on existing **component.xml** files.

Using the **PDE Tools > API Tooling Setup...** action to configure your projects.

![Api_tooling_setup.PNG](https://raw.githubusercontent.com/eclipse-pde/eclipse.pde/master/docs/images/Api_tooling_setup.PNG)


A dialog appears allowing you to select projects you want to setup for API tooling.

![Api_tooling_setup_refactoring.PNG](https://raw.githubusercontent.com/eclipse-pde/eclipse.pde/master/docs/images/Api_tooling_setup_refactoring.PNG)


**Note**: By default, **component.xml files are deleted** from your workspace after corresponding Javadoc tags are inserted into source code. 
There is a checkbox to disable this option. 
You can also preview the text changes that will be made, by clicking 'next'.

![Api_tooling_setup_preview.PNG](https://raw.githubusercontent.com/eclipse-pde/eclipse.pde/master/docs/images/Api_tooling_setup_preview.PNG)

After pressing finish, Javadoc tags will be inserted and the API Tooling nature (and builder) will be added to your projects. 
API problems will now appear in the workspace where appropriate. 
You should manually examine your source code after tags have been inserted to ensure correctness.

API Problems
------------

Once your projects have been configured for API tooling and an API baseline has been selected, API problems will be created during project builds (auto and full builds).

The tooling breaks down the types of problems reported into one of three categories:

1.  Usage Problems - restricted code is being called by unauthorized plugins
2.  Binary Incompatibilities - changes between versions are not binary compatible
3.  Version Problems - plugin versions or code versions (@since tags) not correct

#### Adjusting Problem Severities

Similar to the way you can set compiler problem severities (error vs. warning vs. ignore), API tooling provides an **API Errors/Warnings** preference page that allows you to configure API problem severities. 
You can configure the problem severities on a **per project basis** as well, by using the **API Errors/Warnings** property page for a project, available from the **Properties...** action.

![Api_errors_pref.PNG](https://raw.githubusercontent.com/eclipse-pde/eclipse.pde/master/docs/images/Api_errors_pref.PNG)

#### Filtering Known Problems

Sometimes you will have API problems that are "known problems" or are "approved" API breakages. 
API tooling has support to allow you to filter these problems from your workspace.

You can add a filter for a specific api problem using the Eclipse 'quickfix' mechanism on any api problem marker.

[Examples of API problems in the problems view](/index.php?title=Special:Upload&wpDestFile=Api_tooling_problems.PNG "File:Api tooling problems.PNG")

  
Once a filter has been added you can edit / remove it from the 'Api Problem Filters' project property page.

![Api_tooling_problem_filters_page.PNG](https://raw.githubusercontent.com/eclipse-pde/eclipse.pde/master/docs/images/Api_tooling_problem_filters_page.PNG)

Ant Tasks
---------

API Tools provides a number of ant tasks to integrate the tooling into your build process. 
For more details see [Ant Tasks](/PDE/API_Tools/Tasks "PDE/API Tools/Tasks").

