/*
   $Id: freqt.cpp,v 1.5 2003/01/22 08:37:19 taku-ku Exp $;

   Copyright (C) 2003 Taku Kudo, All rights reserved.
   This is free software with ABSOLUTELY NO WARRANTY.

   This program is free software; you can redistribute it and/or modify
   it under the terms of the GNU General Public License as published by
   the Free Software Foundation; either version 2 of the License, or
   (at your option) any later version.

   This program is distributed in the hope that it will be useful,
   but WITHOUT ANY WARRANTY; without even the implied warranty of
   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
   GNU General Public License for more details.

   You should have received a copy of the GNU General Public License
   along with this program; if not, write to the Free Software
   Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA
   02111-1307, USA
   ===============================
   java implementation: 16 May 2018
   by PHAM Hoang Son
*/
import be.intimals.freqt.util.*;
import be.intimals.freqt.core.*;
import be.intimals.freqt.config.*;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;

import java.lang.String;

public class Main {

    static public void main(String[] args) {

        try{

            for(int minsup = 10; minsup >= 5; minsup-- ) {

                //load basic configuration
                String configPathBasic = args.length == 0 ? "conf/java/config.properties" : args[0];
                Config configBasic = new Config(configPathBasic);

                String inputMinSup = args.length == 0 ? String.valueOf(configBasic.getMinSupport()) : args[1];
                String inputFold = args.length == 0 ? "draw/action" : args[2];
                int time = args.length == 0 ? 60 : Integer.valueOf(args[3]);

                //set time out for program
                TimeOut timeOut = new TimeOut();
                timeOut.setTimes(time * 60 * 1000);
                Thread timeOutThread = new Thread(timeOut);
                timeOutThread.start();

                //create temporary configuration
                Properties prop;
                OutputStream output = null;
                String configPathTemp = "";

                String inputPath = "";
                String outputPath = "";

                String sourceMatcher = "";
                String inputPatterns = "";
                String outputMatches = "";

                String reportFile = "";

                try {
                    prop = configBasic.getProp();

                    inputMinSup = String.valueOf(minsup);

                    //update input dir path
                    inputPath = configBasic.getInputFiles().replace("\"", "") + "/"+inputFold;
                    //update output file path
                    outputPath = configBasic.getOutputFile().replace("\"","") +
                            "/"+inputFold.replaceAll("\\/","-")+"-" + inputMinSup +"-patterns.xml";
                    //delete output file if if exists
                    Files.deleteIfExists(Paths.get(outputPath));

                    //create parameters for forest matcher
                    sourceMatcher = inputPath;
                    inputPatterns = outputPath;
                    outputMatches = configBasic.getOutputFile().replace("\"","") +
                            "/"+inputFold.replaceAll("\\/","-")+"-" +inputMinSup + "-matches.xml";
                    Files.deleteIfExists(Paths.get(outputMatches));

                    //update path of temporary configuration
                    configPathTemp = configBasic.getOutputFile().replace("\"","")
                            + "/"+ inputFold.replaceAll("\\/","-")+"-"+ inputMinSup + "-config.properties";
                    Files.deleteIfExists(Paths.get(configPathTemp));

                    //create report path
                    reportFile = configBasic.getOutputFile().replaceAll("\"","") +
                            "/"+inputFold.replaceAll("\\/","-")+"-"+ inputMinSup + "-report.txt";


                    //update properties
                    prop.replace("minSupport", inputMinSup);
                    prop.replace("inFiles", inputPath);
                    prop.replace("outFile", outputPath);
                    // save new properties
                    output = new FileOutputStream(configPathTemp);
                    prop.store(output, null);

                } catch (IOException io) {
                    io.printStackTrace();
                } finally {
                    if (output != null) {
                        try {
                            output.close();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }

                //load new configuration;
                Config config = new Config(configPathTemp);

                //run Freqt
                long start = System.currentTimeMillis();

                //find frequent subtrees
                FreqT freqt = new FreqT();
                freqt.run(config);
                //create report for each sub-dataset


                FileWriter report = new FileWriter(reportFile);

                report.write("data sources : " + config.getInputFiles() + "\n");
                System.out.println("data sources : " + config.getInputFiles());

                report.write("input files : " + freqt.getNbInputFiles() + "\n");
                System.out.println("input files : " + freqt.getNbInputFiles());

                report.write("minSupport : " + config.getMinSupport() + "\n");
                System.out.println("minSupport : " + config.getMinSupport());

                report.write("frequent patterns : " + freqt.getNbOutputFrequentPatterns() + "\n");
                System.out.println("frequent patterns : " + freqt.getNbOutputFrequentPatterns());

                report.write("maximal patterns : " + freqt.getNbOutputMaximalPatterns() + "\n");
                System.out.println("maximal patterns : " + freqt.getNbOutputMaximalPatterns());

                long end = System.currentTimeMillis();
                long diff = end - start;
                System.out.println("mining time : " + diff + " ms");
                report.write("running time : " + diff + " ms \n");
                //close report file
                report.close();


                //create transaction data for itemset mining
                //if(!config.outputAsXML())
                //Util.createTransaction(config.getOutputFile(),"eclat-"+config.getOutputFile());


                //run forestmatcher
                //System.out.println("Running forestmatcher ...");
                if(config.outputAsXML()){
                    String command = "java -jar forestmatcher.jar " +
                            sourceMatcher + " " + inputPatterns +" " + outputMatches;
                    Process proc = Runtime.getRuntime().exec(command);
                }

                System.out.println("===========================================================");

            /*
            //group patterns by leaf label sets
            System.out.println("finding pattern groups ...");
            String patternsInput = config.getOutputFile();
            String patternGroupOutput = "cobol_output/pattern-group-filter.txt";
            Analyse analyse = new Analyse();
            Ulti.groupPattern(patternsInput,patternGroupOutput);
            */
            }//end for
            System.out.println("finish");
            System.exit(3);



        }
        catch (Exception e){System.out.println("Error: main "+e);}
    }
}
