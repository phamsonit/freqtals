package be.intimals.freqt.core;

import be.intimals.freqt.structure.*;
import be.intimals.freqt.config.*;
import be.intimals.freqt.util.*;


import java.io.*;
import java.util.*;

import be.intimals.freqt.output.*;

public class FreqT {
    static char uniChar = '\u00a5';// Japanese Yen symbol
    protected Config config;

    private AOutputFormatter outputFrequent;
    private Vector <String> pattern;
    private Vector <Vector<NodeFreqT> >  transaction = new Vector<>();

    protected Map <String,Vector<String> > grammar     = new LinkedHashMap<>();
    protected Map <String,Vector<String> > blackLabels = new LinkedHashMap<>();
    protected Map <String,Vector<String> > whiteLabels = new LinkedHashMap<>();
    protected Map <String,String>          xmlCharacters  = new LinkedHashMap<>();

    private Set <String>                 rootLabels  = new LinkedHashSet<>();
    private Map<String,String>           outputFrequentPatternsMap = new LinkedHashMap<>(); //store patterns for post-processing

    private int oldRootSupport;

    private  int maxSize = 0;
    private String maxPattern = "";

    private int nbInputFiles;
    private int nbOutputFrequentPatterns;
    private int nbOutputLagestPatterns;
    private int nbOutputMaximalPatterns;

    private Map<String,String> fileIDs = new LinkedHashMap<>();
    private Map<String,String> rootIDs = new LinkedHashMap<>();

    private boolean threeSteps = true;
    private int nbIdentifiers = 2;


    Set<String> trackCandidates = new LinkedHashSet<>();

    ////////////////////////////////////////////////////////////////////////////////

    public FreqT(Config config) {
        this.config = config;
    }

    public int getNbInputFiles(){
        return this.nbInputFiles;
    }
    public int getNbOutputFrequentPatterns(){
        return this.nbOutputFrequentPatterns;
    }
    public int getNbOutputLagestPatterns(){
        return this.nbOutputLagestPatterns;
    }
    public int getNbOutputMaximalPatterns(){
        return this.nbOutputMaximalPatterns;
    }

    public Map <String,Vector<String> > getGrammar(){ return this.grammar;}
    public Map <String,String> getXmlCharacters(){return this.xmlCharacters;}


    public boolean checkOutput(Vector<String> pat){

        if(Pattern.countLeafNode(pat) < config.getMinLeaf() ||
                Pattern.countIdentifiers(pat) <= nbIdentifiers)
            return true;
        else
            return false;

    }

    /**
     * store frequent subtrees for post-processing
     * @param pat
     * @param projected
     */
    public void addPattern(Vector<String> pat, Projected projected,
                            Map<String,String> _outputFrequentPatternsMap){

        if(checkOutput(pat)) return;

        int support = projected.getProjectedSupport();
        int wsupport = projected.getProjectedRootSupport(); //=> root location
        int size = Pattern.getPatternSize(pat);
        //keep fileIds for itemset mining algorithm
        String fileIds = String.valueOf(projected.getProjectLocation(0).getLocationId());
        int oldId = projected.getProjectLocation(0).getLocationId();
        for(int i=1;i<projected.getProjectLocationSize(); ++i)
            if(oldId != projected.getProjectLocation(i).getLocationId() ) {
                fileIds = fileIds+","+ String.valueOf(projected.getProjectLocation(i).getLocationId());
                oldId = projected.getProjectLocation(i).getLocationId();
            }

        String patternString = Pattern.getPatternString1(pat); //filter out the right part of pattern which misses leaf nodes

        String patStr =
                fileIds + " " +
                        String.valueOf(support)+" "+
                        String.valueOf(wsupport)+" "+
                        String.valueOf(size);

        _outputFrequentPatternsMap.put(patternString,patStr);
    }

