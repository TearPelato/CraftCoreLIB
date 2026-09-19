package net.tearpelato.craftcorelib.api.fluid.blockentity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import org.jetbrains.annotations.Nullable;

public abstract class FluidContainerBlockEntity extends BlockEntity {

    public static final int BUCKET_VOLUME = 1000;

    private Fluid fluid = Fluids.EMPTY;
    private int amount = 0;
    private final int capacity;

    public FluidContainerBlockEntity(BlockEntityType<?> type, BlockPos pos,
                                     BlockState state, int capacity) {
        super(type, pos, state);
        this.capacity = capacity;
    }

    public Fluid getFluid()   { return fluid; }
    public int getAmount()    { return amount; }
    public int getCapacity()  { return capacity; }

    public boolean isEmpty() {
        return fluid == Fluids.EMPTY || amount <= 0;
    }

    public void setFluid(Fluid fluid, int amount) {
        if (fluid == Fluids.EMPTY || amount <= 0) {
            this.fluid  = Fluids.EMPTY;
            this.amount = 0;
        } else {
            this.fluid  = fluid;
            this.amount = Math.min(amount, capacity);
        }
        setChanged();
        if (level != null && !level.isClientSide)
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
    }

    public void clear() {
        setFluid(Fluids.EMPTY, 0);
    }


    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        if (isEmpty()) return;
        ResourceLocation id = BuiltInRegistries.FLUID.getKey(fluid);
        tag.putString("FluidName", id.toString());
        tag.putInt("Amount", amount);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        String fluidName = null;
        int loadedAmount = 0;

        if (tag.contains("FluidName")) {
            fluidName    = tag.getString("FluidName");
            loadedAmount = tag.getInt("Amount");

        } else if (tag.contains("FluidStack")) {
            CompoundTag fs = tag.getCompound("FluidStack");
            if (fs.contains("id"))     fluidName    = fs.getString("id");
            if (fs.contains("amount")) loadedAmount = fs.getInt("amount");
        }

        if (fluidName == null || "minecraft:empty".equals(fluidName)) {
            this.fluid  = Fluids.EMPTY;
            this.amount = 0;
            return;
        }

        var holder = BuiltInRegistries.FLUID.getHolder(ResourceLocation.parse(fluidName));
        if (holder.isEmpty()) {
            this.fluid  = Fluids.EMPTY;
            this.amount = 0;
            return;
        }

        this.fluid  = holder.get().value();
        this.amount = Math.min(Math.max(loadedAmount, 0), capacity);
        if (this.amount == 0) this.fluid = Fluids.EMPTY;
    }

    @Nullable
    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return saveWithoutMetadata(registries);
    }
}