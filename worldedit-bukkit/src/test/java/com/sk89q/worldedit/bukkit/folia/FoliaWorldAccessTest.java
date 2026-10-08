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

import com.sk89q.worldedit.history.change.BlockChange;
import com.sk89q.worldedit.history.change.Change;
import com.sk89q.worldedit.history.changeset.ChangeSet;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.util.formatting.WorldEditText;
import com.sk89q.worldedit.util.formatting.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.enginehub.piston.util.TextHelper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class FoliaWorldAccessTest {
    private MockedStatic<WorldEditText> text;

    @BeforeEach
    void provideTextRenderingWithoutAServerPlatform() {
        text = mockStatic(WorldEditText.class);
        text.when(() -> WorldEditText.reduceToText(any(Component.class), isNull()))
            .thenAnswer(invocation -> TextHelper.reduceToText(invocation.getArgument(0)));
    }

    @AfterEach
    void closeTextRendering() {
        text.close();
    }

    @Test
    void refusesForeignChunksWithoutLoadingThem() {
        World world = mock(World.class);
        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
            bukkit.when(() -> Bukkit.isOwnedByCurrentRegion(any(World.class), anyInt(), anyInt())).thenReturn(false);
            assertThrows(IllegalStateException.class,
                () -> FoliaWorldAccess.checkRegion(world, BlockVector3.ZERO, BlockVector3.at(12000, 0, 0)));
            verify(world, never()).getChunkAt(anyInt(), anyInt());
        }
    }

    @Test
    void validatesNeighbourChunksEvenWhenTheTargetChunkIsLoaded() {
        World world = mock(World.class);
        when(world.isChunkLoaded(anyInt(), anyInt())).thenReturn(true);
        when(world.isChunkLoaded(1, 0)).thenReturn(false);
        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
            bukkit.when(() -> Bukkit.isOwnedByCurrentRegion(any(World.class), anyInt(), anyInt())).thenReturn(true);
            assertThrows(IllegalStateException.class,
                () -> FoliaWorldAccess.checkRegion(world, BlockVector3.ZERO, BlockVector3.ZERO));
            verify(world, never()).getChunkAt(anyInt(), anyInt());
        }
    }

    @Test
    void acceptsOwnedLoadedRegionWithNegativeCoordinates() {
        World world = mock(World.class);
        when(world.isChunkLoaded(anyInt(), anyInt())).thenReturn(true);
        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
            bukkit.when(() -> Bukkit.isOwnedByCurrentRegion(any(World.class), anyInt(), anyInt())).thenReturn(true);
            assertDoesNotThrow(() -> FoliaWorldAccess.checkRegion(world,
                BlockVector3.at(-16, -60, -16), BlockVector3.at(-1, -50, -1)));
            verify(world).isChunkLoaded(-2, -2);
            verify(world).isChunkLoaded(0, 0);
            verify(world, never()).getChunkAt(anyInt(), anyInt());
        }
    }

    @Test
    void checksTheWholeHistoryBeforeItCanBeReplayed() {
        World world = mock(World.class);
        when(world.isChunkLoaded(anyInt(), anyInt())).thenReturn(true);
        BlockChange local = mock(BlockChange.class);
        BlockChange foreign = mock(BlockChange.class);
        when(local.position()).thenReturn(BlockVector3.ZERO);
        when(foreign.position()).thenReturn(BlockVector3.at(12000, 0, 0));
        ChangeSet history = mock(ChangeSet.class);
        when(history.forwardIterator()).thenReturn(List.<Change>of(local, foreign).iterator());
        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
            bukkit.when(() -> Bukkit.isOwnedByCurrentRegion(any(World.class), anyInt(), anyInt()))
                .thenAnswer(invocation -> (int) invocation.getArgument(1) < 100);
            assertThrows(IllegalStateException.class, () -> FoliaWorldAccess.checkHistory(world, history));
            verify(world, never()).getChunkAt(anyInt(), anyInt());
        }
    }
}
