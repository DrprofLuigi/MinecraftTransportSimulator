package minecrafttransportsimulator.vehicles.parts;

import minecrafttransportsimulator.MTS;
import minecrafttransportsimulator.dataclasses.PackVehicleObject.PackPart;
import minecrafttransportsimulator.packets.parts.PacketPartEngineDamage;
import minecrafttransportsimulator.packets.parts.PacketPartEngineSignal;
import minecrafttransportsimulator.packets.parts.PacketPartEngineSignal.PacketEngineTypes;
import minecrafttransportsimulator.systems.ConfigSystem;
import minecrafttransportsimulator.systems.SFXSystem;
import minecrafttransportsimulator.systems.SFXSystem.FXPart;
import minecrafttransportsimulator.vehicles.main.EntityVehicleE_Powered;
import net.minecraft.client.Minecraft;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.DamageSource;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

public abstract class APartEngine extends APart implements FXPart{
	
	//NBT data
	public boolean isCreative;
	public boolean oilLeak;
	public boolean fuelLeak;
	public boolean brokenStarter;
	public double hours;
	
	//Runtime data
	public EngineStates state = EngineStates.ENGINE_OFF;
	public boolean backfired;
	public byte starterLevel;
	public int internalFuel;
	public double fuelFlow;
	public double RPM;
	public double temp = 20;
	public double oilPressure = 90;
	private double ambientTemp;
	private double engineHeat;
	private double coolingFactor;
	
	//Rotation data.  Should be set by each engine type individually.
	protected double engineRotationLast;
	protected double engineRotation;
	protected double engineDriveshaftRotation;
	protected double engineDriveshaftRotationLast;
	
	//Constants
	public static final float engineColdTemp = 30F;
	public static final float engineOverheatTemp1 = 115.556F;
	public static final float engineOverheatTemp2 = 121.111F;
	public static final float engineFailureTemp = 132.222F;
	public static final float engineOilDanger = 40F;
	public final float engineStallRPM;
	public final float engineStartRPM;
	
	
	public APartEngine(EntityVehicleE_Powered vehicle, PackPart packPart, String partName, NBTTagCompound dataTag){
		super(vehicle, packPart, partName, dataTag);
		engineStallRPM = pack.engine.maxRPM < 15000 ? 300 : 1500;
		engineStartRPM = pack.engine.maxRPM < 15000 ? 500 : 2000;
		if(dataTag.hasKey("engineState")){
			this.state = EngineStates.values()[dataTag.getByte("engineState")];
		}else{
			this.state = EngineStates.ENGINE_OFF;
		}
		
		isCreative = dataTag.getBoolean("isCreative");
		oilLeak = dataTag.getBoolean("oilLeak");
		fuelLeak = dataTag.getBoolean("fuelLeak");
		brokenStarter = dataTag.getBoolean("brokenStarter");
		hours = dataTag.getDouble("hours");
		RPM = dataTag.getDouble("rpm");
		MTS.proxy.addVehicleEngineSound(vehicle, this);
	}
	
	@Override
	public void attackPart(DamageSource source, float damage){
		if(source.isExplosion()){
			hours += damage*10;
			if(!oilLeak)oilLeak = Math.random() < ConfigSystem.getDoubleConfig("EngineLeakProbability")*10;
			if(!fuelLeak)fuelLeak = Math.random() < ConfigSystem.getDoubleConfig("EngineLeakProbability")*10;
			if(!brokenStarter)brokenStarter = Math.random() < 0.05;
			MTS.MTSNet.sendToAll(new PacketPartEngineDamage(this, damage*10));
		}else{
			hours += damage;
			if(source.isProjectile()){
				if(!oilLeak)oilLeak = Math.random() < ConfigSystem.getDoubleConfig("EngineLeakProbability");
				if(!fuelLeak)fuelLeak = Math.random() < ConfigSystem.getDoubleConfig("EngineLeakProbability");
			}
			MTS.MTSNet.sendToAll(new PacketPartEngineDamage(this, damage));
		}
	}
	
