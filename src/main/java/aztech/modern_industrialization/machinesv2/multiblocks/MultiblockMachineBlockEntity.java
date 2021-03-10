/*
 * MIT License
 *
 * Copyright (c) 2020 Azercoco & Technici4n
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package aztech.modern_industrialization.machinesv2.multiblocks;

import aztech.modern_industrialization.machinesv2.MachineBlockEntity;
import aztech.modern_industrialization.machinesv2.components.OrientationComponent;
import aztech.modern_industrialization.machinesv2.gui.MachineGuiParameters;
import aztech.modern_industrialization.machinesv2.helper.OrientationHelper;
import aztech.modern_industrialization.util.ChunkUnloadBlockEntity;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.math.Direction;

public abstract class MultiblockMachineBlockEntity extends MachineBlockEntity implements ChunkUnloadBlockEntity {
    public MultiblockMachineBlockEntity(BlockEntityType<?> type, MachineGuiParameters guiParams, OrientationComponent orientation) {
        super(type, guiParams);

        this.orientation = orientation;
        registerComponents(orientation);
    }

    protected final OrientationComponent orientation;

    protected abstract void unlink();

    @Override
    protected ActionResult onUse(PlayerEntity player, Hand hand, Direction face) {
        ActionResult result = OrientationHelper.onUse(player, hand, face, orientation, this);
        if (result.isAccepted() && world.isClient) {
            unlink();
        }
        return result;
    }

    @Override
    public final void markRemoved() {
        super.markRemoved();
        if (!world.isClient) {
            unlink();
        }
    }

    @Override
    public final void onChunkUnload() {
        unlink();
    }

    public abstract ShapeTemplate getActiveShape();
}
