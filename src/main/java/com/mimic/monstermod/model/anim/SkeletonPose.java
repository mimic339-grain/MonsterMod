package com.mimic.monstermod.model.anim;

import com.mimic.monstermod.model.parser.ParsedModel;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.*;

/**
 * アニメーションキーフレームを補間し、
 * 各ボーンの「現在のスキニング行列」を計算するクラス。
 *
 * 処理の流れ:
 *   1. update(animTime) でキーフレームを Lerp/Slerp 補間
 *   2. buildSkinningMatrices() でグローバル行列を確定
 *   3. skinningMatrices[] をシェーダーに渡して頂点変換
 *
 * 配置: com/mimic/monstermod/model/anim/SkeletonPose.java
 */
public class SkeletonPose {

    private final ParsedModel model;
    /** ボーンインデックス → 現在のローカル行列（補間済み） */
    private final Matrix4f[] localMatrices;
    /** ボーンインデックス → スキニング行列（グローバル * invBind） */
    private final Matrix4f[] skinningMatrices;
    /** ボーンインデックス → インバインドポーズ行列 */
    private final Matrix4f[] invBindMatrices;

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

        // インバインドポーズ行列を事前計算
        computeInvBindMatrices(model.boneTree);
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

    /**
     * アニメーション時刻を与えてスキニング行列を更新する。
     *
     * @param anim     アニメーションデータ（nullならレストポーズ）
     * @param animTime 再生時刻（秒）
     */
    public void update(ParsedModel.ParsedAnimation anim, double animTime) {
        if (anim == null) {
            // レストポーズ: Identity
            for (var m : localMatrices) m.identity();
        } else {
            for (Map.Entry<String, ParsedModel.BoneTrack> entry : anim.tracks.entrySet()) {
                Integer idx = model.boneIndexMap.get(entry.getKey());
                if (idx == null) continue;
                localMatrices[idx] = interpolateTrack(entry.getValue(), animTime);
            }
        }
        buildSkinningMatrices(model.boneTree, new Matrix4f());
    }

    /**
     * 2つのキーフレーム間を Lerp（位置/スケール）+ Slerp（回転）で補間。
     */
    private Matrix4f interpolateTrack(ParsedModel.BoneTrack track, double time) {
        double[] times = track.times;
        Matrix4f[] matrices = track.matrices;

        if (times.length == 0) return new Matrix4f();
        if (time <= times[0]) return new Matrix4f(matrices[0]);
        if (time >= times[times.length - 1]) return new Matrix4f(matrices[times.length - 1]);

        // 前後のキーフレームを二分探索
        int lo = 0, hi = times.length - 1;
        while (lo + 1 < hi) {
            int mid = (lo + hi) / 2;
            if (times[mid] <= time) lo = mid; else hi = mid;
        }

        float t = (float)((time - times[lo]) / (times[hi] - times[lo]));

        // 位置・スケールはLerp、回転はSlerp
        Matrix4f a = matrices[lo];
        Matrix4f b = matrices[hi];

        Vector3f locA = a.getTranslation(new Vector3f());
        Vector3f locB = b.getTranslation(new Vector3f());
        Quaternionf rotA = a.getUnnormalizedRotation(new Quaternionf());
        Quaternionf rotB = b.getUnnormalizedRotation(new Quaternionf());
        Vector3f scaA = a.getScale(new Vector3f());
        Vector3f scaB = b.getScale(new Vector3f());

        Vector3f loc = locA.lerp(locB, t);
        Quaternionf rot = rotA.slerp(rotB, t);
        Vector3f sca = scaA.lerp(scaB, t);

        return new Matrix4f().translate(loc).rotate(rot).scale(sca);
    }

    /**
     * 再帰的にグローバル行列を計算し、スキニング行列を確定する。
     * スキニング行列 = グローバル行列 × インバインドポーズ
     */
    private void buildSkinningMatrices(ParsedModel.BoneNode node, Matrix4f parentGlobal) {
        if (node == null) return;
        Integer idx = model.boneIndexMap.get(node.name);
        Matrix4f global;

        if (idx != null) {
            global = new Matrix4f(parentGlobal).mul(localMatrices[idx]);
            skinningMatrices[idx] = new Matrix4f(global).mul(invBindMatrices[idx]);
        } else {
            global = new Matrix4f(parentGlobal);
        }

        if (node.children != null) {
            for (var child : node.children) buildSkinningMatrices(child, global);
        }
    }

    public Matrix4f[] getSkinningMatrices() { return skinningMatrices; }

    /**
     * 頂点位置をスキニング行列で変換する（CPU側スキニング）。
     * @param bindPos 頂点のバインドポーズ位置
     * @param boneIndices 影響するボーンのインデックス配列
     * @param boneWeights 対応するウェイト配列
     * @return 変換後の位置
     */
    public Vector3f skinVertex(Vector3f bindPos, int[] boneIndices, float[] boneWeights) {
        Vector3f result = new Vector3f();
        for (int i = 0; i < boneIndices.length; i++) {
            int bi = boneIndices[i];
            if (bi < 0 || bi >= skinningMatrices.length) continue;
            Vector3f transformed = skinningMatrices[bi].transformPosition(new Vector3f(bindPos));
            result.add(transformed.mul(boneWeights[i]));
        }
        return result;
    }
}