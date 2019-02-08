package be.intimals.freqt.config;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

public class Config {
    private String path;
    private Properties prop;

    public Config(String configPath) throws IOException {
        path = configPath;
        prop = new Properties();
        InputStream input = new FileInputStream(configPath);
        prop.load(input);
        input.close();
    }


    public int getConfigID(){
        return Integer.valueOf(prop.getProperty("configID"));
    }

    public int getTimeout(){return Integer.valueOf(prop.getProperty("timeout"));}

    public Properties getProp(){
        return this.prop;
    }


    public boolean buildGrammar() {
        return Boolean.valueOf(prop.getProperty("buildGrammar"));
    }

    public String getGrammarFile() {
        return prop.getProperty("grammarFile");
    }

    public String getRootLabelFile() {
        return prop.getProperty("rootLabelFile");
    }

    public String getWhiteLabelFile() {
        return prop.getProperty("whiteLabelFile");
    }

    public String getXmlCharacterFile() {
        return prop.getProperty("xmlCharacterFile");
    }

    public String getInputFiles() {
        return prop.getProperty("inFiles");
    }

    public String getOutputFile() {
        return prop.getProperty("outFile");
    }

    public int getMinSupport() {
        return Integer.valueOf(prop.getProperty("minSupport"));
    }

    public int getMinPatternSize() {
        return Integer.valueOf(prop.getProperty("minPatternSize"));
    }

    public int getMaxPatternSize() {
        return Integer.valueOf(prop.getProperty("maxPatternSize"));
    }

    public int getMinLeaf() {
        return Integer.valueOf(prop.getProperty("minLeafSize"));
    }

    public int getMaxLeaf() {
        return Integer.valueOf(prop.getProperty("maxLeafSize"));
    }

    public boolean addLocations() {
        return Boolean.valueOf(prop.getProperty("where"));
    }

    public boolean outputAsXML() {
        return Boolean.valueOf(prop.getProperty("xml"));
    }

    public boolean outputAsENC() {
        return Boolean.valueOf(prop.getProperty("enc"));
    }

    public boolean postProcess(){return Boolean.valueOf(prop.getProperty("post"));}

    public  int getMaxRepeatLabel(){
        return Integer.valueOf(prop.getProperty("maxRepeatLabel"));
    }

    public  int getMinLineDistance(){
        return Integer.valueOf(prop.getProperty("minLineDistance"));
    }

    public  int getMaxLineDistance(){
        return Integer.valueOf(prop.getProperty("maxLineDistance"));
    }


    /**
     * Returns a list of minimum-support values (only used when executing multiple Freq-T runs in parallel)
     * @return
     */
    public List<Integer> getMinSupportList() {
        String msList = prop.getProperty("minSupportList");
        List<Integer> result = new ArrayList<>();
        for(String ms: msList.split(",")) {
            result.add(Integer.valueOf(ms));
        }
        return result;
    }

    /**
     * Returns a list of input folders (only used when executing multiple Freq-T runs in parallel)
     * @return
     */
    public List<String> getInputFilesList() {
        String ifList = prop.getProperty("inFilesList");
        return Arrays.asList(ifList.split(","));
    }

}

