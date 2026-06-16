package sonar.fluxnetworks.common.util;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import sonar.fluxnetworks.api.FluxTranslate;
import sonar.fluxnetworks.api.device.FluxDeviceType;
import sonar.fluxnetworks.api.device.IFluxDevice;
import sonar.fluxnetworks.api.energy.EnergyType;
import sonar.fluxnetworks.common.connection.FluxNetwork;
import sonar.fluxnetworks.common.capability.FluxPlayer;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Collection;

public class FluxUtils {

    private static final double[] COMPACT_SCALE = new double[]{0.001D, 0.000_001D, 0.000_000_001D, 0.000_000_000_001D,
            0.000_000_000_000_001D, 0.000_000_000_000_000_001D};

    public static final Direction[] DIRECTIONS = Direction.values();

    private FluxUtils() {
    }

    @Nonnull
    public static <E extends Enum<E>> E cycle(@Nonnull E val, @Nonnull E[] values) {
        int next = val.ordinal() + 1;
        if (next < values.length) {
            return values[next];
        }
        return values[0];
    }

    @Nullable
    public static Direction getBlockDirection(@Nonnull BlockPos base, @Nonnull BlockPos target) {
        if (base.equals(target)) {
            return null;
        }
        var test = new BlockPos.MutableBlockPos();
        for (var dir : DIRECTIONS) {
            test.set(base);
            if (test.move(dir).equals(target)) {
                return dir;
            }
        }
        return null;
    }

    @Nonnull
    public static String getTransferInfo(@Nonnull IFluxDevice flux, EnergyType energyType) {
        FluxDeviceType type = flux.getDeviceType();
        long change = flux.getTransferChange();
        if (type.isPlug()) {
            if (change == 0) {
                return FluxTranslate.INPUT.get() + ": " + ChatFormatting.GOLD + "0 " + energyType.getUsageSuffix();
            } else {
                return FluxTranslate.INPUT.get() + ": " + ChatFormatting.GREEN + "+" + energyType.getUsage(change);
            }
        }
        if (type.isPoint() || type.isController()) {
            if (change == 0) {
                return FluxTranslate.OUTPUT.get() + ": " + ChatFormatting.GOLD + "0 " + energyType.getUsageSuffix();
            } else {
                return FluxTranslate.OUTPUT.get() + ": " + ChatFormatting.RED + energyType.getUsage(change);
            }
        }
        if (type.isStorage()) {
            if (change == 0) {
                return FluxTranslate.CHANGE.get() + ": " + ChatFormatting.GOLD + "0 " + energyType.getUsageSuffix();
            } else if (change > 0) {
                return FluxTranslate.CHANGE.get() + ": " + ChatFormatting.GREEN + "+" + energyType.getUsage(change);
            } else {
                return FluxTranslate.CHANGE.get() + ": " + ChatFormatting.RED + energyType.getUsage(change);
            }
        }
        return "";
    }

    public static void writeGlobalPos(@Nonnull CompoundTag tag, @Nonnull GlobalPos pos) {
        BlockPos p = pos.pos();
        tag.putInt("x", p.getX());
        tag.putInt("y", p.getY());
        tag.putInt("z", p.getZ());
        tag.putString("dim", pos.dimension().identifier().toString());
    }

    @Nonnull
    public static GlobalPos readGlobalPos(@Nonnull CompoundTag tag) {
        String dimName = tag.getStringOr("dim", "minecraft:overworld");
        Identifier dimId = Identifier.parse(dimName);
        int x = tag.getIntOr("x", 0);
        int y = tag.getIntOr("y", 0);
        int z = tag.getIntOr("z", 0);
        return GlobalPos.of(ResourceKey.create(Registries.DIMENSION, dimId), new BlockPos(x, y, z));
    }

    public static void writeGlobalPos(@Nonnull FriendlyByteBuf buffer, @Nonnull GlobalPos pos) {
        buffer.writeUtf(pos.dimension().identifier().toString());
        buffer.writeBlockPos(pos.pos());
    }

    @Nonnull
    public static GlobalPos readGlobalPos(@Nonnull FriendlyByteBuf buffer) {
        String dimName = buffer.readUtf();
        Identifier dimId = Identifier.parse(dimName);
        return GlobalPos.of(ResourceKey.create(Registries.DIMENSION, dimId), buffer.readBlockPos());
    }

    @Nonnull
    public static String getDisplayPos(@Nonnull GlobalPos pos) {
        BlockPos p = pos.pos();
        return "X: " + p.getX() + " Y: " + p.getY() + " Z: " + p.getZ();
    }

    @Nonnull
    public static String getDisplayDim(@Nonnull GlobalPos pos) {
        return pos.dimension().identifier().toString();
    }

    // MÉTODO REMOVIDO - Use FluxPlayer.get(player) diretamente

    @SuppressWarnings("unused")
    public static <T> boolean addWithCheck(@Nonnull Collection<T> list, @Nullable T toAdd) {
        if (toAdd != null && !list.contains(toAdd)) {
            list.add(toAdd);
            return true;
        }
        return false;
    }

    public static int getModifiedColor(int color, float factor) {
        int r = (color >> 16) & 0xff;
        int g = (color >> 8) & 0xff;
        int b = color & 0xff;

        int max = Math.max(r, Math.max(g, b));
        int min = Math.min(r, Math.min(g, b));

        int delta = max - min;

        if (delta == 0) {
            return Mth.hsvToRgb(0, 0, Math.min(factor * max / 255.0f, 1.0f));
        }

        float h;

        if (max == r) {
            h = (float) (g - b) / delta;
            if (h < 0.0f) {
                h += 6.0f;
            }
        } else if (max == g) {
            h = 2.0f + (float) (b - r) / delta;
        } else {
            h = 4.0f + (float) (r - g) / delta;
        }

        return Mth.hsvToRgb(h / 6.0f, Math.min(factor * delta / max, 1.0f), Math.min(factor * max / 255.0f, 1.0f));
    }

    public static String compact(long in) {
        if (in < 1000) {
            return Long.toString(in);
        }
        int level = (int) (Math.log10(in) / 3) - 1;
        char pre = "kMGTPE".charAt(level);
        return String.format("%.1f%c", in * COMPACT_SCALE[level], pre);
    }

    public static String compact(long in, String suffix) {
        if (in < 1000) {
            return in + " " + suffix;
        }
        int level = (int) (Math.log10(in) / 3) - 1;
        char pre = "kMGTPE".charAt(level);
        return String.format("%.1f %c%s", in * COMPACT_SCALE[level], pre, suffix);
    }

    public static boolean isBadNetworkName(@Nonnull String s) {
        return s.isEmpty() || s.length() > FluxNetwork.MAX_NETWORK_NAME_LENGTH;
    }

    public static boolean isBadPassword(@Nonnull String s) {
        if (s.isEmpty() || s.length() > FluxNetwork.MAX_PASSWORD_LENGTH) {
            return true;
        }
        for (int i = 0, e = s.length(); i < e; i++) {
            if (isBadPasswordChar(s.charAt(i))) {
                return true;
            }
        }
        return false;
    }

    public static boolean isBadPasswordChar(char c) {
        return c >= 0x7f || c <= 0x20;
    }

    public static float getRed(int color) {
        return (float) (color >> 16 & 255) / 255.0F;
    }

    public static float getGreen(int color) {
        return (float) (color >> 8 & 255) / 255.0F;
    }

    public static float getBlue(int color) {
        return (float) (color & 255) / 255.0F;
    }
}