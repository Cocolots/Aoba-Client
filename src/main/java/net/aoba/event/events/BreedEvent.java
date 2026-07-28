/*
 * Aoba Hacked Client
 * Copyright (C) 2019-2024 coltonk9043
 *
 * Licensed under the GNU General Public License, Version 3 or later.
 * See <http://www.gnu.org/licenses/>.
 */

package net.aoba.event.events;

import java.util.ArrayList;
import net.aoba.event.listeners.AbstractListener;
import net.aoba.event.listeners.BreedListener;
import net.minecraft.world.entity.Entity;

public class BreedEvent extends EntityEvent {

	public BreedEvent(Entity entity, byte id)  {
		super(entity, id);
	}

	@Override
	public void Fire(ArrayList<? extends AbstractListener> listeners) {
		for (AbstractListener listener : listeners) {
			BreedListener breedListener = (BreedListener) listener;
			breedListener.onBreed(this);
		}
	}

	@Override
	public Class<BreedListener> GetListenerClassType() {
		return BreedListener.class;
	}
}
