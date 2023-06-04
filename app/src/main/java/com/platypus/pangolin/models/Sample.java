package com.platypus.pangolin.models;

import java.util.Date;

public class Sample {
    //TODO ricordarsi che sample deve avere anche latitudine e longitudine del sample
    //vediamo più avanti come omplementare la cosa

    private SampleType type;
    private Date date;
    private SignalCondition condition;
    private double value;

    public Sample(SampleType type, SignalCondition condition, double value) {
        this.type = type;
        this.value = value;
        this.condition = condition;
        this.date = new Date();
    }

    public SampleType getType() {
        return type;
    }

    public Date getDate() {
        return date;
    }

    public SignalCondition getCondition() {
        return condition;
    }

    public double getValue() {
        return value;
    }

    public void setType(SampleType type) {
        this.type = type;
    }

    public void setDate(Date date) {
        this.date = date;
    }

    public void setCondition(SignalCondition condition) {
        this.condition = condition;
    }

    public void setValue(double value) {
        this.value = value;
    }

    @Override
    public String toString() {
        return "Sample {" +
                "\ntype=" + type +
                ", \ndate=" + date +
                ", \ncondition=" + condition +
                ", \nvalue=" + value +
                "\n}";
    }
}