	@Override
	public void updatePart(){
		super.updatePart();
		fuelFlow = 0;
		
		//Check to see if electric or hand starter can keep running.
		if(state.esOn){
			if(starterLevel == 0){
				if(vehicle.electricPower > 2){
					starterLevel += pack.engine.starterDuration;
					if(vehicle.worldObj.isRemote){
						MTS.proxy.playSound(partPos, partName + "_cranking", 1, (float) (RPM/engineStartRPM));
					}
				}
			}
			if(starterLevel > 0){
				vehicle.electricUsage += 0.05F;
				if(vehicle.fuel > pack.engine.fuelConsumption*ConfigSystem.getDoubleConfig("FuelUsageFactor") && !isCreative){
					vehicle.fuel -= pack.engine.fuelConsumption*ConfigSystem.getDoubleConfig("FuelUsageFactor");
					fuelFlow += pack.engine.fuelConsumption*ConfigSystem.getDoubleConfig("FuelUsageFactor");
				}
			}
		}else if(state.hsOn){
			if(starterLevel == 0){
				state = state.magnetoOn ? EngineStates.MAGNETO_ON_STARTERS_OFF : EngineStates.ENGINE_OFF;
			}
		}
		
		if(starterLevel > 0){
			--starterLevel;
			if(RPM < engineStartRPM*1.2){
				RPM = Math.min(RPM + pack.engine.starterPower, engineStartRPM*1.2);
			}else{
				RPM = Math.max(RPM - pack.engine.starterPower, engineStartRPM*1.2);
			}
		}
		
		ambientTemp = 25*vehicle.worldObj.getBiome(vehicle.getPosition()).getTemperature() - 5*(Math.pow(2, vehicle.posY/400) - 1);
		coolingFactor = 0.001 + vehicle.velocity/500F;
		temp -= (temp - ambientTemp)*coolingFactor;
		vehicle.electricUsage -= 0.05*RPM/pack.engine.maxRPM;
		
		if(state.running){
			//First part is temp affect on oil, second is engine oil pump.
			oilPressure = Math.min(90 - temp/10, oilPressure + RPM/engineStartRPM - 0.5*(oilLeak ? 5F : 1F)*(oilPressure/engineOilDanger));
			if(oilPressure < engineOilDanger){
				temp += Math.max(0, (20*RPM/pack.engine.maxRPM)/20);
				hours += 0.01;
			}else{
				temp += Math.max(0, (7*RPM/pack.engine.maxRPM - temp/(engineColdTemp*2))/20);
				hours += 0.001;	
			}
			if(RPM > engineStartRPM*1.5 && temp < engineColdTemp){//Not warmed up
				hours += 0.001*(RPM/engineStartRPM - 1);
			}
			if(RPM > getSafeRPMFromMax(this.pack.engine.maxRPM)){//Too fast
				hours += 0.001*(RPM - getSafeRPMFromMax(this.pack.engine.maxRPM))/10F;
			}
			if(temp > engineOverheatTemp1){//Too hot
				hours += 0.001*(temp - engineOverheatTemp1);
				if(temp > engineFailureTemp && !vehicle.worldObj.isRemote){
					explodeEngine();
				}
			}
			
			if(hours > 200 && !vehicle.worldObj.isRemote){
				if(Math.random() < hours/10000*(getSafeRPMFromMax(this.pack.engine.maxRPM)/(RPM+getSafeRPMFromMax(this.pack.engine.maxRPM)/2))){
					backfireEngine();
				}
			}

			if(!isCreative){
				fuelFlow = Math.min(pack.engine.fuelConsumption*ConfigSystem.getDoubleConfig("FuelUsageFactor")*RPM*(fuelLeak ? 1.5F : 1.0F)/pack.engine.maxRPM, vehicle.fuel);
				vehicle.fuel -= fuelFlow;
			}
			if(!vehicle.worldObj.isRemote){
				if(vehicle.fuel == 0 && pack.engine.fuelConsumption != 0 && !isCreative){
					stallEngine(PacketEngineTypes.FUEL_OUT);
				}else if(RPM < engineStallRPM){
					stallEngine(PacketEngineTypes.TOO_SLOW);
				}else if(isInLiquid()){
					stallEngine(PacketEngineTypes.DROWN);
				}
			}
		}else{
			oilPressure = 0;
			if(RPM > engineStartRPM){
				if(vehicle.fuel > 0 || isCreative){
					if(!isInLiquid()){
						if(state.magnetoOn && !vehicle.worldObj.isRemote){
							startEngine();
						}
					}
				}
			}
			
			//Internal fuel is used for engine sound wind down.  NOT used for power.
			if(internalFuel > 0){
				--internalFuel;
				if(RPM < engineStartRPM){
					internalFuel = 0;
				}
			}
		}
	}
	
	@Override
	public void removePart(){
		super.removePart();
		this.state = EngineStates.ENGINE_OFF;
	}
	
	@Override
	public NBTTagCompound getPartNBTTag(){
		NBTTagCompound partData = new NBTTagCompound();
		partData.setBoolean("isCreative", this.isCreative);
		partData.setBoolean("oilLeak", this.oilLeak);
		partData.setBoolean("fuelLeak", this.fuelLeak);
		partData.setBoolean("brokenStarter", this.brokenStarter);
		partData.setDouble("hours", hours);
		partData.setByte("engineState", (byte) this.state.ordinal());
		partData.setDouble("rpm", this.RPM);
		return partData;
	}
	
