# grouper-graph

Original grouper graphing code in groovy

## Overview
Provided a starting point in Grouper (a group, a folder or a subject) then graph all
	objects involved as related to the starting point.  Yes, a starting can be Root.
> This capability is particularly useful if you want to graph all the components of groups
	used for a specific application.  As a grouper admin it helps you to determine if
	built everything properly understanding how everything relates.  It also will help
	an application admin understand what Grouper is doing for their application.

run-graph.sh passes parameters into the groovy script through modified gsh container.
> The modified gsh container has graphviz built-in.  Also included InCommon certs - not really needed.

sample docker-compose.yml provided so volumes and other mappings should be obvious

# How-To
Various ways to invoke:

* run-graph.sh Start:Group:or:Folder:or:Subject obtainGroupSizes SkipFolderCount
* run-graph.sh Root true 100
* run-graph.sh App:Two-Factor: false  <-- graph everything in this folder, no group counts
* run-graph.sh App:Two-Factor:Enrolled  <-- graph everything starting with this specific group
* run-graph.sh subject:gettes@ufl.edu false <-- graph all groups where the subject gettes@ufl.edu is related

inspecting the run-graph.sh script and the groovy script you will see lots of options
to control graphviz capability without having to modify the base code.

I learned groovy to develop this capability.  My apologies if my style or implementations are sub-optimal.

Have fun truly visualizing Grouper!

## LICENSE

Copyright 2018 Michael R Gettes

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

  http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.

