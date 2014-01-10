package test.rb;

import java.io.*;
import java.util.*;

public class RBTransformer {

    private static final String[] RBS = new String[]{
            "D:\\Programovanie\\git\\bantip02\\bantip2-hotel\\bantip2-server\\src\\main\\resources\\hotelResources.properties",
            "D:\\Programovanie\\git\\bantip02\\bantip2-hotel\\bantip2-web\\src\\main\\resources\\hotelwResources.properties"
    };

    private static final String[] MERGE_LOCALES = new String[]{
            "_en", "_nl"
    };

    // 1: Merge all files (UNION);
    // 2: Consider only keys from default RB;
    private static final int MODE = 1;

    public static void main(String[] args) {
        for (String rb : RBS) {
            try {
                updateResourceBundles(rb);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private static void updateResourceBundles(String defaultPath) throws Exception {
        File defaultRBFile = new File(defaultPath);
        Map<String, String> defaultRBMap = fileToMap(defaultRBFile);

        if(MODE == 1){
            mergeAllRbs( defaultRBFile, defaultRBMap);
        }else if(MODE == 2){
            updateRbsFromDefault( defaultRBFile, defaultRBMap);
        }
    }

    // merges all locale files
    private static void mergeAllRbs( File defaultRBFile, Map<String, String> defaultRBMap) throws Exception {
        // add new keys to default RB if others RB locale is in MERGE_LOCALE array
        for (File otherRBFile : listOtherRB(defaultRBFile)) {

            if( !isMergable( otherRBFile)) continue;

            Map<String, String> otherRBMap = fileToMap(otherRBFile);
            List<String> otherKeys = new ArrayList<String>(otherRBMap.keySet());

            for (String key : otherKeys) {
                if (!defaultRBMap.containsKey(key)) {
                    defaultRBMap.put(key, "??? " + key + " ???");
                    System.out.println("adding key " + key + " to default RB " + otherRBFile.getName());
                }
            }
        }

        // add all keys from default RB to other RBs if they are missing
        for (File otherRBFile : listOtherRB(defaultRBFile)) {
            Map<String, String> otherRBMap = fileToMap(otherRBFile);

            for (String key : defaultRBMap.keySet()) {
                if (!otherRBMap.containsKey(key)) {
                    otherRBMap.put(key, "??? " + key + " ???");
                    System.out.println("adding key " + key + " to other RB " + otherRBFile.getName());
                }
            }
            // save other RB
            writeMapToFile(otherRBFile, otherRBMap);
        }

        // save default RB
        writeMapToFile(defaultRBFile, defaultRBMap);
    }

    // updates all RB files from default locale
    // adds all missing keys, removes keys which are not in the default RB
    private static void updateRbsFromDefault( File defaultRBFile, Map<String, String> defaultRBMap) throws Exception {
        for (File otherRBFile : listOtherRB(defaultRBFile)) {
            Map<String, String> otherRBMap = fileToMap(otherRBFile);
            List<String> otherKeys = new ArrayList<String>(otherRBMap.keySet());

            // add all missing keys from default RB
            for (String key : defaultRBMap.keySet()) {
                if (!otherRBMap.containsKey(key)) {
                    otherRBMap.put(key, "??? " + key + " ???");
                    System.out.println("adding key " + key + " to " + otherRBFile.getName());
                }
            }

            // remove all keys from other RB, which are not present in default RB
            for (String key : otherKeys) {
                if (!defaultRBMap.containsKey(key)) {
                    otherRBMap.remove(key);
                    System.out.println("dropping key " + key + " from " + otherRBFile.getName());
                }
            }

            // save other RB
            writeMapToFile(otherRBFile, otherRBMap);
        }
    }

    // initializes map from RB file
    private static Map<String, String> fileToMap(File file) throws Exception {
        Map<String, String> map = new HashMap<String, String>();
        BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(file), "UTF-8"));
        String line;
        while ((line = reader.readLine()) != null) {
            if (line.isEmpty() || line.trim().startsWith("#"))
                continue;

            String[] split = line.split("=");
            map.put(split[0].trim(), split.length == 2 ? split[1].trim() : "");
        }

        return map;
    }

    // writes the content of map to RB file
    private static void writeMapToFile(File file, Map<String, String> map) throws Exception {
        BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file), "UTF-8"));
        List<String> keys = new ArrayList<String>(map.keySet());
        Collections.sort(keys);
        for (String key : keys) {
            writer.write(key + " = " + map.get(key));
            writer.newLine();
        }
        writer.close();
    }

    // returns list of locale files for specified default RB
    private static List<File> listOtherRB(File defaultRBFile) {
        List<File> liFile = new ArrayList<File>();
        File[] arrFile = defaultRBFile.getParentFile().listFiles();
        if (arrFile != null) {
            for (File f : arrFile) {
                boolean isLocale = f.getName().split("\\.")[0].startsWith(defaultRBFile.getName().split("\\.")[0])
                        && !f.getName().equals(defaultRBFile.getName());
                if (isLocale) {
                    liFile.add(f);
                }
            }
        }

        return liFile;
    }

    private static boolean isMergable( File file){
        for( String locale: MERGE_LOCALES){
            if( file.getName().contains( locale)){
                return true;
            }
        }

        return false;
    }


}
