package com.mimic.monstermod.model.parser;

import com.mimic.monstermod.model.parser.EntityModelData.ColliderData;
import org.joml.Matrix4f;
import java.util.*;

/**
 * EntityModelLoader が返す、Java描画エンジン向けのモデルデータ。
 *
 * 配置: com/mimic/monstermod/model/parser/ParsedModel.java
 */
public class ParsedModel {

    // ── ボーンツリー ──────────────────────────────────────────────
    public BoneNode boneTree;               // ルートノード（"Root"）
    public Map<String, Integer> boneIndexMap; // ボーン名→インデックス
    public int boneCount;

    // ── 頂点データ ────────────────────────────────────────────────
    public float[] positions;    // stride=3, count=N, [x,y,z, x,y,z, ...]
    public float[] uvs;          // stride=2, [u,v, u,v, ...]
    public float[] normals;      // stride=3
    public int[]   vcounts;      // 各頂点のウェイト数
    public float[] weights;      // ウェイト値プール
    public int[]   vindices;     // (boneIndex, weightIndex) ペア
    public int[]   indexBuffer;  // 三角形の頂点インデックス（3つずつ）

    // ── アニメーション ────────────────────────────────────────────
    public ParsedAnimation animation;

    // ── コライダー ────────────────────────────────────────────────
    public List<ColliderData> colliders;

    // ── ボーンノード ──────────────────────────────────────────────
    public static class BoneNode {
        public String name;
        public Matrix4f localMatrix;        // レストポーズ（ローカル）
        public BoneNode parent;
        public List<BoneNode> children;

        /** ワールド座標のレストポーズ行列（グローバル） */
        public Matrix4f getGlobalRestMatrix() {
            if (parent == null) return new Matrix4f(localMatrix);
            return new Matrix4f(parent.getGlobalRestMatrix()).mul(localMatrix);
        }
    }

    // ── アニメーション ────────────────────────────────────────────
    public static class ParsedAnimation {
        public String name;
        public Map<String, BoneTrack> tracks; // ボーン名 → トラック
    }

    public static class BoneTrack {
        public String boneName;
        public double[] times;        // キーフレーム時刻（秒）
        public Matrix4f[] matrices;   // 各キーフレームのローカル行列
    }
}