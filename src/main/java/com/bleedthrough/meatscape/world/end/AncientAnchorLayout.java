package com.bleedthrough.meatscape.world.end;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Blocks;

/** Small surface marker next to a natural wormhole, confined to its generating chunk. */
public final class AncientAnchorLayout {
    private AncientAnchorLayout() { }

    public record Site(BlockPos anchor, Direction facing, List<BlockPos> markers) { }

    public static Optional<Site> find(LevelAccessor level, BlockPos wormhole, ChunkPos chunk) {
        for (Direction direction : Direction.Plane.HORIZONTAL) {
            BlockPos anchor = wormhole.relative(direction, 2);
            List<BlockPos> markers = new ArrayList<>(4);
            for (Direction side : Direction.Plane.HORIZONTAL) markers.add(anchor.relative(side));
            if (!fits(chunk, anchor, markers)) continue;
            if (!surface(level, anchor)) continue;
            if (markers.stream().allMatch(pos -> surface(level, pos))) {
                return Optional.of(new Site(anchor, direction.getOpposite(), List.copyOf(markers)));
            }
        }
        return Optional.empty();
    }

    public static boolean fits(ChunkPos chunk, BlockPos anchor, List<BlockPos> markers) {
        if (!new ChunkPos(anchor).equals(chunk)) return false;
        return markers.stream().allMatch(pos -> new ChunkPos(pos).equals(chunk));
    }

    private static boolean surface(LevelAccessor level, BlockPos pos) {
        return !level.isOutsideBuildHeight(pos) && level.isEmptyBlock(pos)
                && level.getBlockState(pos.below()).is(Blocks.END_STONE);
    }
}