    /**
     * store fileIDs of pattern for grouping patterns by fileIDs
     * @param pat
     * @param projected
     */
    private void addFileIDs(Vector<String> pat, Projected projected){

        //if(Pattern.checkMissedLeafNode(pat) || (Pattern.countLeafNode(pat) < config.getMinLeaf()) ) return;
        if((Pattern.countLeafNode(pat) < config.getMinLeaf()) ) return;

        String fileIds = String.valueOf(projected.getProjectLocation(0).getLocationId());
        int oldId = projected.getProjectLocation(0).getLocationId();
        for(int i=1;i<projected.getProjectLocationSize(); ++i)
            if(oldId != projected.getProjectLocation(i).getLocationId() ) {
                fileIds = fileIds+","+ String.valueOf(projected.getProjectLocation(i).getLocationId());
                oldId = projected.getProjectLocation(i).getLocationId();
            }

        String patternString = Pattern.getPatternString1(pat); //filter out the right part of pattern which misses leaf nodes

        if(fileIDs.containsKey(fileIds)){
            if(fileIDs.get(fileIds).length() < patternString.length())
                fileIDs.replace(fileIds,patternString);
        }else
            fileIDs.put(fileIds,patternString);
    }

    /**
     * store root occurrences of patterns for grouping patterns by root occurrences
     * @param pat
     * @param projected
     */
    private void addRootIDs(Vector<String> pat, Projected projected){
        try {
            if (checkOutput(pat)) return;
            nbOutputFrequentPatterns++;
            //find rootID of pattern
            String rootOccurrences = "";
            for (int i = 0; i < projected.getProjectRootLocationSize(); ++i) {
                rootOccurrences = rootOccurrences +
                        projected.getProjectRootLocation(i).getLocationId() + (",") +
                        projected.getProjectRootLocation(i).getLocationPos() + ";";
            }
            //find root label of this pattern
            //String rootLabel = Pattern.getPatternString1(pat);
            String rootLabel = pat.elementAt(0);
            rootIDs.put(rootOccurrences, rootLabel);
        }catch (Exception e){System.out.println("Error: adding root IDs "+e);}
    }

    private void filterRootOccurrences(Map<String, String> _rootIDs){
        //for each element of rootIDs check
        //if rootOcc is a subset of an element of rootIDs then replace
        List<String> ttt = new LinkedList(_rootIDs.keySet());
        Collections.sort(ttt, new Comparator<String>() {
            @Override
            public int compare(String o1, String o2) {
                return (Integer.valueOf(o1.length()).compareTo(o2.length()));
            }
        });

        //System.out.println(ttt);
        for(int i=0; i<ttt.size()-1; ++i) {
            for (int j = i + 1; j < ttt.size(); ++j) {
                if (compareTwoRootOccurrences(ttt.get(i), ttt.get(j)))
                    ttt.remove(j);
            }
        }
        //System.out.println(ttt);

        Map<String, String> _newRootIDs = new LinkedHashMap<>();
         for(int i=0;i<ttt.size();++i)
             if(_rootIDs.containsKey(ttt.get(i))) {
                 _newRootIDs.put(ttt.get(i), _rootIDs.get(ttt.get(i)));
             }

         rootIDs = _newRootIDs;
    }

    private boolean compareTwoRootOccurrences(String str1, String str2){
        Collection<String> l1 = Arrays.asList(str1.split(";"));
        Collection<String> l2 = Arrays.asList(str2.split(";"));

        if(l2.containsAll(l1))
            return true;
        else
            return false;
    }

    private void chooseOutput(Vector<String> pat, Projected projected){
        if(threeSteps){
            addRootIDs(pat, projected);
            outputFrequent.report(pat, projected);
        }else{
            if (config.postProcess())
                addPattern(pat,projected,outputFrequentPatternsMap);
            else outputFrequent.report(pat, projected);
        }

    }

