package com.lycanitesmobs.core.util.math;

import com.lycanitesmobs.core.util.helpers.LMHelperClass;
import it.unimi.dsi.fastutil.floats.Float2FloatFunction;

import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;

public class Vector3o {

    public static Vector3o XN = new Vector3o(-1.0F, 0.0F, 0.0F);
    public static Vector3o XP = new Vector3o(1.0F, 0.0F, 0.0F);
    public static Vector3o YN = new Vector3o(0.0F, -1.0F, 0.0F);
    public static Vector3o YP = new Vector3o(0.0F, 1.0F, 0.0F);
    public static Vector3o ZN = new Vector3o(0.0F, 0.0F, -1.0F);
    public static Vector3o ZP = new Vector3o(0.0F, 0.0F, 1.0F);
    public float x;
    public float y;
    public float z;

    public Vector3o() {
    }

    public Vector3o(float x, float y, float z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public Vector3o(float[] values) {
        set(values);
    }

    public Vector3o(Vector3o copy) {
        this.x = copy.x;
        this.y = copy.y;
        this.z = copy.z;
    }

    public boolean equals(Object p_equals_1_) {
        if (this == p_equals_1_) {
            return true;
        } else if (p_equals_1_ != null && this.getClass() == p_equals_1_.getClass()) {
            Vector3o vector3f = (Vector3o) p_equals_1_;
            if (Float.compare(vector3f.x, this.x) != 0) {
                return false;
            } else if (Float.compare(vector3f.y, this.y) != 0) {
                return false;
            } else {
                return Float.compare(vector3f.z, this.z) == 0;
            }
        } else {
            return false;
        }
    }

    public int hashCode() {
        int i = Float.floatToIntBits(this.x);
        i = 31 * i + Float.floatToIntBits(this.y);
        return 31 * i + Float.floatToIntBits(this.z);
    }

    public float x() {
        return this.x;
    }

    public float y() {
        return this.y;
    }

    public float z() {
        return this.z;
    }

    public void mul(float f) {
        this.x *= f;
        this.y *= f;
        this.z *= f;
    }

    public void mul(float x, float y, float z) {
        this.x *= x;
        this.y *= y;
        this.z *= z;
    }

    public void clamp(float min, float max) {
        this.x = Mth.clamp(this.x, min, max);
        this.y = Mth.clamp(this.y, min, max);
        this.z = Mth.clamp(this.z, min, max);
    }

    public void set(float x, float y, float z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public void add(float x, float y, float z) {
        this.x += x;
        this.y += y;
        this.z += z;
    }

    public void add(Vector3o o) {
        this.x += o.x;
        this.y += o.y;
        this.z += o.z;
    }

    public Vector3o sub(Vector3o o) {
        this.x -= o.x;
        this.y -= o.y;
        this.z -= o.z;
        return this;
    }

    public float dot(Vector3o o) {
        return this.x * o.x + this.y * o.y + this.z * o.z;
    }

    public boolean normalize() {
        float f = this.x * this.x + this.y * this.y + this.z * this.z;
        if ((double) f < 1.0E-5D) {
            return false;
        } else {
            float f1 = LMHelperClass.convertToFloat(Mth.fastInvSqrt(f));
            this.x *= f1;
            this.y *= f1;
            this.z *= f1;
            return true;
        }
    }

    public void cross(Vector3o o) {
        float f = this.x;
        float f1 = this.y;
        float f2 = this.z;
        float f3 = o.x();
        float f4 = o.y();
        float f5 = o.z();
        this.x = f1 * f5 - f2 * f4;
        this.y = f2 * f3 - f * f5;
        this.z = f * f4 - f1 * f3;
    }

    public void lerp(Vector3o o, float amount) {
        float f = 1.0F - amount;
        this.x = this.x * f + o.x * amount;
        this.y = this.y * f + o.y * amount;
        this.z = this.z * f + o.z * amount;
    }

    public Vector3o copy() {
        return new Vector3o(this.x, this.y, this.z);
    }

    public void map(Float2FloatFunction function) {
        this.x = function.get(this.x);
        this.y = function.get(this.y);
        this.z = function.get(this.z);
    }

    public Vec3 toVec3() {
        return new Vec3(this.x, this.y, this.z);
    }

    public String toString() {
        return "[" + this.x + ", " + this.y + ", " + this.z + "]";
    }

    public void set(float[] values) {
        this.x = values[0];
        this.y = values[1];
        this.z = values[2];
    }

    public void setX(float x) {
        this.x = x;
    }

    public void setY(float y) {
        this.y = y;
    }

    public void setZ(float z) {
        this.z = z;
    }

    public Quaternionf rotationDegrees(float angle) {
        return rotationDegrees(angle, true);
    }

    public Quaternionf rotationDegrees(float angle, boolean toRadians) {
        if (toRadians) {
            angle *= ((float) Math.PI / 180F);
        }

        float f = sin(angle / 2.0F);
        float i = this.x() * f;
        float j = this.y() * f;
        float k = this.z() * f;
        float r = cos(angle / 2.0F);
        return new Quaternionf(i, j, k, r);
    }

    private float sin(float a) {
        return (float) Math.sin((double) a);
    }

    private float cos(float a) {
        return (float) Math.cos((double) a);
    }
}
