package minecrafttransportsimulator.vehicles.parts;

import minecrafttransportsimulator.MTS;
import minecrafttransportsimulator.dataclasses.PackVehicleObject.PackPart;
import minecrafttransportsimulator.packets.parts.PacketPartGroundDeviceWheelFlat;
import minecrafttransportsimulator.systems.SFXSystem;
import minecrafttransportsimulator.systems.SFXSystem.FXPart;
import minecrafttransportsimulator.vehicles.main.EntityVehicleE_Powered;
import net.minecraft.client.Minecraft;
import net.minecraft.item.Item;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.DamageSource;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

public final class PartGroundDeviceWheel extends APartGroundDevice implements FXPart{
	private ResourceLocation flatModelLocation;
	
	private boolean isFlat;
	private boolean contactThisTick = false;
	private int ticksCalcsSkipped = 0;
	
	public PartGroundDeviceWheel(EntityVehicleE_Powered vehicle, PackPart packPart, String partName, NBTTagCompound dataTag){
		super(vehicle, packPart, partName, dataTag);
		this.isFlat = dataTag.getBoolean("isFlat");
	}
	
	@Override
	public void attackPart(DamageSource source, float damage){
		if(!this.isFlat){
			if(source.isExplosion() || Math.random() < 0.1){
				if(!vehicle.worldObj.isRemote){
					this.setFlat();
					Vec3d explosionPosition = partPos;
					vehicle.worldObj.newExplosion(vehicle, explosionPosition.xCoord, explosionPosition.yCoord, explosionPosition.zCoord, 0.25F, false, false);
					MTS.MTSNet.sendToAll(new PacketPartGroundDeviceWheelFlat(this));
				}
			}
		}
	}
	
	@Override
	public void updatePart(){
		super.updatePart();
		if(this.isOnGround()){
			//Set contact for wheel skid.
			if(angularVelocity/(vehicle.velocity/this.getHeight()) < 0.25 && vehicle.velocity > 0.3){
				BlockPos blockBelow = new BlockPos(partPos).down();
				if(vehicle.worldObj.getBlockState(blockBelow).getBlockHardness(vehicle.worldObj, blockBelow) >= 1.5){
					contactThisTick = true;
				}
			}
			
			//If we have a slipping wheel, count down and possibly pop it.
			if(!skipAngularCalcs){
				if(ticksCalcsSkipped > 0 && !isFlat){
					--ticksCalcsSkipped;
				}
			}else if(!isFlat){
				++ticksCalcsSkipped;
				if(Math.random()*50000 < ticksCalcsSkipped){
					if(!vehicle.worldObj.isRemote){
						this.setFlat();
						Vec3d explosionPosition = partPos;
						vehicle.worldObj.newExplosion(vehicle, explosionPosition.xCoord, explosionPosition.yCoord, explosionPosition.zCoord, 0.25F, false, false);
						MTS.MTSNet.sendToAll(new PacketPartGroundDeviceWheelFlat(this));
					}
				}
			}
		}
	}
	
	@Override
	public NBTTagCompound getPartNBTTag(){
		NBTTagCompound dataTag = new NBTTagCompound();
		dataTag.setBoolean("isFlat", this.isFlat);
		return dataTag;
	}
	
	@Override
	public float getWidth(){
		return this.pack.wheel.diameter/2F;
	}
	
	@Override
	public float getHeight(){
		return this.isFlat ? this.pack.wheel.diameter/2F : this.pack.wheel.diameter;
	}
	
	@Override
	public Item getItemForPart(){
		return this.isFlat ? null : super.getItemForPart();
	}
	
	@Override
	public ResourceLocation getModelLocation(){
		if(this.isFlat){
			if(flatModelLocation == null){
				if(pack.general.modelName != null){
					flatModelLocation = new ResourceLocation(partName.substring(0, partName.indexOf(':')), "objmodels/parts/" + pack.general.modelName + "_flat.obj");
				}else{
					flatModelLocation = new ResourceLocation(partName.substring(0, partName.indexOf(':')), "objmodels/parts/" + partName.substring(partName.indexOf(':') + 1) + "_flat.obj");
				}
			}
			return flatModelLocation;
		}else{
			return super.getModelLocation();
		}
	}
	
	@Override
	public Vec3d getActionRotation(float partialTicks){
		return new Vec3d(Math.toDegrees(this.angularPosition + this.angularVelocity*partialTicks), 0, 0);
	}
	
	@Override
	public float getMotiveFriction(){
		return !this.isFlat ? this.pack.wheel.motiveFriction : this.pack.wheel.motiveFriction/10F;
	}
	
	@Override
	public float getLateralFriction(){
		return !this.isFlat ? this.pack.wheel.lateralFriction : this.pack.wheel.lateralFriction/10F;
	}
	
	@Override
	public float getLongPartOffset(){
		return 0;
	}
	
	@Override
	public boolean canBeDrivenByEngine(){
		return true;
	}
	
	public void setFlat(){
		this.isFlat = true;
	}
	
	@Override
	@SideOnly(Side.CLIENT)
	public void spawnParticles(){
		if(contactThisTick){
			for(byte i=0; i<4; ++i){
				Minecraft.getMinecraft().effectRenderer.addEffect(new SFXSystem.WhiteSmokeFX(vehicle.worldObj, partPos.xCoord, partPos.yCoord, partPos.zCoord, Math.random()*0.10 - 0.05, 0.15, Math.random()*0.10 - 0.05));
			}
			MTS.proxy.playSound(this.partPos, MTS.MODID + ":" + "wheel_striking", 1, 1);
			contactThisTick = false;
		}
		if(skipAngularCalcs && this.isOnGround()){
			for(byte i=0; i<4; ++i){
				Minecraft.getMinecraft().effectRenderer.addEffect(new SFXSystem.WhiteSmokeFX(vehicle.worldObj, partPos.xCoord, partPos.yCoord, partPos.zCoord, Math.random()*0.10 - 0.05, 0.15, Math.random()*0.10 - 0.05));
			}
		}
	}
}
