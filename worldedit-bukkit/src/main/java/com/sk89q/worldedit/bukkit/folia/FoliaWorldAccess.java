/*
 * WorldEdit, a Minecraft world manipulation toolkit
 * Copyright (C) sk89q <http://www.sk89q.com>
 * Copyright (C) WorldEdit team and contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.sk89q.worldedit.bukkit.folia;

import com.sk89q.worldedit.history.change.BiomeChange;
import com.sk89q.worldedit.history.change.BiomeChange3D;
import com.sk89q.worldedit.history.change.BlockChange;
import com.sk89q.worldedit.history.change.Change;
import com.sk89q.worldedit.history.changeset.ChangeSet;
import com.sk89q.worldedit.math.BlockVector2;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.regions.RegionOperationException;
import com.sk89q.worldedit.util.formatting.text.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.World;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

/** Checks ownership without acquiring or loading any chunks. */
public final class FoliaWorldAccess {

    private FoliaWorldAccess() {
    }

    public static void checkRegion(World world, BlockVector3 minimum, BlockVector3 maximum) {
        // Include a chunk of neighbours: editing also runs physics and block updates.
        int minX = (Math.min(minimum.x(), maximum.x()) >> 4) - 1;
        int maxX = (Math.max(minimum.x(), maximum.x()) >> 4) + 1;
        int minZ = (Math.min(minimum.z(), maximum.z()) >> 4) - 1;
        int maxZ = (Math.max(minimum.z(), maximum.z()) >> 4) + 1;
        for (int x = minX; x <= maxX; x++) {
            for (int z = minZ; z <= maxZ; z++) {
                checkChunk(world, x, z);
            }
        }
    }

    public static void checkRead(World world, BlockVector3 position) {
        checkChunk(world, position.x() >> 4, position.z() >> 4);
    }

    private static void checkChunk(World world, int x, int z) {
        if (!Bukkit.isOwnedByCurrentRegion(world, x, z) || !world.isChunkLoaded(x, z)) {
            throw new IllegalStateException(new RegionOperationException(TextComponent.of(
                "Folia: 目标或相邻区块未加载，或不属于当前区域。请靠近目标位置、缩小操作范围后重试。"
            )));
        }
    }

    @SuppressWarnings("deprecation")
    public static void checkHistory(World world, ChangeSet changes) {
        Set<BlockVector2> checked = new HashSet<>();
        Iterator<Change> iterator = changes.forwardIterator();
        while (iterator.hasNext()) {
            Change change = iterator.next();
            BlockVector3 position = switch (change) {
                case BlockChange block -> block.position();
                case BiomeChange3D biome -> biome.position();
                case BiomeChange biome -> biome.getPosition().toBlockVector3();
                default -> throw new IllegalStateException(new RegionOperationException(TextComponent.of(
                    "Folia: 此历史包含不支持的变更类型，尚未执行撤销或重做。"
                )));
            };
            if (checked.add(BlockVector2.at(position.x() >> 4, position.z() >> 4))) {
                checkRegion(world, position, position);
            }
        }
    }
}
