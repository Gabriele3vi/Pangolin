package com.platypus.pangolin.models;

import java.io.Serializable;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Date;

public class Sample implements Comparable<Sample>, Serializable {
    //TODO ricordarsi che sample deve avere anche latitudine e longitudine del sample
    //vediamo più avanti come omplementare la cosa

    private SampleType type;
    private String timeStamp;
    private int condition;
    private double value;

    public Sample(SampleType type, int condition, double value) {
        this.type = type;
        this.value = value;
        this.condition = condition;
        this.timeStamp = calculateTimeStamp();
    }

    public Sample(SampleType type, int condition, double value, String timeStamp) {
        this.type = type;
        this.value = value;
        this.condition = condition;
        this.timeStamp = timeStamp;
    }

    private String calculateTimeStamp(){
        LocalDateTime now = LocalDateTime.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        return now.format(formatter);
    }

    public SampleType getType() {
        return type;
    }

    public String getTimeStamp() {
        return timeStamp;
    }

    public int getCondition() {
        return condition;
    }

    public double getValue() {
        return value;
    }

    public void setType(SampleType type) {
        this.type = type;
    }

    public void setTimeStamp(String timeStamp) {
        this.timeStamp = timeStamp;
    }

    public void setCondition(int condition) {
        this.condition = condition;
    }

    public void setValue(double value) {
        this.value = value;
    }

    @Override
    public String toString() {
        return "Sample {" +
                "\ntype=" + type +
                ", \ntimeStamp=" + timeStamp +
                ", \ncondition=" + condition +
                ", \nvalue=" + value +
                "\n}";
    }

    @Override
    public int compareTo(Sample s) {
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        try {
            Date thisDate = dateFormat.parse(this.getTimeStamp());
            Date otherDate = dateFormat.parse(s.getTimeStamp());

            return thisDate.compareTo(otherDate);
        } catch (ParseException e) {
            System.out.println("Errore nella conversione delle date");
        }
        return 0;
    }
}
