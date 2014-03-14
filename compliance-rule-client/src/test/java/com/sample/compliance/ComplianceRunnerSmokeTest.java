package com.sample.compliance;

import static org.fest.assertions.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.drools.compiler.lang.descr.PackageDescr;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.kie.api.KieServices;
import org.kie.api.builder.KieFileSystem;
import org.kie.api.builder.ReleaseId;
import org.kie.api.definition.KieDescr;
import org.kie.api.event.rule.AfterMatchFiredEvent;
import org.kie.api.event.rule.AgendaEventListener;
import org.kie.api.event.rule.AgendaGroupPoppedEvent;
import org.kie.api.event.rule.AgendaGroupPushedEvent;
import org.kie.api.event.rule.BeforeMatchFiredEvent;
import org.kie.api.event.rule.MatchCancelledEvent;
import org.kie.api.event.rule.MatchCreatedEvent;
import org.kie.api.event.rule.RuleFlowGroupActivatedEvent;
import org.kie.api.event.rule.RuleFlowGroupDeactivatedEvent;
import org.kie.api.io.KieResources;
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
        releaseId = ks.newReleaseId("com.recordsure", "compliance-rules", "0.0.1-SNAPSHOT");
        kContainer = ks.newKieContainer(releaseId);
        kSession = kContainer.newKieSession("recordsure-compliance-session");
        executionTrace = new ArrayList<>();
        // kSession.addEventListener(new DebugAgendaEventListener());
        kSession.addEventListener(new StreamlinedAgendaListener(executionTrace));
        kSession.insert(executionTrace);
    }

    @AfterClass
    public static void tearDown() {
        kSession.dispose();
    }

    @Test
    public void lowAgePersonShould_TrafficLightToAmber() throws Exception {
        SampleCase sampleCase = new SampleCase(1, 20, 1, RiskAversion.LOW, BigDecimal.ZERO);
        sampleCase.getLooseAttributes().put("hasClaims", "true");

        kSession.setGlobal("riskRatingMap", setUpRatings());
        FactHandle fHandle = kSession.insert(sampleCase);

        kSession.startProcess("flow_compliance-full");
        kSession.fireAllRules();

        kSession.delete(fHandle);
        printTrace(String.format("SCORE RESULT: %s", sampleCase), executionTrace, true);

        assertThat(sampleCase.getTrafficLight()).isEqualTo(TrafficLight.AMBER);
    }

    @Test
    public void midAgePersonWithClaimsShould_TrafficLightToGreen() throws Exception {
        SampleCase sampleCase = new SampleCase(2, 45, 1, RiskAversion.LOW, BigDecimal.ZERO);
        sampleCase.getLooseAttributes().put("hasClaims", "true");

        kSession.setGlobal("riskRatingMap", setUpRatings());
        FactHandle fHandle = kSession.insert(sampleCase);

        kSession.startProcess("flow_compliance-full");
        kSession.fireAllRules();

        kSession.delete(fHandle);
        printTrace(String.format("SCORE RESULT: %s", sampleCase), executionTrace, true);

        assertThat(sampleCase.getTrafficLight()).isEqualTo(TrafficLight.GREEN);
    }

    @Test
    public void midAgePersonWithMacheteShould_ExitFast() throws Exception {
        SampleCase sampleCase = new SampleCase(2, 45, 1, RiskAversion.LOW, BigDecimal.ZERO);
        sampleCase.getLooseAttributes().put("hasClaims", "false");
        sampleCase.getLooseAttributes().put("ownsMachete", "true");

        kSession.setGlobal("riskRatingMap", setUpRatings());
        FactHandle fHandle = kSession.insert(sampleCase);

        kSession.startProcess("flow_compliance-full");
        kSession.fireAllRules();

        kSession.delete(fHandle);
        printTrace(String.format("SCORE RESULT: %s", sampleCase), executionTrace, true);

        assertThat(sampleCase.getTrafficLight()).isNull();
        ;
    }

    @Test
    public void highAgePersonShould_TrafficLightToRed() throws Exception {
        SampleCase sampleCase = new SampleCase(3, 91, 1, RiskAversion.LOW, BigDecimal.ZERO);
        sampleCase.getLooseAttributes().put("hasClaims", "true");

        kSession.setGlobal("riskRatingMap", setUpRatings());
        FactHandle fHandle = kSession.insert(sampleCase);

        kSession.startProcess("flow_compliance-full");
        kSession.fireAllRules();

        kSession.delete(fHandle);
        printTrace(String.format("SCORE RESULT: %s", sampleCase), executionTrace, true);

    }

    @Test
    public void rulesCanBeBuiltProgrammatically() throws Exception {
        KieServices ks = KieServices.Factory.get();

        KieFileSystem kfs = ks.newKieFileSystem();
        KieResources kieResources = ks.getResources();
        KieDescr kieDescr = new PackageDescr("namespace1");

    }

    private static Map<String, BigDecimal> setUpRatings() {
        HashMap<String, BigDecimal> result = new HashMap<String, BigDecimal>() {
            {
                put("LOW_AGE_SCORE", new BigDecimal("50"));
                put("MID_AGE_SCORE", new BigDecimal("20"));
                put("HIGH_AGE_SCORE", new BigDecimal("100"));
                put("HAS_PRIOR_CLAIMS_LOW_AGE", new BigDecimal("1"));
                put("HAS_PRIOR_CLAIMS_MID_AGE", new BigDecimal(".5"));
            }
        };
        return result;
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

    // private static KieSession buildKieSession() throws Exception {
    // String[] drlTemplates = new String[] {
    // "compliance/scoring/rules_risk-scoring.template.drl",
    // "compliance/triggers/rules_risk-triggers.template.drl"
    // };
    // Properties props = new Properties();
    // props.load(ComplianceRunner.class.getClassLoader().getResourceAsStream("complianceValues.properties"));
    //
    // List<Map<String, Object>> paramList = new ArrayList<>();
    // Map<String, Object> paramMap = new HashMap<>();
    //
    // for (String propName : props.stringPropertyNames()) {
    // paramMap.put(propName, props.getProperty(propName));
    // }
    //
    // paramList.add(paramMap);
    //
    // KieServices ks = KieServices.Factory.get();
    //
    // KieModuleModel kieModuleModel = ks.newKieModuleModel();
    //
    // KieBaseModel kieBaseModel =
    // kieModuleModel.newKieBaseModel("KBaseCompliance").setDefault(true)
    // .setEqualsBehavior(EqualityBehaviorOption.EQUALITY)
    // .setEventProcessingMode(EventProcessingOption.STREAM);
    //
    // kieBaseModel.newKieSessionModel("kSessionCompliance").setDefault(true)
    // .setType(KieSessionModel.KieSessionType.STATEFUL).setClockType(ClockTypeOption.get("realtime"));
    //
    // KieFileSystem kfs = ks.newKieFileSystem();
    // ReleaseId rid = ks.newReleaseId("com.recordsure",
    // "drools-proofofconcept", "0.0.1-SNAPSHOT");
    // kfs.generateAndWritePomXML(rid);
    //
    // KieResources resources = ks.getResources();
    //
    // ClassLoader loader = ComplianceRunner.class.getClassLoader();
    //
    // // for (String drlTemplate : drlTemplates) {
    // // String drl = mergeRule(paramList, drlTemplate);
    // // Resource drlResource = resources.newReaderResource(
    // // new StringReader(drl), "UTF-8").setTargetPath(
    // // "src/main/resources/" + drlTemplate);
    // //
    // // kfs.write(drlResource);
    // kfs.write(resources.newClassPathResource("compliance/scoring/rules_risk-scoring.template.drl",
    // loader))
    // .write(resources.newClassPathResource("compliance/triggers/rules_risk-triggers.template.drl",
    // loader));
    //
    // // }
    // kfs.write(resources.newClassPathResource("compliance/scoring/flow_risk-scoring.bpmn",
    // loader))
    // .write(resources.newClassPathResource("compliance/triggers/flow_risk-triggers.bpmn",
    // loader))
    // .write(resources.newClassPathResource("compliance/flow_compliance-full.bpmn",
    // loader));
    //
    // KieBuilder kieBuilder = ks.newKieBuilder(kfs).buildAll();
    //
    // KieContainer kieContainer =
    // ks.newKieContainer(kieBuilder.getKieModule().getReleaseId());
    //
    // return kieContainer.newKieSession();
    // }
    //
    // private static String mergeRule(List<Map<String, Object>> paramList,
    // String ruleTemplateInClasspath) {
    // ObjectDataCompiler compiler = new ObjectDataCompiler();
    // InputStream is =
    // ComplianceRunner.class.getClassLoader().getResourceAsStream(ruleTemplateInClasspath);
    // return compiler.compile(paramList, is);
    // }
}
