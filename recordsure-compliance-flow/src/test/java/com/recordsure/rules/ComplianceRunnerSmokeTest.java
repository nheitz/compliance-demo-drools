package com.recordsure.rules;

import java.util.ArrayList;
import java.util.List;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.kie.api.KieServices;
import org.kie.api.builder.ReleaseId;
import org.kie.api.event.rule.AfterMatchFiredEvent;
import org.kie.api.event.rule.AgendaEventListener;
import org.kie.api.event.rule.AgendaGroupPoppedEvent;
import org.kie.api.event.rule.AgendaGroupPushedEvent;
import org.kie.api.event.rule.BeforeMatchFiredEvent;
import org.kie.api.event.rule.MatchCancelledEvent;
import org.kie.api.event.rule.MatchCreatedEvent;
import org.kie.api.event.rule.RuleFlowGroupActivatedEvent;
import org.kie.api.event.rule.RuleFlowGroupDeactivatedEvent;
import org.kie.api.runtime.KieContainer;
import org.kie.api.runtime.KieSession;
import org.kie.api.runtime.rule.FactHandle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ComplianceRunnerSmokeTest {
    private static final Logger logger = LoggerFactory.getLogger(ComplianceRunnerSmokeTest.class);

    private static ReleaseId    releaseId;
    private static KieContainer kContainer;
    private static KieSession   kSession;
    private static List<String> executionTrace;

    @BeforeClass
    public static void setUp() {
        KieServices ks = KieServices.Factory.get();
        releaseId = ks.newReleaseId("com.recordsure.rules.compliance", "compliance-flow", "0.0.1-SNAPSHOT");
        kContainer = ks.newKieContainer(releaseId);
        kSession = kContainer.newKieSession("recordsure-compliance-flow-session");
        executionTrace = new ArrayList<>();
        kSession.addEventListener(new StreamlinedAgendaListener(executionTrace));
        kSession.insert(executionTrace);
    }

    @AfterClass
    public static void tearDown() {
        kSession.dispose();
    }

    @Test
    public void phraseFlowShouldFire() throws Exception {
        Object o = new Object();
        FactHandle fHandle = kSession.insert(o);

        kSession.startProcess("compliance_flow-master");
        kSession.fireAllRules();

        kSession.delete(fHandle);
        printTrace(String.format("SCORE RESULT: %s", o), executionTrace, true);

    }

    private static void printTrace(String result, List<String> executionTrace, boolean andClear) {
        StringBuffer buff = new StringBuffer();
        buff.append("\n").append(result);
        buff.append("\n----------------------------");
        for (String traceElement : executionTrace) {
            buff.append("\n\t").append(traceElement);
        }
        buff.append("\n----------------------------");

        logger.debug(buff.toString());
        if (andClear)
            executionTrace.clear();
    }


    private static class StreamlinedAgendaListener implements AgendaEventListener {

        private final List<String> executionTrace;

        public StreamlinedAgendaListener(List<String> executionTrace) {
            this.executionTrace = executionTrace;

        }

        @Override
        public void matchCreated(MatchCreatedEvent event) {
        }

        @Override
        public void matchCancelled(MatchCancelledEvent event) {
        }

        @Override
        public void beforeMatchFired(BeforeMatchFiredEvent event) {
            executionTrace.add(String.format("==> Matching: rule [id: %s]", event.getMatch().getRule().getId()));
        }

        @Override
        public void afterMatchFired(AfterMatchFiredEvent event) {
        }

        @Override
        public void agendaGroupPopped(AgendaGroupPoppedEvent event) {
        }

        @Override
        public void agendaGroupPushed(AgendaGroupPushedEvent event) {
        }

        @Override
        public void beforeRuleFlowGroupActivated(RuleFlowGroupActivatedEvent event) {
        }

        @Override
        public void afterRuleFlowGroupActivated(RuleFlowGroupActivatedEvent event) {
        }

        @Override
        public void beforeRuleFlowGroupDeactivated(RuleFlowGroupDeactivatedEvent event) {
        }

        @Override
        public void afterRuleFlowGroupDeactivated(RuleFlowGroupDeactivatedEvent event) {
        }

    }
}
