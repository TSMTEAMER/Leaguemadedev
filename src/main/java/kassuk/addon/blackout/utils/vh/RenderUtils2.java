// Decompiled with: CFR 0.152
// Class Version: 17
package kassuk.addon.blackout.utils.vh;

import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.renderer.Renderer3D;
import meteordevelopment.meteorclient.renderer.ShapeMode;
import meteordevelopment.meteorclient.utils.render.color.Color;
import net.minecraft.block.BlockState;
import net.minecraft.block.ShapeContext;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.shape.VoxelShape;

public class RenderUtils2 {
    public static void renderBed(Renderer3D renderer, Color sideColor, Color lineColor, ShapeMode shapeMode, boolean fade, int ticks, int maxTicks, int beforeFadeDelay, Direction placeDirection, BlockPos pos) {
        RenderUtils2.renderBed(renderer, sideColor, sideColor, lineColor, lineColor, shapeMode, fade, ticks, maxTicks, beforeFadeDelay, placeDirection, pos, false);
    }

    public static void renderBed(Renderer3D renderer, Color sideColor, Color sideColor2, Color lineColor, Color lineColor2, ShapeMode shapeMode, boolean fade, float ticks, int maxTicks, int beforeFadeDelay, Direction placeDirection, BlockPos pos, boolean shrink) {
        float factor;
        int preSideA = sideColor.a;
        int preLineA = lineColor.a;
        int preSideA2 = sideColor.a;
        int preLineA2 = lineColor.a;
        float f = factor = ticks > (float)(maxTicks - beforeFadeDelay) ? 1.0f : ticks / (float)maxTicks;
        if (fade) {
            sideColor.a = (int)((float)sideColor.a * factor);
            lineColor.a = (int)((float)lineColor.a * factor);
            sideColor2.a = (int)((float)sideColor2.a * factor);
            lineColor2.a = (int)((float)lineColor2.a * factor);
        }
        float x = pos.getX();
        float y = pos.getY();
        float z = pos.getZ();
        float xOffset = 1.0f;
        float zOffset = 2.0f;
        Color xOffLineColor = lineColor2;
        Color zOffLineColor = lineColor;
        Color xOffSideColor = sideColor2;
        Color zOffSideColor = sideColor;
        switch (placeDirection) {
            case NORTH: {
                z -= 1.0f;
                break;
            }
            case WEST: {
                x -= 1.0f;
            }
            case EAST: {
                xOffset = 2.0f;
                zOffset = 1.0f;
                xOffLineColor = lineColor;
                zOffLineColor = lineColor2;
                xOffSideColor = sideColor;
                zOffSideColor = sideColor2;
            }
        }
        float one = 0.0625f;
        float three = 0.1875f;
        float nine = 0.5625f;
        if (shrink) {
            x = (float)((double)x + (double)(xOffset - (xOffset *= factor)) * 0.5);
            y = (float)((double)y + (double)(nine - (nine *= factor)) * 0.5);
            z = (float)((double)z + (double)(zOffset - (zOffset *= factor)) * 0.5);
            three *= factor;
        }
        if (shapeMode.lines()) {
            renderer.line(x, y + nine, z, x + xOffset, y + nine, z, lineColor2, xOffLineColor);
            renderer.line(x + three, y + three, z, x + xOffset - three, y + three, z, lineColor2, xOffLineColor);
            renderer.line(x, y + nine, z + zOffset, x + xOffset, y + nine, z + zOffset, zOffLineColor, lineColor);
            renderer.line(x + three, y + three, z + zOffset, x + xOffset - three, y + three, z + zOffset, zOffLineColor, lineColor);
            renderer.line(x, y + nine, z, x, y + nine, z + zOffset, lineColor2, zOffLineColor);
            renderer.line(x, y + three, z + three, x, y + three, z + zOffset - three, lineColor2, zOffLineColor);
            renderer.line(x + xOffset, y + nine, z, x + xOffset, y + nine, z + zOffset, xOffLineColor, lineColor);
            renderer.line(x + xOffset, y + three, z + three, x + xOffset, y + three, z + zOffset - three, xOffLineColor, lineColor);
            renderer.line(x, y + three, z + three, x + three, y + three, z + three, lineColor2);
            renderer.line(x + three, y + three, z, x + three, y + three, z + three, lineColor2);
            renderer.line(x, y, z, x + three, y, z, lineColor2);
            renderer.line(x, y, z + three, x + three, y, z + three, lineColor2);
            renderer.line(x, y, z, x, y, z + three, lineColor2);
            renderer.line(x + three, y, z, x + three, y, z + three, lineColor2);
            renderer.line(x, y, z, x, y + nine, z, lineColor2);
            renderer.line(x + three, y, z, x + three, y + three, z, lineColor2);
            renderer.line(x + three, y, z + three, x + three, y + three, z + three, lineColor2);
            renderer.line(x, y, z + three, x, y + three, z + three, lineColor2);
            renderer.line(x + xOffset - three, y + three, z, x + xOffset - three, y + three, z + three, xOffLineColor);
            renderer.line(x + xOffset - three, y + three, z + three, x + xOffset, y + three, z + three, xOffLineColor);
            renderer.line(x + xOffset - three, y, z, x + xOffset, y, z, xOffLineColor);
            renderer.line(x + xOffset - three, y, z + three, x + xOffset, y, z + three, xOffLineColor);
            renderer.line(x + xOffset, y, z, x + xOffset, y, z + three, xOffLineColor);
            renderer.line(x + xOffset - three, y, z, x + xOffset - three, y, z + three, xOffLineColor);
            renderer.line(x + xOffset, y, z, x + xOffset, y + nine, z, xOffLineColor);
            renderer.line(x + xOffset - three, y, z, x + xOffset - three, y + three, z, xOffLineColor);
            renderer.line(x + xOffset - three, y, z + three, x + xOffset - three, y + three, z + three, xOffLineColor);
            renderer.line(x + xOffset, y, z + three, x + xOffset, y + three, z + three, xOffLineColor);
            renderer.line(x, y + three, z + zOffset - three, x + three, y + three, z + zOffset - three, zOffLineColor);
            renderer.line(x + three, y + three, z + zOffset - three, x + three, y + three, z + zOffset, zOffLineColor);
            renderer.line(x, y, z + zOffset, x + three, y, z + zOffset, zOffLineColor);
            renderer.line(x, y, z + zOffset - three, x + three, y, z + zOffset - three, zOffLineColor);
            renderer.line(x, y, z + zOffset - three, x, y, z + zOffset, zOffLineColor);
            renderer.line(x + three, y, z + zOffset - three, x + three, y, z + zOffset, zOffLineColor);
            renderer.line(x, y, z + zOffset, x, y + nine, z + zOffset, zOffLineColor);
            renderer.line(x + three, y, z + zOffset, x + three, y + three, z + zOffset, zOffLineColor);
            renderer.line(x + three, y, z + zOffset - three, x + three, y + three, z + zOffset - three, zOffLineColor);
            renderer.line(x, y, z + zOffset - three, x, y + three, z + zOffset - three, zOffLineColor);
            renderer.line(x + xOffset - three, y + three, z + zOffset - three, x + xOffset - three, y + three, z + zOffset, lineColor);
            renderer.line(x + xOffset - three, y + three, z + zOffset - three, x + xOffset, y + three, z + zOffset - three, lineColor);
            renderer.line(x + xOffset - three, y, z + zOffset, x + xOffset, y, z + zOffset, lineColor);
            renderer.line(x + xOffset - three, y, z + zOffset - three, x + xOffset, y, z + zOffset - three, lineColor);
            renderer.line(x + xOffset, y, z + zOffset - three, x + xOffset, y, z + zOffset, lineColor);
            renderer.line(x + xOffset - three, y, z + zOffset - three, x + xOffset - three, y, z + zOffset, lineColor);
            renderer.line(x + xOffset, y, z + zOffset, x + xOffset, y + nine, z + zOffset, lineColor);
            renderer.line(x + xOffset - three, y, z + zOffset, x + xOffset - three, y + three, z + zOffset, lineColor);
            renderer.line(x + xOffset - three, y, z + zOffset - three, x + xOffset - three, y + three, z + zOffset - three, lineColor);
            renderer.line(x + xOffset, y, z + zOffset - three, x + xOffset, y + three, z + zOffset - three, lineColor);
        }
        if (shapeMode.sides()) {
            RenderUtils2.gradientQuadHorizontal(renderer, x, y + nine, z, x + xOffset, z + zOffset, zOffSideColor, sideColor, xOffSideColor, sideColor2);
            RenderUtils2.gradientQuadHorizontal(renderer, x + three, y + three, z + three, x + xOffset - three, z + zOffset - three, zOffSideColor, sideColor, xOffSideColor, sideColor2);
            RenderUtils2.gradientQuadHorizontal(renderer, x, y + three, z + three, x + three, z + zOffset - three, zOffSideColor, sideColor2);
            RenderUtils2.gradientQuadHorizontal(renderer, x + xOffset - three, y + three, z + three, x + xOffset, z + zOffset - three, sideColor, xOffSideColor);
            RenderUtils2.gradientQuadHorizontal2(renderer, x + three, y + three, z, x + xOffset - three, z + three, xOffSideColor, sideColor2);
            RenderUtils2.gradientQuadHorizontal2(renderer, x + three, y + three, z + zOffset - three, x + xOffset - three, z + zOffset, sideColor, zOffSideColor);
            RenderUtils2.gradientQuadVertical(renderer, x + three, y + three, z, x + xOffset - three, y + nine, z, xOffSideColor, sideColor2);
            RenderUtils2.gradientQuadVertical(renderer, x + three, y + three, z + zOffset, x + xOffset - three, y + nine, z + zOffset, sideColor, zOffSideColor);
            RenderUtils2.gradientQuadVertical(renderer, x, y + three, z + three, x, y + nine, z + zOffset - three, zOffSideColor, sideColor2);
            RenderUtils2.gradientQuadVertical(renderer, x + xOffset, y + three, z + three, x + xOffset, y + nine, z + zOffset - three, sideColor, xOffSideColor);
            renderer.quadHorizontal(x, y, z, x + three, z + three, sideColor2);
            renderer.quadVertical(x, y, z, x + three, y + nine, z, sideColor2);
            renderer.quadVertical(x, y, z, x, y + nine, z + three, sideColor2);
            renderer.quadVertical(x, y, z + three, x + three, y + three, z + three, sideColor2);
            renderer.quadVertical(x + three, y, z, x + three, y + three, z + three, sideColor2);
            renderer.quadHorizontal(x + xOffset, y, z, x + xOffset - three, z + three, xOffSideColor);
            renderer.quadVertical(x + xOffset - three, y, z, x + xOffset, y + nine, z, xOffSideColor);
            renderer.quadVertical(x + xOffset, y, z, x + xOffset, y + nine, z + three, xOffSideColor);
            renderer.quadVertical(x + xOffset - three, y, z + three, x + xOffset, y + three, z + three, xOffSideColor);
            renderer.quadVertical(x + xOffset - three, y, z, x + xOffset - three, y + three, z + three, xOffSideColor);
            renderer.quadHorizontal(x, y, z + zOffset - three, x + three, z + zOffset, zOffSideColor);
            renderer.quadVertical(x, y, z + zOffset, x + three, y + nine, z + zOffset, zOffSideColor);
            renderer.quadVertical(x, y, z + zOffset - three, x, y + nine, z + zOffset, zOffSideColor);
            renderer.quadVertical(x, y, z + zOffset - three, x + three, y + three, z + zOffset - three, zOffSideColor);
            renderer.quadVertical(x + three, y, z + zOffset - three, x + three, y + three, z + zOffset, zOffSideColor);
            renderer.quadHorizontal(x + xOffset, y, z + zOffset - three, x + xOffset - three, z + zOffset, sideColor);
            renderer.quadVertical(x + xOffset - three, y, z + zOffset, x + xOffset, y + nine, z + zOffset, sideColor);
            renderer.quadVertical(x + xOffset, y, z + zOffset - three, x + xOffset, y + nine, z + zOffset, sideColor);
            renderer.quadVertical(x + xOffset - three, y, z + zOffset - three, x + xOffset, y + three, z + zOffset - three, sideColor);
            renderer.quadVertical(x + xOffset - three, y, z + zOffset - three, x + xOffset - three, y + three, z + zOffset, sideColor);
        }
        sideColor.a = preSideA;
        lineColor.a = preLineA;
        sideColor2.a = preSideA2;
        lineColor2.a = preLineA2;
    }

