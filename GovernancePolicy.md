<!--

    DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.

    Copyright (c) 2017 Oracle and/or its affiliates. All rights reserved.

    The contents of this file are subject to the terms of either the GNU
    General Public License Version 2 only ("GPL") or the Common Development
    and Distribution License("CDDL") (collectively, the "License").  You
    may not use this file except in compliance with the License.  You can
    obtain a copy of the License at
    https://oss.oracle.com/licenses/CDDL+GPL-1.1
    or LICENSE.txt.  See the License for the specific
    language governing permissions and limitations under the License.

    When distributing the software, include this License Header Notice in each
    file and include the License file at LICENSE.txt.

    GPL Classpath Exception:
    Oracle designates this particular file as subject to the "Classpath"
    exception as provided by Oracle in the GPL Version 2 section of the License
    file that accompanied this code.

    Modifications:
    If applicable, add the following below the License Header, with the fields
    enclosed by brackets [] replaced by your own identifying information:
    "Portions Copyright [year] [name of copyright owner]"

    Contributor(s):
    If you wish your version of this file to be governed by only the CDDL or
    only the GPL Version 2, indicate your decision by adding "[Contributor]
    elects to include this software in this distribution under the [CDDL or GPL
    Version 2] license."  If you don't indicate a single choice of license, a
    recipient has the option to distribute your version of this file under
    either the CDDL, the GPL Version 2 or to extend the choice of license to
    its licensees as provided above.  However, if you add GPL Version 2 code
    and therefore, elected the GPL Version 2 license, then the option applies
    only if the new code is made subject to such option by the copyright
    holder.

-->

The projects within the GlassFish community have a very flat, lightweight governance structure.
Decisions are made in public discussion on public mailing lists. There are few formal roles--an 
individual's word carries weight in accordance with their contribution to the project. 
Decisions are made by consensus, rather than voting--most decisions are of interest only 
to members of the community who will be affected by it. The combination of public mailing lists 
and consensus ensures that any person who could be affected by a decision both finds out about it,
and has a voice in the discussion.

## Roles and Responsibilities


There are quite a few ways to participate on projects within the GlassFish community, 
and not all of them involve contributing source code to a project! Simply using the software, 
participating on mailing lists, or filing bug reports or enhancement requests is an incredibly valuable 
form of participation.

If one were to break down the forms of participation in GlassFish projects into a set of roles, 
the result would look something like this: Users, Contributors, Committers, Maintainers, and Project Lead.

### Users

Users are the people who use the software. Users are using the software, reporting bugs, 
making feature requests and suggestions. This is by far the most important category of people. 
Without users, there is no reason for the project.

How to become one: Download the software and use it to build an application.

### Contributors

Contributors are individuals who contribute to a GlassFish project, but do not have write access 
to the source tree. Contributions can be in the form of source code patches, new code, or bug reports, 
but could also include web site content like articles, FAQs, or screenshots.

A contributor who has sent in solid, useful source code patches on a project can be elevated 
to committer status by the maintainer.

Integration of a Contributors' submissions is at the discretion of the project maintainer, but this is 
an iterative, communicative process. Note that for code to be integrated, a completed 
Oracle Contribution Agreement (OCA) is required from each contributor. See the [OCA policy](#oca-policy) 
for info.

How to become one: Contribute in any of the ways described above: either code, examples, 
web site updates, tests, bugs, and patches. If you're interested in becoming a Committer to the source base, 
get the sources to the project, make an improvement or fix a bug, and send that code 
to the developers mailing list or attach it to the bug report in the project issue tracking system.

### Committers

Committers have write access to the source tree, either for the individual modules they are working on, 
or in some cases global write permissions everywhere in the source code management system.

A committer must complete and send in a OCA to commit code. See the [OCA policy](#oca-policy) for info.

Rules for how to commit, once you have commit access, will vary by project and module. Be sure to ask 
before you start making changes!

How to become one: Submit some patches via email, and ask the maintainer of the code you've patched 
for commit access. The maintainer will seek consensus before granting Committer access, but their 
decisions are final.

### Maintainers

Each module has one maintainer, who has check-in permissions (either for that module or globally), 
and "manages" a group of Committers. They are responsible for merging contributors' patches, bug fixes, 
and new code from the development branch of the source tree into the stable branch. 
Maintainers are responsible for making sure that these contributions do not break the build.

The Maintainer is also responsible for checking that everyone who contributes code has submitted a OCA. 
See the [OCA policy](#oca-policy) for info.

A Maintainer is responsible for their module, and for granting check-in privileges to contributors. 
They also act as the "police force" of the module, helping to ensure quality across the build.

How to become one: Start a module (you need to have written some working code on your project to do this, 
you'll also need to talk to the Project Lead). Have responsibility for that module handed over to you 
from the current Maintainer. Take over an abandoned project--sometimes someone starts something, 
but for one reason or another can't continue to work on it. If it's interesting to you, volunteer!

### Project Lead

Each project in the GlassFish community has an overall Project Lead. The Project Leads are currently appointed 
by Oracle. They are responsible for managing the entire project, helping to create policies by consensus 
that ensure global quality.

## OCA Policy

The first step to contributing code or submitting a patch is to sign and return a signed copy of the 
[Contributor Agreement][oca-agreement]. Please print this form out, fill in all the necessary detail, 
scan it in, and return it via e-mail : `oracle-ca_us [at] oracle [dot] com`. 

The [main OCA page][oca-main] has more information and the list of current signatories. You may also want to read 
the [Contributor agreement FAQ][oca-faq].

[oca-agreement]: http://oss.oracle.com/oca.pdf
[oca-main]: http://oracle.com/technetwork/goto/oca
[oca-faq]: http://oss.oracle.com/oca-faq.pdf
