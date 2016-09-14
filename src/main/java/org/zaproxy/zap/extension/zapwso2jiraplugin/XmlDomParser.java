package org.zaproxy.zap.extension.zapwso2jiraplugin;/*
 *
 *  * Copyright (c) 2016, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *  *
 *  * WSO2 Inc. licenses this file to you under the Apache License,
 *  * Version 2.0 (the "License"); you may not use this file except
 *  * in compliance with the License.
 *  * You may obtain a copy of the License at
 *  *
 *  * http://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  * Unless required by applicable law or agreed to in writing,
 *  * software distributed under the License is distributed on an
 *  * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  * KIND, either express or implied.  See the License for the
 *  * specific language governing permissions and limitations
 *  * under the License.
 *
 */

import org.apache.log4j.Logger;
import org.apache.xerces.util.SecurityManager;
import org.json.JSONObject;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;

import javax.naming.AuthenticationException;
import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class XmlDomParser {

    String createIssueData, type;
    String description = "";
    JiraRestClient jiraRest;

    private static final Logger log = Logger.getRootLogger();

    /**
     * Checking if there is any issue reported during the scan
     *
     * @return return TRUE if any issues are reported in the report
     */
    public boolean isIssueExistsInReport() {
        try {

            StringBuilder currentSession = new StringBuilder();
            String report = LastScanReport.getInstance().generateReport(currentSession);
            InputStream stream = new ByteArrayInputStream(report.getBytes(StandardCharsets.UTF_8));
            DocumentBuilderFactory dbFactory = getSecuredDocumentBuilderFactory();

            DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
            Document doc = dBuilder.parse(stream);
            doc.getDocumentElement().normalize();

            NodeList session = doc.getElementsByTagName("alerts"); //to check wheter alerts exist

            if (session.getLength() != 0) {
                NodeList alertList = doc.getElementsByTagName("alertitem"); //alert items

                if (alertList.getLength() > 0)
                    return true;
            }
        } catch (Exception e) {
            log.info("Exception occured when generating the report from scan");
        }

        return false;
    }

    /**
     * Creating new Jira issue Json object String is generated from here
     *
     * @param projectKey JIRA project key, under which ticket needs to be created
     * @param assignee   to whom ticket need to be assigned
     * @param issueLabel custom object which used to identify the project
     * @param summary    JIRA heading
     * @param product    product name
     * @return Json String that needs to be sent to the JIRA
     */
    public String createNewTicket(String projectKey, String assignee, String issueLabel, String summary,
            String product) {

        description = product;
        type = "Bug";

        createIssueData = "{\"fields\": {\"project\": {\"key\":\"" + projectKey + "\"}," +
                "\"summary\":" + "\"" + summary + "\"" + ",  \"assignee\": {\"name\": \"" + assignee + "\"},"
                + "\"customfield_10464\": [{\"value\": \"" + issueLabel + "\"}]," +
                "\"description\":" + "\"" + description + "\"" + "," +
                "\"issuetype\":{\"name\":\"" + type + "\"}}}";

        return createIssueData;
    }

    /**
     * Generating the comment that has to be added with the attachment
     *
     * @return comment that needs to be added when uploading a file
     */
    public String createComment() {

        String comment;
        DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
        Date date = new Date();
        comment = "{\"body\": \"This Report is generated on " + dateFormat.format(date).toString() + ".\"}";

        return comment;
    }

    /**
     * Checking if there is any issues already reported in the Jira with the samw topic
     *
     * @param auth     Base64 encoded authorization paramters
     * @param BASE_URL Jira base URL
     * @param summary  JIRA heading which used to find the JIRA existance
     * @return returning the JIRA key, if already jira is created else return an empty String
     */

    public String checkForIssueExistence(String auth, String BASE_URL, String summary, String projectKey) {

        String responseIssuue;
        String key = "";
        JSONObject availableIssue = null;
        summary = summary.replace("][", "+");
        summary = summary.replace("[", "");
        summary = summary.replace("]", "");

        try {
            //            String URL = BASE_URL + "/rest/api/2/search?jql=summary%20~%20%20%22" + summary + "%22" + "&fields="+"SECINTDEV";

            String URL = BASE_URL + "/rest/api/2/search?jql=project+%3d+" + projectKey + "+AND+text+%7e+%22" + summary
                    + "%22" + "&fields=" + "";
            responseIssuue = jiraRest.invokeGetMethod(auth, URL);
            availableIssue = new JSONObject(responseIssuue);
        } catch (AuthenticationException e) {
            log.error("Auth failiur", e);
        }

        try {
            if (availableIssue.getJSONArray("issues").length() != 0)
                key = availableIssue.getJSONArray("issues").getJSONObject(0).getString("key");
        } catch (org.json.JSONException e) {
            log.error("Key is not already available. Creating the Jira");
        }
        return key;
    }

    /**
     * Compressing the file generatated during the zap scan
     *
     * @param path file path in the server
     * @return new file path for the compressed file
     */
    public String compressFile(String path) {

        byte[] buffer = new byte[1024];

        FileOutputStream fos = null;
        ZipOutputStream zos = null;
        FileInputStream in = null;

        try {
            fos = new FileOutputStream(path.concat(".zip"));
            zos = new ZipOutputStream(fos);

            String[] directories = path.split("/");
            ZipEntry ze = new ZipEntry(directories[directories.length - 1]);
            zos.putNextEntry(ze);
            in = new FileInputStream(path);

            int len;
            while ((len = in.read(buffer)) > 0) {
                zos.write(buffer, 0, len);
            }

            log.info("ZIP file is successfully created");

        } catch (FileNotFoundException e) {
            log.info("File need to be compressed is not found ", e);
        } catch (IOException e) {
            log.info("Exception occured during the file compression", e);
        } finally {
            try {
                in.close();
                zos.closeEntry();
                zos.close();
            } catch (IOException e) {
                log.error("Error while closing the Stream");
            }
        }

        return path.concat(".zip");
    }

    /**
     * Renaming the file according to the allowed format in jira
     *
     * @param product  product that get scanned
     * @param filePath file path of the report generated
     * @return filepath after renaming the file
     */
    public String renameFile(String product, String filePath) {

        DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        String fileNewPath="";
        Date date = new Date();
        String newFileName = product + "-" + dateFormat.format(date).toString().substring(0, 10);
        log.info(newFileName);
        String[] filePathDirectories = filePath.split("/");

        // File (or directory) with old name
        File file = new File(filePath);


        if(filePath.contains(".xml"))
        fileNewPath = filePath
                .replace(filePathDirectories[filePathDirectories.length - 1], newFileName.concat(".xml"));
        else if(filePath.contains(".html"))
            fileNewPath = filePath
                    .replace(filePathDirectories[filePathDirectories.length - 1], newFileName.concat(".html"));

        // File (or directory) with new name
        File file2 = new File(fileNewPath);

        // Rename file (or directory)
        boolean success = file.renameTo(file2);

        return fileNewPath;
    }

    /**
     * Create DocumentBuilderFactory with the XXE and XEE prevention measurements.
     *
     * @return DocumentBuilderFactory instance
     */
    public static DocumentBuilderFactory getSecuredDocumentBuilderFactory() {

        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        dbf.setNamespaceAware(true);
        dbf.setXIncludeAware(false);
        dbf.setExpandEntityReferences(false);

        try {
            dbf.setFeature(
                    IssueCreatorConstants.SAX_FEATURE_PREFIX + IssueCreatorConstants.EXTERNAL_GENERAL_ENTITIES_FEATURE,
                    false);
            dbf.setFeature(IssueCreatorConstants.SAX_FEATURE_PREFIX
                    + IssueCreatorConstants.EXTERNAL_PARAMETER_ENTITIES_FEATURE, false);
            dbf.setFeature(
                    IssueCreatorConstants.XERCES_FEATURE_PREFIX + IssueCreatorConstants.LOAD_EXTERNAL_DTD_FEATURE,
                    false);
            dbf.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);

        } catch (ParserConfigurationException e) {
            log.error("Failed to load XML Processor Feature " + IssueCreatorConstants.EXTERNAL_GENERAL_ENTITIES_FEATURE
                    + " or " +
                    IssueCreatorConstants.EXTERNAL_PARAMETER_ENTITIES_FEATURE + " or "
                    + IssueCreatorConstants.LOAD_EXTERNAL_DTD_FEATURE +
                    " or secure-processing.");
        }

        SecurityManager securityManager = new SecurityManager();
        securityManager.setEntityExpansionLimit(IssueCreatorConstants.ENTITY_EXPANSION_LIMIT);
        dbf.setAttribute(IssueCreatorConstants.XERCES_PROPERTY_PREFIX + IssueCreatorConstants.SECURITY_MANAGER_PROPERTY,
                securityManager);

        return dbf;
    }

}