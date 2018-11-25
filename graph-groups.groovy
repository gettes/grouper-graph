:set verbosity quiet

/*	graph-Grouper
	Michael R Gettes <gettes@ufl.edu>, University of Florida, August 2018
*/

import java.text.SimpleDateFormat 
import java.util.Date

def sdf = new SimpleDateFormat("yyyy-MM-dd' 'HH:mm")
def RunTimestamp = sdf.format(new Date())

def String outputDir = "/output"
def skipFolders = [ etc:0 ]
def int skipFolderCount = 15
def boolean doBackTrack = true

// all of the following can be passed over in the RootFile
def String grouperURL = 'https://localhost'
def Boolean doSize = true	// calculate group sizes
def String Root = "Root"
def String outputDotFile = "${outputDir}/graph.dot"
def String outputSvgFile = "${outputDir}/graph.svg"
def String outputPdfFile = "${outputDir}/graph.pdf"
def String graphAttrs = 'bgcolor=gray91; packmode=clust;'
def String defaultNodeAttrs = 'fontname="Courier,monospace";'
//def String nodeAttrs = '<FONT FACE="Courier" POINT-SIZE="12"><TABLE BORDER="1" ALIGN="CENTER" CELLBORDER="0" CELLPADDING="1" CELLSPACING="0">' 
def String nodeAttrs = '<FONT><TABLE BORDER="1" ALIGN="CENTER" CELLBORDER="0" CELLPADDING="1" CELLSPACING="0">' 
def String cellAttrs = '><FONT>'	// always contains FONT - add attrs as BGCOLOR="x"<FONT COLOR="y">
//def String startNodeAttrs = '<FONT FACE="Courier" POINT-SIZE="12"><TABLE BORDER="1" ALIGN="CENTER" CELLBORDER="0" CELLPADDING="1" CELLSPACING="0" BGCOLOR="blue" COLOR="blue">'
def String startNodeAttrs = '<FONT><TABLE BORDER="1" ALIGN="CENTER" CELLBORDER="0" CELLPADDING="1" CELLSPACING="0" BGCOLOR="blue" COLOR="blue">'
def String startCellAttrs = '><FONT COLOR="white">'		// this precedes the text - always close with >
//def String loaderNodeAttrs = '<FONT FACE="Courier" POINT-SIZE="12"><TABLE BORDER="1" ALIGN="CENTER" CELLBORDER="0" CELLPADDING="1" CELLSPACING="0" '
def String loaderNodeAttrs = '<FONT><TABLE BORDER="1" ALIGN="CENTER" CELLBORDER="0" CELLPADDING="1" CELLSPACING="0" '
def String LoaderNodeColor = 'forestgreen'
def String loaderCellAttrs = '><FONT COLOR="white">'		// this precedes the text - always close with >
def String folderNodeColor = 'powderblue'
def String skipFolderNodeColor = 'sandybrown'
def String TotalAttrs = '[ pos="1,5!" shape="rectangle" style="rounded,filled"; fillcolor="navy" label=<<TABLE BORDER="0" ALIGN="LEFT" CELLBORDER="0" CELLPADDING="5" CELLSPACING="0" WIDTH="155"><TR><TD BALIGN="LEFT"><FONT FACE="Courier" POINT-SIZE="16" COLOR="white">'
def int debugRecursion = 0
def int debugDot = 0

def FileData = [:]
def String[] lines 
def key, value, line