	@Override
	public float getWidth(){
		return 1.0F;
	}

	@Override
	public float getHeight(){
		return 1.0F;
	}
	
	public void setMagnetoStatus(boolean on){
		if(on){
			if(state.equals(EngineStates.MAGNETO_OFF_ES_ON)){
				state = EngineStates.MAGNETO_ON_ES_ON;
			}else if(state.equals(EngineStates.MAGNETO_OFF_HS_ON)){
				state = EngineStates.MAGNETO_ON_HS_ON;
			}else if(state.equals(EngineStates.ENGINE_OFF)){
				state = EngineStates.MAGNETO_ON_STARTERS_OFF;
			}
		}else{
			if(state.equals(EngineStates.MAGNETO_ON_ES_ON)){
				state = EngineStates.MAGNETO_OFF_ES_ON;
			}else if(state.equals(EngineStates.MAGNETO_ON_HS_ON)){
				state = EngineStates.MAGNETO_OFF_HS_ON;
			}else if(state.equals(EngineStates.MAGNETO_ON_STARTERS_OFF)){
				state = EngineStates.ENGINE_OFF;
			}else if(state.equals(EngineStates.RUNNING)){
				state = EngineStates.ENGINE_OFF;
				internalFuel = 100;
				MTS.proxy.playSound(partPos, partName + "_starting", 1, 1);
			}
		}
	}
	
	public void setElectricStarterStatus(boolean engaged){
		if(!brokenStarter){
			if(engaged){
				if(state.equals(EngineStates.ENGINE_OFF)){
					state = EngineStates.MAGNETO_OFF_ES_ON;
				}else if(state.equals(EngineStates.MAGNETO_ON_STARTERS_OFF)){
					state = EngineStates.MAGNETO_ON_ES_ON;
				}else if(state.equals(EngineStates.RUNNING)){
					state =  EngineStates.RUNNING_ES_ON;
				}
			}else{
				if(state.equals(EngineStates.MAGNETO_OFF_ES_ON)){
					state = EngineStates.ENGINE_OFF;
				}else if(state.equals(EngineStates.MAGNETO_ON_ES_ON)){
					state = EngineStates.MAGNETO_ON_STARTERS_OFF;
				}else if(state.equals(EngineStates.RUNNING_ES_ON)){
					state = EngineStates.RUNNING;
				}
			}
		}
	}
	
	public void handStartEngine(){
		if(state.equals(EngineStates.ENGINE_OFF)){
			state = EngineStates.MAGNETO_OFF_HS_ON;
		}else if(state.equals(EngineStates.MAGNETO_ON_STARTERS_OFF)){
			state = EngineStates.MAGNETO_ON_HS_ON;
		}else if(state.equals(EngineStates.RUNNING)){
			state = EngineStates.RUNNING_HS_ON;
		}else{
			return;
		}
		starterLevel += pack.engine.starterDuration;
		if(vehicle.worldObj.isRemote){
			MTS.proxy.playSound(partPos, partName + "_cranking", 1, (float) (RPM/engineStartRPM));
		}
	}
	
	public void backfireEngine(){
		RPM -= pack.engine.maxRPM < 15000 ? 100 : 500;
		if(!vehicle.worldObj.isRemote){
			MTS.MTSNet.sendToAll(new PacketPartEngineSignal(this, PacketEngineTypes.BACKFIRE));
		}else{
			MTS.proxy.playSound(partPos, partName + "_sputter", 0.5F, 1);
			backfired = true;
		}
	}
	
	public void startEngine(){
		if(state.equals(EngineStates.MAGNETO_ON_STARTERS_OFF)){
			state = EngineStates.RUNNING;
		}else if(state.equals(EngineStates.MAGNETO_ON_ES_ON)){
			state = EngineStates.RUNNING_ES_ON;
		}else if(state.equals(EngineStates.MAGNETO_ON_HS_ON)){
			state = EngineStates.RUNNING;
		}
		starterLevel = 0;
		oilPressure = 60;
		if(!vehicle.worldObj.isRemote){
			MTS.MTSNet.sendToAll(new PacketPartEngineSignal(this, PacketEngineTypes.START));
		}else{
			MTS.proxy.playSound(partPos, partName + "_starting", 1, 1);
		}
	}
	
