package test.rb;

import java.io.*;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class RBTransformer {

    private static final String[] RBS = new String[]{
            "bantip2-server\\src\\main\\resources\\hotelResources.properties",
            "bantip2-web\\src\\main\\resources\\hotelwResources.properties"
    };

    private static final String[] MERGE_LOCALES = new String[]{
            "_en", "_nl"
    };

    private static final String ROOT = "D:\\Programovanie\\git\\bantip02\\bantip2-hotel\\";
    private static Set<String> setUsedKey = new HashSet<String>();

    private static final Pattern REPORT_PATTERN = Pattern.compile("\\$R\\{(\\S+)\\}");
    private static final Pattern JSF_PATTERN = Pattern.compile("resw\\[\'(\\S+)\'\\]");
    private static final Pattern RANDOM_KEY_PATTERN = Pattern.compile("\"\\{?((?:[A-Za-z0-9_]+\\.)+[A-Za-z0-9_]+)\\}?\"");
    private static final Pattern ENUM_CLASS_PATTERN = Pattern.compile("enum\\s+(\\S+)\\s+implements\\s+Localizable\\s*\\{(.+)\\}");
    private static final Pattern ENUM_PREFIX_PATTERN = Pattern.compile("(?:private|public|protected)?\\s+(?:static\\s+)?(?:final\\s+)?String\\s+(\\S+)\\s*=\\s*\"(\\S+)\";");
    private static final Pattern ENUM_MEMBER_PATTERN = Pattern.compile(
            "(?:^|\\{|\\}|;)(?:\\s*([A-Za-z0-9\\_]+)\\s*(?:\\(.*?\\)\\s*)?,)*+(?:\\s*([A-Za-z0-9\\_]+)\\s*(?:\\(.*?\\))?\\s*);"
    );

    // 1: Merge all files (UNION);
    // 2: Consider only keys from default RB;
    private static final int MODE = 1;

    public static void main(String[] args) {
        dropUnusedKeys();
    }

    public static void mergeResourceBundles(){
        for (String rb : RBS) {
            try {
                updateResourceBundles(ROOT + rb);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public static void dropUnusedKeys(){
        try{
            parseUsedKeys();
        }catch(Exception e){
            e.printStackTrace();
            return;
        }

        for( String fileName: RBS){
            try{
                cleanResourceBundle( ROOT + fileName);
            }catch(Exception e){
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

    private static void parseUsedKeys() throws Exception {
        parseEnums( ROOT + "bantip2-server\\src\\");
        parseEnums( ROOT + "bantip2-web\\src\\");
        parseJSFs( ROOT + "bantip2-web\\src\\main\\webapp");
        parseReports( ROOT + "bantip2-web\\src\\main\\webapp");
        System.out.println("======================");
        System.out.println("======================");
        System.out.println("======================");
    }

    private static void cleanResourceBundle( String fileName) throws Exception {
        File defaultRBFile = new File( fileName);
        Map<String, String> mapKey = fileToMap( defaultRBFile);

        Set<String> setKeyToRemove = new HashSet<String>();
        mapKey = fileToMap( defaultRBFile);

        System.out.println("==== " + defaultRBFile.getName() + " ====");
        for(String key: mapKey.keySet()){
            if( !key.startsWith("javax") && !setUsedKey.contains( key)){
                setKeyToRemove.add( key);
            }
        }

        for( String key: setKeyToRemove){
            System.out.println(key + " = " + mapKey.get( key));
            mapKey.remove( key);
        }
        writeMapToFile( defaultRBFile, mapKey);


        for( File otherRBFile : listOtherRB(defaultRBFile)){
            setKeyToRemove = new HashSet<String>();
            mapKey = fileToMap( otherRBFile);
            System.out.println("==== " + otherRBFile.getName() + " ====");

            for(String key: mapKey.keySet()){
                if( !key.startsWith("javax") && !setUsedKey.contains( key)){
                    setKeyToRemove.add( key);
                }
            }

            for( String key: setKeyToRemove){
                System.out.println(key + " = " + mapKey.get( key));
                mapKey.remove( key);
            }

            writeMapToFile( otherRBFile, mapKey);
        }
    }

    private static void parseEnums(String root) throws Exception {
        File rootF = new File(root);
        if (!rootF.isDirectory()) return;

        for (File inner : rootF.listFiles()) {
            if (inner.getName().endsWith(".java")) {
                StringBuffer buffer = new StringBuffer();
                String line;
                BufferedReader reader = new BufferedReader(new FileReader(inner));
                while ((line = reader.readLine()) != null) {
                    line = line.replaceAll("//.*", "");
                    buffer.append(line);
                }

                line = buffer.toString();
                line = line.replaceAll("/\\*.*\\*/", "");
                line = line.replaceAll("@\\S*", "");
                reader.close();

                Matcher m = RANDOM_KEY_PATTERN.matcher(line);
                while (m.find()) {
                    System.out.println( m.group(1));
                    setUsedKey.add( m.group(1).trim());
                }

                if (line.contains("implements Localizable")) {
                    int i_impl = 0;
                    while (true) {
                        i_impl = line.indexOf(" implements Localizable", i_impl + 1);
                        if (i_impl == -1) break;
                        int i_space_before_name = i_impl - 1;
                        char c;
                        while(true){
                            if( line.charAt( i_space_before_name) != ' '){
                                --i_space_before_name;
                            }else{
                                break;
                            }
                        }
                        String enumName = line.substring( i_space_before_name, i_impl).trim();
                        int i_start = line.indexOf("{", i_impl);
                        int i_end = i_start + 1;

                        int bracketCount = 0;
                        while (i_end < line.length()) {
                            c = line.charAt(i_end);
                            if (c == '}') {
                                if (bracketCount == 0)
                                    break;
                                else
                                    bracketCount--;
                            } else if (c == '{') {
                                bracketCount++;
                            }
                            i_end++;
                        }

                        String current_enum = line.substring(i_start, i_end);
                        Matcher m2 = ENUM_PREFIX_PATTERN.matcher(current_enum);
                        String prefix = "";
                        if (m2.find()) {
                            prefix = m2.group(2);
                        } else {
                            System.err.println(" no prefix found.");
                            continue;
                        }

                        m2 = ENUM_MEMBER_PATTERN.matcher(current_enum);
                        if (m2.find()) {
                            String str = m2.group(0);
                            str = str.replaceAll("\\(.+?\\)", "");
                            String[] members = str.split(",");
                            for (String member : members) {
                                member = member.replaceAll(",|;", "");
                                member = member.replaceAll("\\{", "");
                                member = member.trim();

                                if (enumName.equalsIgnoreCase("RightCodeEnum")) {
                                    setUsedKey.add(prefix + member + ".name");
                                    setUsedKey.add(prefix + member + ".desc");
                                } else if (enumName.equalsIgnoreCase("TimeSlotEnum")) {
                                    setUsedKey.add(prefix + member.toLowerCase());
                                } else {
                                    setUsedKey.add(prefix + member);
                                }
                            }
                        }
                    }
                }
            } else if (inner.isDirectory()) {
                parseEnums(inner.getAbsolutePath());
            }
        }
    }

    private static void parseReports( String root) throws Exception {
        File rootF = new File( root);
        if( !rootF.isDirectory()) return;

        for( File inner: rootF.listFiles()){
            if( inner.getName().endsWith( ".jrxml")){
                StringBuffer buffer = new StringBuffer();
                String line;
                BufferedReader reader = new BufferedReader( new FileReader( inner));
                while(( line = reader.readLine()) != null){
                    buffer.append( line);
                }

                reader.close();
                Matcher m = REPORT_PATTERN.matcher( buffer);
                while( m.find()){
                    setUsedKey.add( m.group( 1));
                }
            }else if( inner.isDirectory()){
                parseReports( inner.getAbsolutePath());
            }
        }
    }

    private static void parseJSFs(String root) throws Exception {
        File rootF = new File( root);
        if( !rootF.isDirectory()) return;

        for( File inner: rootF.listFiles()){
            if( inner.getName().endsWith( ".xhtml")){
                StringBuffer buffer = new StringBuffer();
                String line;
                BufferedReader reader = new BufferedReader( new FileReader( inner));
                while(( line = reader.readLine()) != null){
                    buffer.append( line);
                }

                reader.close();
                Matcher m = JSF_PATTERN.matcher( buffer);
                while( m.find()){
                    setUsedKey.add( m.group( 1));
                }
            }else if( inner.isDirectory()){
                parseJSFs( inner.getAbsolutePath());
            }
        }
    }


}