File RootFile = new File("$outputDir/graph.RootFile")
lines = RootFile.readLines()
for (line in lines) {
	words = line.split("\t"); key=words[0]; value=words.drop(1)
	if (value.size() == 1) value = value[0]
  	FileData[key]=value
}
if (FileData['Root']?.length() > 0) Root = FileData['Root']
if (FileData['debugRecursion']?.length() > 0) debugRecursion = FileData['debugRecursion'] as Integer
if (FileData['debugDot']?.length() > 0) debugDot = FileData['debugDot'] as Integer
if (FileData['doSize']?.toLowerCase().equals("true")) doSize = true
if (FileData['grouperURL']?.length() > 0) grouperURL = FileData['grouperURL']
if (FileData['outputDotFile']?.length() > 0) outputDotFile = FileData['outputDotFile']
if (FileData['graphAttrs']?.length() > 0) graphAttrs = FileData['graphAttrs']
if (FileData['nodeAttrs']?.length() > 0) nodeAttrs = FileData['nodeAttrs']
if (FileData['defaultNodeAttrs']?.length() > 0) defaultNodeAttrs = FileData['defaultNodeAttrs']
if (FileData['cellAttrs']?.length() > 0) cellAttrs = FileData['cellAttrs']
if (FileData['startNodeAttrs']?.length() > 0) startNodeAttrs = FileData['startNodeAttrs']
if (FileData['startCellAttrs']?.length() > 0) startCellAttrs = FileData['startCellAttrs']
if (FileData['loaderNodeAttrs']?.length() > 0) loaderNodeAttrs = FileData['loaderNodeAttrs']
if (FileData['loaderCellAttrs']?.length() > 0) loaderCellAttrs = FileData['loaderCellAttrs']
if (FileData['debugRecursion']?.length() > 0) debugRecursion = FileData['debugRecursion']
if (FileData['TotalAttrs']?.length() > 0) TotalAttrs = FileData['TotalAttrs']
if (FileData['LoaderNodeColor']?.length() > 0) LoaderNodeColor = FileData['LoaderNodeColor']
if (FileData['folderNodeColor']?.length() > 0) folderNodeColor = FileData['folderNodeColor']
if (FileData['skipFolderNodeColor']?.length() > 0) skipFolderNodeColor = FileData['skipFolderNodeColor']
if (FileData['outputPdfFile']?.length() > 0) outputPdfFile = FileData['outputPdfFile']
if (FileData['skipFolderCount']?.length() > 0) skipFolderCount = FileData['skipFolderCount'] as Integer
if (FileData['skipFolders']?.size() > 0) FileData['skipFolders'].each { k -> skipFolders[k] = 0 }

def String cellStart0 = '<TR><TD ALIGN="CENTER" BORDER="1" WIDTH="'
def String cellStart1 = '<TR><TD '
def String cellEnd = '</FONT></TD></TR>'
def String labelEnd = '</TABLE></FONT>>'
def String labelHREF
def String labelHREFgroup = grouperURL+ '/grouperUi/app/UiV2Main.index?operation=UiV2Group.viewGroup&amp;groupId='
def String labelHREFstem = grouperURL+ '/grouperUi/app/UiV2Main.index?operation=UiV2Stem.viewStem&amp;stemId='

println """\nCreating graph for $Root at $RunTimestamp
	Grouper URL = $grouperURL
	skipFolders = $skipFolders
	Group Sizes = $doSize
	outputDotFile = $outputDotFile ...\n"""

def nodeTypeMap = [:]
def nodesMap = [:]
def descriptionMap = [:]
def nodesCommentMap = [:]
def namesMap = [:]
def countMap = [:]
def dashMap = [:]
def seenMap = [:]
def linkSeenMap = [:]
def loaderGroups = [:]
def loaderJobs = [:]
def NotLoaderJobGroup = [:]
def pspToMap = [:]
def pspNotToMap = [:]
def boolean loaderNodeNow = false
def int indent = 0
def boolean overRideNode = false

