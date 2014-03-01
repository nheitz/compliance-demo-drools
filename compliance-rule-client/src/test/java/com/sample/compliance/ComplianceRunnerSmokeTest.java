package com.sample.compliance;

import static org.fest.assertions.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.Stack;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.kie.api.KieServices;
import org.kie.api.builder.ReleaseId;
import org.kie.api.event.rule.DebugAgendaEventListener;
import org.kie.api.runtime.KieContainer;
import org.kie.api.runtime.KieSession;
import org.kie.api.runtime.rule.FactHandle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ComplianceRunnerSmokeTest {
    private static final Logger  logger = LoggerFactory.getLogger(ComplianceRunnerSmokeTest.class);

    private static ReleaseId     releaseId;
    private static KieContainer  kContainer;
    private static KieSession    kSession;
    private static Stack<String> executionStack;

    @BeforeClass
    public static void setUp() {
        KieServices ks = KieServices.Factory.get();
        releaseId = ks.newReleaseId("com.recordsure", "compliance-rules", "0.0.1-SNAPSHOT");
        kContainer = ks.newKieContainer(releaseId);
        kSession = kContainer.newKieSession("recordsure-compliance-session");
        kSession.addEventListener(new DebugAgendaEventListener());
        executionStack = new Stack<String>();
        kSession.insert(executionStack);
    }

    @AfterClass
    public static void tearDown() {
        kSession.dispose();
    }

    @Test
    public void lowAgePersonShould_TrafficLightToAmber() throws Exception {
        SampleCase sampleCase = new SampleCase(20, 1, RiskAversion.LOW, BigDecimal.ZERO);

        kSession.setGlobal("riskRatingMap", setUpRatings());
        FactHandle fHandle = kSession.insert(sampleCase);

        kSession.startProcess("flow_compliance-full");
        kSession.fireAllRules();

        kSession.delete(fHandle);
        printStack(executionStack, true);
        logger.debug(String.format("Result after scoring: %s", sampleCase));

        assertThat(sampleCase.getTrafficLight()).isEqualTo(TrafficLight.AMBER);
    }

    @Test
    public void midAgePersonShould_TrafficLightToGreen() throws Exception {
        SampleCase sampleCase = new SampleCase(45, 1, RiskAversion.LOW, BigDecimal.ZERO);

        kSession.setGlobal("riskRatingMap", setUpRatings());
        FactHandle fHandle = kSession.insert(sampleCase);

        kSession.startProcess("flow_compliance-full");
        kSession.fireAllRules();

        kSession.delete(fHandle);
        printStack(executionStack, true);
        logger.debug(String.format("Result after scoring: %s", sampleCase));
        assertThat(sampleCase.getTrafficLight()).isEqualTo(TrafficLight.GREEN);
    }

    @Test
    public void highAgePersonShould_TrafficLightToRed() throws Exception {
        SampleCase sampleCase = new SampleCase(91, 1, RiskAversion.LOW, BigDecimal.ZERO);

        kSession.setGlobal("riskRatingMap", setUpRatings());
        FactHandle fHandle = kSession.insert(sampleCase);

        kSession.startProcess("flow_compliance-full");
        kSession.fireAllRules();

        kSession.delete(fHandle);
        printStack(executionStack, true);
        logger.debug(String.format("Result after scoring: %s", sampleCase));
        assertThat(sampleCase.getTrafficLight()).isEqualTo(TrafficLight.RED);
    }

    private static Map<String, BigDecimal> setUpRatings() {
        HashMap<String, BigDecimal> result = new HashMap<String, BigDecimal>() {
            {
                put("LOW_AGE_SCORE", new BigDecimal("50"));
                put("MID_AGE_SCORE", new BigDecimal("20"));
                put("HIGH_AGE_SCORE", new BigDecimal("100"));
            }
        };
        return result;
    }

    private static void printStack(Stack<String> executionStack, boolean andClear) {
        while (!executionStack.isEmpty()) {
            logger.debug(executionStack.pop());
        }
        if (andClear)
            executionStack.clear();
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