    /**
     * prune candidates based on blacklist children
     * blacklist is created in the readWhiteLabel procedure
     * @param candidate
     */
    public void pruneBlackList(Vector<String> pat, Map <String, Projected > candidate,
                                Map <String,Vector<String> > _blackLabels){
        Iterator < Map.Entry<String,Projected> > iterTemp = candidate.entrySet().iterator();
        while (iterTemp.hasNext()) {
            Map.Entry<String, Projected> entry = iterTemp.next();
            Set<String> blackListChildren = Pattern.getChildrenLabels(_blackLabels,pat,entry.getKey());
            //System.out.println("blackListChildren "+ blackListChildren);


            String candidateLabel = Pattern.getPotentialCandidateLabel(entry.getKey());
            //System.out.println("candidateLabel "+ candidateLabel);
            if(     blackListChildren.contains(candidateLabel) ||
                    candidateLabel.equals("*get") ||
                    candidateLabel.equals("*set") ||
                    candidateLabel.equals("*RETURN-CODE") ||
                    candidateLabel.equals("*SORT-RETURN") ||
                    candidateLabel.equals("*SORT-CORE-SIZE") ||
                    candidateLabel.equals("*TALLY") ||
                    candidateLabel.equals("*XML-CODE") ){
                //System.out.println(candidateLabel+" in black list");
                iterTemp.remove();
            }
        }
    }

    /**
     * calculate the support of a pattern
     * @param projected
     * @return
     */
    public int support(Projected projected){
        //if(weighted) return projected.getProjectLocationSize();
        int old = 0xffffffff;
        int sup = 0;
        for(int i=0; i<projected.getProjectLocationSize(); ++i) {
            if (projected.getProjectLocation(i).getLocationId() != old)
                ++sup;
            old = projected.getProjectLocation(i).getLocationId();
        }
        return sup;
    }

    /**
     *
     * @param projected
     * @return
     */
    public int rootSupport(Projected projected){
        int rootSup = 1;
        for(int i=0; i< projected.getProjectRootLocationSize()-1;++i) {
            Location location1 = projected.getProjectRootLocation(i);
            Location location2 = projected.getProjectRootLocation(i+1);

            if( (location1.getLocationId() == location2.getLocationId() &&
                    location1.getLocationPos() != location2.getLocationPos()) ||
                    location1.getLocationId() != location2.getLocationId()
                    )
                ++rootSup;
        }

        return rootSup;
    }
    /**
     * prune candidates based on minimal support
     * @param candidates
     */
    public void prune (Map <String, Projected > candidates, int minSup){

        Iterator < Map.Entry<String,Projected> > iter = candidates.entrySet().iterator();
        while (iter.hasNext()) {
            Map.Entry<String,Projected> entry = iter.next();
            int sup = support(entry.getValue());
            int wsup = rootSupport(entry.getValue());
            if(sup < minSup){
                iter.remove();
            }
            else {
                entry.getValue().setProjectedSupport(sup);
                entry.getValue().setProjectedRootSupport(wsup);
            }
        }
    }

    ////Procedures to expand pattern ///////

    /**
     *right most extension
     * @param projected
     * @return
     */
    public Map<String, Projected> generateCandidates(Projected projected,
                                                     Vector <Vector<NodeFreqT> >  _transaction) {
        Map<String, Projected> candidates = new LinkedHashMap<>();
        //keep the order of elements
        try{
            // Find all candidates of the current subtree
            int depth = projected.getProjectedDepth();
            for (int i = 0; i < projected.getProjectLocationSize(); ++i) {
                int id = projected.getProjectLocation(i).getLocationId();
                int pos = projected.getProjectLocation(i).getLocationPos();
                // Add to keep all occurrences --> problem: memory consumption
                //List<Integer> occurrences = projected.getProjectLocation(i).getLocationList();
                //only keep root location and right-most location
                List<Integer> occurrences = projected.getProjectLocation(i).getLocationList().subList(0,1);
                //keep lineNr to calculate distance of two nodes
                //List<Integer> lines = projected.getProjectLineNr(i);

                String prefix = "";
                for (int d = -1; d < depth && pos != -1; ++d) {
                    int start = (d == -1) ? _transaction.elementAt(id).elementAt(pos).getNodeChild() :
                            _transaction.elementAt(id).elementAt(pos).getNodeSibling();
                    int newDepth = depth - d;
                    for (int l = start; l != -1;
                         l = _transaction.elementAt(id).elementAt(l).getNodeSibling()) {
                        String item = prefix + uniChar + _transaction.elementAt(id).elementAt(l).getNodeLabel();
                        //String lineNrTemp = transaction.elementAt(id).elementAt(l).getLineNr();

                        Projected tmp;
                        if (candidates.containsKey(item)) {
                            //candidate.get(item).setProjectLocation(id, l); //keep right most position
                            candidates.get(item).addProjectLocation(id, l, occurrences);//keeping all locations
                            //candidate.get(item).addProjectLineNr(Integer.valueOf(lineNrTemp),lines);
                            //rootId = id, rootPos = ?
                            int rootPos = projected.getProjectLocation(i).getLocationList().get(0);
                            //if (id, rootPos) exists in root ???
                            candidates.get(item).setProjectRootLocation(id, rootPos);//keeping root locations
                        } else {
                            tmp = new Projected();
                            tmp.setProjectedDepth(newDepth);
                            //tmp.setProjectLocation(id, l); //keep right most position
                            tmp.addProjectLocation(id, l, occurrences); //keeping all locations
                            //tmp.addProjectLineNr(Integer.valueOf(lineNrTemp),lines);
                            int rootPos = projected.getProjectLocation(i).getLocationList().get(0);
                            tmp.setProjectRootLocation(id, rootPos); //keeping root locations
                            candidates.put(item, tmp);
                        }
                    }
                    if (d != -1) {
                        pos = _transaction.elementAt(id).elementAt(pos).getNodeParent();
                    }
                    prefix += uniChar + ")";
                }
            }

        }
        catch (Exception e){System.out.println("Error: generate candidates" + e);}
        return candidates;
    }


