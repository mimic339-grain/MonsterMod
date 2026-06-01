package com.mimic.monstermod.model.parser;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import org.joml.Matrix4f;
import org.joml.Quaternionf;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * リソースパックからBlender出力JSONを読み込み、
 * Java用の ParsedModel に変換するローダー。
 *
 * 使い方:
 *   ParsedModel model = EntityModelLoader.load(
 *       resourceManager,
 *       new ResourceLocation("monstermod", "models/entity/mimic.json")
 *   );
 *
 * 配置: com/mimic/monstermod/model/parser/EntityModelLoader.java
 */
public class EntityModelLoader {

    private static final Gson GSON = new GsonBuilder().create();

    public static ParsedModel load(ResourceManager rm, ResourceLocation loc) {
        try (var stream = rm.getResource(loc).get().open()) {
            EntityModelData raw = GSON.fromJson(
                    new InputStreamReader(stream, StandardCharsets.UTF_8),
                    EntityModelData.class);
            return parse(raw);
        } catch (Exception e) {
            throw new RuntimeException("Failed to load model: " + loc, e);
        }
    }

    // ── JSON → ParsedModel 変換 ────────────────────────────────────

    private static ParsedModel parse(EntityModelData raw) {
        ParsedModel model = new ParsedModel();

        // ① ボーンツリー構築
        model.boneTree = buildBoneTree(raw.armature.hierarchy, null);

        // ② ボーン名→インデックスマップ
        model.boneIndexMap = new HashMap<>();
        for (int i = 0; i < raw.armature.joints.size(); i++) {
            model.boneIndexMap.put(raw.armature.joints.get(i), i);
        }
        model.boneCount = raw.armature.joints.size();

        // ③ メッシュデータ（最初のメッシュのみ、複数対応は拡張可能）
        if (raw.meshes != null && !raw.meshes.isEmpty()) {
            EntityModelData.MeshData meshData = raw.meshes.values().iterator().next();
            model.positions  = meshData.positions.toFloatArray();
            model.uvs        = meshData.uvs.toFloatArray();
            model.normals    = meshData.normals.toFloatArray();
            model.vcounts    = meshData.vcounts.toIntArray();
            model.weights    = meshData.weights.toFloatArray();
            model.vindices   = meshData.vindices.toIntArray();
            // パーツ（noGroups が通常の描画対象）
            if (meshData.parts.containsKey("noGroups")) {
                model.indexBuffer = meshData.parts.get("noGroups").toIntArray();
            }
        }

        // ④ アニメーション
        if (raw.animation != null) {
            model.animation = parseAnimation(raw.animation);
        }

        // ⑤ OBBコライダー
        if (raw.colliders != null) {
            model.colliders = raw.colliders;
        }

        return model;
    }

    private static ParsedModel.BoneNode buildBoneTree(
            List<EntityModelData.BoneNode> nodes,
            ParsedModel.BoneNode parent) {

        // 複数ルートを単一ルート（"Root"）でまとめる
        ParsedModel.BoneNode root = new ParsedModel.BoneNode();
        root.name = "Root";
        root.localMatrix = new Matrix4f(); // Identity
        root.parent = parent;
        root.children = new ArrayList<>();

        for (EntityModelData.BoneNode raw : nodes) {
            ParsedModel.BoneNode node = parseBoneNode(raw, root);
            root.children.add(node);
        }
        return root;
    }

    private static ParsedModel.BoneNode parseBoneNode(
            EntityModelData.BoneNode raw,
            ParsedModel.BoneNode parent) {

        ParsedModel.BoneNode node = new ParsedModel.BoneNode();
        node.name = raw.name;
        node.parent = parent;
        node.children = new ArrayList<>();

        // 16要素配列 → Matrix4f（行優先）
        if (raw.transform != null && raw.transform.length == 16) {
            node.localMatrix = new Matrix4f(
                    raw.transform[0],  raw.transform[1],  raw.transform[2],  raw.transform[3],
                    raw.transform[4],  raw.transform[5],  raw.transform[6],  raw.transform[7],
                    raw.transform[8],  raw.transform[9],  raw.transform[10], raw.transform[11],
                    raw.transform[12], raw.transform[13], raw.transform[14], raw.transform[15]
            );
        } else {
            node.localMatrix = new Matrix4f();
        }

        if (raw.children != null) {
            for (EntityModelData.BoneNode child : raw.children) {
                node.children.add(parseBoneNode(child, node));
            }
        }

        return node;
    }

    private static ParsedModel.ParsedAnimation parseAnimation(EntityModelData.AnimationData raw) {
        ParsedModel.ParsedAnimation anim = new ParsedModel.ParsedAnimation();
        anim.name = raw.name;
        anim.tracks = new HashMap<>();

        if (raw.frames == null) return anim;

        for (EntityModelData.BoneAnimTrack track : raw.frames) {
            ParsedModel.BoneTrack bt = new ParsedModel.BoneTrack();
            bt.boneName = track.name;
            bt.times = track.time.stream().mapToDouble(Float::doubleValue).toArray();
            bt.matrices = new Matrix4f[track.time.size()];

            for (int i = 0; i < track.time.size(); i++) {
                if (track.transform != null && i < track.transform.size()) {
                    // ATTRフォーマット → Matrix4f
                    EntityModelData.TransformAttr attr = track.transform.get(i);
                    Quaternionf q = new Quaternionf(attr.rot[1], attr.rot[2], attr.rot[3], attr.rot[0]);
                    bt.matrices[i] = new Matrix4f()
                            .translate(attr.loc[0], attr.loc[1], attr.loc[2])
                            .rotate(q)
                            .scale(attr.sca[0], attr.sca[1], attr.sca[2]);
                } else if (track.matrix != null && i < track.matrix.size()) {
                    float[] m = track.matrix.get(i);
                    bt.matrices[i] = new Matrix4f(
                            m[0],m[1],m[2],m[3], m[4],m[5],m[6],m[7],
                            m[8],m[9],m[10],m[11], m[12],m[13],m[14],m[15]);
                } else {
                    bt.matrices[i] = new Matrix4f();
                }
            }
            anim.tracks.put(track.name, bt);
        }
        return anim;
    }
}