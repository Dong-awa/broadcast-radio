package bili.dongsz.broadcastradio.block;

import bili.dongsz.broadcastradio.block.entity.SimpleRadioBlockEntity;
import bili.dongsz.broadcastradio.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
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
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkHooks;

import javax.annotation.Nullable;

public class SimpleRadioBlock extends Block implements EntityBlock {
    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;

    public SimpleRadioBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    private VoxelShape getShapeForDirection(Direction direction) {
        switch (direction) {
            case NORTH:
                return Shapes.or(
                        box(3, 0, 6, 13, 7, 10),
                        box(11, 7, 7, 12, 13, 8),
                        box(9, 7, 8, 10, 13, 9)
                );
            case SOUTH:
                return Shapes.or(
                        box(3, 0, 6, 13, 7, 10),
                        box(4, 7, 7, 5, 13, 8),
                        box(6, 7, 8, 7, 13, 9)
                );
            case EAST:
                return Shapes.or(
                        box(6, 0, 3, 10, 7, 13),
                        box(7, 7, 11, 8, 13, 12),
                        box(8, 7, 9, 9, 13, 10)
                );
            case WEST:
            default:
                return Shapes.or(
                        box(6, 0, 3, 10, 7, 13),
                        box(7, 7, 4, 8, 13, 5),
                        box(8, 7, 6, 9, 13, 7)
                );
        }
    }

    @Override
    public void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean isMoving) {
        super.onPlace(state, level, pos, oldState, isMoving);
    }

    @Override
    public BlockState getStateForPlacement(net.minecraft.world.item.context.BlockPlaceContext context) {
        return this.defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }
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
        return new SimpleRadioBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (type == ModBlockEntities.SIMPLE_RADIO_BLOCK_ENTITY.get()) {
            return (level1, pos, state1, blockEntity) -> {
                if (blockEntity instanceof SimpleRadioBlockEntity) {
                    SimpleRadioBlockEntity.tick(level1, pos, state1, (SimpleRadioBlockEntity) blockEntity);
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
        if (blockEntity instanceof SimpleRadioBlockEntity) {
            // 打开调频GUI
            NetworkHooks.openScreen((net.minecraft.server.level.ServerPlayer) player, new SimpleMenuProvider(
                (containerId, playerInventory, playerEntity) ->
                    new bili.dongsz.broadcastradio.menu.SimpleRadioMenu(containerId, playerInventory, (SimpleRadioBlockEntity) blockEntity),
                Component.translatable("item.broadcast_radio.simple_radio_block.gui_title")
            ), buf -> buf.writeBlockPos(pos));
            return InteractionResult.CONSUME;
        }
        return InteractionResult.PASS;
    }

    @Override
    public void playerDestroy(Level level, Player player, BlockPos pos, BlockState state, @Nullable net.minecraft.world.level.block.entity.BlockEntity blockEntity, ItemStack tool) {
        if (!level.isClientSide) {
            popResource(level, pos, new ItemStack(this));
        }
        super.playerDestroy(level, player, pos, state, blockEntity, tool);
    }
}