package com.mimic.monstermod.particle.Tornado;

import com.mimic.monstermod.entity.obj.TornadoEntity;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.*;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import java.util.List;

public class TornadoParticle extends TextureSheetParticle {
    private final SpriteSet spriteSet;
    private double currentAngleOffset;
    private final double rotationSpeed;
    private final float tornadoHeight = 30.0f;
    private final float mouthExtendHeight = 6.0f;
    private final int mouthSegments = 12;
    private final int mouthDensity = 3;
    private int targetEntityId = -1;

    protected TornadoParticle(ClientLevel level, double x, double y, double z, double angle, double radius, double unused, SpriteSet spriteSet, float size) {
        super(level, x, y, z);
        this.spriteSet = spriteSet;
        this.pickSprite(spriteSet);
        this.currentAngleOffset = angle;
        this.rotationSpeed = 0.25;
        this.lifetime = 200;
        this.quadSize = size;

        // 初期判定ボックス設定（巨大化）
        updateTornadoBoundingBox();

        this.rCol = 0.85f; this.gCol = 0.95f; this.bCol = 1.0f;
        this.friction = 1.0f;
        this.alpha = 0.8f;

        List<TornadoEntity> entities = level.getEntitiesOfClass(TornadoEntity.class, this.getBoundingBox().inflate(10.0D));
        if (!entities.isEmpty()) {
            this.targetEntityId = entities.get(0).getId();
        }
    }

    @Override
    public void tick() {
        this.xo = this.x;
        this.yo = this.y;
        this.zo = this.z;

        if (this.age++ >= this.lifetime) {
            this.remove();
            return;
        }

        net.minecraft.world.entity.Entity target = level.getEntity(this.targetEntityId);
        if (target != null && target.isAlive()) {
            this.setPos(target.getX(), target.getY(), target.getZ());
            updateTornadoBoundingBox(); // 常に判定を維持
        } else if (this.age > 20) {
            this.remove();
        }
        this.currentAngleOffset += this.rotationSpeed;
    }

    private void updateTornadoBoundingBox() {
        // 半径40、高さ45の巨大な判定箱を設定し、カメラの角度によるカリングを防ぐ
        this.setBoundingBox(new AABB(this.x - 40, this.y - 5, this.z - 40, this.x + 40, this.y + 45, this.z + 40));
    }

    @Override
    public boolean shouldCull() {
        return false; // カリング（描画スキップ）を強制無効化
    }

    @Override
    public ParticleRenderType getRenderType() {
        return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
    }

    @Override
    public void render(VertexConsumer buffer, Camera camera, float partialTick) {
        Vec3 cameraPos = camera.getPosition();
        float centerX = (float)(Mth.lerp(partialTick, this.xo, this.x) - cameraPos.x());
        float centerY = (float)(Mth.lerp(partialTick, this.yo, this.y) - cameraPos.y());
        float centerZ = (float)(Mth.lerp(partialTick, this.zo, this.z) - cameraPos.z());
        float angleTime = (float)(this.currentAngleOffset + partialTick * this.rotationSpeed);

        // PART 1: 胴体
        for (float h = 0; h < tornadoHeight; h += 0.8f) {
            float hProgress = h / tornadoHeight;
            float radius = 3.0f + ((float) Math.pow(hProgress, 4.0) * 12.0f);
            float angle = angleTime + (hProgress * 10.0f);
            drawTornadoSegment(buffer, centerX + (float)Math.cos(angle) * radius, centerY + h, centerZ + (float)Math.sin(angle) * radius, angle, (float)Math.pow(hProgress, 4.0) * ((float)Math.PI / 2f), 1.0f);
        }

        // PART 2: 口
        for (int i = 0; i < mouthSegments; i++) {
            float progress = (float)i / (mouthSegments - 1);
            float radius = 15.0f + ((float)Math.pow(progress, 1.5) * 18.0f);
            float alphaFade = 1.0f - (progress * progress);
            for (int d = 0; d < mouthDensity; d++) {
                float angle = angleTime + 10.0f + (progress * 3.0f) + (d * 0.15f);
                drawTornadoSegment(buffer, centerX + (float)Math.cos(angle) * radius, centerY + tornadoHeight + (progress * mouthExtendHeight), centerZ + (float)Math.sin(angle) * radius, angle, (float)Math.PI / 2f, alphaFade);
            }
        }
    }

    private void drawTornadoSegment(VertexConsumer buffer, float x, float y, float z, float angle, float tilt, float alphaMultiplier) {
        Quaternionf quat = new Quaternionf().rotationY(-angle + (float)Math.PI / 2f).rotateX(tilt);
        Vector3f[] vertices = { new Vector3f(-1.2f, -1.0f, 0), new Vector3f(-1.2f, 1.0f, 0), new Vector3f(1.2f, 1.0f, 0), new Vector3f(1.2f, -1.0f, 0) };
        for (Vector3f v : vertices) v.rotate(quat).add(x, y, z);

        int light = 15728880;
        float a = this.alpha * alphaMultiplier;
        if (this.age > this.lifetime - 10) a *= (this.lifetime - this.age) / 10f;

        buffer.vertex(vertices[0].x(), vertices[0].y(), vertices[0].z()).uv(getU1(), getV1()).color(rCol, gCol, bCol, a).uv2(light).endVertex();
        buffer.vertex(vertices[1].x(), vertices[1].y(), vertices[1].z()).uv(getU1(), getV0()).color(rCol, gCol, bCol, a).uv2(light).endVertex();
        buffer.vertex(vertices[2].x(), vertices[2].y(), vertices[2].z()).uv(getU0(), getV0()).color(rCol, gCol, bCol, a).uv2(light).endVertex();
        buffer.vertex(vertices[3].x(), vertices[3].y(), vertices[3].z()).uv(getU0(), getV1()).color(rCol, gCol, bCol, a).uv2(light).endVertex();

        buffer.vertex(vertices[3].x(), vertices[3].y(), vertices[3].z()).uv(getU0(), getV1()).color(rCol, gCol, bCol, a).uv2(light).endVertex();
        buffer.vertex(vertices[2].x(), vertices[2].y(), vertices[2].z()).uv(getU0(), getV0()).color(rCol, gCol, bCol, a).uv2(light).endVertex();
        buffer.vertex(vertices[1].x(), vertices[1].y(), vertices[1].z()).uv(getU1(), getV0()).color(rCol, gCol, bCol, a).uv2(light).endVertex();
        buffer.vertex(vertices[0].x(), vertices[0].y(), vertices[0].z()).uv(getU1(), getV1()).color(rCol, gCol, bCol, a).uv2(light).endVertex();
    }

    public static class Provider implements ParticleProvider<TornadoParticleOptions> {
        private final SpriteSet spriteSet;
        public Provider(SpriteSet sprite) { this.spriteSet = sprite; }
        @Override
        public Particle createParticle(TornadoParticleOptions data, ClientLevel level, double x, double y, double z, double vx, double vy, double vz) {
            return new TornadoParticle(level, x, y, z, vx, vy, vz, this.spriteSet, data.getSize());
        }
    }
}