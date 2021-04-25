/*
 Copyright (c) 42Crunch Ltd. All rights reserved.
 Licensed under the GNU Affero General Public License version 3. See LICENSE.txt in the project root for license information.
*/

package com.xliic.cicd.audit;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

import com.xliic.cicd.audit.config.model.FailOn;
import com.xliic.cicd.audit.config.model.Severity;
import com.xliic.cicd.audit.model.assessment.AssessmentReport;
import com.xliic.cicd.audit.model.assessment.AssessmentResponse;
import com.xliic.cicd.audit.model.assessment.AssessmentReport.Issue;
import com.xliic.cicd.audit.model.assessment.AssessmentReport.Issues;
import com.xliic.cicd.audit.model.assessment.AssessmentReport.Section;

public class FailureChecker {

    private final HashMap<String, Integer> names = new HashMap<String, Integer>();

    public FailureChecker() {
        names.put("critical", 5);
        names.put("high", 4);
        names.put("medium", 3);
        names.put("low", 2);
        names.put("info", 1);
    }

    public ArrayList<String> checkAssessment(AssessmentResponse assessment, AssessmentReport report, FailOn failOn) {
        ArrayList<String> failures = new ArrayList<String>();

        failures.addAll(checkMinScore(assessment, failOn));
        failures.addAll(checkCategoryScore(report, failOn));
        failures.addAll(checkInvalidContract(report, failOn));
        failures.addAll(checkSeverity(report, failOn));
        failures.addAll(checkIssueId(report, failOn));

        return failures;
    }

    @edu.umd.cs.findbugs.annotations.SuppressFBWarnings("NP_UNWRITTEN_PUBLIC_OR_PROTECTED_FIELD")
    private ArrayList<String> checkMinScore(AssessmentResponse assessment, FailOn conditions) {
        ArrayList<String> failures = new ArrayList<String>();

        Integer overallScore = conditions.getScore().getOverall();
        int score = Math.round(assessment.attr.data.grade);
        if (overallScore != null && score < overallScore) {
            failures.add(
                    String.format("The API score %d is lower than the set minimum score of %d", score, overallScore));
        }

        return failures;
    }

    private ArrayList<String> checkCategoryScore(AssessmentReport report, FailOn conditions) {
        ArrayList<String> failures = new ArrayList<String>();

        Integer dataScore = conditions.getScore().getData();
        if (dataScore != null && getScore(report.data) < dataScore) {
            failures.add(String.format("The API data score %d is lower than the set minimum score of %d",
                    getScore(report.data), dataScore));
        }

        Integer securityScore = conditions.getScore().getSecurity();
        if (securityScore != null && getScore(report.security) < securityScore.intValue()) {
            failures.add(String.format("The API security score %d is lower than the set minimum score of %d",
                    getScore(report.security), securityScore));
        }

        return failures;
    }

    private ArrayList<String> checkInvalidContract(AssessmentReport report, FailOn conditions) {
        ArrayList<String> failures = new ArrayList<String>();

        boolean denyInvalidContract = conditions.getInvalidContract() == null
                || conditions.getInvalidContract().booleanValue();

        if (denyInvalidContract && !report.openapiState.equals("valid")) {
            failures.add("The OpenAPI definition is not valid");
        }

        return failures;
    }

    private ArrayList<String> checkIssueId(AssessmentReport report, FailOn conditions) {
        ArrayList<String> failures = new ArrayList<String>();
        if (conditions.getIssueId() != null) {

            HashSet<String> reportIssueIds = new HashSet<String>();
            if (report.data != null && report.data.issues != null) {
                reportIssueIds.addAll(report.data.issues.keySet());
            }
            if (report.security != null && report.security.issues != null) {
                reportIssueIds.addAll(report.security.issues.keySet());
            }

            for (String id : conditions.getIssueId()) {
                for (String reportId : reportIssueIds) {
                    if (reportId.matches(id)) {
                        failures.add(String.format("Found issue \"%s\"", reportId));
                    }
                }

            }
        }

        return failures;
    }

    private ArrayList<String> checkSeverity(AssessmentReport report, FailOn conditions) {
        ArrayList<String> failures = new ArrayList<String>();
        Severity severity = conditions.getSeverity();

        if (severity != null) {
            String dataSeverity = severity.getData();
            if (dataSeverity != null && report.data != null && report.data.issues != null) {
                int found = findBySeverity(report.data.issues, dataSeverity);
                if (found > 0) {
                    failures.add(String.format("Found %d issues in category \"data\" with severity \"%s\" or higher",
                            found, dataSeverity));
                }
            }

            String securitySeverity = severity.getSecurity();
            if (securitySeverity != null && report.security != null && report.security.issues != null) {
                int found = findBySeverity(report.security.issues, securitySeverity);
                if (found > 0) {
                    failures.add(
                            String.format("Found %d issues in category \"security\" with severity \"%s\" or higher",
                                    found, securitySeverity));
                }
            }

            String overallSeverity = severity.getOverall();
            if (overallSeverity != null) {
                int foundData = (report.data != null && report.data.issues != null)
                        ? findBySeverity(report.data.issues, overallSeverity)
                        : 0;
                int foundSecurity = (report.security != null && report.security.issues != null)
                        ? findBySeverity(report.security.issues, overallSeverity)
                        : 0;
                int found = foundData + foundSecurity;
                if (found > 0) {
                    failures.add(
                            String.format("Found %d issues with severity \"%s\" or higher", found, overallSeverity));
                }
            }

        }

        return failures;
    }

    @edu.umd.cs.findbugs.annotations.SuppressFBWarnings("NP_UNWRITTEN_PUBLIC_OR_PROTECTED_FIELD")
    private int findBySeverity(Issues issues, String severity) {
        if (issues == null) {
            return 0;
        }

        int found = 0;
        int criticality = names.get(severity);

        for (Issue issue : issues.values()) {
            if (issue.criticality >= criticality) {
                found = found + issue.issues.size();
            }
        }

        return found;
    }

    private int getScore(Section section) {
        if (section == null) {
            return 0;
        }
        return Math.round(section.score);
    }

}