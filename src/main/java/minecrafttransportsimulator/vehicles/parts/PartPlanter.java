package minecrafttransportsimulator.vehicles.parts;

import minecrafttransportsimulator.jsondefs.JSONVehicle.VehiclePart;
import minecrafttransportsimulator.vehicles.main.EntityVehicleE_Powered;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.inventory.InventoryBasic;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.common.IPlantable;

public final class PartPlanter extends APartGroundEffector{
	
	public PartPlanter(EntityVehicleE_Powered vehicle, VehiclePart packPart, String partName, NBTTagCompound dataTag){
		super(vehicle, packPart, partName, dataTag);
	}
	
	@Override
	public void performEffectsAt(BlockPos pos){
		//Check if we have farmland below and air above.
		BlockPos farmlandPos = pos;
		IBlockState farmlandState = vehicle.world.getBlockState(farmlandPos);
		Block farmlandBlock = farmlandState.getBlock();
		if(farmlandBlock.equals(Blocks.FARMLAND)){
			BlockPos cropPos = farmlandPos.up();
			Block cropBlock = vehicle.world.getBlockState(cropPos).getBlock();
			if(vehicle.world.isAirBlock(cropPos)){
				//Check for valid seeds and plant if able.
				for(APart part : vehicle.getVehicleParts()){
					if(part instanceof PartCrate){
						InventoryBasic crateInventory = ((PartCrate) part).crateInventory;
						for(byte i=0; i<crateInventory.getSizeInventory(); ++i){
							ItemStack stack = crateInventory.getStackInSlot(i);
							if(stack.getItem() instanceof IPlantable){
								IPlantable plantable = (IPlantable) stack.getItem();
								IBlockState plantState = plantable.getPlant(vehicle.world, cropPos);
								if(farmlandBlock.canSustainPlant(plantState, vehicle.world, farmlandPos, EnumFacing.UP, plantable)){
									vehicle.world.setBlockState(cropPos, plantState, 11);
									vehicle.world.playSound(partPos.x, partPos.y, partPos.z, plantState.getBlock().getSoundType(plantState, vehicle.world, pos, null).getPlaceSound(), SoundCategory.BLOCKS, 1.0F, 1.0F, false);
									crateInventory.decrStackSize(i, 1);
								}
							}
						}
					}
				}
			}
		}
	}
	
	@Override
	protected boolean effectIsBelowPart(){
		return true;
	}
}