def nodeLabel = { stem, g, compositeType, Ltext, Lnode, Rtext, Rnode, colorSpec="", data="" ->
	def node = g.getUuid(); def name = g.getName()
	def String Title = name
	if (descriptionMap[ node ]?.length() > 0) Title += " - " + descriptionMap[ node ]
	if ( ! overRideNode && seenMap.containsKey( node )) return true
	def int len = name.length(); def String nA = nodeAttrs; def String cA = cellAttrs
	s = '{ "'+node+'" '
	if (colorSpec.length() > 0) colorSpec = "color=$colorSpec "
	if ( node == parentRoot ) { nA = startNodeAttrs; cA = startCellAttrs }
	if ( loaderNodeNow ) { nA = loaderNodeAttrs; cA = loaderCellAttrs; nA += ' BGCOLOR="'+LoaderNodeColor+'" COLOR="'+LoaderNodeColor+'">' }
	if ( stem.equals("stem")) labelHREF = labelHREFstem else labelHREF = labelHREFgroup
	if ( stem.equals("stem")) { colorSpec = "shape=folder style=filled $colorSpec" }
	if (Title != null) Title = Title.replace("&","&amp;")
	def nodeLink = 'TITLE="' + Title + '" HREF="' + labelHREF + node + '" '
	s += '['+colorSpec+'label=<' + nA + cellStart0 + ((len*6)+(len*2)) + '" ' + nodeLink + cA + name + '<BR/>'+ cellEnd
	Title = nodesMap[ Lnode ]; if (descriptionMap[ Lnode ]?.length() > 0) Title += " - " + descriptionMap[ Lnode ]
	if (Title != null) Title = Title.replace("&","&amp;")
	if ( Ltext.length() > 0 ) s += cellStart1 + 'TITLE="' + Title + '" HREF="'+ labelHREF + Lnode + '" ' + cA + Ltext + cellEnd
	if ( compositeType.length() > 0 ) s += cellStart1 + nodeLink + cA + compositeType + cellEnd
	Title = nodesMap[ Rnode ]; if (descriptionMap[ Rnode ]?.length() > 0) Title += " - " + descriptionMap[ Rnode ]
	if (Title != null) Title = Title.replace("&","&amp;")
	if ( Rtext.length() > 0 ) s += cellStart1 + 'TITLE="' + Title + '" HREF="'+ labelHREF + Rnode + '" ' + cA + Rtext + cellEnd
	pspng( g )
	if ( pspToMap[ g.getUuid() ] != "" ) data += 'psp To: &nbsp;'+pspToMap[g.getUuid()].join('<BR ALIGN="LEFT"/>'+'&nbsp;'.multiply(9))+'<BR ALIGN="LEFT"/>'
	if ( pspNotToMap[ g.getUuid() ] != "" ) data += 'psp Not: '+pspNotToMap[g.getUuid()].join('<BR ALIGN="LEFT"/>'+'&nbsp;'.multiply(9))+'<BR ALIGN="LEFT"/>'
	if (  data != "" ) {
		s += cellStart1 + nodeLink + 'BORDER="1"' + cA
		for ( def String datum in data ) s += datum
		s += cellEnd
	}
	s += labelEnd + '] ; }' + "\n"
	graph.append s
	if (debugDot) print ( 'NODE: '+ s )
	seenMap[ node ] = true
	if (g.getName().indexOf(':') > -1) {
		folder = g.getName().tokenize(':')[0..-2].join(':') 
		if ( skipFolders.containsKey( folder ) && skipFolders[ folder ] > 0 ) skipFolders[ folder ] --
	}
}

