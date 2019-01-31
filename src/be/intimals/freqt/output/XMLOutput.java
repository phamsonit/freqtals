package be.intimals.freqt.output;

import be.intimals.freqt.structure.*;
import be.intimals.freqt.config.*;

import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Vector;

public class XMLOutput extends AOutputFormatter {

    Map<String,String> patSupMap;

    public XMLOutput(Config _config, Map<String, Vector<String>> _grammar, Map<String,String> _xmlCharacters) throws IOException {
        super(_config, _grammar, _xmlCharacters);
    }

    public XMLOutput(Config _config, Map<String, Vector<String>> _grammar, Map<String,String> _xmlCharacters, Map<String,String> _patSupMap) throws IOException {
        super(_config, _grammar, _xmlCharacters);
        patSupMap = _patSupMap;
    }

    @Override
    protected void openOutputFile() throws IOException {
        super.openOutputFile();
        out.write("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"no\"?>\n\n");
        out.write("<results>\n\n");
    }

    /**
     * Represent subtrees in XML format + Ekeko
     * @param pat
     * @param projected
     */
    @Override
    public void report(Vector<String> pat, Projected projected){
        try{

            if( checkOutputConstraint(pat) ) return;

            Map<String,Integer> metaVariable = new HashMap<>();
            ++nbPattern;

            if(config.postProcess()){
                String patTemp = Pattern.getPatternString(pat);
                String[] sup = patSupMap.get(patTemp).split(" ");
                out.write("<subtree id=\""+ nbPattern+ "\" support=\"" + sup[1] +
                        "\" wsupport=\"" + sup[2] + "\" size=\"" + sup[3] + "\">\n");
            }
            else{
                int sup = projected.getProjectedSupport();
                int wsup = projected.getProjectedRootSupport();
                int size = Pattern.getPatternSize(pat);

                out.write("<subtree id=\""+ nbPattern+ "\" support=\"" + sup +
                        "\" wsupport=\"" + wsup + "\" size=\"" + size + "\">\n");
            }


            //System.out.println(nbPattern);
            int n = 0;
            Vector < String > tmp = new Vector<>();
            //number of meta-variable ???
            for ( int i = 0; i < pat.size () - 1; ++i) {
                //open a node
                if (!pat.elementAt(i).equals(")") && !pat.elementAt(i + 1).equals(")") ) {

                    String nodeOrder = grammar.get(pat.elementAt(i)).elementAt(0);
                    String nodeDegree = grammar.get(pat.elementAt(i)).elementAt(1);
                    Vector<String> childrenList = Pattern.findChildren(pat,i);

                    if(nodeOrder.equals("unordered")){
                        switch (nodeDegree){
                            case "1":
                                switch (childrenList.size()){
                                    case 0:
                                        String metaLabel = getMetaLabel(pat, metaVariable, i);
                                        out.write("<" + pat.elementAt(i) + ">\n");
                                        out.write("<Dummy>\n");
                                        out.write("<__directives>\n");
                                        out.write("<optional />\n");
                                        out.write("<meta-variable>\n");
                                        out.write("<parameter key=\"name\" value=\"?"+metaLabel+"\"/>\n");
                                        out.write("</meta-variable>\n");
                                        out.write("</__directives>\n");
                                        out.write("</Dummy>\n");
                                        break;

                                    default:
                                        out.write("<" + pat.elementAt(i) + ">\n");
                                        break;
                                }
                                break;

                            case "1..*":
                                out.write("<" + pat.elementAt(i)+">\n");
                                out.write("<__directives>");
                                out.write("<match-set/>");
                                out.write("</__directives>\n");
                                break;

                            default:
                                out.write("<" + pat.elementAt(i)+">\n");
                                //out.write("<__directives>");
                                //out.write("<match-set/>");
                                //out.write("</__directives>\n");
                                break;

                        }
                    }
                    else{
                        switch (nodeDegree){
                            case "1":
                                switch (childrenList.size()){
                                    case 0:
                                        String metaLabel = getMetaLabel(pat, metaVariable, i);
                                        out.write("<" + pat.elementAt(i) + ">\n");
                                        out.write("<Dummy>\n");
                                        out.write("<__directives>\n");
                                        out.write("<optional />\n");
                                        out.write("<meta-variable>\n");
                                        out.write("<parameter key=\"name\" value=\"?"+metaLabel+"\"/>\n");
                                        out.write("</meta-variable>\n");
                                        out.write("</__directives>\n");
                                        out.write("</Dummy>\n");
                                        break;

                                    default:
                                        out.write("<" + pat.elementAt(i) + ">\n");
                                        break;
                                }
                                break;

                            default: //N children: if this node has full children
                                out.write("<" + pat.elementAt(i) + ">\n");
                                out.write("<__directives>");
                                out.write("<match-sequence/>");
                                out.write("</__directives>\n");
                                break;

                        }

                    }

                    //track open node label
                    //System.out.println(tmp);
                    tmp.addElement(pat.elementAt(i));
                    ++n;
                }else {
                    //print leaf node of subtree
                    if (!pat.elementAt(i).equals(")") && pat.elementAt(i + 1).equals(")")) {
                        if (pat.elementAt(i).charAt(0) == '*') {

                            for(int t=1; t<pat.elementAt(i).length(); ++t)
                                if (xmlCharacters.containsKey(String.valueOf(pat.elementAt(i).charAt(t))))
                                    out.write(xmlCharacters.get(String.valueOf(pat.elementAt(i).charAt(t))));
                                else out.write(pat.elementAt(i).charAt(t));
                                out.write("\n");

                        } else { //leaf of subtree is an internal node in the original tree
                            outputNode(pat, metaVariable, i);
                        }
                    } else {
                        //close a node
                        if (pat.elementAt(i).equals(")") && pat.elementAt(i + 1).equals(")")) {
                            out.write("</" + tmp.elementAt(n - 1) + ">\n");
                            //tmp.remove(tmp.lastElement());
                            tmp.remove(n-1);
                            --n;
                        }
                    }
                }
            }

            //print the last node of pattern
            if(pat.elementAt(pat.size() - 1).charAt(0) == '*')  {

                for(int t=1; t<pat.elementAt(pat.size() - 1).length(); ++t)
                    if (xmlCharacters.containsKey(String.valueOf(pat.elementAt(pat.size() - 1).charAt(t))))
                        out.write(xmlCharacters.get(String.valueOf(pat.elementAt(pat.size() - 1).charAt(t))));
                    else out.write(pat.elementAt(pat.size() - 1).charAt(t));
                out.write("\n");



            }
            else {
                int i = pat.size() - 1;
                outputNode(pat, metaVariable, i);
            }

            //close nodes
            //System.out.println(tmp);
            for (int i = n - 1; i >= 0; --i)
                out.write( "</" + tmp.elementAt(i) + ">\n");

            out.write("</subtree>\n");
            out.write("\n");

        }
        catch (Exception e){
            System.out.println("report xml error : " + e);
            System.out.println(pat);

        }

    }

