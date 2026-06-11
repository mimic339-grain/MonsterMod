package com.mimic.monstermod.model.anim;

import com.mimic.monstermod.model.parser.ParsedModel;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.*;

/**
 * アニメーション補間とスキニング行列計算クラス（事前計算キャッシュ版）
 */
public class SkeletonPose {

    private final ParsedModel model;
    private final Matrix4f[] localMatrices;
    private final Matrix4f[] skinningMatrices;
    private final Matrix4f[] invBindMatrices;

    // キャッシュ用フィールド
    private Vector3f[] cachedPositions;
    private Vector3f[] cachedNormals;

    public SkeletonPose(ParsedModel model) {
        this.model = model;
        int n = model.boneCount;
        this.localMatrices    = new Matrix4f[n];
        this.skinningMatrices = new Matrix4f[n];
        this.invBindMatrices  = new Matrix4f[n];

        for (int i = 0; i < n; i++) {
            localMatrices[i]    = new Matrix4f();
            skinningMatrices[i] = new Matrix4f();
        }
        computeInvBindMatrices(model.boneTree);
    }

    public void prepareCache(ParsedModel model) {
        this.cachedPositions = new Vector3f[model.positions.length / 3];
        this.cachedNormals = new Vector3f[model.normals.length / 3];
        for(int i = 0; i < cachedPositions.length; i++) {
            cachedPositions[i] = new Vector3f();
            cachedNormals[i] = new Vector3f();
        }
    }

    private void computeInvBindMatrices(ParsedModel.BoneNode node) {
        if (node == null) return;
        Integer idx = model.boneIndexMap.get(node.name);
        if (idx != null) {
            invBindMatrices[idx] = new Matrix4f(node.getGlobalRestMatrix()).invert();
        }
        if (node.children != null) {
            for (var child : node.children) computeInvBindMatrices(child);
        }
    }

    public void update(ParsedModel.ParsedAnimation anim, double animTime) {
        if (anim == null) {
            for (var m : localMatrices) m.identity();
        } else {
            for (Map.Entry<String, ParsedModel.BoneTrack> entry : anim.tracks.entrySet()) {
                Integer idx = model.boneIndexMap.get(entry.getKey());
                if (idx != null) {
                    localMatrices[idx] = interpolateTrack(entry.getValue(), animTime);
                }
            }
        }
        buildSkinningMatrices(model.boneTree, new Matrix4f());

        // 行列更新後に頂点と法線をまとめてキャッシュ（事前計算）
        for (int i = 0; i < cachedPositions.length; i++) {
            Vector3f bindPos = new Vector3f(model.positions[i*3], model.positions[i*3+1], model.positions[i*3+2]);
            Vector3f bindNorm = new Vector3f(model.normals[i*3], model.normals[i*3+1], model.normals[i*3+2]);

            cachedPositions[i].set(skinVertex(bindPos, model.skinnedBoneIndices[i], model.skinnedWeights[i]));
            cachedNormals[i].set(skinNormal(bindNorm, model.skinnedBoneIndices[i], model.skinnedWeights[i]));
        }
    }

    public Vector3f getSkinnedPosition(int vi) { return cachedPositions[vi]; }
    public Vector3f getSkinnedNormal(int vi) { return cachedNormals[vi]; }

    /** 法線のスキニング計算：平行移動を含まない行列の変換 */
    private Vector3f skinNormal(Vector3f normal, int[] indices, float[] weights) {
        Vector3f result = new Vector3f(0, 0, 0);
        Vector3f temp = new Vector3f();
        for (int i = 0; i < indices.length; i++) {
            int bi = indices[i];
            if (bi >= 0 && bi < skinningMatrices.length && weights[i] > 0) {
                // transformDirection は平行移動を無視する（法線用）
                skinningMatrices[bi].transformDirection(new Vector3f(normal), temp);
                result.add(temp.mul(weights[i]));
            }
        }
        return result.normalize(); // 正規化して返す
    }

    public Vector3f skinVertex(Vector3f bindPos, int[] boneIndices, float[] boneWeights) {
        Vector3f result = new Vector3f(0, 0, 0);
        Vector3f temp = new Vector3f();
        for (int i = 0; i < boneIndices.length; i++) {
            int bi = boneIndices[i];
            if (bi >= 0 && bi < skinningMatrices.length && boneWeights[i] > 0) {
                skinningMatrices[bi].transformPosition(new Vector3f(bindPos), temp);
                result.add(temp.mul(boneWeights[i]));
            }
        }
        return result;
    }

    private Matrix4f interpolateTrack(ParsedModel.BoneTrack track, double time) {
        // ... (以前の補間処理と同一) ...
        double[] times = track.times;
        Matrix4f[] matrices = track.matrices;
        if (times.length == 0) return new Matrix4f();
        if (time <= times[0]) return new Matrix4f(matrices[0]);
        if (time >= times[times.length - 1]) return new Matrix4f(matrices[times.length - 1]);
        int lo = 0, hi = times.length - 1;
        while (lo + 1 < hi) { int mid = (lo + hi) / 2; if (times[mid] <= time) lo = mid; else hi = mid; }
        float t = (float)((time - times[lo]) / (times[hi] - times[lo]));
        Matrix4f a = matrices[lo]; Matrix4f b = matrices[hi];
        Vector3f loc = a.getTranslation(new Vector3f()).lerp(b.getTranslation(new Vector3f()), t);
        Vector3f sca = a.getScale(new Vector3f()).lerp(b.getScale(new Vector3f()), t);
        Quaternionf rotA = a.getUnnormalizedRotation(new Quaternionf());
        Quaternionf rotB = b.getUnnormalizedRotation(new Quaternionf());
        if (rotA.dot(rotB) < 0.0f) rotA.set(-rotA.x, -rotA.y, -rotA.z, -rotA.w);
        Quaternionf rot = rotA.slerp(rotB, t);
        return new Matrix4f().translate(loc).rotate(rot).scale(sca);
    }

    private void buildSkinningMatrices(ParsedModel.BoneNode node, Matrix4f parentGlobal) {
        if (node == null) return;
        Integer idx = model.boneIndexMap.get(node.name);
        Matrix4f global = new Matrix4f(parentGlobal);
        if (idx != null) {
            global.mul(localMatrices[idx]);
            skinningMatrices[idx] = new Matrix4f(global).mul(invBindMatrices[idx]);
        }
        if (node.children != null) {
            for (var child : node.children) buildSkinningMatrices(child, global);
        }
    }

    public Matrix4f[] getSkinningMatrices() { return skinningMatrices; }
}