def linkGroup = { child, parentId, dashed, colorSpec="" ->
	if ( parentId == parentRoot )  return true
	mapit( child )
	linkLoader( child )
	def String dashkey = child.getUuid()+"-"+parentId
	if ( dashed ) dashMap[ dashkey ] = 'style=dashed;' 
		else dashMap[ dashkey ] =  'style=solid;'
	def String thisEdge = child.getUuid()+' '+parentId
	if ( ! linkSeenMap.containsKey( thisEdge ) ) {
		def String labelHREF, Title, HeadLink, EdgeLink
		if (colorSpec.length() > 0) colorSpec = "color=$colorSpec; "
		if ( nodeTypeMap[ child.getUuid() ] == 's' ) labelHREF = labelHREFstem else labelHREF = labelHREFgroup
		Title = nodesMap[ child.getUuid() ]; if (descriptionMap[ child.getUuid() ]?.length() > 0) Title += " - " + descriptionMap[ child.getUuid() ]
		HeadLink = 'headtooltip="' + Title + '" headURL="' + labelHREF + child.getUuid() + '";'
		if ( nodeTypeMap[ parentId ] == 's' ) labelHREF = labelHREFstem else labelHREF = labelHREFgroup
		Title = nodesMap[ parentId ]; if (descriptionMap[ parentId ]?.length() > 0) Title += " - " + descriptionMap[ parentId ]
		EdgeLink = 'edgetooltip="' + Title + '" edgeURL="' + labelHREF + parentId        + '";'
		s = " \"${child.getUuid()}\" -> \"$parentId\" [ arrowtail=dot; dir=both; ${colorSpec}${dashMap[dashkey]} $EdgeLink $HeadLink ] ; " +
			"# ${nodesMap[ child.getUuid() ]} -> ${nodesMap[ parentId ]}\n"
		graph.append s
		if (debugDot) print( 'EDGE: '+ s )
	}
	linkSeenMap[ thisEdge ] = true
}

def backTrack = { prevObj, colorSpec="" ->
	if (! doBackTrack) return
	def String previous = prevObj.getName()
	while ( previous.length() > 0 ) {
		if (previous.indexOf(':') > -1)
			previous = previous.tokenize(':')[0..-2].join(':') // remove last token to step backwords in the name
		def LocalStem = StemFinder.findByName( session, previous )
		mapit( LocalStem )
		nodeLabel( 'stem', LocalStem, '', '', '', '', '', colorSpec=folderNodeColor, data="" )
		nodeTypeMap[ LocalStem.getUuid() ] = 's'
		if ( ! linkSeenMap.containsKey( prevObj.getUuid() + '-' + LocalStem.getUuid() ) ) 
			linkGroup( LocalStem, prevObj.getUuid(), false, colorSpec="" )
		if (previous.indexOf(':') == -1) previous = ""
		prevObj = LocalStem
	}
}

def pspng = { g ->
	def v
	if ( ! pspToMap.containsKey( g.getUuid() ) ) {
		v = g.getAttributeValueDelegate().retrieveValuesString("etc:pspng:provision_to")
		if (v == null) v = ""; pspToMap[ g.getUuid() ] = v
	}
	if ( ! pspNotToMap.containsKey( g.getUuid() ) ) {
		v = g.getAttributeValueDelegate().retrieveValuesString("etc:pspng:do_not_provision_to")
		if (v == null) v = ""; pspNotToMap[ g.getUuid() ] = v
	}
}

def linkLoader = { loaderGroup ->
	if ( NotLoaderJobGroup[ loaderGroup ] ) return
	def loaderJob = null
	def loaderNode = null
	def myAttributeName = "etc:attribute:loaderMetadata:loaderMetadata"
	def myAttributeName2 = "etc:attribute:loaderMetadata:grouperLoaderMetadataGroupId"
        try {   // Many thanks Carey Black at OSU for helping get the UUID of the loaderjob creating this group - i couldn't figure out how to get the attribute
		def myAttributeDef = AttributeDefNameFinder.findByName( myAttributeName, true );			// find the attribute Def for myAttributeName
		def myAttributeDef2 = AttributeDefNameFinder.findByName( myAttributeName2, true );			// find the attribute Def for myAttributeName2
		def attrAssign = loaderGroup.getAttributeDelegate().retrieveAssignments( myAttributeDef );		// get all assignments of myAttributeDef to group
		def metaAssigned = attrAssign.getAt(0).getAttributeDelegate().retrieveAssignments(myAttributeDef2)
		loaderNode = metaAssigned.getAt(0).getValueDelegate().retrieveValue().getValueString()
		if (loaderNode != null) loaderJob = GroupFinder.findByUuid( session, loaderNode )
		else {
			NotLoaderJobGroup[ loaderGroup ] = true
			return
		}
        } catch (Exception e) {
		NotLoaderJobGroup[ loaderGroup ] = true
		return
        }
	if ( linkSeenMap.containsKey( loaderJob.getUuid() + '-' + loaderGroup.getUuid() ) ) return
	loaderGroups[ loaderGroup ] = true
	mapit( loaderJob )
	getGroupSize( loaderJob )
	loaderNodeNow = true; overRideNode = true
	nodeLabel( 'group', loaderJob, '', '', '', '', '', colorSpec=LoaderNodeColor, data=countMap[ loaderJob.getUuid() ] )
	loaderNodeNow = false; overRideNode = false
	nodeTypeMap[ loaderJob.getUuid() ] = 'g'
	linkGroup( loaderJob, loaderGroup.getUuid(), true, colorSpec=LoaderNodeColor )
	loaderJobs[ loaderJob ] = true
}

