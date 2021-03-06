package apache.conf.parser;

import apache.conf.directives.Define;
import apache.conf.global.Const;
import apache.conf.global.Utils;
import apache.conf.modules.Module;
import apache.conf.modules.SharedModule;
import apache.conf.modules.StaticModule;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Stack;
import java.util.regex.Pattern;

/**
 * 
 * This class is used to provide generic parser functionality for the Apache configuration.
 *
 */
public class Parser {

    protected String rootConfFile;
    protected String serverRoot;
    protected StaticModule staticModules[];
    protected SharedModule sharedModules[];

    /**
     * @param rootConfFile
     *            the Apache root configuration file.
     * @param serverRoot
     *            the Apache server root
     * @param staticModules
     * @param sharedModules
     * @throws Exception
     *             if the rootConfFile or serverRoot do not exist
     */
    public Parser(String rootConfFile, String serverRoot, StaticModule staticModules[], SharedModule sharedModules[]) throws Exception {
        if (!new File(rootConfFile).exists()) {
            throw new Exception("The root configuration file does not exist");
        }

        if (!new File(serverRoot).exists()) {
            throw new Exception("The server root does not exist");
        }

        this.rootConfFile = rootConfFile;
        this.serverRoot = serverRoot;
        this.staticModules = staticModules;
        this.sharedModules = sharedModules;
    }

    /**
     * Utility to check if a line matches an Apache comment.
     * 
     * @param line
     *            the line to check for a comment.
     * @return a boolean indicating if the line is a comment.
     */
    public static boolean isCommentMatch(String line) {
        Pattern commentPattern = Pattern.compile("^\\s*#");
        return commentPattern.matcher(line).find();
    }

    /**
     * Utility to check if a line matches a directive type.
     * 
     * @param line
     *            the line to check for the directive type.
     * @param directiveType
     *            the type of the directive to match against. This is not case sensitive.
     * @return a boolean indicating if the line mathes the directiveType
     */
    public static boolean isDirectiveMatch(String line, String directiveType) {
        Pattern directivePattern = Pattern.compile("^\\s*\\b" + directiveType + "\\b\\s+", Pattern.CASE_INSENSITIVE);
        return directivePattern.matcher(line).find();
    }

    /**
     * Utility used to check if a line matches a VirtualHost <br/>
     * <br/>
     * Example :<br/>
     * &lt;VirtualHost *:80&gt;
     * 
     * @param line
     *            the line to match against
     * @return a boolean indicating if the line matches a VirtualHost
     */
    public static boolean isVHostMatch(String line) {
        Pattern virtualHostPattern = Pattern.compile("<\\s*\\bVirtualHost\\b.*>", Pattern.CASE_INSENSITIVE);
        return virtualHostPattern.matcher(line).find();
    }

    /**
     * Utility used to check if a line matches a VirtualHost Close declaration<br/>
     * <br/>
     * Example :<br/>
     * &lt;/VirtualHost&gt;
     * 
     * @param line
     *            the line to match against
     * @return a boolean indicating if the line matches a VirtualHost Close declaration.
     */
    public static boolean isVHostCloseMatch(String line) {
        Pattern virtualHostClosePattern = Pattern.compile("</.*\\bVirtualHost\\b.*>", Pattern.CASE_INSENSITIVE);
        return virtualHostClosePattern.matcher(line).find();
    }

    /**
     * Utility used to check if a line matches an IfModule Open Negation <br/>
     * <br/>
     * Example :<br/>
     * &lt;IfModule !mpm_netware_module&gt;
     * 
     * @param line
     *            the line to match against
     * @return a boolean indicating if the line matches an IfModule Open Negation
     */
    public static boolean isIfModuleOpenNegateMatch(String line) {
        Pattern ifModuleOpenNegatePattern = Pattern.compile("<\\s*\\bifmodule\\b\\s+!.*>", Pattern.CASE_INSENSITIVE);
        return ifModuleOpenNegatePattern.matcher(line).find();
    }

