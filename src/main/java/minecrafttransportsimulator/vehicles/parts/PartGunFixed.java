package minecrafttransportsimulator.vehicles.parts;

import minecrafttransportsimulator.jsondefs.JSONVehicle.VehiclePart;
import minecrafttransportsimulator.vehicles.main.EntityVehicleE_Powered;
import net.minecraft.nbt.NBTTagCompound;

public class PartGunFixed extends APartGun{	
		
	public PartGunFixed(EntityVehicleE_Powered vehicle, VehiclePart packPart, String partName, NBTTagCompound dataTag){
		super(vehicle, packPart, partName, dataTag);
	}
	
	@Override
	public float getMinYaw(){
		return 0;
	}
	
	@Override
	public float getMaxYaw(){
		return 0;
	}
	
	@Override
	public float getMinPitch(){
		return 0;
	}
	
	@Override
	public float getMaxPitch(){
		return 0;
	}
}