    public static void renderState(Renderer3D renderer, BlockState state, BlockPos pos, ShapeMode shapeMode, Color sideColor, Color lineColor) {
        VoxelShape shape = state.getOutlineShape(MeteorClient.mc.world, pos, ShapeContext.absent());
        if (shape.isEmpty()) {
            return;
        }
        if (shapeMode == ShapeMode.Both || shapeMode == ShapeMode.Lines) {
            shape.forEachEdge((minX, minY, minZ, maxX, maxY, maxZ) -> renderer.line((double)pos.getX() + minX, (double)pos.getY() + minY, (double)pos.getZ() + minZ, (double)pos.getX() + maxX, (double)pos.getY() + maxY, (double)pos.getZ() + maxZ, lineColor));
        }
        if (shapeMode == ShapeMode.Both || shapeMode == ShapeMode.Sides) {
            for (Box b : shape.getBoundingBoxes()) {
                RenderUtils2.render(renderer, pos, b, shapeMode, sideColor, lineColor);
            }
        }
    }

    private static void render(Renderer3D renderer, BlockPos blockPos, Box box, ShapeMode shapeMode, Color sideColor, Color lineColor) {
        renderer.box((double)blockPos.getX() + box.minX, (double)blockPos.getY() + box.minY, (double)blockPos.getZ() + box.minZ, (double)blockPos.getX() + box.maxX, (double)blockPos.getY() + box.maxY, (double)blockPos.getZ() + box.maxZ, sideColor, lineColor, shapeMode, 0);
    }

    public static void gradientQuadHorizontal(Renderer3D renderer, double x1, double y, double z1, double x2, double z2, Color color, Color color2) {
        renderer.quad(x1, y, z1, x1, y, z2, x2, y, z2, x2, y, z1, color, color, color2, color2);
    }

    public static void gradientQuadHorizontal2(Renderer3D renderer, double x1, double y, double z1, double x2, double z2, Color color, Color color2) {
        renderer.quad(x1, y, z1, x1, y, z2, x2, y, z2, x2, y, z1, color2, color, color, color2);
    }

    public static void gradientQuadHorizontal(Renderer3D renderer, double x1, double y, double z1, double x2, double z2, Color topLeft, Color topRight, Color bottomRight, Color bottomLeft) {
        renderer.quad(x1, y, z1, x1, y, z2, x2, y, z2, x2, y, z1, topLeft, topRight, bottomRight, bottomLeft);
    }

    public static void gradientQuadVertical(Renderer3D renderer, double x1, double y1, double z1, double x2, double y2, double z2, Color color1, Color color2) {
        renderer.quad(x1, y1, z1, x1, y2, z1, x2, y2, z2, x2, y1, z2, color2, color1, color1, color2);
    }
}
