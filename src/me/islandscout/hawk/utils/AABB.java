package me.islandscout.hawk.utils;

import me.islandscout.hawk.Hawk;
import org.bukkit.Bukkit;
import org.bukkit.Effect;
import org.bukkit.World;
import org.bukkit.util.Vector;

public class AABB implements Cloneable {

    private Vector min;
    private Vector max;

    public AABB(Vector min, Vector max) {
        this.min = min;
        this.max = max;
    }

    /**
     * Calculates intersection with the given ray between a certain distance
     * interval.
     * <p>
     * Ray-box intersection is using IEEE numerical properties to ensure the
     * test is both robust and efficient, as described in:
     * <br>
     * <code>Amy Williams, Steve Barrus, R. Keith Morley, and Peter Shirley: "An
     * Efficient and Robust Ray-Box Intersection Algorithm" Journal of graphics
     * tools, 10(1):49-54, 2005</code>
     *
     * @param ray incident ray
     * @param minDist minimum distance
     * @param maxDist maximum distance
     * @return intersection point on the bounding box (only the first is
     *         returned) or null if no intersection
     */
    public Vector intersectsRay(Ray ray, float minDist, float maxDist) {
        Vector invDir = new Vector(1f / ray.getDirection().getX(), 1f / ray.getDirection().getY(), 1f / ray.getDirection().getZ());

        boolean signDirX = invDir.getX() < 0;
        boolean signDirY = invDir.getY() < 0;
        boolean signDirZ = invDir.getZ() < 0;

        Vector bbox = signDirX ? max : min;
        double tmin = (bbox.getX() - ray.getOrigin().getX()) * invDir.getX();
        bbox = signDirX ? min : max;
        double tmax = (bbox.getX() - ray.getOrigin().getX()) * invDir.getX();
        bbox = signDirY ? max : min;
        double tymin = (bbox.getY() - ray.getOrigin().getY()) * invDir.getY();
        bbox = signDirY ? min : max;
        double tymax = (bbox.getY() - ray.getOrigin().getY()) * invDir.getY();

        if ((tmin > tymax) || (tymin > tmax)) {
            return null;
        }
        if (tymin > tmin) {
            tmin = tymin;
        }
        if (tymax < tmax) {
            tmax = tymax;
        }

        bbox = signDirZ ? max : min;
        double tzmin = (bbox.getZ() - ray.getOrigin().getZ()) * invDir.getZ();
        bbox = signDirZ ? min : max;
        double tzmax = (bbox.getZ() - ray.getOrigin().getZ()) * invDir.getZ();

        if ((tmin > tzmax) || (tzmin > tmax)) {
            return null;
        }
        if (tzmin > tmin) {
            tmin = tzmin;
        }
        if (tzmax < tmax) {
            tmax = tzmax;
        }
        if ((tmin < maxDist) && (tmax > minDist)) {
            return ray.getPointAtDistance(tmin);
        }
        return null;
    }

    public void highlight(Hawk hawk, World world, double accuracy){
        Bukkit.getScheduler().scheduleSyncDelayedTask(hawk, () -> {
            for(double x = min.getX(); x < max.getX(); x+=accuracy){
                for(double y = min.getY(); y < max.getY(); y+=accuracy) {
                    for (double z = min.getZ(); z < max.getZ(); z+=accuracy) {
                        Vector position = new Vector(x, y, z);
                        world.playEffect(position.toLocation(world), Effect.COLOURED_DUST,1);
                        world.playEffect(position.toLocation(world), Effect.COLOURED_DUST,1);
                    }
                }
            }
        }, 0L);

    }

    public void translate(Vector vector) {
        min.add(vector);
        max.add(vector);
    }

    public boolean isColliding(AABB other) {
        if(max.getX() < other.getMin().getX() || min.getX() > other.getMax().getX()) {
            return false;
        }
        if(max.getY() < other.getMin().getY() || min.getY() > other.getMax().getY()) {
            return false;
        }
        if(max.getZ() < other.getMin().getZ() || min.getZ() > other.getMax().getZ()) {
            return false;
        }
        return true;
    }

    public AABB clone() {
        AABB clone;
        try {
            clone = (AABB) super.clone();
            clone.min = this.min.clone();
            clone.max = this.max.clone();
            return clone;
        }
        catch (CloneNotSupportedException e) {
            e.printStackTrace();
        }
        return null;
    }

    public double getVolume() {
        return (max.getX() - min.getX()) * (max.getY() - min.getY()) * (max.getZ() - min.getZ());
    }

    public Vector getMax() {
        return max;
    }

    public Vector getMin() {
        return min;
    }
}