package com.lycanitesmobs.client.obj.model;

import com.lycanitesmobs.client.obj.geometry.IndexedModel;
import com.lycanitesmobs.client.obj.geometry.Mesh;
import com.lycanitesmobs.client.obj.geometry.ObjPart;
import com.lycanitesmobs.client.obj.geometry.Vertex;
import com.lycanitesmobs.client.loader.OBJLoader;
import com.lycanitesmobs.core.entity.base.BaseCreatureEntity;
import com.lycanitesmobs.core.util.helpers.LMHelperClass;
import com.lycanitesmobs.core.util.math.Vector3o;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.world.phys.Vec3;
import org.apache.commons.io.output.ByteArrayOutputStream;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Vector2f;
import org.joml.Vector3f;
import org.joml.Vector4f;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;

/**
 * NOTE: this is the plain immediate-mode renderer (always used when vertexBuilder != null,
 * which it always will be here - see PORT_PLAN.md on why VBOObjModel/Iris batching was skipped).
 */
public class ObjModel {
    public String filename;
    public List<ObjPart> objParts = new ArrayList<>();
    public HashMap<ObjPart, IndexedModel> partToIndexedModelMap = new HashMap<>();

    public ObjModel(ResourceLocation resourceLocation) {
        this.filename = resourceLocation.getPath();
        this.objParts = new ArrayList<>();
        this.partToIndexedModelMap = new HashMap<>();
        ResourceManager resourceManager = Minecraft.getInstance().getResourceManager();
        initFromResource(resourceLocation, resourceManager);
    }

    public ObjModel(ResourceLocation resourceLocation, ResourceManager resourceManager) {
        this.filename = resourceLocation.getPath();
        this.objParts = new ArrayList<>();
        this.partToIndexedModelMap = new HashMap<>();
        initFromResource(resourceLocation, resourceManager);
    }

    private void initFromResource(ResourceLocation resourceLocation, ResourceManager resourceManager) {
        String path = resourceLocation.toString();
        try {
            InputStream inputStream = resourceManager.getResource(resourceLocation).get().open();
            String content = new String(read(inputStream), "UTF-8");
            String startPath = path.substring(0, path.lastIndexOf('/') + 1);
            HashMap<ObjPart, IndexedModel> map = new OBJLoader().loadModel(startPath, content);
            this.objParts.clear();
            Set<ObjPart> keys = map.keySet();
            Iterator<ObjPart> it = keys.iterator();
            while (it.hasNext()) {
                ObjPart objPart = it.next();
                Mesh mesh = new Mesh();
                objPart.mesh = mesh;
                this.objParts.add(objPart);
                partToIndexedModelMap.put(objPart, map.get(objPart));
                map.get(objPart).toMesh(mesh);
            }
        } catch (Exception e) {
            LMHelperClass.logWarningMessage("Unable to load model: " + resourceLocation);
            e.printStackTrace();
        }
    }

    public void dispose() {
        for (ObjPart part : this.objParts) {
            if (part.mesh != null) {
                part.mesh.delete();
                part.mesh = null;
            }
        }
        this.objParts.clear();
        this.partToIndexedModelMap.clear();
    }

    private BaseCreatureEntity creature;

    public BaseCreatureEntity getCreature() {
        return creature;
    }

    public void setCreature(BaseCreatureEntity creature) {
        this.creature = creature;
    }

    protected byte[] read(InputStream resource) throws IOException {
        int i;
        byte[] buffer = new byte[65565];
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        while ((i = resource.read(buffer, 0, buffer.length)) != -1) {
            out.write(buffer, 0, i);
        }
        out.flush();
        out.close();
        return out.toByteArray();
    }

    public Vector3o getNormal(Vector3o p1, Vector3o p2, Vector3o p3) {
        Vector3o output = new Vector3o();

        Vector3f calU = new Vector3f(p2.x() - p1.x(), p2.y() - p1.y(), p2.z() - p1.z());
        Vector3f calV = new Vector3f(p3.x() - p1.x(), p3.y() - p1.y(), p3.z() - p1.z());

        output.set(
                calU.y() * calV.z() - calU.z() * calV.y(),
                calU.z() * calV.x() - calU.x() * calV.z(),
                calU.x() * calV.y() - calU.y() * calV.x()
        );

        output.normalize();
        return output;
    }