def mapit = { g ->
	nodesMap[ g.getUuid() ] = g.getName()
	namesMap[ g.getName() ] = g.getUuid()
	descriptionMap [ g.getUuid() ] = g.getDescription()
}

def getGroupSize = { g ->
	if ( countMap.containsKey( g.getUuid()) ) return
	if ( doSize ) countMap[ g.getUuid() ] = g.getMembers().size()
		else countMap[ g.getUuid() ] = 0
}

def resolveUnionGroup = { def indent, def g ->
	for ( def member in g.getMembers() ) {
		if ( member.getSubjectType().toString().equals("group") ) {
			def Mchild = GroupFinder.findByName( session, member.getName() )
			if ( ! linkSeenMap.containsKey( g.getUuid() + '-' + Mchild.getUuid() ) ) {
				if (! graphBySubject || Mchild.hasMember( subject )) {
					getGroupSize( Mchild )
					if (debugRecursion) for (i = 0; i < indent; i++) print(".")
					print("UNION: "+g.getName()+" <- "+Mchild.name+" "+countMap[ Mchild.getUuid() ]+"\n")
					nodeTypeMap[ Mchild.getUuid() ] = 'g'
					linkGroup( Mchild, g.getUuid(), true )
					assert decomposeGroup( indent, g, Mchild ) == Mchild
				}
			}
		}
	}
}

def decomposeGroup = { indent, Dparent, Dchild ->
	if ( seenMap.containsKey( Dchild.getUuid() ) ) return Dchild
	indent++;
	if (debugRecursion) { for (i = 0; i < indent; i++) print("."); print ("DE: "+ Dparent.getName() + " <- "+Dchild.getName() +"\n") }
	mapit( Dchild )
	getGroupSize( Dchild )
	linkLoader( Dchild )
	backTrack( Dchild )
	nodeTypeMap[ Dchild.getUuid() ] = 'g'
	if ( Dchild.hasComposite() ) {
		def composite = Dchild.getComposite(true)
		if (debugRecursion) for (i = 0; i < indent; i++) print(".")
		print ( composite.getType().getName().toUpperCase()+": "+Dchild.getName()+ " "+ countMap[ Dchild.getUuid() ] + " <- " 
			+ composite.getLeftGroup().getName() +" / "+composite.getRightGroup().getName()+"\n" )
		def gR = composite.getRightGroup(); def gL = composite.getLeftGroup()
		mapit( gR ); mapit( gL )
		nodeLabel( 'group', Dchild, composite.getType().getName(), 
				composite.getLeftGroup().getExtension(), composite.getLeftGroup().getUuid(),
				composite.getRightGroup().getExtension(), composite.getRightGroup().getUuid(), 
				colorspec="", data=countMap[ Dchild.getUuid() ] )
		if (! graphBySubject || gL.hasMember( subject )) {
			if (debugRecursion) { for (i = 0; i < indent; i++) print("."); print ("DL: "+ Dchild.getName() + " <- "+gL.getName() +"\n") }
			linkGroup( gL, Dchild.getUuid(), true )
			assert decomposeGroup( indent, Dchild, gL ) == gL
		}

		if (! graphBySubject || gR.hasMember( subject )) {
			if (debugRecursion) { for (i = 0; i < indent; i++) print("."); print ("DR: "+ Dchild.getName() + " <- "+gR.getName() +"\n") }
			linkGroup( gR, Dchild.getUuid(), true )
			assert decomposeGroup( indent, Dchild, gR ) == gR
		}
	} else {
		nodeLabel( 'group', Dchild, '', '', '', '', '', colorSpec="", data=countMap[ Dchild.getUuid() ] )
		if (graphBySubject) println "MEMBR: ${Dchild.getName()} ${countMap[ Dchild.getUuid() ]}"
		linkGroup( Dchild, Dparent.getUuid(), true )
		resolveUnionGroup( indent, Dchild )
	}
	if (debugRecursion) { for (i = 0; i < indent; i++) print("."); print ("DO: "+ Dparent.getName() + " <- "+Dchild.getName() +"\n") }
	Dchild
}

