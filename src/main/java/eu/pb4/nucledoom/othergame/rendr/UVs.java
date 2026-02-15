package eu.pb4.nucledoom.othergame.rendr;

import net.minecraft.core.Direction;
import org.joml.Vector3fc;

public record UVs(float minU, float minV, float maxU, float maxV) {
      public float getVertexU(final int index) {
         return index != 0 && index != 1 ? this.maxU : this.minU;
      }

      public float getVertexV(final int index) {
         return index != 0 && index != 3 ? this.maxV : this.minV;
      }


    public static UVs defaultFaceUV(final Vector3fc from, final Vector3fc to, final Direction facing) {
        return switch (facing) {
            case DOWN -> new UVs(from.x(), 16.0F - to.z(), to.x(), 16.0F - from.z());
            case UP -> new UVs(from.x(), from.z(), to.x(), to.z());
            case NORTH -> new UVs(16.0F - to.x(), 16.0F - to.y(), 16.0F - from.x(), 16.0F - from.y());
            case SOUTH -> new UVs(from.x(), 16.0F - to.y(), to.x(), 16.0F - from.y());
            case WEST -> new UVs(from.z(), 16.0F - to.y(), to.z(), 16.0F - from.y());
            case EAST -> new UVs(16.0F - to.z(), 16.0F - to.y(), 16.0F - from.z(), 16.0F - from.y());
            default -> throw new MatchException(null, null);
        };
    }
   }