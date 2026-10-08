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

package com.sk89q.worldedit.internal.command;

import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.internal.annotation.Selection;
import com.sk89q.worldedit.regions.Region;
import org.enginehub.piston.CommandParameters;
import org.enginehub.piston.gen.CommandCallListener;
import org.enginehub.piston.inject.Key;

import java.lang.reflect.Method;
import java.util.Arrays;

/** Validate selected-region commands before their first world mutation. */
public final class RegionAccessHandler implements CommandCallListener {

    @Override
    public void beforeCall(Method method, CommandParameters parameters) {
        if (!Arrays.asList(method.getParameterTypes()).contains(EditSession.class)
            || Arrays.stream(method.getParameters()).noneMatch(p -> p.isAnnotationPresent(Selection.class))) {
            return;
        }
        EditSession editSession = parameters.injectedValue(Key.of(EditSession.class)).orElseThrow();
        Region region = parameters.injectedValue(Key.of(Region.class, Selection.class)).orElseThrow();
        if (editSession.getWorld() != null) {
            editSession.getWorld().checkRegionAccess(region.getMinimumPoint(), region.getMaximumPoint());
        }
    }
}