    //input: String nodeLabel, Projected projected
    //output: previous sibling nodeLabels always occurred with the input nodeLabel
    //pseudo code
    //for each position, find its parent position
    //for each parent position find all previous sibling positions
    //if existing a previous sibling which has the same number of ... --> prune nodeLabel
    //problem: we cannot guarantee the previous sibling nodeLabel include the pruning nodeLabel

    public Set<String> getPreviousSibling(Projected projected,
                                                     Vector <Vector<NodeFreqT> >  _transaction) {


        Set<String> listSibling = new LinkedHashSet<>();
        try{
            //System.out.println("find sibling==============================");
            List<String> siblingList = new LinkedList<>();

            for (int i = 0; i < projected.getProjectLocationSize(); ++i) {
                int id = projected.getProjectLocation(i).getLocationId();
                int pos = projected.getProjectLocation(i).getLocationPos();
                //System.out.println(_transaction.elementAt(id).elementAt(pos).getNodeLabel());
                //find parent of id,pos
                int parent = _transaction.elementAt(id).elementAt(pos).getNodeParent();
                //System.out.println(_transaction.elementAt(id).elementAt(parent).getNodeLabel());
                //find list of sibling
                String tmp="";
                int start1 = _transaction.elementAt(id).elementAt(parent).getNodeChild();
                for(int sibling = start1; sibling < pos;
                    sibling=_transaction.elementAt(id).elementAt(sibling).getNodeSibling()){
                    tmp = tmp+","+_transaction.elementAt(id).elementAt(sibling).getNodeLabel();
                }
                //System.out.println(tmp);
                siblingList.add(tmp);
            }
            //find intersection of all sibling list
            //if it is not empty return true
            if(siblingList.size()>0) {
                Collection<String> s0 = Arrays.asList(siblingList.get(0).split(","));
                for (int i = 1; i < siblingList.size(); ++i) {
                    Collection<String> si = Arrays.asList(siblingList.get(0).split(","));
                    s0.retainAll(si);
                }
                if(s0.size()>0)
                    //System.out.println(s0);
                    listSibling.addAll(s0);
            }
        }catch (Exception e){System.out.println("Error: find sibling labels" + e);}
        return  listSibling;

    }

    //TODO: expanding all locations for unordered children
    public Map<String, Projected> generateUnorderedCandidates(Projected projected) {
        int depth = projected.getProjectedDepth();
        Map<String, Projected> candidate = new LinkedHashMap<>(); //keep the order of elements
        /**
         * if X has unordered children
         * find candidates of the right most position
         * and candidates of the X in both directions: backward, forward
         * how to extend these two set of candidates ???
         */
        return  candidate;
    }