    /**
     * Utility used to check if a line matches an IfModule Open Declaration<br/>
     * <br/>
     * Example :<br/>
     * &lt;IfModule status_module&gt;
     * 
     * @param line
     *            the line to match against
     * @return a boolean indicating if the line matches an IfModule Open Declaration
     */
    public static boolean isIfModuleOpenMatch(String line) {
        Pattern ifModuleOpenPattern = Pattern.compile("<\\s*\\bifmodule\\b.*>", Pattern.CASE_INSENSITIVE);
        return ifModuleOpenPattern.matcher(line).find();
    }

    /**
     * Utility used to check if a line matches an IfModule Close declaration<br/>
     * <br/>
     * Example :<br/>
     * &lt;/ifmodule&gt;
     * 
     * @param line
     *            the line to match against
     * @return a boolean indicating if the line matches an IfModule Close declaration.
     */
    public static boolean isIfModuleCloseMatch(String line) {
        Pattern ifModuleClosePattern = Pattern.compile("</\\s*\\bifmodule\\b\\s*>", Pattern.CASE_INSENSITIVE);
        return ifModuleClosePattern.matcher(line).find();
    }

    /**
     * Utility used to check if a line matches an enclosure with a specified type.
     * 
     * @param line
     *            the line to match against the enclosure.
     * @param enclosureType
     *            the name of the enclosure to match against. This is not case sensitive.
     * @return a boolean indicating if the line matches the enclosure.
     */
    public static boolean isEnclosureTypeMatch(String line, String enclosureType) {
        Pattern enclosurePattern = Pattern.compile("<\\s*\\b" + enclosureType + "\\b.*>", Pattern.CASE_INSENSITIVE);
        return enclosurePattern.matcher(line).find();
    }

    /**
     * Utility used to check if a line matches a closing enclosure with a specified type.
     * 
     * @param line
     *            the line to match against the closing enclosure.
     * @param enclosureType
     *            the name of the enclosure to match against. This is not case sensitive.
     * @return a boolean indicating if the line matches the closing enclosure type.
     */
    public static boolean isCloseEnclosureTypeMatch(String line, String enclosureType) {
        Pattern closeEnclosurePattern = Pattern.compile("</\\s*\\b" + enclosureType + "\\b\\s*>", Pattern.CASE_INSENSITIVE);
        return closeEnclosurePattern.matcher(line).find();
    }

    /**
     * Utility used to check if a line matches an enclosure format.
     * 
     * @param line
     *            the line to match against the enclosure.
     * @return a boolean indicating if the line matches the enclosure format.
     */
    public static boolean isEnclosureMatch(String line) {
        Pattern enclosurePattern = Pattern.compile("<\\s*[^/].*>", Pattern.CASE_INSENSITIVE);
        return enclosurePattern.matcher(line).find();
    }

    /**
     * Utility used to check if a line matches a closing enclosure.
     * 
     * @param line
     *            the line to match against the closing enclosure.
     * @return a boolean indicating if the line matches a closing enclosure format.
     */
    public static boolean isCloseEnclosureMatch(String line) {
        Pattern closeEnclosurePattern = Pattern.compile("</.*>", Pattern.CASE_INSENSITIVE);
        return closeEnclosurePattern.matcher(line).find();
    }

    /**
     * Utility used to check if a line matches an Include directive
     * 
     * @param line
     *            the line to match against the Include directive.
     * @return a boolean indicating if the line matches an Include directive.
     */
    public static boolean isIncludeMatch(String line) {
        Pattern includePattern = Pattern.compile("^\\s*\\b(Include|IncludeOptional)\\b", Pattern.CASE_INSENSITIVE);
        return includePattern.matcher(line).find();
    }

    protected String getFileFromInclude(String line) {
        return line.replaceAll("(?i)\\b(Include|IncludeOptional)\\b\\s+", "").replaceAll("\"", "");
    }

