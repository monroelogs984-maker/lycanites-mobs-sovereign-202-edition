package com.lycanitesmobs.client.model.animation;

public interface IAnimationModel {
    void doAngle(float rotation, float angleX, float angleY, float angleZ);

    void doRotate(float rotX, float rotY, float rotZ);

    void doTranslate(float posX, float posY, float posZ);

    void doScale(float scaleX, float scaleY, float scaleZ);

    void angle(float rotation, float angleX, float angleY, float angleZ);

    void rotate(float rotX, float rotY, float rotZ);

    void translate(float posX, float posY, float posZ);

    void scale(float scaleX, float scaleY, float scaleZ);

    double rotateToPoint(double aTarget, double bTarget);

    double rotateToPoint(double aCenter, double bCenter, double aTarget, double bTarget);

    double[] rotateToPoint(double xCenter, double yCenter, double zCenter, double xTarget, double yTarget, double zTarget);
}
