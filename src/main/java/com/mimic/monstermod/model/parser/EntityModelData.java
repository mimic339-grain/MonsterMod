package com.mimic.monstermod.model.parser;

import java.util.List;
import java.util.Map;

/**
 * BlenderエクスポートJSONをGsonでデシリアライズするためのデータクラス群。
 *
 * JSONの構造:
 *   {
 *     "metadata": {...},
 *     "armature": { "joints": [...], "hierarchy": [...] },
 *     "meshes": { "Body": { "positions": {...}, "uvs": {...}, ... } },
 *     "animation": { "name": "walk", "frames": [...] }
 *   }
 *
 * 配置: com/mimic/monstermod/model/parser/EntityModelData.java
 */
public class EntityModelData {

    // ── トップレベル ──────────────────────────────────────────────
    public MetaData metadata;
    public ArmatureData armature;
    public Map<String, MeshData> meshes;
    public AnimationData animation;
    public List<ColliderData> colliders;

    // ── メタデータ ────────────────────────────────────────────────
    public static class MetaData {
        public String entityType;
        public int totalHP;
        public String networkPriority;
        public int lodLevels;
    }

    // ── アーマチュア ──────────────────────────────────────────────
    public static class ArmatureData {
        public List<String> joints;        // ボーン名リスト（順序が重要）
        public List<BoneNode> hierarchy;   // ボーンツリー
    }

    public static class BoneNode {
        public String name;
        public float[] transform;          // 16要素 = Matrix4f（行優先）
        public List<BoneNode> children;
    }

    // ── メッシュ ──────────────────────────────────────────────────
    public static class MeshData {
        public ArrayData positions;        // stride=3, float[]
        public ArrayData uvs;             // stride=2, float[]
        public ArrayData normals;         // stride=3, float[]
        public ArrayData vcounts;         // 各頂点のウェイト数
        public ArrayData weights;         // ウェイト値リスト
        public ArrayData vindices;        // (boneIndex, weightIndex) ペア
        public Map<String, ArrayData> parts; // "noGroups"など
    }

    public static class ArrayData {
        public int stride;
        public int count;
        public List<Number> array;

        public float[] toFloatArray() {
            float[] arr = new float[array.size()];
            for (int i = 0; i < arr.length; i++) arr[i] = array.get(i).floatValue();
            return arr;
        }

        public int[] toIntArray() {
            int[] arr = new int[array.size()];
            for (int i = 0; i < arr.length; i++) arr[i] = array.get(i).intValue();
            return arr;
        }
    }

    // ── アニメーション ────────────────────────────────────────────
    public static class AnimationData {
        public String name;
        public List<BoneAnimTrack> frames;
    }

    public static class BoneAnimTrack {
        public String name;              // ボーン名
        public List<Float> time;         // キーフレーム時刻（秒）
        public List<TransformAttr> transform; // ATTRフォーマット
        public List<float[]> matrix;         // MATフォーマット（16要素）
    }

    public static class TransformAttr {
        public float[] loc;  // [x, y, z]
        public float[] rot;  // [qw, qx, qy, qz]
        public float[] sca;  // [sx, sy, sz]
    }

    // ── OBBコライダー ─────────────────────────────────────────────
    public static class ColliderData {
        public String name;
        public String boneName;
        public String partGroup;
        public float[] center;       // [x, y, z]
        public float[] halfExtents;  // [hx, hy, hz]
        public float[] orientation;  // [qw, qx, qy, qz]
        public float damageMultiplier;
        public boolean isWeakPoint;
    }
}