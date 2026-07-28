/*
 * Aoba Hacked Client
 * Copyright (C) 2019-2024 coltonk9043
 *
 * Licensed under the GNU General Public License, Version 3 or later.
 * See <http://www.gnu.org/licenses/>.
 */

package net.aoba.event.events;

import net.minecraft.world.entity.Entity;

public abstract class EntityEvent extends AbstractEvent {
	private final Entity entity;
	private final byte id;

	public EntityEvent(Entity entity, byte id) {
		this.entity = entity;
		this.id = id;
	}

	public Entity getEntity() {
		return entity;
	}

	public byte getId() {
		return id;
	}
}
