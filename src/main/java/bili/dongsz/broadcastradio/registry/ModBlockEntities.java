package bili.dongsz.broadcastradio.registry;

import bili.dongsz.broadcastradio.BroadcastRadio;
import bili.dongsz.broadcastradio.block.entity.SimpleRadioBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModBlockEntities {
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = 
            DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, BroadcastRadio.MOD_ID);
    
    public static final RegistryObject<BlockEntityType<SimpleRadioBlockEntity>> SIMPLE_RADIO_BLOCK_ENTITY = 
            BLOCK_ENTITIES.register("simple_radio_block_entity", 
                    () -> BlockEntityType.Builder.of(SimpleRadioBlockEntity::new, 
                            ModBlocks.SIMPLE_RADIO_BLOCK.get()).build(null));
}