    //expand candidate based on grammar
    private void grammarExpand(Map.Entry<String, Projected> entry){
        //get the current candidate label

        String potentialCandidate = Pattern.getPotentialCandidateLabel(entry.getKey());
        if ( potentialCandidate.charAt(0) == '*' ) { //potentialCandidate is a leaf node
            project( entry.getValue() );
        }else {//internal node
            //find grammar of parent of potentialCandidate
            int parentPos = Pattern.findParentPosition(pattern, entry.getKey());
            String parentLabel = pattern.elementAt(parentPos).split(String.valueOf(uniChar))[0];

            if ( grammar.containsKey(parentLabel) ) {
                String parentOrdered = grammar.get(parentLabel).elementAt(0);
                String parentDegree  = grammar.get(parentLabel).elementAt(1);
                switch (parentDegree) {
                    case "1": //node has one non-terminal child or one leaf
                        project(entry.getValue());
                        break;
                    case "1..*"://node-list
                        if(parentOrdered.equals("unordered")) {
                            /*//check previous siblings
                            Set<String> previousSiblings = getPreviousSibling(entry.getValue(), transaction);
                            //System.out.println(previousSiblings);
                            Set<String> currentChildren = new HashSet<>(Pattern.findChildren(pattern,parentPos));
                            //System.out.println(currentChildren);
                            previousSiblings.retainAll(currentChildren);
                            if(previousSiblings.size()>0){//output the current pattern
                                chooseOutput(pattern,entry.getValue());
                                return;
                            }*/
                            //grammar constraint: don't allow N children of an unordered node to have the same label
                            if (Pattern.checkRepeatedLabel(pattern, entry.getKey(), config.getMaxRepeatLabel())){
                            //check line distance of 2 nodes which have the same label
                            //if(Pattern.checkLineDistance(pattern, entry.getKey(), entry.getValue(), config.getMinLineDistance(), config.getMaxLineDistance()))
                                project(entry.getValue());
                            }else{//output the current pattern
                                    chooseOutput(pattern,entry.getValue());
                                    return;
                            }

                        }else
                            if(parentOrdered.equals("ordered")){
                                project(entry.getValue());
                            }
                        break;

                    default: //AST node has fixed N children
                        //project(entry.getValue());
                        //find all children of parentPos in the grammar
                        Vector<String> listOfChildrenGrammar = new Vector<>();
                        listOfChildrenGrammar.addAll(grammar.get(parentLabel).subList(2, grammar.get(parentLabel).size()));
                        //find all children of parentPos in the pattern
                        Vector<String> listOfChildrenPattern = Pattern.findChildren(pattern, parentPos);
                        //find white labels and black labels
                        Set<String> blackLabelChildren = new LinkedHashSet<>();
                        Set<String> whiteLabelChildren = new LinkedHashSet<>();
                        if(whiteLabels.containsKey(parentLabel))
                            whiteLabelChildren.addAll(whiteLabels.get(parentLabel));
                        if(blackLabels.containsKey(parentLabel))
                            blackLabelChildren.addAll(blackLabels.get(parentLabel));
                        //expand this candidate if it doesn't miss the previous mandatory sibling
                        if (! Pattern.checkMissedMandatoryChild(listOfChildrenPattern, listOfChildrenGrammar, blackLabelChildren, whiteLabelChildren)) {
                            project(entry.getValue());
                        }

                        break;
                }
            }
        }

    }

    /**
     * expand a pattern
     * @param entry
     */
    private void expandCandidate(Map.Entry<String, Projected> entry) {
        try{

            //add a candidate to the current pattern
            String[] p = entry.getKey().split(String.valueOf(uniChar));
            for (int i = 0; i < p.length; ++i) {
                if (!p[i].isEmpty())
                    pattern.addElement(p[i]);
            }

            /*if(Pattern.countLeafNode(pattern) <= config.getMaxLeaf())
                grammarExpand(entry);*/

            if(Pattern.countLeafNode(pattern) <= config.getMaxLeaf()){
                if (Pattern.checkMissedLeafNode(pattern)){
                    chooseOutput(pattern,entry.getValue());
                    return;
                }else
                    grammarExpand(entry);
            }
            else{ //if don't use the second step then expand patterns by root occurrences
                if(!threeSteps){
                    int newRootSupport = rootSupport(entry.getValue());
                    if (oldRootSupport == newRootSupport){
                        if (Pattern.checkMissedLeafNode(pattern)){
                            chooseOutput(pattern,entry.getValue());
                            return;
                        }else
                            grammarExpand(entry);
                    }
                    else{
                        chooseOutput(pattern,entry.getValue());
                        return;
                    }
                }
            }

        }catch (Exception e){System.out.println("Error: expand candidate " + e);}
    }

