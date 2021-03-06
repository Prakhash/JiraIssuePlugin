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

import net.sf.json.JSONObject;
import org.apache.log4j.Logger;
import org.zaproxy.zap.extension.api.*;

public class JiraIssueCreatorAPI extends ApiImplementor {

    private static final Logger log = Logger.getLogger(JiraIssueCreatorAPI.class);

    private JiraIssueCreatorExtension extension;

    public JiraIssueCreatorAPI(JiraIssueCreatorExtension extension) {
        super();
        this.extension = extension;

        //Creating a new API action
        this.addApiAction(new ApiAction(IssueCreatorConstants.ACTION_CREATE_JIRA_ISSUE,
                new String[] { IssueCreatorConstants.ACTION_PARAM_BASEURL,
                        IssueCreatorConstants.ACTION_PARAM_JIRAUSERNAME,
                        IssueCreatorConstants.ACTION_PARAM_JIRAPASSWORD, IssueCreatorConstants.ACTION_PARAM_PROJECT_KEY,
                        IssueCreatorConstants.ACTION_PARAM_ASSIGNEE, IssueCreatorConstants.ACTION_PARAM_PRODUCT,
                        IssueCreatorConstants.ACTION_PARAM_LABEL, IssueCreatorConstants.ACTION_PARAM_PATH,
                        IssueCreatorConstants.ACTION_PARAM_WORKSPACE, IssueCreatorConstants.ACTION_PARAM_FOLDER }));
    }

    @Override public String getPrefix() {
        log.info("This method is called");
        return IssueCreatorConstants.PREFIX;
    }

    @Override public ApiResponse handleApiAction(String name, JSONObject params) throws ApiException {

        if (log.isDebugEnabled()) {
            log.debug("Request for handleApiAction: " + name + " (params: " + params.toString() + ")");
        }

        switch (name) {
        case IssueCreatorConstants.ACTION_CREATE_JIRA_ISSUE:
            extension.createJiraIssues(params.getString(IssueCreatorConstants.ACTION_PARAM_BASEURL),
                    params.getString(IssueCreatorConstants.ACTION_PARAM_JIRAUSERNAME),
                    params.getString(IssueCreatorConstants.ACTION_PARAM_JIRAPASSWORD),
                    params.getString(IssueCreatorConstants.ACTION_PARAM_PROJECT_KEY),
                    params.getString(IssueCreatorConstants.ACTION_PARAM_ASSIGNEE),
                    params.getString(IssueCreatorConstants.ACTION_PARAM_PRODUCT),
                    params.getString(IssueCreatorConstants.ACTION_PARAM_LABEL),
                    params.getString(IssueCreatorConstants.ACTION_PARAM_PATH),
                    params.getString(IssueCreatorConstants.ACTION_PARAM_WORKSPACE),
                    params.getString(IssueCreatorConstants.ACTION_PARAM_FOLDER));
        }
        return new ApiResponseElement(name, params.toString());
    }
}
