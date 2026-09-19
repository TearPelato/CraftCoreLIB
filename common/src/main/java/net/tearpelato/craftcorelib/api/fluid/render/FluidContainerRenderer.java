package net.tearpelato.craftcorelib.api.fluid.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.AABB;
import net.tearpelato.craftcorelib.api.fluid.blockentity.FluidContainerBlockEntity;
import net.tearpelato.craftcorelib.platform.services.IFluidRenderHelper;
import org.joml.Matrix4f;

public class FluidContainerRenderer {

    public static void drawContainer(Level level, BlockPos pos, FluidContainerBlockEntity be,
                                     AABB box, PoseStack ms, MultiBufferSource buf, int light) {
        if (be.isEmpty()) return;
        Fluid fluid = be.getFluid();
        IFluidRenderHelper helper = IFluidRenderHelper.get();

        ResourceLocation stillTexture = helper.getStillTexture(fluid);
        @SuppressWarnings("deprecation")
        TextureAtlasSprite still = Minecraft.getInstance()
                .getTextureAtlas(TextureAtlas.LOCATION_BLOCKS).apply(stillTexture);

        int tint = helper.getTintColor(fluid, level, pos);
        float a = ((tint >> 24) & 0xFF) / 255f;
        float r = ((tint >> 16) & 0xFF) / 255f;
        float g = ((tint >>  8) & 0xFF) / 255f;
        float b = ( tint        & 0xFF) / 255f;

        FluidState state = fluid.defaultFluidState();
        VertexConsumer vc = buf.getBuffer(helper.getRenderLayer(state));

        float fullness = (float) be.getAmount() / be.getCapacity();
        float y = (float) box.minY + (float)(box.maxY - box.minY) * fullness;
        y = Math.clamp(y, (float) box.minY, (float) box.maxY);

        float u0 = still.getU0() + (still.getU1() - still.getU0()) * (float)(box.minX - Math.floor(box.minX));
        float u1 = still.getU0() + (still.getU1() - still.getU0()) * (float)(box.maxX - Math.floor(box.minX));
        float v0 = still.getV0() + (still.getV1() - still.getV0()) * (float)(box.minZ - Math.floor(box.minZ));
        float v1 = still.getV0() + (still.getV1() - still.getV0()) * (float)(box.maxZ - Math.floor(box.minZ));

        Matrix4f mat = ms.last().pose();
        vc.addVertex(mat, (float) box.minX, y, (float) box.minZ).setColor(r,g,b,a).setUv(u0,v0).setLight(light).setNormal(0,1,0);
        vc.addVertex(mat, (float) box.minX, y, (float) box.maxZ).setColor(r,g,b,a).setUv(u0,v1).setLight(light).setNormal(0,1,0);
        vc.addVertex(mat, (float) box.maxX, y, (float) box.maxZ).setColor(r,g,b,a).setUv(u1,v1).setLight(light).setNormal(0,1,0);
        vc.addVertex(mat, (float) box.maxX, y, (float) box.minZ).setColor(r,g,b,a).setUv(u1,v0).setLight(light).setNormal(0,1,0);
    }

    public static AABB createRotatedBox(Direction dir, double minX, double minY, double minZ,
                                        double maxX, double maxY, double maxZ) {
        minX /= 16.0; minY /= 16.0; minZ /= 16.0;
        maxX /= 16.0; maxY /= 17.0; maxZ /= 16.0;
        return switch (dir) {
            case WEST  -> new AABB(1-maxX, minY, 1-maxZ, 1-minX, maxY, 1-minZ);
            case NORTH -> new AABB(  minZ, minY, 1-maxX,   maxZ, maxY, 1-minX);
            case SOUTH -> new AABB(1-minZ, minY,   minX, 1-maxZ, maxY,   maxX);
            default    -> new AABB(  minX, minY,   minZ,   maxX, maxY,   maxZ);
        };
    }
}