    private void updateMaximalPattern(Vector<String> pat){
    if(maxSize < Pattern.getPatternSize(pat)){
        maxSize = Pattern.getPatternSize(pat);
        maxPattern = Pattern.getPatternString1(pat);
    }

}

    /**
     * expand a subtree
     * @param projected
     */
    private void project(Projected projected) {
        try{

            oldRootSupport = rootSupport(projected);

            //System.out.println(pattern);
            //find candidates
            Map<String, Projected> candidates = generateCandidates(projected,transaction);
            //System.out.println("all candidates     " + candidates.keySet());
            prune(candidates,config.getMinSupport());
            //System.out.println("after support pruning " + candidates.keySet());
            //pruning based on blacklist: for each candidate if it occurs in the blacklist --> remove
            pruneBlackList(pattern,candidates,blackLabels);
            //System.out.println("after blacklist pruning " + candidates.keySet());

            //if there is no candidate then report pattern --> stop
            if( candidates.isEmpty() ){
                chooseOutput(pattern,projected);
                //outputFrequent.report(pattern,projected);
                return;
            }

            //expand the current pattern with each candidate
            Iterator < Map.Entry<String,Projected> > iter = candidates.entrySet().iterator();
            while (iter.hasNext()) {
                int oldSize = pattern.size();

                Map.Entry<String, Projected> entry = iter.next();

                expandCandidate(entry);

                oldRootSupport = rootSupport(entry.getValue());

                pattern.setSize(oldSize);
            }
        }catch (Exception e){System.out.println("Error: projected " + e);}
    }