    public void renderAll(VertexConsumer vertexBuilder, Matrix3f matrix3f, Matrix4f matrix4f, int brightness, int fade, Vector4f color, Vector2f textureOffset) {
        Collections.sort(this.objParts, (a, b) -> {
            Vec3 v = Minecraft.getInstance().getCameraEntity().position();
            double aDist = v.distanceTo(new Vec3(a.center.x(), a.center.y(), a.center.z()));
            double bDist = v.distanceTo(new Vec3(b.center.x(), b.center.y(), b.center.z()));
            return Double.compare(aDist, bDist);
        });
        for (ObjPart objPart : this.objParts) {
            this.renderPart(vertexBuilder, matrix3f, matrix4f, brightness, fade, objPart, color, textureOffset);
        }
    }

    public void renderPartGroup(VertexConsumer vertexBuilder, Matrix3f matrix3f, Matrix4f matrix4f, int brightness, int fade, Vector4f color, Vector2f textureOffset, String group) {
        for (ObjPart objPart : this.objParts) {
            if (objPart.getName().equals(group)) {
                renderPart(vertexBuilder, matrix3f, matrix4f, brightness, fade, objPart, color, textureOffset);
            }
        }
    }

    public void renderPart(VertexConsumer vertexBuilder, Matrix3f matrix3f, Matrix4f matrix4f, int brightness, int fade, ObjPart objPart, Vector4f color, Vector2f textureOffset) {
        if (objPart.mesh == null) {
            return;
        }

        int[] indices = objPart.mesh.indices;
        Vertex[] vertices = objPart.mesh.vertices;

        if (indices == null || vertices == null) {
            return;
        }

        if (objPart.mesh.normals == null || objPart.mesh.normals.length != vertices.length) {
            objPart.mesh.computeVertexNormalsIfNeeded();
        }

        if (vertexBuilder == null) {
            return;
        }

        float red = color.x();
        float green = color.y();
        float blue = color.z();
        float alpha = color.w();
        float texOffsetX = textureOffset.x * 0.01f;
        float texOffsetY = textureOffset.y * 0.01f;
        int overlayV = 10 - fade;

        for (int i = 0; i < indices.length; i += 3) {
            Vector3o fallbackNormal = null;
            for (int iv = 0; iv < 3; iv++) {
                int idx = indices[i + iv];
                Vertex v = vertices[idx];

                Vector3o normal = objPart.mesh.normals != null && idx < objPart.mesh.normals.length
                        ? objPart.mesh.normals[idx]
                        : null;

                if (normal == null) {
                    if (fallbackNormal == null) {
                        fallbackNormal = getNormal(
                                vertices[indices[i]].getPos(),
                                vertices[indices[i + 1]].getPos(),
                                vertices[indices[i + 2]].getPos()
                        );
                    }
                    normal = fallbackNormal;
                }

                // NOTE: 1.21.1's VertexConsumer dropped the (Matrix4f, x, y, z)/normal(Matrix3f, ...)
                // chained overloads used by the original 1.20.1 code - positions/normals must be
                // transformed through the matrices manually before being handed to addVertex/setNormal.
                Vector3f pos = new Vector3f(v.getPos().x(), v.getPos().y(), v.getPos().z());
                matrix4f.transformPosition(pos);
                Vector3f norm = new Vector3f(normal.x(), normal.y(), normal.z());
                matrix3f.transform(norm);

                vertexBuilder.addVertex(pos.x(), pos.y(), pos.z())
                        .setColor(red, green, blue, alpha)
                        .setUv(v.getTexCoords().x + texOffsetX, 1f - (v.getTexCoords().y + texOffsetY))
                        .setUv1(0, overlayV)
                        .setLight(brightness)
                        .setNormal(norm.x(), norm.y(), norm.z());
            }
        }
    }
}