	public void stallEngine(PacketEngineTypes packetType){
		if(state.equals(EngineStates.RUNNING)){
			state = EngineStates.MAGNETO_ON_STARTERS_OFF;
		}else if(state.equals(EngineStates.RUNNING_ES_ON)){
			state = EngineStates.MAGNETO_ON_ES_ON;
		}else if(state.equals(EngineStates.RUNNING_HS_ON)){
			state = EngineStates.MAGNETO_ON_HS_ON;
		}
		if(!vehicle.worldObj.isRemote){
			MTS.MTSNet.sendToAll(new PacketPartEngineSignal(this, packetType));
		}else{
			if(!packetType.equals(PacketEngineTypes.DROWN)){
				internalFuel = 100;
				if(packetType.equals(PacketEngineTypes.FUEL_OUT)){
					vehicle.fuel = 0;
				}
			}
			MTS.proxy.playSound(partPos, partName + "_starting", 1, 1);
		}
	}
	
	protected void explodeEngine(){
		if(ConfigSystem.getBooleanConfig("Explosions")){
			vehicle.worldObj.newExplosion(vehicle, partPos.xCoord, partPos.yCoord, partPos.zCoord, 1F, true, true);
		}else{
			vehicle.worldObj.newExplosion(vehicle, partPos.xCoord, partPos.yCoord, partPos.zCoord, 0F, true, true);
		}
		vehicle.removePart(this, true);
	}
	
	public static int getSafeRPMFromMax(int maxRPM){
		return maxRPM < 15000 ? maxRPM - (maxRPM - 2500)/2 : (int) (maxRPM/1.1);
	}
	
	protected boolean isInLiquid(){
		return vehicle.worldObj.getBlockState(new BlockPos(partPos)).getMaterial().isLiquid();
	}
	
	public double getEngineRotation(float partialTicks){
		return engineRotation + (engineRotation - engineRotationLast)*partialTicks;
	}
	
	public double getDriveshaftRotation(float partialTicks){
		return engineDriveshaftRotation + (engineDriveshaftRotation - engineDriveshaftRotationLast)*partialTicks;
	}
	
	public abstract double getForceOutput();

	@Override
	@SideOnly(Side.CLIENT)
	public void spawnParticles(){
		if(Minecraft.getMinecraft().effectRenderer != null){
			if(temp > engineOverheatTemp1){
				Minecraft.getMinecraft().theWorld.spawnParticle(EnumParticleTypes.SMOKE_NORMAL, partPos.xCoord, partPos.yCoord + 0.5, partPos.zCoord, 0, 0.15, 0);
				if(temp > engineOverheatTemp2){
					Minecraft.getMinecraft().theWorld.spawnParticle(EnumParticleTypes.SMOKE_LARGE, partPos.xCoord, partPos.yCoord + 0.5, partPos.zCoord, 0, 0.15, 0);
				}
			}
			if(oilLeak){
				if(vehicle.ticksExisted%20 == 0){
					Minecraft.getMinecraft().effectRenderer.addEffect(new SFXSystem.OilDropParticleFX(vehicle.worldObj, partPos.xCoord - 0.25*Math.sin(Math.toRadians(vehicle.rotationYaw)), partPos.yCoord, partPos.zCoord + 0.25*Math.cos(Math.toRadians(vehicle.rotationYaw))));
				}
			}
			if(fuelLeak){
				if((vehicle.ticksExisted + 5)%20 == 0){
					Minecraft.getMinecraft().effectRenderer.addEffect(new SFXSystem.FuelDropParticleFX(vehicle.worldObj, partPos.yCoord, partPos.yCoord, partPos.zCoord));
				}
			}
			if(backfired){
				backfired = false;
				for(byte i=0; i<5; ++i){
					Minecraft.getMinecraft().theWorld.spawnParticle(EnumParticleTypes.SMOKE_LARGE, partPos.xCoord, partPos.yCoord + 0.5, partPos.zCoord, Math.random()*0.15, 0.15, Math.random()*0.15);
				}
			}
		}
	}
	
	public enum EngineStates{
		ENGINE_OFF(false, false, false, false),
		MAGNETO_ON_STARTERS_OFF(true, false, false, false),
		MAGNETO_OFF_ES_ON(false, true, false, false),
		MAGNETO_OFF_HS_ON(false, false, true, false),
		MAGNETO_ON_ES_ON(true, true, false, false),
		MAGNETO_ON_HS_ON(true, false, true, false),
		RUNNING(true, false, false, true),
		RUNNING_ES_ON(true, true, false, true),
		RUNNING_HS_ON(true, false, true, true);
		
		public final boolean magnetoOn;
		public final boolean esOn;
		public final boolean hsOn;
		public final boolean running;
		
		private EngineStates(boolean magnetoOn, boolean esOn, boolean hsOn, boolean running){
			this.magnetoOn = magnetoOn;
			this.esOn = esOn;
			this.hsOn = hsOn;
			this.running = running;
		}
	}
}