    /**
     * Checks if a line has a valid ifmodule that does not belong to a loaded apache module<br/>
     * 
     * <br/>
     * Example:<br/> 
     * &lt;IfModule !mpm_winnt_module&gt; or &lt;IfModule !mod_ssl.c&gt;
     * 
     * @param line
     *            the line to match against
     * @param modules
     *            list of modules to compare against
     * @return true if the line matches a negate module
     */
    public static boolean isInNegateModules(String line, Module modules[]) {
        for (Module module : modules) {
            if (module.getName().replaceAll("_module", "")
                    .equals(line.replaceAll("(?i)<\\s*\\bifmodule\\b\\s*!mod_", "").replaceAll("\\.c\\s*>", "").replaceAll("(?i)<\\s*\\bifmodule\\b\\s*!", "").replaceAll("_module\\s*>", ""))) {
                return true;
            }
        }

        return false;
    }

    /**
     * Checks if a line has a valid ifmodule that belongs to a loaded apache module<br/>
     * 
     * <br/>
     * Example:<br/>
     * &lt;IfModule mpm_winnt_module&gt; or &lt;IfModule mod_ssl.c&gt;
     * 
     * @param line
     *            the line to match against
     * @param modules
     *            list of modules to compare against
     * @return true if the line matches module
     */
    public static boolean isInModules(String line, Module modules[]) {
        for (Module module : modules) {
            if (module.getName().replaceAll("_module", "")
                    .equals(line.replaceAll("(?i)<\\s*\\bifmodule\\b\\s*mod_", "").replaceAll("\\.c\\s*>", "").replaceAll("(?i)<\\s*\\bifmodule\\b\\s*", "").replaceAll("_module\\s*>", ""))) {
                return true;
            }
        }

        return false;
    }

    private String processConfigurationLine(String line, Define defines[]) {
              
        String processedLine = line.replaceAll("\\s+\\\\\\s*" + Const.newLine, " "); 
        
        processedLine = Define.replaceDefinesInString(defines, Utils.sanitizeLineSpaces(processedLine));
        
        return processedLine;
    }
    
    private ConfigurationLine[] getConfigurationLines(String confFile, boolean loadDefines) throws Exception {

        Define defines[];
        if (loadDefines) {
            defines = Define.getAllDefine(new DirectiveParser(rootConfFile, serverRoot, staticModules, sharedModules));
        } else {
            defines = new Define[0];
        }

        ArrayList<ConfigurationLine> configurationLines = new ArrayList<ConfigurationLine>();

        getConfigurationLines(defines, confFile, configurationLines);

        return configurationLines.toArray(new ConfigurationLine[configurationLines.size()]);
    }

