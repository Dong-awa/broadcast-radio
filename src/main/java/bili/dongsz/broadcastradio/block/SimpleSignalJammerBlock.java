package bili.dongsz.broadcastradio.block;

import bili.dongsz.broadcastradio.block.entity.SimpleSignalJammerBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraftforge.network.NetworkHooks;

import javax.annotation.Nullable;

public class SimpleSignalJammerBlock extends Block implements EntityBlock {
    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;

    public SimpleSignalJammerBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override
    public BlockState getStateForPlacement(net.minecraft.world.item.context.BlockPlaceContext context) {
        return this.defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    private VoxelShape getShapeForDirection(Direction direction) {
        switch (direction) {
            case SOUTH:
                return Shapes.or(
                        box(2, 0, 4, 14, 4, 13),
                        box(3, 1, 2, 5, 11, 3),
                        box(7, 1, 2, 9, 11, 3),
                        box(11, 1, 2, 13, 11, 3)
                );
            case NORTH:
                return Shapes.or(
                        box(2, 0, 3, 14, 4, 12),
                        box(11, 1, 13, 13, 11, 14),
                        box(7, 1, 13, 9, 11, 14),
                        box(3, 1, 13, 5, 11, 14)
                );
            case WEST:
                return Shapes.or(
                        box(3, 0, 2, 12, 4, 14),
                        box(13, 1, 3, 14, 11, 5),
                        box(13, 1, 7, 14, 11, 9),
                        box(13, 1, 11, 14, 11, 13)
                );
            case EAST:
            default:
                return Shapes.or(
                        box(4, 0, 2, 13, 4, 14),
                        box(2, 1, 11, 3, 11, 13),
                        box(2, 1, 7, 3, 11, 9),
                        box(2, 1, 3, 3, 11, 5)
                );
        }
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockGetter world, BlockPos pos, CollisionContext context) {
        return getShapeForDirection(state.getValue(FACING));
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter world, BlockPos pos, CollisionContext context) {
        return getShapeForDirection(state.getValue(FACING));
    }

    @Override
    public VoxelShape getInteractionShape(BlockState state, BlockGetter world, BlockPos pos) {
        return getShapeForDirection(state.getValue(FACING));
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new SimpleSignalJammerBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (type == bili.dongsz.broadcastradio.registry.ModBlockEntities.SIMPLE_SIGNAL_JAMMER_BLOCK_ENTITY.get()) {
            return (level1, pos, state1, blockEntity) -> {
                if (blockEntity instanceof SimpleSignalJammerBlockEntity) {
                    SimpleSignalJammerBlockEntity.tick(level1, pos, state1, (SimpleSignalJammerBlockEntity) blockEntity);
                }
            };
        }
        return null;
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }

        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (blockEntity instanceof SimpleSignalJammerBlockEntity) {
            NetworkHooks.openScreen((net.minecraft.server.level.ServerPlayer) player, new SimpleMenuProvider(
                (containerId, playerInventory, playerEntity) ->
                    new bili.dongsz.broadcastradio.menu.SimpleSignalJammerMenu(containerId, playerInventory, (SimpleSignalJammerBlockEntity) blockEntity),
                Component.translatable("block.broadcast_radio.simple_signal_jammer.gui_title")
            ), buf -> buf.writeBlockPos(pos));
            return InteractionResult.CONSUME;
        }
        return InteractionResult.PASS;
    }

    @Override
    public void playerDestroy(Level level, Player player, BlockPos pos, BlockState state, @Nullable BlockEntity blockEntity, ItemStack tool) {
        if (!level.isClientSide) {
            popResource(level, pos, new ItemStack(this));
        }
        super.playerDestroy(level, player, pos, state, blockEntity, tool);
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean moved) {
        if (!state.is(newState.getBlock())) {
            BlockEntity blockEntity = level.getBlockEntity(pos);
            if (blockEntity instanceof SimpleSignalJammerBlockEntity && !level.isClientSide) {
                SimpleSignalJammerBlockEntity jammerEntity = (SimpleSignalJammerBlockEntity) blockEntity;
                ItemStack batteryStack = jammerEntity.getItem(0);
                if (!batteryStack.isEmpty()) {
                    popResource(level, pos, batteryStack.copy());
                    jammerEntity.setItem(0, ItemStack.EMPTY);
                }
            }
        }
        super.onRemove(state, level, pos, newState, moved);
    }
}