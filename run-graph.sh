#!/bin/bash

# ./$0 Start:Group:or:Folder:or:Subject obtainGroupSizes SkipFolderCount
# ./$0 Root true 100
# ./$0 App:Two-Factor: false  <-- graph everything in this folder, no group counts
# ./$0 App:Two-Factor:Enrolled  <-- graph everything starting with this specific group
# ./$0 subject:gettes@ufl.edu false <-- graph all groups where the subject gettes@ufl.edu is related

prefix="graph"
outputDir="output"
outputFile="$prefix.RootFile"
dotFile="$prefix.dot"

/bin/rm -f $outputDir/$prefix.pdf $outputDir/$prefix.svg $outputDir/$dotFile
echo
echo Graphing $1
echo
# need tab between varName and value
echo "Root	$1" 	> $outputDir/$outputFile
echo "grouperURL	https://localhost/grouper" >> $outputDir/$outputFile
echo "outputFile	/$outputDir/$dotFile" >> $outputDir/$outputFile
echo 'defaultNodeAttrs	fontname="Courier,monospace"; fontsize="12.0";' >> $outputDir/$outputFile
echo 'graphAttrs	fontname="Courier,monospace"; bgcolor=gray91; packmode=clust;' >> $outputDir/$outputFile
echo 'NodeAttrs	<FONT><TABLE BORDER="1" ALIGN="CENTER" CELLBORDER="0" CELLPADDING="0" CELLSPACING="0">' >> $outputDir/$outputFile
echo 'CellAttrs	ALIGN="CENTER"><FONT COLOR="black">' >> $outputDir/$outputFile
echo 'startNodeAttrs	<FONT><TABLE BORDER="1" ALIGN="CENTER" CELLBORDER="0" CELLPADDING="1" CELLSPACING="0" BGCOLOR="purple3" COLOR="blue">' >> $outputDir/$outputFile
echo 'startCellAttrs	><FONT COLOR="white">' >> $outputDir/$outputFile
echo 'loaderNodeAttrs	<FONT><TABLE BORDER="1" ALIGN="CENTER" CELLBORDER="0" CELLPADDING="1" CELLSPACING="0" ' >> $outputDir/$outputFile
echo 'LoaderNodeColor	forestgreen' >> $outputDir/$outputFile
echo 'loaderCellAttrs	><FONT COLOR="white">' >> $outputDir/$outputFile
echo 'skipFolders	etc	basis:byDepartment	LoaderJobs	Course' >> $outputDir/$outputFile

# true or false to calculate group sizes
if [ -n "$2" ]; then 
	echo "doSize	$2" 	>> $outputDir/$outputFile
fi
# number of items in a folder to skip it
if [ -n "$3" ]; then 
	echo "skipFolderCount	$3" 	>> $outputDir/$outputFile
fi

# some debugging
#/bin/cat $outputDir/$outputFile
#exit

case "$1" in
	subject:*)
		echo "Running GSH with Subject Sources all"
		docker-compose run --name Graph-Grouper --rm gsh bin/gsh.sh /app/graph-groups.groovy
		;;
	*)
		echo "Running GSH with only Subject Sources g:gsa,ldap"
		docker-compose run --name Graph-Grouper --rm gsho bin/gsh.sh /app/graph-groups.groovy
		;;
esac
rc=$?
if [ $rc -ne 0 ]; then
	echo NON-ZERO return code = $rc
fi

if [ -e "$outputDir/$prefix.pdf" ]; then
	cp -p $outputDir/$prefix.pdf $outputDir/$prefix-$1.pdf
fi
if [ -e "$outputDir/$prefix.svg" ]; then
	cp -p $outputDir/$prefix.svg $outputDir/$prefix-$1.svg
	echo 
	echo " graph in $outputDir/$prefix-$1.svg"
	echo 
fi
if [ -e "$outputDir/$dotFile" ]; then
	cp -p $outputDir/$dotFile $outputDir/$prefix-$1.dot
fi