    @Override
    public void close() throws IOException {
        out.write("</results>\n");
        out.flush();
        out.close();
    }

    private void outputNode(Vector<String> pat, Map<String, Integer> metaVariable, int i) throws IOException {
        String nodeOrder = grammar.get(pat.elementAt(i)).elementAt(0);
        String nodeDegree = grammar.get(pat.elementAt(i)).elementAt(1);
        if(nodeOrder.equals("unordered")){
            switch (nodeDegree){
                case "1":
                    String metaLabel = getMetaLabel(pat, metaVariable, i);

                    out.write("<" + pat.elementAt(i) + ">\n");
                    out.write("<Dummy>\n");
                    out.write("<__directives>\n");
                    out.write("<optional />\n");
                    out.write("<meta-variable>\n");
                    out.write("<parameter key=\"name\" value=\"?"+metaLabel+"\"/>\n");
                    out.write("</meta-variable>\n");
                    out.write("</__directives>\n");
                    out.write("</Dummy>\n");
                    out.write("</" + pat.elementAt(i) + ">\n");
                    break;

                case "1..*":
                    out.write("<" + pat.elementAt(i)+">\n");
                    out.write("<__directives>");
                    out.write("<match-set/>");
                    out.write("</__directives>\n");
                    out.write("</" + pat.elementAt(i)+">\n");
                    break;

                default:
                    out.write("<" + pat.elementAt(i)+"/>\n");
                    break;

            }
        }
        else{
            out.write("<" + pat.elementAt(i) + ">\n");
            out.write("<__directives>");
            out.write("<match-sequence/>");
            out.write("</__directives>\n");
            out.write("</" + pat.elementAt(i) + ">\n");
        }
    }

    private String getMetaLabel(Vector<String> pat, Map<String, Integer> metaVariable, int i) {
        String metaLabel;
        if(metaVariable.containsKey(pat.elementAt(i))){
            metaVariable.put(pat.elementAt(i), metaVariable.get(pat.elementAt(i))+1);
            metaLabel = pat.elementAt(i)+String.valueOf(metaVariable.get(pat.elementAt(i)));
        }else{
            metaLabel = pat.elementAt(i)+"1";
            metaVariable.put(pat.elementAt(i),1);
        }
        return metaLabel;
    }
}