    private void getConfigurationLines(Define defines[], String confFile, ArrayList<ConfigurationLine> configurationLines) throws Exception {

        BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(confFile), "UTF-8"));

        try {

            String strLine, cmpLine, concatLine = "";
            Stack ifModuleStack = new Stack();
            
            int lineNumInFile = 0, currentConcatLineNum = -1;
            while ((strLine = br.readLine()) != null) {

                lineNumInFile++;

                currentConcatLineNum = (currentConcatLineNum == -1 ? lineNumInFile : currentConcatLineNum);
                concatLine += strLine;
                
                // Multiline configuration line
                if(strLine.trim().endsWith("\\")) {
                    concatLine += Const.newLine;
                    continue;
                } 
                
                cmpLine = processConfigurationLine(concatLine, defines);
                
                boolean isComment = isCommentMatch(cmpLine);
                configurationLines.add(new ConfigurationLine(concatLine, cmpLine, confFile, isComment, currentConcatLineNum, lineNumInFile));

                concatLine = "";
                currentConcatLineNum = -1;
                
                if (!isComment) {
                                       
                    if (isIfModuleOpenNegateMatch(cmpLine)) {
                        if (ifModuleStack.isEmpty()) {
                            if (isInNegateModules(cmpLine, staticModules) || isInNegateModules(cmpLine, sharedModules)) {
                                ifModuleStack.push(cmpLine);
                            }
                        } else {
                            // we have found a nested iFModule iterate the counter
                            ifModuleStack.push(cmpLine);
                        }
                    } else if (isIfModuleOpenMatch(cmpLine)) {
                        // Check if were already in a module that isn't loaded
                        if (ifModuleStack.isEmpty()) {
                            if (!isInModules(cmpLine, staticModules) && !isInModules(cmpLine, sharedModules)) {
                                ifModuleStack.push(cmpLine);
                            }
                        } else {
                            // we have found a nested iFModule iterate the counter
                            ifModuleStack.push(cmpLine);
                        }
                    }

                    if (!ifModuleStack.isEmpty()) {
                        if (isIfModuleCloseMatch(cmpLine)) {
                            ifModuleStack.pop();
                        }

                    } else if (isIncludeMatch(cmpLine)) {
    
                        String file = getFileFromInclude(cmpLine);
    
                        // if the filename starts with it is an absolute path,
                        // otherwise its a relative path
                        File check;
                        if (file.startsWith("/") || (file.contains(":"))) {
                            check = new File(file);
                        } else {
                            check = new File(serverRoot, file);
                        }
    
                        // check if its a directory, if it is we must include all
                        // files in the directory
                        if (check.isDirectory()) {
                            String children[] = check.list();
    
                            Arrays.sort(children);
    
                            File refFile;
                            for (String child : children) {
                                refFile = new File(check.getAbsolutePath(), child);
                                if (!refFile.isDirectory()) {
                                    getConfigurationLines(defines, refFile.getAbsolutePath(), configurationLines);
                                }
                            }
                        } else {
                            // check if its wild card here
                            if (file.contains("*")) {
                                File parent = new File(check.getParentFile());
                                String children[] = parent.list();
    
                                Arrays.sort(children);
    
                                File refFile;
                                for (String child : children) {
                                    refFile = new File(parent.getAbsolutePath(), child);
                                    if (!refFile.isDirectory() && refFile.getName().matches(check.getName().replaceAll("\\.", "\\.").replaceAll("\\*", ".*"))) {
                                        getConfigurationLines(defines, refFile.getAbsolutePath(), configurationLines);
                                    }
                                }
                            } else {
                                getConfigurationLines(defines, check.getAbsolutePath(), configurationLines);
                            }
                        }
                    }
                }

            }
        } finally {
            br.close();
        }

    }

    protected ParsableLine[] getParsableLines(ConfigurationLine[] configurationLines, boolean includeVHosts) throws Exception {

        ArrayList<ParsableLine> lines = new ArrayList<ParsableLine>();
        Stack ifModuleStack = new Stack();
        Stack virtualHostStack = new Stack();

        String cmpLine;
        boolean isComment;
        for (ConfigurationLine configurationLine : configurationLines) {
            cmpLine = configurationLine.getProcessedLine();
            isComment = configurationLine.isComment();
            
            /**
             * Parse IfModule statements to see if we should add the directives
             * 
             * Two types of IfModules <IfModule mpm_prefork_module> <IfModule mod_ssl.c>
             * 
             */
            if (!isComment) {

                if (isIfModuleOpenNegateMatch(cmpLine)) {
                    if (ifModuleStack.isEmpty()) {
                        if (isInNegateModules(cmpLine, staticModules) || isInNegateModules(cmpLine, sharedModules)) {
                            ifModuleStack.push(cmpLine);
                        }
                    } else {
                        // we have found a nested iFModule iterate the counter
                        ifModuleStack.push(cmpLine);
                    }
                } else if (isIfModuleOpenMatch(cmpLine)) {
                    // Check if were already in a module that isn't loaded
                    if (ifModuleStack.isEmpty()) {
                        if (!isInModules(cmpLine, staticModules) && !isInModules(cmpLine, sharedModules)) {
                            ifModuleStack.push(cmpLine);
                        }
                    } else {
                        // we have found a nested iFModule iterate the counter
                        ifModuleStack.push(cmpLine);
                    }
                }

                /**
                 * Parse VirtualHost statements to see if we should add the directives
                 * 
                 * Example VirtualHost <VirtualHost *:80>
                 * 
                 */
                if (!includeVHosts && isVHostMatch(cmpLine)) {
                    virtualHostStack.push(cmpLine);
                }
            }

            if (!ifModuleStack.isEmpty()) {
                if (!isComment && isIfModuleCloseMatch(cmpLine)) {
                    ifModuleStack.pop();
                }

                lines.add(new ParsableLine(configurationLine, false));
            } else if (!virtualHostStack.isEmpty()) {
                if (!isComment && isVHostCloseMatch(cmpLine)) {
                    virtualHostStack.pop();
                }

                lines.add(new ParsableLine(configurationLine, false));
            } else {
                lines.add(new ParsableLine(configurationLine, true));
            }
        }

        return lines.toArray(new ParsableLine[lines.size()]);
    }

    /**
     * Gets a list of all parsable lines in the configuration. The lines will be included in the order that they appear in the Apache configuration.
     * 
     * @param includeVHosts
     *            boolean indicating whether to include parsable lines in Virtual Hosts
     * 
     * @return a list of parsable lines
     * 
     **/
    public ParsableLine[] getConfigurationParsableLines(boolean includeVHosts) throws IOException, Exception {
        return getConfigurationParsableLines(true, includeVHosts);
    }

    protected ParsableLine[] getConfigurationParsableLines(boolean loadDefines, boolean includeVHosts) throws IOException, Exception {
        return getParsableLines(getConfigurationLines(rootConfFile, loadDefines), includeVHosts);
    }

    /**
     * Gets a list of all parsable lines in a file. The lines will be included in the order that they appear in the Apache configuration.
     * 
     * @param includeVHosts
     *            boolean indicating whether to include parsable lines in Virtual Hosts
     * 
     * @return a list of parsable lines
     * 
     **/
    public ParsableLine[] getFileParsableLines(String file, boolean includeVHosts) throws IOException, Exception {
        return getFileParsableLines(file, true, includeVHosts);
    }

    protected ParsableLine[] getFileParsableLines(String file, boolean loadDefines, boolean includeVHosts) throws IOException, Exception {

        ArrayList<ConfigurationLine> fileConfigurationLines = new ArrayList<ConfigurationLine>();

        File currentFile = new File(file);

        // filter any lines that dont belong to this file
        ConfigurationLine configurationLines[] = getConfigurationLines(file, loadDefines);
        for (ConfigurationLine configurationLine : configurationLines) {
            if (currentFile.getAbsolutePath().equals(new File(configurationLine.getFile()).getAbsolutePath())) {
                fileConfigurationLines.add(configurationLine);
            }
        }

        return getParsableLines(fileConfigurationLines.toArray(new ConfigurationLine[fileConfigurationLines.size()]), includeVHosts);
    }

    /**
     *
     * Gets the active file list as it appears in the configuration.
     * If the user includes a configuration file more than once then it will be added multiple times to the list.
     *
     * @return an array with all included configuration files. The list of files is in the order that they appear in the apache configuration.
     * @throws Exception
     */
    public String[] getActiveConfFileListWithDuplicates() throws Exception {

        ParsableLine lines[] = getConfigurationParsableLines(true);

        ArrayList<String> files = new ArrayList<String>();

        ConfigurationLine configurationLine;
        for (ParsableLine line : lines) {
            if (line.isInclude()) {
                configurationLine = line.getConfigurationLine();
                if (configurationLine.getLineOfStart() == 1) {
                    files.add(configurationLine.getFile());
                }
            }
        }

        return files.toArray(new String[files.size()]);

    }

    /**
     * Gets a unique list of configuration files currently included in the apache configuration. 
     * 
     * @return an array with all included configuration files. The list of files is in the order that they appear in the apache configuration.
     * @throws Exception
     */
    public String[] getActiveConfFileList() throws Exception {

        String activeFiles[] = getActiveConfFileListWithDuplicates();

        ArrayList<String> files = new ArrayList<String>();
        for(String activeFile : activeFiles) {
            if(!files.contains(activeFile)) {
                files.add(activeFile);
            }
        }

        return files.toArray(new String[files.size()]);
    }

}
