package me.islandscout.hawk.utils;

public class Line {

    private double height;
    private double slope;

    public Line(double height, double slope) {
        this.height = height;
        this.slope = slope;
    }

    public double getHeight() {
        return height;
    }

    public void setHeight(double height) {
        this.height = height;
    }

    public double getSlope() {
        return slope;
    }

    public void setSlope(double slope) {
        this.slope = slope;
    }

    public double getYatX(double x) {
        return slope * x + height;
    }
}