/*
Main code starts here - graph traversal and output creation
*/

def GrouperSession session = GrouperSession.startRootSession(); 

def String parentId, parentRoot, gRoot, s
def parent, child, topChild, Dparent, Dchild, subject, member
def childList
def folder, k, v, t
def int ct
def boolean graphBySubject = false

File graph = new File( outputDotFile )

// First figure out the starting point

if ( Root.toLowerCase().startsWith('subject:') ) {
        subSpec = Root.split(':').drop(1)
        subSource = subSpec[0]; subId = subSpec[1]
	subject = null
	try { subject = SubjectFinder.findByIdOrIdentifierAndSource( subId, subSource, true);
	} catch (Exception e) {
		println "Subject: subId; $e"
	}
	if (subject != null) {
		// println "Subject = $subject"
		member = MemberFinder.findBySubject(session, subject)
		// println "Member = $member"
		childList = member.getGroups()
		//println "ChildList = $childList"
		Root = subId; gRoot = ""
		parent = StemFinder.findByName( session, gRoot)  // orient ourselves at the top
		parent.name = subId
		graphBySubject = true
		doBackTrack = true
	}
} 
if (Root.equals('Root')) gRoot = "" else gRoot = Root
if ( ! graphBySubject )
	try {	parent = StemFinder.findByName( session, gRoot)  
		childList = parent.getChildGroups( Stem.Scope.SUB )
	} catch (Exception e) {
		// we got here because a Group is the starting point or a bogus stem
		try {	parent = GroupFinder.findByName( session, gRoot )
		} catch (Exception ee) {
			s = "\n\n\tSorry bud!  Bad starting point at ($gRoot)\n\n\tException: $ee\n\n"
			throw new Exception("Aborting: " + s, ee)
		}
		childList = [ parent ]
		if (parent.getName().indexOf(':') > -1)
			parent = parent.getName().tokenize(':')[0..-2].join(':') // chop last token
		parent = StemFinder.findByName( session, parent )
	}

def graphURL = '"'+ labelHREFstem + parent.getUuid() + '"'
graph.text = """digraph "Grouper Graph of: $Root
\n${parent.getDescription()}
\nat $RunTimestamp" {
	node	[
		shape=none;
		$defaultNodeAttrs 
		];
	graph	[ 
		center=true; splines=spline; ratio=auto;
		ranksep = ".5"; nodesep = ".25 equally"; rankdir=LR;
		$graphAttrs
		];

"""

parentRoot = parent.getUuid()
mapit( parent )
if (parent.getName() != "") folder = parent.getName() else folder = Root
print("Start: $folder and ${childList.size()} descendant(s)\n\n")

