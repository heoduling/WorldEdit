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

import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.event.extent.EditSessionEvent;
import com.sk89q.worldedit.extent.AbstractDelegateExtent;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.util.eventbus.Subscribe;
import com.sk89q.worldedit.world.biome.BiomeType;
import com.sk89q.worldedit.world.block.BlockStateHolder;

public class FoliaExtentListener {

    @Subscribe
    public void onEditSessionCreation(EditSessionEvent event) {
        // Reject unsupported writes explicitly, including their neighbouring chunks.
        // Returning false silently consumes undo history and misreports command success.
        event.setExtent(new AbstractDelegateExtent(event.getExtent()) {

            @Override
            public <T extends BlockStateHolder<T>> boolean setBlock(BlockVector3 location, T block) throws WorldEditException {
                event.getWorld().checkRegionAccess(location, location);
                return super.setBlock(location, block);
            }

            @Override
            public boolean setBiome(BlockVector3 position, BiomeType biome) {
                event.getWorld().checkRegionAccess(position, position);
                return super.setBiome(position, biome);
            }
        });
    }
}
