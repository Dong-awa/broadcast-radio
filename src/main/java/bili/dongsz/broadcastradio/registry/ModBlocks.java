package bili.dongsz.broadcastradio.registry;

import bili.dongsz.broadcastradio.BroadcastRadio;
import bili.dongsz.broadcastradio.block.SimpleRadioBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModBlocks {
    public static final DeferredRegister<Block> BLOCKS =
            DeferredRegister.create(ForgeRegistries.BLOCKS, BroadcastRadio.MOD_ID);

    public static final RegistryObject<Block> SIMPLE_RADIO_BLOCK = BLOCKS.register("simple_radio_block",
            () -> new SimpleRadioBlock(Block.Properties.of()
                    .noOcclusion()
                    .strength(3.5f)
                    .sound(SoundType.METAL)
                    .requiresCorrectToolForDrops()
                    .isViewBlocking((state, world, pos) -> false)
            )
    );
}
