package net.aoba.managers;

import net.aoba.Aoba;
import net.aoba.event.events.TickEvent;
import net.aoba.event.listeners.TickListener;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.chunk.LevelChunk;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import static net.aoba.AobaClient.MC;

public class EntityManager implements TickListener
{
    private volatile List<Player> players;
    private volatile List<Entity> entities;
    private volatile List<BlockEntity> blockEntities;

    public EntityManager() {
        players = Collections.emptyList();
        entities = Collections.emptyList();
        blockEntities = Collections.emptyList();

        Aoba.getInstance().eventManager.AddListener(TickListener.class, this);
    }

    @Override
    public void onTick(TickEvent.Pre event)
    {
        update();
    }

    @Override
    public void onTick(TickEvent.Post event)
    {
        update();
    }

    private void update() {
        if (MC.level != null && MC.player != null)
        {
            setLists(
                    new ArrayList<>(getEntities(false)),
                    new ArrayList<>(getPlayers(false)),
                    new ArrayList<>(getBlockEntities(false)));
        }
        else
        {
            setLists(Collections.emptyList(),
                    Collections.emptyList(),
                    Collections.emptyList());
        }
    }

    private void setLists(List<Entity> loadedEntities,
                          List<Player> playerEntities,
                          List<BlockEntity> loadedBlockEntities)
    {
        entities = loadedEntities;
        players = playerEntities;
        blockEntities = loadedBlockEntities;
    }

    public List<Entity> getEntities()
    {
        return entities;
    }

    public List<Player> getPlayers()
    {
        return players;
    }

    public List<BlockEntity> getBlockEntities()
    {
        return blockEntities;
    }

    public List<Entity> getEntitiesAsync()
    {
        return getEntities(!MC.isSameThread());
    }

    public List<Player> getPlayersAsync()
    {
        return getPlayers(!MC.isSameThread());
    }

    public List<BlockEntity> getBlockEntitiesAsync()
    {
        return getBlockEntities(!MC.isSameThread());
    }

    public List<Entity> getEntities(boolean async)
    {
        if (async)
        {
            return entities;
        }
        if (MC.level == null)
        {
            return Collections.emptyList();
        }
        List<Entity> entityList = new ArrayList<>();
        MC.level.entitiesForRendering().forEach(entityList::add);
        return entityList;
    }

    public List<Player> getPlayers(boolean async)
    {
        if (async)
        {
            return players;
        }
        return MC.level == null ? Collections.emptyList() : new ArrayList<>(MC.level.players());
    }

    public List<BlockEntity> getBlockEntities(boolean async)
    {
        if (async)
        {
            return blockEntities;
        }
        if (MC.level == null || MC.player == null)
        {
            return Collections.emptyList();
        }

        List<BlockEntity> list = new ArrayList<>();
        int chunkDistance = Math.min(MC.options.renderDistance().get(), 4);

        for (int x = -chunkDistance; x <= chunkDistance; x++)
        {
            for (int z = -chunkDistance; z <= chunkDistance; z++)
            {
                LevelChunk chunk = MC.level.getChunkSource().getChunkNow((int) MC.player.getX() / 16 + x, (int) MC.player.getZ() / 16 + z);

                if (chunk != null)
                {
                    list.addAll(chunk.getBlockEntities().values());
                }
            }
        }
        return list;
    }

    public Entity getEntity(int id)
    {
        List<Entity> entities = getEntitiesAsync();
        if (entities != null)
        {
            return entities.stream()
                    .filter(e -> e != null && e.getId() == id)
                    .findFirst()
                    .orElse(null);
        }

        return null;
    }
}
