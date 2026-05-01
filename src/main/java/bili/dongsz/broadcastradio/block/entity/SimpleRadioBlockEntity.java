package bili.dongsz.broadcastradio.block.entity;

import bili.dongsz.broadcastradio.block.SimpleRadioBlock;
import bili.dongsz.broadcastradio.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public class SimpleRadioBlockEntity extends BlockEntity {
    public static final String TAG_FREQUENCY = "Frequency";
    public static final String TAG_INTERFERENCE = "Interference";
    public static final float DEFAULT_FREQUENCY = 88.5f;
    public static final int DEFAULT_INTERFERENCE = 0;
    
    private float frequency = DEFAULT_FREQUENCY;
    private int interference = DEFAULT_INTERFERENCE;

    public SimpleRadioBlockEntity(BlockPos pPos, BlockState pBlockState) {
        super(ModBlockEntities.SIMPLE_RADIO_BLOCK_ENTITY.get(), pPos, pBlockState);
    }

    public float getFrequency() {
        return frequency;
    }

    public void setFrequency(float frequency) {
        this.frequency = frequency;
        setChanged();
        if (level != null) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    public int getInterference() {
        return interference;
    }

    public void setInterference(int interference) {
        this.interference = interference;
        setChanged();
        if (level != null) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    @Override
    protected void saveAdditional(CompoundTag pTag) {
        super.saveAdditional(pTag);
        pTag.putFloat(TAG_FREQUENCY, frequency);
        pTag.putInt(TAG_INTERFERENCE, interference);
    }

    @Override
    public void load(CompoundTag pTag) {
        super.load(pTag);
        if (pTag.contains(TAG_FREQUENCY)) {
            frequency = pTag.getFloat(TAG_FREQUENCY);
        }
        if (pTag.contains(TAG_INTERFERENCE)) {
            interference = pTag.getInt(TAG_INTERFERENCE);
        }
    }

    @Override
    public ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public CompoundTag getUpdateTag() {
        CompoundTag tag = new CompoundTag();
        saveAdditional(tag);
        return tag;
    }

    public static void tick(Level level, BlockPos pos, BlockState state, SimpleRadioBlockEntity blockEntity) {
        // 旧代码用了，秉持着能跑不动原则，放个空气在这里
    }
}