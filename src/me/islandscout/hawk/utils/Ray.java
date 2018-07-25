package me.islandscout.hawk.utils;

import org.bukkit.Effect;
import org.bukkit.World;
import org.bukkit.util.Vector;

public class Ray implements Cloneable {

    private Vector origin;
    private Vector direction;

    public Ray(Vector origin, Vector direction) {
        this.origin = origin;
        this.direction = direction;
    }

    public Vector getPointAtDistance(double distance) {
        Vector dir = new Vector(direction.getX(), direction.getY(), direction.getZ());
        Vector orig = new Vector(origin.getX(), origin.getY(), origin.getZ());
        return orig.add(dir.multiply(distance));
    }

    public Ray clone() {
        Ray clone;
        try {
            clone = (Ray) super.clone();
            clone.origin = this.origin.clone();
            clone.direction = this.direction.clone();
            return clone;
        }
        catch (CloneNotSupportedException e) {
            e.printStackTrace();
        }
        return null;
    }

    public void highlight(World world, double blocksAway, double accuracy){
        for(double x = 0; x < blocksAway; x+=accuracy) {
            world.playEffect(getPointAtDistance(x).toLocation(world), Effect.COLOURED_DUST,1);
        }
    }

    public String toString() {
        return "origin: " + origin + " direction: " + direction;
    }

    public Vector getOrigin() {
        return origin;
    }

    public Vector getDirection() {
        return direction;
    }
}