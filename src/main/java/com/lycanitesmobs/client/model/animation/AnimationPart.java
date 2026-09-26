package com.lycanitesmobs.client.model.animation;

import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AnimationPart {
    public String name;
    public AnimationPart parent;
    public AnimationPart offset;
    public String parentName;
    public Map<String, AnimationPart> children = new HashMap<>();
    public float centerX;
    public float centerY;
    public float centerZ;
    public float rotationX;
    public float rotationY;
    public float rotationZ;
    public boolean cullBackfaces;

    public List<ModelObjAnimationFrame> animationFrames = new ArrayList<>();

    public void loadFromJson(JsonObject jsonObject) {
        this.name = jsonObject.get("name").getAsString().toLowerCase();
        this.parentName = jsonObject.get("parent").getAsString().toLowerCase();
        if (this.parentName.isEmpty())
            this.parentName = null;
        this.centerX = Float.parseFloat(jsonObject.get("centerX").getAsString());
        this.centerY = Float.parseFloat(jsonObject.get("centerY").getAsString());
        this.centerZ = Float.parseFloat(jsonObject.get("centerZ").getAsString());
        if (jsonObject.has("rotationX"))
            this.rotationX = Float.parseFloat(jsonObject.get("rotationX").getAsString());
        if (jsonObject.has("rotationY"))
            this.rotationY = Float.parseFloat(jsonObject.get("rotationY").getAsString());
        if (jsonObject.has("rotationZ"))
            this.rotationZ = Float.parseFloat(jsonObject.get("rotationZ").getAsString());
        if (jsonObject.has("cullBackfaces"))
            this.cullBackfaces = jsonObject.get("cullBackfaces").getAsBoolean();
        else if (jsonObject.has("cull"))
            this.cullBackfaces = jsonObject.get("cull").getAsBoolean();
    }

    public void addChildren(AnimationPart[] parts) {
        for (AnimationPart part : parts) {
            if (part == null || part == this || part.parentName == null)
                continue;
            if (this.children.containsKey(part.parentName))
                continue;
            if (this.name.equals(part.parentName)) {
                this.children.put(part.name, part);
                part.parent = this;
            }
        }
    }

    public AnimationPart getRootParent() {
        if (this.parent == null) {
            return this;
        }
        return this.parent.getRootParent();
    }

    public void addAnimationFrame(ModelObjAnimationFrame frame) {
        this.animationFrames.add(frame);
    }

    public void applyAnimationFrames(Animator animator) {
        if (this.parent != null) {
            this.parent.applyAnimationFrames(animator);
        }

        if (this.offset != null) {
            this.offset.applyAnimationFrames(animator);
            animator.doTranslate(this.centerX + this.offset.centerX, this.centerY + this.offset.centerY, this.centerZ + this.offset.centerZ);
            animator.doRotate(-this.offset.rotationX, -this.offset.rotationY, -this.offset.rotationZ);
        }

        animator.doTranslate(this.centerX, this.centerY, this.centerZ);

        for (ModelObjAnimationFrame animationFrame : this.animationFrames) {
            animationFrame.apply(animator);
        }

        animator.doTranslate(-this.centerX, -this.centerY, -this.centerZ);
    }

    public AnimationPart setOffset(AnimationPart offsetPart) {
        this.offset = offsetPart;
        return this;
    }
}