skipFolders.each { k, v -> 
	folder = StemFinder.findByName( session, k )
	v = folder.getChildGroups( Stem.Scope.SUB ).size()
	skipFolders[ k ] = v
}

for ( child in childList ) { 
	if (child.getName().indexOf(':') > -1) {
		folder = child.getName().tokenize(':')[0..-2].join(':') // chop last token
		ct = 1
		skipFolders.each { k, v -> if ( folder.startsWith( k+':' ) ) { ct = 0; t = k } } // filter out known large folders
		if ( ct && ! skipFolders.containsKey( folder ) ) {
			ct = childList.count { it.name.startsWith( folder ) }
			if ( ct > skipFolderCount && folder != parent.getName() ) {
				println "*** Too many items - will only graph explicit references.  $folder ($ct)"
				skipFolders[ folder ] = ct
			} else {
				if (debugDot) println "EvalGroup: $folder <- ${child.getName()}"
				assert decomposeGroup( indent, parent, child ) == child
			}
		} else { 
			if (debugDot) println "skip: ${child.getName()} <- $folder --> ($t)"
		}
	} else {
		print "EvalRootGroup: "
		if (parent.getName() != "") print parent.getName() else print Root
		println " <- ${child.getName()}"
		assert decomposeGroup( indent, parent, child ) == child
	}
}

// graph non-Zero counts on skipFolders nodes

def x, y
def int skipTotal = 0
skipFolders.each { k, v ->
	ct = 0
	nodesMap.each { x, y -> if ( y.startsWith( k+':' ) ) ct++ }
	if (debugDot) println "skipTotal: $k: v = $v, ct = $ct, v - ct = ${v-ct}"
	v -= ct; skipTotal += v; if (v < 0) v = 0
	if ( v > 0 && ct != 0 ) {
		child = StemFinder.findByName( session, k )
		mapit( child )
		overRideNode = true
		if ( !graphBySubject )
			nodeLabel( 'stem', child, '', '', '', '', '', colorSpec=skipFolderNodeColor, "Skipped Nodes = $v" )
		nodeTypeMap[ child.getUuid() ] = 's'
		overRideNode = false
		backTrack( child )
	}	
}

// show stats and run dot to convert the graphviz output file to svg and clean up the svg

print "\n"
def int memTotal = 0, nodesTotal = nodesMap.size()
countMap.each{k, v -> memTotal += v}
if (gRoot == "") nodesTotal--
if (graphBySubject) nodesTotal -= (nodesMap.size() - seenMap.size())

s = """$Root
at $RunTimestamp

Graph Edges: ${linkSeenMap.size()}
Memberships: ${memTotal} 
Nodes: $nodesTotal of ${seenMap.size()} 
Loader Jobs: ${loaderJobs.size()} 
Loader Groups: ${loaderGroups.size()}
Skipped Folders: ${skipFolders.size()}
Skipped Groups: ${skipTotal}"""

println "$s\n"
stitle = s
s = s.replace("\n", "<BR/>");
s = "{ \"Statistics\n$stitle\" ${TotalAttrs}$s</FONT></TD></TR></TABLE>> ]; };"
graph.append "$s\n}\n" // and we're done

if ( nodesMap.size() != seenMap.size() ) {
	println "Nodes referenced but not resolved; likely due to Composites"
	nodesMap.each{k, v -> if (!seenMap.containsKey( k ) && k != parent.getUuid() ) println "$v"}
	println ""
}

def runCommand = { cmdString ->
	println "Executing: $cmdString"
	def sout = new StringBuilder(); serr = new StringBuilder()
	def proc = cmdString.execute()
	proc.consumeProcessOutput(sout, serr)
	proc.waitFor()
	print sout + serr
}

runDot = "dot -Tsvg -o $outputSvgFile $outputDotFile"
runCommand( runDot )
runDot = "dot -Tpdf -o $outputPdfFile $outputDotFile"
//runCommand( runDot )
