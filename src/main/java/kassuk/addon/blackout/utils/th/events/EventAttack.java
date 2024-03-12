package kassuk.addon.blackout.utils.th.events;

import net.minecraft.entity.Entity;
import kassuk.addon.blackout.utils.th.events.Event;


public class EventAttack extends Event {

    private Entity entity;

    public EventAttack(Entity entity){
        this.entity = entity;
    }

    public Entity getEntity(){
        return  entity;
    }
}