package scot.oskar.galaxyfactions.event

import com.hypixel.hytale.component.Ref
import com.hypixel.hytale.event.IEvent
import com.hypixel.hytale.math.vector.Transform
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore
import java.util.UUID

class PlayerMoveEvent(
    val oldTransform: Transform,
    val newTransform: Transform,
    val ref: Ref<EntityStore>
) : IEvent<PlayerMoveEvent>