    /**
     * run Freqt with file config.properties
     */
    public void run() {
        try{

            /*  ==============================  */
            //System.out.println("==============================");
            //System.out.println("running FreqT");
            //System.out.println("==============================");
            if(config.buildGrammar())
                Initial.initGrammar(config.getInputFiles(),grammar,config.buildGrammar());
            else
                Initial.initGrammar(config.getGrammarFile(),grammar,config.buildGrammar()) ;

            //ReadGrammar.printGrammar(grammar);
            Initial.readWhiteLabel(config.getWhiteLabelFile(), grammar, whiteLabels, blackLabels); //read white labels and create black labels
            Initial.readRootLabel(config.getRootLabelFile(), rootLabels);  //read root labels (AST Nodes)
            Initial.readXMLCharacter(config.getXmlCharacterFile(), xmlCharacters); //read list of special XML characters
            Initial.initDatabase(config.getInputFiles(),grammar,transaction);

            nbInputFiles = transaction.size();

            long start = System.currentTimeMillis( );
            outputFrequent = config.outputAsXML() ? new XMLOutput(config, grammar, xmlCharacters) :
                                           new LineOutput(config, grammar, xmlCharacters, uniChar);
            //find 1-subtree
            Map < String , Projected > freq1 = buildFreq1Set(transaction);
            //System.out.println("all candidates " + freq1.keySet());
            //prune 1-subtree
            prune(freq1, config.getMinSupport() );
            //System.out.println("all candidates after pruning " + freq1.keySet());
            //expand 1-subtree to find frequent subtrees
            expandFreq1(freq1);
            outputFrequent.close();

            long end1 = System.currentTimeMillis( );
            long diff1 = end1 - start;
            System.out.print("FREQT: frequent patterns = "+ nbOutputFrequentPatterns+", time = "+ diff1+" ,");

            if(threeSteps){
                //only keep subset rootIDs
                filterRootOccurrences(rootIDs);
                System.out.println("rootIDs groups = "+rootIDs.size());
                //find largest patterns according to rootIDs groups
                FreqT_ext freqT_ext = new FreqT_ext(config, this.grammar, this.blackLabels,this.whiteLabels,this.xmlCharacters);
                freqT_ext.run(rootIDs,transaction);
                nbOutputFrequentPatterns= freqT_ext.getNbOutputLargestPatterns();
                long end2 = System.currentTimeMillis( );
                long diff2 = end2 - end1;
                System.out.println("FREQT_EXT: largest patterns "+nbOutputFrequentPatterns+", time "+ diff2);
                //output freqT_ext.getOutputLargestPatterns

                //maximality check
                FreqT_max post = new FreqT_max(this.config, this.grammar, this.blackLabels, this.whiteLabels, this.xmlCharacters);
                post.run(freqT_ext.getOutputLargestPatterns());
                nbOutputMaximalPatterns = post.getNbMaximalPattern();
                long end3 = System.currentTimeMillis( );
                long diff3 = end3 - end2;
                System.out.println("FREQT_MAX: maximal patterns = "+nbOutputMaximalPatterns+", time = "+ diff3);
            }else{
                if(config.postProcess()){
                    FreqT_max post = new FreqT_max(this.config, this.grammar, this.blackLabels, this.whiteLabels, this.xmlCharacters);
                    post.run(outputFrequentPatternsMap);
                    nbOutputMaximalPatterns = post.getNbMaximalPattern();
                    /*long end3 = System.currentTimeMillis( );
                    long diff3 = end3 - end1;
                    System.out.println("FREQT_MAX: maximal patterns "+post.getNbMaximalPattern()+", time "+ diff3);*/
                }
            }

            //System.out.println(maxSize+" "+maxPattern);
        }
        catch (Exception e) {
            System.out.println("Error: running freqt");
            e.printStackTrace();
        }
    }


    private void expandFreq1(Map < String , Projected > freq1){
        pattern = new Vector<>();
        Iterator < Map.Entry<String,Projected> > iter = freq1.entrySet().iterator();
        while (iter.hasNext()) {
            Map.Entry<String,Projected> entry = iter.next();
            if(entry.getKey() != null && entry.getKey().charAt(0) != '*'){
                if(rootLabels.contains(entry.getKey()) || rootLabels.isEmpty())
                {
                    if (grammar.containsKey(entry.getKey())) {
                        entry.getValue().setProjectedDepth(0);
                        pattern.addElement(entry.getKey());

                        project(entry.getValue());

                        pattern.setSize(pattern.size() - 1);
                    } else {
                        System.out.println(entry.getKey() + " doesn't exist in grammar ");
                    }
                }
            }
        }

    }

    /**
     * Return all frequent subtrees of size 1
     * @return
     */
    public Map<String, Projected> buildFreq1Set(Vector < Vector<NodeFreqT> > trans) {
        Map<String, Projected> freq1 = new LinkedHashMap<>();
        for(int i = 0; i < trans.size(); ++i) {
            for (int j = 0; j < trans.elementAt(i).size(); ++j) {
                String node_label = trans.elementAt(i).elementAt(j).getNodeLabel();
                String lineNr = trans.elementAt(i).elementAt(j).getLineNr();
                //find a list of location then add to freq1[node_label].locations
                if(node_label != null){
                    //System.out.println("Node "+ node_label+" "+lineNr);
                    Projected projected = new Projected();
                    //if node_label already exists
                    if(freq1.containsKey(node_label)) {
                        freq1.get(node_label).setProjectLocation(i,j);
                        //freq1.get(node_label).setProjectLineNr(Integer.valueOf(lineNr)); //add to keep the line number
                        freq1.get(node_label).setProjectRootLocation(i,j);
                    }
                    else {
                        projected.setProjectLocation(i,j);
                        //projected.setProjectLineNr(Integer.valueOf(lineNr)); //add to keep the line number
                        projected.setProjectRootLocation(i,j);
                        freq1.put(node_label, projected);
                    }
                }
            }
        }
        return freq1;
    }

}