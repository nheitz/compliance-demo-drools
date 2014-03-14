package com.sample.compliance;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

import org.kie.api.definition.type.PropertyReactive;

@PropertyReactive
public class SampleCase {
    private static final BigDecimal   ONE_HUNDRED     = new BigDecimal("100");
    private final int                 id;
    private int                       age;
    private int                       state;
    private RiskAversion              riskAversion;
    private final Map<String, String> looseAttributes = new HashMap<>();

    private TrafficLight              trafficLight;
    private BigDecimal                riskRating;

    public SampleCase(int id, int age, int state, RiskAversion riskAversion,
            BigDecimal riskRating) {
        super();
        this.id = id;
        this.age = age;
        this.state = state;
        this.riskAversion = riskAversion;
        this.riskRating = riskRating;
    }

    public int getAge() {
        return age;
    }

    public void setAge(int age) {
        this.age = age;
    }

    public int getState() {
        return state;
    }

    public void setState(int state) {
        this.state = state;
    }

    public RiskAversion getRiskAversion() {
        return riskAversion;
    }

    public void setRiskAversion(RiskAversion riskAversion) {
        this.riskAversion = riskAversion;
    }

    public BigDecimal getRiskRating() {
        return riskRating;
    }

    public void setRiskRating(BigDecimal riskRating) {
        this.riskRating = riskRating;
    }

    public BigDecimal boostRatingByPercent(BigDecimal boostPercent) {
        BigDecimal boostRating = boostPercent.divide(ONE_HUNDRED);
        return getRiskRating().add(boostRating);
    }

    public TrafficLight getTrafficLight() {
        return trafficLight;
    }

    public void setTrafficLight(TrafficLight trafficLight) {
        this.trafficLight = trafficLight;
    }

    public Map<String, String> getLooseAttributes() {
        return looseAttributes;
    }

    public int getId() {
        return id;
    }

    @Override
    public String toString() {
        return "SampleCase [id=" + id + ", age=" + age + ", state=" + state + ", riskAversion=" + riskAversion
                + ", looseAttributes=" + looseAttributes + ", trafficLight=" + trafficLight + ", riskRating="
                + riskRating + "]";
    }


}
