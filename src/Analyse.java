import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;


public class Analyse {

    // read data from output of Freqt

    static  Vector<String> pattern = new Vector<>();


    public void analysePattern(String inputFile, String outputFile) {

        //readPattern(path);
        //String file = inputFile;
        try (BufferedReader br = new BufferedReader(new FileReader(inputFile))) {
            String line;
            while ((line = br.readLine()) != null) {
                if (!line.isEmpty())
                    pattern.add(line);
            }
        } catch (IOException e) {System.out.println("Error: reading patterns");}

        //find leaf set
        Map<String, String> leafMap = new LinkedHashMap();
        Set<String> leafSet = new LinkedHashSet<>();

        for (int i = 0; i < pattern.size(); ++i) {
            int j = 0;
            String leafStringTmp = "";
            while (j < pattern.elementAt(i).length()) {
                String leafTmp = "";
                int start;
                int end;
                if (pattern.elementAt(i).charAt(j) == '*') {
                    start = j;
                    int bracket = 0;
                    //while (pattern.elementAt(i).charAt(j) != ')') {
                    while(bracket >= 0){
                        if(pattern.elementAt(i).charAt(j)=='(') ++bracket;
                        else if(pattern.elementAt(i).charAt(j)==')') --bracket;
                        ++j;
                    }
                    end = j-1;
                    leafTmp = pattern.elementAt(i).substring(start, end);
                    leafStringTmp += leafTmp;
                } else
                    ++j;
            }
            leafMap.put(pattern.elementAt(i), leafStringTmp);
            leafSet.add(leafStringTmp);
        }


        //output
        try{
            FileWriter out = new FileWriter(outputFile);

            Iterator<String> iterSet = leafSet.iterator();
            while (iterSet.hasNext()) {
                String tmp = iterSet.next();
                out.write(tmp+"\n");
                //System.out.println(tmp);
                Iterator<Map.Entry<String, String>> iter = leafMap.entrySet().iterator();
                while (iter.hasNext()) {
                    Map.Entry<String, String> entry = iter.next();
                    if (entry.getValue().equals(tmp)) {
                        //System.out.println(entry.getKey());
                        out.write(entry.getKey()+"\n");
                    }
                }
            }
        }
        catch (Exception e){}

        System.out.println("# leaf sets / patterns " + leafSet.size() + " / " + leafMap.size());
    }


}
